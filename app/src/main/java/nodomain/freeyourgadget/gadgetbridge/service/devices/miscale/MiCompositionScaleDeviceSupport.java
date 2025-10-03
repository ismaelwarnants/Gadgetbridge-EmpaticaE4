/*  Copyright (C) 2019-2024 Andreas Shimokawa, Arjan Schrijver, Damien Gaignon,
    Jean-François Greffier

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.miscale;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Parcelable;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.miscale.MiScaleSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.MiScaleWeightSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MiCompositionScaleDeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MiCompositionScaleDeviceSupport.class);

    private final DeviceInfoProfile<MiCompositionScaleDeviceSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

    public MiCompositionScaleDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BODY_COMPOSITION);
        addSupportedService(UUID.fromString("00001530-0000-3512-2118-0009af100700"));

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        final IntentListener mListener = intent -> {
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(intent.getAction())) {
                final Parcelable deviceInfo = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                if (deviceInfo != null) {
                    handleDeviceInfo((DeviceInfo) deviceInfo);
                }
            }
        };
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
        builder.setDeviceState(GBDevice.State.INITIALIZED);

        // Weight and body composition
        builder.setCallback(this);
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_BODY_COMPOSITION_MEASUREMENT, true);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] data) {
        if (super.onCharacteristicChanged(gatt, characteristic, data)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();
        if (characteristicUUID.equals(GattCharacteristic.UUID_CHARACTERISTIC_BODY_COMPOSITION_MEASUREMENT)) {
            final byte flags = data[1];
            final boolean stabilized = testBit(flags, 5) && !testBit(flags, 7);

            if (stabilized) {
                final int year = BLETypeConversions.toUint16(data, 2);
                final int month = BLETypeConversions.toUnsigned(data, 4);
                final int day = BLETypeConversions.toUnsigned(data, 5);
                final int hour = BLETypeConversions.toUnsigned(data, 6);
                final int minute = BLETypeConversions.toUnsigned(data, 7);
                final int second = BLETypeConversions.toUnsigned(data, 8);
                final Calendar c = GregorianCalendar.getInstance();
                c.set(year, month - 1, day, hour, minute, second);
                final Date date = c.getTime();

                float weightKg = WeightMeasurement.weightToKg(
                        BLETypeConversions.toUint16(data, 11),
                        flags
                );
                handleWeightInfo(date, weightKg);
            }

            return true;
        }

        return false;
    }

    private boolean testBit(final byte value, final int offset) {
        return ((value >> offset) & 1) == 1;
    }

    private void handleDeviceInfo(final DeviceInfo info) {
        LOG.debug("Device info: {}", info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getSoftwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void handleWeightInfo(final Date date, final float weightKg) {
        GB.toast(getContext().getString(R.string.weight_kg, weightKg), Toast.LENGTH_SHORT, GB.INFO);

        try (DBHandler db = GBApplication.acquireDB()) {
            final MiScaleSampleProvider provider = new MiScaleSampleProvider(getDevice(), db.getDaoSession());
            final Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            final Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();

            provider.addSample(new MiScaleWeightSample(
                    date.getTime(),
                    deviceId,
                    userId,
                    weightKg
            ));
        } catch (final Exception e) {
            LOG.error("Error saving weight sample", e);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
