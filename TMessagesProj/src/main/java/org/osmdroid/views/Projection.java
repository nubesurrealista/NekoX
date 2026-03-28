package org.osmdroid.views;

import android.graphics.Point;
import android.graphics.PointF;

import org.maplibre.android.geometry.LatLng;
import org.osmdroid.util.GeoPoint;

public class Projection {
    private final MapView mapView;

    Projection(MapView mapView) {
        this.mapView = mapView;
    }

    public Point toPixels(GeoPoint point, Point reuse) {
        PointF screenPoint = mapView.toScreenLocation(point);
        Point out = reuse != null ? reuse : new Point();
        out.x = Math.round(screenPoint.x);
        out.y = Math.round(screenPoint.y);
        return out;
    }

    public GeoPoint fromPixels(int x, int y) {
        LatLng latLng = mapView.fromScreenLocation(new PointF(x, y));
        return new GeoPoint(latLng.getLatitude(), latLng.getLongitude());
    }
}
