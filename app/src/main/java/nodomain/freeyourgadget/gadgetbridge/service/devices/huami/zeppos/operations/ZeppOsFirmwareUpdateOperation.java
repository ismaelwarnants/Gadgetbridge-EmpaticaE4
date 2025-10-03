/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.calcMaxWriteChunk;

import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsFwHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.update.UpdateFirmwareOperation2020;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * This class is basically UpdateFirmwareOperation2020. However, Zepp OS firmwares are growing huge (200+MB),
 * which crash Gadgetbridge. In order to refactor the upgrade process for Zepp OS devices without having to
 * refactor all 52 devices, the code was moved to this class so it can be refactored with a smaller impact.
 * <p>
 * The protocol for Zepp OS devices differs from UpdateFirmwareOperation2020 in the following points:
 * - no crc16 support (not needed for zepp os)
 * - buildFirmwareInfoCommand is slightly different
 * - at the end of the update, we request display items / watchfaces depending on what was installed, to refresh the
 * preferences in the UI
 * <p>
 * As of btrfcomm support, it also decouples the operation from the abstract ble operation classes.
 */
public class ZeppOsFirmwareUpdateOperation extends AbstractZeppOsOperation<ZeppOsSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFirmwareUpdateOperation.class);

    private final Uri uri;
    private ZeppOsFwHelper fwHelper;
    private RandomAccessFile raf;

    protected int mChunkLength = -1;

    public ZeppOsFirmwareUpdateOperation(final Uri uri, final ZeppOsSupport support) {
        super(support);
        this.uri = uri;
    }

    protected void enableNeededNotifications(ZeppOsTransactionBuilder builder, boolean enable) {
        builder.notify(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, enable);
    }

    protected void enableOtherNotifications(final ZeppOsTransactionBuilder builder, final boolean enable) {
        // Disable 2021 chunked reads, otherwise firmware upgrades and activity sync get interrupted
        // FIXME is this still needed?
        builder.notify(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ, enable);
    }

    @Override
    protected void doPerform() throws IOException {
        final GBDevice device = getDevice();
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof ZeppOsCoordinator)) {
            throw new IOException("Not a Zepp OS coordinator for " + getDevice().getAddress());
        }

        getDevice().setBusyTask(R.string.updating_firmware, getContext()); // mark as busy quickly to avoid interruptions from the outside
        ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("fw update starting");
        enableOtherNotifications(builder, false);
        enableNeededNotifications(builder, true);
        builder.queue();

        fwHelper = new ZeppOsFwHelper(
                uri,
                getContext(),
                ((ZeppOsCoordinator) coordinator).getDeviceBluetoothNames(),
                ((ZeppOsCoordinator) coordinator).getDeviceSources()
        );

        if (!fwHelper.isValid()) {
            throw new IOException("Firmware is not valid for: " + getDevice().getAddress());
        }

        raf = new RandomAccessFile(fwHelper.getFile(), "r");

        requestParameters();
    }

    @Override
    protected void operationFinished() {
        super.operationFinished();
        getSupport().onFirmwareUpdateFinished();
        ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("fw update finish");
        enableNeededNotifications(builder, false);
        enableOtherNotifications(builder, true);
        builder.queue();
    }

    protected void done() {
        LOG.info("Operation done.");
        operationFinished();
        unsetBusy();
    }

    void operationFailed() {
        GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_write_failed), false, 0, getContext());
    }

    public void handleControlNotification(final byte[] value) {
        boolean success = (value[2] == HuamiService.SUCCESS) || ((value[1] == UpdateFirmwareOperation2020.REPLY_UPDATE_PROGRESS) && value.length >= 6); // ugly

        if (value[0] == HuamiService.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case UpdateFirmwareOperation2020.COMMAND_REQUEST_PARAMETERS: {
                        mChunkLength = (value[4] & 0xff) | ((value[5] & 0xff) << 8);
                        LOG.info("got chunk length of " + mChunkLength);
                        sendFwInfo();
                        break;
                    }
                    case UpdateFirmwareOperation2020.COMMAND_SEND_FIRMWARE_INFO:
                        sendTransferStart();
                        break;
                    case UpdateFirmwareOperation2020.COMMAND_START_TRANSFER:
                        sendFirmwareDataChunk(0);
                        break;
                    // not used in zepp os
                    //case HuamiService.COMMAND_FIRMWARE_START_DATA:
                    //    sendChecksum();
                    //    break;
                    case UpdateFirmwareOperation2020.REPLY_UPDATE_PROGRESS:
                        int offset = (value[2] & 0xff) | ((value[3] & 0xff) << 8) | ((value[4] & 0xff) << 16) | ((value[5] & 0xff) << 24);
                        LOG.info("update progress {} bytes", offset);
                        sendFirmwareDataChunk(offset);
                        break;
                    case UpdateFirmwareOperation2020.COMMAND_COMPLETE_TRANSFER:
                        sendFinalize();
                        break;
                    case UpdateFirmwareOperation2020.COMMAND_FINALIZE_UPDATE: {
                        if (fwHelper.getFirmwareType() == HuamiFirmwareType.FIRMWARE) {
                            ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("reboot");
                            builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[]{HuamiService.COMMAND_FIRMWARE_REBOOT});
                            builder.queue();
                        } else {
                            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                            done();
                        }
                        break;
                    }
                    case HuamiService.COMMAND_FIRMWARE_REBOOT: {
                        LOG.info("Reboot command successfully sent.");
                        GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                        done();
                        break;
                    }
                    default: {
                        LOG.error("Unexpected response during firmware update: {}", GB.hexdump(value));
                        operationFailed();
                        displayErrorMessage(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot));
                        done();
                    }
                }
            } catch (Exception ex) {
                displayErrorMessage(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot));
                done();
            }
        } else {
            LOG.error("Unexpected notification during firmware update: {}", GB.hexdump(value));
            operationFailed();
            int errorMessage = R.string.updatefirmwareoperation_metadata_updateproblem;
            // Display a more specific error message for known errors

            if (value[0] == HuamiService.RESPONSE && value[1] == UpdateFirmwareOperation2020.COMMAND_START_TRANSFER && value[2] == UpdateFirmwareOperation2020.REPLY_ERROR_FREE_SPACE) {
                // Not enough free space on the device
                errorMessage = R.string.updatefirmwareoperation_updateproblem_free_space;
            } else if (value[0] == HuamiService.RESPONSE && value[1] == UpdateFirmwareOperation2020.COMMAND_SEND_FIRMWARE_INFO && value[2] == UpdateFirmwareOperation2020.REPLY_ERROR_LOW_BATTERY) {
                // Battery is too low
                errorMessage = R.string.updatefirmwareoperation_updateproblem_low_battery;
            }
            displayErrorMessage(getContext().getString(errorMessage));
            done();
        }

        if (ArrayUtils.startsWith(value, new byte[]{HuamiService.RESPONSE, UpdateFirmwareOperation2020.COMMAND_FINALIZE_UPDATE, HuamiService.SUCCESS})) {
            if (fwHelper.getFirmwareType() == HuamiFirmwareType.APP) {
                // After an app is installed, request the display items from the band (new app will be at the end)
                ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("request display items and apps");
                getSupport().requestDisplayItems(builder);
                getSupport().requestApps(builder);
                builder.queue();
            } else if (fwHelper.getFirmwareType() == HuamiFirmwareType.WATCHFACE) {
                // After a watchface is installed, request the watchfaces from the band (new watchface will be at the end)
                ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("request watchfaces and apps");
                getSupport().requestWatchfaces(builder);
                getSupport().requestApps(builder);
                builder.queue();
            }
        }
    }

    private void displayErrorMessage(String message) {
        getSupport().handleGBDeviceEvent(new GBDeviceEventDisplayMessage(message, Toast.LENGTH_LONG, GB.ERROR));
    }

    public void sendFwInfo() {
        ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("send firmware info");
        builder.setBusy(R.string.updating_firmware);
        builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, buildFirmwareInfoCommand());
        builder.queue();
    }

    protected byte[] buildFirmwareInfoCommand() {
        final int fwSize = fwHelper.getSize();
        final int crc32 = fwHelper.getCrc32();

        final byte[] sizeBytes = BLETypeConversions.fromUint32(fwSize);
        final byte[] chunkSizeBytes = BLETypeConversions.fromUint16(mChunkLength);
        final byte[] crcBytes = BLETypeConversions.fromUint32(crc32);

        return new byte[]{
                UpdateFirmwareOperation2020.COMMAND_SEND_FIRMWARE_INFO,
                fwHelper.getFirmwareType().getValue(),
                sizeBytes[0],
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3],
                crcBytes[0],
                crcBytes[1],
                crcBytes[2],
                crcBytes[3],
                chunkSizeBytes[0],
                chunkSizeBytes[1],
                0, // 0 to update in foreground, 1 for background
                (byte) 0xff, // ??
        };
    }

    public void requestParameters() {
        ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("get update capabilities");
        byte[] bytes = new byte[]{UpdateFirmwareOperation2020.COMMAND_REQUEST_PARAMETERS};
        builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, bytes);
        builder.queue();
    }

    private void sendFirmwareDataChunk(int offset) {
        int len = fwHelper.getSize();
        int remaining = len - offset;
        final int packetLength = calcMaxWriteChunk(getSupport().getMTU());

        int chunkLength = mChunkLength;
        if (remaining < mChunkLength) {
            chunkLength = remaining;
        }

        int packets = chunkLength / packetLength;
        int chunkProgress = 0;

        try {
            if (remaining <= 0) {
                sendTransferComplete();
                return;
            }

            ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("send firmware packets");

            for (int i = 0; i < packets; i++) {
                raf.seek(offset + (long) i * packetLength);
                byte[] fwChunk = new byte[packetLength];
                raf.read(fwChunk);

                builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_DATA, fwChunk);
                chunkProgress += packetLength;
            }

            if (chunkProgress < chunkLength) {
                raf.seek(offset + (long) packets * packetLength);
                byte[] lastChunk = new byte[chunkLength - chunkProgress];
                raf.read(lastChunk);
                builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_DATA, lastChunk);
            }

            int progressPercent = (int) ((((float) (offset + chunkLength)) / len) * 100);

            builder.setProgress(R.string.updatefirmwareoperation_update_in_progress, true, progressPercent);

            builder.queue();

        } catch (final IOException e) {
            LOG.error("Unable to send fw to device", e);
            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_firmware_not_sent), false, 0, getContext());
        }
    }

    private void sendTransferStart() {
        final ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("transfer complete");
        builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[]{
                UpdateFirmwareOperation2020.COMMAND_START_TRANSFER, 1,
        });
        builder.queue();
    }

    private void sendTransferComplete() {
        final ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("transfer complete");
        builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[]{
                UpdateFirmwareOperation2020.COMMAND_COMPLETE_TRANSFER,
        });
        builder.queue();
    }

    private void sendFinalize() {
        final ZeppOsTransactionBuilder builder = getSupport().createZeppOsTransactionBuilder("finalize firmware");
        builder.write(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[]{
                UpdateFirmwareOperation2020.COMMAND_FINALIZE_UPDATE,
        });
        builder.queue();
    }
}
