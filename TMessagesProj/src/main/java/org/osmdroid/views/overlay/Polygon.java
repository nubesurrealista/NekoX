package org.osmdroid.views.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Polygon extends Overlay {
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<GeoPoint> points = Collections.emptyList();

    public Polygon() {
        outlinePaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public Paint getOutlinePaint() {
        return outlinePaint;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setPoints(List<GeoPoint> points) {
        this.points = points != null ? new ArrayList<>(points) : Collections.emptyList();
        invalidate();
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (points.size() < 2) {
            return;
        }
        Path path = new Path();
        Point first = projection.toPixels(points.get(0), null);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            Point point = projection.toPixels(points.get(i), null);
            path.lineTo(point.x, point.y);
        }
        path.close();
        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, outlinePaint);
    }

    public static List<GeoPoint> pointsAsCircle(GeoPoint center, double meters) {
        List<GeoPoint> points = new ArrayList<>(64);
        double radiusDegreesLat = meters / 111320.0d;
        double radiusDegreesLon = meters / (111320.0d * Math.cos(Math.toRadians(center.getLatitude())));
        if (Double.isNaN(radiusDegreesLon) || Double.isInfinite(radiusDegreesLon)) {
            radiusDegreesLon = 0.0d;
        }
        for (int i = 0; i < 64; i++) {
            double angle = 2.0d * Math.PI * i / 64.0d;
            double lat = center.getLatitude() + Math.sin(angle) * radiusDegreesLat;
            double lon = center.getLongitude() + Math.cos(angle) * radiusDegreesLon;
            points.add(new GeoPoint(lat, lon));
        }
        return points;
    }
}
