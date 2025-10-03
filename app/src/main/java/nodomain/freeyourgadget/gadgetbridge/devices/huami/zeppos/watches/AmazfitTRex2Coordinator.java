/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AmazfitTRex2Coordinator extends ZeppOsCoordinator {
    @Override
    public List<String> getDeviceBluetoothNames() {
        return Collections.singletonList("Amazfit T-Rex 2");
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(
                418, // chinese mainland version
                419
        ));
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_trex_2;
    }

    @Override
    public boolean supportsControlCenter() {
        return true;
    }

    @Override
    public boolean supportsToDoList() {
        return true;
    }

    @Override
    public boolean sendAgpsAsFileTransfer() {
        return false;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return false;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
