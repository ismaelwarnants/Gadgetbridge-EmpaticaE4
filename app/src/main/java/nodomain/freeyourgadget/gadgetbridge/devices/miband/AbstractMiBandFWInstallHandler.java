/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType.AGPS_UIHH;

import androidx.annotation.NonNull;

public abstract class AbstractMiBandFWInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMiBandFWInstallHandler.class);

    protected final Uri mUri;
    protected final Context mContext;
    protected AbstractMiBandFWHelper helper;

    public AbstractMiBandFWInstallHandler(Uri uri, Context context) {
        mUri = uri;
        mContext = context;

        try {
            helper = createHelper(uri, context);
        } catch (IOException e) {
            LOG.error("Failed to open as miband fw", e);
        }
    }

    public Uri getUri() {
        return mUri;
    }

    public Context getContext() {
        return mContext;
    }

    public AbstractMiBandFWHelper getHelper() {
        return helper;
    }

    protected abstract AbstractMiBandFWHelper createHelper(Uri uri, Context context) throws IOException;

    private GenericItem createInstallItem(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return new GenericItem(mContext.getString(R.string.installhandler_firmware_name, mContext.getString(coordinator.getDeviceNameResource()), helper.getFirmwareKind(), helper.getHumanFirmwareVersion()));
    }

    protected String getFwUpgradeNotice() {
        return mContext.getString(R.string.fw_upgrade_notice, helper.getHumanFirmwareVersion());
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!isSupportedDeviceType(device)) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        } else if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        try {
            helper.checkValid();
        } catch (IllegalArgumentException ex) {
            installActivity.setInfoText(ex.getLocalizedMessage());
            installActivity.setInstallEnabled(false);
            return;
        }

        DeviceCoordinator coordinator = device.getDeviceCoordinator();

        GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(coordinator.getDefaultIconResource());

        if (!helper.isFirmwareGenerallyCompatibleWith(device)) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (!helper.getFirmwareType().isWatchface() && !helper.getFirmwareType().isApp() && helper.getFirmwareType() != AGPS_UIHH) {
            if (helper.isSingleFirmware()) {
                builder.append(getFwUpgradeNotice());
            } else {
                builder.append(mContext.getString(R.string.fw_multi_upgrade_notice, helper.getHumanFirmwareVersion(), helper.getHumanFirmwareVersion2()));
            }


            if (helper.isFirmwareWhitelisted()) {
                builder.append(" ").append(mContext.getString(R.string.miband_firmware_known));
                fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_compatible_version));
                // TODO: set a CHECK (OKAY) button
            } else {
                builder.append("  ").append(mContext.getString(R.string.miband_firmware_unknown_warning)).append(" \n\n")
                        .append(mContext.getString(R.string.miband_firmware_suggest_whitelist, String.valueOf(helper.getFirmwareVersion())));
                fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_untested_version));
                // TODO: set a UNKNOWN (question mark) button
            }
        }

        installActivity.setPreview(helper.getPreview());

        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    protected abstract boolean isSupportedDeviceType(GBDevice device);

    @Override
    public void onStartInstall(GBDevice device) {

    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return helper != null;
    }
}
