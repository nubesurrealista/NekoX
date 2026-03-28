package org.osmdroid.views.overlay;

import java.util.ArrayList;

import org.osmdroid.views.MapView;

public class OverlayManager extends ArrayList<Overlay> {
    private final MapView mapView;

    public OverlayManager(MapView mapView) {
        this.mapView = mapView;
    }

    @Override
    public boolean add(Overlay overlay) {
        boolean added = super.add(overlay);
        if (added) {
            mapView.onOverlayAdded(overlay);
        }
        return added;
    }

    @Override
    public boolean remove(Object object) {
        boolean removed = super.remove(object);
        if (removed && object instanceof Overlay) {
            mapView.onOverlayRemoved((Overlay) object);
        }
        return removed;
    }
}
