package ij.astro.io.pixel_maps.codecs;

import java.util.List;
import java.util.Objects;

import ij.astro.io.aij.AijFileCodec;
import ij.astro.io.pixel_maps.BpmFile;
import ij.astro.io.pixel_maps.BpmHeader;
import ij.astro.util.PixelPatcher;

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
        var pxs = List.of(new PixelPatcher.Pixel(1, 2), new PixelPatcher.Pixel(3, 4));
        test.patches().put(PixelPatcher.PatchType.Type.AVERAGE_FILL, pxs);
        test.patches().put(PixelPatcher.PatchType.Type.MEDIAN_FILL, pxs);

        var s = write(test);

        System.out.println(s);

        var read = readContents(s);

        //IO.println(read);

        IO.println(Objects.equals(read, test));
    }
}
