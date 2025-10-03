/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Workout {

    public static final byte id = 0x17;

    public static class WorkoutCount {
        public static final byte id = 0x07;

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    int start,
                    int end
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x81, new HuaweiTLV()
                                .put(0x03, start)
                                .put(0x04, end)
                        );

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class WorkoutNumbers {
                public byte[] rawData;

                public short workoutNumber;
                public short dataCount;
                public short paceCount;
                public short segmentsCount = 0;
                public short spO2Count = 0;
                public short sectionsCount = 0;

                //NOTE: trajectoriesData and divingData can be 0 or 1.
                // 1 mean that data is present
                // trajectoriesData is related to _gps.bin and _pdr.bin. If 1 one or both those files are present
                // divingData is related to diving, but I don't know how it should be used for now.
                public byte trajectoriesData = 0;
                public byte divingData = 0;

            }

            public short count;
            public List<WorkoutNumbers> workoutNumbers;

            public Integer error = null;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    this.count = 0;
                    return;
                }
                HuaweiTLV container = this.tlv.getObject(0x81);

                this.count = container.getShort(0x02);
                this.workoutNumbers = new ArrayList<>();

                if (this.count == 0)
                    return;

                List<HuaweiTLV> subContainers = container.getObjects(0x85);
                for (HuaweiTLV subContainerTlv : subContainers) {
                    WorkoutNumbers workoutNumber = new WorkoutNumbers();
                    workoutNumber.rawData = subContainerTlv.serialize();
                    workoutNumber.workoutNumber = subContainerTlv.getShort(0x06);
                    workoutNumber.dataCount = subContainerTlv.getShort(0x07);
                    workoutNumber.paceCount = subContainerTlv.getShort(0x08);
                    if (subContainerTlv.contains(0x09)) {
                        workoutNumber.segmentsCount = subContainerTlv.getShort(0x09);
                    }
                    if (subContainerTlv.contains(0x0c)) {
                        workoutNumber.spO2Count = subContainerTlv.getShort(0x0c);
                    }
                    if (subContainerTlv.contains(0x0d)) {
                        workoutNumber.sectionsCount = subContainerTlv.getShort(0x0d);
                    }
                    if (subContainerTlv.contains(0x0e)) {
                        workoutNumber.trajectoriesData = subContainerTlv.getByte(0x0e);
                    }
                    if (subContainerTlv.contains(0x0f)) {
                        workoutNumber.divingData = subContainerTlv.getByte(0x0f);
                    }

                    this.workoutNumbers.add(workoutNumber);
                }
            }
        }
    }

    public static final Map<Integer, String> huaweiIdToKey;

    static {
        huaweiIdToKey = new HashMap<>();
        huaweiIdToKey.put(300010027, "waterType");
        huaweiIdToKey.put(300010024, "avgDepth");
        huaweiIdToKey.put(300010037, "postureType");
    }

    public static class WorkoutTotals {
        public static final byte id = 0x08;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, short number) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                        .put(0x02, number)
                );

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte[] rawData;

            public Integer error = null;

            public short number;
            public byte status = -1; // TODO: enum?
            public int startTime;
            public int endTime;
            public int calories = -1;
            public int distance = -1;
            public int stepCount = -1;
            public int totalTime = -1;
            public int duration = -1;
            public byte type = -1; // TODO: enum?
            public short strokes = -1;
            public short avgStrokeRate = -1;
            public short poolLength = -1; // In cm
            public short laps = -1;
            public short avgSwolf = -1;

            public Integer maxAltitude = null;
            public Integer minAltitude = null;
            public Integer elevationGain = null;
            public Integer elevationLoss = null;

            public int workoutLoad = 0;
            public int workoutAerobicEffect = 0;
            public byte workoutAnaerobicEffect = -1;
            public short recoveryTime = 0;

            public byte minHeartRatePeak = 0;
            public byte maxHeartRatePeak = 0;

            public byte[] recoveryHeartRates = null;

            public byte swimType = -1;

            public int maxMET = 0;

            public byte hrZoneType = -1;

            public short runPaceZone1Min = -1;
            public short runPaceZone2Min = -1;
            public short runPaceZone3Min = -1;
            public short runPaceZone4Min = -1;
            public short runPaceZone5Min = -1;
            public short runPaceZone5Max = -1;

            public short runPaceZone1Time = -1;
            public short runPaceZone2Time = -1;
            public short runPaceZone3Time = -1;
            public short runPaceZone4Time = -1;
            public short runPaceZone5Time = -1;

            public byte algType = 0;

            public int trainingPoints = -1;

            public int longestStreak = -1;
            public int tripped = -1;

            public Map<String, String> additionalValues = new HashMap<>();

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                HuaweiTLV container = this.tlv.getObject(0x81);

                this.rawData = container.serialize();
                this.number = container.getShort(0x02);
                if (container.contains(0x03))
                    this.status = container.getByte(0x03);
                this.startTime = container.getInteger(0x04);
                this.endTime = container.getInteger(0x05);

                if (container.contains(0x06))
                    this.calories = container.getInteger(0x06);
                if (container.contains(0x07))
                    this.distance = container.getInteger(0x07);
                if (container.contains(0x08))
                    this.stepCount = container.getInteger(0x08);
                if (container.contains(0x09))
                    this.totalTime = container.getInteger(0x09);
                if (container.contains(0x0b))
                    this.elevationGain = container.getInteger(0x0b);
                if (container.contains(0x0c)) {
                    byte[] hrData = container.getBytes(0x0c);
                    minHeartRatePeak = hrData[0];
                    maxHeartRatePeak = hrData[1];
                }
                if (container.contains(0x0d))
                    this.workoutLoad = container.getInteger(0x0d);
                if (container.contains(0x0e))
                    this.workoutAerobicEffect = container.getInteger(0x0e);
                if (container.contains(0x10))
                    this.maxMET = container.getInteger(0x10);
                if (container.contains(0x11))
                    this.recoveryTime = container.getShort(0x11);
                if (container.contains(0x12))
                    this.duration = container.getInteger(0x12);
                if (container.contains(0x14))
                    this.type = container.getByte(0x14);
                if (container.contains(0x15))
                    this.swimType = container.getByte(0x15);
                if (container.contains(0x16))
                    this.strokes = container.getShort(0x16);
                if (container.contains(0x17))
                    this.avgStrokeRate = container.getShort(0x17);
                if (container.contains(0x18))
                    this.poolLength = container.getShort(0x18);
                if (container.contains(0x19))
                    this.laps = container.getShort(0x19);
                if (container.contains(0x1a))
                    this.avgSwolf = container.getShort(0x1a);
                if (container.contains(0x1b))
                    this.elevationLoss = container.getInteger(0x1b);
                if (container.contains(0x1c))
                    this.maxAltitude = container.getInteger(0x1c);
                if (container.contains(0x1d))
                    this.minAltitude = container.getInteger(0x1d);
                if (container.contains(0x20))
                    this.workoutAnaerobicEffect = container.getByte(0x20);
                if (container.contains(0x24))
                    this.hrZoneType = container.getByte(0x24);
                if (container.contains(0x50))
                    this.runPaceZone1Min = container.getShort(0x50);
                if (container.contains(0x51))
                    this.runPaceZone2Min = container.getShort(0x51);
                if (container.contains(0x52))
                    this.runPaceZone3Min = container.getShort(0x52);
                if (container.contains(0x53))
                    this.runPaceZone4Min = container.getShort(0x53);
                if (container.contains(0x54))
                    this.runPaceZone5Min = container.getShort(0x54);
                if (container.contains(0x55))
                    this.runPaceZone5Max = container.getShort(0x55);
                if (container.contains(0x56))
                    this.runPaceZone1Time = container.getShort(0x56);
                if (container.contains(0x57))
                    this.runPaceZone2Time = container.getShort(0x57);
                if (container.contains(0x58))
                    this.runPaceZone3Time = container.getShort(0x58);
                if (container.contains(0x59))
                    this.runPaceZone4Time = container.getShort(0x59);
                if (container.contains(0x5a))
                    this.runPaceZone5Time = container.getShort(0x5a);
                if (container.contains(0x5b))
                    this.longestStreak = container.getAsInteger(0x5b);
                if (container.contains(0x5c))
                    this.tripped = container.getAsInteger(0x5c);
                if (container.contains(0x5d))
                    this.algType = container.getByte(0x5d);
                if (container.contains(0x63))
                    this.trainingPoints = container.getShort(0x63);
                if (container.contains(0x66))
                    this.recoveryHeartRates = container.getBytes(0x66);

                for (HuaweiTLV subTlv : container.getObjects(0xe7)) {
                    if(subTlv.contains(0x68) && subTlv.contains(0x69)) {
                        int tag = subTlv.getInteger(0x68);
                        String value = subTlv.getString(0x69); // The watch returns this value always as string.
                        String key = huaweiIdToKey.get(tag);
                        if(key != null)
                            additionalValues.put(key, value);
                    }
                }
            }
        }
    }

    public static class WorkoutData {
        public static final int id = 0x0a;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider,
                    short workoutNumber,
                    short dataNumber,
                    boolean newSteps
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                HuaweiTLV data = new HuaweiTLV()
                        .put(0x02, workoutNumber)
                        .put(0x03, dataNumber);
                if (newSteps) {
                    data.put(0x06, (byte) 1);
                }
                data.put(0x07);

                this.tlv = new HuaweiTLV().put(0x81, data);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class Header {
                public short workoutNumber;
                public short dataNumber;
                public int timestamp;
                public byte interval;
                public short dataCount;
                public byte dataLength;
                public short bitmap; // TODO: can this be enum-like?

                @NonNull
                @Override
                public String toString() {
                    return "Header{" +
                            "workoutNumber=" + workoutNumber +
                            ", dataNumber=" + dataNumber +
                            ", timestamp=" + timestamp +
                            ", interval=" + interval +
                            ", dataCount=" + dataCount +
                            ", dataLength=" + dataLength +
                            ", bitmap=" + bitmap +
                            '}';
                }
            }

            public static class Data {
                // If unknown data is encountered, the whole tlv will be in here so it can be parsed again later
                public byte[] unknownData = null;

                public byte heartRate = -1;
                public short speed = -1;
                public byte stepRate = -1;

                public short cadence = -1;
                public short stepLength = -1;
                public short groundContactTime = -1;
                public byte impact = -1;
                public short swingAngle = -1;
                public byte foreFootLanding = -1;
                public byte midFootLanding = -1;
                public byte backFootLanding = -1;
                public byte eversionAngle = -1;
                public short hangTime = -1;
                public short impactHangRate = -1;
                public byte rideCadence = -1;
                public float ap = 0.0F;
                public float vo = 0.0F;
                public float gtb = 0.0F;
                public float vr = 0.0F;
                public byte ceiling = -1;
                public byte temp = -1;
                public byte spo2 = -1;
                public short cns = -1;

                public short swolf = -1;
                public short strokeRate = -1;

                public short calories = -1;
                public short cyclingPower = -1;
                public short frequency = -1;
                public Integer altitude = null;

                public int timestamp = -1; // Calculated timestamp for this data point

                @NonNull
                @Override
                public String toString() {
                    return "Data{" +
                            "unknownData=" + Arrays.toString(unknownData) +
                            ", heartRate=" + heartRate +
                            ", speed=" + speed +
                            ", stepRate=" + stepRate +
                            ", cadence=" + cadence +
                            ", stepLength=" + stepLength +
                            ", groundContactTime=" + groundContactTime +
                            ", impact=" + impact +
                            ", swingAngle=" + swingAngle +
                            ", foreFootLanding=" + foreFootLanding +
                            ", midFootLanding=" + midFootLanding +
                            ", backFootLanding=" + backFootLanding +
                            ", eversionAngle=" + eversionAngle +
                            ", hangTime=" + hangTime +
                            ", impactHangRate=" + impactHangRate +
                            ", rideCadence=" + rideCadence +
                            ", ap=" + ap +
                            ", vo=" + vo +
                            ", gtb=" + gtb +
                            ", vr=" + vr +
                            ", ceiling=" + ceiling +
                            ", temp=" + temp +
                            ", spo2=" + spo2 +
                            ", cns=" + cns +
                            ", swolf=" + swolf +
                            ", strokeRate=" + strokeRate +
                            ", calories=" + calories +
                            ", cyclingPower=" + cyclingPower +
                            ", frequency=" + frequency +
                            ", altitude=" + altitude +
                            ", timestamp=" + timestamp +
                            '}';
                }
            }

            private final byte[] bitmapLengths = {1, 2, 1, 2, 2, 4, -1, 2, 2, 2};
            private final byte[] innerBitmapLengths = {2, 2, 2, 1, 2, 1, 1, 1, 1, 2, 2, 1, 2, 2, 2, 2, 1, 1, 1, 2};

            public Integer error = null;

            public short workoutNumber;
            public short dataNumber;
            public byte[] rawHeader;
            public byte[] rawData;
            public int innerBitmap = 0;
            public int extraDataLength = 0;

            public Header header;
            public List<Data> dataList;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            /**
             * This is to be able to easily reparse the error data, only accepts tlv bytes
             *
             * @param rawData The TLV bytes
             */
            public Response(byte[] rawData) throws ParseException {
                super(null);
                this.tlv = new HuaweiTLV().parse(rawData);
                this.parseTlv();
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                HuaweiTLV container = this.tlv.getObject(0x81);

                this.workoutNumber = container.getShort(0x02);
                this.dataNumber = container.getShort(0x03);
                this.rawHeader = container.getBytes(0x04);
                this.rawData = container.getBytes(0x05); // TODO: not sure if 5 can also be omitted

                if (container.contains(0x08))
                    this.extraDataLength = container.getAsInteger(0x08);

                if (container.contains(0x09))
                    this.innerBitmap = container.getAsInteger(0x09);
                else
                    this.innerBitmap = 0x01FF; // This seems to be the default


                if (this.rawHeader.length != 14)
                    throw new LengthMismatchException("Workout data header length mismatch.");

                // Calculate inner data length
                int innerDataLength = 0;
                for (byte i = 0; i < innerBitmapLengths.length; i++) {
                    if ((innerBitmap & (1 << i)) != 0) {
                        innerDataLength += innerBitmapLengths[i];
                    }
                }

                //TODO: innerDataLength should be equal to this.extraDataLength. Should correlate to innerBitmap default value.
                //TODO: I suppose innerBitmap should be 0 by default but not sure and don't have devices for testing. So I do not add this check.
                //TODO: is is possible that innerBitmap = 0x01FF only true for AW70 devices. in this case extraDataLength should be properly calculated.

                this.header = new Header();
                ByteBuffer buf = ByteBuffer.wrap(this.rawHeader);
                header.workoutNumber = buf.getShort();
                header.dataNumber = buf.getShort();
                header.timestamp = buf.getInt();
                header.interval = buf.get();
                header.dataCount = buf.getShort();
                header.dataLength = buf.get();
                header.bitmap = buf.getShort();

                // Check data lengths from header
                if (this.header.dataCount * this.header.dataLength != this.rawData.length)
                    throw new LengthMismatchException("Workout data length mismatch with header.");

                // Check data lengths from bitmap
                int dataLength = 0;
                for (byte i = 0; i < bitmapLengths.length; i++) {
                    if ((header.bitmap & (1 << i)) != 0) {
                        if (i == 6) {
                            dataLength += innerDataLength;
                        } else {
                            dataLength += bitmapLengths[i];
                        }
                    }
                }
                dataLength = dataLength * header.dataCount;
                if (dataLength != this.rawData.length)
                    throw new LengthMismatchException("Workout data length mismatch with bitmap.");

                this.dataList = new ArrayList<>();
                buf = ByteBuffer.wrap(this.rawData);
                for (short i = 0; i < header.dataCount; i++) {
                    Data data = new Data();
                    data.timestamp = header.timestamp + header.interval * i;
                    for (byte j = 0; j < bitmapLengths.length; j++) {
                        if ((header.bitmap & (1 << j)) != 0) {
                            switch (j) {
                                case 0:
                                    data.heartRate = buf.get();
                                    break;
                                case 1:
                                    data.speed = buf.getShort();
                                    break;
                                case 2:
                                    data.stepRate = buf.get();
                                    break;
                                case 3:
                                    data.swolf = buf.getShort();
                                    break;
                                case 4:
                                    data.strokeRate = buf.getShort();
                                    break;
                                case 5:
                                    data.altitude = buf.getInt();
                                    break;
                                case 6:
                                    // Inner data, parsing into data
                                    // TODO: function for readability?
                                    for (byte k = 0; k < innerBitmapLengths.length; k++) {
                                        if ((innerBitmap & (1 << k)) != 0) {
                                            switch (k) {
                                                case 0:
                                                    data.cadence = buf.getShort();
                                                    break;
                                                case 1:
                                                    data.stepLength = buf.getShort();
                                                    break;
                                                case 2:
                                                    data.groundContactTime = buf.getShort();
                                                    break;
                                                case 3:
                                                    data.impact = buf.get();
                                                    break;
                                                case 4:
                                                    data.swingAngle = buf.getShort();
                                                    break;
                                                case 5:
                                                    data.foreFootLanding = buf.get();
                                                    break;
                                                case 6:
                                                    data.midFootLanding = buf.get();
                                                    break;
                                                case 7:
                                                    data.backFootLanding = buf.get();
                                                    break;
                                                case 8:
                                                    data.eversionAngle = buf.get(); // buf.get() - 100;
                                                    break;
                                                case 9:
                                                    data.hangTime = buf.getShort();
                                                    break;
                                                case 10:
                                                    data.impactHangRate = buf.getShort();
                                                    break;
                                                case 11:
                                                    data.rideCadence = buf.get();
                                                    break;
                                                case 12:
                                                    data.ap = (float) buf.getShort() / 10.0f;
                                                    break;
                                                case 13:
                                                    data.vo = (float) buf.getShort() / 10.0f;
                                                    break;
                                                case 14:
                                                    data.gtb = (float) buf.getShort() / 100.0f;
                                                    break;
                                                case 15:
                                                    data.vr = (float) buf.getShort() / 10.0f;
                                                    break;
                                                case 16:
                                                    data.ceiling = buf.get();
                                                    break;
                                                case 17:
                                                    data.temp = buf.get();
                                                    break;
                                                case 18:
                                                    data.spo2 = buf.get();
                                                    break;
                                                case 19:
                                                    data.cns = buf.getShort();
                                                    break;
                                                default:
                                                    data.unknownData = this.tlv.serialize();
                                                    // Fix alignment
                                                    for (int l = 0; l < innerBitmapLengths[k]; l++)
                                                        buf.get();
                                                    break;
                                            }
                                        }
                                    }
                                    break;
                                case 7:
                                    data.calories = buf.getShort();
                                    break;
                                case 8:
                                    data.frequency = buf.getShort();
                                    break;
                                case 9:
                                    data.cyclingPower = buf.getShort();
                                    break;
                                default:
                                    data.unknownData = this.tlv.serialize();
                                    // Fix alignment
                                    for (int k = 0; k < bitmapLengths[j]; k++)
                                        buf.get();
                                    break;
                            }
                        }
                    }
                    this.dataList.add(data);
                }
            }
        }
    }

    public static class WorkoutPace {
        public static final int id = 0x0c;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider,
                    short workoutNumber,
                    short paceNumber
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                        .put(0x02, workoutNumber)
                        .put(0x08, paceNumber)
                );

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class Block {
                public short distance = -1;
                public byte type = -1;
                public int pace = -1;
                public short pointIndex = 0;
                public short correction = 0;
                public boolean hasCorrection = false;

                @NonNull
                @Override
                public String toString() {
                    return "Block{" +
                            "distance=" + distance +
                            ", type=" + type +
                            ", pace=" + pace +
                            ", pointIndex=" + pointIndex +
                            ", correction=" + correction +
                            ", hasCorrection=" + hasCorrection +
                            '}';
                }
            }

            public Integer error = null;

            public short workoutNumber;
            public short paceNumber;
            public List<Block> blocks;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                HuaweiTLV container = this.tlv.getObject(0x81);

                this.workoutNumber = container.getShort(0x02);
                this.paceNumber = container.getShort(0x08);

                this.blocks = new ArrayList<>();
                for (HuaweiTLV blockTlv : container.getObjects(0x83)) {
                    Block block = new Block();
                    block.distance = blockTlv.getShort(0x04);
                    block.type = blockTlv.getByte(0x05);
                    block.pace = blockTlv.getInteger(0x06);
                    if (blockTlv.contains(0x07))
                        block.pointIndex = blockTlv.getShort(0x07);
                    if (blockTlv.contains(0x09)) {
                        block.hasCorrection = true;
                        block.correction = blockTlv.getShort(0x09);
                    }
                    blocks.add(block);
                }
            }
        }
    }

    public static class WorkoutSwimSegments {
        public static final int id = 0x0e;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider,
                    short workoutNumber,
                    short segmentNumber
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                        .put(0x02, workoutNumber)
                        .put(0x08, segmentNumber)
                );

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class Block {
                public short distance = -1;
                public byte type = -1;
                public int pace = -1;
                public short pointIndex = 0;
                public short segment = -1;
                public byte swimType = -1;
                public short strokes = -1;
                public short avgSwolf = -1;
                public int time = -1;

                @NonNull
                @Override
                public String toString() {
                    return "Block{" + "distance=" + distance +
                            ", type=" + type +
                            ", pace=" + pace +
                            ", pointIndex=" + pointIndex +
                            ", segment=" + segment +
                            ", swimType=" + swimType +
                            ", strokes=" + strokes +
                            ", awgSwolf=" + avgSwolf +
                            ", time=" + time +
                            '}';
                }
            }


            public Integer error = null;

            public short workoutNumber;
            public short segmentNumber;
            public List<Block> blocks;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                HuaweiTLV container = this.tlv.getObject(0x81);

                this.workoutNumber = container.getShort(0x02);
                this.segmentNumber = container.getShort(0x08);

                this.blocks = new ArrayList<>();
                for (HuaweiTLV blockTlv : container.getObjects(0x83)) {
                    Block block = new Block();

                    block.distance = blockTlv.getShort(0x04);
                    block.type = blockTlv.getByte(0x05);
                    block.pace = blockTlv.getInteger(0x06);
                    if (blockTlv.contains(0x07))
                        block.pointIndex = blockTlv.getShort(0x07);
                    if (blockTlv.contains(0x09))
                        block.segment = blockTlv.getShort(0x09);
                    if (blockTlv.contains(0x0a))
                        block.swimType = blockTlv.getByte(0x0a);
                    if (blockTlv.contains(0x0b))
                        block.strokes = blockTlv.getShort(0x0b);
                    if (blockTlv.contains(0x0c))
                        block.avgSwolf = blockTlv.getShort(0x0c);
                    if (blockTlv.contains(0x0d))
                        block.time = blockTlv.getInteger(0x0d);

                    blocks.add(block);
                }
            }
        }
    }

    public static class WorkoutSpO2 {
        public static final int id = 0x14;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider,
                    short workoutNumber,
                    short spO2Number
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, workoutNumber)
                        .put(0x02, spO2Number)
                        .put(0x03);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class Block {
                public int interval = -1;
                public int value = -1;

                @NonNull
                @Override
                public String toString() {
                    return "Block{" + "interval=" + interval +
                            ", value=" + value +
                            '}';
                }
            }

            public Integer error = null;

            public short spO2Number1; //TODO: meaning of this field
            public short spO2Number2; //TODO: meaning of this field
            public List<Block> blocks;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                this.spO2Number1 = this.tlv.getShort(0x01);
                this.spO2Number2 = this.tlv.getShort(0x02);

                HuaweiTLV container = this.tlv.getObject(0x83);

                this.blocks = new ArrayList<>();
                for (HuaweiTLV blockTlv : container.getObjects(0x84)) {
                    Block block = new Block();

                    if (blockTlv.contains(0x05))
                        block.interval = blockTlv.getAsInteger(0x05);
                    if (blockTlv.contains(0x06))
                        block.value = blockTlv.getAsInteger(0x06);
                    blocks.add(block);
                }
            }
        }
    }

    public static class WorkoutCapability {
        public static final int id = 0x15;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public boolean supportNewStep = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x01)) {
                    int flags = this.tlv.getAsInteger(0x01);
                    supportNewStep = (flags & 2) == 2;
                }
            }
        }
    }

    public static class WorkoutSections {
        public static final int id = 0x16;

        public static class Request extends HuaweiPacket {

            public Request(
                    ParamsProvider paramsProvider,
                    short workoutNumber,
                    short additionalDataNumber
            ) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, workoutNumber)
                        .put(0x02, additionalDataNumber)
                        .put(0x03);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public static class Block {
                public int num = -1;
                public long time = -1;
                public long distance = -1;
                public int pace = -1;
                public int heartRate = -1;
                public int cadence = -1;
                public int stepLength = -1;
                public long totalRise = -1;
                public long totalDescend = -1;
                public int groundContactTime = -1;
                public int groundImpact = -1;
                public int swingAngle = -1;
                public int eversion = -1;

                public int avgCadence = -1;
                public int intervalTrainingType = -1;
                public int divingMaxDepth = -1;
                public int divingUnderwaterTime = -1;
                public int divingBreakTime = -1;


                @NonNull
                @Override
                public String toString() {
                    return "Block{" + "num=" + num +
                            ", time=" + time +
                            ", distance=" + distance +
                            ", pace=" + pace +
                            ", heartRate=" + heartRate +
                            ", cadence=" + cadence +
                            ", stepLength=" + stepLength +
                            ", totalRise=" + totalRise +
                            ", totalDescend=" + totalDescend +
                            ", groundContactTime=" + groundContactTime +
                            ", groundImpact=" + groundImpact +
                            ", swingAngle=" + swingAngle +
                            ", eversion=" + eversion +
                            ", avgCadence=" + avgCadence +
                            ", intervalTrainingType=" + intervalTrainingType +
                            ", divingMaxDepth=" + divingMaxDepth +
                            ", divingUnderwaterTime=" + divingUnderwaterTime +
                            ", divingBreakTime=" + divingBreakTime +
                            '}';
                }
            }

            public Integer error = null;

            public short workoutId;
            public short number; //TODO: meaning of this field
            public List<Block> blocks;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f)) {
                    error = this.tlv.getAsInteger(0x7f);
                    return;
                }

                this.workoutId = this.tlv.getShort(0x01);
                this.number = this.tlv.getShort(0x02);

                HuaweiTLV container = this.tlv.getObject(0x83);

                this.blocks = new ArrayList<>();
                for (HuaweiTLV blockTlv : container.getObjects(0x84)) {
                    Block block = new Block();

                    if (blockTlv.contains(0x05))
                        block.num = blockTlv.getAsInteger(0x05);
                    if (blockTlv.contains(0x06))
                        block.time = blockTlv.getAsLong(0x06);
                    if (blockTlv.contains(0x07))
                        block.distance = blockTlv.getAsLong(0x07);
                    if (blockTlv.contains(0x08))
                        block.pace = blockTlv.getAsInteger(0x08);
                    if (blockTlv.contains(0x09))
                        block.heartRate = blockTlv.getAsInteger(0x09);
                    if (blockTlv.contains(0xa))
                        block.cadence = blockTlv.getAsInteger(0xa);
                    if (blockTlv.contains(0xb))
                        block.stepLength = blockTlv.getAsInteger(0xb);
                    if (blockTlv.contains(0xc))
                        block.totalRise = blockTlv.getAsLong(0xc);
                    if (blockTlv.contains(0xd))
                        block.totalDescend = blockTlv.getAsLong(0xd);
                    if (blockTlv.contains(0xe))
                        block.groundContactTime = blockTlv.getAsInteger(0xe);
                    if (blockTlv.contains(0xf))
                        block.groundImpact = blockTlv.getAsInteger(0xf);
                    if (blockTlv.contains(0x10))
                        block.swingAngle = blockTlv.getAsInteger(0x10);
                    if (blockTlv.contains(0x11))
                        block.eversion = blockTlv.getAsInteger(0x11);

                    if (blockTlv.contains(0x22))
                        block.avgCadence = blockTlv.getAsInteger(0x22);
                    if (blockTlv.contains(0x23))
                        block.intervalTrainingType = blockTlv.getAsInteger(0x23);
                    if (blockTlv.contains(0x28))
                        block.divingMaxDepth = blockTlv.getAsInteger(0x28);
                    if (blockTlv.contains(0x29))
                        block.divingUnderwaterTime = blockTlv.getAsInteger(0x29);
                    if (blockTlv.contains(0x2a))
                        block.divingBreakTime = blockTlv.getAsInteger(0x2a);

                    blocks.add(block);
                }
            }
        }
    }

    public static class NotifyHeartRate {
        public static final int id = 0x17;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Workout.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x01, 0x03);

                this.complete = true;
            }
        }

    }
}
