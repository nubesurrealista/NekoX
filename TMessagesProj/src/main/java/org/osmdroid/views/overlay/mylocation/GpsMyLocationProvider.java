package org.osmdroid.views.overlay.mylocation;

import android.content.Context;
import android.location.LocationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GpsMyLocationProvider implements IMyLocationProvider {
    private final Context context;
    private long locationUpdateMinTime;
    private float locationUpdateMinDistance;
    private final List<String> locationSources = new ArrayList<>();

    public GpsMyLocationProvider(Context context) {
        this.context = context;
        locationSources.add(LocationManager.GPS_PROVIDER);
    }

    public Context getContext() {
        return context;
    }

    public void setLocationUpdateMinDistance(float locationUpdateMinDistance) {
        this.locationUpdateMinDistance = locationUpdateMinDistance;
    }

    public void setLocationUpdateMinTime(long locationUpdateMinTime) {
        this.locationUpdateMinTime = locationUpdateMinTime;
    }

    public void addLocationSource(String locationSource) {
        if (!locationSources.contains(locationSource)) {
            locationSources.add(locationSource);
        }
    }

    @Override
    public long getLocationUpdateMinTime() {
        return locationUpdateMinTime;
    }

    @Override
    public float getLocationUpdateMinDistance() {
        return locationUpdateMinDistance;
    }

    @Override
    public List<String> getLocationSources() {
        return Collections.unmodifiableList(locationSources);
    }
}
