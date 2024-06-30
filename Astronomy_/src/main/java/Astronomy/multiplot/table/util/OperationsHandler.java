package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.gui.ToolTipProvider;
import ij.astro.gui.nstate.NState;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;

import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;

public class OperationsHandler {
    private static final Property<Operator> OPERATOR = new Property<>(Operator.ADD, OperationsHandler.class);
    private static final Property<Double> OPERAND = new Property<>(0D, OperationsHandler.class);

    public static void dialog(MeasurementsWindow owner, String column) {
        var d = new GenericSwingDialog("Column Operations for " + column, owner);

        d.setOverridePosition(true);
        d.addMessage("Column Value (cv)");
        d.addNStateDropdown(OPERATOR.get(), OPERATOR::set);
        d.addToSameRow();
        d.addUnboundedNumericField("Operand (b):", OPERAND.get(), 1, 10, null, OPERAND::set);
        d.setOverridePosition(false);
        d.addDoubleSpaceLineSeparator();

        d.centerDialog(true);
        d.setIconImage(UIHelper.createImage("Astronomy/images/icons/table/calculator.png"));
        d.showDialog();

        if (d.wasOKed()) {
            owner.getTable().updateValues(column, cv -> OPERATOR.get().operator.applyAsDouble(cv, OPERAND.get()));
        }
    }

    public enum Operator implements NState<Operator>, ToolTipProvider {
        ADD("cv + b", Double::sum),
        MULTIPLY("cv * b", (cv, b) -> cv * b),
        DIVIDE("cv / b", (cv, b) -> cv / b),
        EXPONENTIATE("<html>cv<sup>b</sup></html>", Math::pow),//todo fast path for sqr/cube? sqr seems covered
        /**
         * Logarithm
         */
        ANTIEXPONENTIATE("<html>log<sub>b</sub>(cv)</html>", (cv, b) -> {
            if (b == 0) {
                return Math.log(cv);
            } else if (b == 1 || b == 10) {//todo document cases
                return Math.log10(cv);
            }
            return Math.log(cv) / Math.log(b);
        }),//todo fast path for e/10 (base 1, and 0 are undef)
        ROOT("<html><sup>b</sup>&radic cv</html>", (cv, b) -> {
            if (b == 2) {
                return Math.sqrt(cv);
            } else if (b == 3) {
                return Math.cbrt(cv);
            }
            return Math.pow(cv, 1D / b);
        }),
        ;

        final String description;
        /**
         * (columnValue, b) -> newColumnValue
         */
        final DoubleBinaryOperator operator;

        Operator(String description, DoubleBinaryOperator operator) {
            this.description = description;
            this.operator = operator;
        }

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public Operator[] values0() {
            return Operator.values();
        }

        @Override
        public Function<Operator, String> getDefaultTooltips() {
            return o -> o.description;
        }


        @Override
        public String getToolTip() {
            return description;
        }
    }
}
