/*  Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;

public class HybridHRWatchfaceSettingsActivity extends AbstractSettingsActivityV2 {
    static HybridHRWatchfaceSettings settings;

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new HybridHRWatchfaceSettingsFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            settings = (HybridHRWatchfaceSettings) bundle.getSerializable("watchfaceSettings");
        } else {
            throw new IllegalArgumentException("Must provide a settings object when invoking this activity");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        // Hardware back button
        Intent output = new Intent();
        output.putExtra("watchfaceSettings", settings);
        setResult(RESULT_OK, output);
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Action bar back button
            Intent output = new Intent();
            output.putExtra("watchfaceSettings", settings);
            setResult(RESULT_OK, output);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class HybridHRWatchfaceSettingsFragment extends AbstractPreferenceFragment implements Preference.OnPreferenceChangeListener {
        static final String FRAGMENT_TAG = "HYBRID_HR_WATCHFACE_SETTINGS_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.fossil_hr_watchface_settings, rootKey);

            EditTextPreference refresh_full = findPreference("pref_hybridhr_watchface_refresh_full");
            refresh_full.setOnPreferenceChangeListener(this);
            refresh_full.setText(Integer.toString(settings.getDisplayTimeoutFull()));
            refresh_full.setSummary(Integer.toString(settings.getDisplayTimeoutFull()));
            setInputTypeFor("pref_hybridhr_watchface_refresh_full", InputType.TYPE_CLASS_NUMBER);

            EditTextPreference refresh_partial = findPreference("pref_hybridhr_watchface_refresh_partial");
            refresh_partial.setOnPreferenceChangeListener(this);
            refresh_partial.setText(Integer.toString(settings.getDisplayTimeoutPartial()));
            refresh_partial.setSummary(Integer.toString(settings.getDisplayTimeoutPartial()));
            setInputTypeFor("pref_hybridhr_watchface_refresh_partial", InputType.TYPE_CLASS_NUMBER);

            SwitchPreferenceCompat wrist_flick_relative = findPreference("pref_hybridhr_watchface_wrist_flick_relative");
            wrist_flick_relative.setOnPreferenceChangeListener(this);
            wrist_flick_relative.setChecked(settings.isWristFlickHandsMoveRelative());

            EditTextPreference wrist_flick_hour_hand = findPreference("pref_hybridhr_watchface_wrist_flick_hour_hand");
            wrist_flick_hour_hand.setOnPreferenceChangeListener(this);
            wrist_flick_hour_hand.setText(Integer.toString(settings.getWristFlickMoveHour()));
            wrist_flick_hour_hand.setSummary(Integer.toString(settings.getWristFlickMoveHour()));
            setInputTypeFor("pref_hybridhr_watchface_wrist_flick_hour_hand", InputType.TYPE_CLASS_NUMBER);

            EditTextPreference wrist_flick_minute_hand = findPreference("pref_hybridhr_watchface_wrist_flick_minute_hand");
            wrist_flick_minute_hand.setOnPreferenceChangeListener(this);
            wrist_flick_minute_hand.setText(Integer.toString(settings.getWristFlickMoveMinute()));
            wrist_flick_minute_hand.setSummary(Integer.toString(settings.getWristFlickMoveMinute()));
            setInputTypeFor("pref_hybridhr_watchface_wrist_flick_minute_hand", InputType.TYPE_CLASS_NUMBER);

            EditTextPreference wrist_flick_duration = findPreference("pref_hybridhr_watchface_wrist_flick_duration");
            wrist_flick_duration.setOnPreferenceChangeListener(this);
            wrist_flick_duration.setText(Integer.toString(settings.getWristFlickDuration()));
            wrist_flick_duration.setSummary(Integer.toString(settings.getWristFlickDuration()));
            setInputTypeFor("pref_hybridhr_watchface_wrist_flick_duration", InputType.TYPE_CLASS_NUMBER);

            ListPreference toggle_widgets = findPreference("pref_hybridhr_watchface_toggle_widgets");
            toggle_widgets.setOnPreferenceChangeListener(this);
            toggle_widgets.setValue(settings.getToggleWidgetsEvent());
            toggle_widgets.setSummary(toggle_widgets.getEntry());

            ListPreference toggle_backlight = findPreference("pref_hybridhr_watchface_toggle_backlight");
            toggle_backlight.setOnPreferenceChangeListener(this);
            toggle_backlight.setValue(settings.getToggleBacklightEvent());
            toggle_backlight.setSummary(toggle_backlight.getEntry());

            ListPreference move_hands = findPreference("pref_hybridhr_watchface_move_hands");
            move_hands.setOnPreferenceChangeListener(this);
            move_hands.setValue(settings.getMoveHandsEvent());
            move_hands.setSummary(move_hands.getEntry());

            SwitchPreferenceCompat power_saving_display = findPreference("pref_hybridhr_watchface_power_saving_display");
            power_saving_display.setOnPreferenceChangeListener(this);
            power_saving_display.setChecked(settings.getPowersaveDisplay());

            SwitchPreferenceCompat power_saving_hands = findPreference("pref_hybridhr_watchface_power_saving_hands");
            power_saving_hands.setOnPreferenceChangeListener(this);
            power_saving_hands.setChecked(settings.getPowersaveHands());

            SwitchPreferenceCompat light_up_on_notification = findPreference("pref_hybridhr_watchface_light_up_on_notification");
            light_up_on_notification.setOnPreferenceChangeListener(this);
            light_up_on_notification.setChecked(settings.getLightUpOnNotification());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "pref_hybridhr_watchface_refresh_full":
                    settings.setDisplayTimeoutFull(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_watchface_refresh_partial":
                    settings.setDisplayTimeoutPartial(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_watchface_wrist_flick_relative":
                    settings.setWristFlickHandsMoveRelative((boolean) newValue);
                    break;
                case "pref_hybridhr_watchface_wrist_flick_hour_hand":
                    settings.setWristFlickMoveHour(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_watchface_wrist_flick_minute_hand":
                    settings.setWristFlickMoveMinute(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_watchface_wrist_flick_duration":
                    settings.setWristFlickDuration(Integer.parseInt(newValue.toString()));
                    preference.setSummary(newValue.toString());
                    break;
                case "pref_hybridhr_watchface_toggle_widgets":
                    settings.setToggleWidgetsEvent(newValue.toString());
                    ((ListPreference)preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference)preference).getEntry());
                    break;
                case "pref_hybridhr_watchface_toggle_backlight":
                    settings.setToggleBacklightEvent(newValue.toString());
                    ((ListPreference)preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference)preference).getEntry());
                    break;
                case "pref_hybridhr_watchface_move_hands":
                    settings.setMoveHandsEvent(newValue.toString());
                    ((ListPreference)preference).setValue(newValue.toString());
                    preference.setSummary(((ListPreference)preference).getEntry());
                    break;
                case "pref_hybridhr_watchface_power_saving_display":
                    settings.setPowersaveDisplay((boolean) newValue);
                    break;
                case "pref_hybridhr_watchface_power_saving_hands":
                    settings.setPowersaveHands((boolean) newValue);
                    break;
                case "pref_hybridhr_watchface_light_up_on_notification":
                    settings.setLightUpOnNotification((boolean) newValue);
                    break;
            }
            return true;
        }
    }
}
