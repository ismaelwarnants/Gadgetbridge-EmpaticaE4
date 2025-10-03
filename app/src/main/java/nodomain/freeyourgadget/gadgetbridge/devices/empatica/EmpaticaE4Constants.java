package nodomain.freeyourgadget.gadgetbridge.devices.empatica;

import java.util.UUID;

public class EmpaticaE4Constants {

    // Services
    public static final UUID SENSOR_SERVICE = UUID.fromString("00003ea0-0000-1000-8000-00805f9b34fb");
    public static final UUID CMD_SERVICE = UUID.fromString("00003e70-0000-1000-8000-00805f9b34fb");
    public static final UUID STATUS_SERVICE = UUID.fromString("00003eb0-0000-1000-8000-00805f9b34fb");

    // Characteristics
    public static final UUID BVP_CHARACTERISTIC = UUID.fromString("00003ea1-0000-1000-8000-00805f9b34fb");
    public static final UUID GSR_CHARACTERISTIC = UUID.fromString("00003ea8-0000-1000-8000-00805f9b34fb");
    public static final UUID ACC_CHARACTERISTIC = UUID.fromString("00003ea3-0000-1000-8000-00805f9b34fb");
    public static final UUID ST_CHARACTERISTIC = UUID.fromString("00003ea6-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHARACTERISTIC = UUID.fromString("00003eb3-0000-1000-8000-00805f9b34fb");
    public static final UUID CONTROL_CHARACTERISTIC = UUID.fromString("00003eb2-0000-1000-8000-00805f9b34fb");
    public static final UUID CMD_CHARACTERISTIC = UUID.fromString("00003e71-0000-1000-8000-00805f9b34fb");
}