package Astronomy.multiplot.macro.title.parser;

import astroj.FitsJ;
import astroj.MeasurementTable;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.WindowManager;
import ij.measure.ResultsTable;

import java.util.HashMap;

public class ResolverContext {
    MeasurementTable table;
    private ImagePlus imp;
    private boolean failedImpDiscovery;
    private final HashMap<Integer, FitsJ.Header> headerCache = new HashMap<>();

    public ResolverContext(MeasurementTable table) {
        this.table = table;
    }

    public boolean isValid() {
        return table != null;
    }

    public ImagePlus getImp() {
        if (imp != null) {
            return imp;
        }
        if (!failedImpDiscovery) {
            imp = getImpForAnySlice(table);
            failedImpDiscovery = imp == null;
        }

        return imp;
    }

    public FitsJ.Header getHeader(int slice) {
        headerCache.computeIfAbsent(slice, this::getHeaderForSlice);
        return headerCache.get(slice);
    }

    private FitsJ.Header getHeaderForSlice(int slice) {
        if (slice == -1) {
            if (getImp() != null) {
                return FitsJ.getHeader(imp);
            }
        }

        // Find slice index for tables that are sorted differently
        var c = table.getColumnIndex("slice");
        var index = -1;
        if (c != ResultsTable.COLUMN_NOT_FOUND) {
            if (slice - 1 >= 0 && slice-1 < table.size() && table.getValueAsDouble(c, slice-1) == slice) {
                index = slice-1;
            } else {
                for (int i = 0; i < table.size(); i++) {
                    if (table.getValueAsDouble(c, i) == slice) {
                        index = i;
                        break;
                    }
                }
            }
        } else {
            return null;
        }

        if (index < 0) {
            return null;
        }


        var ids = WindowManager.getIDList();
        var label = table.getLabel(index).split("\n")[0];
        FitsJ.Header header = null;
        if (ids != null) {
            for (int id : ids) {
                var i = WindowManager.getImage(id);
                if (i != null) {

                    var s = i.getImageStack();

                    if (s instanceof VirtualStack) {
                        var cSlice = i.getSlice();
                        i.setSliceWithoutUpdate(slice);

                        var l = s.getSliceLabel(i.getCurrentSlice());
                        if (l != null) {
                            l = l.split("\n")[0];
                        }

                        if (label.equals(l)) {
                            header = FitsJ.getHeader(i);
                        }

                        i.setSliceWithoutUpdate(cSlice);

                        if (header != null) {
                            return header;
                        }
                    } else {
                        if (s.size() >= 1) {
                            for (String sliceLabel : s.getSliceLabels()) {
                                if (sliceLabel != null && label.equals(sliceLabel.split("\n")[0])) {
                                    return FitsJ.getHeader(sliceLabel);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find an open stack that contains a slice that matches the label.
     */
    private static ImagePlus getImpForAnySlice(MeasurementTable table) {
        for (int row = 0; row < table.size(); row++) {
            var label = table.getLabel(row);
            var ids = WindowManager.getIDList();
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
                                if (label.equals(l)) {
                                    return i;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
