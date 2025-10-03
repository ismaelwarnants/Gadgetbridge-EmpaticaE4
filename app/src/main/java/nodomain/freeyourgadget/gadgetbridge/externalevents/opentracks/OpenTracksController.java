/*  Copyright (C) 2022-2024 Arjan Schrijver, José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OpenTracksController extends Activity {
    /*
     * A short explanation of how this integration with OpenTracks works:
     * Starting the recording from a device requires calling `startRecording()`
     * on this class. For a simple example, check out the implementation in
     * `WorkoutRequestHandler`, used by the Fossil HR series.
     * The OpenTracks class can be set in the Gadgetbridge settings and depends
     * on the installation source used for OpenTracks. Details can be found in
     * their documentation here: https://github.com/OpenTracksApp/OpenTracks#api
     * `startRecording()` sends an explicit Intent to OpenTracks signalling it
     * to start recording. It passes along the package name and class name of
     * our `OpenTracksController` which OpenTracks will use to send the 
     * statistics URIs to. After starting the recording service, OpenTracks
     * uses a new explicit Intent to start our `OpenTracksController` and passes
     * along the URIs and the read permissions for those URIs (using
     * `Intent.FLAG_GRANT_READ_URI_PERMISSION`). So at that point
     * `OpenTracksController` is started as a new `Activity` (or `Context`)
     * which has the read permissions for the statistics URIs. The controller
     * saves its `Context` into the `OpenTracksContentObserver` in the GB main
     * process, so it can keep running and read the statistics with the correct
     * `Context`. So, whatever class, device or activity calls the methods on
     * the `OpenTracksContentObserver` from, it will always work.
     */
    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";
    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    private static final Logger LOG = LoggerFactory.getLogger(OpenTracksController.class);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        GBApplication gbApp = GBApplication.app();
        Intent intent = getIntent();
        int protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1);
        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD);
        if (uris != null) {
            if (gbApp.getOpenTracksObserver() != null) {
                LOG.info("Unregistering old OpenTracksContentObserver");
                gbApp.getOpenTracksObserver().unregister();
            }
            Uri tracksUri = uris.get(0);
            Uri trackpointsUri = uris.get(1);
            LOG.info("Registering OpenTracksContentObserver with tracks URI: " + tracksUri);
            LOG.info("Registering OpenTracksContentObserver with trackpoints URI: " + trackpointsUri);
            gbApp.setOpenTracksObserver(new OpenTracksContentObserver(this, tracksUri, trackpointsUri, protocolVersion));
            try {
                getContentResolver().registerContentObserver(tracksUri, false, gbApp.getOpenTracksObserver());
                getContentResolver().registerContentObserver(trackpointsUri, false, gbApp.getOpenTracksObserver());
            } catch (final SecurityException se) {
                LOG.error("Error registering OpenTracksContentObserver", se);
            }
        }
        moveTaskToBack(true);
    }

    public static void sendIntent(Context context, String className, String category, String icon) {
        Prefs prefs = GBApplication.getPrefs();
        String packageName = prefs.getString("opentracks_packagename", "de.dennisguse.opentracks");
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(packageName, className);
        intent.putExtra("STATS_TARGET_PACKAGE", context.getPackageName());
        intent.putExtra("STATS_TARGET_CLASS", OpenTracksController.class.getName());
        if (category != null) {
            intent.putExtra("TRACK_CATEGORY", category);
        }
        if (icon != null) {
            intent.putExtra("TRACK_ICON", icon);
        }
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            GB.toast(e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.WARN, e);
        }
    }

    public static void startRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StartRecording", null, null);
    }

    public static void startRecording(Context context, ActivityKind activityKind) {
        final String category = activityKind.getLabel(context);
        final String icon;
        switch (activityKind) {
            case CYCLING:
                icon = "BIKE";
                break;
            case HIKING:
            case WALKING:
                icon = "WALK";
                break;
            case RUNNING:
                icon = "RUN";
                break;
            default:
                LOG.warn("Unmapped activity kind icon for {}", activityKind);
                icon = null;
        }

        sendIntent(context, "de.dennisguse.opentracks.publicapi.StartRecording", category, icon);
    }

    public static void stopRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StopRecording", null, null);
        OpenTracksContentObserver openTracksObserver = GBApplication.app().getOpenTracksObserver();
        if (openTracksObserver != null) {
            saveToGpx(openTracksObserver.getActivityTrack());
            openTracksObserver.finish();
        }
        GBApplication.app().setOpenTracksObserver(null);
    }

    public static void toggleRecording(Context context) {
        OpenTracksContentObserver openTracksObserver = GBApplication.app().getOpenTracksObserver();
        if (openTracksObserver == null) {
            startRecording(context);
        } else {
            stopRecording(context);
        }
    }

    private static void saveToGpx(ActivityTrack activityTrack) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        final String gpxName = sdf.format(new Date());

        activityTrack.setName(gpxName);

        try {
            final File gpxDir = new File(FileUtils.getExternalFilesDir(), "gpx");
            //noinspection ResultOfMethodCallIgnored
            gpxDir.mkdirs();
            final File gpxFile = new File(gpxDir, gpxName + ".gpx");
            final GPXExporter gpxExporter = new GPXExporter();
            gpxExporter.performExport(activityTrack, gpxFile);
            LOG.info("Saved GPX received from OpenTracks to {}", gpxFile.getPath());
        } catch (IOException | ActivityTrackExporter.GPXTrackEmptyException e) {
            LOG.error("Error while writing generated GPX file", e);
        }
    }
}
