/*  Copyright (C) 2018-2024 Andreas Shimokawa, José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.model;

public class RecordedDataTypes {
    public static final int TYPE_ACTIVITY     = 0x00000001;
    public static final int TYPE_WORKOUTS     = 0x00000002;
    public static final int TYPE_GPS_TRACKS   = 0x00000004;
    public static final int TYPE_TEMPERATURE  = 0x00000008;
    public static final int TYPE_DEBUGLOGS    = 0x00000010;
    public static final int TYPE_SPO2         = 0x00000020;
    public static final int TYPE_STRESS       = 0x00000040;
    public static final int TYPE_HEART_RATE   = 0x00000080;
    public static final int TYPE_PAI          = 0x00000100;
    public static final int TYPE_SLEEP_RESPIRATORY_RATE = 0x00000200;
    public static final int TYPE_HUAMI_STATISTICS = 0x00000400;
    public static final int TYPE_SLEEP        = 0x00000800;
    public static final int TYPE_HRV          = 0x00001000;
    public static final int TYPE_AUDIO_REC    = 0x00002000;

    public static final int TYPE_ALL          = (int)0xffffffff;

    // Types to fetch during sync - scheduled, sync button, etc.
    // Does not include debug logs or workouts
    public static final int TYPE_SYNC = TYPE_ACTIVITY | TYPE_GPS_TRACKS | TYPE_SPO2 | TYPE_STRESS |
            TYPE_TEMPERATURE |
            TYPE_SLEEP |
            TYPE_HRV |
            TYPE_HEART_RATE | TYPE_PAI | TYPE_SLEEP_RESPIRATORY_RATE;
}
