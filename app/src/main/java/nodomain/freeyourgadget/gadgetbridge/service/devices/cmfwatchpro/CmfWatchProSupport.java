/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseTransitSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.CmfWatchProCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherMapper;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;

public class CmfWatchProSupport extends AbstractBTLESingleDeviceSupport implements CmfCharacteristic.Handler {
    private static final Logger LOG = LoggerFactory.getLogger(CmfWatchProSupport.class);

    public static final UUID UUID_SERVICE_CMF_CMD = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CMF_COMMAND_READ = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CMF_COMMAND_WRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_SERVICE_CMF_DATA = UUID.fromString("02f00000-0000-0000-0000-00000000ffe0");
    public static final UUID UUID_CHARACTERISTIC_CMF_DATA_WRITE = UUID.fromString("02f00000-0000-0000-0000-00000000ffe1");
    public static final UUID UUID_CHARACTERISTIC_CMF_DATA_READ = UUID.fromString("02f00000-0000-0000-0000-00000000ffe2");

    public static final UUID UUID_SERVICE_CMF_SHELL = UUID.fromString("77d4e67c-2fe2-2334-0d35-9ccd078f529c");
    public static final UUID UUID_CHARACTERISTIC_CMF_SHELL_WRITE = UUID.fromString("77d4ff01-2fe2-2334-0d35-9ccd078f529c");
    public static final UUID UUID_CHARACTERISTIC_CMF_SHELL_READ = UUID.fromString("77d4ff02-2fe2-2334-0d35-9ccd078f529c");

    public static final UUID UUID_SERVICE_CMF_FIRMWARE = UUID.fromString("02f00000-0000-0000-0000-00000000fe00");
    public static final UUID UUID_CHARACTERISTIC_CMF_FIRMWARE_WRITE = UUID.fromString("02f00000-0000-0000-0000-00000000ff01");
    public static final UUID UUID_CHARACTERISTIC_CMF_FIRMWARE_READ = UUID.fromString("02f00000-0000-0000-0000-00000000ff02");

    // An a5 byte is used a lot in single payloads, probably as a "proof of encryption"?
    public static final byte A5 = (byte) 0xa5;

    private CmfCharacteristic characteristicCommandRead;
    private CmfCharacteristic characteristicCommandWrite;
    private CmfCharacteristic characteristicDataRead;
    private CmfCharacteristic characteristicDataWrite;
    @Nullable
    private CmfCharacteristic characteristicFirmwareRead;
    @Nullable
    private CmfCharacteristic characteristicFirmwareWrite;

    private final byte[] authRandom1 = new byte[16];
    private final byte[] authAppSecret = new byte[16];

    private final CmfActivitySync activitySync = new CmfActivitySync(this);
    private final CmfPreferences preferences = new CmfPreferences(this);
    private CmfDataUploader dataUploader;

    protected MediaManager mediaManager = null;

    public CmfWatchProSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_CMF_CMD);
        addSupportedService(UUID_SERVICE_CMF_DATA);
        addSupportedService(UUID_SERVICE_CMF_SHELL);
        addSupportedService(UUID_SERVICE_CMF_FIRMWARE);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        final BluetoothGattCharacteristic btCharacteristicCommandRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_COMMAND_READ);
        if (btCharacteristicCommandRead == null) {
            LOG.warn("Characteristic command read is null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicCommandWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_COMMAND_WRITE);
        if (btCharacteristicCommandWrite == null) {
            LOG.warn("Characteristic command write is null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicDataWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_DATA_WRITE);
        if (btCharacteristicDataWrite == null) {
            LOG.warn("Characteristic data write is null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicDataRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_DATA_READ);
        if (btCharacteristicDataRead == null) {
            LOG.warn("Characteristic data read is null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            return builder;
        }

        final BluetoothGattCharacteristic btCharacteristicShellWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_SHELL_WRITE);
        if (btCharacteristicShellWrite == null) {
            LOG.warn("Characteristic shell write is null");
        }

        final BluetoothGattCharacteristic btCharacteristicShellRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_SHELL_READ);
        if (btCharacteristicShellRead == null) {
            LOG.warn("Characteristic shell read is null");
        }

        final BluetoothGattCharacteristic btCharacteristicFirmwareWrite = getCharacteristic(UUID_CHARACTERISTIC_CMF_FIRMWARE_WRITE);
        if (btCharacteristicFirmwareWrite == null) {
            LOG.warn("Characteristic firmware write is null");
        }

        final BluetoothGattCharacteristic btCharacteristicFirmwareRead = getCharacteristic(UUID_CHARACTERISTIC_CMF_FIRMWARE_READ);
        if (btCharacteristicFirmwareRead == null) {
            LOG.warn("Characteristic firmware read is null");
        }

        dataUploader = new CmfDataUploader(this);

        characteristicCommandRead = new CmfCharacteristic(btCharacteristicCommandRead, this);
        characteristicCommandWrite = new CmfCharacteristic(btCharacteristicCommandWrite, null);
        characteristicDataRead = new CmfCharacteristic(btCharacteristicDataRead, dataUploader);
        characteristicDataWrite = new CmfCharacteristic(btCharacteristicDataWrite, null);
        if (btCharacteristicFirmwareRead != null && btCharacteristicFirmwareWrite != null) {
            characteristicFirmwareRead = new CmfCharacteristic(btCharacteristicFirmwareRead, dataUploader);
            characteristicFirmwareWrite = new CmfCharacteristic(btCharacteristicFirmwareWrite, null);
        }

        builder.notify(btCharacteristicCommandRead, true);
        builder.notify(btCharacteristicDataRead, true);
        if (btCharacteristicShellRead != null) {
            builder.notify(btCharacteristicShellRead, true);
        }
        if (btCharacteristicFirmwareRead != null) {
            builder.notify(btCharacteristicFirmwareRead, true);
        }

        builder.setDeviceState(GBDevice.State.AUTHENTICATING);

        final byte[] secretKey = getSecretKey(getDevice());

        if (secretKey != null) {
            characteristicCommandRead.setSessionKey(secretKey);
            characteristicCommandWrite.setSessionKey(secretKey);
            characteristicDataRead.setSessionKey(secretKey);
            characteristicDataWrite.setSessionKey(secretKey);
            if (characteristicFirmwareRead != null) {
                characteristicFirmwareRead.setSessionKey(secretKey);
            }
            if (characteristicFirmwareWrite != null) {
                characteristicFirmwareWrite.setSessionKey(secretKey);
            }

            sendCommand(builder, CmfCommand.AUTH_PHONE_NAME, ArrayUtils.addAll(new byte[]{A5}, Build.MODEL.getBytes(StandardCharsets.UTF_8)));
        } else if (btCharacteristicShellWrite != null) {
            builder.write(UUID_CHARACTERISTIC_CMF_SHELL_WRITE, "AT GETSECRET".getBytes());
        } else {
            GB.toast(getContext(), R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN);
            builder.setDeviceState(GBDevice.State.NOT_CONNECTED);
        }

        return builder;
    }

    @Override
    public void setContext(final GBDevice device, final BluetoothAdapter adapter, final Context context) {
        super.setContext(device, adapter, context);

        mediaManager = new MediaManager(context);
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(characteristicCommandRead.getCharacteristicUUID())) {
            characteristicCommandRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicUUID.equals(characteristicDataRead.getCharacteristicUUID())) {
            characteristicDataRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicFirmwareRead != null && characteristicUUID.equals(characteristicFirmwareRead.getCharacteristicUUID())) {
            characteristicFirmwareRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicUUID.equals(UUID_CHARACTERISTIC_CMF_SHELL_READ)) {
            handleShellCommand(value);
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {} {}", characteristicUUID, GB.hexdump(value));
        return false;
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        super.onMtuChanged(gatt, mtu, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        characteristicCommandRead.setMtu(mtu);
        characteristicCommandWrite.setMtu(mtu);
        characteristicDataRead.setMtu(mtu);
        characteristicDataWrite.setMtu(mtu);
    }

    @Override
    public void onCommand(final CmfCommand cmd, final byte[] payload) {
        if (activitySync.onCommand(cmd, payload)) {
            return;
        }

        if (preferences.onCommand(cmd, payload)) {
            return;
        }

        switch (cmd) {
            case DATA_TRANSFER_FIRMWARE_INIT_1_REPLY:
                // This one comes in the command characteristic because of reasons
                dataUploader.onCommand(cmd, payload);
                return;
            case AUTH_PAIR_REPLY:
                final byte[] authRandom2 = ArrayUtils.subarray(payload, 0, 16);
                final byte[] signedAuthRandom2 = ArrayUtils.subarray(payload, 16, 48);

                LOG.debug("authRandom2: {}", GB.hexdump(authRandom2));
                LOG.debug("signedAuthRandom2: {}", GB.hexdump(signedAuthRandom2));

                try {
                    // Validate random2 signature
                    final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    sha256.update(authRandom2);
                    sha256.update(authAppSecret);
                    byte[] random2verify = sha256.digest();

                    if (!Arrays.equals(signedAuthRandom2, random2verify)) {
                        LOG.error("random2 signature mismatch");
                        authNegotiationFailed();
                        return;
                    }

                    // Compute K1 and update preferences
                    sha256.reset();
                    sha256.update(authRandom1);
                    sha256.update(authRandom2);
                    sha256.update(authAppSecret);
                    final byte[] k1full = sha256.digest();
                    final byte[] secretKey = ArrayUtils.subarray(k1full, 0, 16);

                    LOG.debug("Negotiated K1: {}", k1full);

                    evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences("authkey", GB.hexdump(secretKey)));

                    characteristicCommandRead.setSessionKey(secretKey);
                    characteristicCommandWrite.setSessionKey(secretKey);
                    characteristicDataRead.setSessionKey(secretKey);
                    characteristicDataWrite.setSessionKey(secretKey);

                    sendCommand("auth step 2", CmfCommand.AUTH_PHONE_NAME, ArrayUtils.addAll(new byte[]{A5}, Build.MODEL.getBytes(StandardCharsets.UTF_8)));
                } catch (final Exception e) {
                    LOG.error("Failed to negotiate K1", e);
                    authNegotiationFailed();
                    return;
                }

                return;
            case AUTH_FAILED:
                LOG.error("Authentication failed, disconnecting");
                GB.toast(getContext(), R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN);
                final GBDevice device = getDevice();
                if (device != null) {
                    GBApplication.deviceService(device).disconnect();
                }
                return;
            case AUTH_WATCH_MAC:
                LOG.debug("Got auth watch mac, requesting nonce");
                sendCommand("auth request nonce", CmfCommand.AUTH_NONCE_REQUEST, A5);
                return;
            case AUTH_NONCE_REPLY:
                LOG.debug("Got auth nonce");

                try {
                    final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    sha256.update(payload);
                    sha256.update(getSecretKey(getDevice()));
                    final byte[] digest = sha256.digest();
                    final byte[] sessionKey = ArrayUtils.subarray(digest, 0, 16);
                    LOG.debug("New session key: {}", GB.hexdump(sessionKey));
                    characteristicCommandRead.setSessionKey(sessionKey);
                    characteristicCommandWrite.setSessionKey(sessionKey);
                    characteristicDataRead.setSessionKey(sessionKey);
                    characteristicDataWrite.setSessionKey(sessionKey);
                    if (characteristicFirmwareRead != null) {
                        characteristicFirmwareRead.setSessionKey(sessionKey);
                    }
                    if (characteristicFirmwareWrite != null) {
                        characteristicFirmwareWrite.setSessionKey(sessionKey);
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to compute session key from auth nonce", e);
                    return;
                }

                sendCommand("auth confirm", CmfCommand.AUTHENTICATED_CONFIRM_REQUEST, A5);
                return;
            case AUTHENTICATED_CONFIRM_REPLY:
                LOG.debug("Authentication confirmed, starting phase 2 initialization");

                final TransactionBuilder phase2builder = createTransactionBuilder("phase 2 initialize");
                setTime(phase2builder);
                sendCommand(phase2builder, CmfCommand.FIRMWARE_VERSION_GET);
                sendCommand(phase2builder, CmfCommand.SERIAL_NUMBER_GET);
                final Location location = new CurrentPosition().getLastKnownLocation();
                if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                    sendGpsCoords(phase2builder, location);
                }
                //sendCommand(phase2builder, CmfCommand.STANDING_REMINDER_GET);
                //sendCommand(phase2builder, CmfCommand.WATER_REMINDER_GET);
                //sendCommand(phase2builder, CmfCommand.CONTACTS_GET);
                //sendCommand(phase2builder, CmfCommand.ALARMS_GET);
                //sendCommand(phase2builder, CmfCommand.CALL_REMINDER_REQUEST, 0x00);
                // TODO premature to mark as initialized?
                phase2builder.setDeviceState(GBDevice.State.INITIALIZED);
                phase2builder.queue();
                return;
            case BATTERY:
                final int battery = payload[0] & 0xff;
                final boolean charging = payload[1] == 0x01;
                LOG.debug("Got battery: level={} charging={}", battery, charging);
                final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
                eventBatteryInfo.level = battery;
                eventBatteryInfo.state = charging ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;
                evaluateGBDeviceEvent(eventBatteryInfo);
                return;
            case FIRMWARE_VERSION_RET:
                final String[] fwParts = new String[payload.length];
                for (int i = 0; i < payload.length; i++) {
                    fwParts[i] = String.valueOf(payload[i]);
                }
                final String fw = String.join(".", fwParts);
                LOG.debug("Got firmware version: {}", fw);
                final GBDeviceEventVersionInfo gbDeviceEventVersionInfo = new GBDeviceEventVersionInfo();
                gbDeviceEventVersionInfo.fwVersion = fw;
                gbDeviceEventVersionInfo.fwVersion2 = "N/A";
                //gbDeviceEventVersionInfo.hwVersion = "?"; // TODO how?
                evaluateGBDeviceEvent(gbDeviceEventVersionInfo);
                return;
            case SERIAL_NUMBER_RET:
                if (payload.length != (payload[0] & 0xff) + 1) {
                    LOG.warn("Unexpected serial number payload length: {}, expected {}", payload.length, (payload[0] & 0xff));
                    return;
                }
                final String serialNumber = new String(ArrayUtils.subarray(payload, 1, payload.length));
                LOG.debug("Got serial number: {}", serialNumber);
                final GBDeviceEventUpdateDeviceInfo gbDeviceEventUpdateDeviceInfo = new GBDeviceEventUpdateDeviceInfo("SERIAL: ", serialNumber);
                evaluateGBDeviceEvent(gbDeviceEventUpdateDeviceInfo);
                return;
            case FIND_PHONE:
                final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
                if (payload[0] == 1) {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                } else {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                }
                evaluateGBDeviceEvent(findPhoneEvent);
                return;
            case CALL_REMINDER_RESPONSE:
                LOG.debug("Got call reminder response: {}", GB.hexdump(payload));
                // 00 -> ack set
                // 01:00 -> disabled
                // 01:01 -> enabled
                break;
            case MUSIC_INFO_ACK:
                LOG.debug("Got music info ack");
                break;
            case MUSIC_BUTTON:
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (BLETypeConversions.toUint16(payload)) {
                    case 0x0003:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    case 0x0103:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 0x0001:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 0x0101:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 0x0102:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 0x0002:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    default:
                        LOG.warn("Unexpected media button key {}", GB.hexdump(payload));
                        return;
                }
                LOG.debug("Got media button {}", deviceEventMusicControl.event);
                evaluateGBDeviceEvent(deviceEventMusicControl);
                break;
            default:
                LOG.warn("Unhandled command: {}", cmd);
        }
    }

    public void sendCommand(final String taskName, final CmfCommand cmd, final byte... payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendCommand(builder, cmd, payload);
        builder.queue();
    }

    public void sendCommand(final TransactionBuilder builder, final CmfCommand cmd, final byte... payload) {
        characteristicCommandWrite.sendCommand(builder, cmd, payload);
    }

    public void sendData(final String taskName, final CmfCommand cmd, final byte... payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        characteristicDataWrite.sendCommand(builder, cmd, payload);
        builder.queue();
    }

    public void sendFirmware(final String taskName, final CmfCommand cmd, final byte... payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        characteristicFirmwareWrite.sendCommand(builder, cmd, payload);
        builder.queue();
    }

    private void handleShellCommand(final byte[] bytes) {
        final String shellCommand = new String(bytes).strip();
        if (!shellCommand.startsWith("GETSECRET:")) {
            LOG.error("Got unknown shell command: {}", GB.hexdump(bytes));
            return;
        }

        if (!shellCommand.endsWith(",OK")) {
            LOG.error("Failed to get secret: {}", GB.hexdump(bytes));
            authNegotiationFailed();
            return;
        }

        final byte[] signedRandom1;
        try {
            new Random().nextBytes(authRandom1);
            final byte[] secretBytes = GB.hexStringToByteArray(shellCommand.substring(10, 10 + 32));
            System.arraycopy(secretBytes, 0, authAppSecret, 0, authAppSecret.length);

            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(authRandom1);
            sha256.update(authAppSecret);
            signedRandom1 = sha256.digest();

            LOG.debug("authRandom1: {}", GB.hexdump(authRandom1));
            LOG.debug("authAppSecret: {}", GB.hexdump(authAppSecret));
            LOG.debug("signedRandom1: {}", GB.hexdump(signedRandom1));
        } catch (final Exception e) {
            LOG.error("Failed to generate signed random1", e);
            authNegotiationFailed();
            return;
        }

        sendCommand("auth send signed random1", CmfCommand.AUTH_PAIR_REQUEST, ArrayUtils.addAll(authRandom1, signedRandom1));
    }

    private void authNegotiationFailed() {
        GB.toast(getContext(), R.string.authentication_failed_negotiation, Toast.LENGTH_LONG, GB.WARN);
        final GBDevice device = getDevice();
        if (device != null) {
            GBApplication.deviceService(device).disconnect();
        }
    }

    @Nullable
    private static byte[] getSecretKey(final GBDevice device) {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", "").trim();
        if (StringUtils.isBlank(authKey)) {
            return null;
        }

        final byte[] authKeyBytes = new byte[16];

        final byte[] srcBytes;
        // Allow both with and without 0x, to avoid user mistakes
        if (authKey.length() == 34 && authKey.startsWith("0x")) {
            srcBytes = GB.hexStringToByteArray(authKey.trim().substring(2));
        } else {
            srcBytes = GB.hexStringToByteArray(authKey.trim());
        }
        System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));

        return authKeyBytes;
    }

    protected CmfWatchProCoordinator getCoordinator() {
        return (CmfWatchProCoordinator) gbDevice.getDeviceCoordinator();
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        final TransactionBuilder builder = createTransactionBuilder("set gps location");
        sendGpsCoords(builder, location);
        builder.queue();
    }

    private void sendGpsCoords(final TransactionBuilder builder, final Location location) {
        final ByteBuffer buf = ByteBuffer.allocate(16)
                .order(ByteOrder.BIG_ENDIAN);

        buf.putInt((int) (location.getTime() / 1000));
        buf.putInt((int) (location.getLatitude() * 10000000));
        buf.putInt((int) (location.getLongitude() * 10000000));

        sendCommand(builder, CmfCommand.GPS_COORDS, buf.array());
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return;
        }

        final String senderOrTitle = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(
                notificationSpec.sender,
                notificationSpec.title
        );

        final String body = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(notificationSpec.body, "");

        final byte[] senderOrTitleBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(senderOrTitle, 20); // TODO confirm max
        final byte[] bodyBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(body, 128); // TODO confirm max

        final ByteBuffer buf = ByteBuffer.allocate(7 + senderOrTitleBytes.length + bodyBytes.length)
                .order(ByteOrder.BIG_ENDIAN);

        buf.put(CmfNotificationIcon.forNotification(notificationSpec).getCode());
        buf.put((byte) 0x00); // ?
        buf.putInt((int) (notificationSpec.when / 1000));
        buf.put((byte) senderOrTitleBytes.length);
        buf.put(senderOrTitleBytes);
        buf.put(bodyBytes);

        sendCommand("send notification", CmfCommand.APP_NOTIFICATION, buf.array());
    }

    @Override
    public void onSetContacts(final ArrayList<? extends Contact> contacts) {
        final ByteBuffer buf = ByteBuffer.allocate(57 * contacts.size()).order(ByteOrder.BIG_ENDIAN);

        for (final Contact contact : contacts) {
            final byte[] nameBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(contact.getName(), 32);
            buf.put(nameBytes);
            buf.put(new byte[32 - nameBytes.length]);

            final byte[] numberBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(contact.getNumber(), 25);
            buf.put(numberBytes);
            buf.put(new byte[25 - numberBytes.length]);
        }

        sendCommand("set contacts", CmfCommand.CONTACTS_SET, ArrayUtils.subarray(buf.array(), 0, buf.position()));
    }

    @Override
    public void onSetTime() {
        final TransactionBuilder builder = createTransactionBuilder("set time");
        setTime(builder);
        builder.queue();
    }

    private void setTime(final TransactionBuilder builder) {
        final Calendar cal = Calendar.getInstance();
        final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        buf.putInt((int) (cal.getTimeInMillis() / 1000));
        buf.putInt(TimeZone.getDefault().getOffset(cal.getTimeInMillis()));
        sendCommand(builder, CmfCommand.TIME, buf.array());
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        final ByteBuffer buf = ByteBuffer.allocate(40 * alarms.size()).order(ByteOrder.BIG_ENDIAN);

        int i = 0;
        for (final Alarm alarm : alarms) {
            if (alarm.getUnused()) {
                continue;
            }

            buf.putInt(alarm.getHour() * 3600 + alarm.getMinute() * 60);
            buf.put((byte) i++);
            buf.put((byte) (alarm.getEnabled() ? 0x01 : 0x00));
            buf.put((byte) alarm.getRepetition());
            buf.put((byte) 0xff); // ?
            buf.put(new byte[24]); // ?

            // alarm labels do not show up on watch, even in official app
            final byte[] labelBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(alarm.getTitle(), 8);
            buf.put(new byte[8 - labelBytes.length]);
            buf.put(labelBytes);
        }

        sendCommand("set alarms", CmfCommand.ALARMS_SET, ArrayUtils.subarray(buf.array(), 0, buf.position()));
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        super.onSetCallState(callSpec); // TODO onSetCallState
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        super.onSetCannedMessages(cannedMessagesSpec); // TODO onSetCannedMessages
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        if (mediaManager.onSetMusicState(stateSpec)) {
            sendMusicStateToDevice();
        }
    }

    @Override
    public void onSetPhoneVolume(final float ignoredVolume) {
        sendMusicStateToDevice();
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        if (mediaManager.onSetMusicInfo(musicSpec)) {
            sendMusicStateToDevice();
        }
    }

    private void sendMusicStateToDevice() {
        final MusicSpec musicSpec = mediaManager.getBufferMusicSpec();
        final MusicStateSpec musicStateSpec = mediaManager.getBufferMusicStateSpec();

        final byte stateByte;
        if (musicSpec == null || musicStateSpec == null) {
            stateByte = 0x00;
        } else if (musicStateSpec.state == MusicStateSpec.STATE_PLAYING) {
            stateByte = 0x02;
        } else {
            stateByte = 0x01;
        }

        final byte[] track;
        final byte[] artist;

        if (musicSpec != null) {
            track = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(musicSpec.track, 63);
            artist = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(musicSpec.artist, 63);
        } else {
            track = new byte[0];
            artist = new byte[0];
        }

        final AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        final ByteBuffer buf = ByteBuffer.allocate(131);
        buf.put(stateByte);
        buf.put((byte) volumeLevel);
        buf.put((byte) volumeMax);
        buf.put(track);
        buf.put(new byte[64 - track.length]);
        buf.put(artist);
        buf.put(new byte[64 - artist.length]);

        sendCommand("set music info", CmfCommand.MUSIC_INFO_SET, buf.array());
    }

    @Override
    public void onInstallApp(final Uri uri, @NonNull final Bundle options) {
        dataUploader.onInstallApp(uri);
    }

    @Override
    public void onAppInfoReq() {
        super.onAppInfoReq(); // TODO onAppInfoReq
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        super.onAppStart(uuid, start); // TODO onAppStart for watchfaces
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        sendCommand("fetch recorded data step 1", CmfCommand.ACTIVITY_FETCH_1, A5);
    }

    @Override
    public void onReset(final int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            sendCommand("factory reset", CmfCommand.FACTORY_RESET, A5);
        } else {
            LOG.warn("Unknown reset flags: {}", String.format("0x%x", flags));
        }
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        preferences.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onSendConfiguration(final String config) {
        preferences.onSendConfiguration(config);
    }

    @Override
    public void onFindDevice(final boolean start) {
        if (!start) {
            return;
        }

        sendCommand("find device", CmfCommand.FIND_WATCH);
    }

    @Override
    public void onSendWeather() {
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            LOG.warn("No weather found in singleton");
            return;
        }
        // TODO consider adjusting the condition code for clear/sunny so "clear" at night doesn't show a sunny icon (perhaps 23 decimal)?
        // Each weather entry takes up 9 bytes
        // There are 7 of those weather entries - 7*9 bytes
        // Then there are 24-hour entries of temp and weather condition (2 bytes each)
        // Then the location name as bytes - allow for 30 bytes, watch auto-scrolls. Pad it to 32 bytes if it supports sunset/sunrise
        // Then finally the sunrise / sunset pairs, for 7 days (7*8)
        final boolean supportsSunriseSunset = getCoordinator().supportsSunriseSunset();
        final int payloadLength = (7 * 9) + (24 * 2) + (supportsSunriseSunset ? 32 : 30) + (supportsSunriseSunset ? 7 * 8 : 0);
        final ByteBuffer buf = ByteBuffer.allocate(payloadLength).order(ByteOrder.BIG_ENDIAN);
        // start with the current day's weather
        buf.put(WeatherMapper.mapToCmfCondition(weatherSpec.getCurrentConditionCode()));
        buf.put((byte) (weatherSpec.getCurrentTemp() - 273 + 100)); // convert Kelvin to C, add 100
        buf.put((byte) (weatherSpec.getTodayMaxTemp() - 273 + 100)); // convert Kelvin to C, add 100
        buf.put((byte) (weatherSpec.getTodayMinTemp() - 273 + 100)); // convert Kelvin to C, add 100
        buf.put((byte) weatherSpec.getCurrentHumidity());
        buf.putShort((short) (weatherSpec.getAirQuality() != null ? weatherSpec.getAirQuality().getAqi() : 0));
        buf.put((byte) weatherSpec.getUvIndex()); // UV index isn't shown. uvi decimal/100, so 0x07 = 700 UVI.
        buf.put((byte) weatherSpec.getWindSpeed()); // isn't shown by watch, unsure of correct units

        // find out how many future days' forecasts are available
        int maxForecastsAvailable = weatherSpec.getForecasts().size();
        // For each day of the forecast
        for (int i = 0; i < 6; i++) {
            if (i < maxForecastsAvailable) {
                WeatherSpec.Daily forecastDay = weatherSpec.getForecasts().get(i);
                buf.put(WeatherMapper.mapToCmfCondition(forecastDay.getConditionCode()));  // weather condition flag
                buf.put((byte) (forecastDay.getMaxTemp() - 273 + 100)); // temp in C (not shown in future days' forecasts)
                buf.put((byte) (forecastDay.getMaxTemp() - 273 + 100)); // max temp in C, + 100
                buf.put((byte) (forecastDay.getMinTemp() - 273 + 100)); // min temp in C, + 100
                buf.put((byte) forecastDay.getHumidity()); // humidity as a %
                buf.putShort((short) (forecastDay.getAirQuality() != null ? forecastDay.getAirQuality().getAqi() : 0));
                buf.put((byte) forecastDay.getUvIndex()); // UV index isn't shown. uvi decimal/100, so 0x07 = 700 UVI.
                buf.put((byte) forecastDay.getWindSpeed()); // isn't shown by watch, unsure of correct units
            } else {
                // we need to provide a dummy forecast as there's no data available
                buf.put((byte) 0x00); // NULL weather condition
                buf.put((byte) 0x01); // -99 C temp temp
                buf.put((byte) 0x01); // -99 C max temp
                buf.put((byte) 0x01); // -99 C min temp
                buf.put((byte) 0x00); // 0 humidity
                buf.putShort((short) 0); // aqi
                buf.put((byte) 0x00); // 0 UV index
                buf.put((byte) 0x00); // 0 wind speed
            }

        }
        // now add the hourly data for today - just condition and temperature
        int maxHourlyForecastsAvailable = weatherSpec.getHourly().size();
        for (int i = 0; i < 24; i++) {
            if (i < maxHourlyForecastsAvailable) {
                WeatherSpec.Hourly forecastHr = weatherSpec.getHourly().get(i);
                buf.put((byte) (forecastHr.getTemp() - 273 + 100)); // temperature
                buf.put((byte) forecastHr.getConditionCode()); // condition
            } else {
                buf.put((byte) (weatherSpec.getCurrentTemp() - 273 + 100)); // assume current temp
                buf.put(WeatherMapper.mapToCmfCondition(weatherSpec.getCurrentConditionCode())); // current condition
            }
        }
        // place name - watch scrolls after ~10 chars. Pad up to 32 bytes.
        final byte[] locationNameBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(weatherSpec.getLocation(), 30);
        buf.put(locationNameBytes);

        // Sunrise / sunset
        if (supportsSunriseSunset) {
            buf.put(new byte[32 - locationNameBytes.length]);

            buf.order(ByteOrder.LITTLE_ENDIAN); // why...
            final Location location = weatherSpec.getLocationObject() != null ? weatherSpec.getLocationObject() : new CurrentPosition().getLastKnownLocation();
            final GregorianCalendar sunriseDate = new GregorianCalendar();

            if (weatherSpec.getSunRise() != 0 && weatherSpec.getSunSet() != 0) {
                buf.putInt(weatherSpec.getSunRise());
                buf.putInt(weatherSpec.getSunSet());
            } else {
                putSunriseSunset(buf, location, sunriseDate);
            }

            for (int i = 0; i < 6; i++) {
                sunriseDate.add(Calendar.DAY_OF_MONTH, 1);
                if (i < weatherSpec.getForecasts().size() && weatherSpec.getForecasts().get(i).getSunRise() != 0 && weatherSpec.getForecasts().get(i).getSunSet() != 0) {
                    buf.putInt(weatherSpec.getForecasts().get(i).getSunRise());
                    buf.putInt(weatherSpec.getForecasts().get(i).getSunSet());
                } else {
                    putSunriseSunset(buf, location, sunriseDate);
                }
            }
        }

        sendCommand("send weather", CmfCommand.WEATHER_SET_1, buf.array());
    }

    private void putSunriseSunset(final ByteBuffer buf, final Location location, final GregorianCalendar date) {
        final SunriseTransitSet sunriseTransitSet = SPA.calculateSunriseTransitSet(
                date.toZonedDateTime(),
                location.getLatitude(),
                location.getLongitude(),
                DeltaT.estimate(date.toZonedDateTime().toLocalDate())
        );

        if (sunriseTransitSet.getSunrise() != null && sunriseTransitSet.getSunset() != null) {
            buf.putInt((int) sunriseTransitSet.getSunrise().toInstant().getEpochSecond());
            buf.putInt((int) sunriseTransitSet.getSunset().toInstant().getEpochSecond());
        } else {
            buf.putInt(0);
            buf.putInt(0);
        }
    }

    @Override
    public void onTestNewFunction() {

    }
}
