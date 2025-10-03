/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiDictTypes;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PDataDictionarySyncService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PDataDictionarySyncService.class);

    public static final String MODULE = "hw.unitedevice.datadictionarysync";

    private final AtomicBoolean serviceAvailable = new AtomicBoolean(false);


    private List<Integer> classesToSync = null;

    public interface DictionarySyncCallback {
        long onGetLastDataSyncTimestamp(int dictClass);
        void onData(int dictClass, List<HuaweiP2PDataDictionarySyncService.DictData> dictData);
        void onComplete(boolean complete);
    }

    private final Map<Integer, DictionarySyncCallback> currentRequests = new HashMap<>();

    public HuaweiP2PDataDictionarySyncService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("P2PDataDictionarySyncService");
    }

    @Override
    public String getModule() {
        return HuaweiP2PDataDictionarySyncService.MODULE;
    }

    @Override
    public String getPackage() {
        return "hw.watch.health.filesync";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    public static byte[] dictToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public List<Integer> checkSupported(HuaweiCoordinator coordinator, List<Integer> list) {
        List<Integer> result = new ArrayList<>();

        for(Integer cl: list) {
            if((cl == HuaweiDictTypes.BODY_TEMPERATURE_CLASS || cl == HuaweiDictTypes.SKIN_TEMPERATURE_CLASS) && coordinator.supportsTemperature()) {
                result.add(cl);
            } else if(cl == HuaweiDictTypes.BLOOD_PRESSURE_CLASS && coordinator.supportsBloodPressure()) {
                result.add(cl);
            }
        }
        return result;
    }

    public void startSync(List<Integer> dictClasses, DictionarySyncCallback callback) {
        LOG.info("P2PDataDictionarySyncService startSync {}", dictClasses);
        if(callback == null) {
            LOG.error("P2PDataDictionarySyncService  startSync callback is null");
            return;
        }
        classesToSync = dictClasses;
        if(classesToSync.isEmpty()) {
            callback.onComplete(false);
            return;
        }
        sendSyncRequest(classesToSync.remove(0), 0, callback);
    }

    private void sendSyncRequest(int dictClass, long startTime, DictionarySyncCallback callback) {

        LOG.info("P2PDataDictionarySyncService class {}", dictClass);
        if(callback == null) {
            LOG.error("P2PDataDictionarySyncService sendSyncRequest callback is null");
            return;
        }

        if (!serviceAvailable.get()) {
            LOG.info("P2PDataDictionarySyncService not available");
            callback.onComplete(false);
            return;
        }

        if(currentRequests.containsKey(dictClass)) {
            LOG.info("P2PDataDictionarySyncService current class in progress");
            callback.onComplete(false);
            return;
        }
        if(startTime == 0) {
            startTime = callback.onGetLastDataSyncTimestamp(dictClass);
        }
        if(startTime < 0) {
            LOG.info("P2PDataDictionarySyncService start time is less then 0");
            callback.onComplete(false);
            return;
        }

        if(startTime > 0) {
            startTime += 1000;
        }

        HuaweiTLV tlv = new HuaweiTLV()
                .put(0x1, (byte) 1)
                .put(0x2, dictToBytes(dictClass)) //-- skin temperature
                .put(0x5, startTime)
                .put(0x6, System.currentTimeMillis())
                .put(0x0d, (byte) 1);
        byte[] data = tlv.serialize();
        if (data == null) {
            LOG.error("Incorrect data");
            callback.onComplete(false);
            return;
        }


        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x1); // type tlv
        packet.put(data);
        packet.flip();

        LOG.info("P2PDataDictionarySyncService send command");
        currentRequests.put(dictClass, callback);

        sendCommand(packet.array(), null);
    }

    @Override
    public void registered() {
        sendPing((code, data) -> {
            if ((byte) code != (byte) 0xca)
                return;
            serviceAvailable.set(true);
        });
    }

    @Override
    public void unregister() {
        serviceAvailable.set(false);
    }

    public static class DictData {
        public static class DictDataValue {
            private final int dataType;
            private final byte tag;
            private final byte[] value;

            public DictDataValue(int dataType, byte tag, byte[] value) {
                this.dataType = dataType;
                this.tag = tag;
                this.value = value;
            }

            public int getDataType() {
                return dataType;
            }

            public byte getTag() {
                return tag;
            }

            public byte[] getValue() {
                return value;
            }

            @NonNull
            @Override
            public String toString() {
                final StringBuffer sb = new StringBuffer("HuaweiDictDataValue{");
                sb.append("dataType=").append(dataType);
                sb.append(", tag=").append(tag);
                sb.append(", value=");
                if (value == null) sb.append("null");
                else {
                    sb.append('[');
                    for (int i = 0; i < value.length; ++i)
                        sb.append(i == 0 ? "" : ", ").append(value[i]);
                    sb.append(']');
                }
                sb.append('}');
                return sb.toString();
            }
        }


        private final int dictClass;
        private final long startTimestamp;
        private final long endTimestamp;
        private final long modifyTimestamp;
        private final List<DictDataValue> data;

        public DictData(int dictClass, long startTimestamp, long endTimestamp, long modifyTimestamp, List<DictDataValue> data) {
            this.dictClass = dictClass;
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
            this.modifyTimestamp = modifyTimestamp;
            this.data = data;
        }

        public int getDictClass() { return dictClass; }

        public long getStartTimestamp() {
            return startTimestamp;
        }

        public long getEndTimestamp() {
            return endTimestamp;
        }

        public long getModifyTimestamp() {
            return modifyTimestamp;
        }

        public List<DictDataValue> getData() {
            return data;
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("HuaweiDictSample{");
            sb.append("startTime=").append(startTimestamp);
            sb.append(", endTime=").append(endTimestamp);
            sb.append(", modifyTime=").append(modifyTimestamp);
            sb.append(", data=").append(data);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("P2PDataDictionarySyncService handleData: {}", data.length);
        if (data[0] == 1) {
            DictionarySyncCallback callback = null;
            try {
                HuaweiTLV tlv = new HuaweiTLV();
                tlv.parse(data, 1, data.length - 1);

                int operation = tlv.getInteger(0x01);
                int dictClass = tlv.getInteger(0x02);

                if(!currentRequests.containsKey(dictClass)) {
                    return;
                }
                callback = currentRequests.remove(dictClass);

                if(callback == null) {
                    return;
                }

                if(operation != 1) {
                    return;
                    //I never see value differ from 1. So I don't know how to interpret others. Just ignore for now
                    //callback.onComplete(true);
                }

                //NOTE: all tags with high bit set should be parsed as container

                List<DictData> result =  new ArrayList<>();

                long lastTimestamp = 0;

                for (HuaweiTLV blockTlv : tlv.getObjects(0x83)) {
                    for (HuaweiTLV l : blockTlv.getObjects(0x84)) {
                        //5 - start time, 6 - end time, 0xc - modify time
                        long startTimestamp = l.getLong(0x5);
                        long endTimestamp = 0;
                        long modifyTimestamp = 0;
                        if (l.contains(0x6))
                            endTimestamp = l.getLong(0x6);
                        if (l.contains(0xc))
                            modifyTimestamp = l.getLong(0xc);
                        List<DictData.DictDataValue> dataValues = new ArrayList<>();
                        for (HuaweiTLV l1 : l.getObjects(0x87)) {
                            for (HuaweiTLV ll : l1.getObjects(0x88)) {
                                int type = ll.getInteger(0x9);
                                if (ll.contains(0xa))
                                    dataValues.add(new DictData.DictDataValue(type, (byte) 0xa, ll.getBytes(0xa)));
                                if (ll.contains(0xb))
                                    dataValues.add(new DictData.DictDataValue(type, (byte) 0xb, ll.getBytes(0xb)));
                            }
                        }
                        result.add(new DictData(dictClass, startTimestamp, endTimestamp, modifyTimestamp, dataValues));
                        lastTimestamp = Math.max(lastTimestamp, Math.max(endTimestamp, modifyTimestamp));
                    }
                }

                callback.onData(dictClass, result);

                if (!result.isEmpty()) {
                    sendSyncRequest(dictClass, lastTimestamp, callback);
                } else {
                    if(classesToSync.isEmpty()) {
                        classesToSync = null;
                        callback.onComplete(true);
                    } else {
                        sendSyncRequest(classesToSync.remove(0), 0, callback);
                    }
                }
            } catch (HuaweiPacket.MissingTagException e) {
                LOG.error("P2PDataDictionarySyncService parse error", e);
                if(callback != null) {
                    callback.onComplete(false);
                }
            }
        }
    }

    public static HuaweiP2PDataDictionarySyncService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PDataDictionarySyncService) manager.getRegisteredService(HuaweiP2PDataDictionarySyncService.MODULE);
    }

}
