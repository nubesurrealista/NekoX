package org.osmdroid.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdate;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.TileSet;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.List;
public class MapView extends FrameLayout {
    private final org.maplibre.android.maps.MapView backingMapView;
    private final OverlayCanvasView overlayCanvasView;
    private final FrameLayout markerContainer;
    private final OverlayManager overlayManager = new OverlayManager(this);
    private final List<MapListener> mapListeners = new ArrayList<>();
    private final Projection projection = new Projection(this);
    private final CompatMapController controller = new CompatMapController();

    private MapLibreMap mapLibreMap;
    private ITileSource tileSource = TileSourceFactory.MAPNIK;
    private boolean started;
    private GeoPoint pendingCenter;
    private Double pendingZoom;
    private double maxZoomLevel = 20.0d;
    private double minZoomLevel = 0.0d;
    private double lastZoomLevel = Double.NaN;
    private GeoPoint lastCenter;
    private int compatPaddingLeft;
    private int compatPaddingTop;
    private int compatPaddingRight;
    private int compatPaddingBottom;

    public MapView(@NonNull Context context) {
        this(context, null);
    }

    public MapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        MapLibre.getInstance(context.getApplicationContext());

        backingMapView = new org.maplibre.android.maps.MapView(context);
        backingMapView.onCreate(null);
        addView(backingMapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        overlayCanvasView = new OverlayCanvasView(context);
        addView(overlayCanvasView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        markerContainer = new FrameLayout(context);
        markerContainer.setClipChildren(false);
        markerContainer.setClipToPadding(false);
        LayoutParams markerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        markerParams.gravity = Gravity.TOP | Gravity.LEFT;
        addView(markerContainer, markerParams);

        backingMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapLibreMap maplibreMap) {
                mapLibreMap = maplibreMap;
                mapLibreMap.addOnCameraMoveListener(() -> {
                    notifyCameraChanged();
                    dispatchScroll();
                });
                mapLibreMap.addOnCameraIdleListener(MapView.this::notifyCameraChanged);
                mapLibreMap.setMaxZoomPreference(maxZoomLevel);
                mapLibreMap.setMinZoomPreference(minZoomLevel);
                applyPendingCamera();
                applyTileSource();
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!started) {
            started = true;
            backingMapView.onStart();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (started) {
            backingMapView.onStop();
            started = false;
        }
        super.onDetachedFromWindow();
    }

    public void onResume() {
        backingMapView.onResume();
    }

    public void onPause() {
        backingMapView.onPause();
    }

    public FrameLayout getMarkerContainer() {
        return markerContainer;
    }

    public Projection getProjection() {
        return projection;
    }

    public IMapController getController() {
        return controller;
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }

    public OverlayManager getOverlays() {
        return overlayManager;
    }

    public void addMapListener(MapListener mapListener) {
        mapListeners.add(mapListener);
    }

    public void setTileSource(ITileSource tileSource) {
        this.tileSource = tileSource;
        applyTileSource();
    }

    public void setMaxZoomLevel(double maxZoomLevel) {
        this.maxZoomLevel = maxZoomLevel;
        if (mapLibreMap != null) {
            mapLibreMap.setMaxZoomPreference(maxZoomLevel);
        }
    }

    public double getMaxZoomLevel() {
        return maxZoomLevel;
    }

    public double getMinZoomLevel() {
        return minZoomLevel;
    }

    public double getZoomLevelDouble() {
        return mapLibreMap != null ? mapLibreMap.getCameraPosition().zoom : (pendingZoom != null ? pendingZoom : minZoomLevel);
    }

    public void setMultiTouchControls(boolean enabled) {
    }

    public void setBuiltInZoomControls(boolean enabled) {
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        compatPaddingLeft = left;
        compatPaddingTop = top;
        compatPaddingRight = right;
        compatPaddingBottom = bottom;
        invalidateOverlays();
    }

    @Override
    public int getPaddingLeft() {
        return compatPaddingLeft;
    }

    @Override
    public int getPaddingTop() {
        return compatPaddingTop;
    }

    @Override
    public int getPaddingRight() {
        return compatPaddingRight;
    }

    @Override
    public int getPaddingBottom() {
        return compatPaddingBottom;
    }

    public IGeoPoint getMapCenter() {
        if (mapLibreMap != null) {
            LatLng target = mapLibreMap.getCameraPosition().target;
            return new GeoPoint(target.getLatitude(), target.getLongitude());
        }
        if (pendingCenter != null) {
            return pendingCenter;
        }
        return new GeoPoint(0.0d, 0.0d);
    }

    public void zoomToBoundingBox(BoundingBox boundingBox, boolean animated, int padding) {
        zoomToBoundingBox(boundingBox, animated, padding, maxZoomLevel, 300L);
    }

    public void zoomToBoundingBox(BoundingBox boundingBox, boolean animated, int padding, double maxZoom, long durationMs) {
        if (mapLibreMap == null) {
            pendingCenter = boundingBox.getCenterWithDateLine();
            return;
        }
        LatLngBounds bounds = new LatLngBounds.Builder()
            .include(new LatLng(boundingBox.getLatNorth(), boundingBox.getLonEast()))
            .include(new LatLng(boundingBox.getLatSouth(), boundingBox.getLonWest()))
            .build();
        int[] paddingArray = new int[]{
            getPaddingLeft() + padding,
            getPaddingTop() + padding,
            getPaddingRight() + padding,
            getPaddingBottom() + padding
        };
        CameraPosition cameraPosition = mapLibreMap.getCameraForLatLngBounds(bounds, paddingArray);
        if (cameraPosition == null) {
            return;
        }
        double boundedZoom = Math.min(cameraPosition.zoom, maxZoom);
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(cameraPosition).zoom(boundedZoom).build());
        if (animated) {
            mapLibreMap.animateCamera(update, (int) durationMs);
        } else {
            mapLibreMap.moveCamera(update);
        }
    }

    public void onOverlayAdded(Overlay overlay) {
        overlay.onAdd(this);
        if (overlay instanceof Marker) {
            ((Marker) overlay).updateView();
        }
        invalidateOverlays();
    }

    public void onOverlayRemoved(Overlay overlay) {
        overlay.onRemove(this);
        invalidateOverlays();
    }

    public void invalidateOverlays() {
        for (Overlay overlay : overlayManager) {
            if (overlay instanceof Marker) {
                ((Marker) overlay).updateView();
            }
        }
        overlayCanvasView.invalidate();
    }

    PointF toScreenLocation(GeoPoint point) {
        if (mapLibreMap == null) {
            return new PointF();
        }
        return mapLibreMap.getProjection().toScreenLocation(new LatLng(point.getLatitude(), point.getLongitude()));
    }

    LatLng fromScreenLocation(PointF point) {
        if (mapLibreMap == null) {
            IGeoPoint center = getMapCenter();
            return new LatLng(center.getLatitude(), center.getLongitude());
        }
        return mapLibreMap.getProjection().fromScreenLocation(point);
    }

    public double metersPerPixel(double latitude) {
        double zoom = getZoomLevelDouble();
        return 156543.03392d * Math.cos(Math.toRadians(latitude)) / Math.pow(2.0d, zoom);
    }

    private void applyPendingCamera() {
        if (pendingCenter != null) {
            mapLibreMap.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(pendingCenter)));
        }
        if (pendingZoom != null) {
            mapLibreMap.moveCamera(CameraUpdateFactory.zoomTo(pendingZoom));
        }
        if (pendingCenter != null || pendingZoom != null) {
            notifyCameraChanged();
        }
    }

    private void applyTileSource() {
        if (mapLibreMap == null || tileSource == null) {
            return;
        }
        mapLibreMap.setStyle(new Style.Builder().fromJson("{\"version\":8,\"sources\":{},\"layers\":[]}"), style -> {
            TileSet tileSet = new TileSet("2.0.0", buildTileTemplates(tileSource));
            tileSet.setScheme("xyz");
            tileSet.setMinZoom(tileSource.getMinimumZoomLevel());
            tileSet.setMaxZoom(tileSource.getMaximumZoomLevel());
            tileSet.setAttribution(tileSource.getAttribution());
            style.addSource(new RasterSource("raster-source", tileSet, tileSource.getTileSizePixels()));
            style.addLayer(new RasterLayer("raster-layer", "raster-source"));
            applyPendingCamera();
            notifyCameraChanged();
        });
    }

    private String[] buildTileTemplates(ITileSource tileSource) {
        String[] baseUrls = tileSource.getBaseUrls();
        String[] tiles = new String[baseUrls.length];
        for (int i = 0; i < baseUrls.length; i++) {
            String baseUrl = baseUrls[i];
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            tiles[i] = baseUrl + "{z}/{x}/{y}" + tileSource.getImageFilenameEnding();
        }
        return tiles;
    }

    private void notifyCameraChanged() {
        if (mapLibreMap == null) {
            return;
        }
        CameraPosition cameraPosition = mapLibreMap.getCameraPosition();
        GeoPoint center = new GeoPoint(cameraPosition.target.getLatitude(), cameraPosition.target.getLongitude());
        double zoom = cameraPosition.zoom;
        boolean zoomChanged = Double.isNaN(lastZoomLevel) || Math.abs(lastZoomLevel - zoom) > 0.0001d;
        boolean scrollChanged = lastCenter == null
            || Math.abs(lastCenter.getLatitude() - center.getLatitude()) > 0.0000001d
            || Math.abs(lastCenter.getLongitude() - center.getLongitude()) > 0.0000001d;
        lastCenter = center;
        lastZoomLevel = zoom;
        invalidateOverlays();
        if (scrollChanged) {
            dispatchScroll();
        }
        if (zoomChanged) {
            ZoomEvent zoomEvent = new ZoomEvent(this, zoom);
            for (MapListener mapListener : mapListeners) {
                mapListener.onZoom(zoomEvent);
            }
        }
    }

    private void dispatchScroll() {
        ScrollEvent event = new ScrollEvent(this);
        for (MapListener mapListener : mapListeners) {
            mapListener.onScroll(event);
        }
    }

    private static LatLng toLatLng(IGeoPoint geoPoint) {
        return new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    private final class CompatMapController implements IMapController {
        @Override
        public void setCenter(IGeoPoint point) {
            pendingCenter = new GeoPoint(point.getLatitude(), point.getLongitude());
            if (mapLibreMap != null) {
                mapLibreMap.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
            }
        }

        @Override
        public void setZoom(double zoomLevel) {
            pendingZoom = zoomLevel;
            if (mapLibreMap != null) {
                mapLibreMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
            }
        }

        @Override
        public void animateTo(IGeoPoint point) {
            pendingCenter = new GeoPoint(point.getLatitude(), point.getLongitude());
            if (mapLibreMap != null) {
                mapLibreMap.animateCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
            }
        }

        @Override
        public void animateTo(IGeoPoint point, Double zoomLevel, Long durationMs) {
            pendingCenter = new GeoPoint(point.getLatitude(), point.getLongitude());
            if (zoomLevel != null) {
                pendingZoom = zoomLevel;
            }
            if (mapLibreMap == null) {
                return;
            }
            CameraPosition.Builder builder = new CameraPosition.Builder()
                .target(toLatLng(point))
                .zoom(zoomLevel != null ? zoomLevel : mapLibreMap.getCameraPosition().zoom);
            int duration = durationMs != null ? durationMs.intValue() : 300;
            mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), duration);
        }
    }

    private final class OverlayCanvasView extends android.view.View {
        OverlayCanvasView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (Overlay overlay : overlayManager) {
                if (!(overlay instanceof Marker)) {
                    overlay.draw(canvas, projection);
                }
            }
        }
    }
}
