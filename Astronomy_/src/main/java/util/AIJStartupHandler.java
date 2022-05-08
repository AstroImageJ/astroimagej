package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiPlot_;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Prefs;
import ij.astro.util.FileAssociationHandler;
import ij.astro.util.FileAssociationHandler.AssociationMapper;
import ij.plugin.PlugIn;

/**
 * Handle tasks on AIJ startup that need to reference code outside of the IJ package.
 * <p>
 * Run as plugin in {@link ij.ImageJ#main(String[])}.
 */
public class AIJStartupHandler implements PlugIn {
    private static final AssociationMapper multiplotTableHandler =
            new AssociationMapper(p -> {
                if (!MultiPlot_.isRunning()) {
                    IJ.runPlugIn("Astronomy.MultiPlot_", "");
                }
                //todo why does DnD cause second instance to open?
                //MultiPlot_.openDragAndDropFiles(new File[]{p.toFile()});
                MeasurementTable table = MeasurementTable.getTableFromFile(p.toString());
                if (table != null) {
                    table.show();
                    MultiPlot_.loadDataOpenConfig(table, p.toString());
                }
            }, true, Prefs.defaultResultsExtension());

    @Override
    public void run(String arg) {
        IJ.runPlugIn(AstroImageJ_Updater.class.getCanonicalName(), "check");
        FileAssociationHandler.registerAssociation(multiplotTableHandler);
    }
}
