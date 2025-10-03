package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor.support;

import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;

public class CyclingSensorBaseSupport extends AbstractBTLESingleDeviceSupport {
    public CyclingSensorBaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
