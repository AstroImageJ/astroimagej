package Astronomy.multiplot.macro.title.parser;

import astroj.AstroStackWindow;
import astroj.FitsJ;
import astroj.MeasurementTable;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.WindowManager;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ResolverContext {
    MeasurementTable table;
    private ImagePlus imp;
    private boolean failedImpDiscovery;
    private static final Map<ImagePlus, Map<Integer, FitsJ.Header>> headerCache = new WeakHashMap<>();

    public ResolverContext(MeasurementTable table) {
        this.table = table;
    }

    public boolean isValid() {
        return table != null;
    }

    public static void invalidateHeaderCache(ImagePlus imp, int slice) {
        var m = headerCache.get(imp);
        if (m != null) {
            m.remove(slice);
        }
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
        if (slice == 0) {
            return null;
        }
        if (getImp() != null) {
            headerCache.computeIfAbsent(imp, $ -> new HashMap<>());
            headerCache.get(imp).computeIfAbsent(slice, this::getHeaderForSlice);
            return headerCache.get(imp).get(slice);
        }

        return null;
    }

    private FitsJ.Header getHeaderForSlice(int slice) {
        if (slice == -1) {
            if (getImp() != null) {
                return FitsJ.getHeader(imp);
            }
        }

        if (slice < 1 || imp.getStackSize() < slice) {
            slice = imp.getStackSize();
        }

        FitsJ.Header header;
        if (getImp() != null) {
            var s = imp.getImageStack();
            if (s instanceof VirtualStack) {
                var cSlice = imp.getSlice();

                imp.setSliceWithoutUpdate(slice);

                header = FitsJ.getHeader(imp);

                imp.setSliceWithoutUpdate(cSlice);

                return header;
            } else {
                var sliceLabel = s.getSliceLabel(slice);
                return FitsJ.getHeader(sliceLabel);
            }
        }

        return null;
    }

    /**
     * Find an open stack that contains a slice that matches the label.
     */
    private static ImagePlus getImpForAnySlice(MeasurementTable table) {
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
                            for (int row = 0; row < table.size(); row++) {
                                var label = table.getLabel(row);
                                if (label.equals(sliceLabel.split("\n")[0])) {
                                    return i;
                                }
                            }
                        }
                    } else if (s instanceof VirtualStack virtualStack) {
                        for (int slice = 1; slice <= virtualStack.size(); slice++) {
                            var l = virtualStack.getSliceLabel(slice);
                            if (l != null) {
                                l = l.split("\n")[0];
                                for (int row = 0; row < table.size(); row++) {
                                    var label = table.getLabel(row);
                                    if (label.equals(l)) {
                                        return i;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        var id = WindowManager.getCurrentImage();
        if (id!= null && (id.getWindow() instanceof AstroStackWindow)) {
            return id;
        }else{
            ids = WindowManager.getIDList();
            if (ids != null) {
                for (int idd : ids) {
                    id = WindowManager.getImage(idd);
                    if ((id.getWindow() instanceof AstroStackWindow)) {
                        return id;
                    }
                }
            }
        }
        return null;
    }
}
