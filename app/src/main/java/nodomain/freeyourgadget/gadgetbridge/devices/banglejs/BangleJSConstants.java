/*  Copyright (C) 2019-2024 Gordon Williams, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import java.util.UUID;

public final class BangleJSConstants {


    public static final UUID UUID_SERVICE_NORDIC_UART = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_NORDIC_UART_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final String PREF_BANGLEJS_ACTIVITY_FULL_SYNC_TRIGGER = "pref_banglejs_activity_full_sync_trigger";
    public static final String PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STATUS = "pref_banglejs_activity_full_sync_status";
    public static final String PREF_BANGLEJS_ACTIVITY_FULL_SYNC_START = "pref_banglejs_activity_full_sync_start";
    public static final String PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STOP = "pref_banglejs_activity_full_sync_stop";
    public static final String PREF_BANGLEJS_NOTIFICATION_MISSED_CALL_ENABLE = "pref_notification_enable_missed_call";

}
