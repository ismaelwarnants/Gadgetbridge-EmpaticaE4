/*  Copyright (C) 2023-2024 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gb6900;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class CasioGB6900HandlerThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGB6900HandlerThread.class);
    private static final int TX_PERIOD = 60;
    private final Object waitObject = new Object();
    private boolean mQuit;
    private final CasioGB6900DeviceSupport mDeviceSupport;


    private Calendar mTxTime = GregorianCalendar.getInstance();

    public CasioGB6900HandlerThread(GBDevice gbDevice, Context context, CasioGB6900DeviceSupport deviceSupport) {
        super(gbDevice, context);
        LOG.info("Initializing Casio Handler Thread");
        mQuit = false;
        mDeviceSupport = deviceSupport;
    }

    @Override
    public void run() {
        LOG.debug("started thread {}", getName());

        mQuit = false;

        long waitTime = TX_PERIOD * 1000;
        while (!mQuit) {

            if (waitTime > 0) {
                synchronized (waitObject) {
                    try {
                        waitObject.wait(waitTime);
                    } catch (InterruptedException e) {
                        LOG.warn("exception in run", e);
                    }
                }
            }

            if (mQuit) {
                break;
            }

            GBDevice.State state = gbDevice.getState();
            if (state == GBDevice.State.NOT_CONNECTED || state == GBDevice.State.WAITING_FOR_RECONNECT) {
                LOG.debug("Closing handler thread, state not connected or waiting for reconnect.");
                quit();
                continue;
            }

            Calendar now = GregorianCalendar.getInstance();

            if (now.compareTo(mTxTime) > 0) {
                requestTxPowerLevel();
            }

            now = GregorianCalendar.getInstance();
            waitTime = mTxTime.getTimeInMillis() - now.getTimeInMillis();
        }
        LOG.debug("finished thread {}", getName());
    }

    public void requestTxPowerLevel() {
        try {
            mDeviceSupport.readTxPowerLevel();
        } catch (Exception ignored) {
        }

        mTxTime = GregorianCalendar.getInstance();
        mTxTime.add(Calendar.SECOND, TX_PERIOD);
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    @Override
    public void quit() {
        LOG.info("CasioGB6900HandlerThread: Quit Handler Thread");
        mQuit = true;
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

}
