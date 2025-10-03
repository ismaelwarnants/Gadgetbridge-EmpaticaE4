/*  Copyright (C) 2018-2024 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

public abstract class StopNotificationAction extends AbortTransactionAction {

    private final BluetoothGattCharacteristic alertLevelCharacteristic;

    public StopNotificationAction(BluetoothGattCharacteristic alertLevelCharacteristic) {
        this.alertLevelCharacteristic = alertLevelCharacteristic;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        if (!super.run(gatt)) {
            // send a signal to stop the vibration
            WriteAction.writeCharacteristic(gatt, alertLevelCharacteristic, new byte[]{HuamiService.ALERT_LEVEL_NONE});
            return false;
        }
        return true;
    }
};

