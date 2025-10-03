/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuaweiMusicUtils {

    public static class PageStruct {
        public short startFrame = 0;
        public short endFrame = 0;
        public short count = 0;
        public byte[] hashCode = null;

        @NonNull
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("PageStruct{");
            sb.append("startFrame=").append(startFrame);
            sb.append(", endFrame=").append(endFrame);
            sb.append(", count=").append(count);
            sb.append(", hashCode=");
            if (hashCode == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < hashCode.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(hashCode[i]);
                sb.append(']');
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static class FormatRestrictions {
        public byte formatIdx = -1;
        public String[] sampleRates = null;
        public byte musicEncode = -1;  // TODO: not sure
        public short bitrate = -1;
        public byte channels = 2;
        public short unknownBitrate = -1;   // TODO: not sure

        // TODO: I am not sure about this. Most of formats unknown for me.
        private static final String[] formats = {"mp3", "wav", "aac", "sbc", "msbc", "hwa", "cvsd", "pcm", "ape", "m4a", "flac", "opus", "ogg", "butt", "amr", "imy"};

        public String getName() {
            return (formatIdx >= 0 && formatIdx < formats.length)?formats[formatIdx]:null;
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("FormatRestrictions{");
            sb.append("formatIdx=").append(formatIdx);
            sb.append(", sampleRates=").append(Arrays.toString(sampleRates));
            sb.append(", musicEncode=").append(musicEncode);
            sb.append(", bitrate=").append(bitrate);
            sb.append(", channels=").append(channels);
            sb.append(", unknownBitrate=").append(unknownBitrate);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class MusicCapabilities {
        public int availableSpace = 0;
        public List<String> supportedFormats = null;
        public int maxMusicCount = 0;
        public int maxPlaylistCount = 0;
        public int currentMusicCount = 0; // TODO: not sure
        public int unknown = 0; // TODO: not sure
        public List<FormatRestrictions> formatsRestrictions = null;

        @NonNull
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("MusicCapabilities{");
            sb.append("availableSpace=").append(availableSpace);
            sb.append(", supportedFormats=").append(supportedFormats);
            sb.append(", maxMusicCount=").append(maxMusicCount);
            sb.append(", maxPlaylistCount=").append(maxPlaylistCount);
            sb.append(", currentMusicCount=").append(currentMusicCount);
            sb.append(", unknown=").append(unknown);
            sb.append(", formatsRestrictions=").append(formatsRestrictions);
            sb.append('}');
            return sb.toString();
        }
    }


    // TODO: I am not sure about this. Most of formats unknown for me.
    private static final String[] formatsByte0 = {"mp3", "wav", "aac", "sbc", "msbc", "hwa", "cvsd"};
    private static final String[] formatsByte1 = {"pcm", "ape", "m4a", "flac", "opus", "ogg", "butt"};
    private static final String[] formatsByte2 = {"amr", "imy"};

    public static void parseNext(int dt, String[] info, List<String> res) {
        for (byte k = 0; k < info.length; k++) {
            if ((dt & (1 << k)) != 0) {
                res.add(info[k]);
            }
        }
    }

    public static List<String> parseFormats(List<Integer> data) {
        List<String> res = new ArrayList<>();

        if (data.size() >= 1) {
            parseNext(data.get(0), formatsByte0, res);
        }

        if (data.size() >= 2) {
            parseNext(data.get(1), formatsByte1, res);
        }

        if (data.size() >= 3) {
            parseNext(data.get(2), formatsByte2, res);
        }

        return res;
    }

    public static List<String> parseFormatBits(byte[] formatBits) {
        if (formatBits.length == 0)
            return null;
        List<Integer> toDecode = new ArrayList<>();
        for (byte formatBit : formatBits) {
            int dt = formatBit & 0xFF;
            toDecode.add(dt);
            if ((dt & 128) == 0) break;
        }
        return parseFormats(toDecode);
    }
}
