/*  Copyright (C) 2016-2024 Andreas Shimokawa, Anemograph, Carsten Pfeiffer,
    Daniel Dakhno, Daniele Gobbetti, Davis Mosenkovs, Dikay900, Felix Konstantin
    Maurer, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.text.ParseException;
import java.time.LocalTime;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBPrefs extends Prefs {
    // Since this class must not log to slf4j, we use plain android.util.Log
    private static final String TAG = "GBPrefs";

    public static final String PACKAGE_BLACKLIST = "package_blacklist";
    public static final String PACKAGE_PEBBLEMSG_BLACKLIST = "package_pebblemsg_blacklist";
    public static final String CALENDAR_BLACKLIST = "calendar_blacklist";
    public static final String DEVICE_AUTO_RECONNECT = "prefs_key_device_auto_reconnect";
    public static final String DEVICE_CONNECT_BACK = "prefs_key_device_reconnect_on_acl";
    private static final String AUTO_START = "general_autostartonboot";
    public static final String AUTO_CONNECT_BLUETOOTH = "general_autoconnectonbluetooth";
    public static final String PING_TONE = "ping_tone";
    private static final boolean AUTO_START_DEFAULT = true;
    public static final String RTL_SUPPORT = "rtl";
    public static final String RTL_CONTEXTUAL_ARABIC = "contextualArabic";
    public static boolean AUTO_RECONNECT_DEFAULT = true;
    public static final String PREF_ALLOW_INTENT_API = "prefs_key_allow_bluetooth_intent_api";

    public static final String PREF_AUTO_FETCH_ENABLED = "auto_fetch_enabled";
    public static final String PREF_AUTO_FETCH_INTERVAL_LIMIT = "auto_fetch_interval_limit";

    // These should get the prefix appended - see below
    public static final String AUTO_EXPORT_ENABLED = "auto_export_enabled";
    public static final String AUTO_EXPORT_LOCATION = "auto_export_location";
    public static final String AUTO_EXPORT_INTERVAL = "auto_export_interval";
    public static final String AUTO_EXPORT_LAST_EXECUTION = "auto_export_last_execution";
    public static final String AUTO_EXPORT_NEXT_EXECUTION = "auto_export_next_execution";

    // DB export has no prefix
    public static final String AUTO_EXPORT_DB_ENABLED = AUTO_EXPORT_ENABLED;
    public static final String AUTO_EXPORT_DB_LOCATION = AUTO_EXPORT_LOCATION;
    public static final String AUTO_EXPORT_DB_INTERVAL = AUTO_EXPORT_INTERVAL;
    public static final String AUTO_EXPORT_DB_LAST_EXECUTION = AUTO_EXPORT_LAST_EXECUTION;
    public static final String AUTO_EXPORT_DB_NEXT_EXECUTION = AUTO_EXPORT_NEXT_EXECUTION;

    // Zip export with "zip_" prefix
    public static final String AUTO_EXPORT_ZIP_ENABLED = "zip_auto_export_enabled";
    public static final String AUTO_EXPORT_ZIP_LOCATION = "zip_auto_export_location";
    public static final String AUTO_EXPORT_ZIP_INTERVAL = "zip_auto_export_interval";
    public static final String AUTO_EXPORT_ZIP_LAST_EXECUTION = "zip_auto_export_last_execution";
    public static final String AUTO_EXPORT_ZIP_NEXT_EXECUTION = "zip_auto_export_next_execution";

    // Intent API
    public static final String INTENT_API_BROADCAST_EXPORT_DB = "intent_api_broadcast_export";
    public static final String INTENT_API_BROADCAST_EXPORT_ZIP = "intent_api_broadcast_zip_export";

    public static final String RECONNECT_SCAN_KEY = "prefs_general_key_auto_reconnect_scan";
    public static final boolean RECONNECT_SCAN_DEFAULT = false;

    public static final String USER_NAME = "mi_user_alias";
    public static final String USER_NAME_DEFAULT = "gadgetbridge-user";
    private static final String USER_BIRTHDAY = "";

    public static final String CHART_MAX_HEART_RATE = "chart_max_heart_rate";
    public static final String CHART_MIN_HEART_RATE = "chart_min_heart_rate";

    public static final String LAST_DEVICE_ADDRESSES = "last_device_addresses";
    public static final String RECONNECT_ONLY_TO_CONNECTED = "general_reconnectonlytoconnected";
    public static final String BLOCK_SCREENSHOTS = "block_screenshots";

    @Deprecated
    public GBPrefs(Prefs prefs) {
        this(prefs.getPreferences());
    }

    public GBPrefs(final SharedPreferences sharedPrefs) {
        super(sharedPrefs);
    }

    public boolean getAutoReconnect(GBDevice device) {
        SharedPreferences deviceSpecificPreferences = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
        return deviceSpecificPreferences.getBoolean(DEVICE_AUTO_RECONNECT, AUTO_RECONNECT_DEFAULT);
    }

    public boolean getAutoReconnectByScan() {
        return getBoolean(RECONNECT_SCAN_KEY, RECONNECT_SCAN_DEFAULT);
    }

    public boolean getAutoStart() {
        return getBoolean(AUTO_START, AUTO_START_DEFAULT);
    }

    public String getUserName() {
        return getString(USER_NAME, USER_NAME_DEFAULT);
    }

    @Nullable
    public Date getUserBirthday() {
        String date = getString(USER_BIRTHDAY, null);
        if (date == null) {
            return null;
        }
        try {
            return DateTimeUtils.dayFromString(date);
        } catch (ParseException ex) {
            GB.log("Error parsing date: " + date, GB.ERROR, ex);
            return null;
        }
    }

    public int getUserGender() {
        return 0;
    }

    public float[] getLongLat(Context context) {
        float latitude = getFloat("location_latitude", 0);
        float longitude = getFloat("location_longitude", 0);
        Log.i(TAG, "got longitude/latitude from preferences: " + latitude + "/" + longitude);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                getBoolean("use_updated_location_if_available", false)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            if (provider != null) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    latitude = (float) lastKnownLocation.getLatitude();
                    longitude = (float) lastKnownLocation.getLongitude();
                    Log.i(TAG, "got longitude/latitude from last known location: " + latitude + "/" + longitude);
                }
            }
        }
        return new float[]{longitude, latitude};
    }

    public boolean getNotificationTimesEnabled() {
        return getBoolean("notification_times_enabled", false);
    }

    public LocalTime getNotificationTimesStart() {
        return getLocalTime("notification_times_start", "08:00");
    }

    public LocalTime getNotificationTimesEnd() {
        return getLocalTime("notification_times_end", "22:00");
    }

    public boolean isMetricUnits() {
        return getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric").equals("metric");
    }

    public boolean syncTime() {
        return getBoolean("datetime_synconconnect", true);
    }

    public boolean refreshOnSwipe() {
        return getBoolean("pref_refresh_on_swipe", true);
    }
}
