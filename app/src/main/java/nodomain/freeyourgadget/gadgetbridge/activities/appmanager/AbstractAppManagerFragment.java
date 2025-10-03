/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Daniele Gobbetti, José Rebelo, Konrad Iturbe,
    TylerWilliamson

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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport.QHYBRID_ACTION_DOWNLOADED_FILE;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ExternalPebbleJSActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAppAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.FossilFileReader;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.FossilHRInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.QHybridConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GridAutoFitLayoutManager;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Version;


public abstract class AbstractAppManagerFragment extends Fragment {
    public static final String ACTION_REFRESH_APPLIST
            = "nodomain.freeyourgadget.gadgetbridge.appmanager.action.refresh_applist";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAppManagerFragment.class);
    private static final int CHILD_ACTIVITY_WATCHFACE_EDITOR = 0;

    private ItemTouchHelper appManagementTouchHelper;

    protected final List<GBDeviceApp> appList = new ArrayList<>();
    private GBDeviceAppAdapter mGBDeviceAppAdapter;
    protected GBDevice mGBDevice = null;
    protected DeviceCoordinator mCoordinator = null;
    private Class<? extends Activity> watchfaceDesignerActivity;

    protected abstract List<GBDeviceApp> getSystemAppsInCategory();

    protected abstract String getSortFilename();

    protected abstract boolean isCacheManager();

    protected abstract boolean filterApp(GBDeviceApp gbDeviceApp);

    public void startDragging(RecyclerView.ViewHolder viewHolder) {
        appManagementTouchHelper.startDrag(viewHolder);
    }

    protected void onChangedAppOrder() {
        List<UUID> uuidList = new ArrayList<>();
        for (GBDeviceApp gbDeviceApp : mGBDeviceAppAdapter.getAppList()) {
            uuidList.add(gbDeviceApp.getUUID());
        }
        AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuidList);
    }

    protected void refreshList() {
        appList.clear();
        ArrayList<UUID> uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        List<GBDeviceApp> systemApps = getSystemAppsInCategory();
        boolean needsRewrite = false;
        for (GBDeviceApp systemApp : systemApps) {
            if (!uuids.contains(systemApp.getUUID())) {
                uuids.add(systemApp.getUUID());
                needsRewrite = true;
            }
        }
        if (needsRewrite) {
            AppManagerActivity.rewriteAppOrderFile(getSortFilename(), uuids);
        }
        appList.addAll(getCachedApps(uuids));
    }

    private void refreshListFromDevice(Intent intent) {
        final Map<UUID, GBDeviceApp> cachedAppsMap = getCachedAppsMap(null);

        appList.clear();
        int appCount = intent.getIntExtra("app_count", 0);
        for (int i = 0; i < appCount; i++) {
            String appName = intent.getStringExtra("app_name" + i);
            String appCreator = intent.getStringExtra("app_creator" + i);
            String appVersion = intent.getStringExtra("app_version" + i);
            UUID uuid = UUID.fromString(intent.getStringExtra("app_uuid" + i));
            GBDeviceApp.Type appType = GBDeviceApp.Type.values()[intent.getIntExtra("app_type" + i, 0)];
            Bitmap previewImage = getAppPreviewImage(uuid.toString());

            // Fill out information from the cached app if missing
            final GBDeviceApp cachedApp = cachedAppsMap.get(uuid);
            if (cachedApp != null) {
                if (StringUtils.isBlank(appName)) {
                    appName = cachedApp.getName();
                }
                if (StringUtils.isBlank(appCreator)) {
                    appCreator = cachedApp.getCreator();
                }
            } else {
                if (StringUtils.isBlank(appName)) {
                    // If the app does not have a name, fallback to uuid
                    appName = uuid.toString();
                }
            }

            GBDeviceApp app = new GBDeviceApp(uuid, appName, appCreator, appVersion, appType, previewImage);
            app.setOnDevice(true);
            if (mGBDevice.getType() == DeviceType.FOSSILQHYBRID) {
                if ((app.getType() == GBDeviceApp.Type.WATCHFACE) && (!QHybridConstants.HYBRIDHR_WATCHFACE_VERSION.equals(appVersion))) {
                    app.setUpToDate(false);
                }
                try {
                    if ((app.getType() == GBDeviceApp.Type.APP_GENERIC) && ((new Version(app.getVersion())).smallerThan(new Version(QHybridConstants.KNOWN_WAPP_VERSIONS.get(app.getName()))))) {
                        app.setUpToDate(false);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("App JSON: " + app.getJSON().toString());
                    LOG.warn("Couldn't read app version", e);
                }
            }

            if (filterApp(app)) {
                appList.add(app);
            }
        }
        if (mGBDevice.getType() == DeviceType.FOSSILQHYBRID) {
            List<GBDeviceApp> systemApps = getSystemAppsInCategory();
            for (GBDeviceApp systemApp : systemApps) {
                appList.add(systemApp);
            }
        }
    }

    private Bitmap getAppPreviewImage(String name) {
        Bitmap previewImage = null;
        try {
            File cacheDir = mCoordinator.getAppCacheDir();
            File previewImgFile = new File(cacheDir, name + "_preview.png");
            if (previewImgFile.exists()) {
                previewImage = BitmapFactory.decodeFile(previewImgFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOG.warn("Couldn't load watch app preview image", e);
        }
        return previewImage;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case ACTION_REFRESH_APPLIST: {
                    if (intent.hasExtra("app_count")) {
                        LOG.info("got app info from device");
                        if (!isCacheManager()) {
                            LOG.info("will refresh list based on data from device");
                            refreshListFromDevice(intent);
                        }
                    } else if (mCoordinator.supportsAppListFetching(mGBDevice)) {
                        refreshList();
                    } else if (isCacheManager()) {
                        refreshList();
                    }
                    mGBDeviceAppAdapter.notifyDataSetChanged();
                    break;
                }
                case QHYBRID_ACTION_DOWNLOADED_FILE: {
                    if (!intent.getBooleanExtra("EXTRA_SUCCESS", false)) {
                        LOG.warn("wapp download was not successful");
                        GB.toast(context.getString(R.string.appmanager_download_app_error), Toast.LENGTH_LONG, GB.ERROR);
                        break;
                    }
                    if (!intent.getBooleanExtra("EXTRA_TOCACHE", false)) {
                        break;
                    }
                    String path = intent.getStringExtra("EXTRA_PATH");
                    String name = intent.getStringExtra("EXTRA_NAME");
                    LOG.info("Attempting to add downloaded app {} to cache", name);
                    FossilFileReader fileReader;
                    try {
                        fileReader = new FossilFileReader(Uri.fromFile(new File(path)), context);
                    } catch (IOException e) {
                        LOG.warn("Could not find downloaded wapp", e);
                        break;
                    }
                    if (FossilHRInstallHandler.saveAppInCache(fileReader, fileReader.getBackground(), fileReader.getPreview(), mCoordinator, context)) {
                        LOG.info("Successfully moved downloaded app {} to cache", name);
                        GB.toast(String.format(context.getString(R.string.appmanager_downloaded_to_cache), name), Toast.LENGTH_LONG, GB.INFO);
                        if (isCacheManager()) {
                            refreshList();
                            mGBDeviceAppAdapter.notifyDataSetChanged();
                        }
                        (new File(path)).delete();
                    } else {
                        LOG.warn("Parsing downloaded wapp was not successful");
                        GB.toast(context.getString(R.string.appmanager_download_app_error), Toast.LENGTH_LONG, GB.ERROR);
                    }
                    break;
                }
            }
        }
    };

    protected Map<UUID, GBDeviceApp> getCachedAppsMap(final Collection<UUID> uuids) {
        final List<GBDeviceApp> cachedApps = getCachedApps(uuids);
        final Map<UUID, GBDeviceApp> cachedAppsMap = new HashMap<>();
        for (GBDeviceApp cachedApp : cachedApps) {
            cachedAppsMap.put(cachedApp.getUUID(), cachedApp);
        }
        return cachedAppsMap;
    }

    protected List<GBDeviceApp> getCachedApps(Collection<UUID> uuids) {
        List<GBDeviceApp> cachedAppList = new ArrayList<>();
        File cachePath;
        try {
            cachePath = mCoordinator.getAppCacheDir();
        } catch (IOException e) {
            LOG.warn("could not get external dir while reading app cache.");
            return cachedAppList;
        }

        if (cachePath == null) {
            LOG.warn("Cached apps path is null");
            return Collections.emptyList();
        }

        File[] files;
        if (uuids == null) {
            files = cachePath.listFiles();
        } else {
            files = new File[uuids.size()];
            int index = 0;
            for (UUID uuid : uuids) {
                files[index++] = new File(uuid.toString() + mCoordinator.getAppFileExtension());
            }
        }
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(mCoordinator.getAppFileExtension())) {
                    String baseName = file.getName().substring(0, file.getName().length() - mCoordinator.getAppFileExtension().length());
                    //metadata
                    File jsonFile = new File(cachePath, baseName + ".json");
                    //configuration
                    File configFile = new File(cachePath, baseName + "_config.js");
                    try {
                        String jsonstring = FileUtils.getStringFromFile(jsonFile);
                        JSONObject json = new JSONObject(jsonstring);
                        GBDeviceApp app = new GBDeviceApp(json, configFile.exists(), getAppPreviewImage(baseName));
                        if (mGBDevice.getType() == DeviceType.FOSSILQHYBRID) {
                            if ((app.getType() == GBDeviceApp.Type.WATCHFACE) && (!QHybridConstants.HYBRIDHR_WATCHFACE_VERSION.equals(app.getVersion()))) {
                                app.setUpToDate(false);
                            }
                            try {
                                if ((app.getType() == GBDeviceApp.Type.APP_GENERIC) && ((new Version(app.getVersion())).smallerThan(new Version(QHybridConstants.KNOWN_WAPP_VERSIONS.get(app.getName()))))) {
                                    app.setUpToDate(false);
                                }
                            } catch (IllegalArgumentException e) {
                                LOG.warn("Couldn't read app version", e);
                            }
                        }
                        cachedAppList.add(app);
                    } catch (Exception e) {
                        LOG.info("could not read json file for " + baseName);
                        if (mGBDevice.getType() == DeviceType.PEBBLE) {
                            //FIXME: this is really ugly, if we do not find system uuids in pbw cache add them manually. Also duplicated code
                            switch (baseName) {
                                case "8f3c8686-31a1-4f5f-91f5-01600c9bdc59":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Tic Toc (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
                                    break;
                                case "1f03293d-47af-4f28-b960-f2b02a6dd757":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Music (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    break;
                                case "b2cae818-10f8-46df-ad2b-98ad2254a3c1":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Notifications (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    break;
                                case "67a32d95-ef69-46d4-a0b9-854cc62f97f9":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Alarms (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    break;
                                case "18e443ce-38fd-47c8-84d5-6d0c775fbe55":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Watchfaces (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    break;
                                case "0863fc6a-66c5-4f62-ab8a-82ed00a98b5d":
                                    cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Send Text (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    break;
                            }
                            /*
                            else if (baseName.equals("4dab81a6-d2fc-458a-992c-7a1f3b96a970")) {
                                cachedAppList.add(new GBDeviceApp(UUID.fromString("4dab81a6-d2fc-458a-992c-7a1f3b96a970"), "Sports (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                            } else if (baseName.equals("cf1e816a-9db0-4511-bbb8-f60c48ca8fac")) {
                                cachedAppList.add(new GBDeviceApp(UUID.fromString("cf1e816a-9db0-4511-bbb8-f60c48ca8fac"), "Golf (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                            }
                            */
                            if (mGBDevice != null) {
                                if (PebbleUtils.hasHealth(mGBDevice.getModel())) {
                                    if (baseName.equals(PebbleProtocol.UUID_PEBBLE_HEALTH.toString())) {
                                        cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_PEBBLE_HEALTH, "Health (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                        continue;
                                    }
                                }
                                if (PebbleUtils.hasHRM(mGBDevice.getModel())) {
                                    if (baseName.equals(PebbleProtocol.UUID_WORKOUT.toString())) {
                                        cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_WORKOUT, "Workout (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                        continue;
                                    }
                                }
                                if (PebbleUtils.getFwMajor(mGBDevice.getFirmwareVersion()) >= 4) {
                                    if (baseName.equals("3af858c3-16cb-4561-91e7-f1ad2df8725f")) {
                                        cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), "Kickstart (System)", "Pebble Inc.", "", GBDeviceApp.Type.WATCHFACE_SYSTEM));
                                    }
                                    if (baseName.equals(PebbleProtocol.UUID_WEATHER.toString())) {
                                        cachedAppList.add(new GBDeviceApp(PebbleProtocol.UUID_WEATHER, "Weather (System)", "Pebble Inc.", "", GBDeviceApp.Type.APP_SYSTEM));
                                    }
                                }
                            }
                            if (uuids == null) {
                                cachedAppList.add(new GBDeviceApp(UUID.fromString(baseName), baseName, "N/A", "", GBDeviceApp.Type.UNKNOWN));
                            }
                        }
                    }
                }
            }
        }
        return cachedAppList;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REFRESH_APPLIST);
        filter.addAction(QHYBRID_ACTION_DOWNLOADED_FILE);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);

        if (mCoordinator.supportsAppListFetching(mGBDevice)) {
            GBApplication.deviceService(mGBDevice).onAppInfoReq();
            if (isCacheManager()) {
                refreshList();
            }
        } else {
            refreshList();
        }

        try {
            File appCacheDir = mCoordinator.getAppCacheDir();
            File appTempDir = new File(appCacheDir, "temp_sharing");
            if (appTempDir.isDirectory()) {
                for (File child : appTempDir.listFiles())
                    child.delete();
                appTempDir.delete();
            }
        } catch (IOException e) {
            LOG.warn("Could not delete temporary app cache directory", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mGBDevice = ((AppManagerActivity) getActivity()).getGBDevice();
        mCoordinator = mGBDevice.getDeviceCoordinator();

        final FloatingActionButton appListFab = ((FloatingActionButton) getActivity().findViewById(R.id.fab));
        final FloatingActionButton appListFabNew = ((FloatingActionButton) getActivity().findViewById(R.id.fab_new));
        watchfaceDesignerActivity = mCoordinator.getWatchfaceDesignerActivity(mGBDevice);
        View rootView = inflater.inflate(R.layout.activity_appmanager, container, false);

        RecyclerView appListView = (RecyclerView) (rootView.findViewById(R.id.appListView));

        appListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    appListFab.hide();
                    appListFabNew.hide();
                } else if (dy < 0) {
                    if (mCoordinator.supportsFlashing(mGBDevice)) {
                        appListFab.show();
                    }
                    if (watchfaceDesignerActivity != null) {
                        appListFabNew.show();
                    }
                }
            }
        });
        appListView.setLayoutManager(new GridAutoFitLayoutManager(getActivity(), 300));
        mGBDeviceAppAdapter = new GBDeviceAppAdapter(
                appList,
                R.layout.item_appmanager_watchapp,
                this,
                mCoordinator.supportsAppReordering(mGBDevice) || isCacheManager()
        );
        appListView.setAdapter(mGBDeviceAppAdapter);

        ItemTouchHelper.Callback appItemTouchHelperCallback = new AppItemTouchHelperCallback(mGBDeviceAppAdapter);
        appManagementTouchHelper = new ItemTouchHelper(appItemTouchHelperCallback);

        appManagementTouchHelper.attachToRecyclerView(appListView);

        if ((watchfaceDesignerActivity != null) && (appListFabNew != null)) {
            appListFabNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startIntent = new Intent(getContext(), watchfaceDesignerActivity);
                    startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
                    getContext().startActivity(startIntent);
                }
            });
            appListFabNew.show();
        }

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == CHILD_ACTIVITY_WATCHFACE_EDITOR) {
            refreshList();
        }
    }

    protected void sendOrderToDevice(String concatFilename) {
        ArrayList<UUID> uuids = new ArrayList<>();
        for (GBDeviceApp gbDeviceApp : mGBDeviceAppAdapter.getAppList()) {
            uuids.add(gbDeviceApp.getUUID());
        }
        if (concatFilename != null) {
            ArrayList<UUID> concatUuids = AppManagerActivity.getUuidsFromFile(concatFilename);
            uuids.addAll(concatUuids);
        }
        GBApplication.deviceService(mGBDevice).onAppReorder(uuids.toArray(new UUID[uuids.size()]));
    }

    public void onItemClick(View view, GBDeviceApp deviceApp) {
        openPopupMenu(view, deviceApp);
    }

    public boolean openPopupMenu(View view, GBDeviceApp deviceApp) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.appmanager_context, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();
        final GBDeviceApp selectedApp = deviceApp;

        if (!selectedApp.isOnDevice() || selectedApp.getType() != GBDeviceApp.Type.WATCHFACE) {
            menu.removeItem(R.id.appmanager_watchface_activate);
        }
        if (!selectedApp.isOnDevice() || selectedApp.getType() != GBDeviceApp.Type.APP_GENERIC) {
            menu.removeItem(R.id.appmanager_app_start);
        }
        if (!selectedApp.isInCache()) {
            menu.removeItem(R.id.appmanager_app_edit);
            menu.removeItem(R.id.appmanager_app_reinstall);
            menu.removeItem(R.id.appmanager_app_share);
            menu.removeItem(R.id.appmanager_app_delete_cache);
        }
        if (!PebbleProtocol.UUID_PEBBLE_HEALTH.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_health_activate);
            menu.removeItem(R.id.appmanager_health_deactivate);
        }
        if (!PebbleProtocol.UUID_WORKOUT.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_hrm_activate);
            menu.removeItem(R.id.appmanager_hrm_deactivate);
        }
        if (!PebbleProtocol.UUID_WEATHER.equals(selectedApp.getUUID())) {
            menu.removeItem(R.id.appmanager_weather_activate);
            menu.removeItem(R.id.appmanager_weather_deactivate);
        }
        if (selectedApp.getType() == GBDeviceApp.Type.APP_SYSTEM || selectedApp.getType() == GBDeviceApp.Type.WATCHFACE_SYSTEM) {
            menu.removeItem(R.id.appmanager_app_delete);
        }
        if (!selectedApp.isConfigurable()) {
            menu.removeItem(R.id.appmanager_app_configure);
        }

        if ((mGBDevice.getType() != DeviceType.FOSSILQHYBRID) || (selectedApp.getType() != GBDeviceApp.Type.WATCHFACE)) {
            menu.removeItem(R.id.appmanager_app_edit);
        }
        if ((mGBDevice.getType() != DeviceType.FOSSILQHYBRID) || (!selectedApp.isOnDevice()) || ((selectedApp.getType() != GBDeviceApp.Type.WATCHFACE) && (selectedApp.getType() != GBDeviceApp.Type.APP_GENERIC))) {
            menu.removeItem(R.id.appmanager_app_download);
        }
        if (mGBDevice.getType() == DeviceType.FOSSILQHYBRID && selectedApp.getName().equals("workoutApp")) {
            menu.removeItem(R.id.appmanager_app_delete);
        }

        if (mGBDevice.getType() == DeviceType.PEBBLE) {
            switch (selectedApp.getType()) {
                case WATCHFACE:
                case APP_GENERIC:
                case APP_ACTIVITYTRACKER:
                    break;
                default:
                    menu.removeItem(R.id.appmanager_app_openinstore);
            }
        } else {
            menu.removeItem(R.id.appmanager_app_openinstore);
        }
        //menu.setHeaderTitle(selectedApp.getName());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                 public boolean onMenuItemClick(MenuItem item) {
                                                     return onContextItemSelected(item, selectedApp);
                                                 }
                                             }
        );

        popupMenu.show();
        return true;
    }

    private boolean onContextItemSelected(final MenuItem item, final GBDeviceApp selectedApp) {
        final File appCacheDir;
        try {
            appCacheDir = mCoordinator.getAppCacheDir();
        } catch (final IOException e) {
            LOG.warn("could not get external dir while trying to access app cache.");
            return true;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.appmanager_app_delete_cache) {
            deleteAppConfirm(selectedApp, true);
            return true;
        } else if (itemId == R.id.appmanager_app_delete) {
            deleteAppConfirm(selectedApp, false);
            return true;
        } else if (itemId == R.id.appmanager_app_start || itemId == R.id.appmanager_watchface_activate) {
            GBApplication.deviceService(mGBDevice).onAppStart(selectedApp.getUUID(), true);
            return true;
        } else if (itemId == R.id.appmanager_app_download) {
            GBApplication.deviceService(mGBDevice).onAppDownload(selectedApp.getUUID());
            GB.toast(requireContext().getString(R.string.appmanager_download_started), Toast.LENGTH_LONG, GB.INFO);
            return true;
        } else if (itemId == R.id.appmanager_app_reinstall) {
            final File cachePath = new File(appCacheDir, selectedApp.getUUID() + mCoordinator.getAppFileExtension());
            GBApplication.deviceService(mGBDevice).onInstallApp(Uri.fromFile(cachePath), Bundle.EMPTY);
            return true;
        } else if (itemId == R.id.appmanager_app_share) {
            final File origFilePath = new File(appCacheDir, selectedApp.getUUID() + mCoordinator.getAppFileExtension());
            final File appTempDir = new File(appCacheDir, "temp_sharing");
            final File sharedAppFile = new File(appTempDir, selectedApp.getName() + mCoordinator.getAppFileExtension());
            try {
                appTempDir.mkdirs();
                FileUtils.copyFile(origFilePath, sharedAppFile);
            } catch (final IOException e) {
                return true;
            }
            final Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".screenshot_provider", sharedAppFile);
            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType("*/*");
            try {
                startActivity(Intent.createChooser(shareIntent, null));
            } catch (ActivityNotFoundException e) {
                LOG.warn("Sharing watchface failed", e);
            }
            return true;
        } else if (itemId == R.id.appmanager_health_activate) {
            GBApplication.deviceService(mGBDevice).onInstallApp(Uri.parse("fake://health"), Bundle.EMPTY);
            return true;
        } else if (itemId == R.id.appmanager_hrm_activate) {
            GBApplication.deviceService(mGBDevice).onInstallApp(Uri.parse("fake://hrm"), Bundle.EMPTY);
            return true;
        } else if (itemId == R.id.appmanager_weather_activate) {
            GBApplication.deviceService(mGBDevice).onInstallApp(Uri.parse("fake://weather"), Bundle.EMPTY);
            return true;
        } else if (itemId == R.id.appmanager_health_deactivate || itemId == R.id.appmanager_hrm_deactivate || itemId == R.id.appmanager_weather_deactivate) {
            GBApplication.deviceService(mGBDevice).onAppDelete(selectedApp.getUUID());
            return true;
        } else if (itemId == R.id.appmanager_app_configure) {
            GBApplication.deviceService(mGBDevice).onAppStart(selectedApp.getUUID(), true);

            final Intent startIntent = new Intent(getContext().getApplicationContext(), ExternalPebbleJSActivity.class);
            startIntent.putExtra(DeviceService.EXTRA_APP_UUID, selectedApp.getUUID());
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
            startIntent.putExtra(ExternalPebbleJSActivity.SHOW_CONFIG, true);
            startActivity(startIntent);
            return true;
        } else if (itemId == R.id.appmanager_app_openinstore) {
            final String url = "https://apps.rebble.io/en_US/search/" + ((selectedApp.getType() == GBDeviceApp.Type.WATCHFACE) ? "watchfaces" : "watchapps") + "/1/?native=true&?query=" +  Uri.encode(selectedApp.getName());
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            return true;
        } else if (itemId == R.id.appmanager_app_edit) {
            final Intent editWatchfaceIntent = new Intent(getContext(), watchfaceDesignerActivity);
            editWatchfaceIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
            editWatchfaceIntent.putExtra(GBDevice.EXTRA_UUID, selectedApp.getUUID().toString());
            startActivityForResult(editWatchfaceIntent, CHILD_ACTIVITY_WATCHFACE_EDITOR);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void deleteAppConfirm(final GBDeviceApp selectedApp, final boolean deleteFromCache) {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.Delete)
                .setMessage(requireContext().getString(R.string.contact_delete_confirm_description, selectedApp.getName()))
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    if (deleteFromCache) {
                        deleteAppFromCache(selectedApp);
                    }
                    deleteAppFromDevice(selectedApp);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteAppFromCache(final GBDeviceApp selectedApp) {
        final File appCacheDir;
        try {
            appCacheDir = mCoordinator.getAppCacheDir();
        } catch (final IOException e) {
            LOG.warn("Could not get external dir while trying to access app cache", e);
            return;
        }

        String baseName = selectedApp.getUUID().toString();
        String[] suffixToDelete = new String[]{mCoordinator.getAppFileExtension(), ".json", "_config.js", "_preset.json", ".png", "_preview.png", "_bg.png"};
        for (String suffix : suffixToDelete) {
            File fileToDelete = new File(appCacheDir, baseName + suffix);
            if (!fileToDelete.delete()) {
                LOG.warn("Could not delete file from app cache: {}", fileToDelete);
            } else {
                LOG.debug("Deleted from app cache: {}", fileToDelete);
            }
        }
        AppManagerActivity.deleteFromAppOrderFile(getSortFilename(), selectedApp.getUUID()); // FIXME: only if successful
        Intent refreshIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(refreshIntent);
    }

    private void deleteAppFromDevice(final GBDeviceApp selectedApp) {
        if (mCoordinator.supportsAppReordering(mGBDevice)) {
            AppManagerActivity.deleteFromAppOrderFile(mGBDevice.getAddress() + ".watchapps", selectedApp.getUUID()); // FIXME: only if successful
            AppManagerActivity.deleteFromAppOrderFile(mGBDevice.getAddress() + ".watchfaces", selectedApp.getUUID()); // FIXME: only if successful
            Intent refreshIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(refreshIntent);
        }

        GBApplication.deviceService(mGBDevice).onAppDelete(selectedApp.getUUID());
    }

    /**
     * Sort an app list by the UUIDs in the sort filename.
     *
     * @param appList the app list to sort in-place
     */
    protected void sortAppList(final List<GBDeviceApp> appList) {
        final ArrayList<UUID> uuids = AppManagerActivity.getUuidsFromFile(getSortFilename());
        final Map<UUID, Integer> uuidPosMap = new HashMap<>();
        for (int i = 0; i < uuids.size(); i++) {
            uuidPosMap.put(uuids.get(i), i);
        }

        Collections.sort(appList, (a1, a2) -> {
            final Integer pos1 = uuidPosMap.get(a1.getUUID());
            final Integer pos2 = uuidPosMap.get(a2.getUUID());

            if (pos1 != null && pos2 != null) return Integer.compare(pos1, pos2);
            if (pos1 == null && pos2 == null) return a1.getName().compareToIgnoreCase(a2.getName());
            if (pos1 != null) return -1;

            return 1;
        });
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    public class AppItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final GBDeviceAppAdapter gbDeviceAppAdapter;

        public AppItemTouchHelperCallback(GBDeviceAppAdapter gbDeviceAppAdapter) {
            this.gbDeviceAppAdapter = gbDeviceAppAdapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (!mCoordinator.supportsAppReordering(mGBDevice) && !isCacheManager()) {
                return 0;
            }
            //we only support up and down movement and only for moving, not for swiping apps away
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            gbDeviceAppAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            //nothing to do
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            onChangedAppOrder();
        }

    }
}
