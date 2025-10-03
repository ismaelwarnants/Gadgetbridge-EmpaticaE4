/*  Copyright (C) 2022-2024 José Rebelo, Maxime Reyrolle, Reiner Herrmann,
    Sky233ml

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import java.util.HashMap;
import java.util.Map;

public class ZeppOsMenuType {
    /**
     * These somewhat match the ones in HuamiMenuType, but not all. The band sends and
     * receives those as 8-digit upper case hex strings.
     */
    public static final Map<String, String> displayItemNameLookup = new HashMap<>() {{
        put("00000001", "personal_activity_intelligence");
        put("00000002", "hr");
        put("00000003", "workout");
        put("00000004", "weather");
        put("00000009", "alarm");
        put("0000000A", "takephoto");
        put("0000000B", "music");
        put("0000000C", "stopwatch");
        put("0000000D", "countdown");
        put("0000000E", "findphone");
        put("0000000F", "mutephone");
        put("00000010", "cards");
        put("00000011", "alipay");
        put("00000013", "settings");
        put("00000014", "workout_history");
        put("00000015", "eventreminder");
        put("00000016", "compass");
        put("00000019", "pai");
        put("0000001A", "worldclock");
        put("0000001C", "stress");
        put("0000001D", "female_health");
        put("0000001E", "workout_status");
        put("00000020", "calendar");
        put("00000023", "sleep");
        put("00000024", "spo2");
        put("00000025", "phone");
        put("00000026", "events");
        put("00000031", "wechat_pay");
        put("00000033", "breathing");
        put("00000038", "pomodoro");
        put("00000039", "alexa2");
        put("0000003B", "thermometer");
        put("0000003E", "todo");
        put("0000003F", "mi_ai");
        put("00000041", "barometer");
        put("00000042", "voice_memos");
        put("00000044", "sun_moon");
        put("00000045", "one_tap_measuring");
        put("00000046", "zepp_coach");
        put("00000047", "membership_cards");
        put("00000049", "body_composition");
        put("0000004A", "readiness");
        put("0000004B", "map");
        put("0000004C", "zepp_pay");
        put("0000004D", "heart_rate_push");
        put("00000100", "alexa");
        put("00000101", "offline_voice");
        put("00000102", "flashlight");
        put("000FFD39", "hrv");
        put("000FC452", "bluetooth_scale_assistant");
        put("000F425B", "calculator");
        put("000F5D0A", "running_calculator");
        put("000F4258", "real_time_heart_rate");
        put("000F4263", "watch_storage_space");
        put("000F4259", "water_time");
        put("000F653B", "meditation");
        put("0010660A", "zepp_flow");
    }};

    public static final Map<String, String> shortcutsNameLookup = new HashMap<>() {{
        put("00000001", "hr");
        put("00000002", "weather");
        put("00000003", "pai");
        put("00000004", "music");
        put("00000005", "sleep");
        put("00000006", "alipay");
        put("00000008", "wechat_pay");
        put("00000009", "cards");
        put("0000000A", "workout");
        put("0000000B", "workout_history");
        put("0000000C", "workout_status");
        put("0000000E", "one_tap_measuring");
        put("0000000F", "stress");
        put("00000011", "female_health");
        put("00000012", "breathing");
        put("00000013", "spo2");
        put("00000015", "thermometer");
        put("00000016", "alarm");
        put("00000017", "calendar");
        put("00000018", "events");
        put("00000019", "todo");
        put("0000001A", "worldclock");
        put("0000001B", "compass");
        put("0000001C", "barometer");
        put("0000001E", "voice_memos");
        put("00000020", "activity");
        put("00000021", "eventreminder");
        put("00000023", "mi_ai");
        put("00000025", "alexa");
        put("00000027", "workout_shortcuts");
        put("00000028", "apps_shortcuts");
        put("00000029", "body_composition");
        put("0000002A", "readiness");
        put("0000002B", "zepp_pay");
        put("000FFD39", "hrv");
    }};

    public static final Map<String, String> controlCenterNameLookup = new HashMap<String, String>() {{
        put("00000000", "flashlight");
        put("00000001", "brightness");
        put("00000002", "lockscreen");
        put("00000003", "dnd");
        put("00000004", "sleep");
        put("00000005", "findphone");
        put("00000006", "volume");
        put("00000007", "battery");
        put("00000008", "theater_mode");
        put("00000009", "screen_always_lit");
        put("0000000A", "bluetooth");
        put("0000000B", "wifi");
        put("0000000D", "calendar");
        put("00000010", "music");
        put("00000012", "alarm");
        put("00000013", "settings");
        put("00000014", "buzzer_intensity");
        put("00000015", "barometer");
        put("00000016", "compass");
        put("00000017", "countdown");
        put("00000018", "stopwatch");
        put("00000019", "eject_water");
        put("0000001A", "headphone");
        put("0000001B", "night_display");
        put("0000001C", "always_on_display");
    }};
}
