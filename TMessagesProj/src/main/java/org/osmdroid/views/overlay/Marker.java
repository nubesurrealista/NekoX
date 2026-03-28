package org.osmdroid.views.overlay;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class Marker extends Overlay {
    public static final float ANCHOR_CENTER = 0.5f;
    public static final float ANCHOR_BOTTOM = 1.0f;

    private final MapView ownerMap;
    private final ImageView markerView;
    private GeoPoint position;
    private Drawable icon;
    private float anchorU = ANCHOR_CENTER;
    private float anchorV = ANCHOR_BOTTOM;
    private float rotation;
    private boolean flat;
    private String title;
    private String snippet;
    private OnMarkerClickListener onMarkerClickListener;

    public interface OnMarkerClickListener {
        boolean onMarkerClick(Marker marker, MapView mapView);
    }

    public Marker(MapView mapView) {
        this.ownerMap = mapView;
        this.markerView = new ImageView(mapView.getContext());
        markerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        markerView.setOnClickListener(v -> {
            if (onMarkerClickListener != null) {
                onMarkerClickListener.onMarkerClick(this, ownerMap);
            }
        });
    }

    @Override
    public void onAdd(MapView mapView) {
        super.onAdd(mapView);
        ownerMap.getMarkerContainer().addView(markerView);
        updateView();
    }

    @Override
    public void onRemove(MapView mapView) {
        ownerMap.getMarkerContainer().removeView(markerView);
        super.onRemove(mapView);
    }

    public void setPosition(GeoPoint position) {
        this.position = position;
        updateView();
    }

    public GeoPoint getPosition() {
        return position;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
        markerView.setImageDrawable(icon);
        updateView();
    }

    public void setAnchor(float anchorU, float anchorV) {
        this.anchorU = anchorU;
        this.anchorV = anchorV;
        updateView();
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        markerView.setRotation(rotation);
    }

    public void setFlat(boolean flat) {
        this.flat = flat;
    }

    public boolean isFlat() {
        return flat;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setOnMarkerClickListener(OnMarkerClickListener onMarkerClickListener) {
        this.onMarkerClickListener = onMarkerClickListener;
    }

    public void remove(MapView mapView) {
        mapView.getOverlays().remove(this);
    }

    public void updateView() {
        if (position == null || icon == null || mapView == null) {
            return;
        }
        int width = Math.max(icon.getIntrinsicWidth(), 1);
        int height = Math.max(icon.getIntrinsicHeight(), 1);
        markerView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        );
        markerView.layout(0, 0, width, height);
        Point point = ownerMap.getProjection().toPixels(position, null);
        markerView.setRotation(rotation);
        markerView.setTranslationX(point.x - width * anchorU);
        markerView.setTranslationY(point.y - height * anchorV);
    }
}
