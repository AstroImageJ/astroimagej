package ij.astro.accessors;

import ij.Prefs;
import ij.gui.Plot;
import ij.measure.ResultsTable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public record TransferablePlot(Plot plot) implements Transferable {
    private static final DataFlavor[] flavors;

    static {
        try {
            flavors = new DataFlavor[]{DataFlavor.imageFlavor,
                    new DataFlavor("text/tab-separated-values"), DataFlavor.stringFlavor};
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public TransferablePlot {//todo Can the copy button be added to the seeing profile stack display as well (would copy the currently displayed image)?
        if (plot == null) {
            throw new IllegalArgumentException("Plot may not be null");
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor dataFlavor : flavors) {
            if (flavor.equals(dataFlavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(DataFlavor.imageFlavor)) {
            plot.updateImage();
            return plot.getImagePlus().getBufferedImage();
        } else if (flavor.equals(flavors[1]) || flavor.equals(flavors[2])) {
            ResultsTable rt = plot.getResultsTableWithLabels();
            if (rt == null) return "";
            CharArrayWriter aw = new CharArrayWriter(10*rt.size());
            PrintWriter pw = new PrintWriter(aw);
            if (!Prefs.dontSaveHeaders) {
                String headings = rt.getColumnHeadings();
                pw.println(headings);
            }
            for (int i=0; i<rt.size(); i++) {
                pw.println(rt.getRowAsString(i));
            }
            String text = aw.toString();
            pw.close();
            return text;
        }

        throw new UnsupportedFlavorException(flavor);
    }
}
