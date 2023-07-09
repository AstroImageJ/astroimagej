package Astronomy.multiplot.settings;

import ij.astro.gui.nstate.NState;

import javax.swing.*;
import java.util.function.Function;

public enum MPOperator implements NState<MPOperator> {
    NONE("astroj/images/icons/none.png", "", false),
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
        return Enum::name;
    }

    private ImageIcon makeIcon(String path) {
        var i = new ImageIcon(getClass().getClassLoader().getResource(path), this.name());
        return new ImageIcon(i.getImage().getScaledInstance(14, 14,  java.awt.Image.SCALE_SMOOTH), this.name());
    }
}
