package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.CapabilitiesDeviceEvent;


public class ConfigurationMessage extends GFDIMessage {
    private final byte[] incomingConfigurationPayload;
    private final Set<GarminCapability> capabilities;
    private final byte[] ourConfigurationPayload = GarminCapability.setToBinary(GarminCapability.OUR_CAPABILITIES);

    public ConfigurationMessage(GarminMessage garminMessage, byte[] configurationPayload) {
        this.garminMessage = garminMessage;
        if (configurationPayload.length > 255)
            throw new IllegalArgumentException("Too long payload");
        this.incomingConfigurationPayload = configurationPayload;
        this.capabilities = GarminCapability.setFromBinary(configurationPayload);
        LOG.info("Received configuration message; capabilities: {}", GarminCapability.setToString(capabilities));

        this.statusMessage = this.getStatusMessage();
    }

    public static ConfigurationMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int numBytes = reader.readByte();
        return new ConfigurationMessage(garminMessage, reader.readBytes(numBytes));
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        final Set<Object> capabilitiesPref = new HashSet<>();
        for (final GarminCapability capability : capabilities) {
            capabilitiesPref.add(capability.name());
        }
        return Arrays.asList(
                new CapabilitiesDeviceEvent(capabilities),
                new GBDeviceEventUpdatePreferences(GarminPreferences.PREF_GARMIN_CAPABILITIES, capabilitiesPref)
        );
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // placeholder for packet size
        writer.writeShort(garminMessage.getId());
        writer.writeByte(ourConfigurationPayload.length);
        writer.writeBytes(ourConfigurationPayload);
        return true;
    }

}
