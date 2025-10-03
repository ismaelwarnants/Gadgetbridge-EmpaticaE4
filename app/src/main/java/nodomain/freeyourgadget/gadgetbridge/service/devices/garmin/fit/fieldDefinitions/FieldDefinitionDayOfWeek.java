package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionDayOfWeek extends FieldDefinition {

    public FieldDefinitionDayOfWeek(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return DayOfWeek.of(raw == 0 ? 7 : raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof DayOfWeek) {
            baseType.encode(byteBuffer, (((DayOfWeek) o).getValue() % 7), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, (Instant.ofEpochSecond((int) o).atZone(ZoneId.systemDefault()).getDayOfWeek().getValue() % 7), scale, offset);
    }
}
