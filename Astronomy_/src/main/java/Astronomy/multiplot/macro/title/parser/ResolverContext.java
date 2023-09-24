package Astronomy.multiplot.macro.title.parser;

import astroj.MeasurementTable;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.WindowManager;

public class ResolverContext {
    MeasurementTable table;
    private ImagePlus imp;
    private boolean failedImpDiscovery;

    public ResolverContext(MeasurementTable table) {
        this.table = table;
    }

    public ImagePlus getImp() {
        if (imp != null) {
            return imp;
        }
        if (!failedImpDiscovery) {
            imp = getImpForSlice(table);
            failedImpDiscovery = imp == null;
        }

        return imp;
    }

    /**
     * Find an open stack that contains a slice that matches the label.
     */
    private static ImagePlus getImpForSlice(MeasurementTable table) {
        var ids = WindowManager.getIDList();
        var label = table.getLabel(0).split("\n")[0];
        if (ids != null) {
            for (int id : ids) {
                var i = WindowManager.getImage(id);
                if (i != null) {
                    var s = i.getImageStack();
                    //var n = s.getShortSliceLabel(0, 500);
                    var ls = s.getSliceLabels();
                    if (ls != null) {
                        for (String sliceLabel : s.getSliceLabels()) {
                            if (sliceLabel == null) {
                                break;
                            }
                            if (label.equals(sliceLabel.split("\n")[0])) {
                                return i;
                            }
                        }
                    } else if (s instanceof VirtualStack virtualStack) {
                        var l = virtualStack.getSliceLabel(i.getCurrentSlice());
                        if (l != null) {
                            l = l.split("\n")[0];
                        }
                        for (int row = 0; row < table.size(); row++) {
                            var sliceLabel = table.getLabel(0);
                            if (sliceLabel == null) {
                                continue;
                            }
                            if (table.getLabel(row).split("\n")[0].equals(l)) {
                                return i;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
