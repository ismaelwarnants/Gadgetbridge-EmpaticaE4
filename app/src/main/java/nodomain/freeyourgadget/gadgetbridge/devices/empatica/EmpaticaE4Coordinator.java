package nodomain.freeyourgadget.gadgetbridge.devices.empatica;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.empatica.EmpaticaE4DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.unknown.UnknownDeviceSupport;

public class EmpaticaE4Coordinator extends AbstractBLEDeviceCoordinator {

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_empatica_e4;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    public String getManufacturer() {
        return "Empatica";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(GBDevice device) {
        return EmpaticaE4DeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        /* return Pattern.compile("Amazfit T-Rex", Pattern.CASE_INSENSITIVE); */
        /* return Pattern.compile("Xiaomi Smart Band 7.*");  */
    /* return Pattern.compile("Bangle\\.js.*|Pixl\\.js.*|Puck\\.js.*|MDBT42Q.*|Espruino.*"); /*
    /* return Pattern.compile("M6.*|M4.*|LH716|Sunset 6|Watch7|Fit1900"); */
        return Pattern.compile("Empatica E4");
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }

}

