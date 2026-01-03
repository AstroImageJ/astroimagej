package Astronomy.multiplot.table.util;

import java.util.Objects;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import Astronomy.multiplot.table.MeasurementsWindow;
import ij.astro.util.UIHelper;

public class FindHandler extends FilterHandler {
    private TreeSet<Coordinate> matches = new TreeSet<>();
    //private final TableModelListener listener = _ -> buildMatchingCells(null, window.getJTable());
    private JTextField count;

    public FindHandler(MeasurementsWindow window) {
        super(window);
        setTitle("Find...");

        /*window.getJTable().getModel().addTableModelListener(listener);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                window.getJTable().getModel().removeTableModelListener(listener);
            }
        });*/
        setIconImage(UIHelper.createImage("Astronomy/images/icons/table/find.png"));

        pack();
    }

    @Override
    protected void addComponents() {
        var panel = new JPanel();
        count = new JTextField("0/0");
        count.setEditable(false);
        count.setHorizontalAlignment(JTextField.RIGHT);
        count.setColumns(8);
        var input = new JTextField(30);
        input.addActionListener(_ -> {
            var next = matches.lower(new Coordinate(window.getJTable().getSelectedRow(), window.getJTable().getSelectedColumn()));
            if (next != null) {
                window.getJTable().changeSelection(next.row, next.col, false, false);
                count.setText("%s/%s".formatted(matches.tailSet(next).size(), matches.size()));
                panel.repaint();
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                buildMatchingCells(input.getText(), window.getJTable());
                count.setText("%s/%s".formatted(0, matches.size()));
            }
        });
        panel.add(input);

        var up = new JButton("˄");
        up.addActionListener(_ -> {
            var next = matches.higher(new Coordinate(window.getJTable().getSelectedRow(), window.getJTable().getSelectedColumn()));
            if (next != null) {
                window.getJTable().changeSelection(next.row, next.col, false, false);
                count.setText("%s/%s".formatted(matches.tailSet(next).size(), matches.size()));
            }
            panel.revalidate();
            panel.repaint();
        });
        panel.add(up);

        var down = new JButton("˅");
        down.addActionListener(_ -> {
            var next = matches.lower(new Coordinate(window.getJTable().getSelectedRow(), window.getJTable().getSelectedColumn()));
            if (next != null) {
                window.getJTable().changeSelection(next.row, next.col, false, false);
                count.setText("%s/%s".formatted(matches.tailSet(next).size(), matches.size()));
            }
            panel.revalidate();
            panel.repaint();
        });
        panel.add(down);

        panel.add(count);

        add(panel);
    }

    private void buildMatchingCells(String filter, JTable table) {
        if (filter == null) {
            count.setText("N/A");
            matches.clear();
            return;
        }
        matches = new TreeSet<>();
        for (int col = 0; col < table.getColumnCount(); col++) {
            for (int row = 0; row < table.getRowCount(); row++) {
                //todo regex mode, compile pattern
                if (String.valueOf(table.getValueAt(row, col)).contains(filter)) {
                    matches.add(new Coordinate(row, col));
                }
            }
        }
    }

    private final class Coordinate implements Comparable<Coordinate> {
        private final int row;
        private final int col;

        private Coordinate(int row, int col) {
            this.row = row;
            this.col = col;
        }

        private int distanceInTable() {
            return row * window.getJTable().getColumnCount() + col;
        }

        public int distanceTo(Coordinate c) {
            return c.distanceInTable() - distanceInTable();
        }

        public int row() {
            return row;
        }

        public int col() {
            return col;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Coordinate) obj;
            return this.row == that.row &&
                    this.col == that.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }

        @Override
        public String toString() {
            return "Coordinate[" +
                    "row=" + row + ", " +
                    "col=" + col + ']';
        }

        @Override
        public int compareTo(Coordinate o) {
            return distanceTo(o);
        }
    }
}
