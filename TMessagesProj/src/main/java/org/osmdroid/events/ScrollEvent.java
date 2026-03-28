package org.osmdroid.events;

import org.osmdroid.views.MapView;

public class ScrollEvent {
    private final MapView source;

    public ScrollEvent(MapView source) {
        this.source = source;
    }

    public MapView getSource() {
        return source;
    }
}
