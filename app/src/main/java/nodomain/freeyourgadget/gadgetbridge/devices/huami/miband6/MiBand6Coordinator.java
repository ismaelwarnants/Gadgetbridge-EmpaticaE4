/*  Copyright (C) 2021-2024 Aidan Crane, Andreas Shimokawa, Daniel Dakhno,
    Daniele Gobbetti, jhey, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband6;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband6.MiBand6Support;

public class MiBand6Coordinator extends HuamiCoordinator {
    @Override
    public String getManufacturer() {
        // Actual manufacturer is Huami
        return "Xiaomi";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(HuamiConst.MI_BAND6_NAME, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final MiBand6FWInstallHandler handler = new MiBand6FWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPai(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 20; // as enforced by Mi Fit
    }

    @Override
    public int getWorldClocksLabelLength() {
        return 30; // at least
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        final List<Integer> generic = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.GENERIC);
        generic.add(R.xml.devicesettings_wearlocation);
        final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
        dateTime.add(R.xml.devicesettings_timeformat);
        dateTime.add(R.xml.devicesettings_dateformat);
        dateTime.add(R.xml.devicesettings_world_clocks);
        final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
        display.add(R.xml.devicesettings_miband6);
        display.add(R.xml.devicesettings_nightmode);
        display.add(R.xml.devicesettings_liftwrist_display_sensitivity);
        display.add(R.xml.devicesettings_swipeunlock);
        display.add(R.xml.devicesettings_password);
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress);
        health.add(R.xml.devicesettings_inactivity_dnd);
        health.add(R.xml.devicesettings_goal_notification);
        final List<Integer> workout = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT);
        workout.add(R.xml.devicesettings_workout_start_on_phone);
        workout.add(R.xml.devicesettings_workout_send_gps_to_band);
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_custom_emoji_font);
        notifications.add(R.xml.devicesettings_vibrationpatterns);
        notifications.add(R.xml.devicesettings_phone_silent_mode);
        notifications.add(R.xml.devicesettings_transliteration);
        final List<Integer> calendar = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALENDAR);
        calendar.add(R.xml.devicesettings_sync_calendar);
        calendar.add(R.xml.devicesettings_reserve_reminders_calendar);
        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_expose_hr_thirdparty);
        connection.add(R.xml.devicesettings_bt_connected_advertisement);
        connection.add(R.xml.devicesettings_high_mtu);
        connection.add(R.xml.devicesettings_overwrite_settings_on_connection);
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_huami2021_fetch_operation_time_unit);

        return deviceSpecificSettings;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey,
                R.xml.devicesettings_miband6_new_protocol,
                R.xml.devicesettings_miband6_new_auth_protocol_explanation,
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "cs_CZ",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "he_IL",
                "id_ID",
                "it_IT",
                "nl_NL",
                "pt_PT",
                "pl_PL",
                "ro_RO",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NUMBERS_6;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return MiBand6Support.class;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband6;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miband6;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.FITNESS_BAND;
    }
}
