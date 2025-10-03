/*  Copyright (C) 2018-2024 Daniel Dakhno, Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.roidmi;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi.RoidmiSupport;

public class Roidmi1Coordinator extends RoidmiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(Roidmi1Coordinator.class);

    @Override
    public boolean supports(@NonNull final GBDeviceCandidate candidate) {
        try {
            final String name = candidate.getName();

            if (name != null && name.contains("睿米车载蓝牙播放器")) {
                return true;
            }
        } catch (final Exception ex) {
            LOG.error("unable to check device support", ex);
        }

        return false;
    }

    @Override
    public int getBatteryCount(final GBDevice device) {
        // Roidmi 1 does not have voltage support
        return 0;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return RoidmiSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_roidmi;
    }
}
