package ij.astro.io.prefs;

import ij.IJ;
import ij.Prefs;
import ij.astro.gui.nstate.NState;
import ij.astro.util.UIHelper;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Enum} values are stored and loaded based on the value of {@link Enum#name()},
 * which should NOT change.
 *
 * @see PropertyKey for more control over the property key.
 *
 * @param <T> the type of the property.
 */
public class Property<T> {
    private final Object owner;
    private final Class<?> ownerClass;
    private volatile T value;
    private final T defaultValue;
    private PropertyLoadValidator<T> loadValidator;
    private PropertyChangeValidator<T> changeValidator;

    private String propertyKey;
    private boolean hasBuiltKey = false;
    private boolean hasLoaded = false;
    private final Function<String, T> deserializer;
    private final Function<T, String> serializer;
    private final Supplier<String> keySuffix;
    private final Supplier<String> keyPrefix;
    private final Class<T> type;
    final Set<PropertyChangeListener<T>> listeners = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Property<T>> variants = new HashMap<>();
    private static final HashSet<WeakReference<Property<?>>> propertyCache = new HashSet<>();

    public Property(T defaultValue, Object owner) {
        this(defaultValue, "", "", null, $ -> null, owner);
    }

    public Property(T defaultValue, Function<T, String> serializer, Function<String, T> deserializer, Object owner) {
        this(defaultValue, "", "", serializer, deserializer, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Object owner) {
        this(defaultValue, keyPrefix, keySuffix, null, $ -> null, owner);
    }

    public Property(T defaultValue, Supplier<String> keyPrefix, Supplier<String> keySuffix, Object owner) {
        this(defaultValue, keyPrefix, keySuffix, null, $ -> null, owner);
    }

    public Property(T defaultValue, String keyPrefix, String keySuffix, Function<T, String> serializer, Function<String, T> deserializer, Object owner) {
        this(defaultValue, () -> keyPrefix, () -> keySuffix, serializer, deserializer, owner);
    }

    @SuppressWarnings({"unchecked"})
    public Property(T defaultValue, Supplier<String> keyPrefix, Supplier<String> keySuffix,
            Function<T, String> serializer, Function<String, T> deserializer, Object owner) {
        this.deserializer = deserializer;
        this.keySuffix = keySuffix;
        this.keyPrefix = keyPrefix;
        this.type = (Class<T>) getType(defaultValue);
        value = defaultValue;
        this.serializer = serializer;
        if (owner instanceof Class<?> clazz) {
            this.ownerClass = clazz;
            this.owner = null;
        } else {
            this.owner = owner;
            this.ownerClass = owner.getClass();
        }
        this.defaultValue = defaultValue;
        propertyCache.add(new WeakReference<>(this));
    }

    private Property(String key, Property<T> base) {
        this.deserializer = base.deserializer;
        this.serializer = base.serializer;
        this.keySuffix = base.keySuffix;
        this.keyPrefix = base.keyPrefix;
        this.type = base.type;
        value = base.get();
        this.ownerClass = base.ownerClass;
        this.owner = base.owner;
        this.hasBuiltKey = true;
        this.propertyKey = key;
        this.defaultValue = base.defaultValue;
        propertyCache.add(new WeakReference<>(this));
        this.loadValidator = base.loadValidator;
        this.changeValidator = base.changeValidator;
    }

    public T get() {
        loadProperty();
        return value;
    }

    public void set(T value) {
        set(value, true);
    }

    public void setWithoutNotify(T value) {
        set(value, false);
    }

    @SuppressWarnings("unchecked")
    private void set(T value, boolean doNotify) {
        var valueChanged = !Objects.equals(value, this.value);

        if (valueChanged && changeValidator != null) {
            value = changeValidator.valueChanged(getPropertyKey(), this.value, value);
            valueChanged = !Objects.equals(value, this.value);
        }

        updatePrefs(value);
        this.value = value;
        if (doNotify && valueChanged) {
            for (PropertyChangeListener<T> l : listeners.toArray(PropertyChangeListener[]::new)) {
                l.valueChanged(getPropertyKey(), value);
            }
        }
    }

    public String getPropertyKey() {
        return getOrCreatePropertyKey();
    }

    public <X> X ifProp(X truthy) {
        return ifProp(truthy, null);
    }

    public <X> X ifProp(X truthy, X falsy) {
        if (type == Boolean.TYPE || type == Boolean.class) {
            if (((Boolean) get())) {
                return truthy;
            }
        }

        return falsy;
    }

    @Deprecated
    public void ifProp(Runnable truthy) {
        ifProp(truthy, () -> {});
    }

    @Deprecated
    public void ifProp(Runnable truthy, Runnable falsy) {
        if (type == Boolean.TYPE || type == Boolean.class) {
            if (((Boolean) get())) {
                truthy.run();
                return;
            }
        }

        falsy.run();
    }

    public boolean hasSaved() {
        return Prefs.ijPrefs.containsKey(Prefs.KEY_PREFIX+getPropertyKey());
    }

    /**
     * @param listener (key, newValue) -> {}
     */
    public void addListener(PropertyChangeListener<T> listener) {
        listeners.add(listener);
    }

    public void locationSavingWindow(Frame window) {
        locationSavingWindow(window, IJ.getInstance());
    }

    public void locationSavingWindow(Frame window, Frame reference) {
        if (type != Point.class) {
            return;
        }
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                set((T) e.getWindow().getLocation());
            }});
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (e.getComponent().isVisible()) {
                    set((T) e.getComponent().getLocationOnScreen());
                }
            }
        });
        if (hasSaved() || reference == null) {
            window.setLocation((Point) get());
        } else {
            UIHelper.setCenteredOnScreen(window, reference);
        }
    }

    public void sizeSavingWindow(Frame window) {
        if (type != Dimension.class) {
            return;
        }
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                set((T) e.getWindow().getSize());
            }});
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (e.getComponent().isVisible()) {
                    set((T) e.getComponent().getSize());
                }
            }
        });
        if (hasSaved()) {
            window.setSize((Dimension) get());
        }
    }

    public void clearListeners() {
        listeners.clear();
    }

    /**
     * @param loadValidator (loadedValue) -> value; value may equal loadedValue.
     */
    public void setLoadValidator(PropertyLoadValidator<T> loadValidator) {
        this.loadValidator = loadValidator;
    }

    /**
     * @param changeValidator (key, oldValue, newValue) -> value; value may equal newValue or oldValue.
     */
    public void setChangeValidator(PropertyChangeValidator<T> changeValidator) {
        this.changeValidator = changeValidator;
    }

    public Property<T> getOrCreateVariant(Object suffix) {
        return getOrCreateVariant(String.valueOf(suffix));
    }

    public Property<T> getOrCreateVariant(String suffix) {
        return getOrCreateVariant(suffix, "");
    }

    public Property<T> getOrCreateVariant(String suffix, String separator) {
        return variants.computeIfAbsent(getPropertyKey() + separator + suffix, s -> new Property<>(s, this));
    }

    public static void resetLoadStatus() {
        for (WeakReference<Property<?>> propertyWeakReference : propertyCache) {
            var p = propertyWeakReference.get();
            if (p != null) {
                p.resetProperty();
            }
        }
    }

    private void resetProperty() {
        hasLoaded = false;
        value = defaultValue;
    }

    private void loadProperty() {
        if (!hasLoaded) {
            try {
                value = handleLoad();
                if (loadValidator != null) {
                    setWithoutNotify(loadValidator.valueLoaded(value));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!type.isInstance(value)) {
                throw new IllegalStateException("Expected type: %s, received type %s"
                        .formatted(type, value == null ? null : value.getClass()));
            }

            hasLoaded = true;
        }
    }

    private String getOrCreatePropertyKey() {
        if (!hasBuiltKey) {
            for (Field declaredField : ownerClass.getDeclaredFields()) {
                if (Property.class.isAssignableFrom(declaredField.getType())) {
                    try {
                        if (Modifier.isStatic(declaredField.getModifiers()) && owner != null) {
                            continue;
                        }
                        if (!declaredField.canAccess(owner)) {
                            declaredField.trySetAccessible();
                        }
                        if (this.equals(declaredField.get(owner))) {
                            var pk = declaredField.getAnnotation(PropertyKey.class);
                            var gs = declaredField.toGenericString().split(" ");
                            var prefix = keyPrefix.get();
                            var suffix = keySuffix.get();
                            if (prefix == null) {
                                prefix = "";
                            }
                            if (suffix == null) {
                                suffix = "";
                            }
                            if (pk != null) {
                                if (pk.ignoreAffixes()) {
                                    propertyKey = pk.value();
                                } else {
                                    propertyKey = prefix + pk.value() + suffix;
                                }
                            } else {
                                propertyKey = prefix + gs[gs.length - 1] + suffix;
                            }

                            hasBuiltKey = true;
                            break;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return propertyKey;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private T handleLoad() {
        //todo add safeties for failed to parse, Optional?
        var nv = Prefs.get(getPropertyKey(), serializer != null ? serializer.apply(value) : String.valueOf(value));
        if (type == Double.TYPE || type == Double.class) {
            return (T) Double.valueOf(nv);
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (T) Integer.valueOf(nv);
        }else if (type == Float.TYPE || type == Float.class) {
            return (T) Float.valueOf(nv);
        } else if (type == Long.TYPE || type == Long.class) {
            return (T) Long.valueOf(nv);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return (T) Boolean.valueOf(nv);
        } else if (type == String.class) {
            return (T) nv;
        } else if (type.isEnum()) {
            // If the enum overrides #toString, we can't just use String#valueOf
            nv = Prefs.get(getPropertyKey(), ((Enum<?>) value).name());

            if (NState.class.isAssignableFrom(type)) {
                if (value != null) {
                    return (T) ((NState<?>) value).fromString(nv);
                }
            }
            return (T) Enum.valueOf((Class<? extends Enum>) type, nv);
        } else if (type == Point.class) {
            var v = Prefs.getLocation(getPropertyKey());
            return v == null ? value : (T) v;
        } else if (type == Dimension.class) {
            var v = Prefs.getLocation(getPropertyKey());
            if (v != null) {
                return (T) new Dimension(v.x, v.y);
            }
            return value;
        } /*else if (type == EnumSet.class) {
            //todo handle Serializable objects? byte array to string? will it have special chars that break things?
        }*/

        return deserializer.apply(nv);
    }

    private void updatePrefs(T value) {
        if (type == Point.class) {
            Prefs.saveLocation(getPropertyKey(), (Point) value);
            return;
        } else if (type == Dimension.class) {
            Prefs.saveLocation(getPropertyKey(), new Point(((Dimension) value).width, ((Dimension) value).height));
            return;
        } else if (type.isEnum()) {
            Prefs.set(getPropertyKey(), ((Enum<?>) value).name());
            return;
        }

        Prefs.set(getPropertyKey(), serializer != null ? serializer.apply(value) : value.toString());
    }

    private static Class<?> getType(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("default value may not be null");
        }

        var c = o.getClass();
        var s = c.getSuperclass();
        if (s != null && s.isEnum()) {
            return s;
        }

        return c;
    }

    /**
     * Takes (key, newValue) for processing.
     */
    @FunctionalInterface
    public interface PropertyChangeListener<T> {
        void valueChanged(String key, T newValue);
    }

    /**
     * Takes the loadedValue and returns a (possibly different) value.
     */
    @FunctionalInterface
    public interface PropertyLoadValidator<T> {
        T valueLoaded(T loadedValue);
    }

    /**
     * Takes (key, oldValue, newValue) and returns a (possibly different) new value.
     */
    @FunctionalInterface
    public interface PropertyChangeValidator<T> {
        T valueChanged(String key, T oldValue, T newValue);
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyKey='" + getPropertyKey() + '\'' +
                ", owner=" + owner +
                ", heldValue=" + value +
                ", type=" + type +
                '}';
    }
}