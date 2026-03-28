package org.osmdroid.events;

public interface MapListener {
    boolean onScroll(ScrollEvent event);

    boolean onZoom(ZoomEvent event);
}
