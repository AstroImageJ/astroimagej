package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.IJ;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.gui.ToolTipProvider;
import ij.astro.gui.nstate.NState;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;
import ij.measure.ResultsTable;
import org.hipparchus.special.Gamma;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;

public class OperationsHandler {
    private static final String VALUE_SOURCE_CONST_B = "Constant: b (from input box)";
    private static final String VALUE_SOURCE_CONST_E = "Constant: e";
    private static final String VALUE_SOURCE_CONST_PI = "Constant: Pi";
    private static final Property<Operator> OPERATOR = new Property<>(Operator.ADD, OperationsHandler.class);
    private static final Property<Double> OPERAND = new Property<>(0D, OperationsHandler.class);
    private static final Property<Boolean> FLIP_INPUTS = new Property<>(false, OperationsHandler.class);
    private static final Property<Boolean> RESULTS_IN_NEW_COLUMN = new Property<>(false, OperationsHandler.class);
    private static final Property<ColumnType> COLUMN_TYPE = new Property<>(ColumnType.POSTFIX, OperationsHandler.class);
    private static final Property<String> COLUMN_NAME = new Property<>("_m", OperationsHandler.class);
    private static final Property<String> VALUE_SOURCE = new Property<>(VALUE_SOURCE_CONST_B, OperationsHandler.class);

    public static void dialog(MeasurementsWindow owner, String column) {
        var d = new GenericSwingDialog("Column Operations for " + column, owner);

        d.setOverridePosition(true);
        d.addMessage("Column Value (cv)");
        d.addNStateDropdown(OPERATOR.get(), OPERATOR::set);
        d.addToSameRow();
        var bInput = d.addUnboundedNumericField("Operand (b):", OPERAND.get(), 1, 10, null, OPERAND::set);

        var options = new ArrayList<String>(owner.getTable().getLastColumn());
        options.add(VALUE_SOURCE_CONST_B);
        options.add(VALUE_SOURCE_CONST_E);
        options.add(VALUE_SOURCE_CONST_PI);

        var headings = new ArrayList<String>();
        Collections.addAll(headings, owner.getTable().getHeadings());

        // Remove labels column
        if (owner.getTable().hasRowLabels()) {
            headings.remove(0);
        }

        options.addAll(headings);

        // Check if column exists
        var previousSource = VALUE_SOURCE.get();
        var isConst = VALUE_SOURCE_CONST_B.equals(previousSource) ||
                VALUE_SOURCE_CONST_E.equals(previousSource) || VALUE_SOURCE_CONST_PI.equals(previousSource);

        // Reset to const_b if column does not exist
        if (!isConst && !owner.getTable().columnExists(VALUE_SOURCE.get())) {
            VALUE_SOURCE.set(VALUE_SOURCE_CONST_B);
        }

        // Disable input when not constant
        bInput.c1().setEnabled(VALUE_SOURCE_CONST_B.equals(previousSource));
        VALUE_SOURCE.addListener((key, s) -> bInput.c1().setEnabled(VALUE_SOURCE_CONST_B.equals(s)));

        d.addToSameRow();
        var c = d.addChoice("b value source", options.toArray(String[]::new), VALUE_SOURCE.get(), VALUE_SOURCE::set);
        c.c2().setToolTipText("Select value source to use as the 'b' value with the operator.");
        d.addToSameRow();
        d.addCheckbox("Flip inputs", FLIP_INPUTS.get(), FLIP_INPUTS::set).setToolTipText("Flip Inputs (cv, b) -> (b, cv)");
        d.setOverridePosition(false);

        d.addLineSeparator();

        d.addCheckbox("Put results in new column", RESULTS_IN_NEW_COLUMN.get(), RESULTS_IN_NEW_COLUMN::set);
        d.addToSameRow();
        var typeBox = d.addNStateDropdown(COLUMN_TYPE.get(), COLUMN_TYPE::set);
        var textField = new JTextField(COLUMN_NAME.get(), 10);
        textField.addActionListener(l -> COLUMN_NAME.set(l.getActionCommand()));
        d.addToSameRow();
        d.addGenericComponent(textField);

        RESULTS_IN_NEW_COLUMN.addListener(($, putResultsInNewCol) -> {
            typeBox.setEnabled(putResultsInNewCol);
            textField.setEnabled(putResultsInNewCol);
        });

        if (!RESULTS_IN_NEW_COLUMN.get()) {
            typeBox.setEnabled(false);
            textField.setEnabled(false);
        }

        d.centerDialog(true);
        d.setIconImage(UIHelper.createImage("Astronomy/images/icons/table/calculator.png"));
        d.showDialog();

        if (d.wasOKed()) {
            DoubleBinaryOperator baseOp = OPERATOR.get().getOperator();
            DoubleBinaryOperator operator;
            if (FLIP_INPUTS.get()) {
                operator = (cv, b) -> baseOp.applyAsDouble(b, cv);
            } else {
                operator = baseOp;
            }

            var destCol = column;

            if (RESULTS_IN_NEW_COLUMN.get()) {
                // Make sure colName is updated
                COLUMN_NAME.set(textField.getText());

                String newCol = switch (COLUMN_TYPE.get()) {
                    case POSTFIX -> column + COLUMN_NAME.get();
                    case PREFIX -> COLUMN_NAME.get() + column;
                    case NEW_NAME -> COLUMN_NAME.get();
                };

                var newIdx = owner.getTable().getFreeColumn(newCol);

                if (newIdx == ResultsTable.COLUMN_IN_USE) {
                    if (!IJ.showMessageWithCancel("Column Operator",
                            "Column '%s' already exists, replace it?".formatted(newCol))) {
                        return;
                    }
                }

                destCol = newCol;
            }

            switch (VALUE_SOURCE.get()) {
                case VALUE_SOURCE_CONST_B -> {
                    var b = OPERAND.get();
                    owner.getTable().updateValues(destCol, column, cv -> operator.applyAsDouble(cv, b));
                }
                case VALUE_SOURCE_CONST_E ->
                        owner.getTable().updateValues(destCol, column, cv -> operator.applyAsDouble(cv, Math.E));
                case VALUE_SOURCE_CONST_PI ->
                        owner.getTable().updateValues(destCol, column, cv -> operator.applyAsDouble(cv, Math.PI));
                // Column source
                default -> {
                    owner.getTable().updateValues(destCol, column, VALUE_SOURCE.get(), operator);
                }
            }
        }
    }

    public enum Operator implements NState<Operator>, ToolTipProvider {
        ADD("cv + b", Double::sum),
        MULTIPLY("cv * b", (cv, b) -> cv * b),
        DIVIDE("cv / b", (cv, b) -> cv / b),
        EXPONENTIATE("<html>cv<sup>b</sup></html>", (cv, b) -> {
            if (cv == Math.E) {
                return Math.exp(b);
            } else {
                return Math.pow(cv, b);
            }
        }),
        /**
         * Logarithm
         */
        ANTIEXPONENTIATE("<html>log<sub>b</sub>(cv)<br>Set b = 0 for natural log</html>", (cv, b) -> {
            if (b == 0) {
                return Math.log(cv);
            } else if (b == 10) {
                return Math.log10(cv);
            }
            return Math.log(cv) / Math.log(b);
        }),
        ROOT("<html><sup>b</sup>&radic cv</html>", (cv, b) -> {
            if (b == 2) {
                return Math.sqrt(cv);
            } else if (b == 3) {
                return Math.cbrt(cv);
            }
            return Math.pow(cv, 1D / b);
        }),
        TO_DEGREES("cv (radians) -> degrees", (cv, b) -> Math.toDegrees(cv)),
        TO_RADIANS("cv° -> radians", (cv, b) -> Math.toRadians(cv)),
        SIN("sin(cv [rad])", (cv, b) -> Math.sin(cv)),
        COS("cos(cv [rad])", (cv, b) -> Math.cos(cv)),
        TAN("tan(cv [rad])", (cv, b) -> Math.tan(cv)),
        ARCSIN("arcsin(cv) -> y [rad]", (cv, b) -> Math.asin(cv)),
        ARCCOS("arccos(cv) -> y [rad]", (cv, b) -> Math.acos(cv)),
        ARCTAN("arctan(cv) -> y [rad]", (cv, b) -> Math.atan(cv)),
        ARCTAN2("arctan2(cv, b)", Math::atan2),
        SINH("sinh(cv)", (cv, b) -> Math.sinh(cv)),
        COSH("cosh(cv)", (cv, b) -> Math.cosh(cv)),
        TANH("tanh(cv)", (cv, b) -> Math.tanh(cv)),
        ARSINH("arsinh(cv)", (cv, b) -> Math.log(cv + Math.sqrt(cv*cv + 1))),
        ARCOSH("arcosh(cv)", (cv, b) -> Math.log(cv + Math.sqrt(cv*cv - 1))),
        ARTANH("artanh(cv)", (cv, b) -> 0.5 * Math.log((1+cv)/(1-cv))),
        FACTORIAL("Uses the Gamma function. Gamma(cv+1)", (cv, b) -> Gamma.gamma(cv+1)),
        MIN("min(cv, b)", Math::min),
        MAX("max(cv, b)", Math::max),
        ABS("|cv|", (cv, b) -> Math.abs(cv)),
        QUADRATURE_SUM("<html>&radic (cv<sup>2</sup> + b<sup>2</sup>)</html>", Math::hypot),
        ROUND("<html>round(cv, b)<br>Use b=0 to round to nearest integer, " +
                "otherwise cv is rounded to b decimal places</html>", (cv, b) -> {
            if (b == 0) {
                return Math.rint(cv);
            } else {
                return BigDecimal.valueOf(cv).setScale((int) b, RoundingMode.HALF_EVEN).doubleValue();
            }
        }),
        CEIL("ceil(cv)", (cv, b) -> Math.ceil(cv)),
        FLOOR("floor(cv)", (cv, b) -> Math.floor(cv)),
        MODULO("cv % b", (cv, b) -> cv % b),
        ADD_RANDOM("<html>cv + b * random<br>random is drawn from [0,b), " +
                "which is approximately uniformly distributed</html>", (cv, b) -> b*Math.random() + cv),
        IDENTITY("cv -> cv", (cv, b) -> cv),
        INCREMENT("cv + b * row, where row starts at 0", new IncrementOperator()),
        ;

        final String description;
        /**
         * (columnValue, b) -> newColumnValue
         */
        private final DoubleBinaryOperator operator;

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

        public DoubleBinaryOperator getOperator() {
            if (operator instanceof MutableStateOperator mutableStateOperator) {
                mutableStateOperator.resetMutableState();
            }

            return operator;
        }

        @Override
        public String getToolTip() {
            return description;
        }
    }

    public enum ColumnType implements NState<ColumnType> {
        POSTFIX,
        PREFIX,
        NEW_NAME,
        ;

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public ColumnType[] values0() {
            return ColumnType.values();
        }
    }

    static class IncrementOperator implements MutableStateOperator {
        private int m = 0;

        @Override
        public double applyAsDouble(double cv, double b) {
            return cv + b * m++;
        }

        @Override
        public void resetMutableState() {
            m = 0;
        }
    }

    interface MutableStateOperator extends DoubleBinaryOperator {
        void resetMutableState();
    }
}
