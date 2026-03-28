package org.osmdroid.util;

import java.util.List;

public class BoundingBox {
    private final double latNorth;
    private final double lonEast;
    private final double latSouth;
    private final double lonWest;

    public BoundingBox(double latNorth, double lonEast, double latSouth, double lonWest) {
        this.latNorth = latNorth;
        this.lonEast = lonEast;
        this.latSouth = latSouth;
        this.lonWest = lonWest;
    }

    public static BoundingBox fromGeoPoints(List<GeoPoint> geoPoints) {
        if (geoPoints == null || geoPoints.isEmpty()) {
            throw new IllegalArgumentException("geoPoints must not be empty");
        }
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (GeoPoint point : geoPoints) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }
        return new BoundingBox(maxLat, maxLon, minLat, minLon);
    }

    public GeoPoint getCenterWithDateLine() {
        return new GeoPoint((latNorth + latSouth) / 2.0d, (lonEast + lonWest) / 2.0d);
    }

    public double getLatNorth() {
        return latNorth;
    }

    public double getLonEast() {
        return lonEast;
    }

    public double getLatSouth() {
        return latSouth;
    }

    public double getLonWest() {
        return lonWest;
    }
}
