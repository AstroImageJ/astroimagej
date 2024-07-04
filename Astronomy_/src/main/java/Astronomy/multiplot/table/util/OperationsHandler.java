package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.gui.ToolTipProvider;
import ij.astro.gui.nstate.NState;
import ij.astro.io.prefs.Property;
import ij.astro.util.UIHelper;

import javax.swing.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;

public class OperationsHandler {
    private static final Property<Operator> OPERATOR = new Property<>(Operator.ADD, OperationsHandler.class);
    private static final Property<Double> OPERAND = new Property<>(0D, OperationsHandler.class);
    private static final Property<Boolean> FLIP_INPUTS = new Property<>(false, OperationsHandler.class);
    private static final Property<Boolean> RESULTS_IN_NEW_COLUMN = new Property<>(false, OperationsHandler.class);
    private static final Property<COLUMN_TYPE> COLUMN_TYPE_PROPERTY = new Property<>(COLUMN_TYPE.POSTFIX, OperationsHandler.class);
    private static final Property<String> COLUMN_NAME = new Property<>("_m", OperationsHandler.class);

    public static void dialog(MeasurementsWindow owner, String column) {
        var d = new GenericSwingDialog("Column Operations for " + column, owner);

        d.setOverridePosition(true);
        d.addMessage("Column Value (cv)");
        d.addNStateDropdown(OPERATOR.get(), OPERATOR::set);
        d.addToSameRow();
        d.addUnboundedNumericField("Operand (b):", OPERAND.get(), 1, 10, null, OPERAND::set);
        d.addToSameRow();
        d.addCheckbox("Flip inputs", FLIP_INPUTS.get(), FLIP_INPUTS::set).setToolTipText("Flip Inputs (cv, b) -> (b, cv)");
        d.setOverridePosition(false);

        d.addLineSeparator();

        d.addCheckbox("Put results in new column", RESULTS_IN_NEW_COLUMN.get(), RESULTS_IN_NEW_COLUMN::set);
        d.addToSameRow();
        var typeBox = d.addNStateDropdown(COLUMN_TYPE_PROPERTY.get(), COLUMN_TYPE_PROPERTY::set);
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
            DoubleBinaryOperator baseOp = OPERATOR.get().operator;
            DoubleBinaryOperator operator;
            if (FLIP_INPUTS.get()) {
                operator = (cv, b) -> baseOp.applyAsDouble(b, cv);
            } else {
                operator = baseOp;
            }

            var b = OPERAND.get();

            if (RESULTS_IN_NEW_COLUMN.get()) {
                // Make sure colName is updated
                COLUMN_NAME.set(textField.getText());

                String newCol = switch (COLUMN_TYPE_PROPERTY.get()) {
                    case POSTFIX -> column + COLUMN_NAME.get();
                    case PREFIX -> COLUMN_NAME.get() + column;
                    case NEW_NAME -> COLUMN_NAME.get();
                };

                owner.getTable().generateValues(newCol, column, cv -> operator.applyAsDouble(cv, b));
            } else {
                owner.getTable().updateValues(column, cv -> operator.applyAsDouble(cv, b));
            }
        }
    }

    public enum Operator implements NState<Operator>, ToolTipProvider {
        ADD("cv + b", Double::sum),
        MULTIPLY("cv * b", (cv, b) -> cv * b),
        DIVIDE("cv / b", (cv, b) -> cv / b),
        EXPONENTIATE("<html>cv<sup>b</sup></html>", Math::pow),
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

    public enum COLUMN_TYPE implements NState<COLUMN_TYPE> {
        POSTFIX,
        PREFIX,
        NEW_NAME,
        ;

        @Override
        public boolean isOn() {
            return false;
        }

        @Override
        public COLUMN_TYPE[] values0() {
            return COLUMN_TYPE.values();
        }
    }
}
