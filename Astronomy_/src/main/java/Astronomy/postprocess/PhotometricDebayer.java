package Astronomy.postprocess;

import astroj.FitsJ;
import astroj.IJU;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.FITS_Writer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.IntProcessor;
import ij.process.ShortProcessor;
import util.GenericSwingDialog;

import java.io.File;
import java.nio.file.Path;
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
    private boolean isVirtual = false;
    private Path virtualDebayerFolder = null;

    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
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
            options[i] = Color.values()[i].toString();
            defaults[i] = enabledColors.getOrDefault(Color.values()[i], true);
            int finalI = i;
            settings.add(b -> enabledColors.put(Color.values()[finalI], b));
        }

        gd.addChoice("Subpixel arrangement", Pallete.names(), pallet.get().name(),
                (s) -> pallet.set(Pallete.valueOf(s)));
        gd.addMessage("Output images:");
        gd.addCheckboxGroup((Color.values().length/2) + 1, 2, options, defaults, settings);
        gd.centerDialog(true);
        gd.showDialog();

        Color.resetStacks();

        try {
            if (gd.wasOKed()) {
                processImage(imp, pallet.get());

                if (!isVirtual) {
                    for (Color color : Color.values()) {
                        if (!enabledColors.get(color)) continue;
                        var impC = color.makeStackDisplayable(imp.getTitle());
                        header = headerUpdate(header, impC);
                        FitsJ.putHeader(impC, header);
                        impC.show();
                    }
                } else {
                    IJ.showMessage("Virtual stack debayering complete.\nAligned images are saved in subdirectory 'debayered'.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        isVirtual = imp.getStack().isVirtual();
        if (isVirtual) {
            var imageDir = Path.of(imp.getOriginalFileInfo().directory, "debayered");
            File dir = imageDir.toFile();
            if (!dir.exists()) {
                dir.mkdir();
            } else if (dir.isFile()) {
                IJ.beep();
                IJ.showMessage("A file named 'debayered' in the stack directory is blocking the creation of the sub-directory.");
                return DONE;
            }
            virtualDebayerFolder = imageDir;
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

                if (!isVirtual) {
                    color.stack.addSlice(stack.getSliceLabel(slice), mim.makeImageProcessor(pallete, color, transform));
                } else {
                    var imageFilename = IJU.getSliceFilename(imp, slice);
                    ImagePlus imp2 = new ImagePlus(imp.getStack().getSliceLabel(slice), mim.makeImageProcessor(pallete, color, transform));
                    imp2.setCalibration(imp.getCalibration());
                    imp2.setFileInfo(imp.getFileInfo());
                    String[] scienceHeader = FitsJ.getHeader(imp);
                    if (scienceHeader != null) {
                        scienceHeader = headerUpdate(scienceHeader, imp2);
                        FitsJ.putHeader(imp2, scienceHeader);
                    }
                    if (color == Color.LUMINOSITY) {
                        imp2.setType(ImagePlus.GRAY32);
                        imp2.getStack().setOptions("32-bit int");
                    } else if (imp2.getBytesPerPixel() == 1){
                        imp2.setType(ImagePlus.GRAY8);
                    } else {
                        imp2.setType(ImagePlus.GRAY16);
                    }
                    FITS_Writer.saveImage(imp2, virtualDebayerFolder.resolve(color.name().toLowerCase()).resolve(imageFilename).toString());
                }
            }
        }
    }

    private String[] headerUpdate(String[] header, ImagePlus imp) {
        if (header == null) return null;
        header = FitsJ.setCard("NAXIS1", imp.getWidth()/2, "Width", header);
        header = FitsJ.setCard("NAXIS2", imp.getHeight()/2, "Height", header);
        header = FitsJ.removeCards("BAYERPAT", header);
        header = FitsJ.removeCards("ROWORDER", header);
        header = FitsJ.removeCards("XBAYROFF", header);
        header = FitsJ.removeCards("YBAYROFF", header);
        header = FitsJ.removeCards("IMAGEW", header);
        header = FitsJ.removeCards("IMAGEH", header);

        return header;
    }

    // raw coords -> metacoords (half the size of the original, represent the full subbixel array (2x2 of raw pixels))

    /**
     * x is width, y is height. Top left pixel is 0, 0
     */
    record MetaImage(MetaPixel[] pixels, int width, int height, BiFunction<Integer, Integer, ImageProcessor> ipMaker) {
        public MetaImage(int width, int height, BiFunction<Integer, Integer, ImageProcessor> ipMaker) {
            this(new MetaPixel[width * height], width, height, ipMaker);
        }

        private MetaImage(ImageProcessor ip) {
            this(ip.getWidth() / 2, ip.getHeight() / 2, getMaker(ip));
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
            var ip = color.makeImageProcessor(ipMaker).apply(width, height);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    ip.putPixel(x, y, pallete.getColorValue(transform.apply(getMetaPixel(x, y)), color));
                }
            }

            return ip;
        }

        private static BiFunction<Integer, Integer, ImageProcessor> getMaker(ImageProcessor ip) {
            if (ip instanceof ShortProcessor) {
                return ShortProcessor::new;
            } else if (ip instanceof ByteProcessor) {
                return ByteProcessor::new;
            }
            return ShortProcessor::new;
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
        } else {
            transform = transform.andThen(MetaPixel::flipY);
        }

        var bayerShiftXI = FitsJ.findCardWithKey("XBAYROFF", header);
        var bayerShiftYI = FitsJ.findCardWithKey("YBAYROFF", header);

        if (bayerShiftXI != -1) {
            var xs = FitsJ.getCardIntValue(header[bayerShiftXI]);
            transform = transform.andThen(xs % 2 == 0 ? Function.identity() : MetaPixel::flipX);
        }

        if (bayerShiftYI != -1) {
            var ys = FitsJ.getCardIntValue(header[bayerShiftYI]);
            transform = transform.andThen(ys % 2 == 0 ? Function.identity() : MetaPixel::flipY);
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
                    case GAV_LUM -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft) + metaPixel.bottomRight + metaPixel.topLeft;
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
                    case GAV_LUM -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight) + metaPixel.bottomLeft + metaPixel.topRight;
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
                    case GAV_LUM -> avgGreen(metaPixel.topLeft, metaPixel.bottomRight) + metaPixel.bottomLeft + metaPixel.topRight;
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
                    case GAV_LUM -> avgGreen(metaPixel.topRight, metaPixel.bottomLeft) + metaPixel.bottomRight + metaPixel.topLeft;
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
        LUMINOSITY {
            @Override
            public String toString() {
                return "R+G₁+G₂+B";
            }
        },
        G_AVE_LUM {
            @Override
            public String toString() {
                return "R+½(G₁+G₂)+B";
            }
        };

        public BiFunction<Integer, Integer, ImageProcessor>
        makeImageProcessor(BiFunction<Integer, Integer, ImageProcessor> ipMaker) {
            if (this == LUMINOSITY) return IntProcessor::new;
            return ipMaker;
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
