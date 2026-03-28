package org.osmdroid.views.overlay;

import android.graphics.Canvas;

import androidx.annotation.Nullable;

import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

public abstract class Overlay {
    @Nullable
    protected MapView mapView;

    public void onAdd(MapView mapView) {
        this.mapView = mapView;
    }

    public void onRemove(MapView mapView) {
        this.mapView = null;
    }

    public void draw(Canvas canvas, Projection projection) {
    }

    public void invalidate() {
        if (mapView != null) {
            mapView.invalidateOverlays();
        }
    }
}
