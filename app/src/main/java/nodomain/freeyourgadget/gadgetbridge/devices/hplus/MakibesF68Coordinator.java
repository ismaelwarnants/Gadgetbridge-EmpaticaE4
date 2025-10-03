/*  Copyright (C) 2017-2024 Daniel Dakhno, Daniele Gobbetti, João Paulo
    Barraca, José Rebelo, Stan Gomin

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
package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hplus.MakibesF68Support;

/**
 * Pseudo Coordinator for the Makibes F68, a sub type of the HPLUS devices
 */
public class MakibesF68Coordinator extends HPlusCoordinator {
    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        String name = candidate.getName();
        return name != null && name.startsWith("SPORT") && !name.startsWith("SPORTAGE");
    }

    @Override
    public String getManufacturer() {
        return "Makibes";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_makibes_f68;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return MakibesF68Support.class;
    }
}
