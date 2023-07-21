package Astronomy.multiplot.settings;

import Astronomy.MultiPlot_;
import ij.astro.gui.MergedIcon;
import ij.astro.gui.nstate.NState;
import ij.astro.util.EmojiIcon;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public enum MPOperator implements NState<MPOperator> {
    NONE(null, "", false),
    DIVIDE_BY("astroj/images/icons/divide.png", " / ", true),
    MULTIPLY_BY("astroj/images/icons/multiply.png", " * ", true),
    SUBTRACT("astroj/images/icons/subtract.png", " - ", true),
    ADD("astroj/images/icons/add.png", " + ", true),
    CENTROID_DISTANCE("astroj/images/icons/centroidDistance.png", " -> ", false),
    CUSTOM_ERROR("astroj/images/icons/customError.png", " (with error) ", false),
    ;

    private final Icon icon;
    private final String symbol;
    private final boolean normalOperator;

    MPOperator(String path, String symbol, boolean normalOperator) {
        this.icon = makeIcon(path);
        this.symbol = symbol;
        this.normalOperator = normalOperator;
    }

    @Override
    public Icon icon() {
        return icon;
    }

    @Override
    public boolean isOn() {
        return this != NONE;
    }

    @Override
    public MPOperator next() {
        var n = NState.super.next();
        if (n == CENTROID_DISTANCE && !MultiPlot_.hasRefStars()) {
            n = CENTROID_DISTANCE.next();
        }
        return n;
    }

    @Override
    public MPOperator previous() {
        var n = NState.super.previous();
        if (n == CENTROID_DISTANCE && !MultiPlot_.hasRefStars()) {
            n = CENTROID_DISTANCE.previous();
        }
        return n;
    }

    @Override
    public MPOperator[] values0() {
        return MPOperator.values();
    }

    @Override
    public MPOperator fromString(String s) {
        if (s.length() < 3) {
            try {
                Integer i = Integer.getInteger(s);
                return switch (i) {
                    case 0 -> NONE;
                    case 1 -> DIVIDE_BY;
                    case 2 -> MULTIPLY_BY;
                    case 3 -> SUBTRACT;
                    case 4 -> ADD;
                    case 5 -> CENTROID_DISTANCE;
                    case 6 -> CUSTOM_ERROR;
                    default -> NState.super.fromString(s);
                };
            } catch (Exception ignored) {

            }
        }
        return NState.super.fromString(s);
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isNormalOperator() {
        return normalOperator;
    }

    @Override
    public Function<MPOperator, String> getDefaultTooltips() {
        return o -> switch (o) {
            case NONE -> "No Operation, [Divide, Multiply, Subtract, Add, Centroid Distance, Custom Error]";
            case DIVIDE_BY -> "Divide";
            case MULTIPLY_BY -> "Multiply";
            case SUBTRACT -> "Subtract";
            case ADD -> "Add";
            case CENTROID_DISTANCE -> "Centroid Distance";
            case CUSTOM_ERROR -> "Custom Error";
        };
    }

    private Icon makeIcon(String path) {
        if (path == null) {
            return new MergedIcon(new EmojiIcon("■", 19, Color.WHITE), new EmojiIcon("☐", 19), -3, 2, true);
        }
        var i = new ImageIcon(getClass().getClassLoader().getResource(path), this.name());
        return new ImageIcon(i.getImage().getScaledInstance(14, 14,  java.awt.Image.SCALE_SMOOTH), this.name());
    }
}
