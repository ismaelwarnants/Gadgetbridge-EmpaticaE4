/*  Copyright (C) 2017-2024 Andreas Shimokawa, Daniel Dakhno, Daniele Gobbetti,
    João Paulo Barraca, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class BluetoothPairingRequestReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothPairingRequestReceiver.class);

    final DeviceCommunicationService service;

    public BluetoothPairingRequestReceiver(DeviceCommunicationService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (!BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            return;
        }
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            return;
        }

        GBDevice gbDevice = null;
        try {
            gbDevice = service.getDeviceByAddress(device.getAddress());

            DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            try {
                if (coordinator.getBondingStyle() == DeviceCoordinator.BONDING_STYLE_NONE) {
                    LOG.info("Aborting unwanted pairing request");
                    abortBroadcast();
                }
            } catch (Exception e) {
                LOG.warn("Could not abort pairing request process");
            }
        } catch (DeviceCommunicationService.DeviceNotFoundException e) {
            LOG.warn("exception in onReceive", e);
        }
    }
}