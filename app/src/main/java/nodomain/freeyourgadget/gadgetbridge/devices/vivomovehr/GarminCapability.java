/*  Copyright (C) 2023-2025 Petr Kadlec, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum GarminCapability {
    CONNECT_MOBILE_FIT_LINK,
    GOLF_FIT_LINK,
    VIVOKID_JR_FIT_LINK,
    SYNC,
    DEVICE_INITIATES_SYNC,
    HOST_INITIATED_SYNC_REQUESTS,
    GNCS,
    ADVANCED_MUSIC_CONTROLS,
    FIND_MY_PHONE,
    FIND_MY_WATCH,
    CONNECTIQ_HTTP,
    CONNECTIQ_SETTINGS,
    CONNECTIQ_WATCH_APP_DOWNLOAD,
    CONNECTIQ_WIDGET_DOWNLOAD,
    CONNECTIQ_WATCH_FACE_DOWNLOAD,
    CONNECTIQ_DATA_FIELD_DOWNLOAD,
    CONNECTIQ_APP_MANAGEMENT,
    COURSE_DOWNLOAD,
    WORKOUT_DOWNLOAD,
    GOLF_COURSE_DOWNLOAD,
    DELTA_SOFTWARE_UPDATE_FILES,
    FITPAY,
    LIVETRACK,
    LIVETRACK_AUTO_START,
    LIVETRACK_MESSAGING,
    GROUP_LIVETRACK,
    WEATHER_CONDITIONS,
    WEATHER_ALERTS,
    GPS_EPHEMERIS_DOWNLOAD,
    EXPLICIT_ARCHIVE,
    SWING_SENSOR,
    SWING_SENSOR_REMOTE,
    INCIDENT_DETECTION,
    TRUEUP,
    INSTANT_INPUT,
    SEGMENTS,
    AUDIO_PROMPT_LAP,
    AUDIO_PROMPT_PACE_SPEED,
    AUDIO_PROMPT_HEART_RATE,
    AUDIO_PROMPT_POWER,
    AUDIO_PROMPT_NAVIGATION,
    AUDIO_PROMPT_CADENCE,
    SPORT_GENERIC,
    SPORT_RUNNING,
    SPORT_CYCLING,
    SPORT_TRANSITION,
    SPORT_FITNESS_EQUIPMENT,
    SPORT_SWIMMING,
    STOP_SYNC_AFTER_SOFTWARE_UPDATE,
    CALENDAR,
    WIFI_SETUP,
    SMS_NOTIFICATIONS,
    BASIC_MUSIC_CONTROLS,
    AUDIO_PROMPTS_SPEECH,
    DELTA_SOFTWARE_UPDATES,
    GARMIN_DEVICE_INFO_FILE_TYPE,
    SPORT_PROFILE_SETUP,
    HSA_SUPPORT,
    SPORT_STRENGTH,
    SPORT_CARDIO,
    UNION_PAY,
    IPASS,
    CIQ_AUDIO_CONTENT_PROVIDER,
    UNION_PAY_INTERNATIONAL,
    REQUEST_PAIR_FLOW,
    LOCATION_UPDATE,
    LTE_SUPPORT,
    DEVICE_DRIVEN_LIVETRACK_SUPPORT,
    CUSTOM_CANNED_TEXT_LIST_SUPPORT,
    EXPLORE_SYNC,
    INCIDENT_DETECT_AND_ASSISTANCE,
    CURRENT_TIME_REQUEST_SUPPORT,
    CONTACTS_SUPPORT,
    LAUNCH_REMOTE_CIQ_APP_SUPPORT,
    DEVICE_MESSAGES,
    WAYPOINT_TRANSFER,
    MULTI_LINK_SERVICE,
    OAUTH_CREDENTIALS,
    GOLF_9_PLUS_9,
    ANTI_THEFT_ALARM,
    INREACH,
    EVENT_SHARING,
    UNK_82,
    UNK_83,
    UNK_84,
    UNK_85,
    UNK_86,
    UNK_87,
    UNK_88,
    UNK_89,
    UNK_90,
    UNK_91,
    REALTIME_SETTINGS,
    UNK_93,
    UNK_94,
    UNK_95,
    UNK_96,
    UNK_97,
    UNK_98,
    UNK_99,
    UNK_100,
    UNK_101,
    UNK_102,
    UNK_103,
    UNK_104,
    UNK_105,
    UNK_106,
    UNK_107,
    UNK_108,
    UNK_109,
    UNK_110,
    UNK_111,
    UNK_112,
    UNK_113,
    UNK_114,
    UNK_115,
    UNK_116,
    UNK_117,
    UNK_118,
    UNK_119,
    ;

    public static final Set<GarminCapability> OUR_CAPABILITIES = new HashSet<>(values().length);
    private static final Map<Integer, GarminCapability> FROM_ORDINAL = new HashMap<>(values().length);

    static {
        for (final GarminCapability cap : values()) {
            FROM_ORDINAL.put(cap.ordinal(), cap);
            OUR_CAPABILITIES.add(cap);
        }

        // so far dumps from Garmin Connect have only supported
        // UNK_112 and UNK_113
        OUR_CAPABILITIES.remove(UNK_104);
        OUR_CAPABILITIES.remove(UNK_105);
        OUR_CAPABILITIES.remove(UNK_106);
        OUR_CAPABILITIES.remove(UNK_107);
        OUR_CAPABILITIES.remove(UNK_108);
        OUR_CAPABILITIES.remove(UNK_109);
        OUR_CAPABILITIES.remove(UNK_110);
        OUR_CAPABILITIES.remove(UNK_111);
        OUR_CAPABILITIES.remove(UNK_114);
        OUR_CAPABILITIES.remove(UNK_115);
        OUR_CAPABILITIES.remove(UNK_116);
        OUR_CAPABILITIES.remove(UNK_117);
        OUR_CAPABILITIES.remove(UNK_118);
        OUR_CAPABILITIES.remove(UNK_119);
    }

    public static Set<GarminCapability> setFromBinary(final byte[] bytes) {
        final Set<GarminCapability> result = new HashSet<>(GarminCapability.values().length);
        int current = 0;
        for (int b : bytes) {
            for (int i = 0; i < 8; i++) {
                if ((b & (1 << i)) != 0) {
                    if (FROM_ORDINAL.containsKey(current)) {
                        result.add(FROM_ORDINAL.get(current));
                    }
                }
                ++current;
            }
        }
        return result;
    }

    public static byte[] setToBinary(final Set<GarminCapability> capabilities) {
        final GarminCapability[] values = values();
        final byte[] result = new byte[(values.length + 7) / 8];
        int bytePos = 0;
        int bitPos = 0;
        for (int i = 0; i < values.length; ++i) {
            if (capabilities.contains(FROM_ORDINAL.get(i))) {
                result[bytePos] |= (1 << bitPos);
            }
            ++bitPos;
            if (bitPos >= 8) {
                bitPos = 0;
                ++bytePos;
            }
        }
        return result;
    }

    public static String setToString(final Set<GarminCapability> capabilities) {
        final StringBuilder result = new StringBuilder();
        for (final GarminCapability cap : capabilities) {
            if (result.length() > 0) result.append(", ");
            result.append(cap.name());
        }
        return result.toString();
    }
}
