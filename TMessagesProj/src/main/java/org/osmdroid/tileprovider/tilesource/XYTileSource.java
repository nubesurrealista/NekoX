package org.osmdroid.tileprovider.tilesource;

public class XYTileSource implements ITileSource {
    private final String name;
    private final int minimumZoomLevel;
    private final int maximumZoomLevel;
    private final int tileSizePixels;
    private final String imageFilenameEnding;
    private final String[] baseUrls;
    private final String attribution;

    public XYTileSource(String name, int minimumZoomLevel, int maximumZoomLevel, int tileSizePixels,
                        String imageFilenameEnding, String[] baseUrls, String attribution) {
        this.name = name;
        this.minimumZoomLevel = minimumZoomLevel;
        this.maximumZoomLevel = maximumZoomLevel;
        this.tileSizePixels = tileSizePixels;
        this.imageFilenameEnding = imageFilenameEnding;
        this.baseUrls = baseUrls;
        this.attribution = attribution;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int getMinimumZoomLevel() {
        return minimumZoomLevel;
    }

    @Override
    public int getMaximumZoomLevel() {
        return maximumZoomLevel;
    }

    @Override
    public int getTileSizePixels() {
        return tileSizePixels;
    }

    @Override
    public String[] getBaseUrls() {
        return baseUrls;
    }

    @Override
    public String getImageFilenameEnding() {
        return imageFilenameEnding;
    }

    @Override
    public String getAttribution() {
        return attribution;
    }
}
