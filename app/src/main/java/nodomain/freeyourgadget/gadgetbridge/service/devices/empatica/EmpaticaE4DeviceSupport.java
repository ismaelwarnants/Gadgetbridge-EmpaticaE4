package nodomain.freeyourgadget.gadgetbridge.service.devices.empatica;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.empatica.EmpaticaE4Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class EmpaticaE4DeviceSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(EmpaticaE4DeviceSupport.class);

    public EmpaticaE4DeviceSupport() {
        super(LOG);
        // Add all the services the E4 uses
        addSupportedService(EmpaticaE4Constants.SENSOR_SERVICE);
        addSupportedService(EmpaticaE4Constants.STATUS_SERVICE);
        addSupportedService(EmpaticaE4Constants.CMD_SERVICE);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing Empatica E4...");
        // Mark the device as initializing
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // Set placeholder firmware versions to prevent database errors
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // Request a larger MTU for better data throughput.
        builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.RequestMtuAction(247));

        // Wait a moment after MTU request
        builder.add(new nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction(500));

        // Sequentially enable notifications for all required characteristics.
        builder.notify(getCharacteristic(EmpaticaE4Constants.BVP_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(EmpaticaE4Constants.GSR_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(EmpaticaE4Constants.ACC_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(EmpaticaE4Constants.ST_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(EmpaticaE4Constants.BATTERY_CHARACTERISTIC), true);
        builder.notify(getCharacteristic(EmpaticaE4Constants.CONTROL_CHARACTERISTIC), true);

        // After all notifications are enabled, send the command to start streaming.
        // This is the authorization step that was failing before.
        byte[] timestamp = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (System.currentTimeMillis() / 1000))
                .array();
        byte[] command = { 1, timestamp[0], timestamp[1], timestamp[2], timestamp[3] };
        builder.write(getCharacteristic(EmpaticaE4Constants.CMD_CHARACTERISTIC), command);
        LOG.info("Start streaming command sent to Empatica E4.");

        // Mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        LOG.info("Empatica E4 initialization sequence complete.");
        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        super.onCharacteristicChanged(gatt, characteristic, data);

        UUID characteristicUUID = characteristic.getUuid();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        if (EmpaticaE4Constants.BATTERY_CHARACTERISTIC.equals(characteristicUUID)) {
            // Battery level is a single byte representing the percentage.
            float batteryLevel = buffer.get();
            GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
            batteryInfo.level = (short) batteryLevel;
            handleGBDeviceEvent(batteryInfo);
            LOG.info(String.format("Battery: %.0f %%", batteryLevel));

        } else if (EmpaticaE4Constants.BVP_CHARACTERISTIC.equals(characteristicUUID)) {
            // BVP is a stream of float values. The BVP signal is proportional to the amount of red light reflected by the blood vessels.
            // It is not directly in a standard unit but is a relative measure.
            while (buffer.hasRemaining()) {
                float bvp = buffer.getFloat();
                LOG.info(String.format("BVP: %.4f", bvp));
            }

        } else if (EmpaticaE4Constants.GSR_CHARACTERISTIC.equals(characteristicUUID)) {
            // GSR (EDA) is a stream of float values.
            // Unit: microsiemens (μS)
            while (buffer.hasRemaining()) {
                float gsr = buffer.getFloat();
                // Convert to microsiemens by multiplying by 1,000,000 (Empatica sends it in MegaOhms which needs conversion)
                // We'll log the raw value and a converted value for clarity. The actual conversion might need tweaking based on device specifics.
                LOG.info(String.format("GSR/EDA: %.6f μS", gsr * 1000));
            }

        } else if (EmpaticaE4Constants.ACC_CHARACTERISTIC.equals(characteristicUUID)) {
            // Accelerometer data comes in packets of 3 signed bytes (x, y, z).
            // Unit: g-force. The raw value is scaled, where 1g = 64.
            while (buffer.remaining() >= 3) {
                int x = buffer.get(); // signed byte
                int y = buffer.get(); // signed byte
                int z = buffer.get(); // signed byte
                // Convert raw values to g's
                float x_g = x / 64.0f;
                float y_g = y / 64.0f;
                float z_g = z / 64.0f;
                LOG.info(String.format("Accelerometer: X: %.2fg, Y: %.2fg, Z: %.2fg", x_g, y_g, z_g));
            }

        } else if (EmpaticaE4Constants.ST_CHARACTERISTIC.equals(characteristicUUID)) {
            // Skin Temperature is a stream of float values.
            // Unit: degrees Celsius (°C)
            while (buffer.hasRemaining()) {
                float temperature = buffer.getFloat();
                LOG.info(String.format("Temperature: %.2f °C", temperature));
            }
        } else {
            LOG.debug("Unhandled characteristic changed: " + characteristicUUID + " value: " + GB.hexdump(data));
        }

        return true;
    }

    /*@Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        // The superclass handles some basic logging, it's good practice to call it.
        super.onCharacteristicChanged(gatt, characteristic, data);

        UUID characteristicUUID = characteristic.getUuid();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        if (EmpaticaE4Constants.BATTERY_CHARACTERISTIC.equals(characteristicUUID)) {
            GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
            batteryInfo.level = data[0];
            handleGBDeviceEvent(batteryInfo);
            LOG.info("Battery Level: " + batteryInfo.level);
        } else if (EmpaticaE4Constants.BVP_CHARACTERISTIC.equals(characteristicUUID)) {
            float bvp = buffer.getFloat();
            LOG.info("BVP: " + bvp);
        } else if (EmpaticaE4Constants.GSR_CHARACTERISTIC.equals(characteristicUUID)) {
            float gsr = buffer.getFloat();
            LOG.info("GSR/EDA: " + gsr);
        } else if (EmpaticaE4Constants.ACC_CHARACTERISTIC.equals(characteristicUUID)) {
            // Accelerometer data is 3 signed bytes (x, y, z)
            int x = data[0];
            int y = data[1];
            int z = data[2];
            LOG.info("Accelerometer: X=" + x + ", Y=" + y + ", Z=" + z);
        } else if (EmpaticaE4Constants.ST_CHARACTERISTIC.equals(characteristicUUID)) {
            float temperature = buffer.getFloat();
            LOG.info("Temperature: " + temperature);
        } else {
            LOG.debug("Unhandled characteristic changed: " + characteristicUUID + " value: " + GB.hexdump(data));
        }

        return true;
    }*/

    @Override
    public boolean useAutoConnect() {
        return true;
    }
}