package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.q30;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public class SoundcoreQ30IOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreQ30IOThread.class);
    private final SoundcoreQ30Protocol mSoundcoreProtocol;

    public SoundcoreQ30IOThread(GBDevice gbDevice, Context context, SoundcoreQ30Protocol deviceProtocol, AbstractSerialDeviceSupport deviceSupport, BluetoothAdapter btAdapter) {
        super(gbDevice, context, deviceProtocol, deviceSupport, btAdapter);
        mSoundcoreProtocol = deviceProtocol;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read {} bytes. {}", bytes, hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }

    @Override
    protected void initialize() {
        write(mSoundcoreProtocol.encodeDeviceInfoRequest());
        super.initialize();
    }

    @Override
    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    }
}
