/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import android.location.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxWaypoint;

public class ZeppOsGpxRouteFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsGpxRouteFile.class);

    private static final double COORD_MULTIPLIER = 3000000.0;

    private final long timestamp;
    private final GpxFile gpxFile;
    private final String trackName;

    public ZeppOsGpxRouteFile(final GpxFile gpxFile, final String trackName) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.gpxFile = gpxFile;
        this.trackName = trackName;
    }

    public boolean isValid() {
        return this.gpxFile != null;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getEncodedBytes() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (gpxFile == null) {
            LOG.error("Failed to read gpx file - this should never happen");
            return null;
        }

        final List<GpxTrackPoint> trackPoints = gpxFile.getPoints();
        final List<GpxWaypoint> waypoints = gpxFile.getWaypoints();

        double minLatitude = 180;
        double maxLatitude = -180;
        double minLongitude = 180;
        double maxLongitude = -180;
        double minAltitude = 10000;
        double maxAltitude = -10000;

        for (final GPSCoordinate coord : trackPoints) {
            minLatitude = Math.min(minLatitude, coord.getLatitude());
            maxLatitude = Math.max(maxLatitude, coord.getLatitude());
            minLongitude = Math.min(minLongitude, coord.getLongitude());
            maxLongitude = Math.max(maxLongitude, coord.getLongitude());
            minAltitude = Math.min(minAltitude, coord.getAltitude());
            maxAltitude = Math.max(maxAltitude, coord.getAltitude());
        }

        try {
            baos.write(BLETypeConversions.fromUint32(0)); // ?
            baos.write(BLETypeConversions.fromUint32(0x54)); // ?
            baos.write(BLETypeConversions.fromUint32(0x01)); // ?
            baos.write(BLETypeConversions.fromUint32((int) timestamp));
            baos.write(BLETypeConversions.fromUint32((int) (minLatitude * COORD_MULTIPLIER)));
            baos.write(BLETypeConversions.fromUint32((int) (maxLatitude * COORD_MULTIPLIER)));
            baos.write(BLETypeConversions.fromUint32((int) (minLongitude * COORD_MULTIPLIER)));
            baos.write(BLETypeConversions.fromUint32((int) (maxLongitude * COORD_MULTIPLIER)));
            baos.write(BLETypeConversions.fromUint32((int) minAltitude));
            baos.write(BLETypeConversions.fromUint32((int) maxAltitude));
            baos.write(truncatePadString(trackName));
            baos.write(BLETypeConversions.fromUint32(0)); // ?

            if (!waypoints.isEmpty()) {
                baos.write(BLETypeConversions.fromUint32(2));
                baos.write(BLETypeConversions.fromUint32(waypoints.size() * 68));
                for (final GpxWaypoint waypoint : waypoints) {
                    baos.write(BLETypeConversions.fromUint32(0x1a)); // ?
                    baos.write(BLETypeConversions.fromUint32((int) (waypoint.getLatitude() * COORD_MULTIPLIER)));
                    baos.write(BLETypeConversions.fromUint32((int) (waypoint.getLongitude() * COORD_MULTIPLIER)));
                    baos.write(BLETypeConversions.fromUint32((int) waypoint.getAltitude()));
                    baos.write(truncatePadString(waypoint.getName()));
                    baos.write(BLETypeConversions.fromUint32(0)); // ?
                }
            }

            baos.write(BLETypeConversions.fromUint32(1)); // ?
            baos.write(BLETypeConversions.fromUint32(trackPoints.size() * 14));

            // Keep track of the total distance
            double totalDist = 0;
            GPSCoordinate prevPoint = trackPoints.isEmpty() ? null : trackPoints.get(0);

            for (final GPSCoordinate point : trackPoints) {
                totalDist += distanceBetween(prevPoint, point);

                baos.write(BLETypeConversions.fromUint32((int) totalDist));
                baos.write(BLETypeConversions.fromUint32((int) (point.getLatitude() * COORD_MULTIPLIER)));
                baos.write(BLETypeConversions.fromUint32((int) (point.getLongitude() * COORD_MULTIPLIER)));
                baos.write(BLETypeConversions.fromUint16((int) point.getAltitude()));

                prevPoint = point;
            }
        } catch (final IOException e) {
            LOG.error("Failed to encode gpx file", e);
            return null;
        }

        return baos.toByteArray();
    }

    public static double distanceBetween(final GPSCoordinate a, final GPSCoordinate b) {
        final Location start = new Location("start");
        start.setLatitude(a.getLatitude());
        start.setLongitude(a.getLongitude());

        final Location end = new Location("end");
        end.setLatitude(b.getLatitude());
        end.setLongitude(b.getLongitude());

        return end.distanceTo(start);
    }

    /**
     * Truncates / pads a string to 48 bytes (including null terminator).
     */
    public static byte[] truncatePadString(final String s) {
        final ByteBuffer buf = ByteBuffer.allocate(48);
        buf.put(StringUtils.truncateToBytes(s, 47));
        return buf.array();
    }
}
