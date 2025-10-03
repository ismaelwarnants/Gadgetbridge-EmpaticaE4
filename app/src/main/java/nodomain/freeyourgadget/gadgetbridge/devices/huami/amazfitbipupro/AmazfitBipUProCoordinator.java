/*  Copyright (C) 2021-2024 Andreas Shimokawa, DanialHanif, Daniel Dakhno,
    José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipupro;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbipupro.AmazfitBipUProSupport;

public class AmazfitBipUProCoordinator extends HuamiCoordinator {
    @Override
    public String getManufacturer() {
        // Actual manufacturer is Huami
        return "Amazfit";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Amazfit Bip U Pro", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final AmazfitBipUProFWInstallHandler handler = new AmazfitBipUProFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return 0;
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
        generic.add(R.xml.devicesettings_device_actions);
        final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
        dateTime.add(R.xml.devicesettings_timeformat);
        dateTime.add(R.xml.devicesettings_world_clocks);
        final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
        display.add(R.xml.devicesettings_amazfitbipupro);
        display.add(R.xml.devicesettings_liftwrist_display_sensitivity);
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress);
        health.add(R.xml.devicesettings_inactivity_dnd);
        health.add(R.xml.devicesettings_goal_notification);
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
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "cs_CZ",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "pl_PL",
                "pt_BR",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CH",
                "zh_TW",
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return AmazfitBipUProSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_bipupro;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
