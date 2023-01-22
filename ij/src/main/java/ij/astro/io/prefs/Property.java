package ij.astro.io.prefs;

import ij.Prefs;
import ij.astro.logging.AIJLogger;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.function.Function;

public class Property<T> {
    private final Object owner;
    private T value;

    private String propertyKey;
    private boolean hasBuiltKey = false;
    private boolean hasLoaded = false;
    private final Function<String, T> deserializer;
    private final String keySuffix;
    private final String keyPrefix;
    private final Class<T> type;
    final HashSet<PropertyChangeListener<T>> listeners = new HashSet<>();

    public Property(T defaultValue, Object owner) {
        this(defaultValue, "", "", $ -> null, owner);
    }

    public Property(T defaultValue, Function<String, T> deserializer, Object owner) {
        this(defaultValue, "", "", deserializer, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Object owner) {
        this(defaultValue, keyPrefix, keySuffix, $ -> null, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Function<String, T> deserializer, Object owner) {
        this.owner = owner;
        this.deserializer = deserializer;
        this.keySuffix = keySuffix;
        this.keyPrefix = keyPrefix;
        this.type = (Class<T>) defaultValue.getClass();
        value = defaultValue;
    }

    public T get() {
        loadProperty();
        AIJLogger.log(this);
        return value;
    }

    public void set(T value) {
        updatePrefs(value);
        listeners.forEach(l -> l.valueChanged(getOrCreatePropertyKey(), value));
        this.value = value;
    }

    public String getPropertyKey() {
        return getOrCreatePropertyKey();
    }

    private void loadProperty() {
        if (!hasLoaded) {
            value = handleLoad();

            if (!type.isInstance(value)) {
                throw new IllegalStateException("Expected type: %s, received type %s".formatted(type, value.getClass()));
            }

            hasLoaded = true;
        }
    }

    private String getOrCreatePropertyKey() {
        if (!hasBuiltKey) {
            for (Field declaredField : owner.getClass().getDeclaredFields()) {
                if (declaredField.getType().equals(getClass())) {
                    try {
                        if (this.equals(declaredField.get(owner))) {
                            var gs = declaredField.toGenericString().split(" ");
                            propertyKey = keyPrefix + gs[gs.length - 1] + keySuffix;
                            hasBuiltKey = true;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return propertyKey;
    }

    private T handleLoad() {
        //todo add safeties for failed to parse, Optional?
        var nv = Prefs.get(getPropertyKey(), String.valueOf(value));
        if (type == Double.TYPE || type == Double.class) {
            return (T) Double.valueOf(nv);
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (T) Integer.valueOf(nv);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return (T) Boolean.valueOf(nv);
        } else if (type == String.class) {
            return (T) nv;
        } else if (type.isEnum()) {
            return (T) Enum.valueOf((Class<? extends Enum>) type, nv);
        } else if (type == Point.class) {
            var v = Prefs.getLocation(getPropertyKey());
            return v == null ? value : (T) v;
        }

        return deserializer.apply(nv);
    }

    private void updatePrefs(T value) {
        if (type == Point.class) {
            Prefs.saveLocation(getPropertyKey(), (Point) value);
            return;
        }

        Prefs.set(getPropertyKey(), value.toString());
    }

    @FunctionalInterface
    public interface PropertyChangeListener<T> {
        void valueChanged(String key, T newValue);
    }

    @Override
    public String toString() {
        return "Property{" +
                "owner=" + owner +
                ", value=" + value +
                ", propertyKey='" + getPropertyKey() + '\'' +
                ", type=" + type +
                '}';
    }
}