package Astronomy;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.SystemColor;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ij.gui.DialogListener;
import ij.gui.GenericDialog;

/**
 * Range checks for the numeric fields of a {@link GenericDialog}.  While any
 * rule fails, the offending fields are tinted red, the first problem is shown
 * below the fields, and OK / Enter are disabled.
 *
 * <p>Fields are addressed by their index in {@link GenericDialog#getNumericFields()}
 * and checkboxes by their index in {@link GenericDialog#getCheckboxes()}.
 */
final class DialogRangeValidator implements DialogListener {
    private static final Color INVALID_BACKGROUND = new Color(255, 190, 190);

    interface Test {
        boolean ok(double[] values, boolean[] checked);
    }

    private static final class Rule {
        final String message;
        final Test test;
        final int[] fields;

        Rule(String message, Test test, int[] fields) {
            this.message = message;
            this.test = test;
            this.fields = fields;
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private Label messageLabel;

    /**
     * Adds a rule; {@code fields} are highlighted when it fails.  Rules are
     * evaluated in order, and a rule is skipped if any of its fields already
     * failed an earlier rule, so put single-field checks before cross-field ones.
     */
    DialogRangeValidator rule(String message, Test test, int... fields) {
        rules.add(new Rule(message, test, fields));
        return this;
    }

    /** {@code min <= value <= max}. */
    DialogRangeValidator range(int field, String name, double min, double max) {
        return rule(name + " must be between " + fmt(min) + " and " + fmt(max) + ".",
                (v, c) -> Double.isFinite(v[field]) && v[field] >= min && v[field] <= max, field);
    }

    /** {@code min < value < max}. */
    DialogRangeValidator openRange(int field, String name, double min, double max) {
        return rule(name + " must be greater than " + fmt(min) + " and less than " + fmt(max) + ".",
                (v, c) -> Double.isFinite(v[field]) && v[field] > min && v[field] < max, field);
    }

    DialogRangeValidator positive(int field, String name) {
        return rule(name + " must be greater than 0.",
                (v, c) -> Double.isFinite(v[field]) && v[field] > 0, field);
    }

    /** 0 (meaning "automatic") or a positive number. */
    DialogRangeValidator autoOrPositive(int field, String name) {
        return rule(name + " must be 0 (auto) or a positive number.",
                (v, c) -> Double.isFinite(v[field]) && v[field] >= 0, field);
    }

    DialogRangeValidator wholeNumber(int field, String name, int min, int max) {
        return rule(name + " must be a whole number from " + min + " to " + max + ".",
                (v, c) -> v[field] >= min && v[field] <= max && v[field] == Math.rint(v[field]), field);
    }

    /** Adds the message line to the dialog and starts validating; call after all fields are added. */
    void install(GenericDialog gd) {
        gd.addMessage(" ", null, Color.red);
        Component label = gd.getMessage();
        if (label instanceof Label) messageLabel = (Label) label;
        gd.addDialogListener(this);
        // Values restored from preferences may already be out of range.
        gd.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                Button ok = gd.getButtons()[0];
                boolean valid = check(gd) == null;
                if (ok != null) ok.setEnabled(valid);
            }
        });
    }

    /**
     * Validates the current dialog values and returns the first problem, or
     * {@code null} if all rules pass.  Leaves the dialog's read counters reset,
     * so it can be called right after {@code showDialog()}; this also covers
     * macro runs, where the dialog is never shown.
     */
    String check(GenericDialog gd) {
        gd.resetCounters();
        String error = evaluate(gd);
        gd.resetCounters();
        return error;
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, java.awt.AWTEvent e) {
        return evaluate(gd) == null;
    }

    private String evaluate(GenericDialog gd) {
        Vector<?> fields = gd.getNumericFields();
        double[] values = new double[size(fields)];
        for (int i = 0; i < values.length; i++) values[i] = gd.getNextNumber();
        boolean[] checked = new boolean[size(gd.getCheckboxes())];
        for (int i = 0; i < checked.length; i++) checked[i] = gd.getNextBoolean();
        // GenericDialog only records macro options for items read by the first
        // DialogListener, so every remaining item has to be read here as well.
        for (int i = size(gd.getChoices()); i > 0; i--) gd.getNextChoice();
        for (int i = size(gd.getRadioButtonGroups()); i > 0; i--) gd.getNextRadioButton();
        for (int i = size(gd.getStringFields()); i > 0; i--) gd.getNextString();

        boolean[] bad = new boolean[values.length];
        String firstError = null;
        for (Rule r : rules) {
            boolean skip = false;
            for (int f : r.fields) skip |= bad[f];
            if (skip || r.test.ok(values, checked)) continue;
            for (int f : r.fields) bad[f] = true;
            if (firstError == null) firstError = r.message;
        }

        for (int i = 0; i < values.length; i++) {
            Color bg = bad[i] ? INVALID_BACKGROUND : SystemColor.text;
            TextField tf = (TextField) fields.get(i);
            if (!bg.equals(tf.getBackground())) tf.setBackground(bg);
        }
        if (messageLabel != null) messageLabel.setText(firstError == null ? " " : firstError);
        return firstError;
    }

    private static int size(Vector<?> v) {
        return v == null ? 0 : v.size();
    }

    private static String fmt(double d) {
        return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : String.valueOf(d);
    }
}
