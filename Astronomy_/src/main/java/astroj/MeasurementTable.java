// MeasurementTable.java

package astroj;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;


/**
 * Like an ImageJ ResultsTable but with more functionality.
 *
 * @author F.V. Hessman, Georg-August-Universitaet Goettingen
 * @version 1.4
 * @date 2006-Sep-11
 */
public class MeasurementTable extends ResultsTable {
    public static String PREFIX = "Measurements";
    public static String RESULTS = "Results";
    public static int DEFAULT_DECIMALS = 6;
    protected String shortName = null;
    protected boolean locked = false;
    protected String filePath = "";
    private final HashSet<Runnable> listeners = new HashSet<>();
    private KeyAdapter gotoListener;

    /**
     * Creates an empty default MeasurementTable.
     */
    public MeasurementTable() {
        setPrecision(DEFAULT_DECIMALS);
        shortName = null;
    }

    /**
     * Creates an empty MeasurementTable with a given long name.
     */
    public MeasurementTable(String tableName) {
        setPrecision(DEFAULT_DECIMALS);
        shortName = MeasurementTable.shorterName(tableName);
    }

    /**
     * Creates a MeasurementTable object from a TextWindow.
     */
    public MeasurementTable(TextWindow w) {
        super();
        setPrecision(DEFAULT_DECIMALS);
        TextPanel panel = w.getTextPanel();
        shortName = MeasurementTable.shorterName(w.getTitle());
        int count = panel.getLineCount();
        // IJ.showMessage(panel.getText());
    }

    /**
     * Transfers the last entry from a MeasurementTable to the standard Results table.
     */
    public static void transferLastRow(String tableName) {
        MeasurementTable t = new MeasurementTable(tableName);
        ResultsTable r = ResultsTable.getResultsTable();
        if (t == null || r == null) {
            IJ.error("Unable to read measurement table or Results table!");
            return;
        }
        int row = t.getCounter();
        int ncols = t.getLastColumn();
        IJ.log("row=" + row + ", ncols=" + ncols);
        for (int col = 0; col < ncols; col++) {
            String heading = t.getColumnHeading(col);
            double val = t.getValueAsDouble(col, row);
            IJ.log(heading + " " + val);
            r.addValue(heading, val);
        }
    }

    /**
     * Class method for extracting a TextPanel from a MeasurementTable with a given name.
     */
    public static TextPanel getTextPanel(String tableName) {
        Frame frame = WindowManager.getFrame(tableName);
        if (frame == null || !(frame instanceof TextWindow win)) {
            // IJ.error ("Unable to access "+tableName+"!");
            return null;
        } else {
            return win.getTextPanel();
        }
    }

    /**
     * Indicates whether a MeasurementTable exists with the given name.
     */
    public static boolean exists(String tableName) {
        TextPanel tp = getTextPanel(tableName);
        TextPanel tp2 = getTextPanel(MeasurementTable.shorterName(tableName));
        return tp != null || tp2 != null;
    }

    /**
     * Returns a MeasurementTable reconstructed from a text file produced by ImageJ from a MeasurementTable/ResultsTable.
     */
    public static MeasurementTable getTableFromFile(String filename) {
        BufferedReader in = null;
        MeasurementTable table = null;

        try {
            in = new BufferedReader(new FileReader(filename));
            File file = new File(filename);

            // READ HEADER LINE

            String line = in.readLine();
            if (line == null) {
                IJ.error("MeasurementTable: cannot read header line!");
                in.close();
                return null;
            }

            // OPEN NEW MeasurementTable
            String previousLine = null;
            table = new MeasurementTable(file.getName().equals("") ?
                    "Measurements" :
                    file.getName());    // SHOULD USE filename); BUT CAN'T BECAUSE OF PRIVATE WindowManager.getNonImageWindows() METHOD

            table.filePath = filename;
            String delimiter = "\t";
            if (filename.endsWith(".csv")) {
                delimiter = ",";
            } else if (filename.endsWith(".prn") || filename.endsWith(".spc")) {
                delimiter = " +";
            } else if (filename.endsWith(".txt")) {
                delimiter = "\\s+";  //whitespace delimiter
            }

            while (line != null && line.startsWith("#")) { //discard leading comments line, except last one which may be column headers
                previousLine = line;
                line = in.readLine();
            }


            String[] labels = line.split(delimiter);
            int n = labels.length;
            if (n < 2 || (n == 2 && labels[0].trim().equals("") && !labels[1].trim().equals(""))) {
                delimiter = "\\s+";
                labels = line.split(delimiter);
                n = labels.length;
            }
            String[] labels1 = labels.clone();

            int row = 0;
            boolean hasLabels = false;
            boolean hasImageLabel = false;
            int shift = 0;

            if (n > 1 && Double.isNaN(Tools.parseDouble(labels[0])) && Double.isNaN(Tools.parseDouble(labels[1]))) {
                hasLabels = true;    //the last line read has good column headers
                previousLine = null;  //forget the previous comment line, if it exists
            } else if (previousLine != null) { //the last line read has no column headers, so try previous comment line, if it exists
                while (previousLine.startsWith("#")) {
                    previousLine = previousLine.substring(1);
                }
                labels = previousLine.split(delimiter);
                n = labels.length;
                if (n > 1 && Double.isNaN(Tools.parseDouble(labels[0])) && Double.isNaN(Tools.parseDouble(labels[1]))) {
                    hasLabels = true;    //the last line read has good labels
                }
            }

            if (!hasLabels && n == 1 && Double.isNaN(Tools.parseDouble(labels1[0]))) {
                labels = labels1;
                shift = 2;
                hasImageLabel = false;
                hasLabels = true;
            } else if (hasLabels && n > 0 && (labels[0].equalsIgnoreCase("Label") || labels[0].equalsIgnoreCase("image"))) {
                shift = 1;
                hasImageLabel = true;
            } else if (hasLabels && n > 1 && (labels[1].equalsIgnoreCase("Label") || labels[1].equalsIgnoreCase("image"))) {
                shift = 0;
                hasImageLabel = true;
            } else if (hasLabels && n > 0 && (labels[0].trim().equals("") || labels[0].trim().equals("#"))) {
                shift = 1;
            } else if (!hasLabels && n > 1 && Double.isNaN(Tools.parseDouble(labels1[1]))) {
                labels = labels1;
                shift = 0;
                hasImageLabel = true;
            } else if (!hasLabels && n > 0 && Double.isNaN(Tools.parseDouble(labels1[0]))) {
                labels = labels1;
                shift = 1;
                hasImageLabel = true;
            } else {
                shift = 2;
            }

            String[] header = new String[n + shift];
            int h = header.length;
            header[0] = "";
            header[1] = "Label";
            if (hasImageLabel && hasLabels) { //input table has standard MeasurementsTable headings and row labels, use headings as they are
                System.arraycopy(labels, 1 - shift, header, 1, h - 1);
            } else if (hasImageLabel && !hasLabels) { //input table has no headers, but has row lables, make generic headers and store line of data
                table.incrementCounter();
                row++;
                table.addLabel(shift == 0 ? labels[1] : labels[0]);
                for (int i = 2; i < h; i++) {
                    header[i] = "Col_" + (i - 1);
                    table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                }
            } else if (!hasImageLabel && hasLabels) { //input table has generic headers and no row labels, use standard header prefix and add headers
                System.arraycopy(labels, 2 - shift, header, 2, h - 2);
            } else { // input table has no headers and no row labels, make generic headers and store line of data
                table.incrementCounter();
                row++;
                table.addLabel(header[1], "Row_1");
                for (int i = 2; i < h; i++) {
                    header[i] = "Col_" + (i - 1);
                    table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                }
            }

            double d = 0.0;

            line = previousLine == null ?
                    in.readLine() :
                    line;  //get a new line if the last line read was used as column headers

            while (line != null) {
                if (!line.startsWith("#") && (line.trim().length() > 0)) {
                    table.incrementCounter();
                    String[] words = line.split(delimiter);
                    if (shift == 0) {
                        table.addLabel(header[1], words[1]);
                    } else if (shift == 1) {
                        table.addLabel(header[1], hasImageLabel ? words[0] : "Row_" + (row + 1));
                    } else if (shift == 2) {
                        table.addLabel(header[1], "Row_" + (row + 1));
                    }

                    for (int col = (2 - shift); col < h - shift; col++) {
                        if (col >= words.length) {
                            d = Double.NaN;
                        } else if (words[col] == null) {
                            d = Double.NaN;
                        } else if (words[col].trim().equals("") || words[col].trim().equals("-")) {
                            d = Double.NaN;
                        } else if (isHMS(words[col])) {
                            d = hms(words[col]);
                        } else {
                            d = Tools.parseDouble(words[col]);
                        }
                        table.addValue(header[col + shift], d);
                    }
                    row++;
                }
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            System.err.println("MeasurementTable IO: " + e.getMessage());
            IJ.error("MeasurementTable: " + e.getMessage());
            table = null;
        } catch (NumberFormatException nfe) {
            System.err.println("MeasurementTable Number Format: " + nfe.getMessage());
            IJ.error("MeasurementTable: " + nfe.getMessage());
            table = null;
        }
        try {
            in.close();
        } catch (Exception exc) {
        }
        return table;
    }

    /**
     * Returns an existing MeasurementTable reconstructed from the TextWindow with the appropriate name.
     */
    public static MeasurementTable getTable(String tableName) {
        return getTable(tableName, tableName);
    }

    /**
     * Returns an existing MeasurementTable reconstructed from the source TextWindow or TextArea with the specified destination table name.
     */
    public static MeasurementTable getTable(String tableNameDestination, String tableNameSource) {
        int n;
        boolean goodPanel = false;
        int nextLine = 0;
        String inputPanelName = "";
        String delimiter = "\t";
        String line = "";
        String previousLine = null;
        String[] lines = null;

        String destName = MeasurementTable.longerName(tableNameDestination);
        String sourceName = MeasurementTable.longerName(tableNameSource);

        // CREATE EMPTY TABLE

        MeasurementTable table = new MeasurementTable(destName);

        // GET CONTENTS OF EXISTING DATA

        inputPanelName = sourceName;
        TextPanel panel = getTextPanel(inputPanelName);
        if (panel == null) {
            inputPanelName = MeasurementTable.shorterName(sourceName);
            panel = getTextPanel(inputPanelName);
        }
        if (panel != null) {
            if (panel.getResultsTable() instanceof MeasurementTable m) {
                return m;
            }
            lines = panel.getText().split("\n");
            goodPanel = true;
        } else {
            inputPanelName = sourceName;
            TextArea textArea = null;
            Frame inframe = WindowManager.getFrame(inputPanelName);
            if (inframe != null) {
                textArea = (TextArea) inframe.getComponent(0);
            }
            if (textArea == null) {
                inputPanelName = MeasurementTable.shorterName(sourceName);
                inframe = WindowManager.getFrame(inputPanelName);
                if (inframe != null) {
                    textArea = (TextArea) inframe.getComponent(0);
                }
            }
            if (textArea == null) {
                return table;
            }
            lines = textArea.getText().split("\n");
        }

        if (lines.length < 1 || lines[0] == null || lines[0].equals("")) {
            //IJ.showMessage ("Error: no lines to process in "+sourceName);
            return table;
        }
        //IJ.log(""+(lines[0].equals("\t")?"first line is single tab":"lines[0]="+lines[0]));
        if (goodPanel) {
            delimiter = "\t";
        } else if (sourceName.endsWith(".csv")) {
            delimiter = ",";
        } else if (sourceName.endsWith(".prn") || sourceName.endsWith(".spc")) {
            delimiter = " +";
        }
        for (int i = 0; i < lines.length; i++) { //discard leading comments line, except last one which may be column headers
            nextLine++;
            if (i > 0) {
                previousLine = line;
            }
            line = lines[i];
            if (!line.startsWith("#")) {
                break;
            }
        }
        if (nextLine <= lines.length) {
            String[] labels = line.split(delimiter);
            String[] labels1 = line.split(delimiter);
            n = labels.length;
            if (n < 2) {
                delimiter = "\\s+";
                labels = line.split(delimiter);
                labels1 = line.split(delimiter);
                n = labels.length;
            }
            int row = 0;
            boolean hasLabels = false;
            boolean hasImageLabel = false;
            int shift = 0;

            if (n > 1 && Double.isNaN(Tools.parseDouble(labels[0])) && Double.isNaN(Tools.parseDouble(labels[1]))) {
                hasLabels = true;    //the last line read has good column headers
                previousLine = null;  //forget the previous comment line, if it exists
            } else if (previousLine != null) { //the last line read has no column headers, so try previous comment line, if it exists
                while (previousLine.startsWith("#")) {
                    previousLine = previousLine.substring(1);
                }
                labels = previousLine.split(delimiter);
                n = labels.length;
                if (n > 1 && Double.isNaN(Tools.parseDouble(labels[0])) && Double.isNaN(Tools.parseDouble(labels[1]))) {
                    hasLabels = true;    //the last line read has good labels
                }
            }

            if (!hasLabels && n == 1 && Double.isNaN(Tools.parseDouble(labels1[0]))) {
                labels = labels1;
                shift = 2;
                hasImageLabel = false;
                hasLabels = true;
            } else if (hasLabels && n > 0 && (labels[0].equalsIgnoreCase("Label") || labels[0].equalsIgnoreCase("image"))) {
                shift = 1;
                hasImageLabel = true;
            } else if (hasLabels && n > 1 && (labels[1].equalsIgnoreCase("Label") || labels[1].equalsIgnoreCase("image"))) {
                shift = 0;
                hasImageLabel = true;
            } else if (hasLabels && n > 0 && (labels[0].trim().equals("") || labels[0].trim().equals("#"))) {
                shift = 1;
            } else if (!hasLabels && n > 1 && Double.isNaN(Tools.parseDouble(labels1[1]))) {
                labels = labels1;
                shift = 0;
                hasImageLabel = true;
            } else if (!hasLabels && n > 0 && Double.isNaN(Tools.parseDouble(labels1[0]))) {
                labels = labels1;
                shift = 1;
                hasImageLabel = true;
            } else {
                shift = 2;
            }

            String[] header = new String[(n + shift < 2 ? 2 : n + shift)];
            int h = header.length;
            header[0] = "";
            header[1] = "Label";
            if (hasImageLabel && hasLabels) { //input table has standard MeasurementsTable headings and row labels, use headings as they are
                System.arraycopy(labels, 1 - shift, header, 1, h - 1);
            } else if (hasImageLabel && !hasLabels) { //input table has no headers, but has row lables, make generic headers and store line of data
                table.incrementCounter();
                row++;
                //                table.addValue(1, 1);
                table.addLabel(shift == 0 ? labels[1] : labels[0]);
                for (int i = 2; i < h; i++) {
                    header[i] = "Col_" + (i - 1);
                    table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                }
            } else if (!hasImageLabel && hasLabels) { //input table has generic headers and no row labels, use standard header prefix and add headers
                System.arraycopy(labels, 2 - shift, header, 2, h - 2);
            } else if (!line.trim().equals("")) { //input table is not empty but has no headers or row labels, make generic headers and store line of data
                table.incrementCounter();
                row++;
                //                table.addValue(1, 1);
                table.setValue(header[1], row, "Row_1");
                for (int i = 2; i < h; i++) {
                    header[i] = "Col_" + (i - 1);
                    table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                }
            }

            double d = 0.0;

            if (nextLine < lines.length) {
                for (int i = nextLine; i < lines.length; i++) {
                    line = lines[i];
                    if (!line.startsWith("#") && (line.trim().length() > 0)) {
                        table.incrementCounter();
                        String[] words = line.split(delimiter);
                        if (shift == 0) {
                            table.addLabel(header[1], words[1]);
                        } else if (shift == 1) {
                            table.addLabel(header[1], hasImageLabel ? words[0] : "Row_" + (row + 1));
                        } else if (shift == 2) {
                            table.addLabel(header[1], "Row_" + (row + 1));
                        }

                        for (int col = (2 - shift); col < h - shift; col++) {
                            if (col >= words.length) {
                                d = Double.NaN;
                            } else if (words[col] == null) {
                                d = Double.NaN;
                            } else if (words[col].trim().equals("") || words[col].trim().equals("-")) {
                                d = Double.NaN;
                            } else if (isHMS(words[col])) {
                                d = hms(words[col]);
                            } else {
                                d = Tools.parseDouble(words[col]);
                            }
                            table.addValue(header[col + shift], d);
                        }
                        row++;
                    }
                    nextLine++;
                }
            } else {
                table.incrementCounter();
                //noinspection deprecation
                table.addLabel(header[1], "Dummy_Row");
                for (int col = (2); col < h; col++) {
                    table.addValue(header[col], 0.0);
                }
                table.deleteRow(0);

            }

        }

        if (!goodPanel) {
            Frame frame = WindowManager.getFrame(inputPanelName);
            WindowManager.removeWindow(frame);
            frame.dispose();
            table.show();
        }
        //table.show();

        return table;
    }

    /**
     * Returns the full name of a MeasurementTable, including the standard prefix if not already given.
     */
    public static String longerName(String name) {
        if (name == null) {
            return MeasurementTable.PREFIX;
        } else if (name.equals(MeasurementTable.RESULTS)) {
            return name;
        } else if (name.equals(MeasurementTable.PREFIX)) {
            return name;
        } else if (name.startsWith(MeasurementTable.PREFIX + " in ")) {
            return name;
        } else if (name.indexOf(".txt") >= 0) {
            return name;
        } else {
            return MeasurementTable.PREFIX + " in " + name;
        }
    }

    /**
     * Returns the part of the MeasurementTable name without the standard prefix.
     */
    public static String shorterName(String tableName) {
        if (tableName == null) {
            return null;
        } else if (tableName.equals(MeasurementTable.PREFIX)) {
            return tableName;
        } else if (tableName.equals(MeasurementTable.RESULTS)) {
            return tableName;
        } else if (tableName.startsWith(MeasurementTable.PREFIX + " in ")) {
            return tableName.substring(tableName.indexOf(" in ") + 4);
        } else {
            return tableName;
        }
    }

    /**
     * This method desperately attempts to replace the private functionality of WindowManager.getNonImageWindows() (before ImageJ Version 1.38q)
     */
    public static String[] getMeasurementTableNames() {
        Vector frames = new Vector();

        // FIND MEASUREMENT TABLES ASSOCIATED WITH IMAGES

        if (IJ.versionLessThan("1.40")) {
            Frame std = WindowManager.getFrame("Results");
            if (std != null) {
                frames.addElement("Results");
            }
            std = WindowManager.getFrame(PREFIX);
            if (std != null) {
                frames.addElement(PREFIX);
            }
            String[] openImages = IJU.listOfOpenImages(null);
            if (openImages != null && openImages.length > 0) {
                for (int i = 0; i < openImages.length; i++) {
                    String longName = longerName(openImages[i]);
                    if (WindowManager.getFrame(longName) != null) {
                        frames.addElement(longName);
                    }
                }
            }
        } else {
            Frame[] windows = WindowManager.getNonImageWindows();
            if (windows != null && windows.length > 0) {
                Frame std = WindowManager.getFrame("Results");
                if (std != null) {
                    frames.addElement("Results");
                }
                for (int i = 0; i < windows.length; i++) {
                    String title = windows[i].getTitle();
                    // if (title.startsWith("Measurement"))
                    frames.addElement(title);
                }
            }
        }

        // ANYTHING AT ALL?

        int n = frames.size();
        if (n == 0) {
            return null;
        }

        // THEN GATHER INTO STRING ARRAY

        String[] result = new String[n];
        for (int i = 0; i < n; i++) {
            result[i] = (String) frames.elementAt(i);
        }
        return result;
    }

    /**
     * Checks to see if a string contains a hh:mm:ss.sss representation of an angle/time.
     */
    static boolean isHMS(String s) {
        String[] arr = s.split(":");
        return arr.length > 1;
    }

    /**
     * Converts hh:mm:ss.sss to a number.
     */
    static double hms(String s) {
        double[] d = new double[]{0.0, 0.0, 0.0};
        String[] arr = s.split(":");
        int n = (arr.length > 3) ? 3 : arr.length;
        double sgn = 1.0;
        for (int i = 0; i < n; i++) {
            d[i] = Double.parseDouble(arr[i]);
            if (arr[i].trim().startsWith("-")) {
                sgn = -1.0;
            }
        }
        return sgn * (Math.abs(d[0]) + Math.abs(d[1]) / 60.0 + Math.abs(d[2]) / 3600.0);
    }

    public void setLock(boolean lockState) {
        locked = lockState;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * Returns a MeasurementTable with the path pathname.  If already exists, the table in reconstructed.
     */
//	public static MeasurementTable readTable (String pathname)
//		{
//
//		MeasurementTable table = new MeasurementTable (pathname);
//
//		try	{
//			int column=-1;
//			int row = -1;
//			FileInputStream stream = new FileInputStream(pathname);
//			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//
//			StreamTokenizer parser = new StreamTokenizer(reader);
//			parser.eolIsSignificant(true);
//			parser.whitespaceChars((int)'\t',(int)'\t') ;     // JUST HTABS
//			parser.wordChars((int)'!',(int)'~');
//
//			int ttype=0;
//			while (parser.nextToken() != StreamTokenizer.TT_EOF)
//				{
//				ttype = parser.ttype;
//				if (ttype == StreamTokenizer.TT_EOL)
//					{
//					column=-1;
//					row++;
//					}
//				else if (row == -1)
//					{
//					if ( ttype == StreamTokenizer.TT_WORD)
//						table.setHeading(++column, parser.sval);
//					// IJ.showMessage("row="+row+", column="+column+", label="+parser.sval);
//					}
//				else if (ttype == StreamTokenizer.TT_NUMBER)
//					{
//					if (column == -1)
//						table.incrementCounter();
//					else
//						table.setValue(column,row,parser.nval);
//					column++;
//					}
//				}
//			reader.close();
//			stream.close();
//			}
//		catch (FileNotFoundException e)
//			{
//			IJ.error("Error: "+e.getMessage());
//			}
//		catch (IOException e)
//			{
//			IJ.error("Error: "+e.getMessage());
//			}
//		return table;
//		}

    /**
     * Stores a number using a temporary different number of decimal places.
     */
    public void addValue(String column, double value, int places) {
        setPrecision(16);
        super.addValue(column, value);
        // setPrecision (DEFAULT_DECIMALS);
        // PRESENT ResultsTable DOESN'T KEEP TRACK OF INDIVIDUAL PRECISIONS!!!
    }

    /**
     * Displays/Refreshes a MeasurementTable with long name.
     */
    public void show() {
        super.show(MeasurementTable.longerName(shortName)/*, false*/);
        if (window != null && window.get() != null) {
            window.get().getTextPanel().removeKeyListener(getOrCreateGotoListener()); // show is called multiple times
            window.get().getTextPanel().addKeyListener(getOrCreateGotoListener());
        }
    }

    private KeyAdapter getOrCreateGotoListener() {
        if (gotoListener == null) {
            gotoListener = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    e.consume();
                    int modifiers = e.getModifiersEx();
                    boolean isCtrlPressed = true;//(modifiers & KeyEvent.CTRL_DOWN_MASK) != 0;
                    boolean isCommandPressed = (modifiers & KeyEvent.META_DOWN_MASK) != 0;
                    //Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                    boolean isF3Pressed = e.getKeyCode() == KeyEvent.VK_F3;

                    if ((isCtrlPressed && isF3Pressed) || (isCommandPressed && isF3Pressed)) {
                        // Create the dropdown menu with column headings
                        JComboBox<String> columnDropdown = new JComboBox<>(MeasurementTable.this.getHeadings());

                        // Display the dialog with the dropdown menu and input field for row
                        JPanel panel = new JPanel(new GridLayout(2, 2));
                        panel.add(new JLabel("Column:"));
                        panel.add(columnDropdown);
                        panel.add(new JLabel("Row:"));
                        JTextField rowField = new JTextField("0");
                        panel.add(rowField);

                        int result = JOptionPane.showConfirmDialog(MeasurementTable.this.window.get(), panel, "Go To Cell", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            // Retrieve the selected column index and row input
                            int column = columnDropdown.getSelectedIndex();
                            String rowInput = rowField.getText();

                            if (!rowInput.isEmpty()) {
                                int row = Integer.parseInt(rowInput.trim());

                                if (row >= 0 && row < MeasurementTable.this.size() && column >= 0 && column < MeasurementTable.this.getLastColumn()) {
                                    if (MeasurementTable.this.window != null && MeasurementTable.this.window.get() != null) {
                                        MeasurementTable.this.window.get().getTextPanel().showCell(row, (String) columnDropdown.getSelectedItem());
                                    }
                                } else {
                                    IJ.beep();
                                    JOptionPane.showMessageDialog(null, "Invalid cell coordinates.");
                                }
                            }
                        }
                    }
                }
            };
        }

        return gotoListener;
    }

    /**
     * ResultTable method to be overridden: a ResultTable's shortTitle is a MeasurementTable's longName.
     */
    public String shortTitle() {
        return MeasurementTable.longerName(shortName);
    }

    /**
     * Returns the ImagePlus from which the measurements in the MeasurementTable
     * were obtained.  The image must still be displayed for the WindowManager to find it.
     */
    public ImagePlus getImage() {
        if (shortName == null) {
            return null;
        }
        int[] ids = WindowManager.getIDList();
        int n = WindowManager.getWindowCount();
        for (int i = 0; i < n; i++) {
            ImagePlus img = WindowManager.getImage(ids[i]);
            if (shortName.equals(img.getTitle())) { // getShortTitle()?
                return img;
            }
        }
        return null;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Retrieves a double array column.
     */
    public double[] getDoubleColumn(int col) {
        int cntr = this.getCounter();
        double[] d = new double[cntr];
        for (int r = 0; r < cntr; r++) {
            double dd = this.getValueAsDouble(col, r);
            // IJ.log("arr["+col+"]["+r+"]="+dd);
            d[r] = dd;
        }
        /* OLD VERSION DOESN'T WORK IF TABLE CONTAINS NaN!!!
                float[] f = this.getColumn(col);
                if (f == null) return null;

                double[] d = new double[f.length];
                for (int i=0; i < f.length; i++)
                    d[i] = f[i];
        */
        return d;
    }

    /**
     * Inserts a double array column.
     */
    public boolean putDoubleColumn(String title, double[] arr) {
        int col = getFreeColumn(title);
        if (col == COLUMN_IN_USE || col == TABLE_FULL) {
            return false;
        }

        for (int i = 0; i < arr.length; i++) {
            setValue(col, i, arr[i]);
        }
        return true;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        listeners.forEach(Runnable::run);
    }

    public synchronized void addListener(Runnable r) {
        listeners.add(r);
    }

    public synchronized void removeListener(Runnable r) {
        listeners.remove(r);
    }

    public synchronized void removeListeners() {
        listeners.clear();
    }
}
