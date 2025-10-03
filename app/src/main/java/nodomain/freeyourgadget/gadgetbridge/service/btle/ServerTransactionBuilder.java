/*  Copyright (C) 2019-2025 Andreas Böhler, Damien Gaignon, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyCharacteristicChangedAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ServerResponseAction;

public class ServerTransactionBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ServerTransactionBuilder.class);

    private final ServerTransaction mTransaction;
    private boolean mQueued;

    public ServerTransactionBuilder(String taskName) {
        mTransaction = new ServerTransaction(taskName);
    }

    @NonNull
    public ServerTransactionBuilder writeServerResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] data) {
        if(device == null) {
            LOG.warn("Unable to write to device: null");
            return this;
        }
        ServerResponseAction action = new ServerResponseAction(device, requestId, status, offset, data);
        return add(action);
    }

    @NonNull
    public ServerTransactionBuilder notifyCharacteristicChanged(@NonNull BluetoothDevice device,
                                                                @NonNull BluetoothGattCharacteristic characteristic,
                                                                @NonNull byte[] value) {
        if (device == null) {
            LOG.warn("Unable to write to device: null");
            return this;
        }
        BtLEServerAction action = new NotifyCharacteristicChangedAction(device, characteristic, value);
        return add(action);
    }

    @NonNull
    public ServerTransactionBuilder add(@NonNull BtLEServerAction action) {
        mTransaction.add(action);
        return this;
    }

    /**
     * Sets a GattServerCallback instance that will be called when the transaction is executed,
     * resulting in GattServerCallback events.
     *
     * @param callback the callback to set, may be null
     */
    public void setCallback(@Nullable GattServerCallback callback) {
        mTransaction.setCallback(callback);
    }

    public
    @Nullable
    GattServerCallback getGattCallback() {
        return mTransaction.getGattCallback();
    }

    /**
     * To be used as the final step to execute the transaction by the given queue.
     */
    public void queue(@NonNull BtLEQueue queue) {
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        queue.add(mTransaction);
    }

    public ServerTransaction getTransaction() {
        return mTransaction;
    }

    public String getTaskName() {
        return mTransaction.getTaskName();
    }
}
