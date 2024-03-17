package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiPlot_;
import astroj.IJU;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Prefs;
import ij.astro.util.FileAssociationHandler;
import ij.astro.util.FileAssociationHandler.AssociationMapper;
import ij.astro.util.FitsExtensionUtil;
import ij.plugin.FITS_Reader;
import ij.plugin.PlugIn;
import nom.tam.fits.Fits;
import nom.tam.util.FitsFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

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
    private static final AssociationMapper multiplotFitsTableHandler =
            new AssociationMapper(p -> {
                if (FitsExtensionUtil.isFitsFile(p.toString())) {
                    try (var fits = new Fits(new FitsFile(p.toFile()))) {
                        fits.read(); // Read the headers in
                        return fits.getPrimaryHeader().getBooleanValue("AIJ_TBL", false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return false;
            },
            p -> {
                var table = new MeasurementTable(p.getFileName() + " Measurements");
                table.setFilePath(p.toString());
                if (FITS_Reader.handleTable(p, table) != null) {
                    table.show();

                    if (!MultiPlot_.isRunning()) {
                        //IJ.runPlugIn("Astronomy.MultiPlot_", "");
                        // Fixes NPE when opening via file association
                        new MultiPlot_().run(table.shortTitle());
                        return;
                    }

                    MultiPlot_.loadDataOpenConfig(table, p.toString());
                }
            }, true);
    private static final AssociationMapper multiplotPlotCfgHandler =
            new AssociationMapper(p -> MultiPlot_.loadConfigOfOpenTable(p.toString()), true, ".plotcfg");
    private static final AssociationMapper radecHandler =
            new AssociationMapper(p -> IJU.openRaDecApertures(p.toString()), true, ".radec");
    private static final AssociationMapper aperturesHandler =
            new AssociationMapper(p -> {
                var asw = IJU.getBestOpenAstroStackWindow();
                if (asw != null) {
                    asw.openApertures(p.toString());
                }
            }, true, ".apertures");

    @Override
    public void run(String arg) {
        if (IJ.isWindows()) {
            WinMutexHandler.createMutex();
        }
        FileAssociationHandler.registerAssociation(multiplotTableHandler);
        FileAssociationHandler.registerAssociation(multiplotFitsTableHandler);
        FileAssociationHandler.registerAssociation(radecHandler);
        FileAssociationHandler.registerAssociation(aperturesHandler);
        FileAssociationHandler.registerAssociation(multiplotPlotCfgHandler);
        ensureConfigFileExists();
        Executors.newSingleThreadExecutor()
                .execute(() -> IJ.runPlugIn(AstroImageJ_Updater.class.getCanonicalName(), "check"));
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
