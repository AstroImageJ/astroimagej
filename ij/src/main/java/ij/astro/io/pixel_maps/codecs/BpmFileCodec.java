package ij.astro.io.pixel_maps.codecs;

import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.pixel_maps.BpmFile;
import ij.astro.io.pixel_maps.BpmHeader;
import ij.astro.util.PixelPatcher;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BpmFileCodec {
    public static final String EXT = "bpm";
    public static final AijFileCodec CODEC = new AijFileCodec() {
        @Override
        public void initialize() {
            registerTransformer(BpmFile.class, new BpmFileTransformer(this));
            registerTransformer(BpmHeader.class, new BpmHeaderTransformer(this));
        }
    };

    public static BpmFile readContents(String contents) {
        try {
            return CODEC.read(BpmFile.class, AijFileCodec.readToSection(contents));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BpmFile readFile(String filePath) {
        try {
            return CODEC.read(BpmFile.class, AijFileCodec.read(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String write(BpmFile apFile) {
        return AijFileCodec.write(CODEC.write(BpmFile.class, apFile));
    }

    static void main() {
        var test = new BpmFile();
        var pxs = new HashSet<PixelPatcher.BpmPixel>(Set.of(new PixelPatcher.BpmPixel.Pixel(1, 2), new PixelPatcher.BpmPixel.Pixel(3, 4)));
        test.patches().put(new PixelPatcher.PatchType.AverageFill(1, 1), pxs);
        test.patches().put(new PixelPatcher.PatchType.MedianFill(1, 1), pxs);
        test.patches().put(new PixelPatcher.PatchType.AverageFill(3, 3), pxs);
        test.patches().put(new PixelPatcher.PatchType.MedianFill(4, 4), pxs);
        test.patches().put(new PixelPatcher.PatchType.NearestNeighbor(PixelPatcher.PatchType.NearestNeighbor.MergeType.AVERAGE), pxs);
        test.patches().put(new PixelPatcher.PatchType.NearestNeighbor(PixelPatcher.PatchType.NearestNeighbor.MergeType.MEDIAN), pxs);
        test.patches().put(new PixelPatcher.PatchType.FitGaussian(), pxs);
        test.patches().put(new PixelPatcher.PatchType.FitMoffat(), pxs);
        test.patches().put(new PixelPatcher.PatchType.FloodFill(), pxs);
        test.patches().put(new PixelPatcher.PatchType.FitPlane(), pxs);
        test.patches().put(new PixelPatcher.PatchType.ConstantValue(Double.NaN), pxs);

        var s = write(test);

        System.out.println(s);

        var read = readContents(s);

        //IO.println(write(read));

        IO.println(Objects.equals(read, test));
    }
}
