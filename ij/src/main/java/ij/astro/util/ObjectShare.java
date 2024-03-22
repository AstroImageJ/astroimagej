package ij.astro.util;

import java.util.HashMap;

public final class ObjectShare {
    private final HashMap<String, Object> objectShare = new HashMap<>();
    private static final ObjectShare OBJECT_SHARE = new ObjectShare();

    private ObjectShare() {
    }

    public static Object get(String key) {
        return OBJECT_SHARE.objectShare.get(key);
    }

    public static Object put(String key, Object value) {
        return OBJECT_SHARE.objectShare.put(key, value);
    }

    public static Object putIfAbsent(String key, Object value) {
        return OBJECT_SHARE.objectShare.putIfAbsent(key, value);
    }
}
