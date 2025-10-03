/*  Copyright (C) 2017-2024 Daniel Dakhno, Daniele Gobbetti, José Rebelo,
    Quallenauge

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
* @author Quallenauge &lt;Hamsi2k@freenet.de&gt;
*/

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hplus.EXRIZUK8Support;

/**
 * Pseudo Coordinator for the EXRIZU K8, a sub type of the HPLUS devices
 */
public class EXRIZUK8Coordinator extends HPlusCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("iRun .*");
    }

    @Override
    public String getManufacturer() {
        return "Exrizu";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_exrizu_k8;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return EXRIZUK8Support.class;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.FITNESS_BAND;
    }
}
