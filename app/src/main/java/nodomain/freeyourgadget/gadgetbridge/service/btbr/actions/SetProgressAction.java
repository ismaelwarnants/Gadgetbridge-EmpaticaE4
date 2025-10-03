/*  Copyright (C) 2015-2025 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Yoran Vulker, Thomas Kuehne

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btbr.actions;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.PlainAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SetProgressAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(SetProgressAction.class);

    private final String text;
    private final boolean ongoing;
    private final int percentage;
    private final Context context;

    /**
     * When run, will update the progress notification.
     *
     * @param textRes Text shown in the notification
     * @param ongoing State of action, true when the action is still being performed
     * @param percentage Current percentage indicating how far along the action has progressed
     * @param context Context in which to create the notification
     */
    public SetProgressAction(@StringRes int textRes, boolean ongoing, int percentage, Context context) {
        this.text = context.getString(textRes);
        this.ongoing = ongoing;
        this.percentage = percentage;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothSocket unused) {
        LOG.info(toString());
        GB.updateInstallNotification(this.text, this.ongoing, this.percentage, this.context);

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_PROGRESS, percentage));

        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return getCreationTime() + ": " + getClass().getSimpleName() + ": " + text + "; " + percentage + "%";
    }
}
