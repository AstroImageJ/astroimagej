package Astronomy.multiplot.table.util;

import Astronomy.multiplot.table.MeasurementsWindow;
import astroj.MeasurementTable;
import ij.IJ;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;
import ij.measure.CurveFitter;
import ij.measure.Minimizer;

import javax.swing.*;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CurveFitterHandler {
    private static final Property<String> EQUATION = new Property<>("y = a + b*x + c*x*x", CurveFitterHandler.class);
    private static final Property<String> FIT_TYPE = new Property<>(CurveFitter.fitList[0], CurveFitterHandler.class);
    private static final Property<Boolean> SHOW_SETTINGS = new Property<>(false, CurveFitterHandler.class);
    private static final Executor FIT_THREAD = Executors.newSingleThreadExecutor();


    private final MeasurementsWindow window;

    private CurveFitterHandler(MeasurementsWindow window) {
        this.window = window;
    }

    public static void fitCurve(MeasurementsWindow window) {
        var handler = new CurveFitterHandler(window);

        var columns = handler.dialog();

        if (columns == null) {
            return;
        }

        FIT_THREAD.execute(() -> handler.fitCurve(columns));
    }

    private String[] dialog() {
        var d = new GenericSwingDialog("Curve Fitter", window);

        // Add CUSTOM to list
        var types = Arrays.copyOf(CurveFitter.fitList, CurveFitter.fitList.length+1);
        String customTypeKey = "*Custom*";
        types[types.length-1] = customTypeKey;

        // Ensure type exists
        if (!FIT_TYPE.get().equals(customTypeKey) && CurveFitter.getFitCode(FIT_TYPE.get()) < 0) {
            FIT_TYPE.set(types[0]);
        }

        d.addChoice("Fit Type:", types, FIT_TYPE.get(), FIT_TYPE::set);
        var equationInput = new JTextField("y = a + b*x + c*x*x");
        equationInput.setToolTipText("""
                Press <Enter> to confirm entry.
                """);
        equationInput.addActionListener(l -> EQUATION.set(l.getActionCommand()));
        d.addGenericComponent(equationInput);

        equationInput.setEnabled(FIT_TYPE.get().equals(customTypeKey));
        FIT_TYPE.addListener(($, v) -> equationInput.setEnabled(customTypeKey.equals(v)));

        MeasurementTable table = window.getTable();
        var columns = table.getHeadings();

        // Remove labels column
        if (table.hasRowLabels()) {
            columns = Arrays.copyOfRange(columns, 1, columns.length);
        }

        var dataColumns = new String[2];

        d.addChoice("X Data", columns, columns[0], s -> dataColumns[0] = s);
        d.addChoice("Y Data", columns, columns.length > 1 ? columns[1] : columns[0], s -> dataColumns[1] = s);

        d.addCheckbox("Show settings", SHOW_SETTINGS.get(), SHOW_SETTINGS::set);

        d.enableYesNoCancel();
        d.disableNo();
        d.centerDialog(true);

        d.showDialog();

        return d.wasOKed() ? dataColumns : null;
    }

    private void fitCurve(String[] columns) {
        var x = window.getTable().bulkGetColumnAsDoubles(window.getTable().getColumnIndex(columns[0]));
        var y = window.getTable().bulkGetColumnAsDoubles(window.getTable().getColumnIndex(columns[1]));

        if (x == null || y == null) {
            return;
        }

        var cf = new CurveFitter(x, y);
        cf.setStatusAndEsc("Optimization: Iteration ", true);
        try {
            var fitType = CurveFitter.getFitCode(FIT_TYPE.get());
            if (fitType == -1) {
                if (EQUATION.get() == null || EQUATION.get().isBlank()) {
                    IJ.error("Curve Fitting", "Must have a valid formula");
                    return;
                }

                var paramCount = cf.doCustomFit(EQUATION.get(), null, SHOW_SETTINGS.get());

                if (paramCount == 0) {
                    IJ.beep();
                    IJ.log("Bad formula; should be:\n   y = function(x, a, ...)");
                    return;
                }
            } else {
                cf.doFit(fitType, SHOW_SETTINGS.get());
            }
            if (cf.getStatus() == Minimizer.INITIALIZATION_FAILURE) {
                IJ.beep();
                IJ.showStatus(cf.getStatusString());
                IJ.log("Curve Fitting Error:\n"+cf.getStatusString());
                return;
            }
            if (Double.isNaN(cf.getSumResidualsSqr())) {
                IJ.beep();
                IJ.showStatus("Error: fit yields Not-a-Number");
                return;
            }

        } catch (Exception e) {
            IJ.handleException(e);
            return;
        }

        IJ.log(cf.getResultString());

        var plot = cf.getPlot();

        plot.show();
    }
}
