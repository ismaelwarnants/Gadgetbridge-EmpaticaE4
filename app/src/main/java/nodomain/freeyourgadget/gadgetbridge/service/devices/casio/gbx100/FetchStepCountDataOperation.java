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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchStepCountDataOperation  extends AbstractBTLEOperation<CasioGBX100DeviceSupport>  {
    private static final Logger LOG = LoggerFactory.getLogger(FetchStepCountDataOperation.class);

    private final CasioGBX100DeviceSupport support;

    public FetchStepCountDataOperation(CasioGBX100DeviceSupport support) {
        super(support);
        this.support = support;
    }

    private void enableRequiredNotifications(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("enableRequiredNotifications");
            builder.setCallback(this);
            builder.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, enable);
            builder.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, enable);
            builder.queue();
        } catch(IOException e) {
            LOG.error("Error enabling required notifications", e);
        }
    }

    private void requestStepCountData() {
        byte[] command = {0x00, 0x11, 0x00, 0x00, 0x00};

        try {
            TransactionBuilder builder = performInitialized("requestStepCountDate");
            builder.setCallback(this);
            builder.writeLegacy(getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID), command);
            builder.queue();
        } catch(IOException e) {
            LOG.error("Error requesting step count data", e);
        }
    }

    private void writeStepCountAck() {
        byte[] command = {0x04, 0x11, 0x00, 0x00, 0x00};

        try {
            TransactionBuilder builder = performInitialized("writeStepCountAck");
            builder.setCallback(this);
            builder.writeLegacy(getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID), command);
            builder.queue();
        } catch(IOException e) {
            LOG.error("Error writing step count ack", e);
        }
    }

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask(R.string.busy_task_fetch_steps, getContext()); // mark as busy quickly to avoid interruptions from the outside
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 0, getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        enableRequiredNotifications(true);
        requestStepCountData();
    }

    @Override
    protected void operationFinished() {
        LOG.info("FetchStepCountDataOperation finished");
        unsetBusy();
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), false, 100, getContext());
        GB.signalActivityDataFinish(getDevice());

        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null) {
            try {
                TransactionBuilder builder = performInitialized("finished operation");
                builder.setCallback(null); // unset ourselves from being the queue's gatt callback
                builder.wait(0);
                builder.queue();
            } catch (IOException ex) {
                LOG.error("Error resetting Gatt callback", ex);
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        UUID characteristicUUID = characteristic.getUuid();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)) {
            int length = 0;
            if (data.length > 3) {
                length = (data[2] & 0xff) | ((data[3] & 0xff) << 8);
            }
            LOG.debug("Response is going to be {} bytes long", length);
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 10, getContext());

            return true;
        } else if(characteristicUUID.equals(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID)) {
            if(data.length < 18) {
                LOG.info("Data length too short.");
            } else {
                for(int i=0; i<data.length; i++)
                    data[i] = (byte)(~data[i]);

                int payloadLength = ((data[0] & 0xff) | ((data[1] & 0xff) << 8));

                if(data.length == (payloadLength + 2)) {
                    LOG.debug("Payload length and data length match.");
                } else {
                    LOG.warn("Payload length and data length do not match: {} vs. {}", payloadLength, data.length);
                }

                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                ArrayList<CasioGBX100ActivitySample> stepCountData = new ArrayList<>();

                int year = data[2];
                int month = data[3] - 1; // Zero-based
                int day = data[4];
                int hour = data[5];
                int minute = data[6];

                int stepCount = ((data[7] & 0xff) | ((data[8] & 0xff) << 8) | ((data[9] & 0xff) << 16) | ((data[10] & 0xff) << 24));
                 // it reports 0xfffffffe if no steps have been recorded
                if(stepCount == 0xfffffffe)
                    stepCount = 0;

                int calories = ((data[11] & 0xff) | ((data[12] & 0xff) << 8));
                if(calories == 0xfffe)
                    calories = 0;
                int yearOfBirth = ((data[13] & 0xff) | ((data[14] & 0xff) << 8));
                int monthOfBirth = data[15];
                int dayOfBirth = data[16];
                LOG.debug("Current step count value: {}", stepCount);
                LOG.debug("Current calories: {}", calories);
                // data[17]:
                // 0x01 probably means finished.
                // 0x00 probably means more data.

                // Set timestamps for retrieving recorded data for the current day

                cal.set(year + 2000, month, day, hour, 30, 0);
                int ts_to = (int)(cal.getTimeInMillis() / 1000);
                cal.set(year + 2000, month, day, 0, 0, 0);
                int ts_from = (int)(cal.getTimeInMillis() / 1000);

                CasioGBX100ActivitySample sum = support.getSumWithinRange(ts_from, ts_to);

                int caloriesToday = sum.getCalories();
                int stepsToday = sum.getSteps();

                // Set timestamp to currently fetched data for fetching historic data
                cal.set(year + 2000, month, day, hour, 30, 0);

                if(data[17] == 0x00 && data.length > 18) {
                    LOG.info("We got historic step count data.");
                    int index = 18;
                    boolean inPacket = false;
                    int packetIndex = 0;
                    int packetLength = 0;
                    int type = 0;
                    while(index < data.length) {
                        if(!inPacket) {
                            type = data[index];
                            packetLength = ((data[index + 1] & 0xff) | ((data[index + 2] & 0xff) << 8));
                            packetIndex = 0;
                            inPacket = true;
                            index = index + 3;
                            LOG.debug("Decoding packet with type: {} and length: {}", type, packetLength);
                        }
                        int count = ((data[index] & 0xff) | ((data[index + 1] & 0xff) << 8));
                        if(count == 0xfffe)
                            count = 0;
                        LOG.debug("Got count {}", count);

                        index = index+2;
                        if(index >= data.length) {
                            LOG.debug("End of packet.");
                        }
                        if(type == CasioConstants.CASIO_CONVOY_DATATYPE_STEPS) {
                            cal.add(Calendar.HOUR, -1);
                            int ts = (int)(cal.getTimeInMillis() / 1000);
                            stepCountData.add(new CasioGBX100ActivitySample());
                            stepCountData.get(packetIndex/2).setSteps(count);
                            stepCountData.get(packetIndex/2).setTimestamp(ts);
                            if(count > 0) {
                                stepCountData.get(packetIndex / 2).setRawKind(ActivityKind.ACTIVITY.getCode());
                            } else {
                                stepCountData.get(packetIndex / 2).setRawKind(ActivityKind.NOT_MEASURED.getCode());
                            }
                            if(ts > ts_from && ts < ts_to) {
                                stepsToday += count;
                            }
                        } else if(type == CasioConstants.CASIO_CONVOY_DATATYPE_CALORIES) {
                            if(stepCountData.get(packetIndex/2).getSteps() > 0) {
                                // The last packet might contain an invalid calory count
                                // of 255, but only if the steps are also invalid.
                                stepCountData.get(packetIndex / 2).setCalories(count);
                                int ts = stepCountData.get(packetIndex / 2).getTimestamp();
                                if (ts > ts_from && ts < ts_to) {
                                    caloriesToday += count;
                                }
                            }
                        }
                        packetIndex = packetIndex + 2;
                        if(packetIndex >= packetLength)
                            inPacket = false;
                    }
                }

                // This generates an artificial "now" timestamp for the current
                // activity based on the existing data. This timestamp will be overwritten
                // by the next fetch operation with the actual value.
                int steps = stepCount - stepsToday;
                int cals = calories - caloriesToday;

                // For a yet unknown reason, the sum calculated by the watch is sometimes lower than
                // the sum calculated by us. I suspect it is just refreshed at a later time!?

                if(steps > 0 && cals > 0) {

                    cal.set(year + 2000, month, day, hour, 30, 0);
                    int ts = (int) (cal.getTimeInMillis() / 1000);

                    LOG.debug("Artificial timestamp: {} calories and {} steps", cals, steps);
                    CasioGBX100ActivitySample sample = new CasioGBX100ActivitySample();
                    sample.setSteps(steps);
                    sample.setCalories(cals);
                    sample.setTimestamp(ts);
                    if (steps > 0)
                        sample.setRawKind(ActivityKind.ACTIVITY.getCode());
                    else
                        sample.setRawKind(ActivityKind.NOT_MEASURED.getCode());
                    stepCountData.add(0, sample);
                }

                support.stepCountDataFetched(stepCount, calories, stepCountData);
            }
            GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 80, getContext());

            writeStepCountAck();
            return true;
        } else {
            LOG.warn("Unhandled characteristic changed: {}", characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic, data);
        }
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)) {
            if(data[0] == 0x00) {
                LOG.debug("Request sent successfully");
            } else if(data[0] == 0x04) {
                LOG.debug("Read step count operation finished");
                enableRequiredNotifications(false);
                operationFinished();
            }
            return true;
        }
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }
}
