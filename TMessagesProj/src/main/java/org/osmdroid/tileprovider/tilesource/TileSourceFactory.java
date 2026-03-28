package org.osmdroid.tileprovider.tilesource;

public final class TileSourceFactory {
    public static final ITileSource MAPNIK = new XYTileSource(
        "Mapnik",
        0,
        19,
        256,
        ".png",
        new String[]{"https://tile.openstreetmap.org/"},
        "© OpenStreetMap contributors"
    );

    private TileSourceFactory() {
    }
}
