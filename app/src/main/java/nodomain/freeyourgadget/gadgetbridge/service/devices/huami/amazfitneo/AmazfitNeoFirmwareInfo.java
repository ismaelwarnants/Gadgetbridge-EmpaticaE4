/*  Copyright (C) 2021-2024 Andreas Shimokawa, xaos

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitneo;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitNeoFirmwareInfo extends HuamiFirmwareInfo {

    public static final byte[] FW_HEADER = new byte[]{
            // this might be incorrect but found in 1.1.1.18 and 1.1.2.52
            0x06, 0x68, 0x45, 0x68, (byte) 0x84, 0x68, (byte) 0xC1, 0x68, 0x02, (byte) 0x91, 0x01, 0x69, 0x01, (byte) 0x91, 0x41, 0x69
    };

    public static final int FW_HEADER_OFFSET = 0x90;

    private static final Map<Integer, String> crcToVersion = new HashMap<>();

    static {
        // 1.1.2.58 - #5280
        crcToVersion.put(25427, "1.1.2.58"); // firmware
        crcToVersion.put(51301, "1.1.2.58"); // resources
    }

    public AmazfitNeoFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {

        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET)) {
            if (searchString32BitAligned(bytes, "Amazfit Neo")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }
        if (ArrayUtils.startsWith(bytes, FT_HEADER)) {
            return HuamiFirmwareType.FONT;
        }

        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITNEO;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }

    @Override
    protected String searchFirmwareVersion(byte[] fwbytes) {
        // Not implemented
        return null;
    }
}
