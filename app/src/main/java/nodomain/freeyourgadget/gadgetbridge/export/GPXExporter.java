/*  Copyright (C) 2017-2024 Andreas Shimokawa, AndrewH, Carsten Pfeiffer,
    Daniele Gobbetti, Dikay900, José Rebelo, Nick Spacek, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.export;

import android.util.Xml;

import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class GPXExporter implements ActivityTrackExporter {
    private static final String NS_GPX_URI = "http://www.topografix.com/GPX/1/1";
    private static final String NS_GPX_PREFIX = "";
    private static final String NS_TRACKPOINT_EXTENSION = "gpxtpx";
    private static final String NS_TRACKPOINT_EXTENSION_URI = "https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd";
    private static final String NS_XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String TOPOGRAFIX_NAMESPACE_XSD = "http://www.topografix.com/GPX/1/1/gpx.xsd";
    private static final String OPENTRACKS_PREFIX = "opentracks";
    private static final String OPENTRACKS_NAMESPACE_URI = "http://opentracksapp.com/xmlschemas/v1";

    private String creator;
    private boolean includeHeartRate = true;
    private boolean includeHeartRateOfNearestSample = true;

    @Override
    public void performExport(ActivityTrack track, File targetFile) throws IOException, GPXTrackEmptyException {
        String encoding = StandardCharsets.UTF_8.name();
        XmlSerializer ser = Xml.newSerializer();
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            ser.setOutput(outputStream, encoding);
            ser.startDocument(encoding, Boolean.TRUE);
            ser.setPrefix("xsi", NS_XSI_URI);
            ser.setPrefix(NS_TRACKPOINT_EXTENSION, NS_TRACKPOINT_EXTENSION_URI);
            ser.setPrefix(NS_GPX_PREFIX, NS_GPX_URI);
            ser.setPrefix(OPENTRACKS_PREFIX, OPENTRACKS_NAMESPACE_URI);

            ser.startTag(NS_GPX_URI, "gpx");
            ser.attribute(null, "version", "1.1");
            if (creator != null) {
                ser.attribute(null, "creator", creator);
            } else {
                ser.attribute(null, "creator", GBApplication.app().getNameAndVersion());
            }
            ser.attribute(NS_XSI_URI, "schemaLocation",NS_GPX_URI + " " + TOPOGRAFIX_NAMESPACE_XSD);

            exportMetadata(ser, track);
            exportTrack(ser, track);

            ser.endTag(NS_GPX_URI, "gpx");
            ser.endDocument();
            ser.flush();
        }
    }

    private void exportMetadata(XmlSerializer ser, ActivityTrack track) throws IOException {
        ser.startTag(NS_GPX_URI, "metadata");
        if (track.getName() != null) {
            ser.startTag(NS_GPX_URI, "name").text(track.getName()).endTag(NS_GPX_URI, "name");
        }

        final User user = track.getUser();
        if (user != null) {
            ser.startTag(NS_GPX_URI, "author");
            ser.startTag(NS_GPX_URI, "name").text(user.getName()).endTag(NS_GPX_URI, "name");
            ser.endTag(NS_GPX_URI, "author");
        }

        ser.startTag(NS_GPX_URI, "time").text(formatTime(new Date())).endTag(NS_GPX_URI, "time");

        ser.endTag(NS_GPX_URI, "metadata");
    }

    private String formatTime(Date date) {
        return DateTimeUtils.formatIso8601(date);
    }

    private void exportTrack(XmlSerializer ser, ActivityTrack track) throws IOException, GPXTrackEmptyException {
        String uuid = UUID.randomUUID().toString();
        ser.startTag(NS_GPX_URI, "trk");
        ser.startTag(NS_GPX_URI, "extensions");
        ser.startTag(NS_GPX_URI, OPENTRACKS_PREFIX + ":trackid").text(uuid).endTag(NS_GPX_URI, OPENTRACKS_PREFIX + ":trackid");
        ser.endTag(NS_GPX_URI, "extensions");

        List<List<ActivityPoint>> segments = track.getSegments();
        boolean atLeastOnePointExported = false;
        for (List<ActivityPoint> segment : segments) {
            if (segment.isEmpty()) {
                // Skip empty segments
                continue;
            }

            ser.startTag(NS_GPX_URI, "trkseg");
            for (ActivityPoint point : segment) {
                atLeastOnePointExported |= exportTrackPoint(ser, point, segment);
            }
            ser.endTag(NS_GPX_URI, "trkseg");
        }

        if (!atLeastOnePointExported) {
            throw new GPXTrackEmptyException();
        }

        ser.endTag(NS_GPX_URI, "trk");
    }

    private boolean exportTrackPoint(XmlSerializer ser, ActivityPoint point, Iterable<ActivityPoint> trackPoints) throws IOException {
        GPSCoordinate location = point.getLocation();
        if (location == null) {
            return false; // skip invalid points, that just contain hr data, for example
        }
        ser.startTag(NS_GPX_URI, "trkpt");
        // lon and lat attributes do not have an explicit namespace
        ser.attribute(null, "lon", formatDouble(location.getLongitude()));
        ser.attribute(null, "lat", formatDouble(location.getLatitude()));
        if (location.getAltitude() != GPSCoordinate.UNKNOWN_ALTITUDE) {
            ser.startTag(NS_GPX_URI, "ele").text(formatDouble(location.getAltitude())).endTag(NS_GPX_URI, "ele");
        }
        ser.startTag(NS_GPX_URI, "time").text(DateTimeUtils.formatIso8601UTC(point.getTime())).endTag(NS_GPX_URI, "time");
        String description = point.getDescription();
        if (description != null) {
            ser.startTag(NS_GPX_URI, "desc").text(description).endTag(NS_GPX_URI, "desc");
        }
        //ser.startTag(NS_GPX_URI, "src").text(source).endTag(NS_GPX_URI, "src");
        if (location.hasHdop()) {
            ser.startTag(NS_GPX_URI, "hdop").text(formatDouble(location.getHdop())).endTag(NS_GPX_URI, "hdop");
        }
        if (location.hasVdop()) {
            ser.startTag(NS_GPX_URI, "vdop").text(formatDouble(location.getVdop())).endTag(NS_GPX_URI, "vdop");
        }
        if (location.hasPdop()) {
            ser.startTag(NS_GPX_URI, "pdop").text(formatDouble(location.getPdop())).endTag(NS_GPX_URI, "pdop");
        }

        exportTrackpointExtensions(ser, point, trackPoints);

        ser.endTag(NS_GPX_URI, "trkpt");

        return true;
    }

    private void exportTrackpointExtensions(XmlSerializer ser, ActivityPoint point, Iterable<ActivityPoint> trackPoints) throws IOException {
        if (!includeHeartRate) {
            return;
        }

        float speed = point.getSpeed();
        int cadence = point.getCadence();
        int hr = point.getHeartRate();
        if (!HeartRateUtils.getInstance().isValidHeartRateValue(hr) && includeHeartRateOfNearestSample) {

            ActivityPoint closestPointItem = findClosestSensibleActivityPoint(point.getTime(), trackPoints);
            if (closestPointItem != null) {
                hr = closestPointItem.getHeartRate();
            }

        }

        boolean exportHr = HeartRateUtils.getInstance().isValidHeartRateValue(hr) && includeHeartRate;

        if (!exportHr && speed < 0 && cadence < 0) {
            // No valid data to export in extensions
            return;
        }

        ser.startTag(NS_GPX_URI, "extensions");
        ser.setPrefix(NS_TRACKPOINT_EXTENSION, NS_TRACKPOINT_EXTENSION_URI);
        ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "TrackPointExtension");
        if (exportHr) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "hr").text(String.valueOf(hr)).endTag(NS_TRACKPOINT_EXTENSION_URI, "hr");
        }
        if (cadence >= 0) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "cad").text(String.valueOf(cadence)).endTag(NS_TRACKPOINT_EXTENSION_URI, "cad");
        }
        if (speed >= 0) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "speed").text(formatDouble(speed)).endTag(NS_TRACKPOINT_EXTENSION_URI, "speed");
        }
        ser.endTag(NS_TRACKPOINT_EXTENSION_URI, "TrackPointExtension");
        ser.endTag(NS_GPX_URI, "extensions");
    }

    private @Nullable ActivityPoint findClosestSensibleActivityPoint(Date time, Iterable<ActivityPoint> trackPoints) {
        ActivityPoint closestPointItem = null;
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        long lowestDifference = 60 * 2 * 1000; // minimum distance is 2min
        for (ActivityPoint pointItem : trackPoints) {
            int hrItem = pointItem.getHeartRate();
            if (heartRateUtilsInstance.isValidHeartRateValue(hrItem)) {
                Date timeItem = pointItem.getTime();
                if (timeItem.after(time) || timeItem.equals(time)) {
                    break; // we assume that the given trackPoints are sorted in time ascending order (oldest first)
                }
                long difference = time.getTime() - timeItem.getTime();
                if (difference < lowestDifference) {
                    lowestDifference = difference;
                    closestPointItem = pointItem;
                }
            }
        }
        return closestPointItem;
    }

    private String formatDouble(double value) {
        return new BigDecimal(value).setScale(GPSCoordinate.GPS_DECIMAL_DEGREES_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    public String getCreator() {
        return creator; // TODO: move to some kind of BrandingInfo class
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setIncludeHeartRate(boolean includeHeartRate) {
        this.includeHeartRate = includeHeartRate;
    }

    public boolean isIncludeHeartRate() {
        return includeHeartRate;
    }
}
