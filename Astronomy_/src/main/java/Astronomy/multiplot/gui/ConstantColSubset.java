package Astronomy.multiplot.gui;

import Astronomy.MultiPlot_;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.prefs.Property;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ConstantColSubset {
    private static final Property<ConstantCol> ENTRY_1 = new Property<>(new ConstantCol(), ConstantCol::serialize, ConstantCol::deserialize, ConstantColSubset.class);
    private static final Property<ConstantCol> ENTRY_2 = new Property<>(new ConstantCol(), ConstantCol::serialize, ConstantCol::deserialize, ConstantColSubset.class);
    private static final Property<ConstantCol> ENTRY_3 = new Property<>(new ConstantCol(), ConstantCol::serialize, ConstantCol::deserialize, ConstantColSubset.class);
    private static final Property<ConstantCol> ENTRY_4 = new Property<>(new ConstantCol(), ConstantCol::serialize, ConstantCol::deserialize, ConstantColSubset.class);
    private static final Property<ConstantCol> ENTRY_5 = new Property<>(new ConstantCol(), ConstantCol::serialize, ConstantCol::deserialize, ConstantColSubset.class);

    public static List<ConstantCol> constantCols() {
        return allProps().stream().map(Property::get).filter(Predicate.not(ConstantCol::blank)).toList();
    }

    public static boolean constantCol(String name) {
        return allProps().stream().map(Property::get).map(ConstantCol::name).anyMatch(name::equals);
    }

    private static List<Property<ConstantCol>> allProps() {
        var o = new ArrayList<Property<ConstantCol>>();

        o.add(ENTRY_1);
        o.add(ENTRY_2);
        o.add(ENTRY_3);
        o.add(ENTRY_4);
        o.add(ENTRY_5);

        return o;
    }

    public static void dialog(String path) {
        var gd = new GenericSwingDialog("Constant Columns");

        var name = Box.createVerticalBox();
        var val = Box.createVerticalBox();

        var r1 = Box.createHorizontalBox();
        r1.add(Box.createHorizontalGlue());
        r1.add(new JLabel("Column Name"));
        r1.add(Box.createHorizontalGlue());
        name.add(r1);

        var r2 = Box.createHorizontalBox();
        r2.add(Box.createHorizontalGlue());
        r2.add(new JLabel("Column Value"));
        r2.add(Box.createHorizontalGlue());
        val.add(r2);

        for (Property<ConstantCol> prop : allProps()) {
            var nr = Box.createHorizontalBox();
            nr.add(Box.createHorizontalGlue());
            nr.add(makeEntry(prop, true));
            nr.add(Box.createHorizontalGlue());
            name.add(nr);

            var vr = Box.createHorizontalBox();
            vr.add(Box.createHorizontalGlue());
            vr.add(makeEntry(prop, false));
            vr.add(Box.createHorizontalGlue());
            val.add(vr);
        }

        var b = Box.createHorizontalBox();
        b.add(name);
        b.add(val);

        gd.addGenericComponent(b);

        gd.centerDialog(true);
        gd.showDialog();

        MultiPlot_.saveDataSubsetDialog(path);
    }

    private static JTextField makeEntry(Property<ConstantCol> property, boolean isName) {
        var tf = new JTextField(10);
        tf.setText(isName ? property.get().name : property.get().val);

        tf.addActionListener($ -> {
            if (isName) {
                property.set(new ConstantCol(tf.getText(), property.get().val));
            } else {
                property.set(new ConstantCol(property.get().name, tf.getText()));
            }
        });

        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                impl();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                impl();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                impl();
            }

            private void impl() {
                if (isName) {
                    property.set(new ConstantCol(tf.getText(), property.get().val));
                } else {
                    property.set(new ConstantCol(property.get().name, tf.getText()));
                }
            }
        });

        return tf;
    }

    public record ConstantCol(String name, String val) {
        private static final Pattern EXTRACTOR = Pattern.compile("\\{\\{(?<NAME>.*)}=\\{(?<VAL>.*)}}");
        public ConstantCol() {
            this("", "");
        }

        public boolean blank() {
            return name.isBlank() && val.isBlank();
        }

        public static String serialize(ConstantCol col) {
            return "{{%s}={%s}}".formatted(col.name, col.val);
        }

        public static ConstantCol deserialize(String s) {
            var m = EXTRACTOR.matcher(s);
            if (m.matches()) {
                try {
                    return new ConstantCol(m.group("NAME"), m.group("VAL"));
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ConstantCol();
                }
            }

            return new ConstantCol();
        }
    }
}
