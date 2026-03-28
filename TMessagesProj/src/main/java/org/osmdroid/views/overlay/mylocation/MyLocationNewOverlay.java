package org.osmdroid.views.overlay.mylocation;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class MyLocationNewOverlay extends Overlay implements LocationListener {
    private final GpsMyLocationProvider locationProvider;
    private final List<Runnable> firstFixRunnables = new ArrayList<>();
    private final Paint accuracyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LocationManager locationManager;
    private boolean enabled;
    private boolean drawAccuracyEnabled;
    private Location lastFix;

    public MyLocationNewOverlay(GpsMyLocationProvider locationProvider, MapView mapView) {
        this.locationProvider = locationProvider;
        this.mapView = mapView;
        accuracyPaint.setStyle(Paint.Style.FILL);
        accuracyPaint.setColor(0x334286F5);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(Color.rgb(66, 134, 245));
    }

    @Override
    public void onAdd(MapView mapView) {
        super.onAdd(mapView);
        if (enabled) {
            startLocationUpdates();
        }
    }

    @Override
    public void onRemove(MapView mapView) {
        stopLocationUpdates();
        super.onRemove(mapView);
    }

    public void enableMyLocation() {
        enabled = true;
        startLocationUpdates();
    }

    public void disableMyLocation() {
        enabled = false;
        stopLocationUpdates();
    }

    public void setDrawAccuracyEnabled(boolean drawAccuracyEnabled) {
        this.drawAccuracyEnabled = drawAccuracyEnabled;
        invalidate();
    }

    public Location getLastFix() {
        return lastFix;
    }

    public void runOnFirstFix(Runnable runnable) {
        if (lastFix != null) {
            runnable.run();
        } else {
            firstFixRunnables.add(runnable);
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!enabled || mapView == null) {
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) locationProvider.getContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        }
        if (locationManager == null) {
            return;
        }
        stopLocationUpdates();
        for (String source : locationProvider.getLocationSources()) {
            try {
                locationManager.requestLocationUpdates(source, locationProvider.getLocationUpdateMinTime(), locationProvider.getLocationUpdateMinDistance(), this);
            } catch (Throwable ignore) {
            }
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastFix = location == null ? null : new Location(location);
        if (lastFix != null && !firstFixRunnables.isEmpty()) {
            List<Runnable> pending = new ArrayList<>(firstFixRunnables);
            firstFixRunnables.clear();
            for (Runnable runnable : pending) {
                runnable.run();
            }
        }
        onLocationChanged(location, locationProvider);
        invalidate();
    }

    public void onLocationChanged(Location location, IMyLocationProvider source) {
    }

    @Override
    public void draw(Canvas canvas, Projection projection) {
        if (lastFix == null) {
            return;
        }
        GeoPoint point = new GeoPoint(lastFix.getLatitude(), lastFix.getLongitude());
        android.graphics.Point screenPoint = projection.toPixels(point, null);
        if (drawAccuracyEnabled && lastFix.hasAccuracy()) {
            double metersPerPixel = mapView.metersPerPixel(lastFix.getLatitude());
            float radius = metersPerPixel > 0.0d ? (float) (lastFix.getAccuracy() / metersPerPixel) : 0.0f;
            canvas.drawCircle(screenPoint.x, screenPoint.y, radius, accuracyPaint);
        }
        canvas.drawCircle(screenPoint.x, screenPoint.y, 10.0f, pointPaint);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
