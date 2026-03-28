package org.osmdroid.tileprovider.tilesource;

public interface ITileSource {
    String name();

    int getMinimumZoomLevel();

    int getMaximumZoomLevel();

    int getTileSizePixels();

    String[] getBaseUrls();

    String getImageFilenameEnding();

    String getAttribution();
}
