/*  Copyright (C) 2019-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Dmitriy Bogdanov, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.PackageConfigHelper;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.WatchAdapterFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.DownloadFileRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.MoveHandsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class QHybridSupport extends QHybridBaseSupport {
    public static final String QHYBRID_COMMAND_CONTROL = "qhybrid_command_control";
    public static final String QHYBRID_COMMAND_UNCONTROL = "qhybrid_command_uncontrol";
    public static final String QHYBRID_COMMAND_SET = "qhybrid_command_set";
    public static final String QHYBRID_COMMAND_MOVE = "qhybrid_command_move";
    public static final String QHYBRID_COMMAND_SAVE_CALIBRATION = "qhybrid_command_save_calibration";
    public static final String QHYBRID_COMMAND_VIBRATE = "qhybrid_command_vibrate";
    public static final String QHYBRID_COMMAND_UPDATE = "qhybrid_command_update";
    public static final String QHYBRID_COMMAND_UPDATE_TIMEZONE = "qhybrid_command_update_timezone";
    public static final String QHYBRID_COMMAND_NOTIFICATION = "qhybrid_command_notification";
    public static final String QHYBRID_COMMAND_UPDATE_SETTINGS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_SETTINGS";
    public static final String QHYBRID_COMMAND_OVERWRITE_BUTTONS = "nodomain.freeyourgadget.gadgetbridge.Q_OVERWRITE_BUTTONS";
    public static final String QHYBRID_COMMAND_UPDATE_WIDGETS = "nodomain.freeyourgadget.gadgetbridge.Q_UPDATE_WIDGETS";
    public static final String QHYBRID_COMMAND_SET_MENU_MESSAGE = "nodomain.freeyourgadget.gadgetbridge.Q_SET_MENU_MESSAGE";
    public static final String QHYBRID_COMMAND_SEND_MENU_ITEMS = "nodomain.freeyourgadget.gadgetbridge.Q_SEND_MENU_ITEMS";
    public static final String QHYBRID_COMMAND_SET_WIDGET_CONTENT = "nodomain.freeyourgadget.gadgetbridge.Q_SET_WIDGET_CONTENT";
    public static final String QHYBRID_COMMAND_SET_BACKGROUND_IMAGE = "nodomain.freeyourgadget.gadgetbridge.Q_SET_BACKGROUND_IMAGE";
    public static final String QHYBRID_COMMAND_UNINSTALL_APP = "nodomain.freeyourgadget.gadgetbridge.Q_UNINSTALL_APP";
    public static final String QHYBRID_COMMAND_PUSH_CONFIG = "nodomain.freeyourgadget.gadgetbridge.Q_PUSH_CONFIG";
    public static final String QHYBRID_COMMAND_SWITCH_WATCHFACE = "nodomain.freeyourgadget.gadgetbridge.Q_SWITCH_WATCHFACE";

    public static final String QHYBRID_COMMAND_DOWNLOAD_FILE = "nodomain.freeyourgadget.gadgetbridge.Q_DOWNLOAD_FILE";
    public static final String QHYBRID_COMMAND_UPLOAD_FILE = "nodomain.freeyourgadget.gadgetbridge.Q_UPLOAD_FILE";

    public static final String QHYBRID_COMMAND_SET_MENU_STRUCTURE = "nodomain.freeyourgadget.gadgetbridge.Q_SET_MENU_STRUCTURE";

    public static final String QHYBRID_ACTION_DOWNLOADED_FILE = "nodomain.freeyourgadget.gadgetbridge.Q_DOWNLOADED_FILE";
    public static final String QHYBRID_ACTION_UPLOADED_FILE = "nodomain.freeyourgadget.gadgetbridge.Q_UPLOADED_FILE";

    private static final String QHYBRID_ACTION_SET_ACTIVITY_HAND = "nodomain.freeyourgadget.gadgetbridge.Q_SET_ACTIVITY_HAND";

    public static final String QHYBRID_EVENT_SETTINGS_UPDATED = "nodomain.freeyourgadget.gadgetbridge.Q_SETTINGS_UPDATED";
    public static final String QHYBRID_EVENT_FILE_UPLOADED = "nodomain.freeyourgadget.gadgetbridge.Q_FILE_UPLOADED";
    public static final String QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED = "nodomain.freeyourgadget.gadgetbridge.Q_NOTIFICATION_CONFIG_CHANGED";

    public static final String QHYBRID_EVENT_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_BUTTON_PRESSED";
    public static final String QHYBRID_EVENT_MULTI_BUTTON_PRESS = "nodomain.freeyourgadget.gadgetbridge.Q_MULTI_BUTTON_PRESSED";
    public static final String QHYBRID_EVENT_COMMUTE_MENU = "nodomain.freeyourgadget.gadgetbridge.Q_COMMUTE_MENU";

    public static final String ITEM_STEP_GOAL = "STEP_GOAL";
    public static final String ITEM_STEP_COUNT = "STEP_COUNT";
    public static final String ITEM_VIBRATION_STRENGTH = "VIBRATION_STRENGTH";
    public static final String ITEM_ACTIVITY_POINT = "ACTIVITY_POINT";
    public static final String ITEM_EXTENDED_VIBRATION_SUPPORT = "EXTENDED_VIBRATION";
    public static final String ITEM_HAS_ACTIVITY_HAND = "HAS_ACTIVITY_HAND";
    public static final String ITEM_USE_ACTIVITY_HAND = "USE_ACTIVITY_HAND";
    public static final String ITEM_LAST_HEARTBEAT = "LAST_HEARTBEAT";
    public static final String ITEM_TIMEZONE_OFFSET = "TIMEZONE_OFFSET_COUNT";
    public static final String ITEM_HEART_RATE_MEASUREMENT_MODE = "HEART_RATE_MEASUREMENT_MODE";

    private static final Logger logger = LoggerFactory.getLogger(QHybridSupport.class);
    private final BroadcastReceiver commandReceiver;
    private final BroadcastReceiver globalCommandReceiver;

    private final PackageConfigHelper helper;

    public volatile boolean searchDevice = false;

    private long timeOffset;

    private boolean useActivityHand;

    private WatchAdapter watchAdapter;

    public QHybridSupport() {
        super(logger);
        addSupportedService(UUID.fromString("108b5094-4c03-e51c-555e-105d1a1155f0"));
        addSupportedService(UUID.fromString("3dda0001-957f-7d4a-34a6-74696673696d"));
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        IntentFilter commandFilter = new IntentFilter(QHYBRID_COMMAND_CONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_UNCONTROL);
        commandFilter.addAction(QHYBRID_COMMAND_SET);
        commandFilter.addAction(QHYBRID_COMMAND_MOVE);
        commandFilter.addAction(QHYBRID_COMMAND_SAVE_CALIBRATION);
        commandFilter.addAction(QHYBRID_COMMAND_VIBRATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_TIMEZONE);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_SETTINGS);
        commandFilter.addAction(QHYBRID_COMMAND_OVERWRITE_BUTTONS);
        commandFilter.addAction(QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED);
        commandFilter.addAction(QHYBRID_COMMAND_UPDATE_WIDGETS);
        commandFilter.addAction(QHYBRID_COMMAND_SEND_MENU_ITEMS);
        commandFilter.addAction(QHYBRID_COMMAND_SET_BACKGROUND_IMAGE);
        commandFilter.addAction(QHYBRID_COMMAND_UNINSTALL_APP);
        commandFilter.addAction(QHYBRID_COMMAND_UPLOAD_FILE);
        commandFilter.addAction(QHYBRID_COMMAND_DOWNLOAD_FILE);
        commandReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                NotificationConfiguration config = extras == null ? null : (NotificationConfiguration) intent.getExtras().get("CONFIG");
                if (intent.getAction() == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case QHYBRID_COMMAND_CONTROL: {
                        log("sending control request");
                        watchAdapter.requestHandsControl();
                        MoveHandsRequest.MovementConfiguration movement = new MoveHandsRequest.MovementConfiguration(false);
                        if (config != null) {
                            if(config.getHour() != -1) movement.setHourDegrees(config.getHour());
                            if(config.getMin() != -1) movement.setMinuteDegrees(config.getMin());
                            if(config.getSubEye() != -1) movement.setSubDegrees(config.getSubEye());
                            watchAdapter.setHands(movement);
                        } else {
                            movement.setHourDegrees(0);
                            movement.setMinuteDegrees(0);
                            movement.setSubDegrees(0);
                            watchAdapter.setHands(movement);
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_UNCONTROL: {
                        watchAdapter.releaseHandsControl();
                        break;
                    }
                    case QHYBRID_COMMAND_SET: {
                        if(config == null) break;
                        MoveHandsRequest.MovementConfiguration movement = new MoveHandsRequest.MovementConfiguration(false);
                        if(config.getHour() != -1) movement.setHourDegrees(config.getHour());
                        if(config.getMin() != -1) movement.setMinuteDegrees(config.getMin());
                        if(config.getSubEye() != -1) movement.setSubDegrees(config.getSubEye());
                        watchAdapter.setHands(movement);
                        break;
                    }
                    case QHYBRID_COMMAND_MOVE: {
                        MoveHandsRequest.MovementConfiguration movement = new MoveHandsRequest.MovementConfiguration(true);
                        if (extras == null) {
                            logger.error("Got QHYBRID_COMMAND_MOVE without extras");
                            break;
                        }
                        if(extras.containsKey("EXTRA_DISTANCE_HOUR")) movement.setHourDegrees(extras.getShort("EXTRA_DISTANCE_HOUR"));
                        if(extras.containsKey("EXTRA_DISTANCE_MINUTE")) movement.setMinuteDegrees(extras.getShort("EXTRA_DISTANCE_MINUTE"));
                        if(extras.containsKey("EXTRA_DISTANCE_SUB")) movement.setSubDegrees(extras.getShort("EXTRA_DISTANCE_SUB"));
                        watchAdapter.setHands(movement);
                        break;
                    }
                    case QHYBRID_COMMAND_SAVE_CALIBRATION: {
                        watchAdapter.saveCalibration();
                        break;
                    }
                    case QHYBRID_COMMAND_VIBRATE: {
                        if (config != null) {
                            watchAdapter.vibrate(config.getVibration());
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION: {
                        if (config != null) {
                            watchAdapter.playNotification(config);
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE: {
                        loadTimeOffset();
                        onSetTime();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_TIMEZONE:{
                        loadTimezoneOffset();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_SETTINGS: {
                        String newSetting = intent.getStringExtra("EXTRA_SETTING");
                        if (newSetting == null) {
                            logger.error("newSetting is null");
                            break;
                        }
                        switch (newSetting) {
                            case ITEM_VIBRATION_STRENGTH: {
                                final ItemWithDetails itemVibrationStrength = gbDevice.getDeviceInfo(ITEM_VIBRATION_STRENGTH);
                                if (itemVibrationStrength == null) {
                                    logger.error("itemVibrationStrength is null");
                                    break;
                                }
                                watchAdapter.setVibrationStrength(Short.parseShort(itemVibrationStrength.getDetails()));
                                break;
                            }
                            case ITEM_STEP_GOAL: {
                                final ItemWithDetails itemStepGoal = gbDevice.getDeviceInfo(ITEM_STEP_GOAL);
                                if (itemStepGoal == null) {
                                    logger.error("itemStepGoal is null");
                                    break;
                                }
                                watchAdapter.setStepGoal(Integer.parseInt(itemStepGoal.getDetails()));
                                break;
                            }
                            case ITEM_USE_ACTIVITY_HAND: {
                                final ItemWithDetails itemUseActivityHand = gbDevice.getDeviceInfo(ITEM_USE_ACTIVITY_HAND);
                                if (itemUseActivityHand == null) {
                                    logger.error("itemUseActivityHand is null");
                                    break;
                                }
                                QHybridSupport.this.useActivityHand = itemUseActivityHand.getDetails().equals("true");
                                GBApplication.getPrefs().getPreferences().edit().putBoolean("QHYBRID_USE_ACTIVITY_HAND", useActivityHand).apply();
                                break;
                            }
                        }

                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(QHYBRID_EVENT_SETTINGS_UPDATED));
                        break;
                    }
                    case QHYBRID_COMMAND_OVERWRITE_BUTTONS: {
                        String buttonConfig = intent.getStringExtra(FossilWatchAdapter.ITEM_BUTTONS);
                        watchAdapter.overwriteButtons(buttonConfig);
                        break;
                    }
                    case QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED: {
                        watchAdapter.syncNotificationSettings();
                        break;
                    }
                    case QHYBRID_COMMAND_UPDATE_WIDGETS: {
                        watchAdapter.updateWidgets();
                        break;
                    }
                    case QHYBRID_COMMAND_SET_BACKGROUND_IMAGE:{
                        byte[] pixels = intent.getByteArrayExtra("EXTRA_PIXELS_ENCODED");
                        watchAdapter.setBackgroundImage(pixels);
                        break;
                    }
                    case QHYBRID_COMMAND_DOWNLOAD_FILE:{
                        byte majorHandle = intent.getByteExtra("EXTRA_MAJORHANDLE", (byte) 0x00);
                        byte minorHandle = intent.getByteExtra("EXTRA_MINORHANDLE", (byte) 0x00);
                        String filename = intent.getStringExtra("EXTRA_NAME");
                        watchAdapter.downloadFile(majorHandle, minorHandle, filename, intent.getBooleanExtra("EXTRA_ENCRYPTED", false), false);
                        break;
                    }
                    case QHYBRID_COMMAND_UPLOAD_FILE:{
                        handleFileUploadIntent(intent);
                        break;
                    }
                    case QHYBRID_COMMAND_UNINSTALL_APP:{
                        watchAdapter.uninstallApp(intent.getStringExtra("EXTRA_APP_NAME"));
                        break;
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(commandReceiver, commandFilter);

        helper = new PackageConfigHelper(GBApplication.getContext());

        IntentFilter globalFilter = new IntentFilter();
        globalFilter.addAction(QHYBRID_ACTION_SET_ACTIVITY_HAND);
        globalFilter.addAction(QHYBRID_COMMAND_SET_MENU_MESSAGE);
        globalFilter.addAction(QHYBRID_COMMAND_SET_WIDGET_CONTENT);
        globalFilter.addAction(QHYBRID_COMMAND_UPLOAD_FILE);
        globalFilter.addAction(QHYBRID_COMMAND_PUSH_CONFIG);
        globalFilter.addAction(QHYBRID_COMMAND_SWITCH_WATCHFACE);
        globalFilter.addAction(QHYBRID_COMMAND_SET_MENU_STRUCTURE);
        globalCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (watchAdapter == null || intent.getAction() == null) {
                    return;
                }

                final Bundle extras = intent.getExtras();
                if (extras == null) {
                    logger.error("Got {} without extras", intent.getAction());
                    return;
                }

                switch (intent.getAction()) {
                    case QHYBRID_ACTION_SET_ACTIVITY_HAND: {
                        try {
                            String extra = String.valueOf(extras.get("EXTRA_PROGRESS"));
                            float progress = Float.parseFloat(extra);
                            watchAdapter.setActivityHand(progress);

                            watchAdapter.playNotification(new NotificationConfiguration(
                                    (short) -1,
                                    (short) -1,
                                    (short) (progress * 180),
                                    PlayNotificationRequest.VibrationType.NO_VIBE
                            ));
                        } catch (Exception e) {
                            logger.error("wrong number format", e);
                            logger.debug("trash extra should be number 0.0-1.0");
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_SET_MENU_MESSAGE: {
                        String message = String.valueOf(extras.get("EXTRA_MESSAGE"));
                        boolean finished = Boolean.parseBoolean(String.valueOf(extras.get("EXTRA_FINISHED")));

                        watchAdapter.setCommuteMenuMessage(message, finished);

                        break;
                    }
                    case QHYBRID_COMMAND_SET_WIDGET_CONTENT: {
                        HashMap<String, String> widgetValues = new HashMap<>();

                        for(String key : extras.keySet()){
                            if(key.matches("^EXTRA_WIDGET_ID_.*$")){
                                widgetValues.put(key.substring(16), String.valueOf(extras.get(key)));
                            }
                        }
                        boolean render = intent.getBooleanExtra("EXTRA_RENDER", true);
                        if (!widgetValues.isEmpty()){
                            Iterator<String> valuesIterator = widgetValues.keySet().iterator();
                            valuesIterator.next();

                            while(valuesIterator.hasNext()){
                                String id = valuesIterator.next();
                                watchAdapter.setWidgetContent(id, widgetValues.get(id), false);
                            }

                            valuesIterator = widgetValues.keySet().iterator();
                            String id = valuesIterator.next();
                            watchAdapter.setWidgetContent(id, widgetValues.get(id), render);
                        }else {
                            String id = String.valueOf(extras.get("EXTRA_WIDGET_ID"));
                            String content = String.valueOf(extras.get("EXTRA_CONTENT"));
                            watchAdapter.setWidgetContent(id, content, render);
                        }
                        break;
                    }
                    case QHYBRID_COMMAND_UPLOAD_FILE:{
                        if(!dangerousIntentsAllowed()) break;
                        handleFileUploadIntent(intent);
                        break;
                    }
                    case QHYBRID_COMMAND_PUSH_CONFIG:{
                        handleConfigSetIntent(extras);
                        break;
                    }
                    case QHYBRID_COMMAND_SWITCH_WATCHFACE:{
                        handleSwitchWatchfaceIntent(extras);
                        break;
                    }
                    case QHYBRID_COMMAND_SET_MENU_STRUCTURE:{
                        handleSetMenuStructure(intent);
                        break;
                    }
                }
            }
        };
        ContextCompat.registerReceiver(GBApplication.getContext(), globalCommandReceiver, globalFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    private void handleConfigSetIntent(Bundle extras) {
        String configJson = extras.getString("EXTRA_CONFIG_JSON", "{}");
        watchAdapter.pushConfigJson(configJson);
    }

    private void handleSwitchWatchfaceIntent(Bundle extras) {
        String watchfaceName = extras.getString("WATCHFACE_NAME", "");
        if (!StringUtils.isBlank(watchfaceName)) {
            ((FossilHRWatchAdapter) watchAdapter).activateWatchface(watchfaceName);
        }
    }

    private void handleSetMenuStructure(Intent intent){
        if(intent == null){
            logger.error("intent null");
            return;
        }
        String menuStructureJson = intent.getStringExtra("EXTRA_MENU_STRUCTURE_JSON");
        if(menuStructureJson == null){
            logger.error("Menu structure json null");
            return;
        }
        if(menuStructureJson.isEmpty()){
            logger.error("Menu structure json empty");
            return;
        }
        try {
            JSONObject menuStructure = new JSONObject(menuStructureJson);
            watchAdapter.handleSetMenuStructure(menuStructure);
            GB.toast(getContext().getString(R.string.info_menu_structure_set), Toast.LENGTH_SHORT, GB.INFO);
        } catch (JSONException e) {
            logger.error("Menu structure json empty");
            GB.toast(getContext().getString(R.string.error_invalid_menu_structure), Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    private boolean dangerousIntentsAllowed(){
        return GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS, true);
    }

    private void handleFileUploadIntent(Intent intent){
        boolean generateHeader = intent.getBooleanExtra("EXTRA_GENERATE_FILE_HEADER", false);
        String filePath = intent.getStringExtra("EXTRA_PATH");
        if(!generateHeader){
            watchAdapter.uploadFileIncludesHeader(filePath);
            return;
        }
        Object handleObject = intent.getSerializableExtra("EXTRA_HANDLE");
        if(handleObject == null)return;
        if(handleObject instanceof String){
            handleObject = FileHandle.fromName((String)handleObject);
        }
        if(!(handleObject instanceof FileHandle handle)) return;
        watchAdapter.uploadFileGenerateHeader(handle, filePath, intent.getBooleanExtra("EXTRA_ENCRYPTED", false));
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        super.onSetCallState(callSpec);
        watchAdapter.onSetCallState(callSpec);
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
            GBApplication.getContext().unregisterReceiver(globalCommandReceiver);
            if (watchAdapter != null) {
                watchAdapter.dispose();
            }
            super.dispose();
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        super.onSetAlarms(alarms);
        if(this.watchAdapter == null){
            GB.toast("watch not connected", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        this.watchAdapter.onSetAlarms(alarms);
    }

    @Override
    public void onSendConfiguration(String config) {
        if (watchAdapter != null) {
            watchAdapter.onSendConfiguration(config);
        }
    }
    private void loadTimeOffset() {
        timeOffset = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIME_OFFSET", 0);
    }

    private void loadTimezoneOffset(){
        short offset = (short) getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getInt("QHYBRID_TIMEZONE_OFFSET", 0);

        this.watchAdapter.setTimezoneOffsetMinutes(offset);
    }

    public long getTimeOffset(){
        return this.timeOffset;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        this.useActivityHand = GBApplication.getPrefs().getBoolean("QHYBRID_USE_ACTIVITY_HAND", false);
        getDevice().addDeviceInfo(new GenericItem(ITEM_USE_ACTIVITY_HAND, String.valueOf(this.useActivityHand)));

        if (GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_USE_CUSTOM_DEVICEICON, true)) {
            getDevice().setNotificationIconConnected(R.drawable.ic_notification_qhybrid);
            getDevice().setNotificationIconDisconnected(R.drawable.ic_notification_disconnected_qhybrid);
        }

        for (int i = 2; i <= 7; i++)
            builder.notify(UUID.fromString("3dda000" + i + "-957f-7d4a-34a6-74696673696d"), true);

        builder.notify(UUID.fromString("010541ae-efe8-11c0-91c0-105d1a1155f0"), true);
        builder.notify(UUID.fromString("fef9589f-9c21-4d19-9fc0-105d1a1155f0"), true);
        builder.notify(UUID.fromString("842d2791-0d20-4ce4-1ada-105d1a1155f0"), true);

        builder
                .read(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"))
                .read(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"))
                .read(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"))
                // .notify(getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")), true)
        ;


        loadTimeOffset();

        return builder;
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        super.onSetMusicInfo(musicSpec);

        try {
            watchAdapter.setMusicInfo(musicSpec);
        }catch (Exception e){
            logger.error("setMusicInfo error", e);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        super.onSetMusicState(stateSpec);

        watchAdapter.setMusicState(stateSpec);
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), "", true, 0, getContext());
            getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
            getDevice().sendDeviceUpdateIntent(getContext());
            this.watchAdapter.onFetchActivityData();
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        log("notif from " + notificationSpec.sourceAppId + "  " + notificationSpec.sender + "   " + notificationSpec.phoneNumber);
        //new Exception().printStackTrace();

        if(this.watchAdapter instanceof FossilHRWatchAdapter){
            if(((FossilHRWatchAdapter) watchAdapter).playRawNotification(notificationSpec)) return;
        }

        String packageName = notificationSpec.sourceName;

        NotificationConfiguration config = null;
        try {
            config = helper.getNotificationConfiguration(packageName);
        } catch (Exception e) {
            GB.toast("error getting notification configuration", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        if (config == null) return;

        log("handling notification");

        if (config.getRespectSilentMode()) {
            int mode = ((AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
            if (mode == AudioManager.RINGER_MODE_SILENT) return;
        }

        boolean enforceActivityHandNotification = config.getHour() == -1 && config.getMin() == -1;

        playNotification(config);

        showNotificationsByAllActive(enforceActivityHandNotification);
    }

    private void log(String message){
        logger.debug(message);
    }

    @Override
    public String customStringFilter(String inputString) {
        // Remove Unicode code points that can't be displayed by the watch like emoji skin tones and variation selectors
        // \x{1f3fb}-\x{1f3ff} = Emoji skin tones: https://en.wikipedia.org/wiki/Emoticons_(Unicode_block)
        // \ufe00-\ufe0f = Variation selectors: https://en.wikipedia.org/wiki/Variation_Selectors_(Unicode_block)
        return inputString.replaceAll("[\\x{1f3fb}-\\x{1f3ff}|\\ufe00-\\ufe0f]", "");
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        this.watchAdapter.onDeleteNotification(id);

        showNotificationsByAllActive(true);
    }

    private void showNotificationsByAllActive(boolean enforceByNotification) {
        if (!this.useActivityHand) return;
        double progress = calculateNotificationProgress();
        showNotificationCountOnActivityHand(progress);

        if (enforceByNotification) {
            watchAdapter.playNotification(new NotificationConfiguration(
                    (short) -1,
                    (short) -1,
                    (short) (progress * 180),
                    PlayNotificationRequest.VibrationType.NO_VIBE
            ));
        }
    }

    @Override
    public void onReset(int flags) {
        super.onReset(flags);
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            this.watchAdapter.factoryReset();
        }
    }

    public double calculateNotificationProgress() {
        HashMap<NotificationConfiguration, Boolean> configs = new HashMap<>(0);
        try {
            for (NotificationConfiguration config : helper.getNotificationConfigurations()) {
                configs.put(config, false);
            }
        } catch (Exception e) {
            GB.toast("error getting notification configs", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        double notificationProgress = 0;

        for (String notificationPackage : NotificationListener.notificationStack) {
            for (NotificationConfiguration notificationConfiguration : configs.keySet()) {
                if (Boolean.TRUE.equals(configs.get(notificationConfiguration))) continue;
                if (notificationConfiguration.getPackageName().equals(notificationPackage)) {
                    notificationProgress += 0.25;
                    configs.put(notificationConfiguration, true);
                }
            }
        }

        return notificationProgress;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (watchAdapter != null)
            watchAdapter.onConnectionStateChange(gatt, status, newState);
    }

    //TODO toggle "Notifications when screen on" options on this check
    private void showNotificationCountOnActivityHand(double progress) {
        if (useActivityHand) {
            watchAdapter.setActivityHand(progress);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }
        if(watchAdapter == null) return;
        watchAdapter.onMtuChanged(gatt, mtu, status);
    }

    private void playNotification(NotificationConfiguration config) {
        if (config.getMin() == -1 && config.getHour() == -1 && config.getVibration() == PlayNotificationRequest.VibrationType.NO_VIBE)
            return;
        watchAdapter.playNotification(config);
    }

    @Override
    public void onSetTime() {
        watchAdapter.setTime();
    }

    @Override
    public void onFindDevice(boolean start) {
        watchAdapter.onFindDevice(start);
    }

    @Override
    public void onSendWeather() {
        final WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            return;
        }
        watchAdapter.onSendWeather(weatherSpec);
    }

    @Override
    public void onTestNewFunction() {
        watchAdapter.onTestNewFunction();
    }

    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {
        watchAdapter.onInstallApp(uri);
    }

    private void backupFile(DownloadFileRequest request) {
        try {
            File file = FileUtils.getExternalFile("qFiles/" + request.timeStamp);
            if (file.exists()) {
                throw new Exception("file " + file.getPath() + " exists");
            }
            logger.debug("Writing file {}", file.getPath());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(request.file);
            }
            logger.debug("file written.");

            file = FileUtils.getExternalFile("qFiles/steps");
            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                fos.write(("file " + request.timeStamp + " cut\n\n").getBytes());
            }

            //TODO file stuff
            // queueWrite(new EraseFileRequest((short) request.fileHandle));
        } catch (Exception e) {
            logger.error("error", e);
            if (request.fileHandle > 257) {
                // queueWrite(new DownloadFileRequest((short) (request.fileHandle - 1)));
            }
        }
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        watchAdapter.onCharacteristicWrite(gatt, characteristic, status);
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        switch (characteristic.getUuid().toString()) {
            case "00002a26-0000-1000-8000-00805f9b34fb": {
                String firmwareVersion = BLETypeConversions.getStringValue(value, 0);
                gbDevice.setFirmwareVersion(firmwareVersion);

                Matcher matcher = Pattern
                        .compile("(?<=[A-Z]{2}[0-9]\\.[0-9]\\.)[0-9]+\\.[0-9]+")
                        .matcher(firmwareVersion);
                if (matcher.find()){
                    gbDevice.setFirmwareVersion2(matcher.group());
                }

                this.watchAdapter = new WatchAdapterFactory().createWatchAdapter(firmwareVersion, this);
                this.watchAdapter.initialize();
                showNotificationsByAllActive(false);
                break;
            }
            case "00002a24-0000-1000-8000-00805f9b34fb": {
                String modelNumber = BLETypeConversions.getStringValue(value, 0);
                gbDevice.setModel(modelNumber);
                gbDevice.setName(watchAdapter.getModelName());
                try {
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, String.valueOf(watchAdapter.supportsExtendedVibration())));
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_HAS_ACTIVITY_HAND, String.valueOf(watchAdapter.supportsActivityHand())));
                } catch (UnsupportedOperationException e) {
                    logger.error("error", e);
                    gbDevice.addDeviceInfo(new GenericItem(ITEM_EXTENDED_VIBRATION_SUPPORT, "false"));
                }
                break;
            }
            case "00002a19-0000-1000-8000-00805f9b34fb": {
                short level = value[0];
                gbDevice.setBatteryLevel(level);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = gbDevice.getBatteryLevel();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                handleGBDeviceEvent(batteryInfo);
                break;

            }
        }

        return true;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
            characteristic, byte[] value) {
        if(watchAdapter == null) return super.onCharacteristicChanged(gatt, characteristic, value);
        return watchAdapter.onCharacteristicChanged(gatt, characteristic, value);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        if(this.watchAdapter instanceof FossilHRWatchAdapter){
            ((FossilHRWatchAdapter) watchAdapter).setQuickRepliesConfiguration();
        }
    }

    @Override
    public void onAppInfoReq() {
        if(this.watchAdapter instanceof FossilHRWatchAdapter){
            ((FossilHRWatchAdapter) watchAdapter).listApplications();
        }
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        if(this.watchAdapter instanceof FossilHRWatchAdapter) {
            String appName = ((FossilHRWatchAdapter) watchAdapter).getInstalledAppNameFromUUID(uuid);
            if ((appName != null) && (appName.endsWith("App"))) {
                ((FossilHRWatchAdapter) watchAdapter).startAppOnWatch(appName);
            } else {
                ((FossilHRWatchAdapter) watchAdapter).activateWatchface(appName);
            }
        }
    }

    @Override
    public void onAppDownload(UUID uuid) {
        ((FossilHRWatchAdapter) watchAdapter).downloadAppToCache(uuid);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        if(this.watchAdapter instanceof FossilHRWatchAdapter) {
            String appName = ((FossilHRWatchAdapter) watchAdapter).getInstalledAppNameFromUUID(uuid);
            if (appName != null) {
                watchAdapter.uninstallApp(appName);
            }
        }
    }

    @Override
    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {
        ((FossilHRWatchAdapter) watchAdapter).onSetNavigationInfo(navigationInfoSpec);
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        ((FossilHRWatchAdapter) watchAdapter).onSendCalendar();
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        ((FossilHRWatchAdapter) watchAdapter).onSendCalendar();
    }
}
