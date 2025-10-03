/*  Copyright (C) 2023-2024 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gb6900;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class SetAlarmOperation extends AbstractBTLEOperation<CasioGB6900DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(SetAlarmOperation.class);
    private final ArrayList<? extends Alarm> mAlarms;

    public SetAlarmOperation(CasioGB6900DeviceSupport support, ArrayList<? extends Alarm> alarms) {
        super(support);
        this.mAlarms = alarms;
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask(R.string.busy_task_setting_alarms, getContext()); // mark as busy quickly to avoid interruptions from the outside
    }

    private void getSettingForAlarm() {
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("getSettingForAlarm");
                builder.setCallback(this);
                builder.read(CasioConstants.CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID);
                builder.queue();
            } catch (IOException ex) {
                LOG.info("Error retrieving alarm settings: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void doPerform() throws IOException {
        getSettingForAlarm();
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        unsetBusy();
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("finished operation");
                builder.wait(0);
                builder.setCallback(null); // unset ourselves from being the queue's gatt callback
                builder.queue();
            } catch (IOException ex) {
                LOG.info("Error resetting Gatt callback: " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, byte[] data,
                                        int status) {

        UUID characteristicUUID = characteristic.getUuid();

        if(data.length == 0)
            return true;

        if(characteristicUUID.equals(CasioConstants.CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID)) {
            StringBuilder str = new StringBuilder("onCharacteristicRead: Received alarm settings: ");
            for(int i=0; i<data.length; i++) {
                str.append(String.format("0x%1x ", data[i]));
            }
            LOG.info(str.toString());

            int alarmOffset = 4;

            for(int i=0; i<mAlarms.size(); i++)
            {
                Alarm alm = mAlarms.get(i);
                data[i * alarmOffset] &= ~(0x60);
                if(alm.getEnabled()) {
                    data[i * alarmOffset] |= 0x40;
                }
                if(alm.getRepetition() == Alarm.ALARM_ONCE) {
                    data[i * alarmOffset] |= 0x20;
                }
                data[i * alarmOffset + 1] = 0;
                data[i * alarmOffset + 2] = (byte)alm.getHour();
                data[i * alarmOffset + 3] = (byte)alm.getMinute();
            }

            try {
                TransactionBuilder builder = performInitialized("setAlarm");
                builder.writeLegacy(getCharacteristic(CasioConstants.CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID), data);
                builder.queue();
            } catch(IOException e) {
                LOG.error("Error setting alarm: " + e.getMessage());
            }

            operationFinished();
        }
        else {
            return super.onCharacteristicRead(gatt, characteristic, data, status);
        }

        return true;
    }
}
