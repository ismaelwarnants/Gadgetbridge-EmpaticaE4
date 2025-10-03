/*  Copyright (C) 2020-2024 Arjan Schrijver, opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventBase;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm.BandAlarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm.BandAlarms;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.CommandCode;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.ControlPointLowVibration;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control.ControlPointWithValue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.time.BandTime;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

// done:
// - time sync
// - alarms (also smart)
// - fetching activity(walking, sleep)
// - stamina mode
// - vibration intensity
// - realtime heart rate
// todo options:
// - "get moving"
// - get notified: -call, -notification, -notification from, -do not disturb
// - media control: media/find phone(tap once for play pause, tap twice for next, tap triple for previous)

public class SonySWR12DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SonySWR12DeviceSupport.class);
    private SonySWR12HandlerThread processor = null;

    private final BatteryInfoProfile<SonySWR12DeviceSupport> batteryInfoProfile;
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            if (intent.getAction().equals(BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
                GBDeviceEventBatteryInfo gbInfo = new GBDeviceEventBatteryInfo();
                gbInfo.level = (short) info.getPercentCharged();
                handleGBDeviceEvent(gbInfo);
            }
        }
    };

    public SonySWR12DeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(SonySWR12Constants.UUID_SERVICE_AHS);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        initialize();
        setTime(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        return builder;
    }

    private SonySWR12HandlerThread getProcessor() {
        if (processor == null) {
            processor = new SonySWR12HandlerThread(getDevice(), getContext());
            processor.start();
        }
        return processor;
    }

    private void initialize() {
        if (gbDevice.getState() != GBDevice.State.INITIALIZED) {
            gbDevice.setFirmwareVersion("N/A");
            gbDevice.setFirmwareVersion2("N/A");
            gbDevice.setUpdateState(GBDevice.State.INITIALIZED, getContext());
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            setTime(builder);
            builder.queue();
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void setTime(TransactionBuilder builder) {
        builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_TIME, new BandTime(Calendar.getInstance()).toByteArray());
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("alarm");
            int prefInterval = Integer.valueOf(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                    .getString(DeviceSettingsPreferenceConst.PREF_SONYSWR12_SMART_INTERVAL, "0"));
            ArrayList<BandAlarm> bandAlarmList = new ArrayList<>();
            for (Alarm alarm : alarms) {
                BandAlarm bandAlarm = BandAlarm.fromAppAlarm(alarm, bandAlarmList.size(), alarm.getSmartWakeup() ? prefInterval : 0);
                if (bandAlarm != null)
                    bandAlarmList.add(bandAlarm);
            }
            builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_ALARM, new BandAlarms(bandAlarmList).toByteArray());
            builder.queue();
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting alarms: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value))
            return true;
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT)) {
            try {
                EventBase event = EventFactory.readEventFromByteArray(value);
                getProcessor().process(event);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            TransactionBuilder builder = performInitialized("fetchActivity");
            builder.notify(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT, true);
            ControlPointWithValue flushControl = new ControlPointWithValue(CommandCode.FLUSH_ACTIVITY, 0);
            builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT, flushControl.toByteArray());
            builder.queue();
        } catch (Exception e) {
            LOG.error("failed to fetch activity data", e);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.notify(SonySWR12Constants.UUID_CHARACTERISTIC_EVENT, enable);
            ControlPointWithValue controlPointHeart = new ControlPointWithValue(CommandCode.HEARTRATE_REALTIME, enable ? 1 : 0);
            builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT, controlPointHeart.toByteArray());
            builder.queue();
        } catch (IOException ex) {
            LOG.error("Unable to read heart rate from Sony device", ex);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        try {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_STAMINA: {
                    //stamina can be:
                    //disabled = 0, enabled = 1 or todo auto on low battery = 2
                    int status = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(config, false) ? 1 : 0;
                    TransactionBuilder builder = performInitialized(config);
                    ControlPointWithValue vibrationControl = new ControlPointWithValue(CommandCode.STAMINA_MODE, status);
                    builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT, vibrationControl.toByteArray());
                    builder.queue();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_LOW_VIBRATION: {
                    boolean isEnabled = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(config, false);
                    TransactionBuilder builder = performInitialized(config);
                    ControlPointLowVibration vibrationControl = new ControlPointLowVibration(isEnabled);
                    builder.write(SonySWR12Constants.UUID_CHARACTERISTIC_CONTROL_POINT, vibrationControl.toByteArray());
                    builder.queue();
                    break;
                }
                case DeviceSettingsPreferenceConst.PREF_SONYSWR12_SMART_INTERVAL: {
                    onSetAlarms(new ArrayList(DBHelper.getAlarms(gbDevice)));
                }
            }
        } catch (Exception exc) {
            LOG.error("failed to send config " + config, exc);
        }
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
