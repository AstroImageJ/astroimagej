package Astronomy.multiaperture.io;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Properties;

import Astronomy.multiaperture.CompositeShape;
import Astronomy.multiaperture.io.transformers.AffineTransformTransformer;
import Astronomy.multiaperture.io.transformers.ApertureFileTransformer;
import Astronomy.multiaperture.io.transformers.ApertureHeader;
import Astronomy.multiaperture.io.transformers.ApertureTransformer;
import Astronomy.multiaperture.io.transformers.CompositeShapeTransformer;
import Astronomy.multiaperture.io.transformers.FlatMatrixTransformer;
import Astronomy.multiaperture.io.transformers.PrefsTransformer;
import Astronomy.multiaperture.io.transformers.ShapeTransformer;
import astroj.Aperture;
import ij.astro.io.aij.AijFileCodec;

public class AperturesFileCodec {
    public static AijFileCodec CODEC = new AijFileCodec() {
        @Override
        public void initialize() {
            registerTransformer(ApertureHeader.class, new ApertureHeader());
            registerTransformer(Properties.class, new PrefsTransformer(this));
            registerTransformer(Aperture.class, new ApertureTransformer(this));
            registerTransformer(AffineTransform.class, new AffineTransformTransformer(this));
            registerTransformer(Shape.class, new ShapeTransformer(this));
            registerTransformer(CompositeShape.class, new CompositeShapeTransformer(this));
            registerTransformer(ApFile.class, new ApertureFileTransformer(this));
            registerTransformer(double[].class, new FlatMatrixTransformer(this));
        }
    };

    public static ApFile readContents(String contents) {
        try {
            return CODEC.read(ApFile.class, AijFileCodec.readToSection(contents));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ApFile readFile(String filePath) {
        try {
            return CODEC.read(ApFile.class, AijFileCodec.read(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String write(ApFile apFile) {
        return AijFileCodec.write(CODEC.write(ApFile.class, apFile));
    }
}
