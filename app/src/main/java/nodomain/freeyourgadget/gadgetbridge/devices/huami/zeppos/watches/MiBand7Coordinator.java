/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.watches;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class MiBand7Coordinator extends ZeppOsCoordinator {
    @Override
    public String getManufacturer() {
        // Actual manufacturer is Huami
        return "Xiaomi";
    }

    @Override
    public List<String> getDeviceBluetoothNames() {
        return Collections.singletonList("Xiaomi Smart Band 7");
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(260, 262, 263, 264, 265));
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return new HashMap<Integer, String>() {{
            // firmware
            put(26036, "1.20.3.1");
            put(55449, "1.27.0.4");
            put(14502, "2.0.0.2");
            put(25658, "2.1.0.1");
        }};
    }

    @Override
    public boolean supportsAgpsUpdates() {
        return false;
    }

    @Override
    public boolean supportsScreenshots(@NonNull final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return false;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband7;
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
