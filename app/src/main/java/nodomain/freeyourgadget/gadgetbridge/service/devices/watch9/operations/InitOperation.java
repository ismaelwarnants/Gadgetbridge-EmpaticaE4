/*  Copyright (C) 2018-2024 Damien Gaignon, maxirnilian

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.watch9.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.watch9.Watch9Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.watch9.Watch9DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation extends AbstractBTLEOperation<Watch9DeviceSupport>{

    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    private final TransactionBuilder builder;
    private final boolean needsAuth;
    private final BluetoothGattCharacteristic cmdCharacteristic = getCharacteristic(Watch9Constants.UUID_CHARACTERISTIC_WRITE);

    public InitOperation(boolean needsAuth, Watch9DeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.needsAuth = needsAuth;
        this.builder = builder;
        builder.setCallback(this);
    }

    @Override
    protected void doPerform() throws IOException {
        builder.notify(cmdCharacteristic, true);
        if (needsAuth) {
            builder.setDeviceState(GBDevice.State.AUTHENTICATING);
            getSupport().authorizationRequest(builder, needsAuth);
        } else {
            builder.setDeviceState(GBDevice.State.INITIALIZING);
            getSupport().initialize(builder);
            builder.queueImmediately();
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        UUID characteristicUUID = characteristic.getUuid();
        if (Watch9Constants.UUID_CHARACTERISTIC_WRITE.equals(characteristicUUID) && needsAuth) {
            try {
                getSupport().logMessageContent(value);
                if (ArrayUtils.equals(value, Watch9Constants.RESP_AUTHORIZATION_TASK, 5) && value[8] == 0x01) {
                    TransactionBuilder builder = getSupport().createTransactionBuilder("authInit");
                    builder.setCallback(this);
                    builder.setDeviceState(GBDevice.State.INITIALIZING);
                    getSupport().initialize(builder);
                    builder.queueImmediately();
                } else {
                    return super.onCharacteristicChanged(gatt, characteristic, value);
                }
            } catch (Exception e) {
                GB.toast(getContext(), "Error authenticating Watch 9", Toast.LENGTH_LONG, GB.ERROR, e);
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic, value);
        }
    }


}
