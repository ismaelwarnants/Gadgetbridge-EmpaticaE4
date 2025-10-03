/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vibratissimo;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;

public class VibratissimoSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(VibratissimoSupport.class);
    private final DeviceInfoProfile<VibratissimoSupport> deviceInfoProfile;
    private final BatteryInfoProfile<VibratissimoSupport> batteryInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String s = intent.getAction();
            if (s.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            } else if (s.equals(BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                handleBatteryInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
            }
        }
    };

    public VibratissimoSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(UUID.fromString("00001523-1212-efde-1523-785feabcd123"));

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
        addSupportedProfile(batteryInfoProfile);
    }

    private void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        requestDeviceInfo(builder);
        setInitialized(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        return builder;
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZED);
    }


    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    @Override
    public void onFindDevice(boolean start) {
        onSetConstantVibration(start ? 0xff : 0x00);
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        getQueue().clear();
        BluetoothGattCharacteristic characteristic2 = getCharacteristic(UUID.fromString("00001526-1212-efde-1523-785feabcd123"));
        BluetoothGattCharacteristic characteristic1 = getCharacteristic(UUID.fromString("00001524-1212-efde-1523-785feabcd123"));

        TransactionBuilder builder = createTransactionBuilder("vibration");
        builder.write(characteristic1, new byte[]{0x03, (byte) 0x80});

        builder.write(characteristic2, new byte[]{(byte) intensity, 0x00});
        builder.queue();
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, byte[] value,
                                        int status) {
        if (super.onCharacteristicRead(gatt, characteristic, value, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
