package Astronomy.postprocess;

import astroj.FitsJ;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import ij.process.IntProcessor;
import ij.process.ShortProcessor;
import util.GenericSwingDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class PhotometricDebayer implements ExtendedPlugInFilter {
    private static final String ENABLE_COLOR_BASE = ".photomatric.debayer_color_";
    private final HashMap<Color, Boolean> enabledColors = new HashMap<>();

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        /*AIJLogger.log(4);
        //FITS_Writer.writeImageData(imp.getStack().getProcessor(1), null);
        FITS_Writer.saveImage(imp, null);
        AIJLogger.log(1);
        if (true)return DONE;*/

        var gd = new GenericSwingDialog("Debayer");

        AtomicReference<Pallete> pallet = new AtomicReference<>(Pallete.RGGB);

        // Suggest pallet from header
        var header = FitsJ.getHeader(imp);
        if (header != null) {
            var cardId = FitsJ.findCardWithKey("BAYERPAT", header);

            if (cardId != -1) {
                if (Arrays.asList(Pallete.names()).contains(FitsJ.getCardStringValue(header[cardId]))) {
                    pallet.set(Pallete.valueOf(FitsJ.getCardStringValue(header[cardId])));
                }
            }
        }

        var options = new String[Color.values().length];
        var defaults = new boolean[Color.values().length];
        var settings = new ArrayList<Consumer<Boolean>>();
        for (int i = 0; i < Color.values().length; i++) {
            options[i] = Color.values()[i].name();
            defaults[i] = enabledColors.getOrDefault(Color.values()[i], true);
            int finalI = i;
            settings.add(b -> enabledColors.put(Color.values()[finalI], b));
        }

        gd.addChoice("Subpixel arrangement", Pallete.names(), pallet.get().name(),
                (s) -> pallet.set(Pallete.valueOf(s)));
        gd.addCheckboxGroup(2, 2, options, defaults, settings);
        gd.centerDialog(true);
        gd.showDialog();

        Color.resetStacks();

        if (gd.wasOKed()) {
            processImage(imp, pallet.get());

            //FitsJ.copyHeader(imp, impC);
            header = FitsJ.setCard("NAXIS1", imp.getWidth()/2, "Width", header);
            header = FitsJ.setCard("NAXIS2", imp.getHeight()/2, "Height", header);

            for (Color color : Color.values()) {
                if (!enabledColors.get(color)) continue;
                var impC = color.makeStackDisplayable(imp.getTitle());
                FitsJ.putHeader(impC, header);
                impC.show();
            }
        }

        savePrefs();
        return DOES_16 | DOES_8G | NO_CHANGES | DONE;
    }

    @Override
    public void setNPasses(int nPasses) {
        // STUB
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        if (imp.getWidth() % 2 != 0 && imp.getHeight() % 2 != 0) {
            IJ.error("Image is not in the form of 2x2 subpixels.");
            return DONE;
        }
        loadPrefs();
        return DOES_16 | DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
        // Not called
    }

    private void processImage(ImagePlus imp, Pallete pallete) {
        var stackSize = imp.getStackSize();
        var stack = imp.getStack();

        for (int slice = 1; slice <= stackSize; slice++) {
            var mim = MetaImage.createImage(stack.getProcessor(slice));
            for (Color color : Color.values()) {
                if (!enabledColors.get(color)) continue;
                var transform = buildTransforms(FitsJ.getHeader(imp, slice));
                color.stack.addSlice(stack.getSliceLabel(slice), mim.makeImageProcessor(pallete, color, transform));
            }
        }
    }

    // raw coords -> metacoords (half the size of the original, represent the full subbixel array (2x2 of raw pixels))

    /**
     * x is width, y is height. Top left pixel is 0, 0
     */
    record MetaImage(MetaPixel[] pixels, int width, int height) {
        public MetaImage(int width, int height) {
            this(new MetaPixel[width * height], width, height);
        }

        private MetaImage(ImageProcessor ip) {
            this(ip.getWidth() / 2, ip.getHeight() / 2);
        }

        public MetaPixel getMetaPixel(int x, int y) {
            return pixels[x + (y * width)];
        }

        public static MetaImage createImage(ImageProcessor ip) {
            var mim = new MetaImage(ip);

            for (int x = 0; x < mim.width(); x++) {
                for (int y = 0; y < mim.height(); y++) {
                    var xi = 2*x;
                    var yi = 2*y;

                    // Extract subpixels
                    var topLeft = (short) ip.get(xi ,yi);
                    xi++;
                    var topRight = (short) ip.get(xi ,yi);
                    yi++;
                    var bottomRight = (short) ip.get(xi ,yi);
                    xi--;
                    var bottomLeft = (short) ip.get(xi ,yi);
                    mim.pixels[x + (y * mim.width)] = new MetaPixel(topLeft, topRight, bottomLeft, bottomRight);
                }
            }

            return mim;
        }

        public ImageProcessor makeImageProcessor(Pallete pallete, Color color, Function<MetaPixel, MetaPixel> transform) {
            var ip = color.makeImageProcessor.apply(width, height);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    ip.putPixel(x, y, pallete.getColorValue(transform.apply(getMetaPixel(x, y)), color));
                }
            }

            return ip;
        }
    }

    // The extra identity functions could be removed, but are left in for clarity
    Function<MetaPixel, MetaPixel> buildTransforms(String[] header) {
        Function<MetaPixel, MetaPixel> transform = Function.identity();

        var orderI = FitsJ.findCardWithKey("ROWORDER", header);
        if (orderI != -1) {
            var s = FitsJ.getCardStringValue(header[orderI]);
            if ("BOTTOM-UP".equals(s)) {
                transform = transform.andThen(Function.identity());
            } else {
                transform = transform.andThen(MetaPixel::flipY);
            }
        }

        var bayerShiftXI = FitsJ.findCardWithKey("XBAYROFF", header);
        var bayerShiftYI = FitsJ.findCardWithKey("YBAYROFF", header);

        if (bayerShiftXI != -1) {
            var xs = FitsJ.getCardIntValue(header[bayerShiftXI]);
            transform = xs % 2 == 0 ? Function.identity() : MetaPixel::flipX;
        }

        if (bayerShiftYI != -1) {
            var ys = FitsJ.getCardIntValue(header[bayerShiftYI]);
            transform = ys % 2 == 0 ? Function.identity() : MetaPixel::flipY;
        }

        return transform;
    }

    private void savePrefs() {
        enabledColors.forEach((c, b) -> Prefs.set(ENABLE_COLOR_BASE.substring(1)+c.name(), b));
    }

    private void loadPrefs() {
        for (Color color : Color.values()) {
            enabledColors.put(color, Prefs.getBoolean( ENABLE_COLOR_BASE+color.name(), true));
        }
    }

    /**
     * Describes the 2x2 super/meta pixel of the pallete
     */
    record MetaPixel(short topLeft, short topRight, short bottomLeft, short bottomRight) {
        int topLeftAsInt() {
            return Short.toUnsignedInt(topLeft);
        }

        int topRightAsInt() {
            return Short.toUnsignedInt(topRight);
        }

        int bottomLeftAsInt() {
            return Short.toUnsignedInt(bottomLeft);
        }

        int bottomRightAsInt() {
            return Short.toUnsignedInt(bottomRight);
        }

        /**
         * @return the metaPixel rotated over the y-axis, such that the top left corner is now the top right corner
         */
        MetaPixel flipX() {
            return new MetaPixel(topRight, topLeft, bottomRight, bottomLeft);
        }

        /**
         * @return the metaPixel rotated over the x-axis, such that the top left corner is now the bottom left corner
         */
        MetaPixel flipY() {
            return new MetaPixel(bottomLeft, bottomRight, topLeft, topRight);
        }
    }

    enum Pallete {
        BGGR {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.bottomRightAsInt();
                    case BLUE -> metaPixel.topLeftAsInt();
                    case GREEN -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        GBRG {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.bottomLeftAsInt();
                    case BLUE -> metaPixel.topRightAsInt();
                    case GREEN -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        GRBG {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.topRightAsInt();
                    case BLUE -> metaPixel.bottomLeftAsInt();
                    case GREEN -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        },
        RGGB {
            public int getColorValue(MetaPixel metaPixel, Color color) {
                return switch (color) {
                    case RED -> metaPixel.topLeftAsInt();
                    case BLUE -> metaPixel.bottomRightAsInt();
                    case GREEN -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft);
                    case LUMINOSITY -> sum(metaPixel);
                };
            }
        };

        abstract int getColorValue(MetaPixel metaPixel, Color color);

        public static String[] names() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }

        private static int avgGreen(short a, short b) {
            return ((Short.toUnsignedInt(a) + Short.toUnsignedInt(b)) / 2);
        }

        private static int sum(MetaPixel mp) {
            return mp.topLeftAsInt() + mp.bottomLeftAsInt() + mp.bottomRightAsInt() + mp.topRightAsInt();
        }
    }

    enum Color {
        RED,
        GREEN,
        BLUE,
        LUMINOSITY(IntProcessor::new);

        public final BiFunction<Integer, Integer, ImageProcessor> makeImageProcessor;

        Color() {
            makeImageProcessor = ShortProcessor::new;
        }

        Color(BiFunction<Integer, Integer, ImageProcessor> makeImageProcessor) {
            this.makeImageProcessor = makeImageProcessor;
        }

        public ImageStack stack = new ImageStack();

        public ImagePlus makeStackDisplayable(String title) {
            if (this == LUMINOSITY) stack.setOptions("32-bit int");
            var impC = new ImagePlus(title + " (" + name() + ")", stack);
            if (this == Color.LUMINOSITY) impC.setType(ImagePlus.GRAY32);
            return impC;
        }

        static void resetStacks() {
            for (Color value : values()) {
                value.stack = new ImageStack();
            }
        }
    }
}
