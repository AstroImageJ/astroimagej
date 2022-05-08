package util;

import Astronomy.AstroImageJ_Updater;
import Astronomy.MultiPlot_;
import ij.IJ;
import ij.Prefs;
import ij.astro.util.FileAssociationHandler;
import ij.astro.util.FileAssociationHandler.AssociationMapper;
import ij.plugin.PlugIn;

import java.io.File;

/**
 * Handle tasks on AIJ startup that need to reference code outside of the IJ package.
 * <p>
 * Run as plugin in {@link ij.ImageJ#ImageJ(java.applet.Applet, int) the main ImageJ constructor}.
 */
public class AIJStartupHandler implements PlugIn {
    private static final AssociationMapper multiplotTableHandler =
            new AssociationMapper(p -> {
                IJ.runPlugIn("Astronomy.MultiPlot_", "");
                MultiPlot_.openDragAndDropFiles(new File[]{p.toFile()});
            }, Prefs.defaultResultsExtension());

    @Override
    public void run(String arg) {
        IJ.runPlugIn(AstroImageJ_Updater.class.getCanonicalName(), "check");
        FileAssociationHandler.registerAssociation(multiplotTableHandler);
    }
}
