/*  Copyright (C) 2020-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.update;

import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.calcMaxWriteChunk;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo.UIHH_HEADER;

public class UpdateFirmwareOperation2020 extends UpdateFirmwareOperation {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation2020.class);

    public UpdateFirmwareOperation2020(Uri uri, HuamiSupport support) {
        super(uri, support);
    }

    public static final byte COMMAND_REQUEST_PARAMETERS = (byte) 0xd0;
    public static final byte COMMAND_UNKNOWN_D1 = (byte) 0xd1;
    public static final byte COMMAND_SEND_FIRMWARE_INFO = (byte) 0xd2;
    public static final byte COMMAND_START_TRANSFER = (byte) 0xd3;
    public static final byte REPLY_UPDATE_PROGRESS = (byte) 0xd4;
    public static final byte COMMAND_COMPLETE_TRANSFER = (byte) 0xd5;
    public static final byte COMMAND_FINALIZE_UPDATE = (byte) 0xd6;

    public static final byte REPLY_ERROR_FREE_SPACE = (byte) 0x47;
    public static final byte REPLY_ERROR_LOW_BATTERY = (byte) 0x22;

    protected int mChunkLength = -1;

    @Override
    protected void doPerform() throws IOException {
        firmwareInfo = createFwInfo(uri, getContext());
        if (!firmwareInfo.isGenerallyCompatibleWith(getDevice())) {
            throw new IOException("Firmware is not compatible with the given device: " + getDevice().getAddress());
        }

        if (!requestParameters()) {
            displayMessage(getContext(), "Error requesting parameters, aborting.", Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
    }


    @Override
    protected void handleNotificationNotif(byte[] value) {
        boolean success = (value[2] == HuamiService.SUCCESS) || ((value[1] == REPLY_UPDATE_PROGRESS) && value.length >= 6); // ugly

        if (value[0] == HuamiService.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case COMMAND_REQUEST_PARAMETERS: {
                        mChunkLength = (value[4] & 0xff) | ((value[5] & 0xff) << 8);
                        LOG.info("got chunk length of " + mChunkLength);
                        sendFwInfo();
                        break;
                    }
                    case COMMAND_SEND_FIRMWARE_INFO:
                        sendTransferStart();
                        break;
                    case COMMAND_START_TRANSFER:
                        sendFirmwareDataChunk(getFirmwareInfo(), 0);
                        break;
                    case HuamiService.COMMAND_FIRMWARE_START_DATA:
                        sendChecksum(getFirmwareInfo());
                        break;
                    case REPLY_UPDATE_PROGRESS:
                        int offset = (value[2] & 0xff) | ((value[3] & 0xff) << 8) | ((value[4] & 0xff) << 16) | ((value[5] & 0xff) << 24);
                        LOG.info("update progress " + offset + " bytes");
                        sendFirmwareDataChunk(getFirmwareInfo(), offset);
                        break;
                    case COMMAND_COMPLETE_TRANSFER:
                        sendFinalize();
                        break;
                    case COMMAND_FINALIZE_UPDATE: {
                        if (getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.FIRMWARE) {
                            TransactionBuilder builder = performInitialized("reboot");
                            getSupport().sendReboot(builder);
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
                        LOG.error("Unexpected response during firmware update: ");
                        getSupport().logMessageContent(value);
                        operationFailed();
                        displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                        done();
                    }
                }
            } catch (Exception ex) {
                displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                done();
            }
        } else {
            LOG.error("Unexpected notification during firmware update: ");
            operationFailed();
            getSupport().logMessageContent(value);
            int errorMessage = R.string.updatefirmwareoperation_metadata_updateproblem;
            // Display a more specific error message for known errors

            if (value[0] == HuamiService.RESPONSE && value[1] == COMMAND_START_TRANSFER && value[2] == REPLY_ERROR_FREE_SPACE) {
                // Not enough free space on the device
                errorMessage = R.string.updatefirmwareoperation_updateproblem_free_space;
            } else if (value[0] == HuamiService.RESPONSE && value[1] == COMMAND_SEND_FIRMWARE_INFO && value[2] == REPLY_ERROR_LOW_BATTERY) {
                // Battery is too low
                errorMessage = R.string.updatefirmwareoperation_updateproblem_low_battery;
            }
            displayMessage(getContext(), getContext().getString(errorMessage), Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
    }


    @Override
    public boolean sendFwInfo() {
        try {
            TransactionBuilder builder = performInitialized("send firmware info");
            builder.setBusyTask(R.string.updating_firmware);
            int fwSize = getFirmwareInfo().getSize();
            byte[] sizeBytes = BLETypeConversions.fromUint32(fwSize);
            byte[] bytes = buildFirmwareInfoCommand();

            if (getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.WATCHFACE) {
                byte[] fwBytes = firmwareInfo.getBytes();
                if (ArrayUtils.startsWith(fwBytes, UIHH_HEADER)) {
                    getSupport().writeToConfiguration(builder,
                            new byte[]{0x39, 0x00,
                                    sizeBytes[0],
                                    sizeBytes[1],
                                    sizeBytes[2],
                                    sizeBytes[3],
                                    fwBytes[18],
                                    fwBytes[19],
                                    fwBytes[20],
                                    fwBytes[21]
                            });
                }
            }
            builder.write(fwCControlChar, bytes);
            builder.queue();
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }

    protected byte[] buildFirmwareInfoCommand() {
        int fwSize = getFirmwareInfo().getSize();
        byte[] sizeBytes = BLETypeConversions.fromUint32(fwSize);
        int crc32 = firmwareInfo.getCrc32();
        byte[] chunkSizeBytes = BLETypeConversions.fromUint16(mChunkLength);
        byte[] crcBytes = BLETypeConversions.fromUint32(crc32);
        return new byte[]{
                COMMAND_SEND_FIRMWARE_INFO,
                getFirmwareInfo().getFirmwareType().getValue(),
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
                0, // index
                1, // count
                sizeBytes[0], // total size? right now it is equal to the size above
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3]
        };
    }

    public boolean requestParameters() {
        try {
            TransactionBuilder builder = performInitialized("get update capabilities");
            byte[] bytes = new byte[]{COMMAND_REQUEST_PARAMETERS};
            builder.write(fwCControlChar, bytes);
            builder.queue();
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }


    private boolean sendFirmwareDataChunk(AbstractHuamiFirmwareInfo info, int offset) {
        byte[] fwbytes = info.getBytes();
        int len = fwbytes.length;
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
                return true;
            }

            TransactionBuilder builder = performInitialized("send firmware packets");

            for (int i = 0; i < packets; i++) {
                byte[] fwChunk = Arrays.copyOfRange(fwbytes, offset + i * packetLength, offset + i * packetLength + packetLength);

                builder.write(fwCDataChar, fwChunk);
                chunkProgress += packetLength;
            }

            if (chunkProgress < chunkLength) {
                byte[] lastChunk = Arrays.copyOfRange(fwbytes, offset + packets * packetLength, offset + packets * packetLength + (chunkLength - chunkProgress));
                builder.write(fwCDataChar, lastChunk);
            }

            int progressPercent = (int) ((((float) (offset + chunkLength)) / len) * 100);

            builder.setProgress(R.string.updatefirmwareoperation_update_in_progress, true, progressPercent);

            builder.queue();

        } catch (IOException ex) {
            LOG.error("Unable to send fw to device", ex);
            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_firmware_not_sent), false, 0, getContext());
            return false;
        }
        return true;
    }


    protected void sendTransferStart() throws IOException {
        TransactionBuilder builder = performInitialized("transfer complete");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_START_TRANSFER, 1,
        });
        builder.queue();
    }

    protected void sendTransferComplete() throws IOException {
        TransactionBuilder builder = performInitialized("transfer complete");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_COMPLETE_TRANSFER,
        });
        builder.queue();
    }

    protected void sendFinalize() throws IOException {
        TransactionBuilder builder = performInitialized("finalize firmware");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_FINALIZE_UPDATE,
        });
        builder.queue();
    }
}