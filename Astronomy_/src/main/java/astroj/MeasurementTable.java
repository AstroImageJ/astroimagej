// MeasurementTable.java

package astroj;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.table.MeasurementsWindow;
import Astronomy.multiplot.table.util.UpdateEvent;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.util.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;


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
    protected boolean dataChanged = false;
    protected String filePath = "";
    private final HashSet<Runnable> listeners = new HashSet<>();
    private static final Map<String, MeasurementTable> INSTANCES = new WeakHashMap<>();
    private MeasurementsWindow window;

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
     * Class method for extracting a MeasurementsWindow from a MeasurementTable with a given name.
     */
    public static MeasurementsWindow getMeasurementsWindow(String tableName) {
        var t = INSTANCES.get(tableName);
        if (t != null) {
            return t.window;
        }

        t = INSTANCES.get(shorterName(tableName));
        if (t != null) {
            return t.window;
        }

        t = INSTANCES.get(longerName(tableName));
        if (t != null) {
            return t.window;
        }

        return null;
    }

    public void clearTable() {
        reset();
        updateView(UpdateEvent.REBUILD);
    }

    /**
     * Indicates whether a MeasurementTable exists with the given name.
     */
    public static boolean exists(String tableName) {
        var tp = getMeasurementsWindow(tableName);
        return tp != null;
    }

    /**
     * Returns a MeasurementTable reconstructed from a text file produced by ImageJ from a MeasurementTable/ResultsTable.
     */
    public static MeasurementTable getTableFromFile(String filename) {
        MeasurementTable table;

        var path = Path.of(filename);
        try(var stream = Files.lines(path)) {
            var tac = new TableAccumulator(path);
            stream.forEachOrdered(tac::accept);
            table = tac.table;
        } catch (IOException e) {
            System.err.println("MeasurementTable IO: " + e.getMessage());
            IJ.error("MeasurementTable: " + e.getMessage());
            table = null;
        } catch (NumberFormatException nfe) {
            System.err.println("MeasurementTable Number Format: " + nfe.getMessage());
            IJ.error("MeasurementTable: " + nfe.getMessage());
            table = null;
        }

        return table;
    }

    public static boolean isMeasurementsTable(String tableName) {
        var s = MeasurementTable.longerName(tableName);
        var panel = getMeasurementsWindow(s);

        return panel != null;
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
        var panel = getMeasurementsWindow(inputPanelName);

        if (panel != null) {
            return panel.getTable();
        }

        // THIS CODE DOESN'T USUALLY RUN, IT JUST RETURNS THE NEW TABLE
        // since MeasurementsWindow is not part of the window manager,
        // no textArea is found

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
        return INSTANCES.keySet().toArray(String[]::new);
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

        if (!locked && dataChanged) {
            updateView(UpdateEvent.DATA_CHANGED);
        }
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


    @Override
    public synchronized void incrementCounter() {
        super.incrementCounter();
        updateViewSynced(UpdateEvent.ROW_INSERTED, getCounter()-1, getCounter()-1);
    }

    /**
     * Stores a number using a temporary different number of decimal places.
     */
    public void addValue(String column, double value, int places) {
        setPrecision(16);
        super.addValue(column, value);
        updateView(UpdateEvent.CELL_UPDATED, getCounter()-1, getColumnIndex(column));
        // setPrecision (DEFAULT_DECIMALS);
        // PRESENT ResultsTable DOESN'T KEEP TRACK OF INDIVIDUAL PRECISIONS!!!
    }

    @Override
    public void setValue(int column, int row, double value) {
        var newColNeeded = column > getLastColumn();
        super.setValue(column, row, value);
        if (newColNeeded) {
            updateViewSynced(UpdateEvent.COL_ADDED, getLastColumn(), getLastColumn());
        }
        updateView(UpdateEvent.CELL_UPDATED, row, column);
    }

    public void rename(String newName) {
        var oldName = shortName;
        if (INSTANCES.containsKey(oldName)) {
            INSTANCES.put(newName, INSTANCES.remove(oldName));
            if (window != null) {
                window.setTitle(newName);
            }
        }

        shortName = newName;
    }

    /**
     * @see #rename(String)
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Displays/Refreshes a MeasurementTable with long name.
     */
    public void show() {
        if (SwingUtilities.isEventDispatchThread()) {
            threadlessShow();
        } else {
            try {
                SwingUtilities.invokeAndWait(this::threadlessShow);
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void show(String name) {
        if (shortName == null) {
            shortName = name;
        }
        show();
    }

    private void threadlessShow() {
        INSTANCES.putIfAbsent(shortName, this);
        if (window == null) {
            // Fetch previous window and update it
            window = getMeasurementsWindow(shortName);

            if (window == null) {
                window = new MeasurementsWindow(this);
                window.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        window.getTable().window = null;
                        INSTANCES.remove(shortName);
                    }
                });
            }
        }
        if (window.getTable() != this) {
            window.setTable(this);
            INSTANCES.put(shortName, this);
        }

        window.setVisible(true);
    }

    private void updateView(UpdateEvent event) {
        updateView(event, 0, 0);
    }

    private void updateView(UpdateEvent event, int i1, int i2) {
        dataChanged = true;
        if (window == null ||  (isLocked() && !event.structureModification)) return;
        // If coming from the event thread, it may be the table -
        // in which case delaying the update can cause the view and the model to desync
        if (SwingUtilities.isEventDispatchThread()) {
            if (window != null) {
                window.update(event, i1, i2);
            }
            dataChanged = false;
        } else {
            SwingUtilities.invokeLater(() -> {
                if (window != null) {
                    window.update(event, i1, i2);
                }
                dataChanged = false;
            });
        }
    }

    private void updateViewSynced(UpdateEvent event, int i1, int i2) {
        dataChanged = true;
        if (window == null || (isLocked() && !event.structureModification)) return;
        // If coming from the event thread, it may be the table -
        // in which case delaying the update can cause the view and the model to desync
        if (SwingUtilities.isEventDispatchThread()) {
            if (window != null) {
                window.update(event, i1, i2);
            }
            dataChanged = false;
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (window != null) {
                        window.update(event, i1, i2);
                    }
                    dataChanged = false;
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
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

    public void setFilePath(String string) {
        filePath = string;
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
        updateView(UpdateEvent.REBUILD);
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

    @Override
    @Deprecated
    public void setHeading(int column, String heading) {
        super.setHeading(column, heading);
        updateView(UpdateEvent.COL_RENAMED, column, column);
    }

    @Override
    public synchronized void deleteRow(int rowIndex) {
        super.deleteRow(rowIndex);
        updateView(UpdateEvent.ROW_DELETED, rowIndex, rowIndex);
    }

    public synchronized void updateRelatedPlot() {
        if (MultiPlot_.isRunning() && MultiPlot_.getTable() == this) {
            MultiPlot_.updatePlot();
        }
    }

    @Override
    public int getFreeColumn(String heading) {
        var i = super.getFreeColumn(heading);
        if (COLUMN_IN_USE != i) {
            updateViewSynced(UpdateEvent.COL_ADDED, i, i);
        }
        return i;
    }

    @Override
    public synchronized MeasurementTable clone() {
        var n = (MeasurementTable) super.clone();
        n.window = null;
        return n;
    }

    @Override
    public void sort(String column) {
        super.sort(column);
        updateView(UpdateEvent.DATA_CHANGED);
    }

    @Override
    public void showRowNumbers(boolean showNumbers) {
        super.showRowNumbers(showNumbers);
        updateView(UpdateEvent.REBUILD);
    }

    @Override
    public void showRowIndexes(boolean showIndexes) {
        super.showRowIndexes(showIndexes);
        updateView(UpdateEvent.REBUILD);
    }

    @Override
    @Deprecated
    public void addLabel(String columnHeading, String label) {
        var needsUpdate = super.hasRowLabels();
        super.addLabel(columnHeading, label);
        if (!needsUpdate) {
            updateView(UpdateEvent.REBUILD);
        }
    }

    @Override
    public void setLabel(String label, int row) {
        var needsUpdate = super.hasRowLabels();
        super.setLabel(label, row);
        if (!needsUpdate) {
            updateView(UpdateEvent.REBUILD);
        }
    }

    private static class TableAccumulator {
        final MeasurementTable table;
        private String delimiter;
        private String maybeHeaderLine;
        private boolean pastHeader = false;
        private int row;
        private boolean hasLabels;
        private boolean hasImageLabel;
        private int shift;
        private String[] header;
        private Pattern metadataExtractor = Pattern.compile("#\\s+AIJ_(?<ID>\\w+) (?<VAL>\\w+)");

        public TableAccumulator(Path path) {
            table = new MeasurementTable(path.getFileName().toString().isEmpty() ?
                    "Measurements" :
                    path.getFileName().toString());

            var fileName = path.toString();

            table.filePath = fileName;

            if (fileName.endsWith(".csv")) {
                delimiter = ",";
            } else if (fileName.endsWith(".prn") || fileName.endsWith(".spc")) {
                delimiter = " +";
            } else if (fileName.endsWith(".txt")) {
                delimiter = "\\s+";  //whitespace delimiter
            } else {
                delimiter = "\t";
            }
        }

        public void accept(String line) {
            if (line == null) {
                return;
            }

            if (!pastHeader && line.startsWith("#")) {
                if (line.startsWith("AIJ_", 1) || line.startsWith("AIJ_", 2)) {
                    var m = metadataExtractor.matcher(line);
                    if (m.matches()) {
                        var id = m.group("ID");
                        if (id != null) {
                            table.metadata.put(id, m.group("VAL"));
                        }
                    }
                }

                maybeHeaderLine = line;
                return;
            }

            if (!pastHeader) {
                String[] labels = line.split(delimiter);
                int n = labels.length;
                if (n < 2 || (n == 2 && labels[0].trim().isEmpty() && !labels[1].trim().isEmpty())) {
                    delimiter = "\\s+";
                    labels = line.split(delimiter);
                    n = labels.length;
                }
                String[] labels1 = labels.clone();

                hasLabels = false;
                hasImageLabel = false;
                shift = 0;

                if (n > 1 && Double.isNaN(Tools.parseDouble(labels[0])) && Double.isNaN(Tools.parseDouble(labels[1]))) {
                    hasLabels = true;    //the last line read has good column headers
                    maybeHeaderLine = null;  //forget the previous comment line, if it exists
                } else if (maybeHeaderLine != null) { //the last line read has no column headers, so try previous comment line, if it exists
                    while (maybeHeaderLine.startsWith("#")) {
                        maybeHeaderLine = maybeHeaderLine.substring(1);
                    }
                    labels = maybeHeaderLine.split(delimiter);
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

                header = new String[n + shift];
                header[0] = "";
                header[1] = "Label";
                row = 0;
                if (hasImageLabel && hasLabels) { //input table has standard MeasurementsTable headings and row labels, use headings as they are
                    System.arraycopy(labels, 1 - shift, header, 1, header.length - 1);
                } else if (hasImageLabel && !hasLabels) { //input table has no headers, but has row lables, make generic headers and store line of data
                    table.incrementCounter();
                    row++;
                    table.addLabel(shift == 0 ? labels[1] : labels[0]);
                    for (int i = 2; i < header.length; i++) {
                        header[i] = "Col_" + (i - 1);
                        table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                    }
                } else if (!hasImageLabel && hasLabels) { //input table has generic headers and no row labels, use standard header prefix and add headers
                    System.arraycopy(labels, 2 - shift, header, 2, header.length - 2);
                } else { // input table has no headers and no row labels, make generic headers and store line of data
                    table.incrementCounter();
                    row++;
                    table.addLabel(header[1], "Row_1");
                    for (int i = 2; i < header.length; i++) {
                        header[i] = "Col_" + (i - 1);
                        table.addValue(header[i], Double.parseDouble(labels[i - shift]));
                    }
                }

                pastHeader = true;
                return;
            }

            if (maybeHeaderLine != null) {
                return;
            }

            double d = 0.0;

            if (!line.startsWith("#") && !line.isBlank()) {
                table.incrementCounter();
                String[] words = line.split(delimiter);
                if (shift == 0) {
                    table.addLabel(header[1], words[1]);
                } else if (shift == 1) {
                    table.addLabel(header[1], hasImageLabel ? words[0] : "Row_" + (row + 1));
                } else if (shift == 2) {
                    table.addLabel(header[1], "Row_" + (row + 1));
                }

                for (int col = (2 - shift); col < header.length - shift; col++) {
                    if (col >= words.length) {
                        d = Double.NaN;
                    } else if (words[col] == null) {
                        d = Double.NaN;
                    } else if (words[col].isBlank() || words[col].trim().equals("-")) {
                        d = Double.NaN;
                    } else if (isHMS(words[col])) {
                        d = hms(words[col]);
                    } else {
                        d = Tools.parseDouble(words[col]);

                        try {
                            if (d != Double.parseDouble(words[col])) {
                                throw new IllegalArgumentException(words[col]);
                            }
                        } catch (Exception e) {}
                    }
                    table.addValue(header[col + shift], d);
                }
                row++;
            }
        }
    }
}
