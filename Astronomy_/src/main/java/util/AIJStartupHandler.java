package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import astroj.IJU;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Prefs;
import ij.astro.util.FileAssociationHandler;
import ij.astro.util.FileAssociationHandler.AssociationMapper;
import ij.astro.util.FitsExtensionUtil;
import ij.astro.util.ObjectShare;
import ij.plugin.FITS_Reader;
import ij.plugin.PlugIn;
import nom.tam.fits.Fits;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import static Astronomy.MultiPlot_.useMacroSubtitle;
import static Astronomy.MultiPlot_.useMacroTitle;

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
                    try (var fits = new Fits(Files.newInputStream(p))) {
                        fits.read(); // Read the headers in
                        return fits.getPrimaryHeader().getBooleanValue("AIJ_TBL", false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return false;
            },
            p -> {
                var table = new MeasurementTable(p.getFileName().toString());
                table.setFilePath(p.toString());
                var tableRead = FITS_Reader.handleTable(p, table);
                if (tableRead != null) {
                    if (tableRead.loadTable()) {
                        useMacroSubtitle.set(false);
                        useMacroTitle.set(false);
                        table.show();
                    }

                    var asw = IJU.getBestOpenAstroStackWindow();
                    if (asw != null && tableRead.apertures() != null) {
                        asw.openApertures(new ByteArrayInputStream(tableRead.apertures()));
                    } else if (tableRead.apertures() != null) {
                        try {
                            Prefs.set("multiaperture.xapertures", "");
                            Prefs.set("multiaperture.yapertures", "");
                            Prefs.set("multiaperture.raapertures", "");
                            Prefs.set("multiaperture.decapertures", "");
                            Prefs.set("multiaperture.isrefstar", "");
                            Prefs.set("multiaperture.isalignstar", "");
                            Prefs.set("multiaperture.centroidstar", "");
                            Prefs.set("multiaperture.absmagapertures", "");
                            Prefs.set("multiaperture.import.xapertures", "");
                            Prefs.set("multiaperture.import.yapertures", "");
                            Prefs.set("multiaperture.import.raapertures", "");
                            Prefs.set("multiaperture.import.decapertures", "");
                            Prefs.set("multiaperture.import.isrefstar", "");
                            Prefs.set("multiaperture.import.isalignstar", "");
                            Prefs.set("multiaperture.import.centroidstar", "");
                            Prefs.ijPrefs.load(new ByteArrayInputStream(tableRead.apertures()));
                        } catch (IOException e) {
                            e.printStackTrace();
                            IJ.error("Failed to read apertures");
                        }
                    }

                    if (tableRead.loadTable()) {
                        if (!MultiPlot_.isRunning()) {
                            //IJ.runPlugIn("Astronomy.MultiPlot_", "");
                            // Fixes NPE when opening via file association
                            new MultiPlot_().run(table.shortTitle());
                            return;
                        }

                        MultiPlot_.loadDataOpenConfig(table, p.toString());
                    }
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
                } else {
                    try {
                        Prefs.set("multiaperture.xapertures", "");
                        Prefs.set("multiaperture.yapertures", "");
                        Prefs.set("multiaperture.raapertures", "");
                        Prefs.set("multiaperture.decapertures", "");
                        Prefs.set("multiaperture.isrefstar", "");
                        Prefs.set("multiaperture.isalignstar", "");
                        Prefs.set("multiaperture.centroidstar", "");
                        Prefs.set("multiaperture.absmagapertures", "");
                        Prefs.set("multiaperture.import.xapertures", "");
                        Prefs.set("multiaperture.import.yapertures", "");
                        Prefs.set("multiaperture.import.raapertures", "");
                        Prefs.set("multiaperture.import.decapertures", "");
                        Prefs.set("multiaperture.import.isrefstar", "");
                        Prefs.set("multiaperture.import.isalignstar", "");
                        Prefs.set("multiaperture.import.centroidstar", "");
                        Prefs.ijPrefs.load(Files.newBufferedReader(p));
                    } catch (IOException e) {
                        e.printStackTrace();
                        IJ.error("Failed to read apertures");
                    }
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
        ObjectShare.putIfAbsent("multiapertureKeys", MultiAperture_.getApertureKeys());
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
