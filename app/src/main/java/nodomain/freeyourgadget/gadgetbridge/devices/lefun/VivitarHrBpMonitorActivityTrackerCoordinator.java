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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class VivitarHrBpMonitorActivityTrackerCoordinator extends LefunDeviceCoordinator {
    @NonNull
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^IMP-2027$");
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_liftwrist_display_noshed,
                R.xml.devicesettings_timeformat,
                // R.xml.devicesettings_antilost, not supported
                R.xml.devicesettings_inactivity,
                R.xml.devicesettings_hydration_reminder,
                R.xml.devicesettings_lefun_interface_language,
                R.xml.devicesettings_transliteration
        };
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return false;  // not supported
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_vivitar_hr_bp_monitor_activity_tracker;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.FITNESS_BAND;
    }
}
