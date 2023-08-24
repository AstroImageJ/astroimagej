package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiPlot_;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Prefs;
import ij.astro.util.FileAssociationHandler;
import ij.astro.util.FileAssociationHandler.AssociationMapper;
import ij.plugin.PlugIn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handle tasks on AIJ startup that need to reference code outside of the IJ package.
 * <p>
 * Run as plugin in {@link ij.ImageJ#main(String[])}.
 */
public class AIJStartupHandler implements PlugIn {
    private static final AssociationMapper multiplotTableHandler =
            new AssociationMapper(p -> {
                MeasurementTable table = MeasurementTable.getTableFromFile(p.toString());
                if (table != null) {
                    table.show();

                    if (!MultiPlot_.isRunning()) {
                        //IJ.runPlugIn("Astronomy.MultiPlot_", "");
                        // Fixes NPE when opening via file association
                        new MultiPlot_().run(table.shortTitle());
                        return;
                    }

                    MultiPlot_.loadDataOpenConfig(table, p.toString());
                }
            }, true, Prefs.defaultResultsExtension());

    @Override
    public void run(String arg) {
        FileAssociationHandler.registerAssociation(multiplotTableHandler);
        ensureConfigFileExists();
        IJ.runPlugIn(AstroImageJ_Updater.class.getCanonicalName(), "check");
    }

    private void ensureConfigFileExists() {
        var p = Path.of("AstroImageJ.cfg");
        try {
            if (Files.notExists(p)) {
                Files.createFile(p);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
