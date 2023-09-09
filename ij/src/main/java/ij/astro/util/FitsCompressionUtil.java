package ij.astro.util;

import ij.astro.gui.GenericSwingDialog;
import ij.astro.gui.RadioEnum;
import ij.astro.gui.nstate.NState;
import ij.astro.io.prefs.Property;
import nom.tam.fits.FitsException;
import nom.tam.fits.compression.algorithm.quant.QuantizeOption;
import nom.tam.fits.header.Compression;
import nom.tam.image.compression.hdu.CompressedImageHDU;

import javax.swing.*;

public class FitsCompressionUtil {
    private static final Property<CompressionState> INTEGER_COMPRESSION = new Property<>(CompressionState.RICE_1, FitsCompressionUtil.class);
    private static final Property<CompressionState> FLOAT_COMPRESSION = new Property<>(CompressionState.GZIP_1, FitsCompressionUtil.class);
    private static final Property<Dither> FLOAT_DITHER = new Property<>(Dither.DITHER_1, FitsCompressionUtil.class);
    private static final Property<Dither> INTEGER_DITHER = new Property<>(Dither.NONE, FitsCompressionUtil.class);
    private static final Property<Double> INTEGER_Q = new Property<>(4d, FitsCompressionUtil.class);
    private static final Property<Double> FLOAT_Q = new Property<>(0d, FitsCompressionUtil.class);

    public static void setCompression(CompressedImageHDU hdu, boolean isFloatingPoint) throws FitsException {
        Property<CompressionState> compPref;
        Property<Double> qPref;
        Property<Dither> ditherPref;
        if (isFloatingPoint) {
            compPref = FLOAT_COMPRESSION;
            qPref = FLOAT_Q;
            ditherPref = FLOAT_DITHER;
        } else {
            compPref = INTEGER_COMPRESSION;
            qPref = INTEGER_Q;
            ditherPref = INTEGER_DITHER;
        }

        hdu.setCompressAlgorithm(compPref.get().id);
        if (isFloatingPoint) {
            if (ditherPref.get() != Dither.NONE) {
                hdu.setQuantAlgorithm(ditherPref.get().id);
                hdu.getCompressOption(QuantizeOption.class).setQlevel(qPref.get());
            }
        }
    }

    public static void dialog() {
        var gd = new GenericSwingDialog("FITS Compression Settings");
        gd.addMessage("Floating Point Compression");
        gd.addNStateDropdown(FLOAT_COMPRESSION.get(), FLOAT_COMPRESSION::set);
        gd.addToSameRow();
        gd.addNStateDropdown(FLOAT_DITHER.get(), FLOAT_DITHER::set);
        gd.addToSameRow();
        gd.addUnboundedNumericField("Q Level", FLOAT_Q.get(), 1, 5, null, FLOAT_Q::set);
        gd.addDoubleSpaceLineSeparator();
        gd.addMessage("Integer Compression");
        gd.addNStateDropdown(INTEGER_COMPRESSION.get(), INTEGER_COMPRESSION::set);
        /*gd.addToSameRow();
        gd.addNStateDropdown(INTEGER_DITHER.get(), INTEGER_DITHER::set);
        gd.addToSameRow();
        gd.addUnboundedNumericField("Q Level", INTEGER_Q.get(), 1, 5, null, INTEGER_Q::set);*/
        gd.showDialog();
    }

    public static Property<CompressionState> compPref(boolean isFloatingPoint) {
        return isFloatingPoint ? FLOAT_COMPRESSION : INTEGER_COMPRESSION;
    }
    public static Property<Double> qPref(boolean isFloatingPoint) {
        return isFloatingPoint ? FLOAT_Q : INTEGER_Q;
    }
    public static Property<Dither> ditherPref(boolean isFloatingPoint) {
        return isFloatingPoint ? FLOAT_DITHER : INTEGER_DITHER;
    }

    public enum CompressionState implements NState<CompressionState>, RadioEnum {
        RICE_1(Compression.ZCMPTYPE_RICE_1),
        HCOMPRESS(Compression.ZCMPTYPE_HCOMPRESS_1),
        GZIP_1(Compression.ZCMPTYPE_GZIP_1),
        GZIP_2(Compression.ZCMPTYPE_GZIP_2),
        IRAF(Compression.ZCMPTYPE_PLIO_1), //todo needs testing
        // todo Debug only?
        NONE(Compression.ZCMPTYPE_NOCOMPRESS),
        ;

        final String id;

        CompressionState(String id) {
            this.id = id;
        }

        @Override
        public Icon icon() {
            return new EmojiIcon(name(), 13);
        }

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public CompressionState[] values0() {
            return CompressionState.values();
        }
    }

    public enum Dither implements NState<Dither>, RadioEnum {
        DITHER_1(Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1),
        DITHER_2(Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_2),
        NONE(Compression.ZQUANTIZ_NO_DITHER),
        ;

        final String id;

        Dither(String id) {
            this.id = id;
        }

        @Override
        public Icon icon() {
            return new EmojiIcon(name(), 13);
        }

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public Dither[] values0() {
            return Dither.values();
        }
    }
}
