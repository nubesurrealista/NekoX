package org.telegram.messenger.chromecast;

import java.util.ArrayList;

public class ChromecastMediaVariations {
    // ChromecastMedia
    private final ArrayList<Object> variations;

    private ChromecastMediaVariations(ArrayList<Object> list) {
        variations = list;
    }

    private ChromecastMediaVariations(Object media) {
        variations = new ArrayList<>(1);
        variations.add(media);
    }

    public int getVariationsCount () {
        return variations.size();
    }

    public Object getVariation(int index) {
        return variations.get(index);
    }

    public static ChromecastMediaVariations of (Object list) {
        return new ChromecastMediaVariations(list);
    }

    public static ChromecastMediaVariations of (ArrayList<Object> list) {
        return new ChromecastMediaVariations(list);
    }

    public static class Builder {
        private final ArrayList<Object> variations = new ArrayList<>();

        public Builder add (Object media) {
            this.variations.add(media);
            return this;
        }

        public ChromecastMediaVariations build () {
            return new ChromecastMediaVariations(this.variations);
        }

        public boolean isEmpty () {
            return this.variations.isEmpty();
        }
    }
}
