package Astronomy.multiaperture.io;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.io.transformers.*;
import astroj.Aperture;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Transformers {
    private static final Map<Class<?>, Transformer<?>> transformerMap = new HashMap<>();

    static {
        registerTransformer(Header.class, new Header());
        registerTransformer(Properties.class, new PrefsTransformer("multiapertureSettings"));
        registerTransformer(Aperture.class, new ApertureTransformer());
        registerTransformer(AffineTransform.class, new AffineTransformTransformer());
        registerTransformer(Shape.class, new ShapeTransformer());
        registerTransformer(CompositeShape.class, new CompositeShapeTransformer());
        registerTransformer(ApFile.class, new ApertureFileTransformer());
    }

    public static <T> void registerTransformer(Class<T> clazz, Transformer<T> transformer) {
        transformerMap.put(clazz, transformer);
    }

    public static <T> T read(Class<T> clazz, Section section) {
        var transformer = transformerMap.get(clazz);

        if (transformer == null) {
            throw new IllegalStateException("Could not find transformer for " + clazz.getName());
        }

        try {
            return (T) transformer.load(section);
        } catch (Exception e) {
            throw new RuntimeException("Error reading section: " + section, e);
        }
    }

    public static <T> Section write(Class<T> clazz, T obj) {
        var transformer = (Transformer<T>) transformerMap.get(clazz);

        if (transformer == null) {
            throw new IllegalStateException("Could not find transformer for " + clazz.getName());
        }

        return transformer.write(obj);
    }
}
