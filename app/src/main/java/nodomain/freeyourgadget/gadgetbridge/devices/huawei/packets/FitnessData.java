/*  Copyright (C) 2024 Damien Gaignon, Martin.JM, Vitalii Tomin

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiReportThreshold;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiRunPaceConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class FitnessData {

    public static final byte id = 0x07;

    public static class MotionGoal {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           byte goalType,
                           byte frameType,
                           int stepGoal,
                           int calorieGoal,
                           short durationGoal) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                frameType = (frameType == 0x01) ? 0x01 : Type.motion;
                HuaweiTLV subTlv = new HuaweiTLV()
                        .put(0x03, goalType)
                        .put(0x04, frameType);
                stepGoal = ((Type.data & 0x01) != 0x00) ? stepGoal : 0xffffffff;
                if (stepGoal != 0xffffffff)
                    subTlv.put(0x05, stepGoal);
                int calorieGoalFinal = ((Type.data & 0x02) != 0x00) ? calorieGoal : 0xffffffff;
                if (calorieGoalFinal != 0xffffffff) {
                    subTlv.put(0x06, calorieGoalFinal);
                } else if (frameType == 0x01) {
                    subTlv.put(0x06, stepGoal / 0x1e);
                }
                int distanceGoal = ((Type.data & 0x04) != 0x00) ? durationGoal : 0xffffffff;
                if (distanceGoal != 0xffffffff) {
                    subTlv.put(0x07, distanceGoal);
                } else if (frameType == 0x01) {
                    subTlv.put(0x06, stepGoal);
                }
                short durationGoalFinal = ((Type.data & 0x08) != 0x00) ? durationGoal : 0xffffffff;
                if (durationGoalFinal != 0xffffffff) {
                    subTlv.put(0x08, durationGoalFinal);
                }
                HuaweiTLV containerTlv = new HuaweiTLV().put(0x82, subTlv);
                this.tlv = new HuaweiTLV()
                        .put(0x81, containerTlv);
            }
        }
    }

    public static class UserInfo {
        public static final byte id = 0x02;

        public static class UserInfoData {
            public static final int GENDER_MALE = 1;
            public static final int GENDER_FEMALE = 2;
            public static final int GENDER_UNKNOWN = 3;

            private static final float RUN_CF = 0.83f;
            private static final float WALK_CF = 0.42f;

            public static final int DEFAULT_HEIGHT = 175;
            public static final int DEFAULT_WEIGHT = 65;

            private final int height;
            private final float weight;
            private final int age;
            private final int birthday;
            private final byte gender;

            private final boolean unknownGenderSupported;

            private final boolean vo2Supported;
            private final int vo2Max;
            private final int vo2Time;

            private final boolean precisionWeightSupported;

            public UserInfoData(int height, float weight, int age, int birthday, byte gender, boolean unknownGenderSupported, boolean vo2Supported, int vo2Max, int vo2Time, boolean precisionWeightSupported) {
                this.height = height;
                this.weight = weight;
                this.age = age;
                this.birthday = birthday;
                this.gender = gender;
                this.unknownGenderSupported = unknownGenderSupported;
                this.vo2Supported = vo2Supported;
                this.vo2Max = vo2Max;
                this.vo2Time = vo2Time;
                this.precisionWeightSupported = precisionWeightSupported;
            }

            public int getHeight() {
                if (this.height > 0)
                    return this.height;
                return DEFAULT_HEIGHT;
            }

            public float getWeight() {
                if (this.weight > 0)
                    return this.weight;
                return DEFAULT_WEIGHT;
            }

            public int getAge() {
                return age;
            }

            public int getBirthday() {
                return birthday;
            }

            public byte getGender() {
                return gender;
            }

            public boolean isUnknownGenderSupported() {
                return unknownGenderSupported;
            }

            public boolean isVo2Supported() {
                return vo2Supported;
            }

            public int getVo2Max() {
                return vo2Max;
            }

            public int getVo2Time() {
                return vo2Time;
            }

            public boolean isPrecisionWeightSupported() {
                return precisionWeightSupported;
            }

            public boolean isGenderValid() {
                return this.gender == GENDER_MALE || this.gender == GENDER_FEMALE || this.gender == GENDER_UNKNOWN;
            }

            public boolean isBirthdayValid() {
                return this.birthday > 0;
            }

        }

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, UserInfoData info) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                int height = info.getHeight();
                float weight = info.getWeight();

                byte bmiWalk = (byte) Math.round(UserInfoData.WALK_CF * height);
                byte bmiRun = (byte) Math.round(UserInfoData.RUN_CF * height);

                this.tlv = new HuaweiTLV()
                        .put(0x01, (byte) height)
                        .put(0x02, (byte) weight)
                        .put(0x03, (byte) info.getAge());
                if (info.isBirthdayValid())
                    this.tlv.put(0x04, info.getBirthday());
                if (info.isGenderValid()) {
                    if (info.unknownGenderSupported || info.gender != UserInfoData.GENDER_UNKNOWN)
                        this.tlv.put(0x05, info.gender);
                }
                this.tlv.put(0x06, bmiWalk);
                this.tlv.put(0x07, bmiRun);

                if (info.vo2Supported) {
                    this.tlv.put(0x08, info.getVo2Max());
                    this.tlv.put(0x09, info.getVo2Time());
                }

                if (info.precisionWeightSupported) {
                    int precisionWeight = Math.round(weight * 100f);
                    this.tlv.put(0x0b, precisionWeight);
                }

            }
        }
    }

    public static class MessageCount {
        public static final byte sleepId = 0x0C;
        public static final byte stepId = 0x0A;

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    byte commandId,
                    int start,
                    int end
            ) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = commandId;

                this.tlv = new HuaweiTLV()
                        .put(0x81)
                        .put(0x03, start)
                        .put(0x04, end);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public short count;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                this.count = this.tlv.getObject(0x81).getShort(0x02);
                this.complete = true;
            }
        }
    }

    public static class MessageData {
        public static final byte sleepId = 0x0D;
        public static final byte stepId = 0x0B;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte commandId, short count) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = commandId;

                this.tlv = new HuaweiTLV()
                        .put(0x81, new HuaweiTLV()
                                .put(0x02, count)
                        );

                this.complete = true;
            }
        }

        public static class SleepResponse extends HuaweiPacket {
            public static class SubContainer {
                public byte type;
                public byte[] timestamp;

                @Override
                public String toString() {
                    return "SubContainer{" +
                            "type=" + type +
                            ", timestamp=" + Arrays.toString(timestamp) +
                            '}';
                }
            }

            public short number;
            public List<SubContainer> containers;

            public SleepResponse(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = sleepId;
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV container = this.tlv.getObject(0x81);
                List<HuaweiTLV> subContainers = container.getObjects(0x83);

                this.number = container.getShort(0x02);
                this.containers = new ArrayList<>();
                for (HuaweiTLV subContainerTlv : subContainers) {
                    SubContainer subContainer = new SubContainer();
                    subContainer.type = subContainerTlv.getByte(0x04);
                    subContainer.timestamp = subContainerTlv.getBytes(0x05);
                    this.containers.add(subContainer);
                }
            }
        }

        public static class StepResponse extends HuaweiPacket {
            public static class SubContainer {
                public static class TV {
                    public final byte bitmap;
                    public final byte tag;
                    public final short value;

                    public TV(byte bitmap, byte tag, short value) {
                        this.bitmap = bitmap;
                        this.tag = tag;
                        this.value = value;
                    }

                    @Override
                    public String toString() {
                        return "TV{" +
                                "bitmap=" + bitmap +
                                ", tag=" + tag +
                                ", value=" + value +
                                '}';
                    }
                }

                /*
                 * Data directly from packet
                 */
                public byte timestampOffset;
                public byte[] data;

                /*
                 * Inferred data
                 */
                public int timestamp;

                public List<TV> parsedData = null;
                public String parsedDataError = "";

                public int steps = -1;
                public int calories = -1;
                public int distance = -1;
                public int heartrate = -1;

                public int spo = -1;

                public List<TV> unknownTVs = null;

                @Override
                public String toString() {
                    return "SubContainer{" +
                            "timestampOffset=" + timestampOffset +
                            ", data=" + Arrays.toString(data) +
                            ", timestamp=" + timestamp +
                            ", parsedData=" + parsedData +
                            ", parsedDataError='" + parsedDataError + '\'' +
                            ", steps=" + steps +
                            ", calories=" + calories +
                            ", distance=" + distance +
                            ", spo=" + spo +
                            ", unknownTVs=" + unknownTVs +
                            '}';
                }
            }

            public short number;
            public int timestamp;
            public List<SubContainer> containers;

            private static final List<Byte> singleByteTagListBitmap1 = new ArrayList<>();

            static {
                singleByteTagListBitmap1.add((byte) 0x20);
                singleByteTagListBitmap1.add((byte) 0x40);
            }

            public StepResponse(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = stepId;
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV container = this.tlv.getObject(0x81);
                List<HuaweiTLV> subContainers = container.getObjects(0x84);

                this.number = container.getShort(0x02);
                this.timestamp = container.getInteger(0x03);
                this.containers = new ArrayList<>();
                for (HuaweiTLV subContainerTlv : subContainers) {
                    SubContainer subContainer = new SubContainer();
                    subContainer.timestampOffset = subContainerTlv.getByte(0x05);
                    subContainer.timestamp = this.timestamp + 60 * subContainer.timestampOffset;
                    subContainer.data = subContainerTlv.getBytes(0x06);
                    parseData(subContainer, subContainer.data);
                    this.containers.add(subContainer);
                }
            }

            private static void parseData(SubContainer returnValue, byte[] data) {
                int i = 0;

                if (data.length <= 0) {
                    returnValue.parsedData = null;
                    returnValue.parsedDataError = "Data is missing feature bitmap.";
                    return;
                }
                byte featureBitmap1 = data[i++];

                byte featureBitmap2 = 0;
                if ((featureBitmap1 & 128) != 0) {
                    if (data.length <= i) {
                        returnValue.parsedData = null;
                        returnValue.parsedDataError = "Data is missing second feature bitmap.";
                        return;
                    }
                    featureBitmap2 = data[i++];
                }

                returnValue.parsedData = new ArrayList<>();
                returnValue.unknownTVs = new ArrayList<>();

                // The greater than zero check is because Java is always signed, so we only check 7 bits
                for (byte bitToCheck = 1; bitToCheck > 0; bitToCheck <<= 1) {
                    if ((featureBitmap1 & bitToCheck) != 0) {
                        short value;

                        if (singleByteTagListBitmap1.contains(bitToCheck)) {
                            if (data.length - 1 < i) {
                                returnValue.parsedData = null;
                                returnValue.parsedDataError = "Data is too short for selected features.";
                                return;
                            }

                            value = data[i++];
                        } else {
                            if (data.length - 2 < i) {
                                returnValue.parsedData = null;
                                returnValue.parsedDataError = "Data is too short for selected features.";
                                return;
                            }

                            value = (short) ((data[i++] & 0xFF) << 8 | (data[i++] & 0xFF));
                        }

                        // The bitToCheck is used as tag, which may not be optimal, but works
                        SubContainer.TV tv = new SubContainer.TV((byte) 1, bitToCheck, value);
                        returnValue.parsedData.add(tv);

                        if (bitToCheck == 0x02)
                            returnValue.steps = value;
                        else if (bitToCheck == 0x04)
                            returnValue.calories = value;
                        else if (bitToCheck == 0x08)
                            returnValue.distance = value;
                        else if (bitToCheck == 0x40)
                            returnValue.heartrate = value;
                        else
                            returnValue.unknownTVs.add(tv);
                    }
                }

                if (featureBitmap2 != 0) {
                    // We want to check 8 bits here, and java is java, so we use a short
                    for (short bitToCheck = 1; bitToCheck < 0x0100; bitToCheck <<= 1) {
                        if ((featureBitmap2 & bitToCheck) != 0) {
                            if (data.length - 1 < i) {
                                returnValue.parsedData = null;
                                returnValue.parsedDataError = "Data is too short for selected features.";
                                return;
                            }

                            byte value = data[i++];

                            SubContainer.TV tv = new SubContainer.TV((byte) 2, (byte) bitToCheck, value);
                            returnValue.parsedData.add(tv);

                            if (bitToCheck == 0x01)
                                returnValue.spo = value;
                            else
                                returnValue.unknownTVs.add(tv);
                        }
                    }
                }
            }
        }
    }

    public static class FitnessTotals {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {

            public int totalSteps = 0;
            public int totalCalories = 0;
            public int totalDistance = 0;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV container = this.tlv.getObject(0x81);
                List<HuaweiTLV> containers = container.getObjects(0x83);

                for (HuaweiTLV tlv : containers) {
                    if (tlv.contains(0x05))
                        totalSteps += tlv.getInteger(0x05);
                    if (tlv.contains(0x06))
                        totalCalories += tlv.getShort(0x06);
                    if (tlv.contains(0x07))
                        totalDistance += tlv.getInteger(0x07);
                }

                this.complete = true;
            }
        }
    }

    public static class ActivityReminder {
        public static final byte id = 0x07;

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    boolean longSitSwitch,
                    byte longSitInterval,
                    byte[] longSitStart,
                    byte[] longSitEnd,
                    byte cycle
            ) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x81, new HuaweiTLV()
                                .put(0x02, longSitSwitch)
                                .put(0x03, longSitInterval)
                                .put(0x04, longSitStart)
                                .put(0x05, longSitEnd)
                                .put(0x06, cycle)
                        );

                this.complete = true;
            }
        }
    }

    public static class DeviceReportThreshold {
        public static final byte id = 0xe;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, List<HuaweiReportThreshold> thresholds) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;


                HuaweiTLV subTlv = new HuaweiTLV();
                for (HuaweiReportThreshold th : thresholds) {
                    subTlv.put(0x02, th.getBytes());
                }
                this.tlv = new HuaweiTLV().put(0x81, subTlv);
                this.complete = true;
            }
        }
    }

    public static class HeartRateZoneConfigPacket {
        // It can use two IDs with basically the same format.
        public static final byte id_simple = 0x13;
        public static final byte id_extended = 0x21;

        public static class Request extends HuaweiPacket {
            private Request(
                    ParamsProvider paramsProvider,
                    byte id,
                    HeartRateZonesConfig heartRateZonesConfig
            ) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                HuaweiTLV subTlv = new HuaweiTLV().
                        put(0x08, heartRateZonesConfig.getWarningEnable());

                if (
                        heartRateZonesConfig.hasValidMHRData() &&
                        heartRateZonesConfig.getWarningHRLimit() > 0 &&
                        heartRateZonesConfig.getMaxHRThreshold() > 0
                ) {
                    subTlv
                            .put(0x09, (byte) heartRateZonesConfig.getWarningHRLimit())
                            .put(0x02, (byte) heartRateZonesConfig.getMHRWarmUp())
                            .put(0x03, (byte) heartRateZonesConfig.getMHRFatBurning())
                            .put(0x04, (byte) heartRateZonesConfig.getMHRAerobic())
                            .put(0x05, (byte) heartRateZonesConfig.getMHRAnaerobic())
                            .put(0x06, (byte) heartRateZonesConfig.getMHRExtreme())
                            .put(0x07, (byte) heartRateZonesConfig.getMaxHRThreshold())
                            .put(0x0b, (byte) heartRateZonesConfig.getMaxHRThreshold());
                }

                if (id == id_extended && heartRateZonesConfig.hasValidHRRData()) {
                    subTlv
                            .put(0x0d, (byte) heartRateZonesConfig.getHRRBasicAerobic())
                            .put(0x0e, (byte) heartRateZonesConfig.getHRRAdvancedAerobic())
                            .put(0x0f, (byte) heartRateZonesConfig.getHRRLactate())
                            .put(0x10, (byte) heartRateZonesConfig.getHRRBasicAnaerobic())
                            .put(0x11, (byte) heartRateZonesConfig.getHRRAdvancedAnaerobic());
                }

                if (id == id_extended && heartRateZonesConfig.getRestHeartRate() > 0) {
                    subTlv
                            .put(0x0a, (byte) heartRateZonesConfig.getCalculateMethod())
                            .put(0x0c, (byte) heartRateZonesConfig.getRestHeartRate());
                }

                this.tlv = new HuaweiTLV().put(0x81, subTlv);

                this.complete = true;
            }

            public static Request requestSimple(ParamsProvider paramsProvider, HeartRateZonesConfig heartRateZonesConfig) {
                return new Request(paramsProvider, id_simple, heartRateZonesConfig);
            }

            public static Request requestExtended(ParamsProvider paramsProvider, HeartRateZonesConfig heartRateZonesConfig) {
                return new Request(paramsProvider, id_extended, heartRateZonesConfig);
            }
        }
    }

    public static class TruSleep {
        public static final byte id = 0x16;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean truSleepSwitch) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, truSleepSwitch);

                this.complete = true;
            }
        }
    }

    public static class EnableAutomaticHeartrate {
        public static final byte id = 0x17;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enableAutomaticHeartrate) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enableAutomaticHeartrate);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class EnableRealtimeHeartRate {
        public static final byte id = 0x1c;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enableRealtimeHeartRate) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enableRealtimeHeartRate);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class HighHeartRateAlert {
        public static final byte id = 0x1d;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enabled, byte highHeartRateAlert) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enabled);
                if(enabled)
                        this.tlv.put(0x02, highHeartRateAlert);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class LowHeartRateAlert {
        public static final byte id = 0x22;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enabled, byte lowHeartRateAlert) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enabled);
                if(enabled)
                    this.tlv.put(0x02, lowHeartRateAlert);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class NotifyRestHeartRate {
        public static final byte id = 0x23;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, 0x01);

                this.complete = true;
            }
        }
    }

    public static class EnableAutomaticSpo {
        public static final byte id = 0x24;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enableAutomaticSpo) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enableAutomaticSpo);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class LowSpoAlert {
        public static final byte id = 0x25;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean enabled, byte lowHeartRateAlert) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, enabled);
                if(enabled)
                    this.tlv.put(0x02, lowHeartRateAlert);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class RunPaceConfig {
        public static final byte id = 0x28;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, final HuaweiRunPaceConfig runPaceConfig) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, (short) runPaceConfig.getZone1JogMin())
                        .put(0x02, (short) runPaceConfig.getZone2MarathonMin())
                        .put(0x03, (short) runPaceConfig.getZone3LactateThresholdMin())
                        .put(0x04, (short) runPaceConfig.getZone4AnaerobicMin())
                        .put(0x05, (short) runPaceConfig.getZone5HIITRunMin())
                        .put(0x06, (short) runPaceConfig.getZone5HIITRunMax());
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {

            public boolean isOk;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = FitnessData.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                isOk = this.tlv.getInteger(0x7f) == 0x000186A0;
            }
        }

    }

    public static class MediumToStrengthThreshold {
        public static final byte id = 0x29;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           byte walkRun,
                           byte climb,
                           byte heartRate,
                           byte cycleSpeed,
                           byte sample,
                           byte countLength,
                           int walkRunSpeed,
                           int walkRunWithHeartRate) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                if (walkRun < 0x00 || walkRun > 0xc8) walkRun = 0x6E;
                if (climb < 0x0 || climb > 0xc8) climb = 0x3c;
                if (heartRate < 0x0 || heartRate > 0x64) heartRate = 0x40;
                if (cycleSpeed < 0x0 || cycleSpeed > 0xff) cycleSpeed = 0x50;
                if (sample < 0x1 || sample > 0xa) sample = 0x3;
                if (countLength < 0x1 || countLength > 0xa) countLength = 0x5;
                if (countLength < sample) countLength = sample;

                this.tlv = new HuaweiTLV()
                        .put(0x01, walkRun)
                        .put(0x02, climb)
                        .put(0x03, heartRate)
                        .put(0x04, cycleSpeed)
                        .put(0x05, sample)
                        .put(0x06, countLength);
                if (walkRunSpeed != -1) {
                    this.tlv.put(0x07, (byte) walkRunSpeed);
                }
                if (walkRunWithHeartRate != -1) {
                    this.tlv.put(0x08, (byte) walkRunWithHeartRate);
                }
                this.complete = true;
            }
        }
    }

    public static class SkinTemperatureMeasurement {
        public static final byte id = 0x2a;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, boolean temperatureSwitch) {
                super(paramsProvider);

                this.serviceId = FitnessData.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, (byte) 0x01)
                        .put(0x02, temperatureSwitch);

                this.complete = true;
            }
        }
    }

    public static class Type {
        // TODO: enum?

        public static final byte goal = 0x01;
        public static final byte motion = 0x00;
        public static final byte data = 0x01;
    }
}
