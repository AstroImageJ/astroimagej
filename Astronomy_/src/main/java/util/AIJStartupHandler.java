package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import Astronomy.multiaperture.FreeformPixelApertureHandler;
import Astronomy.multiaperture.io.AperturesFileCodec;
import astroj.*;
import ij.IJ;
import ij.Prefs;
import ij.astro.io.prefs.Property;
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
import java.util.ArrayList;
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
            new AssociationMapper((p, openOptions) -> {
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
            (p, openOptions) -> {
                var table = new MeasurementTable(p.getFileName().toString());
                table.setFilePath(p.toString());
                var tableRead = FITS_Reader.handleTable(p, table, openOptions);
                if (tableRead != null) {
                    if (tableRead.loadTable()) {
                        useMacroSubtitle.set(false);
                        useMacroTitle.set(false);
                        table.show();
                    }

                    var asw = IJU.getBestOpenAstroStackWindow();
                    if (asw != null && tableRead.apertures() != null) {
                        var d = AperturesFileCodec.readContents(new String(tableRead.apertures()));
                        if (d != null && !d.legacy()) {
                            var pixelAps = d.getAperturesOfType(FreeformPixelApertureRoi.class, Aperture.ApertureShape.FREEFORM_PIXEL);
                            FreeformPixelApertureHandler.APS.set(pixelAps);
                            FreeformPixelApertureHandler.IMPORTED_APS.set(pixelAps);
                            Prefs.ijPrefs.putAll(d.prefs());
                            Property.resetLoadStatus();
                        } else {
                            asw.openApertures(new ByteArrayInputStream(tableRead.apertures()));
                        }
                    } else if (tableRead.apertures() != null) {
                        try {
                            var d = AperturesFileCodec.readContents(new String(tableRead.apertures()));
                            if (d != null && !d.legacy()) {
                                var pixelAps = d.getAperturesOfType(FreeformPixelApertureRoi.class, Aperture.ApertureShape.FREEFORM_PIXEL);
                                if (!pixelAps.isEmpty()) {
                                    FreeformPixelApertureHandler.APS.set(pixelAps);
                                    FreeformPixelApertureHandler.IMPORTED_APS.set(pixelAps);
                                }

                                var shapedAps = d.getAperturesOfType(ShapedApertureRoi.class, Aperture.ApertureShape.FREEFORM_SHAPE);
                                if (!shapedAps.isEmpty()) {
                                    MultiAperture_.SHAPED_APS.set(shapedAps);
                                    MultiAperture_.SHAPED_IMPORTED_APS.set(shapedAps);
                                }
                                Prefs.ijPrefs.putAll(d.prefs());
                                Property.resetLoadStatus();
                            } else {
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
                                Property.resetLoadStatus();
                            }
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
            new AssociationMapper((p, openOptions) -> MultiPlot_.loadConfigOfOpenTable(p.toString()), true, ".plotcfg");
    private static final AssociationMapper radecHandler =
            new AssociationMapper((p, openOptions) -> IJU.openRaDecApertures(p.toString()), true, ".radec");
    private static final AssociationMapper aperturesHandler =
            new AssociationMapper((p, openOptions) -> {
                var asw = IJU.getBestOpenAstroStackWindow();
                if (asw != null) {
                    try {
                        var s = Files.readString(p);
                        var d = AperturesFileCodec.readContents(s);
                        if (d != null && !d.legacy()) {
                            var pixelAps = d.getAperturesOfType(FreeformPixelApertureRoi.class, Aperture.ApertureShape.FREEFORM_PIXEL);
                            if (!pixelAps.isEmpty()) {
                                FreeformPixelApertureHandler.APS.set(pixelAps);
                                FreeformPixelApertureHandler.IMPORTED_APS.set(pixelAps);
                            }

                            var shapedAps = d.getAperturesOfType(ShapedApertureRoi.class, Aperture.ApertureShape.FREEFORM_SHAPE);
                            if (!shapedAps.isEmpty()) {
                                MultiAperture_.SHAPED_APS.set(shapedAps);
                                MultiAperture_.SHAPED_IMPORTED_APS.set(shapedAps);
                            }

                            Prefs.ijPrefs.putAll(d.prefs());
                            Property.resetLoadStatus();
                            if (asw.getCanvas() instanceof AstroCanvas ac) {
                                ac.removeApertureRois();
                            }

                            var aps = pixelAps.isEmpty() ? shapedAps : pixelAps;
                            for (int i = 0; i < aps.size(); i++) {
                                var ap = aps.get(i);
                                ap.setPhantom(true);
                                ap.setName((ap.getIsComparisonStar() ? "C" : "T") + (i+1));
                                ap.setImage(asw.getImagePlus());
                                if (asw.getCanvas() instanceof AstroCanvas ac) {
                                    ac.add(ap);
                                }
                            }
                        } else {
                            asw.openApertures(p.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        var s = Files.readString(p);
                        var d = AperturesFileCodec.readContents(s);
                        if (d != null && !d.legacy()) {
                            var pixelAps = d.getAperturesOfType(FreeformPixelApertureRoi.class, Aperture.ApertureShape.FREEFORM_PIXEL);
                            if (!pixelAps.isEmpty()) {
                                FreeformPixelApertureHandler.APS.set(pixelAps);
                                FreeformPixelApertureHandler.IMPORTED_APS.set(pixelAps);
                            }

                            var shapedAps = d.getAperturesOfType(ShapedApertureRoi.class, Aperture.ApertureShape.FREEFORM_SHAPE);
                            if (!shapedAps.isEmpty()) {
                                MultiAperture_.SHAPED_APS.set(shapedAps);
                                MultiAperture_.SHAPED_IMPORTED_APS.set(shapedAps);
                            }

                            Prefs.ijPrefs.putAll(d.prefs());
                            Property.resetLoadStatus();
                        } else {
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
                            FreeformPixelApertureHandler.IMPORTED_APS.set(new ArrayList<>());
                            Prefs.ijPrefs.load(Files.newBufferedReader(p));
                            Property.resetLoadStatus();
                        }
                    } catch (Exception e) {
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
        ObjectShare.putIfAbsent("multiapertureCircularKeys", MultiAperture_.getCircularApertureKeys());
        ObjectShare.putIfAbsent("customApertureKey", FreeformPixelApertureHandler.APS.getPropertyKey());
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
