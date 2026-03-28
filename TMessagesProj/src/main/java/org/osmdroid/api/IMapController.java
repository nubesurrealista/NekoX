package org.osmdroid.api;

public interface IMapController {
    void setCenter(IGeoPoint point);

    void setZoom(double zoomLevel);

    void animateTo(IGeoPoint point);

    void animateTo(IGeoPoint point, Double zoomLevel, Long durationMs);
}
