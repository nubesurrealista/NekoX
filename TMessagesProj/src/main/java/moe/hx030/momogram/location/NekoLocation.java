package moe.hx030.momogram.location;


import android.location.Location;
import android.util.Pair;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

public class NekoLocation {
    public final static Set<Long> recent = Collections.synchronizedSet(Collections.newSetFromMap(new Cache<>()));

    private static long coordHash(double latitude, double longitude) {
        return Double.doubleToLongBits(latitude) ^ Long.rotateRight(Double.doubleToLongBits(longitude), 1);
    }

    public static void transform(Location location) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();

        if (recent.contains(coordHash(latitude, longitude))) return;

        final Pair<Double, Double> trans = GeodeticTransform.transform(latitude, longitude);
        location.setLatitude(trans.first);
        location.setLongitude(trans.second);

        recent.add(coordHash(trans.first, trans.second));

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d(String.format(Locale.US, "%.4f,%.4f => %.4f,%.4f", latitude, longitude, trans.first, trans.second));
        }
    }

    static class Cache<K, V> extends LinkedHashMap<K, V> {

        private static final int KMaxEntries = 128;
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(final Entry<K, V> eldest) {
            return (size() > KMaxEntries);
        }
    }
}
