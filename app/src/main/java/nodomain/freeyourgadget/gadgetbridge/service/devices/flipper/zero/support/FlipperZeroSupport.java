/*  Copyright (C) 2022-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.flipper.zero.support;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.MessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.NestedMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.RootMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.StringMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.VarintMessageField;

public class FlipperZeroSupport extends FlipperZeroBaseSupport{
    private static final Logger LOG = LoggerFactory.getLogger(FlipperZeroSupport.class);
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);

    private BatteryInfoProfile batteryInfoProfile = new BatteryInfoProfile(this);

    private final String UUID_SERIAL_SERVICE = "8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000";
    private final String UUID_SERIAL_CHARACTERISTIC_WRITE = "19ed82ae-ed21-4c9d-4145-228e62fe0000";
    private final String UUID_SERIAL_CHARACTERISTIC_RESPONSE = "19ed82ae-ed21-4c9d-4145-228e61fe0000";

    private final String COMMAND_PLAY_FILE = "nodomain.freeyourgadget.gadgetbridge.flipper.zero.PLAY_FILE";
    private final String ACTION_PLAY_DONE = "nodomain.freeyourgadget.gadgetbridge.flipper.zero.PLAY_DONE";

    private final int REQUEST_ID_OPEN_APP = 16;
    private final int REQUEST_ID_EXIT_APP = 47;
    private final int REQUEST_ID_LOAD_FILE = 48;
    private final int REQUEST_ID_PRESS_BUTTON = 49;
    private final int REQUEST_ID_RELEASE_BUTTON = 50;

    private int messageId = 0;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(COMMAND_PLAY_FILE.equals(intent.getAction())){
                        handlePlaySubGHZ(intent);
                    }
                }
            }, "FlipperZeroSupport_" + THREAD_COUNTER.getAndIncrement()).start();
        }
    };
    boolean recevierRegistered = false;

    private void handlePlaySubGHZ(Intent intent) {
        String appName = intent.getExtras().getString("EXTRA_APP_NAME", "Sub-GHz");

        String filePath = intent.getStringExtra("EXTRA_FILE_PATH");
        if(filePath == null){
            LOG.error("missing EXTRA_FILE_PATH in intent");
            return;
        }
        if(filePath.isEmpty()){
            LOG.error("empty EXTRA_FILE_PATH in intent");
            return;
        }

        String buttonName = intent.getExtras().getString("EXTRA_BUTTON_NAME", "center");

        long millis = intent.getExtras().getInt("EXTRA_DURATION", 1000);

        GB.toast(String.format("playing %s file", appName), Toast.LENGTH_SHORT, GB.INFO);
        playFile(appName, filePath, buttonName, millis);

        Intent response = new Intent(ACTION_PLAY_DONE);
        getContext().sendBroadcast(response);
    }

    public FlipperZeroSupport() {
        super();

        batteryInfoProfile.addListener(new IntentListener() {
            @Override
            public void notify(Intent intent) {
                BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
                GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
                batteryEvent.state = BatteryState.BATTERY_NORMAL;
                batteryEvent.level = info.getPercentCharged();
                evaluateGBDeviceEvent(batteryEvent);
            }
        });
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedProfile(batteryInfoProfile);

        addSupportedService(UUID.fromString(UUID_SERIAL_SERVICE));
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        if(!recevierRegistered) {
            ContextCompat.registerReceiver(getContext(), receiver, new IntentFilter(COMMAND_PLAY_FILE), ContextCompat.RECEIVER_EXPORTED);
            recevierRegistered = true;
        }

        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.read(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING);

        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        return builder
                .notify(UUID.fromString(UUID_SERIAL_CHARACTERISTIC_RESPONSE), true)
                .requestMtu(512)
                .setDeviceState(GBDevice.State.INITIALIZED);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if(characteristic.getUuid().equals(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING)){
            String revision = BLETypeConversions.getStringValue(value, 0);
            getDevice().setFirmwareVersion(revision);
            getDevice().sendDeviceUpdateIntent(getContext());
        }
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            super.dispose();

            if (recevierRegistered) {
                getContext().unregisterReceiver(receiver);
                recevierRegistered = false;
            }
        }
    }

    private void sendSerialData(byte[] data){
        createTransactionBuilder("send serial data")
                .write(UUID.fromString(UUID_SERIAL_CHARACTERISTIC_WRITE), data)
                .queue();
    }

    private RootMessageField createMainRequest(int requestFieldNumber, MessageField... children){
        return new RootMessageField(
                new VarintMessageField(1, messageId++),
                new NestedMessageField(requestFieldNumber, children)
        );
    }

    private void sendMainRequest(int requestFieldNumber, MessageField... children) throws IOException {
        RootMessageField root = createMainRequest(requestFieldNumber, children);
        sendSerialData(root.encodeToBytes());
    }

    private void openApp(String appName) throws IOException {
        sendMainRequest(
                REQUEST_ID_OPEN_APP,
                new StringMessageField(1, appName),
                new StringMessageField(2, "RPC")
        );
    }

    private void openSubGhzApp() throws IOException {
        openApp("Sub-GHz");
    }

    private void appLoadFile(String filePath) throws IOException {
        sendMainRequest(
                REQUEST_ID_LOAD_FILE,
                new StringMessageField(1, filePath)
        );
    }

    private void appButtonPress(String button) throws IOException {
        sendMainRequest(
                REQUEST_ID_PRESS_BUTTON,
                new StringMessageField(1, button)
        );
    }

    private void appButtonRelease() throws IOException {
        sendMainRequest(
                REQUEST_ID_RELEASE_BUTTON
        );
    }
    private void appExitRequest() throws IOException {
        sendMainRequest(
                REQUEST_ID_EXIT_APP
        );
    }

    @Override
    public void onTestNewFunction() {
        try {
            openApp("Infrared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playFile(String appName, String filePath, String buttonName, long durationMillis){
        try {
            openApp(appName);
            Thread.sleep(1000);
            appLoadFile(filePath);
            Thread.sleep(500);
            appButtonPress(buttonName);
            Thread.sleep(durationMillis);
            appButtonRelease();
            Thread.sleep(100);
            appExitRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        onTestNewFunction();
    }
}
