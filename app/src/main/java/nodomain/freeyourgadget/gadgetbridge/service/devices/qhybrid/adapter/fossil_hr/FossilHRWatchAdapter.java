/*  Copyright (C) 2019-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Enrico Brambilla, Hasan Ammar, José Rebelo, Morten
    Rieger Hannemose, mvn23, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CALENDAR_MAX_DESC_LENGTH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CALENDAR_MAX_TITLE_LENGTH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_EVENTS_AMOUNT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CALENDAR_TARGET_APP;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.FitnessConfigItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.InactivityWarningItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.UnitsConfigItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.VibrationStrengthConfigItem;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_PHONE_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest.MUSIC_WATCH_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil.convertDrawableToBitmap;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID;
import static nodomain.freeyourgadget.gadgetbridge.util.StringUtils.shortenPackageName;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseTransitSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.CommuteActionsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.FossilFileReader;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.FossilHRInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HybridHRActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.HybridHRSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityEntry;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityFileParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.RequestMtuRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.SetDeviceStateRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileDeleteRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRawRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRawRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.DismissTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayCallNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification.PlayTextNotificationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.alexa.AlexaMessageSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application.ApplicationInformation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application.ApplicationsListRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.async.ConfirmAppStatusRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.CheckDeviceNeedsConfirmationRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.CheckDevicePairingRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.ConfirmOnDeviceRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.PerformDevicePairingRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication.VerifyPrivateKeyRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfiguration;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons.ButtonConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.commute.CommuteConfigPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedInterface;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.AssetImageFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImagesSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.menu.SetCommuteMenuMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicControlRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music.MusicInfoSetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationFilterPutHRRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationImage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification.NotificationImagePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply.QuickReplyConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply.QuickReplyConfirmationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.theme.SelectedThemePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomBackgroundWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomTextWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.CustomWidgetElement;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.Widget;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget.WidgetsPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.workout.WorkoutRequestHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.FactoryResetRequest;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.Version;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;

public class FossilHRWatchAdapter extends FossilWatchAdapter {
    public static final int MESSAGE_WHAT_VOICE_DATA_RECEIVED = 0;

    private byte[] phoneRandomNumber;
    private byte[] watchRandomNumber;

    private static ArrayList<Widget> widgets = new ArrayList<>();

    private NotificationHRConfiguration[] notificationConfigurations;

    private CallSpec currentCallSpec = null;
    private MusicSpec currentSpec = null;

    private byte jsonIndex = 0;

    private AssetImage backGroundImage = null;

    public FossilHRWatchAdapter(QHybridSupport deviceSupport) {
        super(deviceSupport);
    }

    private boolean saveRawActivityFiles = false;
    private boolean notifiedAboutMissingNavigationApp = false;

    HashMap<String, Bitmap> appIconCache = new HashMap<>();
    String lastPostedApp = null;

    List<ApplicationInformation> installedApplications = new ArrayList();

    Messenger voiceMessenger = null;

    private Version cleanFirmwareVersion = null;

    private final Set<CalendarEvent> lastSync = new HashSet<>();

    ServiceConnection voiceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOG.info("attached to voice service");
            voiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOG.info("detached from voice service");
            voiceMessenger = null;
        }
    };

    enum CONNECTION_MODE {
        NOT_INITIALIZED,
        AUTHENTICATED,
        NOT_AUTHENTICATED
    }

    CONNECTION_MODE connectionMode = CONNECTION_MODE.NOT_INITIALIZED;

    @Override
    public void initialize() {
        timeoutThread.start();

        saveRawActivityFiles = getDeviceSpecificPreferences().getBoolean("save_raw_activity_files", false);

        queueWrite(new RequestMtuRequest(512));

        listApplications();
        getDeviceInfos();
    }

    @Override
    protected void initializeWithSupportedFileVersions() {
        if (getDeviceSupport().getDevice().getFirmwareVersion().contains("prod")) {
            GB.toast("Dummy FW, skipping initialization", Toast.LENGTH_LONG, GB.INFO);
            queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED), false);
            return;
        }

        queueWrite(new SetDeviceStateRequest(GBDevice.State.AUTHENTICATING));

        negotiateSymmetricKey();
    }

    public void listApplications() {
        queueWrite(new ApplicationsListRequest(this){
            @Override
            public void handleApplicationsList(List<ApplicationInformation> installedApplications) {
                ((FossilHRWatchAdapter) getAdapter()).setInstalledApplications(installedApplications);
                GBDevice device = getAdapter().getDeviceSupport().getDevice();
                JSONArray array = new JSONArray();
                for(ApplicationInformation info : installedApplications){
                    array.put(info.getAppName());
                }
                device.addDeviceInfo(new GenericItem("INSTALLED_APPS", array.toString()));
                device.sendDeviceUpdateIntent(getAdapter().getContext());
            }
        });
    }

    private void initializeAfterAuthentication(boolean authenticated) {
        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZING));

        if (!authenticated) {
            GB.toast(getContext().getString(R.string.fossil_hr_auth_failed), Toast.LENGTH_LONG, GB.ERROR);
            initializeAfterWatchConfirmation(false);
            return;
        }
        boolean versionSupportsConfirmation = getCleanFWVersion().greaterOrEqualThan(new Version("2.22"));
        if(!versionSupportsConfirmation){
            initializeAfterWatchConfirmation(true);
            return;
        }
        boolean shouldAuthenticateOnWatch = getDeviceSpecificPreferences().getBoolean("enable_on_device_confirmation", true);
        if (!shouldAuthenticateOnWatch) {
            GB.toast(getContext().getString(R.string.fossil_hr_confirmation_skipped), Toast.LENGTH_SHORT, GB.INFO);
            initializeAfterWatchConfirmation(false);
            return;
        }
        confirmOnWatch();
    }

    TimerTask confirmTimeoutRunnable = new TimerTask() {
        @Override
        public void run() {
            if (!(fossilRequest instanceof ConfirmOnDeviceRequest)) {
                return;
            }
            GB.toast(getContext().getString(R.string.fossil_hr_confirmation_timeout), Toast.LENGTH_SHORT, GB.INFO);
            ((ConfirmOnDeviceRequest) fossilRequest).onResult(false);
        }
    };

    private void confirmOnWatch() {
        queueWrite(new CheckDeviceNeedsConfirmationRequest() {
            @Override
            public void onResult(boolean needsConfirmation) {
                LOG.info("needs confirmation: {}", needsConfirmation);
                if (needsConfirmation) {
                    final Timer timer = new Timer();
                    GB.toast(getContext().getString(R.string.fossil_hr_confirm_connection), Toast.LENGTH_SHORT, GB.INFO);
                    queueWrite(new ConfirmOnDeviceRequest() {
                        @Override
                        public void onResult(boolean confirmationSuccess) {
                            isFinished = true;
                            timer.cancel();
                            if (confirmationSuccess) {
                                pairToWatch();
                            } else {
                                GB.toast(getContext().getString(R.string.fossil_hr_connection_not_confirmed), Toast.LENGTH_LONG, GB.ERROR);
                            }
                            initializeAfterWatchConfirmation(confirmationSuccess);
                        }
                    }, true);
                    timer.schedule(confirmTimeoutRunnable, 30000);
                } else {
                    initializeAfterWatchConfirmation(true);
                }
            }
        });
    }

    private void pairToWatch() {
        queueWrite(new CheckDevicePairingRequest() {
            @Override
            public void onResult(boolean pairingStatus) {
                LOG.info("watch pairing status: {}", pairingStatus);
                if (!pairingStatus) {
                    queueWrite(new PerformDevicePairingRequest() {
                        @Override
                        public void onResult(boolean pairingSuccess) {
                            isFinished = true;
                            LOG.info("watch pairing result: {}", pairingSuccess);
                            if (pairingSuccess) {
                                GB.toast(getContext().getString(R.string.fossil_hr_pairing_successful), Toast.LENGTH_LONG, GB.ERROR);
                            } else {
                                GB.toast(getContext().getString(R.string.fossil_hr_pairing_failed), Toast.LENGTH_LONG, GB.ERROR);
                            }
                        }
                    }, true);
                }
            }
        });
    }

    private void respondToAlexa(String message, boolean isResponse){
        queueWrite(new AlexaMessageSetRequest(message, isResponse, this));
    }

    private void attachToVoiceService(){
        String servicePackage = getDeviceSpecificPreferences().getString("voice_service_package", "");
        String servicePath = getDeviceSpecificPreferences().getString("voice_service_class", "");

        if(servicePackage.isEmpty()){
            GB.toast("voice service package is not configured", Toast.LENGTH_LONG, GB.ERROR);
            respondToAlexa("voice service package not configured on phone", true);
            return;
        }

        if(servicePath.isEmpty()){
            respondToAlexa("voice service class not configured on phone", true);
            GB.toast("voice service class is not configured", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        ComponentName component = new ComponentName(servicePackage, servicePath);

        // extract to somewhere
        Intent voiceIntent = new Intent("nodomain.freeyourgadget.gadgetbridge.VOICE_COMMAND");
        voiceIntent.setComponent(component);

        int flags = 0;

        flags |= Service.BIND_AUTO_CREATE;

        LOG.info("binding to voice service...");

        getContext().bindService(
                voiceIntent,
                voiceServiceConnection,
                flags
        );

        PackageManager pm = getContext().getPackageManager();
        boolean serviceEnabled = true;
        try {
            int enabledState = pm.getComponentEnabledSetting(component);

            if(enabledState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && enabledState != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT){
                respondToAlexa("voice service is disabled on phone", true);
                GB.toast("voice service is disabled", Toast.LENGTH_LONG, GB.ERROR);
                serviceEnabled = false;
            }
        }catch (IllegalArgumentException e){
            serviceEnabled = false;
            respondToAlexa("voice service not found on phone", true);
            GB.toast("voice service not found", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        if(!serviceEnabled){
            detachFromVoiceService();
        }
    }

    private void detachFromVoiceService(){
        getContext().unbindService(voiceServiceConnection);
    }

    private void handleVoiceStatus(byte status){
        if(status == 0x00){
            attachToVoiceService();
        }else if(status == 0x01){
            detachFromVoiceService();
        }
    }

    private void handleVoiceStatusCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value){
        handleVoiceStatus(value[0]);
    }

    private void handleVoiceDataCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value){
        if(voiceMessenger == null){
            return;
        }
        Message message = Message.obtain(
                null,
                MESSAGE_WHAT_VOICE_DATA_RECEIVED
        );
        Bundle dataBundle = new Bundle(1);
        dataBundle.putByteArray("VOICE_DATA", value);
        dataBundle.putString("VOICE_ENCODING", "OPUS");
        message.setData(dataBundle);
        try {
            voiceMessenger.send(message);
        } catch (RemoteException e) {
            LOG.error("error sending voice data to service", e);
            GB.toast("error sending voice data to service", Toast.LENGTH_LONG, GB.ERROR);
            voiceMessenger = null;
            detachFromVoiceService();
            respondToAlexa("error sending voice data to service", true);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        switch (characteristic.getUuid().toString()){
            case "010541ae-efe8-11c0-91c0-105d1a1155f0":
                handleVoiceStatusCharacteristic(characteristic, value);
                return true;
            case "842d2791-0d20-4ce4-1ada-105d1a1155f0":
                handleVoiceDataCharacteristic(characteristic, value);
                return true;
        }
        return super.onCharacteristicChanged(gatt, characteristic, value);
    }

    private void initializeAfterWatchConfirmation(boolean authenticated) {
        setNotificationConfigurations();
        setQuickRepliesConfiguration();

        if (authenticated) {
            // setVibrationStrengthFromConfig();
            setUnitsConfig();
            syncSettings();
            setTime();
        }

        overwriteButtons(null);
        loadBackground();
        loadWidgets();
        // renderWidgets();
        // dunno if there is any point in doing this at start since when no watch is connected the QHybridSupport will not receive any intents anyway

        updateBuiltinAppsInCache();

        onSendCalendar();

        queueWrite(new SetDeviceStateRequest(GBDevice.State.INITIALIZED));
    }

    private void handleAuthenticationResult(boolean success) {
        if (this.connectionMode != CONNECTION_MODE.NOT_INITIALIZED) return;
        this.connectionMode = success ? CONNECTION_MODE.AUTHENTICATED : CONNECTION_MODE.NOT_AUTHENTICATED;
        this.initializeAfterAuthentication(success);
    }

    @Override
    public void uninstallApp(String appName) {
        for (ApplicationInformation appInfo : this.installedApplications) {
            if (appInfo.getAppName().equals(appName)) {
                byte handle = appInfo.getFileHandle();
                short fullFileHandle = (short) ((FileHandle.APP_CODE.getMajorHandle()) << 8 | handle);
                queueWrite(new FileDeleteRequest(fullFileHandle));
                listApplications();
                break;
            }
        }
    }

    public void activateWatchface(String appName) {
        queueWrite(new SelectedThemePutRequest(this, appName));
    }

    public void startAppOnWatch(String appName) {
        try {
            JSONObject jsonObject = new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("customWatchFace._.config.start_app", appName)
                            )
                    );
            queueWrite(new JsonPutRequest(jsonObject, this));
        } catch (JSONException e) {
            LOG.warn("Couldn't start app on watch: " + e.getMessage());
        }
    }

    public void downloadAppToCache(UUID uuid) {
        LOG.info("Going to download app with UUID " + uuid.toString());
        for (ApplicationInformation appInfo : installedApplications) {
            if (UUID.nameUUIDFromBytes(appInfo.getAppName().getBytes(StandardCharsets.UTF_8)).equals(uuid)) {
                LOG.info("Going to download app with name " + appInfo.getAppName() + " and handle " + appInfo.getFileHandle());
                downloadFile(FileHandle.APP_CODE.getMajorHandle(), appInfo.getFileHandle(), appInfo.getAppName(), false, true);
            }
        }
    }

    private void setVibrationStrengthFromConfig() {
        Prefs prefs = new Prefs(getDeviceSpecificPreferences());
        int vibrationStrengh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE, 2);
        if (vibrationStrengh > 0) {
            vibrationStrengh = (vibrationStrengh + 1) * 25; // Seems 0,50,75,100 are working...
        }
        setVibrationStrength((short) (vibrationStrengh));
    }

    private void setUnitsConfig() {
        Prefs prefs = GBApplication.getPrefs();
        String unit = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        int value = 8; // dont know what this bit means but it was set for me before tampering
        if (!unit.equals("metric")) {
            value |= (4 | 1); // temperature and distance
        }
        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(new UnitsConfigItem(value), this)
        );

    }

    @Override
    public void setVibrationStrength(short strength) {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(new VibrationStrengthConfigItem((byte) strength), this)
        );
    }

    private void setNotificationConfigurations() {
        // Set default icons
        ArrayList<NotificationImage> images = new ArrayList<>();
        images.add(new NotificationImage("icIncomingCall.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_phone_outline)), 24, 24));
        images.add(new NotificationImage("icMissedCall.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_phone_missed_outline)), 24, 24));
        images.add(new NotificationImage("icMessage.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_message_outline)), 24, 24));
        images.add(new NotificationImage("general_white.bin", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_alert_circle_outline)), 24, 24));

        // Set default notification filters
        ArrayList<NotificationHRConfiguration> notificationFilters = new ArrayList<>();
        notificationFilters.add(new NotificationHRConfiguration("generic", "general_white.bin"));
        notificationFilters.add(new NotificationHRConfiguration("call", new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x59, (byte) 0xB7}, "icIncomingCall.icon"));

        // Add icons and notification filters from cached past notifications
        Set<Map.Entry<String, Bitmap>> entrySet = this.appIconCache.entrySet();
        for (Map.Entry<String, Bitmap> entry : entrySet) {
            String iconName = shortenPackageName(entry.getKey()) + ".icon";
            images.add(new NotificationImage(iconName, entry.getValue()));
            notificationFilters.add(new NotificationHRConfiguration(entry.getKey(), iconName));
        }

        // Send notification icons
        try {
            queueWrite(new NotificationImagePutRequest(images.toArray(new NotificationImage[images.size()]), this));
        } catch (IOException e) {
            LOG.error("Error while sending notification icons", e);
        }

        // Send notification filters configuration
        this.notificationConfigurations = notificationFilters.toArray(new NotificationHRConfiguration[notificationFilters.size()]);
        queueWrite(new NotificationFilterPutHRRequest(this.notificationConfigurations, this));
    }

    private String[] getQuickReplies() {
        ArrayList<String> configuredReplies = new ArrayList<>();
        Prefs prefs = new Prefs(getDeviceSpecificPreferences());
        for (int i = 1; i <= 16; i++) {
            String quickReply = prefs.getString("canned_message_dismisscall_" + i, null);
            if (quickReply != null) {
                configuredReplies.add(quickReply);
            }
        }
        return configuredReplies.toArray(new String[0]);
    }

    public void setQuickRepliesConfiguration() {
        String[] quickReplies = getQuickReplies();
        if (quickReplies.length > 0) {
            NotificationImage quickReplyIcon = new NotificationImage("icMessage.icon", NotificationImage.getEncodedIconFromDrawable(getContext().getResources().getDrawable(R.drawable.ic_message_outline)), 24, 24);
            queueWrite(new NotificationImagePutRequest(quickReplyIcon, this));
            queueWrite(new QuickReplyConfigurationPutRequest(quickReplies, this));
        }
    }

    private File getBackgroundFile() {
        return new File(getContext().getExternalFilesDir(null), "hr_background.bin");
    }

    private void loadBackground() {
        this.backGroundImage = null;
        try {
            FileInputStream fis = new FileInputStream(getBackgroundFile());
            int count = fis.available();
            if (count != 14400) {
                throw new RuntimeException("wrong background file length");
            }
            byte[] file = new byte[14400];
            fis.read(file);
            fis.close();
            this.backGroundImage = AssetImageFactory.createAssetImage(file, 0, 0, 0);
        } catch (FileNotFoundException e) {
            SharedPreferences preferences = getDeviceSpecificPreferences();
            if (preferences.getBoolean("force_white_color_scheme", false)) {
                Bitmap whiteBitmap = Bitmap.createBitmap(239, 239, Bitmap.Config.ARGB_8888);
                new Canvas(whiteBitmap).drawColor(Color.WHITE);

                try {
                    this.backGroundImage = AssetImageFactory.createAssetImage(whiteBitmap, true, 0, 1, 0);
                } catch (IOException e2) {
                    LOG.error("Backgroundimage error", e2);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.error("error opening background file", e);
            GB.toast("error opening background file", Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void setBackgroundImage(byte[] pixels) {
        if (pixels == null) {
            getBackgroundFile().delete();
            loadBackground(); // recreates the white background in force-white mode, else backgroundImage=null
        } else {
            this.backGroundImage = AssetImageFactory.createAssetImage(pixels, 0, 0, 0);
            try {
                FileOutputStream fos = new FileOutputStream(getBackgroundFile(), false);
                fos.write(pixels);
            } catch (IOException e) {
                LOG.error("error saving background", e);
                GB.toast("error persistent saving background", Toast.LENGTH_LONG, GB.ERROR);
            }
        }
        renderWidgets();
    }

    private void loadWidgets() {
        Version firmwareVersion = getCleanFWVersion();
        if (firmwareVersion != null && firmwareVersion.greaterOrEqualThan(new Version("2.20"))) {
            return; // this does not work on newer firmware versions
        }
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress()));
        boolean forceWhiteBackground = prefs.getBoolean("force_white_color_scheme", false);
        String fontColor = forceWhiteBackground ? "black" : "default";

        Widget[] oldWidgets = widgets.toArray(new Widget[0]);

        widgets.clear();
        String widgetJson = GBApplication.getPrefs().getPreferences().getString("FOSSIL_HR_WIDGETS", "{}");
        String customWidgetJson = GBApplication.getPrefs().getString("QHYBRID_CUSTOM_WIDGETS", "[]");

        try {
            JSONObject widgetConfig = new JSONObject(widgetJson);
            JSONArray customWidgets = new JSONArray(customWidgetJson);

            Iterator<String> keyIterator = widgetConfig.keys();
            HashMap<String, Integer> positionMap = new HashMap<>(4);
            positionMap.put("top", 0);
            positionMap.put("right", 90);
            positionMap.put("bottom", 180);
            positionMap.put("left", 270);

            while (keyIterator.hasNext()) {
                String position = keyIterator.next();
                String identifier = widgetConfig.getString(position);
                Widget.WidgetType type = Widget.WidgetType.fromJsonIdentifier(identifier);

                Widget widget = null;
                if (type != null) {
                    widget = new Widget(type, positionMap.get(position), 63, fontColor);
                } else {
                    identifier = identifier.substring(7);
                    for (int i = 0; i < customWidgets.length(); i++) {
                        JSONObject customWidget = customWidgets.getJSONObject(i);
                        if (customWidget.getString("name").equals(identifier)) {
                            boolean drawCircle = false;
                            if (customWidget.has("drawCircle"))
                                drawCircle = customWidget.getBoolean("drawCircle");
                            CustomWidget newWidget = new CustomWidget(
                                    customWidget.getString("name"),
                                    positionMap.get(position),
                                    63,
                                    fontColor
                            );
                            JSONArray elements = customWidget.getJSONArray("elements");

                            for (int i2 = 0; i2 < elements.length(); i2++) {
                                JSONObject element = elements.getJSONObject(i2);
                                if (element.getString("type").equals("text")) {
                                    newWidget.addElement(new CustomTextWidgetElement(
                                            element.getString("id"),
                                            element.getString("value"),
                                            element.getInt("x"),
                                            element.getInt("y")
                                    ));
                                } else if (element.getString("type").equals("background")) {
                                    newWidget.addElement(new CustomBackgroundWidgetElement(
                                            element.getString("id"),
                                            element.getString("value")
                                    ));
                                }
                            }
                            widget = newWidget;
                        }
                    }
                }

                if (widget == null) continue;
                widgets.add(widget);
            }
        } catch (JSONException e) {
            LOG.error("Error while updating widgets", e);
        }

        for (Widget oldWidget : oldWidgets) {
            if (!(oldWidget instanceof CustomWidget)) continue;
            CustomWidget customOldWidget = (CustomWidget) oldWidget;
            for (CustomWidgetElement oldElement : customOldWidget.getElements()) {
                for (Widget newWidget : widgets) {
                    if (newWidget instanceof CustomWidget) {
                        ((CustomWidget) newWidget).updateElementValue(oldElement.getId(), oldElement.getValue());
                    }
                }
            }

        }

        uploadWidgets();
    }

    public void setInstalledApplications(List<ApplicationInformation> installedApplications) {
        this.installedApplications = installedApplications;
        GBDeviceEventAppInfo appInfoEvent = new GBDeviceEventAppInfo();
        appInfoEvent.apps = new GBDeviceApp[installedApplications.size()];
        for (int i = 0; i < installedApplications.size(); i++) {
            String appName = installedApplications.get(i).getAppName();
            String appVersion = installedApplications.get(i).getAppVersion();
            UUID appUUID = UUID.nameUUIDFromBytes(appName.getBytes(StandardCharsets.UTF_8));
            GBDeviceApp.Type appType;
            if (installedApplications.get(i).getAppName().endsWith("App")) {
                appType = GBDeviceApp.Type.APP_GENERIC;
            } else {
                appType = GBDeviceApp.Type.WATCHFACE;
            }
            appInfoEvent.apps[i] = new GBDeviceApp(appUUID, appName, "(unknown)", appVersion, appType);
        }
        getDeviceSupport().evaluateGBDeviceEvent(appInfoEvent);
    }

    private void uploadWidgets() {
        ArrayList<Widget> systemWidgets = new ArrayList<>(widgets.size());
        for (Widget widget : widgets) {
            if (!(widget instanceof CustomWidget) && !widget.getWidgetType().isCustom())
                systemWidgets.add(widget);
        }
        queueWrite(new WidgetsPutRequest(systemWidgets.toArray(new Widget[0]), this));
    }

    private void renderWidgets() {
        Version firmwareVersion = getCleanFWVersion();
        if (firmwareVersion != null && firmwareVersion.greaterOrEqualThan(new Version("2.20"))) {
            return; // this does not work on newer firmware versions
        }
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress()));
        boolean forceWhiteBackground = prefs.getBoolean("force_white_color_scheme", false);
        boolean drawCircles = prefs.getBoolean("widget_draw_circles", false);

        Bitmap circleBitmap = null;
        if (drawCircles) {
            circleBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);
            Canvas circleCanvas = new Canvas(circleBitmap);
            Paint circlePaint = new Paint();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(forceWhiteBackground ? Color.WHITE : Color.BLACK);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setStrokeWidth(3);
            circleCanvas.drawCircle(38, 38, 35, circlePaint);

            circlePaint.setColor(forceWhiteBackground ? Color.BLACK : Color.WHITE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(3);
            circleCanvas.drawCircle(38, 38, 35, circlePaint);
        }

        try {
            ArrayList<AssetImage> widgetImages = new ArrayList<>();

            if (this.backGroundImage != null) {
                widgetImages.add(this.backGroundImage);
            }


            for (int i = 0; i < widgets.size(); i++) {
                Widget w = widgets.get(i);
                if (!(w instanceof CustomWidget)) {
                    if (w.getWidgetType() == Widget.WidgetType.LAST_NOTIFICATION) {
                        Bitmap widgetBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);
                        Canvas widgetCanvas = new Canvas(widgetBitmap);
                        if (drawCircles) {
                            widgetCanvas.drawBitmap(circleBitmap, 0, 0, null);
                        }

                        Paint p = new Paint();
                        p.setStyle(Paint.Style.FILL);
                        p.setTextSize(10);
                        p.setColor(Color.WHITE);

                        if (this.lastPostedApp != null) {

                            Bitmap icon = Bitmap.createScaledBitmap(appIconCache.get(this.lastPostedApp), 40, 40, true);

                            if (icon != null) {

                                widgetCanvas.drawBitmap(
                                        icon,
                                        (float) (38 - (icon.getWidth() / 2.0)),
                                        (float) (38 - (icon.getHeight() / 2.0)),
                                        null
                                );
                            }
                        }

                        widgetImages.add(AssetImageFactory.createAssetImage(
                                widgetBitmap,
                                true,
                                w.getAngle(),
                                w.getDistance(),
                                1
                        ));
                    } else if (drawCircles) {
                        widgetImages.add(AssetImageFactory.createAssetImage(
                                circleBitmap,
                                true,
                                w.getAngle(),
                                w.getDistance(),
                                1
                        ));
                    }
                    continue;
                }

                CustomWidget widget = (CustomWidget) w;

                Bitmap widgetBitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888);

                Canvas widgetCanvas = new Canvas(widgetBitmap);

                if (drawCircles) {
                    widgetCanvas.drawBitmap(circleBitmap, 0, 0, null);
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_BACKGROUND) {
                        File imageFile = new File(element.getValue());

                        if (!imageFile.exists() || !imageFile.isFile()) {
                            LOG.debug("Image file " + element.getValue() + " not found");
                            continue;
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());
                        if (imageBitmap == null) {
                            LOG.debug("image file " + element.getValue() + " could not be decoded");
                            continue;
                        }
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 76, 76, false);

                        widgetCanvas.drawBitmap(
                                scaledBitmap,
                                0,
                                0,
                                null);
                        break;
                    }
                }

                for (CustomWidgetElement element : widget.getElements()) {
                    if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_TEXT) {
                        Paint textPaint = new Paint();
                        textPaint.setStrokeWidth(4);
                        textPaint.setTextSize(17f);
                        textPaint.setStyle(Paint.Style.FILL);
                        textPaint.setColor(forceWhiteBackground ? Color.BLACK : Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);

                        widgetCanvas.drawText(element.getValue(), element.getX(), element.getY() - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
                    } else if (element.getWidgetElementType() == CustomWidgetElement.WidgetElementType.TYPE_IMAGE) {
                        Bitmap imageBitmap = BitmapFactory.decodeFile(element.getValue());

                        widgetCanvas.drawBitmap(imageBitmap, element.getX() - imageBitmap.getWidth() / 2f, element.getY() - imageBitmap.getHeight() / 2f, null);
                    }
                }
                widgetImages.add(AssetImageFactory.createAssetImage(
                        widgetBitmap,
                        true,
                        widget.getAngle(),
                        widget.getDistance(),
                        1
                ));
            }


            AssetImage[] images = widgetImages.toArray(new AssetImage[0]);

            ArrayList<AssetImage> pushFiles = new ArrayList<>(4);
            imgloop:
            for (AssetImage image : images) {
                for (AssetImage pushedImage : pushFiles) {
                    // no need to send same file multiple times, filtering by name since name is hash
                    if (image.getFileName().equals(pushedImage.getFileName())) continue imgloop;
                }
                pushFiles.add(image);
            }

            if (pushFiles.size() > 0) {
                queueWrite(new AssetFilePutRequest(
                        pushFiles.toArray(new AssetImage[0]),
                        FileHandle.ASSET_BACKGROUND_IMAGES,
                        this
                ));
            }
            // queueWrite(new FileDeleteRequest((short) 0x0503));
            queueWrite(new ImagesSetRequest(
                    images,
                    this
            ));
        } catch (IOException e) {
            LOG.error("Error while rendering widgets", e);
        }
    }

    private void handleFileDownload(String name, byte[] file, boolean toCache) {
        Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_DOWNLOADED_FILE);
        File outputFile = new File(getContext().getExternalFilesDir("download"), name + "_" + System.currentTimeMillis() + ".bin");
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(file);
            fos.close();
            resultIntent.putExtra("EXTRA_SUCCESS", true);
            resultIntent.putExtra("EXTRA_PATH", outputFile.getAbsolutePath());
            resultIntent.putExtra("EXTRA_NAME", name);
            resultIntent.putExtra("EXTRA_TOCACHE", toCache);
            LOG.info("Wrote downloaded file to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Error while downloading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
    }

    @Override
    public void uploadFileGenerateHeader(FileHandle handle, String filePath, boolean fileIsEncrypted) {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        byte[] fileData;

        try {
            FileInputStream fis = new FileInputStream(filePath);
            fileData = new byte[fis.available()];
            fis.read(fileData);
            fis.close();
        } catch (IOException e) {
            LOG.error("Error while reading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            return;
        }

        queueWrite(new FilePutRequest(handle, fileData, this) {
            @Override
            public void onFilePut(boolean success) {
                resultIntent.putExtra("EXTRA_SUCCESS", success);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            }
        });
    }

    @Override
    public void uploadFileIncludesHeader(String filePath) {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        try {
            FileInputStream fis = new FileInputStream(filePath);
            uploadFileIncludesHeader(fis);
            fis.close();
        } catch (Exception e) {
            LOG.error("Error while uploading file", e);
            resultIntent.putExtra("EXTRA_SUCCESS", false);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
        }
    }

    private void uploadFileIncludesHeader(InputStream fis) throws IOException {
        final Intent resultIntent = new Intent(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        byte[] fileData = new byte[fis.available()];
        fis.read(fileData);

        short handleBytes = (short) (fileData[0] & 0xFF | ((fileData[1] & 0xFF) << 8));
        FileHandle handle = FileHandle.fromHandle(handleBytes);

        if (handle == null) {
            throw new RuntimeException("unknown handle");
        }

        queueWrite(new FilePutRawRequest(handle, fileData, this) {
            @Override
            public void onFilePut(boolean success) {
                resultIntent.putExtra("EXTRA_SUCCESS", success);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
            }
        });

        if (handle == FileHandle.APP_CODE) {
            listApplications();
        }
    }

    @Override
    public void downloadFile(byte majorHandle, byte minorHandle, String name, boolean fileIsEncrypted, boolean toCache) {
        if (fileIsEncrypted) {
            queueWrite((FileEncryptedInterface) new FileEncryptedGetRequest(majorHandle, minorHandle, this) {
                @Override
                public void handleFileData(byte[] fileData) {
                    LOG.debug("downloaded encrypted file");
                    handleFileDownload(name, fileData, toCache);
                }
            });
        } else {
            queueWrite(new FileGetRawRequest(majorHandle, minorHandle, this) {
                @Override
                public void handleFileRawData(byte[] fileData) {
                    LOG.debug("downloaded regular file");
                    handleFileDownload(name, fileData, toCache);
                }
            });
        }
    }

    @Override
    public void handleSetMenuStructure(JSONObject menuStructure) {
        String serialized = menuStructure.toString();
        getDeviceSpecificPreferences()
                .edit()
                .putString("MENU_STRUCTURE_JSON", serialized)
                .apply();

        try {
            String payload = new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("customWatchFace._.config.menu_structure", menuStructure)
                            )
                    ).toString();
            pushConfigJson(payload);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setWidgetContent(String widgetID, String content, boolean renderOnWatch) {
        boolean update = false;
        for (Widget widget : widgets) {
            if (!(widget instanceof CustomWidget)) continue;
            if (((CustomWidget) widget).updateElementValue(widgetID, content)) update = true;
        }

        if (renderOnWatch && update) renderWidgets();
    }

    private void queueWrite(final FileEncryptedInterface request) {
        try {
            queueWrite(new VerifyPrivateKeyRequest(
                    this.getSecretKey(),
                    this
            ) {
                @Override
                protected void handleAuthenticationResult(boolean success) {
                    if (success) {
                        LOG.info("success auth");
                        queueWrite((FossilRequest) request, true);
                    }
                }
            });
        } catch (IllegalAccessException e) {
            GB.toast("error getting key: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        FossilFileReader fossilFile;
        try {
            fossilFile = new FossilFileReader(uri, getContext());
            if (fossilFile.isFirmware()) {
                super.onInstallApp(uri);
            } else if (fossilFile.isApp() || fossilFile.isWatchface()) {
                UriHelper uriHelper = UriHelper.get(uri, getContext());
                InputStream in = new BufferedInputStream(uriHelper.openInputStream());
                uploadFileIncludesHeader(in);
                in.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void negotiateSymmetricKey() {
        try {
            queueWrite(new VerifyPrivateKeyRequest(
                    this.getSecretKey(),
                    this
            ) {
                @Override
                protected void handleAuthenticationResult(boolean success) {
                    FossilHRWatchAdapter.this.handleAuthenticationResult(success);
                }
            });
        } catch (IllegalAccessException e) {
            GB.toast("error getting key: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            this.handleAuthenticationResult(false);
        }
    }

    @Override
    public void setTime() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        queueWrite(
                (FileEncryptedInterface) new ConfigurationPutRequest(this.generateTimeConfigItemNow(), this)
        );
    }

    @Override
    public void setMusicInfo(MusicSpec musicSpec) {
        musicSpec = new MusicSpec(musicSpec);
        if(musicSpec.album == null) musicSpec.album = "";
        if(musicSpec.artist == null) musicSpec.artist = "";
        if(musicSpec.track == null) musicSpec.track = "";
        if (
                currentSpec != null
                        && currentSpec.album.equals(musicSpec.album)
                        && currentSpec.artist.equals(musicSpec.artist)
                        && currentSpec.track.equals(musicSpec.track)
        ) return;
        currentSpec = musicSpec;
        try {
            queueWrite(new MusicInfoSetRequest(
                    musicSpec.artist,
                    musicSpec.album,
                    musicSpec.track,
                    this
            ));
        } catch (BufferOverflowException e) {
            LOG.error("musicInfo: {}", musicSpec, e);
        }
    }

    @Override
    public void setMusicState(MusicStateSpec stateSpec) {
        super.setMusicState(stateSpec);

        queueWrite(new MusicControlRequest(
                stateSpec.state == MusicStateSpec.STATE_PLAYING ? MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PLAYING : MUSIC_PHONE_REQUEST.MUSIC_REQUEST_SET_PAUSED
        ));
    }

    @Override
    public void updateWidgets() {
        loadWidgets();
        renderWidgets();
    }

    @Override
    public void onFetchActivityData() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        syncSettings();

        queueWrite(new FileLookupRequest(FileHandle.ACTIVITY_FILE, this) {
            @Override
            public void handleFileLookup(final short fileHandle) {
                queueWrite((FileEncryptedInterface) new FileEncryptedGetRequest(fileHandle, FossilHRWatchAdapter.this) {
                    @Override
                    public void handleFileData(byte[] fileData) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            User user = DBHelper.getUser(dbHandler.getDaoSession());
                            Long userId = user.getId();
                            Device device = DBHelper.getDevice(getDeviceSupport().getDevice(), dbHandler.getDaoSession());
                            Long deviceId = device.getId();
                            ActivityFileParser parser = new ActivityFileParser();
                            parser.parseFile(fileData);
                            // Activities
                            ArrayList<ActivityEntry> entries = parser.getActivitySamples();
                            HybridHRActivitySampleProvider provider = new HybridHRActivitySampleProvider(getDeviceSupport().getDevice(), dbHandler.getDaoSession());
                            HybridHRActivitySample[] samples = new HybridHRActivitySample[entries.size()];
                            for (int i = 0; i < entries.size(); i++) {
                                samples[i] = entries.get(i).toDAOActivitySample(userId, deviceId);
                            }
                            provider.addGBActivitySamples(samples);
                            // SpO2, should be empty for an unsupported device
                            ArrayList<HybridHRSpo2Sample> spo2Samples = parser.getSpo2Samples();
                            HybridHRSpo2SampleProvider spo2Provider = new HybridHRSpo2SampleProvider(getDeviceSupport().getDevice(), dbHandler.getDaoSession());
                            for (HybridHRSpo2Sample sample : spo2Samples) {
                                sample.setDevice(device);
                                sample.setUser(user);
                            }
                            spo2Provider.addSamples(spo2Samples);
                            // Workout summaries
                            ArrayList<BaseActivitySummary> workoutSummaries = parser.getWorkoutSummaries();
                            LOG.debug("WORKOUT SUMMARIES FOUND: {}", workoutSummaries);
                            BaseActivitySummaryDao summaryDao = provider.getSession().getBaseActivitySummaryDao();
                            for (BaseActivitySummary summary : workoutSummaries) {
                                summary.setDevice(device);
                                summary.setUser(user);
                                summaryDao.insert(summary);
                            }

                            if (saveRawActivityFiles) {
                                writeFile(String.valueOf(System.currentTimeMillis()), fileData);
                            }
                            queueWrite(new FileDeleteRequest(fileHandle));
                            GB.updateTransferNotification(null, "", false, 100, getContext());
                            GB.signalActivityDataFinish(getDeviceSupport().getDevice());
                            LOG.debug("Synchronized activity data");
                        } catch (Exception ex) {
                            GB.toast(getContext(), "Error saving activity data: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                            LOG.error("Error saving activity data: ", ex);
                            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
                        }
                        getDeviceSupport().getDevice().unsetBusyTask();
                        getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
                    }
                });
            }

            @Override
            public void handleFileLookupError(FILE_LOOKUP_ERROR error) {
                if (error == FILE_LOOKUP_ERROR.FILE_EMPTY) {
                    LOG.debug("No activity data to sync");
                } else {
                    throw new RuntimeException("strange lookup stuff");
                }
                getDeviceSupport().getDevice().unsetBusyTask();
                GB.updateTransferNotification(null, "", false, 100, getContext());
                getDeviceSupport().getDevice().sendDeviceUpdateIntent(getContext());
            }
        });
    }

    private void writeFile(String fileName, byte[] value) {
        File activityDir = new File(getContext().getExternalFilesDir(null), "activity_hr");
        activityDir.mkdir();
        File f = new File(activityDir, fileName);
        try {
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(value);
            fos.close();
            GB.toast("saved file data", Toast.LENGTH_SHORT, GB.INFO);
        } catch (IOException e) {
            LOG.error("file error", e);
        }
    }

    private void syncSettings() {
        if (connectionMode == CONNECTION_MODE.NOT_AUTHENTICATED) {
            GB.toast(getContext().getString(R.string.fossil_hr_unavailable_unauthed), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        queueWrite((FileEncryptedInterface) new ConfigurationGetRequest(this));
    }

    @Override
    public void setActivityHand(double progress) {
        // super.setActivityHand(progress);
    }

    private boolean isNotificationWidgetVisible() {
        for (Widget widget : widgets) {
            if (widget.getWidgetType() == Widget.WidgetType.LAST_NOTIFICATION) {
                return true;
            }
        }
        return false;
    }

    public boolean playRawNotification(NotificationSpec notificationSpec) {
        String sourceAppId = notificationSpec.sourceAppId;
        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        // Retrieve and store notification or app icon
        if (sourceAppId != null) {
            if (appIconCache.get(sourceAppId) == null) {
                try {
                    Drawable icon = null;
                    if (notificationSpec.iconId != 0) {
                        Context sourcePackageContext = getContext().createPackageContext(sourceAppId, 0);
                        icon = ResourcesCompat.getDrawable(sourcePackageContext.getResources(), notificationSpec.iconId, null);
                    }
                    if (icon == null) {
                        icon = NotificationUtils.getAppIcon(getContext(), sourceAppId);
                    }
                    Bitmap iconBitmap = convertDrawableToBitmap(icon);
                    appIconCache.put(sourceAppId, iconBitmap);
                    setNotificationConfigurations();
                } catch (PackageManager.NameNotFoundException e) {
                    LOG.error("Error while updating notification icons", e);
                }
            }
        }

        boolean packageFound = false;

        // Send notification to watch
        try {
            for (NotificationHRConfiguration configuration : this.notificationConfigurations) {
                if (configuration.getPackageName().equals(sourceAppId)) {
                    LOG.info("Package found in notificationConfigurations, using custom icon: " + sourceAppId);
                    queueWrite(new PlayTextNotificationRequest(sourceAppId, senderOrTitle, notificationSpec, this));
                    packageFound = true;
                }
            }

            if (!packageFound) {
                LOG.info("Package not found in notificationConfigurations, using generic icon: " + sourceAppId);
                queueWrite(new PlayTextNotificationRequest("generic", senderOrTitle, notificationSpec, this));
            }
        } catch (Exception e) {
            LOG.error("Error while forwarding notification", e);
        }

        // Update notification icon custom widget
        if (isNotificationWidgetVisible() && sourceAppId != null) {
            if (!sourceAppId.equals(this.lastPostedApp)) {
                this.lastPostedApp = sourceAppId;
                renderWidgets();
            }
        }
        return true;
    }

    @Override
    public void onDeleteNotification(int id) {
        super.onDeleteNotification(id);

        // send notification dismissal message to watch
        try {
            queueWrite(new DismissTextNotificationRequest(id, this));
        } catch (Exception e) {
            LOG.error("Error while dismissing notification", e);
        }

        // only delete app icon when no notification of said app is present
        for (String app : NotificationListener.notificationStack) {
            if (app.equals(this.lastPostedApp)) return;
        }

        this.lastPostedApp = null;

        if (isNotificationWidgetVisible()) {
            renderWidgets();
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        super.onSetCallState(callSpec);
        String[] quickReplies = getQuickReplies();
        boolean quickRepliesEnabled = quickReplies.length > 0 && callSpec.number != null && callSpec.number.matches("^\\+(?:[0-9] ?){6,14}[0-9]$");
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            currentCallSpec = callSpec;
            queueWrite(new PlayCallNotificationRequest(StringUtils.getFirstOf(callSpec.name, callSpec.number), true, quickRepliesEnabled, callSpec.dndSuppressed, this));
        } else {
            currentCallSpec = null;
            queueWrite(new PlayCallNotificationRequest(StringUtils.getFirstOf(callSpec.name, callSpec.number), false, quickRepliesEnabled, callSpec.dndSuppressed, this));
        }
    }

    // this method is based on the one from AppMessageHandlerYWeather.java
    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        final int CLEAR_DAY = 0;
        final int CLEAR_NIGHT = 1;
        final int CLOUDY = 2;
        final int PARTLY_CLOUDY_DAY = 3;
        final int PARTLY_CLOUDY_NIGHT = 4;
        final int RAIN = 5;
        final int SNOW = 6;
        final int SNOW_2 = 7; // same as 6?
        final int THUNDERSTORM = 8;
        final int CLOUDY_2 = 9; // same as 2?
        final int WINDY = 10;

        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? CLEAR_NIGHT : CLEAR_DAY;
        } else if (conditionCode > 800 && conditionCode < 900) {
            return isNight ? PARTLY_CLOUDY_NIGHT : PARTLY_CLOUDY_DAY;
        } else if (conditionCode >= 300 && conditionCode < 400) {
            return RAIN; // drizzle mapped to rain
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return CLOUDY;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762) {
            return CLOUDY; // fog mapped to cloudy
        } else if (conditionCode == 771) {
            return CLOUDY; // squalls mapped to cloudy
        } else if (conditionCode == 781) {
            return WINDY; // tornato mapped to windy
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return THUNDERSTORM;
        } else if (conditionCode >= 600 && conditionCode <= 602) {
            return SNOW;
        } else if (conditionCode >= 611 && conditionCode <= 622) {
            return RAIN;
        } else if (conditionCode == 906) {
            return RAIN; // hail mapped to rain
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return WINDY;
        } else if (conditionCode == 905) {
            return WINDY;
        } else if (conditionCode == 900) {
            return WINDY;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return WINDY;
        }
        return isNight ? CLEAR_NIGHT : CLEAR_DAY;
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        boolean isNight = false;
        if (weatherSpec.getSunRise() != 0 && weatherSpec.getSunSet() != 0) {
            isNight = weatherSpec.getSunRise() * 1000L > System.currentTimeMillis() || weatherSpec.getSunSet() * 1000L < System.currentTimeMillis();
        } else {
            Location location = weatherSpec.getLocationObject();
            if (location == null) {
                location = new CurrentPosition().getLastKnownLocation();
            }
            final ZonedDateTime now = ZonedDateTime.now();
            final SunriseTransitSet sunriseTransitSet = SPA.calculateSunriseTransitSet(
                    now,
                    location.getLatitude(),
                    location.getLongitude(),
                    DeltaT.estimate(now.toLocalDate())
            );
            if (sunriseTransitSet.getSunrise() != null && sunriseTransitSet.getSunset() != null) {
                isNight = sunriseTransitSet.getSunrise().isAfter(now) || sunriseTransitSet.getSunset().isBefore(now);
            }
        }

        long ts = System.currentTimeMillis();
        ts /= 1000;
        try {
            JSONObject responseObject = new JSONObject()
                    .put("res", new JSONObject()
                            .put("id", 0) // seems the id does not matter?
                            .put("set", new JSONObject()
                                    .put("weatherInfo", new JSONObject()
                                            .put("alive", ts + 60 * 60)
                                            .put("unit", "c") // FIXME: do not hardcode
                                            .put("temp", weatherSpec.getCurrentTemp() - 273)
                                            .put("cond_id", getIconForConditionCode(weatherSpec.getCurrentConditionCode(), isNight))
                                    )
                            )
                    );

            queueWrite(new JsonPutRequest(responseObject, this));

            JSONArray forecastWeekArray = new JSONArray();
            final String[] weekdays = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(weatherSpec.getTimestamp() * 1000L);
            int i = 0;
            for (WeatherSpec.Daily forecast : weatherSpec.getForecasts()) {
                cal.add(Calendar.DATE, 1);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                forecastWeekArray.put(new JSONObject()
                        .put("day", weekdays[dayOfWeek])
                        .put("cond_id", getIconForConditionCode(forecast.getConditionCode(), false))
                        .put("high", forecast.getMaxTemp() - 273)
                        .put("low", forecast.getMinTemp() - 273)
                );
                if (++i == 3) break; // max 3
            }

            JSONArray forecastDayArray = new JSONArray();
            final int[] hours = {0, 0, 0};

            for (int hour : hours) {
                forecastDayArray.put(new JSONObject()
                        .put("hour", hour)
                        .put("cond_id", 0)
                        .put("temp", 0)
                );
            }

            JSONObject forecastResponseObject = new JSONObject()
                    .put("res", new JSONObject()
                            .put("id", 0)
                            .put("set", new JSONObject()
                                    .put("weatherApp._.config.locations", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("alive", ts + 60 * 60)
                                                    .put("city", weatherSpec.getLocation())
                                                    .put("unit", "c") // FIXME: do not hardcode
                                                    .put("temp", weatherSpec.getCurrentTemp() - 273)
                                                    .put("high", weatherSpec.getTodayMaxTemp() - 273)
                                                    .put("low", weatherSpec.getTodayMinTemp() - 273)
                                                    .put("rain", weatherSpec.getPrecipProbability())
                                                    .put("uv", Math.round(weatherSpec.getUvIndex()))
                                                    .put("message", weatherSpec.getCurrentCondition())
                                                    .put("cond_id", getIconForConditionCode(weatherSpec.getCurrentConditionCode(), isNight))
                                                    .put("forecast_day", forecastDayArray)
                                                    .put("forecast_week", forecastWeekArray)
                                            )
                                    )
                            )
                    );

            queueWrite(new JsonPutRequest(forecastResponseObject, this));

        } catch (JSONException e) {
            LOG.error("JSON exception: ", e);
        }
    }

    public void onSendChanceOfRain(WeatherSpec weatherSpec) {
        long ts = System.currentTimeMillis();
        ts /= 1000;
        try {
            JSONObject rainObject = new JSONObject()
                .put("res", new JSONObject()
                    .put("set", new JSONObject()
                        .put("widgetChanceOfRain._.config.info", new JSONObject()
                            .put("alive", ts + 60 * 15)
                            .put("rain", weatherSpec.getPrecipProbability())
                        )
                    )
                );

            queueWrite(new JsonPutRequest(rainObject, this));
        } catch (JSONException e) {
            LOG.error("JSON exception: ", e);
        }
    }

    public void onSendUVIndex(WeatherSpec weatherSpec) {
        long ts = System.currentTimeMillis();
        ts /= 1000;
        try {
            JSONObject rainObject = new JSONObject()
                .put("res", new JSONObject()
                    .put("set", new JSONObject()
                        .put("widgetUV._.config.info", new JSONObject()
                            .put("alive", ts + 60 * 15)
                            .put("uv", Math.round(weatherSpec.getUvIndex()))
                        )
                    )
                );

            queueWrite(new JsonPutRequest(rainObject, this));
        } catch (JSONException e) {
            LOG.error("JSON exception: ", e);
        }
    }

    public void onSendCalendar() {
        if (!getDeviceSpecificPreferences().getBoolean("sync_calendar", false)) {
            LOG.debug("Ignoring calendar sync request, sync is disabled");
            return;
        }

        int maxItems = Integer.parseInt(getDeviceSpecificPreferences().getString(PREF_CALENDAR_SYNC_EVENTS_AMOUNT, "5"));
        int titleLength = Integer.parseInt(getDeviceSpecificPreferences().getString(PREF_CALENDAR_MAX_TITLE_LENGTH, "40"));
        int descLength = Integer.parseInt(getDeviceSpecificPreferences().getString(PREF_CALENDAR_MAX_DESC_LENGTH, "40"));
        String targetApp = getDeviceSpecificPreferences().getString(PREF_CALENDAR_TARGET_APP, "customWatchFace");

        final CalendarManager upcomingEvents = new CalendarManager(getContext(), getDeviceSupport().getDevice().getAddress());
        final List<CalendarEvent> calendarEvents = upcomingEvents.getCalendarEventList();

        final Set<CalendarEvent> thisSync = new HashSet<>();
        int nEvents = 0;

        for (final CalendarEvent calendarEvent : calendarEvents) {
            if (++nEvents > maxItems) {
                LOG.warn("Syncing only first {} events of {}", maxItems, calendarEvents.size());
                break;
            }
            thisSync.add(calendarEvent);
        }

        if (thisSync.equals(lastSync)) {
            LOG.debug("Already synced this set of events, won't send to device");
            return;
        }

        lastSync.clear();
        lastSync.addAll(thisSync);

        List<CalendarEvent> sortedEventList = new ArrayList<>(thisSync);
        Collections.sort(sortedEventList, Comparator.comparingLong(CalendarEvent::getBegin));

        LOG.debug("Syncing {} calendar events", sortedEventList.size());

        try {
            JSONArray items = new JSONArray();
            for(CalendarEvent event : sortedEventList) {
                JSONArray reminders = new JSONArray();
                for (long reminder : event.getRemindersAbsoluteTs()) {
                    reminders.put(reminder / 1000);
                }
                String title = event.getTitle();
                if (title != null && title.length() > titleLength)
                    title = event.getTitle().substring(0, titleLength);
                String desc = event.getDescription();
                if (desc != null && desc.length() > descLength)
                    desc = event.getDescription().substring(0, descLength);
                items.put(new JSONObject()
                        .put("id", event.getId())
                        .put("title", title)
                        .put("desc", desc)
                        .put("start", event.getBeginSeconds())
                        .put("end", event.getEndSeconds())
                        .put("reminders", reminders)
                );
            }
            JSONObject calendarObj = new JSONObject()
                    .put("res", new JSONObject()
                            .put("set", new JSONObject()
                                    .put(targetApp + "._.config.events", items)
                            )
                    );

            queueWrite(new JsonPutRequest(calendarObj, this));
        } catch (JSONException e) {
            LOG.error("Error sending calendar events: ", e);
        }
    }

    @Override
    public void factoryReset() {
        queueWrite(new FactoryResetRequest());
    }

    @Override
    public void onTestNewFunction() {
        onSendCalendar();
    }

    public byte[] getSecretKey() throws IllegalAccessException {
        byte[] authKeyBytes = new byte[16];

        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDeviceSupport().getDevice().getAddress());

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            authKey = authKey.replace(" ", "");
            authKey = authKey.replace("0x", "");
            if (authKey.length() != 32) {
                throw new IllegalAccessException("Key should be 16 bytes long as hex string");
            }
            byte[] srcBytes = GB.hexStringToByteArray(authKey);

            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    @Override
    public void pushConfigJson(String configJson) {
        configJson = configJson.replace("\n", "");
        queueWrite(new JsonPutRequest(configJson, this));
    }

    public void setPhoneRandomNumber(byte[] phoneRandomNumber) {
        this.phoneRandomNumber = phoneRandomNumber;
    }

    public byte[] getPhoneRandomNumber() {
        return phoneRandomNumber;
    }

    public void setWatchRandomNumber(byte[] watchRandomNumber) {
        this.watchRandomNumber = watchRandomNumber;
    }

    public byte[] getWatchRandomNumber() {
        return watchRandomNumber;
    }

    @Override
    public void overwriteButtons(String jsonConfigString) {
        try {
            SharedPreferences prefs = getDeviceSpecificPreferences();

            String singlePressEvent = "short_press_release";

            Version firmwareVersion = getCleanFWVersion();
            if (firmwareVersion != null && firmwareVersion.smallerThan(new Version("2.19"))) {
                singlePressEvent = "single_click";
            }
            ArrayList<ButtonConfiguration> configs = new ArrayList<>(6);
            configs.add(new ButtonConfiguration("top_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_SHORT, "weatherApp")));
            configs.add(new ButtonConfiguration("top_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_LONG, "weatherApp")));
            // configs.add(new ButtonConfiguration("top_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_DOUBLE, "weatherApp")));
            configs.add(new ButtonConfiguration("middle_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_SHORT, "commuteApp")));
            if (firmwareVersion != null && firmwareVersion.greaterOrEqualThan(new Version("3.0"))) {
                configs.add(new ButtonConfiguration("middle_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_LONG, "launcherApp")));
            }
            // configs.add(new ButtonConfiguration("middle_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_DOUBLE, "commuteApp")));
            configs.add(new ButtonConfiguration("bottom_" + singlePressEvent, prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_SHORT, "musicApp")));
            configs.add(new ButtonConfiguration("bottom_hold", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_LONG, "musicApp")));
            // configs.add(new ButtonConfiguration("bottom_double_click", prefs.getString(DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_DOUBLE, "musicApp")));

            // filter out all apps not installed on watch
            ArrayList<ButtonConfiguration> availableConfigs = new ArrayList<>();
            outerLoop:
            for (ButtonConfiguration config : configs) {
                for (ApplicationInformation installedApp : installedApplications) {
                    if (installedApp.getAppName().equals(config.getAction()) ||
                            config.getAction().equals("workoutApp") //workoutApp is part of internal firmware
                    ) {
                        availableConfigs.add(config);
                        continue outerLoop;
                    }
                }
            }

            queueWrite(new ButtonConfigurationPutRequest(
                    availableConfigs.toArray(new ButtonConfiguration[0]),
                    this
            ));

            for (ApplicationInformation info : installedApplications) {
                if (info.getAppName().equals("commuteApp")) {
                    JSONArray jsonArray = new JSONArray(
                            GBApplication.getPrefs().getString(CommuteActionsActivity.CONFIG_KEY_Q_ACTIONS, "[]")
                    );
                    String[] menuItems = new String[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++)
                        menuItems[i] = jsonArray.getString(i);
                    queueWrite(new CommuteConfigPutRequest(menuItems, this));
                    break;
                }
            }
        } catch (JSONException e) {
            LOG.error("Error while configuring buttons", e);
        }
    }

    private void setActivityRecognition(){
        SharedPreferences prefs = getDeviceSpecificPreferences();
        boolean runningEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ENABLED, false);
        boolean runningAskFirst = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ASK_FIRST, false);
        int runningMinutes = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_MINUTES, "3"));
        boolean bikingEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ENABLED, false);
        boolean bikingAskFirst = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ASK_FIRST, false);
        int bikingMinutes = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_MINUTES, "5"));
        boolean walkingEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ENABLED, false);
        boolean walkingAskFirst = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ASK_FIRST, false);
        int walkingMinutes = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_MINUTES, "10"));
        boolean rowingEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ENABLED, false);
        boolean rowingAskFirst = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ASK_FIRST, false);
        int rowingMinutes = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_MINUTES, "3"));

        if (runningMinutes < 1) runningMinutes = 1;
        if (runningMinutes > 255) runningMinutes = 255;
        if (bikingMinutes < 1) bikingMinutes = 1;
        if (bikingMinutes > 255) bikingMinutes = 255;
        if (walkingMinutes < 1) walkingMinutes = 1;
        if (walkingMinutes > 255) walkingMinutes = 255;
        if (rowingMinutes < 1) rowingMinutes = 1;
        if (rowingMinutes > 255) rowingMinutes = 255;

        FitnessConfigItem fitnessConfigItem = new FitnessConfigItem(
                runningEnabled,
                runningAskFirst,
                runningMinutes,
                bikingEnabled,
                bikingAskFirst,
                bikingMinutes,
                walkingEnabled,
                walkingAskFirst,
                walkingMinutes,
                rowingEnabled,
                rowingAskFirst,
                rowingMinutes
        );

        queueWrite((FileEncryptedInterface) new ConfigurationPutRequest(fitnessConfigItem, this));
    }

    private void setInactivityWarning(){
        SharedPreferences prefs = getDeviceSpecificPreferences();
        boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false);
        int threshold = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, "60"));
        String start = prefs.getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "06:00");
        String end = prefs.getString(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "22:00");

        int startHour = Integer.parseInt(start.split(":")[0]);
        int startMinute = Integer.parseInt(start.split(":")[1]);
        int endHour = Integer.parseInt(end.split(":")[0]);
        int endMinute = Integer.parseInt(end.split(":")[1]);

        InactivityWarningItem inactivityWarningItem = new InactivityWarningItem(
                startHour, startMinute, endHour, endMinute, threshold, enabled
        );

        queueWrite((FileEncryptedInterface) new ConfigurationPutRequest(inactivityWarningItem, this));
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_SHORT:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_LONG:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_1_FUNCTION_DOUBLE:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_2_FUNCTION_DOUBLE:
            case DeviceSettingsPreferenceConst.PREF_BUTTON_3_FUNCTION_DOUBLE:
                overwriteButtons(null);
                break;
            case DeviceSettingsPreferenceConst.PREF_VIBRATION_STRENGH_PERCENTAGE:
                setVibrationStrengthFromConfig();
                break;
            case "force_white_color_scheme":
                loadBackground();
                // not break here
            case "widget_draw_circles": {
                renderWidgets();
                break;
            }
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES: {
                saveRawActivityFiles = getDeviceSpecificPreferences().getBoolean("save_raw_activity_files", false);
                break;
            }
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setUnitsConfig();
                break;
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ASK_FIRST:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_MINUTES:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ASK_FIRST:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_MINUTES:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ASK_FIRST:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_MINUTES:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ASK_FIRST:
            case DeviceSettingsPreferenceConst.PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_MINUTES:
                setActivityRecognition();
                break;
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
                setInactivityWarning();
                break;
        }
    }

    @Override
    public void handleHeartRateCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        super.handleHeartRateCharacteristic(characteristic, value);

        int heartRate = value[1];

        LOG.debug("heart rate: " + heartRate);
    }

    @Override
    protected void handleBackgroundCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        super.handleBackgroundCharacteristic(characteristic, value);

        byte requestType = value[1];

        if (requestType == (byte) 0x04) {
            if (value[7] == 0x00 || value[7] == 0x01) {
                handleCallRequest(value);
            } else if (value[7] == 0x02) {
                handleDeleteNotification(value);
            } else if (value[7] == 0x03) {
                handleQuickReplyRequest(value);
            }
        } else if (requestType == (byte) 0x05) {
            handleMusicRequest(value);
        } else if (requestType == (byte) 0x01) {
            int eventId = value[2];
            LOG.info("got event id " + eventId);
            try {
                String jsonString = new String(value, 3, value.length - 3);
                // logger.info(jsonString);
                JSONObject requestJson = new JSONObject(jsonString);

                JSONObject request = requestJson.getJSONObject("req");
                int requestId = request.getInt("id");

                if (request.has("ringMyPhone")) {
                    String action = request.getJSONObject("ringMyPhone").getString("action");
                    LOG.info("got ringMyPhone request; " + action);
                    GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

                    JSONObject responseObject = new JSONObject()
                            .put("res", new JSONObject()
                                    .put("id", requestId)
                                    .put("set", new JSONObject()
                                            .put("ringMyPhone", new JSONObject()
                                            )
                                    )
                            );

                    if ("on".equals(action)) {
                        findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                        getDeviceSupport().evaluateGBDeviceEvent(findPhoneEvent);
                        responseObject
                                .getJSONObject("res")
                                .getJSONObject("set")
                                .getJSONObject("ringMyPhone")
                                .put("result", "on");
                        queueWrite(new JsonPutRequest(responseObject, this));
                    } else if ("off".equals(action)) {
                        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                        getDeviceSupport().evaluateGBDeviceEvent(findPhoneEvent);
                        responseObject
                                .getJSONObject("res")
                                .getJSONObject("set")
                                .getJSONObject("ringMyPhone")
                                .put("result", "off");
                        queueWrite(new JsonPutRequest(responseObject, this));
                    }
                } else if (request.has("weatherInfo") || request.has("weatherApp._.config.locations")) {
                    LOG.info("Got weatherInfo request");
                    WeatherSpec weatherSpec = Weather.getWeatherSpec();
                    if (weatherSpec != null) {
                        onSendWeather(weatherSpec);
                    } else {
                        LOG.info("no weather data available  - ignoring request");
                    }
                } else if (request.has("widgetChanceOfRain._.config.info")) {
                    LOG.info("Got widgetChanceOfRain request");
                    WeatherSpec weatherSpec = Weather.getWeatherSpec();
                    if (weatherSpec != null) {
                        onSendChanceOfRain(weatherSpec);
                    } else {
                        LOG.info("no weather data available  - ignoring request");
                    }
                } else if (request.has("widgetUV._.config.info")) {
                    LOG.info("Got widgetUV request");
                    WeatherSpec weatherSpec = Weather.getWeatherSpec();
                    if (weatherSpec != null) {
                        onSendUVIndex(weatherSpec);
                    } else {
                        LOG.info("no weather data available  - ignoring request");
                    }
                } else if (request.has("commuteApp._.config.commute_info")) {
                    String action = request.getJSONObject("commuteApp._.config.commute_info")
                            .getString("dest");

                    String startStop = request.getJSONObject("commuteApp._.config.commute_info")
                            .getString("action");

                    if (startStop.equals("stop")) {
                        // overwriteButtons(null);
                        return;
                    }

                    Intent menuIntent = new Intent(QHybridSupport.QHYBRID_EVENT_COMMUTE_MENU);
                    menuIntent.putExtra("EXTRA_ACTION", action);
                    getContext().sendBroadcast(menuIntent);
                } else if (request.has("master._.config.app_status")) {
                    queueWrite(new ConfirmAppStatusRequest(requestId, this));
                } else if (request.has("workoutApp")) {
                    JSONObject workoutRequest = request.getJSONObject("workoutApp");
                    String workoutState = workoutRequest.optString("state");
                    String workoutType = workoutRequest.optString("type");
                    LOG.info("Got workoutApp request, state=" + workoutState + ", type=" + workoutType);
                    JSONObject workoutResponse = WorkoutRequestHandler.handleRequest(getContext(), requestId, workoutRequest);
                    if (workoutResponse.length() > 0) {
                        JSONObject responseObject = new JSONObject()
                            .put("res", new JSONObject()
                                .put("id", requestId)
                                .put("set", workoutResponse)
                            );
                        queueWrite(new JsonPutRequest(responseObject, this));
                    }
                } else if (request.optString("custom_menu").equals("request_config")) {
                    PackageManager manager = getContext().getPackageManager();
                    try{
                        // only show toast when companion app is installed
                        manager.getApplicationInfo("d.d.hrmenucompanion", 0);
                        GB.toast(getContext().getString(R.string.info_fossil_rebuild_watchface_custom_menu), Toast.LENGTH_SHORT, GB.INFO);
                    }catch (PackageManager.NameNotFoundException e){
                    }
                } else {
                    LOG.warn("Unhandled request from watch: " + requestJson.toString());
                }
            } catch (JSONException e) {
                LOG.error("Error while handling received characteristic", e);
            }
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        super.onFindDevice(start);

        boolean versionSupportsConfirmation = getCleanFWVersion().greaterOrEqualThan(new Version("2.22"));

        if(!versionSupportsConfirmation){
            GB.toast("not supported in this version", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }

        if (start) {
            queueWrite(new ConfirmOnDeviceRequest());
        }
    }

    private void handleDeleteNotification(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int notifId = buffer.getInt(3);

        Intent deleteIntent = new Intent(NotificationListener.ACTION_DISMISS);
        deleteIntent.putExtra("handle", (long) notifId);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(deleteIntent);
    }

    private void handleCallRequest(byte[] value) {
        SharedPreferences prefs = getDeviceSpecificPreferences();
        String rejectMethodPref = prefs.getString(DeviceSettingsPreferenceConst.PREF_CALL_REJECT_METHOD, "reject");
        GBDeviceEventCallControl.Event rejectMethod = GBDeviceEventCallControl.Event.REJECT;
        if (rejectMethodPref.equals("ignore")) rejectMethod = GBDeviceEventCallControl.Event.IGNORE;

        boolean acceptCall = value[7] == (byte) 0x00;
        queueWrite(new PlayCallNotificationRequest("", false, false, 0,this));

        GBDeviceEventCallControl callControlEvent = new GBDeviceEventCallControl();
        callControlEvent.event = acceptCall ? GBDeviceEventCallControl.Event.START : rejectMethod;

        getDeviceSupport().evaluateGBDeviceEvent(callControlEvent);
    }

    private void handleQuickReplyRequest(byte[] value) {
        if (currentCallSpec == null) {
            return;
        }
        String[] quickReplies = getQuickReplies();
        byte callId = value[3];
        byte replyChoice = value[8];
        if (replyChoice >= quickReplies.length) {
            return;
        }
        GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
        devEvtNotificationControl.handle = callId;
        devEvtNotificationControl.phoneNumber = currentCallSpec.number;
        devEvtNotificationControl.reply = quickReplies[replyChoice];
        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        getDeviceSupport().evaluateGBDeviceEvent(devEvtNotificationControl);
        queueWrite(new QuickReplyConfirmationPutRequest(callId));
    }

    private void handleMusicRequest(byte[] value) {
        byte command = value[3];
        LOG.info("got music command: " + command);
        MUSIC_WATCH_REQUEST request = MUSIC_WATCH_REQUEST.fromCommandByte(command);

        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;

        // TODO add skipping/seeking

        switch (request) {
            case MUSIC_REQUEST_PLAY_PAUSE: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PLAY_PAUSE));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            }
            case MUSIC_REQUEST_NEXT: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_NEXT));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            }
            case MUSIC_REQUEST_PREVIOUS: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_PREVIOUS));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            }
            case MUSIC_REQUEST_LOUDER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_LOUDER));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                break;
            }
            case MUSIC_REQUEST_QUITER: {
                queueWrite(new MusicControlRequest(MUSIC_PHONE_REQUEST.MUSIC_REQUEST_QUITER));
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                break;
            }
        }

        getDeviceSupport().evaluateGBDeviceEvent(deviceEventMusicControl);
    }

    @Override
    public void setCommuteMenuMessage(String message, boolean finished) {
        queueWrite(new SetCommuteMenuMessage(message, finished, this));
    }

    public byte getJsonIndex() {
        return jsonIndex++;
    }

    private Version getCleanFWVersion() {
        return new Version(getDeviceSupport().getDevice().getFirmwareVersion2());
    }

    public String getInstalledAppNameFromUUID(UUID uuid) {
        for (ApplicationInformation appInfo : installedApplications) {
            if (UUID.nameUUIDFromBytes(appInfo.getAppName().getBytes(StandardCharsets.UTF_8)).equals(uuid)) {
                return appInfo.getAppName();
            }
        }
        if (uuid.equals(UUID.nameUUIDFromBytes("workoutApp".getBytes(StandardCharsets.UTF_8)))) {
            return "workoutApp";
        }
        return null;
    }

    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {
        SharedPreferences prefs = getDeviceSpecificPreferences();
        ItemWithDetails installedAppsJson = getDeviceSupport().getDevice().getDeviceInfo("INSTALLED_APPS");
        if (installedAppsJson == null || !installedAppsJson.getDetails().contains("navigationApp")) {
            if (!notifiedAboutMissingNavigationApp) {
                notifiedAboutMissingNavigationApp = true;
                NotificationCompat.Builder ncomp = new NotificationCompat.Builder(getContext(), NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getContext().getString(R.string.fossil_hr_nav_app_not_installed_notify_title))
                        .setContentText(getContext().getString(R.string.fossil_hr_nav_app_not_installed_notify_text))
                        .setTicker(getContext().getString(R.string.fossil_hr_nav_app_not_installed_notify_text))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true);
                GB.notify((int) System.currentTimeMillis(), ncomp.build(), getContext());
                GB.toast(getContext().getString(R.string.fossil_hr_nav_app_not_installed_notify_text), Toast.LENGTH_LONG, GB.WARN);
            }
            return;
        }
        try {
            JSONObject navJson = new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("navigationApp._.config.info", new JSONObject()
                                            .put("distance", navigationInfoSpec.distanceToTurn)
                                            .put("eta", navigationInfoSpec.ETA)
                                            .put("instruction", navigationInfoSpec.instruction)
                                            .put("nextAction", navigationInfoSpec.nextAction)
                                            .put("autoFg", prefs.getBoolean("fossil_hr_nav_auto_foreground", true))
                                            .put("vibrate", prefs.getBoolean("fossil_hr_nav_vibrate", true))
                                    )
                            )
                    );

            queueWrite(new JsonPutRequest(navJson, this));
        } catch (JSONException e) {
            LOG.error("JSON exception: ", e);
        }
    }

    private void updateBuiltinAppsInCache() {
        FossilFileReader fileReader;
        try {
            fileReader = new FossilFileReader(FileUtils.getUriForAsset("fossil_hr/navigationApp.wapp", getContext()), getContext());
            if (FossilHRInstallHandler.saveAppInCache(fileReader, fileReader.getBackground(), fileReader.getPreview(), getDeviceSupport().getDevice().getDeviceCoordinator(), getContext())) {
                LOG.info("Successfully copied navigationApp for Fossil Hybrids to cache");
            }
        } catch (IOException e) {
            LOG.warn("Could not copy navigationApp to cache", e);
        }
    }
}
