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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsAgpsFile;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class ZeppOsAgpsInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsInstallHandler.class);

    protected final Context mContext;
    private ZeppOsAgpsFile file;

    private static final int MAX_EXPECTED_SIZE = 1024 * 1024; // 1MB, they're usually ~128KB

    public ZeppOsAgpsInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        if (uriHelper.getFileSize() > MAX_EXPECTED_SIZE) {
            LOG.debug("Not agps - file too large");
            return;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            final byte[] rawBytes = FileUtils.readAll(in, MAX_EXPECTED_SIZE);
            final ZeppOsAgpsFile agpsFile = new ZeppOsAgpsFile(rawBytes);
            if (agpsFile.isValid()) {
                this.file = agpsFile;
            }
        } catch (final Exception e) {
            LOG.error("Failed to read file", e);
        }
    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return file != null && file.isValid();
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof ZeppOsCoordinator zeppOsCoordinator)) {
            LOG.warn("Coordinator is not a ZeppOsCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }
        if (!zeppOsCoordinator.supportsAgpsUpdates()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        final GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(coordinator.getDefaultIconResource());

        if (file == null) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }

        final StringBuilder builder = new StringBuilder();
        final String agpsBundle = mContext.getString(R.string.kind_agps_bundle);
        builder.append(mContext.getString(R.string.fw_upgrade_notice, agpsBundle));
        builder.append("\n\n").append(mContext.getString(R.string.miband_firmware_unknown_warning));
        fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_untested_version));
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    public ZeppOsAgpsFile getFile() {
        return file;
    }

    private GenericItem createInstallItem(final GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final String firmwareName = mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(R.string.kind_agps_bundle),
                ""
        );
        return new GenericItem(firmwareName);
    }
}
