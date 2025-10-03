/*  Copyright (C) 2022-2024 Andreas Shimokawa, Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitpop;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipu.AmazfitBipUCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitpop.AmazfitPopSupport;

public class AmazfitPopCoordinator extends AmazfitBipUCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Amazfit Pop", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final AmazfitPopFWInstallHandler handler = new AmazfitPopFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_pop;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return AmazfitPopSupport.class;
    }
}
