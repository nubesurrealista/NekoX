package org.osmdroid.views.overlay.mylocation;

import java.util.List;

public interface IMyLocationProvider {
    long getLocationUpdateMinTime();

    float getLocationUpdateMinDistance();

    List<String> getLocationSources();
}
