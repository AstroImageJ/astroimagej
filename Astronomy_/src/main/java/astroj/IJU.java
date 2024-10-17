// IJU.java

package astroj;

import Astronomy.MultiAperture_;
import Astronomy.multiaperture.FreeformPixelApertureHandler;
import Astronomy.multiaperture.io.AperturesFile;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.util.FitsExtensionUtil;
import ij.astro.util.ObjectShare;
import ij.gui.ImageWindow;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.process.ImageProcessor;
import ij.util.Tools;
import util.BrowserOpener;
import util.LinearInterpolator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

import static java.lang.Math.*;

/**
 * Various static utilities
 */
public class IJU {
    public static final Locale locale = Locale.US;
    public static final DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
    public static final DecimalFormat uptoEightPlaces = new DecimalFormat("#####0.########", IJU.dfs);
    public static final DecimalFormat twoPlaces = new DecimalFormat("######0.00", IJU.dfs);
    public static final String separator = System.getProperty("file.separator");

    public static final double a1 = 0.44325141463;
    public static final double a2 = 0.06260601220;
    public static final double a3 = 0.04757383546;
    public static final double a4 = 0.01736506451;
    public static final double b1 = 0.24998368310;
    public static final double b2 = 0.09200180037;
    public static final double b3 = 0.04069697526;
    public static final double b4 = 0.00526449639;

    public static final double aa0 = 1.38629436112;
    public static final double aa1 = 0.09666344259;
    public static final double aa2 = 0.03590092383;
    public static final double aa3 = 0.03742563713;
    public static final double aa4 = 0.01451196212;
    public static final double bb0 = 0.5;
    public static final double bb1 = 0.12498593597;
    public static final double bb2 = 0.06880248576;
    public static final double bb3 = 0.03328355346;
    public static final double bb4 = 0.00441787012;

    public static final String[] spType = {"O8V", "B0V", "B3V", "B5V", "B8V", "A0V", "A5V", "F0V", "F5V", "G0V", "G5V", "K0V", "K5V", "M0V", "M2V", "M5V", "M8V"};
    public static final double[] tStar = {37000, 31500, 19000, 15400, 11800, 9480, 8160, 7020, 6530, 5930, 5680, 5240, 4340, 3800, 3530, 3120, 2600};
    public static final double[] JminusK = {-0.19, -0.16, -0.10, -0.07, -0.03, 0.00, 0.08, 0.16, 0.27, 0.36, 0.41, 0.53, 0.72, 0.84, 0.86, 0.95, 1.14};
    public static final double[] rStar = {8.50, 7.40, 4.80, 3.90, 3.00, 2.40, 1.70, 1.50, 1.30, 1.10, 0.92, 0.85, 0.72, 0.60, 0.50, 0.27, 0.15};
    public static final double[] mStar = {23.00, 17.50, 7.60, 5.90, 3.80, 2.90, 2.00, 1.60, 1.40, 1.05, 0.92, 0.79, 0.67, 0.51, 0.40, 0.21, 0.06};
    public static final double[] rhoStar = {0.05, 0.06, 0.10, 0.14, 0.20, 0.28, 0.56, 0.71, 0.89, 1.00, 1.12, 1.77, 2.50, 3.15, 8.88, 14.08, 22.32};
    public static final double[] rStarJ = {82.62, 71.93, 46.66, 37.91, 29.16, 23.33, 16.52, 14.58, 12.64, 10.69, 8.94, 8.26, 7.00, 5.83, 4.86, 2.62, 1.46};

    public static final LinearInterpolator getRstarJFromTeff = new LinearInterpolator(tStar, rStarJ);
    public static final LinearInterpolator getJminusKFromTeff = new LinearInterpolator(tStar, JminusK);
    public static final LinearInterpolator getTeffFromJminusK = new LinearInterpolator(JminusK, tStar);
    public static final LinearInterpolator getTeffFromMStar = new LinearInterpolator(mStar, tStar);
    public static final LinearInterpolator getMStarFromTeff = new LinearInterpolator(tStar, mStar);
    public static final LinearInterpolator getTeffFromRStar = new LinearInterpolator(rStar, tStar);
    public static final LinearInterpolator getRStarFromTeff = new LinearInterpolator(tStar, rStar);
    public static final LinearInterpolator getTeffFromRhoStar = new LinearInterpolator(rhoStar, tStar);
    public static final LinearInterpolator getRhoStarFromTeff = new LinearInterpolator(tStar, rhoStar);

    public static final String[] colors = new String[]
            {
                    "black",
                    "dark gray",
                    "gray",
                    "light gray",
                    "white",
                    "light green",
                    "green",
                    "dark green",
                    "light blue",
                    "blue",
                    "magenta",
                    "pink",
                    "red",
                    "orange",
                    "yellow",
                    "brown",
                    "purple",
                    "teal"
            };

    public static Color colorOf(String colorName) {
        Color color = Color.red;
        if (colorName.equalsIgnoreCase("black")) color = Color.black;
        else if (colorName.equalsIgnoreCase("dark gray")) color = Color.darkGray;
        else if (colorName.equalsIgnoreCase("gray")) color = Color.gray;
        else if (colorName.equalsIgnoreCase("light gray")) color = Color.lightGray;
        else if (colorName.equalsIgnoreCase("white")) color = Color.white;
        else if (colorName.equalsIgnoreCase("light green")) color = new Color(196, 222, 155); //light green
        else if (colorName.equalsIgnoreCase("green")) color = Color.green;                  //green
        else if (colorName.equalsIgnoreCase("dark green")) color = new Color(0, 155, 0);      //dark green
        else if (colorName.equalsIgnoreCase("light blue")) color = new Color(84, 201, 245);   //light blue
        else if (colorName.equalsIgnoreCase("blue")) color = Color.blue;
        else if (colorName.equalsIgnoreCase("magenta")) color = Color.magenta;
        else if (colorName.equalsIgnoreCase("pink")) color = Color.pink;
        else if (colorName.equalsIgnoreCase("orange")) color = Color.orange;
        else if (colorName.equalsIgnoreCase("yellow")) color = Color.yellow;
        else if (colorName.equalsIgnoreCase("brown")) color = new Color(167, 131, 96);        //brown
        else if (colorName.equalsIgnoreCase("purple")) color = new Color(124, 0, 255);        //purple
        else if (colorName.equalsIgnoreCase("teal")) color = new Color(3, 148, 163);          //teal
        return color;
    }

    public static String colorNameOf(Color color) {
        String colorName = "red";
        if (color.equals(Color.black)) colorName = "black";
        else if (color.equals(Color.darkGray)) colorName = "dark gray";
        else if (color.equals(Color.gray)) colorName = "gray";
        else if (color.equals(Color.lightGray)) colorName = "light gray";
        else if (color.equals(Color.white)) colorName = "white";
        else if (color.equals(new Color(196, 222, 155))) colorName = "light green";      //light green
        else if (color.equals(Color.green)) colorName = "green";            //green
        else if (color.equals(new Color(0, 155, 0))) colorName = "dark green";       //dark green
        else if (color.equals(new Color(84, 201, 245))) colorName = "light blue";       //light blue
        else if (color.equals(Color.blue)) colorName = "blue";
        else if (color.equals(Color.magenta)) colorName = "magenta";
        else if (color.equals(Color.pink)) colorName = "pink";
        else if (color.equals(Color.orange)) colorName = "orange";
        else if (color.equals(Color.yellow)) colorName = "yellow";
        else if (color.equals(new Color(167, 131, 96))) colorName = "brown";            //brown
        else if (color.equals(new Color(124, 0, 255))) colorName = "purple";           //purple
        else if (color.equals(new Color(3, 148, 163))) colorName = "teal";             //teal
        return colorName;
    }

    public static double[] ij2fits(double imageHeight, double[] ij) {
        double[] fits = new double[2];
        fits[0] = ij[0] + Centroid.PIXELCENTER;
        fits[1] = imageHeight - ij[1] + Centroid.PIXELCENTER;
        return fits;
    }

    public static double[] fits2ij(double imageHeight, double[] fits) {
        double[] ij = new double[2];
        ij[0] = fits[0] - Centroid.PIXELCENTER;
        ij[1] = imageHeight - fits[1] + Centroid.PIXELCENTER;
        return ij;
    }

    public static double ijX2fitsX(double ijX) {
        return ijX + Centroid.PIXELCENTER;
    }

    public static double ijY2fitsY(double imageHeight, double ijY) {
        return imageHeight - ijY + Centroid.PIXELCENTER;
    }

    public static double fitsX2ijX(double fitsX) {
        return fitsX - Centroid.PIXELCENTER;
    }

    public static double fitsY2ijY(double imageHeight, double fitsY) {
        return imageHeight - fitsY + Centroid.PIXELCENTER;
    }

    public static String spectralTypeFromDensity(double d) {
        int n = rhoStar.length;
        if (d < rhoStar[0]) return ">" + spType[0];
        if (d > rhoStar[n - 1]) return "<" + spType[n - 1];
        if (d >= rhoStar[0] && d < (rhoStar[1] + rhoStar[0]) / 2) return spType[0];
        if (d > (rhoStar[n - 2] + rhoStar[n - 1]) / 2 && d <= rhoStar[n - 1]) return spType[n - 1];
        for (int i = 0; i < n - 2; i++) {
            if (d >= (rhoStar[i] + rhoStar[i + 1]) / 2 && d < (rhoStar[i + 1] + rhoStar[i + 2]) / 2)
                return spType[i + 1];
        }
        return "error";
    }

    public static String getSpTFromTeff(double t) {
        int n = tStar.length;
        if (t > tStar[0]) return spType[0];
        if (t < tStar[n - 1]) return spType[n - 1];
        if (t <= tStar[0] && t > (tStar[1] + tStar[0]) / 2) return spType[0];
        if (t < (tStar[n - 2] + tStar[n - 1]) / 2 && t >= tStar[n - 1]) return spType[n - 1];
        for (int i = 0; i < n - 2; i++) {
            if (t < (tStar[i] + tStar[i + 1]) / 2 && t >= (tStar[i + 1] + tStar[i + 2]) / 2) return spType[i + 1];
        }
        return spType[n - 1];
    }

    public static double getTeffFromSpT(String spt) {
        int n = tStar.length;
        for (int i = 0; i < n; i++)
            if (spt.equals(spType[i])) return tStar[i];
        return tStar[tStar.length - 1];
    }

    public static double getPlanetRadiusFromTeff(double teff, double RpOverRstar) {
        if (Double.isNaN(teff) || Double.isNaN(RpOverRstar) || RpOverRstar <= 0) return Double.NaN;
        if (teff < tStar[tStar.length - 1]) return 0.0;
        if (teff > tStar[0]) return 0.0;
        return RpOverRstar * getRstarJFromTeff.interpolate(teff);
    }

    public static String planetRadiusFromTeff(double teff, double RpOverRstar) {
        if (Double.isNaN(teff) || Double.isNaN(RpOverRstar) || RpOverRstar <= 0) return "NaN";
        if (teff < tStar[tStar.length - 1]) return "<" + twoPlaces.format(RpOverRstar * rStarJ[tStar.length - 1]);
        if (teff > tStar[0]) return ">" + twoPlaces.format(RpOverRstar * rStarJ[0]);
        return twoPlaces.format(RpOverRstar * getRstarJFromTeff.interpolate(teff));
    }

    public static double getJminusKFromTeff(double teff) {
        if (Double.isNaN(teff)) return JminusK[JminusK.length - 1];
        if (teff < tStar[tStar.length - 1]) return JminusK[JminusK.length - 1];
        if (teff > tStar[0]) return JminusK[0];
        return getJminusKFromTeff.interpolate(teff);
    }

    public static double getTeffFromJminusK(double jminusk) {
        if (Double.isNaN(jminusk)) return tStar[tStar.length - 1];
        if (jminusk > JminusK[JminusK.length - 1]) return tStar[tStar.length - 1];
        if (jminusk < JminusK[0]) return tStar[0];
        return getTeffFromJminusK.interpolate(jminusk);
    }

    public static double getMStarFromTeff(double teff) {
        if (Double.isNaN(teff)) return mStar[mStar.length - 1];
        if (teff < tStar[tStar.length - 1]) return mStar[mStar.length - 1];
        if (teff > tStar[0]) return mStar[0];
        return getMStarFromTeff.interpolate(teff);
    }

    public static double getTeffFromMStar(double mstar) {
        if (Double.isNaN(mstar)) return tStar[tStar.length - 1];
        if (mstar > mStar[0]) return tStar[0];
        if (mstar < mStar[mStar.length - 1]) return tStar[tStar.length - 1];
        return getTeffFromMStar.interpolate(mstar);
    }

    public static double getRStarFromTeff(double teff) {
        if (Double.isNaN(teff)) return rStar[rStar.length - 1];
        if (teff < tStar[tStar.length - 1]) return rStar[rStar.length - 1];
        if (teff > tStar[0]) return rStar[0];
        return getRStarFromTeff.interpolate(teff);
    }

    public static double getTeffFromRStar(double rstar) {
        if (Double.isNaN(rstar)) return tStar[tStar.length - 1];
        if (rstar > rStar[0]) return tStar[0];
        if (rstar < rStar[rStar.length - 1]) return tStar[tStar.length - 1];
        return getTeffFromRStar.interpolate(rstar);
    }

    public static double getRhoStarFromTeff(double teff) {
        if (Double.isNaN(teff)) return rhoStar[rhoStar.length - 1];
        if (teff < tStar[tStar.length - 1]) return rhoStar[rhoStar.length - 1];
        if (teff > tStar[0]) return rhoStar[0];
        return getRhoStarFromTeff.interpolate(teff);
    }

    public static double getTeffFromRhoStar(double rhostar) {
        if (Double.isNaN(rhostar)) return tStar[tStar.length - 1];
        if (rhostar > rhoStar[rhoStar.length - 1]) return tStar[tStar.length - 1];
        if (rhostar < rhoStar[0]) return tStar[0];
        return getTeffFromRhoStar.interpolate(rhostar);
    }

    public static boolean backupAllAIJSettings(boolean fromImage) {
        if (fromImage) {
            if (!IJ.showMessageWithCancel("Close All AIJ Control Panels", "All AIJ windows except the AIJ Toolbar and image windows (e.g. Multi-plot, Data Processor, etc.)\n" +
                    "must be closed to enable capture of all current settings into the backup file.\n\n" +
                    "Press CANCEL to abort the backup, or press OK to continue.")) {
                return false;
            } else if (!IJ.showMessageWithCancel("Close AIJ Images and Control Panels", "All AIJ Images and control panels, except the AIJ Toolbar and Multi-plot Panels,\n" +
                    "must be closed to enable capture of all current settings into the backup file.\n\n" +
                    "Press CANCEL to abort the backup, or press OK to continue.")) {
                return false;
            }
        }
        Prefs.savePreferences();

        String prevDirectory = OpenDialog.getDefaultDirectory();
        SaveDialog sf = new SaveDialog("Backup all AIJ Preferences to File", Prefs.getPrefsDir(), "AIJ_Prefs_userbackup", ".txt");
        if (sf.getDirectory() == null || sf.getFileName() == null) {
            if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
            return false;
        }
        if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
        String path = sf.getDirectory() + sf.getFileName();
        try {
            Prefs.savePrefs(Prefs.ijPrefs, path);
        } catch (IOException ioe) {
            IJ.showMessage("Error backing up preferences to file " + path);
            return false;
        }
        return true;
    }

    public static boolean restoreAllAIJSettings() {
        String prevDirectory = OpenDialog.getDefaultDirectory();
        OpenDialog.setDefaultDirectory(Prefs.getPrefsDir());
        OpenDialog of = new OpenDialog("Restore all AIJ Preferences From Backup File", "");
        if (of.getDirectory() == null || of.getFileName() == null) {
            if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
            return false;
        }
        String autoBackupName = "AIJ_Prefs_autobackup.txt";
        if ((of.getDirectory() + of.getFileName()).equals(Prefs.getPrefsDir() + separator + autoBackupName)) {
            autoBackupName = "AIJ_Prefs_autobackup2.txt";
        }

        try {
            Prefs.savePrefs(Prefs.ijPrefs, Prefs.getPrefsDir() + separator + autoBackupName);
        } catch (IOException ioe) {
            if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
            IJ.showMessage("Error creating autobackup of current preferences to file:\n" + Prefs.getPrefsDir() + separator + autoBackupName);
            return false;
        }

        Prefs.loadPrefs(of.getDirectory() + of.getFileName());
        if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
        Prefs.savePreferences();
        if (IJ.showMessageWithCancel("Closing AIJ to Activate New Preferences",
                "AIJ will now close. New preference settings will be activated the next time AIJ is started.\n" +
                        "The previous preference settings have been backed up to '" + autoBackupName + "'.\n" +
                        "Press CANCEL to abort closing of AIJ (some new preferences may not be activated properly).")) {
            System.exit(0);
        }
        return true;
    }

    public static boolean restoreDefaultAIJSettings(boolean fromImageDisplay) {
        if (fromImageDisplay) {
            if (!IJ.showMessageWithCancel("Close All AIJ Control Panels", "All AIJ windows except the AIJ Toolbar and image windows (e.g. Multi-plot, Data Processor, etc.)\n" +
                    "must be closed to enable capture of all current settings into the backup file.\n\n" +
                    "Press CANCEL to abort the backup, or press OK to continue.")) {
                return false;
            } else if (!IJ.showMessageWithCancel("Close AIJ Images and Control Panels", "All AIJ Images and control panels, except the AIJ Toolbar and Multi-plot Panels,\n" +
                    "must be closed to enable capture of all current settings into the backup file.\n\n" +
                    "Press CANCEL to abort the backup, or press OK to continue.")) {
                return false;
            }
        }
        Prefs.savePreferences();
        String prevDirectory = OpenDialog.getDefaultDirectory();
        SaveDialog sf = new SaveDialog("Backup all current AIJ Preferences to File", Prefs.getPrefsDir(), "AIJ_Prefs_autobackup_00", ".txt");
        if (sf.getDirectory() == null || sf.getFileName() == null) {
            if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
            return false;
        }
        if (prevDirectory != null) OpenDialog.setDefaultDirectory(prevDirectory);
        String path = sf.getDirectory() + sf.getFileName();
        try {
            Prefs.savePrefs(Prefs.ijPrefs, path);
        } catch (IOException ioe) {
            IJ.showMessage("Error backing up preferences to file " + path);
            return false;
        }
        File prefsFile = new File(Prefs.getPrefsDir() + separator + "AIJ_Prefs.txt");
        if (prefsFile.isFile()) {
            prefsFile.delete();
        }
        if (IJ.showMessageWithCancel("Closing AIJ to Activate Default Preferences",
                "AIJ will now close. Default preference settings will be activated the next time AIJ is started.\n" +
                        "The previous preference settings have been backed up to '" + path + "'.\n" +
                        "Press CANCEL to abort closing of AIJ (some default preferences may not be activated properly).")) {
            System.exit(0);
        }
        return true;
    }

    public static void openRaDecApertures() {
        openRaDecApertures(null);
    }

    public static void openRaDecApertures(String path) {
        BufferedReader in = null;
        String apsPath;
        if (path == null) {
            OpenDialog of = new OpenDialog("Import apertures from RA/DEC list", "");
            if (of.getDirectory() == null || of.getFileName() == null)
                return;
            apsPath = of.getDirectory() + of.getFileName();
            if (apsPath.trim().equals(""))
                return;
        } else {
            apsPath = path.trim();
        }
        if (apsPath.equals("")) {
            IJ.beep();
            IJ.showMessage("Bad file path when opening ra/dec list. Aborting.");
            return;
        }
        List<Double> xpos = new ArrayList<Double>();
        List<Double> ypos = new ArrayList<Double>();
        List<Double> absMags = new ArrayList<Double>();
        List<Double> ras = new ArrayList<Double>();
        List<Double> decs = new ArrayList<Double>();
//        List<String> names = new ArrayList<String>();
        List<Boolean> isRefs = new ArrayList<Boolean>();
        List<Boolean> isCentroids = new ArrayList<Boolean>();

        ImagePlus imp = getBestOpenAstroImage();
        ImageWindow win = null;
        AstroStackWindow asw = null;
        AstroCanvas ac = null;
        WCS wcs = null;
        boolean hasWCS = false;

        if (imp != null) {
            win = imp.getWindow();
            if (win != null) {
                if (win instanceof AstroStackWindow) {
                    asw = (AstroStackWindow) win;
                    wcs = asw.getWCS();
                    if (wcs != null && wcs.hasWCS()) {
                        hasWCS = true;
                    }
                }
            }
        }
        if (asw != null) ac = (AstroCanvas) (OverlayCanvas.getOverlayCanvas(imp));

        try {
            in = new BufferedReader(new FileReader(apsPath));
            String line = in.readLine();
            if (line == null) {
                IJ.beep();
                IJ.showMessage("No Lines in file. Can not open RA/Dec apertures!");
                in.close();
                return;
            }
            while (line.startsWith("#")) {
                line = in.readLine();
                if (line == null) {
                    IJ.beep();
                    IJ.showMessage("No RA/Dec entries found in file!");
                    in.close();
                    return;
                }
            }

            String[] columns;
            double ra;
            double dec;
            double mag;
            double[] xy;
            while (line != null) {
                if (!line.startsWith("#")) {
                    columns = line.split(",");
                    if (columns.length > 1) {
                        ra = 15 * sexToDec(columns[0].trim());
                        dec = sexToDec(columns[1].trim());
                        if (!Double.isNaN(ra) && !Double.isNaN(dec)) {
                            if (hasWCS) {
                                xy = wcs.wcs2pixels(new double[]{ra, dec});
                                xpos.add(xy[0]);
                                ypos.add(xy[1]);
                            } else {
                                xpos.add(xpos.size() * 10.0);
                                ypos.add(ypos.size() * 10.0);
                            }
                            ras.add(ra);
                            decs.add(dec);
//                            names.add(columns.length > 2 ? columns[2].trim() : "");
                            isRefs.add(columns.length > 2 ?
                                    (!columns[2].trim().equals("0") && (columns[2].trim().equals("1") || (ras.size() > 1))) :
                                    (ras.size() > 1));
                            isCentroids.add(columns.length <= 3 || (!columns[3].trim().equals("0")));
                            absMags.add(columns.length > 4 ? Tools.parseDouble(columns[4].trim(), 99.999) : 99.999);
                        }
                    }
                }
                line = in.readLine();
            }
            in.close();
        } catch (Exception e) {
            try {
                in.close();
            } catch (Exception ee) {
            }
            IJ.beep();
            IJ.showMessage("Error reading Ra/Dec apertures file");
            return;
        }

        if (ras.size() < 1) {
            IJ.beep();
            IJ.showMessage("No valid RA/Dec entries found in file!");
            return;
        }
        if (ras.size() != xpos.size() || ras.size() != ypos.size() || ras.size() != decs.size() ||
                ras.size() != isRefs.size() || ras.size() != isCentroids.size() || absMags.size() != isCentroids.size()) {
            IJ.beep();
            IJ.showMessage("Inconsistent apertures after reading file!\nContact AIJ development team if you see the message.");
            return;
        }

        int nAps = ras.size();

        String xapertureString = "" + (float) xpos.get(0).doubleValue();
        String yapertureString = "" + (float) ypos.get(0).doubleValue();
        String raapertureString = uptoEightPlaces.format(ras.get(0));
        String decapertureString = uptoEightPlaces.format(decs.get(0));
        String isRefStarString = "" + isRefs.get(0);
        String isAlignStarString = "true";
        String centroidStarString = "" + isCentroids.get(0);
        String absMagString = "" + (float) absMags.get(0).doubleValue();

        if (nAps > 1) {
            for (int i = 1; i < nAps; i++) {
                xapertureString += "," + (float) xpos.get(i).doubleValue();
                yapertureString += "," + (float) ypos.get(i).doubleValue();
                raapertureString += "," + uptoEightPlaces.format(ras.get(i));
                decapertureString += "," + uptoEightPlaces.format(decs.get(i));
                isRefStarString += "," + isRefs.get(i);
                isAlignStarString += ",true";
                centroidStarString += "," + isCentroids.get(i);
                absMagString += "," + (float) absMags.get(i).doubleValue();
            }
        }

        Prefs.set("multiaperture.xapertures", xapertureString);
        Prefs.set("multiaperture.yapertures", yapertureString);
        Prefs.set("multiaperture.raapertures", raapertureString);
        Prefs.set("multiaperture.decapertures", decapertureString);
        Prefs.set("multiaperture.isrefstar", isRefStarString);
        Prefs.set("multiaperture.centroidstar", centroidStarString);
        Prefs.set("multiaperture.isalignstar", isAlignStarString);
        Prefs.set("multiaperture.absmagapertures", absMagString);
        Prefs.set("multiaperture.import.xapertures", xapertureString);
        Prefs.set("multiaperture.import.yapertures", yapertureString);
        Prefs.set("multiaperture.import.raapertures", raapertureString);
        Prefs.set("multiaperture.import.decapertures", decapertureString);
        Prefs.set("multiaperture.import.isrefstar", isRefStarString);
        Prefs.set("multiaperture.import.centroidstar", centroidStarString);
        Prefs.set("multiaperture.import.isalignstar", isAlignStarString);
        Prefs.set("multiaperture.import.absmagapertures", absMagString);
        MultiAperture_.apLoading.set(MultiAperture_.ApLoading.IMPORTED);
        Prefs.set("multiaperture.usewcs", true);

        if (ac != null) {
            ac.removeApertureRois();
            double radius = Prefs.get("aperture.radius", 20);
            double rBack1 = Prefs.get("aperture.rback1", 30);
            double rBack2 = Prefs.get("aperture.rback2", 40);
            boolean showSkyOverlay = Prefs.get("aperture.skyoverlay", false);
            boolean nameOverlay = true;
            boolean valueOverlay = true;

            for (int i = 0; i < nAps; i++) {
                ApertureRoi roi = new ApertureRoi(xpos.get(i), ypos.get(i), radius, rBack1, rBack2, Double.NaN, isCentroids.get(i));
                roi.setAppearance(true, isCentroids.get(i), showSkyOverlay, nameOverlay, valueOverlay, isRefs.get(i) ? Color.PINK : new Color(196, 222, 155), (isRefs.get(i) ? "C" : "T") + (i + 1), Double.NaN);
                roi.setAMag(absMags.get(i));
                roi.setImage(imp);
                ac.add(roi);
            }
            ac.paint(ac.getGraphics());
        }
    }

    /**
     * Saves new aperture locations to preferences.
     */
    public static void updateApertures(ImagePlus imp) {
        var oc = OverlayCanvas.getOverlayCanvas(imp);
        var hasWCS = false;
        WCS wcs = null;
        if (imp.getWindow() instanceof astroj.AstroStackWindow asw) {
            var ac = (AstroCanvas) oc;
            hasWCS = asw.hasWCS();
            if (hasWCS) wcs = asw.getWCS();
        }
        var apertureRois = Arrays.stream(oc.getRois()).filter(roi -> roi instanceof ApertureRoi).toArray(ApertureRoi[]::new);
        var nApertures = apertureRois.length;

        StringBuilder ra = new StringBuilder();
        StringBuilder dec = new StringBuilder();
        StringBuilder amag = new StringBuilder();
        StringBuilder isref = new StringBuilder();
        StringBuilder centroid = new StringBuilder();
        uptoEightPlaces.setDecimalFormatSymbols(IJU.dfs);
        for (int i = 0; i < nApertures; i++) {
            if (i == 0) {
                amag.append((float) apertureRois[i].getAMag());
                if (hasWCS) {
                    var raDec = wcs.pixels2wcs(new double[]{apertureRois[i].xPos, apertureRois[i].yPos});
                    ra.append(uptoEightPlaces.format(raDec[0]));
                    dec.append(uptoEightPlaces.format(raDec[1]));
                }
                isref.append(apertureRois[i].getIsRefStar());
                centroid.append(apertureRois[i].getIsCentroid());
            } else {
                amag.append(",").append((float) apertureRois[i].getAMag());
                if (hasWCS) {
                    var raDec = wcs.pixels2wcs(new double[]{apertureRois[i].xPos, apertureRois[i].yPos});
                    ra.append(",").append(uptoEightPlaces.format(raDec[0]));
                    dec.append(",").append(uptoEightPlaces.format(raDec[1]));
                }
                isref.append(",").append(apertureRois[i].getIsRefStar());
                centroid.append(",").append(apertureRois[i].getIsCentroid());
            }
        }
        if (nApertures > 0) {
            IJ.showStatus("saving new aperture locations");

            Prefs.set("multiaperture.raapertures", ra.toString());
            Prefs.set("multiaperture.decapertures", dec.toString());
            Prefs.set("multiaperture.absmagapertures", amag.toString());
            Prefs.set("multiaperture.isrefstar", isref.toString());
            Prefs.set("multiaperture.centroidstar", centroid.toString());
        }
    }

    public static void saveRaDecApertures() {
        if ("CA".equals(Prefs.get("multiaperture.lastrun", ""))) {
            IJ.error("RADEC Export", "RADEC export not supported for custom apertures");
            return;
        }

        int nAps = 0;

        String raapertureString = Prefs.get("multiaperture.raapertures", "");
        String decapertureString = Prefs.get("multiaperture.decapertures", "");
        String isRefStarString = Prefs.get("multiaperture.isrefstar", "");
        String centroidStarString = Prefs.get("multiaperture.centroidstar", "");
        String absMagString = Prefs.get("multiaperture.absmagapertures", "");
        if (raapertureString.trim().equals("") || decapertureString.trim().equals("")) {
            IJ.beep();
            IJ.showMessage("No valid RA/Dec coordinates to save. Aborting.");
            return;
        }
        String[] raaps = raapertureString.split(",");
        String[] decaps = decapertureString.split(",");
        String[] isRefStar = isRefStarString.split(",");
        String[] centroidStar = centroidStarString.split(",");
        String[] absMags = absMagString.split(",");
        double[] ra;
        double[] dec;
        boolean[] isRef;
        boolean[] isCentroid;
        double[] absMag;
        if (raaps.length == 0 || decaps.length == 0) {
            IJ.beep();
            IJ.showMessage("No RA/Dec coordinates to save. Aborting.");
            return;
        }
        if (raaps.length != decaps.length) {
            IJ.beep();
            IJ.showMessage("Error: The number of stored RA and Dec coordinates is different. Aborting.");
            return;
        }

        ra = extractDoubles(raaps);
        if (ra == null) {
            IJ.beep();
            IJ.showMessage("RA coordinate parse error. Aborting.");
            return;
        }
        dec = extractDoubles(decaps);
        if (dec == null) {
            IJ.beep();
            IJ.showMessage("Dec coordinate parse error. Aborting.");
            return;
        }

        nAps = ra.length;

        if (nAps != isRefStar.length) {
            isRef = new boolean[nAps];
            for (int ap = 0; ap < nAps; ap++) {
                isRef[ap] = ap != 0;
            }
        } else {
            isRef = extractBoolean(isRefStar);
        }

        if (nAps != centroidStar.length) {
            isCentroid = new boolean[nAps];
            for (int ap = 0; ap < nAps; ap++) {
                isCentroid[ap] = true;
            }
        } else {
            isCentroid = extractBoolean(centroidStar);
        }

        if (nAps != absMags.length) {
            absMag = new double[nAps];
            for (int ap = 0; ap < nAps; ap++) {
                absMag[ap] = 99.999;
            }
        } else {
            absMag = extractAbsMagDoubles(absMags);
        }

        ImagePlus imp = WindowManager.getCurrentImage();
        String defaultFileName = (imp != null ? IJU.getSliceFilename(imp) : "apertures.radec");

        SaveDialog sf = new SaveDialog("Export apertures as Ra/Dec", defaultFileName, ".radec");
        if (sf.getDirectory() == null || sf.getFileName() == null)
            return;

        String savePath = sf.getDirectory() + sf.getFileName();

        File outFile = new File(savePath);
        if (outFile.isDirectory()) {
            IJ.beep();
            IJ.showMessage("Bad RA/Dec save filename. Aborting.");
            return;
        }
        if (outFile.isFile()) outFile.delete();
        PrintWriter pw = null;
        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            pw = new PrintWriter(bos);
            String line = "";

            line = "#RA in decimal or sexagesimal HOURS";
            pw.println(line);
            line = "#Dec in decimal or sexagesimal DEGREES";
            pw.println(line);
            line = "#Ref Star=0,1,missing (0=target star, 1=ref star, missing->first ap=target, others=ref)";
            pw.println(line);
            line = "#Centroid=0,1,missing (0=do not centroid, 1=centroid, missing=centroid)";
            pw.println(line);
            line = "#Apparent Magnitude or missing (value = apparent magnitude, or value > 99 or missing = no mag info)";
            pw.println(line);
            line = "#Add one comma separated line per aperture in the following format:";
            pw.println(line);
            line = "#RA, Dec, Ref Star, Centroid, Magnitude";
            pw.println(line);

            for (int ap = 0; ap < nAps; ap++) {
                line = decToSex(ra[ap] / 15.0, 3, 24, false) + ", " + decToSex(dec[ap], 2, 90, true) + ", " + (isRef[ap] ? "1" : "0") + ", " + (isCentroid[ap] ? "1" : "0") + ", " + absMag[ap];
                pw.println(line);
            }
        } catch (IOException ioe) {
            IJ.beep();
            IJ.showMessage("Error writing aperture RA/Dec file");
        } finally {
            if (pw != null) pw.close();
        }

    }


    public static void showInSIMBAD(double ra, double dec, double searchRadius) {
        boolean useHarvard = Prefs.get("coords.useHarvard", true);
        try {
            String queryType = "sim-fbasic";
            String object = "";
            queryType = "sim-coo?Coord=";
            object = URLEncoder.encode(decToSex(ra / 15.0, 3, 24, false) + decToSex(dec, 2, 90, true), StandardCharsets.UTF_8);
            object += "&Radius=" + searchRadius + "&Radius.unit=arcsec";

            URI simbad;
            if (useHarvard)
                simbad = new URI("http://simbad.cfa.harvard.edu/simbad/" + queryType + object);
            else
                simbad = new URI("http://simbad.u-strasbg.fr/simbad/" + queryType + object);

            BrowserOpener.openURL(simbad.toString());
        } catch (Exception e) {
            IJ.showMessage("SIMBAD access error", "<html>" + "Could not open link to Simbad " + (useHarvard ? "at Harvard." : "in France.") + "<br>" +
                    "Check internet connection or" + "<br>" +
                    "try " + (useHarvard ? "France" : "Harvard") + " server (see Preferences menu in Coordinate Converter)." + "</html>");
        }
    }


    public static void updateApMags(int nAps, AstroCanvas ac) {
        double totRefMag = 0.0;             //calculate total reference star magnitude
        double totRefCnts = 0.0;
        int numRefMags = 0;
        ApertureRoi aRoi = null;
        for (int ap = 0; ap < nAps; ap++) {
            aRoi = ac.findApertureRoiByNumber(ap);
            if (aRoi != null && aRoi.getIsRefStar() && aRoi.getAMag() < 99.0 && !Double.isNaN(aRoi.getIntCnts())) {
                numRefMags++;
                totRefMag += Math.pow(2.512, -aRoi.getAMag());
                totRefCnts += aRoi.getIntCnts();

            }
        }
        if (numRefMags > 0)                    //recalculate target star magnitude(s)
        {
            totRefMag = -Math.log(totRefMag) / Math.log(2.512);
        }
        for (int ap = 0; ap < nAps; ap++) {
            aRoi = ac.findApertureRoiByNumber(ap);
            if (aRoi != null && !aRoi.getIsRefStar()) {
                if (numRefMags > 0 && !Double.isNaN(aRoi.getIntCnts())) {
                    aRoi.setAMag(totRefMag - 2.5 * Math.log10(aRoi.getIntCnts() / totRefCnts));
                } else {
                    aRoi.setAMag(99.999);
                }
            }
        }
    }

    /**
     * Extracts a double array from a string array.
     */
    public static double[] extractDoubles(String[] s) {
        double[] arr = new double[s.length];
        try {
            for (int i = 0; i < arr.length; i++)
                arr[i] = Double.parseDouble(s[i]);
        } catch (NumberFormatException e) {
            arr = null;
        }
        return arr;
    }

    /**
     * Extracts a double array from a string array and returns 99.999 as NaN.
     */
    public static double[] extractAbsMagDoubles(String[] s) {
        if (s == null || s.length < 1) return null;
        double[] arr = new double[s.length];

        for (int i = 0; i < arr.length; i++)
            arr[i] = Tools.parseDouble(s[i], 99.999);

        return arr;
    }

    /**
     * Extracts a boolean array from a string array.
     */
    public static boolean[] extractBoolean(String[] s) {
        boolean[] arr = new boolean[s.length];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = s[i].equalsIgnoreCase("true");
        }
        return arr;
    }


    public static double sexToDec(String text) {
        double X = Double.NaN;
        boolean XNegative = false;
        String[] pieces = text.replace("-", " -").replaceAll("[^0-9\\.\\-]{1,}", " ").trim().split("[^0-9\\.\\-]{1,}");
        if (pieces.length > 0) {
            X = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(X) && pieces[0].contains("-")) {
                X = -X;
                XNegative = true;
            }
            if (pieces.length > 1) X += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
            if (pieces.length > 2) X += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
        }

        if (XNegative) X = -X;
        return X;
    }

    public static String decToSexRA(double d) {
        return decToSex(d / 15.0, 3, 24, false, false, false);
    }

    public static String decToSexDec(double d) {
        return decToSex(d, 2, 90, true, false, false);
    }

    public static String decToSexDeg(double d, int fractionPlaces, Boolean trimZeros) {
        return decToSex(d, fractionPlaces, 360, false, true, trimZeros);
    }

    public static String decToSex(double d, int fractionPlaces, int base, Boolean showPlus) {
        return decToSex(d, fractionPlaces, base, showPlus, false, false);
    }

    public static String decToSex(double d, int fractionPlaces, int base, Boolean showPlus, Boolean showDegMinSecSymbols, Boolean trimZeros) {
        DecimalFormat nf = new DecimalFormat();
        DecimalFormat nf2x = new DecimalFormat();
        nf.setDecimalFormatSymbols(IJU.dfs);
        nf2x.setDecimalFormatSymbols(IJU.dfs);
        nf.setMinimumIntegerDigits(2);
        nf2x.setMinimumIntegerDigits(2);
        nf2x.setMinimumFractionDigits(fractionPlaces);
        nf2x.setMaximumFractionDigits(fractionPlaces);

        boolean ampm = false;
        boolean pm = false;
        if (base == 1224) {
//            base = 12;
            ampm = true;
            if (d >= 12.0) {
                d -= 12.0;
                pm = true;
            }
        }

        double dd = Math.abs(d);
//        dd += 0.0000001;

        int h = (int) dd;
        int m = (int) (60.0 * (dd - (double) h));
        double s = 3600.0 * (dd - (double) h - (double) m / 60.0);

        if (Tools.parseDouble(nf2x.format(s)) >= 60.0) {
            s = 0.0;
            m += 1;
        }
        if (m > 59) {
            m -= 60;
            h += 1;
        }
        if (d > 0 && h >= base) {
            if (base == 180 || (base == 12 && !ampm)) {
                d = -d;
                if (s != 0) {
                    s = 60 - s;
                    m = 59 - m;
                    h--;
                } else if (m != 0) {
                    m = 59 - m;
                    h--;
                }
            } else if (base == 12 && ampm) {
                h -= base;
                pm = !pm;
            } else if (base == 90) {
                h = 90;
                m = 0;
                s = 0;
            } else
                h -= base;
        } else if (base == 90 && d < -90) {
            h = 90;
            m = 0;
            s = 0;
        }

        if (ampm && h == 0) h = 12;
        String str = "";
        if (d < 0.0) str = "-";
        else if (showPlus) str = "+";
        if (showDegMinSecSymbols)
            str += "" + (trimZeros && h == 0.0 ? "" : nf.format(h) + "\u00B0") + (trimZeros && m == 0 ? "" : nf.format(m) + "\u2032") + nf2x.format(s) + "\u2033";
        else
            str += "" + nf.format(h) + ":" + nf.format(m) + ":" + nf2x.format(s);
        if (ampm) str += pm ? " PM" : " AM";
        return str;
    }

    static public ImagePlus getBestOpenAstroImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        ImageWindow iw = null;

        if (imp != null) {
            iw = imp.getWindow();
            if (iw != null && iw instanceof AstroStackWindow) {
                WindowManager.toFront(iw);
                return imp;
            } else if (WindowManager.getWindowCount() > 0) {
                int[] ID = WindowManager.getIDList();
                for (int win = 0; win < ID.length; win++) {
                    imp = WindowManager.getImage(ID[win]);
                    if (imp != null)
                        iw = imp.getWindow();
                    else
                        iw = null;
                    if (iw != null && iw instanceof AstroStackWindow) {
                        WindowManager.toFront(iw);
                        return imp;
                    }
                }
            }
        }
        return null;
    }

    static public AstroStackWindow getBestOpenAstroStackWindow() {
        ImagePlus imp = WindowManager.getCurrentImage();
        ImageWindow iw = null;

        if (imp != null) {
            iw = imp.getWindow();
            if (iw != null && iw instanceof AstroStackWindow) {
                WindowManager.toFront(iw);
                return (AstroStackWindow) iw;
            } else if (WindowManager.getWindowCount() > 0) {
                int[] ID = WindowManager.getIDList();
                for (int win = 0; win < ID.length; win++) {
                    imp = WindowManager.getImage(ID[win]);
                    if (imp != null)
                        iw = imp.getWindow();
                    else
                        iw = null;
                    if (iw != null && iw instanceof AstroStackWindow) {
                        WindowManager.toFront(iw);
                        return (AstroStackWindow) iw;
                    }
                }
            }
        }
        return null;
    }


    public static double getTextSpinnerDoubleValue(JSpinner spinner) {
//        boolean debug = true;
//        DecimalFormat df = new DecimalFormat("0.0000", dfs);
//        DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
//        char decSep = symbols.getDecimalSeparator(); 
//        char thouSep = symbols.getGroupingSeparator();  
        String text = (String) spinner.getModel().getValue();
//        if (debug) IJ.log("Spinner Original Text = "+text);
//        text = text.replace(""+thouSep, "").replace(decSep, '.').trim();
//        if (debug) IJ.log("Spinner Modified Text = "+text);
        return Tools.parseDouble(text.trim());
    }

    public static double getTextFieldDoubleValue(JTextField textField) {
//        boolean debug = true;
//        DecimalFormat df = new DecimalFormat("0.0000",dfs);
//        DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
//        char decSep = symbols.getDecimalSeparator(); 
//        char thouSep = symbols.getGroupingSeparator();  
        String text = textField.getText().trim();
//        if (debug) IJ.log("Spinner Original Text = "+text);
//        text = text.replace(""+thouSep, "").replace(decSep, '.').trim();
//        if (debug) IJ.log("Spinner Modified Text = "+text);
        return Tools.parseDouble(text);
//        if (Double.isNaN(value)) 
//            {
////            if (debug) IJ.log("Value is NaN");
//            textField.setText("0"+decSep+"0");
//            value = 0.0;
//            }
//        if (debug) IJ.log("Value = "+value);
//        return value;
    }

    public static String getSliceFilename(ImagePlus imp, int slice) {
        String filename = imp.getStack().getSliceLabel(slice);
        if (filename == null) {
            filename = imp.getTitle().trim();
        } else {
            int newline = filename.indexOf('\n');
            if (newline != -1) filename = filename.substring(0, newline);
        }
        return filename.trim();
    }

    public static String getSliceFilename(ImagePlus imp) {
        return getSliceFilename(imp, imp.getCurrentSlice());
    }

    public static boolean saveFile(ImagePlus imp, String path) {
        return saveFile(imp, path, false, false, "");
    }

    public static void replaceImageInWindow(ImagePlus imp, String imageName) {
        ImagePlus openImp = WindowManager.getImage(imageName);
        ImageProcessor ip = imp.getProcessor();
        Frame openFrame = null;
        Properties props;
        Enumeration<?> enProps;
        String key;
        FileInfo imf;
        if (openImp != null) {
            openFrame = WindowManager.getFrame(imageName);

            imf = imp.getFileInfo();
            openImp.setFileInfo(imf);

            //CLEAR PROPERTIES FROM OPENIMAGE
            props = openImp.getProperties();
            if (props != null) {
                enProps = props.propertyNames();
                key = "";
                while (enProps.hasMoreElements()) {
                    key = (String) enProps.nextElement();
                    openImp.setProperty(key, null);
                }
            }
            // COPY NEW PROPERTIES TO OPEN WINDOW IMAGEPLUS
            props = imp.getProperties();
            if (props != null) {
                enProps = props.propertyNames();
                key = "";
                while (enProps.hasMoreElements()) {
                    key = (String) enProps.nextElement();
                    openImp.setProperty(key, props.getProperty(key));
                }
            }
            if (imp.getType() == ImagePlus.COLOR_RGB) {
                imp.setDisplayRange(0, 255);
                ip.snapshot();
            }
            if (openFrame != null && openFrame instanceof astroj.AstroStackWindow) {
                astroj.AstroStackWindow asw = (astroj.AstroStackWindow) openFrame;
                asw.setUpdatesEnabled(false);
                IJ.wait(10);
            }
            openImp.setProcessor(ip);
            if (openFrame != null && openFrame instanceof astroj.AstroStackWindow) {
                astroj.AstroStackWindow asw = (astroj.AstroStackWindow) openFrame;
                asw.setUpdatesEnabled(true);
                asw.setAstroProcessor(true);
            }
        } else {
            imp.show();
            if (imp.getType() == ImagePlus.COLOR_RGB) {
                imp.setDisplayRange(0, 255);
                ip.snapshot();
            }
        }
    }

    public static boolean saveFile(ImagePlus imp, String path, boolean showLog, boolean showLogDateTime, String logFilenameDescription) {
        if (!logFilenameDescription.equals("")) logFilenameDescription = logFilenameDescription + " ";
        boolean compress = false;
        String savePath = path.trim();
        if (path.endsWith(".gz")) {
            int dotIndex = path.lastIndexOf(".");
            path = path.substring(0, dotIndex);
            compress = true;
        }

        if (imp != null) {
            if (FitsExtensionUtil.isFitsFile(path)) {
                IJ.runPlugIn(imp, "ij.plugin.FITS_Writer", path + (compress ? ".gz" : ""));
                compress = false; // FITS_Writer already compresses
            } else {
                IJ.save(imp, path);
            }
            if (compress) {
                File infile = new File(path);
                File outfile = new File(path + ".gz");
                if (infile.exists()) {
                    if (outfile.exists()) outfile.delete();
                    try {
                        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(path + ".gz"));
                        FileInputStream in = new FileInputStream(path);

                        // Transfer bytes from the input file to the GZIP output stream
                        byte[] buf = new byte[2880];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();

                        // Complete the GZIP file
                        out.finish();
                        out.close();
                        infile.delete();
                        if (showLog)
                            log("Compressed and saved " + logFilenameDescription + "file \"" + savePath + "\"", showLogDateTime);
                    } catch (IOException e) {
                        IJ.showMessage("ERROR: IO exception while creating GZIP compressed file \"" + savePath + "\". Save aborted");
                        return false;
                    }
                }
            } else {
                if (showLog) log("Saved " + logFilenameDescription + "file \"" + savePath + "\"", showLogDateTime);
            }
        } else {
            IJ.showMessage("ERROR: image data was null prior to saving file \"" + savePath + "\". Save aborted");
            return false;
        }
        return true;
    }

    public static void saveAsPngOrJpg(BufferedImage image, File filename, String format) {
        try {
            if (format.equalsIgnoreCase("jpg")) {
                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
                javax.imageio.ImageWriter writer = iter.next();
                ImageWriteParam iwp = writer.getDefaultWriteParam();
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionQuality(1.0f);   // an integer between 0 and 1 where 1 specifies minimum compression and maximum quality
                FileImageOutputStream output = new FileImageOutputStream(filename);
                writer.setOutput(output);
                IIOImage imageOut = new IIOImage(image, null, null);
                writer.write(null, imageOut, iwp);
                output.close();
                writer.dispose();
            } else if (format.equalsIgnoreCase("png")) {
                ImageIO.write(image, format, filename);
            } else {
                IJ.error("bad image save format '" + format + "'");
            }

        } catch (IOException ex) {
            IJ.error("File Write Error", "Error writing image display to file '" + filename.toString() + "'");
        }
    }

    public static ImagePlus getBestAstroImage() {
        ImagePlus iplus = WindowManager.getCurrentImage();
        ImageWindow iw = null;

        if (iplus != null) {
            iw = iplus.getWindow();
            if (iw != null && iw instanceof AstroStackWindow) {
                WindowManager.toFront(iw);
            } else if (WindowManager.getWindowCount() > 0) {
                int[] ID = WindowManager.getIDList();
                for (int win = 0; win < ID.length; win++) {
                    iplus = WindowManager.getImage(ID[win]);
                    if (iplus != null)
                        iw = iplus.getWindow();
                    else
                        iw = null;
                    if (iw != null && iw instanceof AstroStackWindow) {
                        WindowManager.setCurrentWindow(iw);
                        WindowManager.toFront(iw);
                        return iplus;
                    }
                }
                iplus = null;
            } else {
                iplus = null;
            }
        }
        return iplus;
    }

    static void log(String message) {
        log(message, false);
    }

    static void log(String message, boolean showLogDateTime) {
        if (showLogDateTime) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            Calendar cal = Calendar.getInstance();
            IJ.log("[" + sdf.format(cal.getTime()) + "]      " + message);
        } else {
            IJ.log(message);
        }
    }

    public synchronized static <T extends Window> void setFrameSizeAndLocation(T frame, int defaultX, int defaultY, int defaultWidth, int defaultHeight) {
        setFrameSizeAndLocation(frame, defaultX, defaultY, defaultWidth, defaultHeight, true);
    }

    public synchronized static <T extends Window> void setFrameSizeAndLocation(T frame, int defaultX, int defaultY, int defaultWidth, int defaultHeight, boolean enforceScreenBounds) {
        if (frame == null) {
            return;
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        GraphicsDevice gd = null;
        Rectangle screenBounds = new Rectangle();
        boolean foundScreen = false;
        for (GraphicsDevice graphicsDevice : gds) {
            gd = graphicsDevice;
            screenBounds.setRect(gd.getDefaultConfiguration().getBounds());
            if (screenBounds.contains(defaultX, defaultY)) {
                foundScreen = true;
                break;
            }
        }
        if (!foundScreen) {
            gd = ge.getDefaultScreenDevice();
            screenBounds.setRect(gd.getDefaultConfiguration().getBounds());
        }

        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        screenBounds.x += insets.left;
        screenBounds.y += insets.top;
        screenBounds.width -= insets.left + insets.right;
        screenBounds.height -= insets.top + insets.bottom;
        if (enforceScreenBounds && frame.getHeight() > screenBounds.height) {
            frame.setSize(frame.getWidth() + (Integer) UIManager.get("ScrollBar.width"), screenBounds.height);
        }
        if (enforceScreenBounds && frame.getWidth() > screenBounds.width) {
            frame.setSize(screenBounds.width, frame.getHeight());
        }
        if (!screenBounds.contains(defaultX, defaultY)) {
            defaultX = screenBounds.x + screenBounds.width / 2 - frame.getWidth() / 2;
            defaultY = screenBounds.y + screenBounds.height / 2 - frame.getHeight() / 2;
        }
        if (enforceScreenBounds && (defaultWidth > 0 && defaultWidth < screenBounds.width)) {
            frame.setSize(defaultWidth, frame.getHeight());
        }
        if (enforceScreenBounds && (defaultHeight > 0 && defaultHeight < screenBounds.height)) {
            frame.setSize(frame.getWidth(), defaultHeight);
        }
        frame.setLocation(defaultX, defaultY);
    }

    public static void saveApertures(String apsPath) {
        saveApertures(apsPath, null);
    }

    public static void saveApertures(String apsPath, AstroStackWindow astroStackWindow) {
        File outFile = new File(apsPath);
        if (outFile.isDirectory()) {
            IJ.error("bad aperture save filename");
            return;
        }
        if (outFile.isFile()) outFile.delete();
        Properties prefs = new Properties();
        if (ObjectShare.get("multiapertureKeys") instanceof Set<?> keysGeneric) {
            var keys = (Set<String>) keysGeneric;
            for (String key : keys) {
                if (Prefs.ijPrefs.containsKey(key)) {
                    prefs.put(key, Prefs.ijPrefs.getProperty(key));
                }
            }
        }

        // Custom Apertures
        if ("CA".equals(Prefs.get("multiaperture.lastrun", ""))) {
            // Remove circular setting
            if (ObjectShare.get("multiapertureCircularKeys") instanceof Set<?> keysGeneric) {
                var keys = (Set<String>) keysGeneric;
                for (String key : keys) {
                    prefs.remove(key);
                }
            }

            var data = new AperturesFile.Data(FreeformPixelApertureHandler.APS.get(), prefs);
            try {
                Files.writeString(outFile.toPath(), data.toString());
            } catch (IOException e) {
                e.printStackTrace();
                IJ.beep();
                IJ.showMessage("Error writing apertures to file");
            }
        } else {
            if (astroStackWindow != null) {
                var rois = astroStackWindow.ac.getRois();
                if (rois != null) {
                    Arrays.stream(rois)
                            .filter(r -> r instanceof ApertureRoi)
                            .findAny()
                            .ifPresent(r -> {
                                var roi = (ApertureRoi) r;
                                prefs.put(".aperture.radius", Double.toString(roi.r1));
                                prefs.put(".aperture.rback1", Double.toString(roi.r2));
                                prefs.put(".aperture.rback2", Double.toString(roi.r3));
                            });
                }
            }

            try {
                FileOutputStream fos = new FileOutputStream(apsPath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                prefs.store(bos, "AstroImageJ Saved Apertures");
                bos.close();
            } catch (IOException ioe) {
                IJ.beep();
                IJ.showMessage("Error writing apertures to file");
            }
        }
    }

    public static double radialDistributionFWHM(ImageProcessor ip, double X0, double Y0, double rFixed, double background) {
        int iterations = 0;
        boolean foundR1 = false;
        int nBins;
        double R;
        double mR = rFixed;
        float z, peak;
        double meanPeak;
        double[] radii;
        double[] means;
        int[] count;
        double fwhmRD = 0.0;
        double rRD = rFixed;
        boolean foundFWHM = false;

        while (!foundR1 && iterations < 10) {
            nBins = (int) mR;
            foundFWHM = false;
            radii = new double[nBins];
            means = new double[nBins];
            count = new int[nBins];
            meanPeak = Double.MIN_VALUE;
            peak = Float.MIN_VALUE;
            int xmin = (int) (X0 - mR);
            int xmax = (int) (X0 + mR);
            int ymin = (int) (Y0 - mR);
            int ymax = (int) (Y0 + mR);

            // ACCUMULATE ABOUT CENTROID POSITION

            for (int j = ymin; j < ymax; j++) {
                double dy = (double) j + Centroid.PIXELCENTER - Y0;
                for (int i = xmin; i < xmax; i++) {
                    double dx = (double) i + Centroid.PIXELCENTER - X0;
                    R = Math.sqrt(dx * dx + dy * dy);
                    int bin = (int) R; //Math.round((float)R);  //
                    if (bin >= nBins) continue; //bin = nBins-1;
                    z = ip.getPixelValue(i, j);
                    radii[bin] += R;
                    means[bin] += z;
                    count[bin]++;
                    if (z > peak) peak = z;
                }
            }

            for (int bin = 0; bin < nBins; bin++) {
                if (count[bin] > 0 && (means[bin] / count[bin]) > meanPeak) meanPeak = means[bin] / count[bin];
            }
            meanPeak -= background;

            // NORMALIZE

            peak -= background;
            for (int bin = 0; bin < nBins; bin++) {
                if (count[bin] > 0) {
                    means[bin] = ((means[bin] / count[bin]) - background) / meanPeak;
                    radii[bin] /= count[bin];
                } else {
                    //                IJ.log("No samples at radius "+bin);
                    means[bin] = Double.NaN;
                    radii[bin] = Double.NaN;
                }
            }


            // FIND FWHM

            for (int bin = 1; bin < nBins; bin++) {
                if (!foundFWHM && means[bin - 1] > 0.5 && means[bin] <= 0.5) {
                    if (bin + 1 < nBins && means[bin + 1] > means[bin] && bin + 2 < nBins && means[bin + 2] > means[bin])
                        continue;
                    double m = (means[bin] - means[bin - 1]) / (radii[bin] - radii[bin - 1]);
                    fwhmRD = 2.0 * (radii[bin - 1] + (0.5 - means[bin - 1]) / m);
                    foundFWHM = true;
                } else if (foundFWHM && bin < nBins - 5) {
                    if (means[bin] < 0.01) {
                        rRD = radii[bin];
                        foundR1 = true;
                        break;
                    }
                }
            }
            if (!foundR1) {
                mR += 10;
            }
            iterations++;
        }
        return foundR1 ? fwhmRD : Double.NaN;
    }

    public static double[] transitModel(double[] bjd, double f0, double inclination, double p0, double ar, double tc, double P,
                                        double e, double omega, double u1, double u2, boolean useLonAscNode, double lonAscNode, boolean fitPrimary) {
        return transitModelV1(bjd, f0, inclination, p0, ar, tc, P, e, omega, u1, u2, useLonAscNode, lonAscNode, fitPrimary);
    }

    /**
     * Based on EXOFASTv1.
     */
    private static double[] transitModelV1(double[] bjd, double f0, double inclination, double p0, double ar, double tc, double P,
                                                double e, double omega, double u1, double u2, boolean useLonAscNode, double lonAscNode, boolean fitPrimary) {
        // This routine computes the lightcurve for occultation of a
        // quadratically limb-darkened source and was derived from the EXOFAST
        // procedure exofast_occultquad (Mandel & Agol (2002); Eastman et al., (2013))
        // 2014/05/05 - Transcoded to Java by Karen Collins (University of Louisville)
        // MODIFICATION HISTORY
        // 
        //  2002 -- Eric Agol 2002
        //
        //  2009/04/06 -- Eric Agol (University of Washington) and 
        //                Jason Eastman (Ohio State University)
        //    fixed bugs for p > 1 (e.g., secondary eclipses)
        //    used double precision constants throughout
        //    replaced rj,rc,rf with ellpic_bulirsch
        //      faster, more robust integration for specialized case of
        //      elliptic integral of the third kind
        //    more efficient case handling
        //    combined ellk and ellec into ellke for speed
        //    200x speed improvement over previous IDL code in typical case
        //    allow negative values of p (anti-transit) to avoid Lucy-Sweeney like bias
        // Limb darkening has the form:
        //  I(r)=[1-u1*(1-sqrt(1-(r/rs)^2))-u2*(1-sqrt(1-(r/rs)^2))^2]/(1-u1/3-u2/6)/pi
        // 
        // INPUTS:
        //
        //    u1 - linear    limb-darkening coefficient (gamma_1 in paper)
        //    u2 - quadratic limb-darkening coefficient (gamma_2 in paper)
        //    p0 - occulting star size in units of rs
        //
        // OUTPUTS:
        //
        //    muo1 - fraction of flux at each z0 for a limb-darkened source
        //
        // OPTIONAL OUTPUTS:
        //
        //    mu0  - fraction of flux at each z0 for a uniform source
        //    d    - The coefficients required to analytically calculate the
        //           limb darkening parameters (see Eastman et al, 2013). For
        //           backward compatibility, u1 and u2 are required, but not
        //           necessary if d is used.        

        double[] muo1 = new double[bjd.length];
//        double[] mu0 = new double[bjd.length];
        double tp = tc - P * getTcPhase(e, omega);
//        IJ.log("tc="+tc);
//        IJ.log("P="+P);
//        IJ.log("TcPhase="+getTcPhase(e,omega));
//        IJ.log("tp="+tp);

        double[][] bzArray = impactParameter(bjd, inclination, ar, tp, P, e, omega, useLonAscNode, lonAscNode); // impact parameter in units of rs

        int nz = bjd.length;

        double[] lambdad = new double[nz];
        double[] etad = new double[nz];
        double[] lambdae = new double[nz];

        double z;
        double p = abs(p0);
        double p2 = p * p;

        // tolerance for double precision equalities
        // special case integrations
        double tol = 1.0e-14;
        double kap1, kap0, kapArg1, kapArg0, lambdaeArg, z2, x1, x2, x3, q, n;

        var bz0 = bzArray[0];
        var bz1 = bzArray[1];
        for (int i = 0; i < nz; i++) {
            if (fitPrimary && bz1[i] <= 0.0 || !fitPrimary && bz1[i] > 0) {
                // Only consider the part of the orbit where a primary or secondary could occur, depending on which is being modeled.
                //(When z > 0, the planet is closer to the observer than the star.)
                // Otherwise, the overall model returned will include identical transits at both the primary and secondary times of transit,
                // which would cause a problem when fitting full-phase data, such as TESS or Kepler space-based data.
                // To calculate both a primary and secondary transit model for full phase data, the model must be calculated twice using the different set
                // of transit and eclipse shape parameters and combined separately from this function.
                continue;
            }
            if (abs(p - bz0[i]) < tol) bz0[i] = p;
            else if (abs((p - 1.0) - bz0[i]) < tol) bz0[i] = p - 1.0;
            else if (abs((1.0 - p) - bz0[i]) < tol) bz0[i] = 1.0 - p;
            else if (bz0[i] < tol) bz0[i] = 0.0;
            z = bz0[i];
            z2 = z * z;
            x1 = (p - z) * (p - z);
            x2 = (p + z) * (p + z);
            x3 = p2 - z2;

            // Case 1 - the star is unocculted or there is no planet (p <= 0)
            if (z >= (1.0 + p) || p <= 0.0) {
                etad[i] = 0.0;
                lambdae[i] = 0.0;
                lambdad[i] = 0.0;
                continue;
            }

            // Case 11 - the  source is completely occulted:
            if (p >= 1.0 && z <= p - 1.0) {
                etad[i] = 0.5; // corrected typo in paper
                lambdae[i] = 1.0;
                lambdad[i] = 0.0;
                continue;
            }

            // Case 2, 7, 8 - ingress/egress (uniform disk only)
            if (z >= abs(1.0 - p) && (z < 1.0 + p)) {
                kapArg1 = (1.0 - p2 + z2) / 2.0 / z;
                if (kapArg1 < -1.0) {
                    kapArg1 = -1.0;
                    kap1 = PI;
                } else if (kapArg1 > 1.0) {
                    kapArg1 = 1.0;
                    kap1 = 0;
                } else {
                    kap1 = acos(kapArg1);
                }
                kapArg0 = (p2 + z2 - 1.0) / 2.0 / p / z;
                if (kapArg0 < -1.0) {
                    kapArg0 = -1.0;
                    kap0 = PI;
                } else if (kapArg0 > 1.0) {
                    kapArg0 = 1.0;
                    kap0 = 0;
                } else {
                    kap0 = acos(kapArg0);
                }

                lambdaeArg = 1.0 + z2 - p2;
                lambdaeArg *= lambdaeArg;
                lambdaeArg = 4.0 * z2 - lambdaeArg;
                if (lambdaeArg < 0.0) lambdaeArg = 0.0;
                lambdae[i] = (p2 * kap0 + kap1 - 0.5 * sqrt(lambdaeArg)) / PI;
                // eta_1
                etad[i] = 1.0 / 2.0 / PI * (kap1 + p2 * (p2 + 2.0 * z2) * kap0 - (1.0 + 5.0 * p2 + z2) / 4.0 * sqrt((1.0 - x1) * (x2 - 1.0)));
            }

            // Case 5, 6, 7 - the edge of planet lies at origin of star
            if (z == p) {
                if (p < 0.5) {
                    // Case 5
                    q = 2.0 * p; //corrected typo in paper (2k -> 2p)
                    var ell = ellke(q);
                    var ek = ell[0];  //The elliptic integral of the first kind
                    var kk = ell[1];  // The elliptic integral of the second kind
                    lambdad[i] = 1.0 / 3.0 + 2.0 / 9.0 / PI * (4.0 * (2.0 * p2 - 1.0) * ek + (1.0 - 4.0 * p2) * kk);
                    etad[i] = 3.0 * p2 * p2 / 2.0;
                    lambdae[i] = p2;// uniform disk
                } else if (p > 0.5) {
                    // Case 7
                    q = 0.5 / p; //corrected typo in paper (1/2k -> 1/2p)
                    var ell = ellke(q);
                    var ek = ell[0];  //The elliptic integral of the first kind
                    var kk = ell[1];  // The elliptic integral of the second kind
                    lambdad[i] = 1.0 / 3.0 + 16.0 * p / 9.0 / PI * (2.0 * p2 - 1.0) * ek - (32.0 * p2 * p2 - 20.0 * p2 + 3.0) / 9.0 / PI / p * kk;
                    // etad = eta_1 already
                } else {
                    // Case 6
                    lambdad[i] = 1.0 / 3.0 - 4.0 / PI / 9.0;
                    etad[i] = 3.0 / 32.0;
                }
                continue;
            }

            // Case 2, Case 8 - ingress/egress (with limb darkening)
            if ((z > 0.5 + abs(p - 0.5) && z < 1.0 + p) || (p > 0.5 && z > abs(1.0 - p) && z < p)) {
                q = sqrt((1.0 - x1) / (x2 - x1));
                var ell = ellke(q);
                var ek = ell[0];  //The elliptic integral of the first kind
                var kk = ell[1];  // The elliptic integral of the second kind
                n = 1.0 / x1 - 1.0;

                // lambda_1:
                lambdad[i] = 2.0 / 9.0 / PI / sqrt(x2 - x1) * (((1.0 - x2) * (2.0 * x2 + x1 - 3.0) - 3.0 * x3 * (x2 - 2.0)) * kk + (x2 - x1) *
                        (z2 + 7.0 * p2 - 4.0) * ek - 3.0 * x3 / x1 * ellpic_bulirsch(n, q));
                continue;
            }

            // Case 3, 4, 9, 10 - planet completely inside star
            if (p < 1.0 && z <= (1.0 - p)) {
                // eta_2
                etad[i] = p2 / 2.0 * (p2 + 2.0 * z2);

                // uniform disk
                lambdae[i] = p2;

                // Case 4 - edge of planet hits edge of star
                if (z == 1.0 - p) {
                    // lambda_5
                    lambdad[i] = 2.0 / 3.0 / PI * acos(1.0 - 2.0 * p) - 4.0 / 9.0 / PI * sqrt(p * (1.0 - p)) * (3.0 + 2.0 * p - 8.0 * p2) - 2.0 / 3.0 * (p > 0.5 ? 1 : 0);
                }

                // Case 10 - origin of planet hits origin of star
                else if (z == 0) {
                    // lambda_6
                    lambdad[i] = -2.0 / 3.0 * pow((1.0 - p2), 1.5);
                } else {
                    q = sqrt((x2 - x1) / (1.0 - x1));
                    n = x2 / x1 - 1.0;
                    var ell = ellke(q);
                    var ek = ell[0];  //The elliptic integral of the first kind
                    var kk = ell[1];  // The elliptic integral of the second kind

                    // Case 3, Case 9 - anywhere in between
                    // lambda_2
                    lambdad[i] = 2.0 / 9.0 / PI / sqrt(1.0 - x1) * ((1.0 - 5.0 * z2 + p2 + x3 * x3) * kk + (1.0 - x1) * (z2 + 7.0 * p2 - 4.0) * ek - 3.0 * x3 / x1 * ellpic_bulirsch(n, q));
                }
                continue;
            }
//            IJ.log("transitModel: Undefined case -- please report to Karen Collins at the AIJ support forum.");
//            IJ.log("transitModel: the impact parameter z = "+z+" does not fit any condition handled by the code.");
        }

//        var priStart = tc - P * getTcPhase(e, omega, TransitLocation.L4);
//        var priEnd = tc + P * getTcPhase(e, omega, TransitLocation.L5);
        for (int i = 0; i < nz; i++) {

            if (fitPrimary && bz1[i] <= 0.0 || !fitPrimary && bz1[i] > 0) {
                // Only consider the part of the orbit where a primary or secondary could occur, depending on which is being modeled.
                //(When z > 0, the planet is closer to the observer than the star.)
                // Otherwise, the overall model returned will include identical transits at both the primary and secondary times of transit,
                // which would cause a problem when fitting full-phase data, such as TESS or Kepler space-based data.
                // To calculate both a primary and secondary transit model for full phase data, the model must be calculated twice using the different set
                // of transit and eclipse shape parameters and combined separately from this function.
                muo1[i] = f0;
                continue;
            }

            // avoid Lutz-Kelker bias (negative values of p0 allowed)
            if (p0 > 0) {
                // limb darkened flux
                muo1[i] = (1.0 - ((1.0 - u1 - 2.0 * u2) * lambdae[i] + (u1 + 2.0 * u2) * (lambdad[i] + 2.0 / 3.0 * (p > bz0[i] ? 1 : 0)) + u2 * etad[i]) / (1.0 - u1 / 3.0 - u2 / 6.0)) * f0;
//                IJ.log("yModel["+i+"]="+muo1[i]);
                // uniform disk
//                mu0[i]=1.0-lambdae[i];

                // coeffs for quadratic limb darkening fit
//                if arg_present(d) then $
//                d = transpose([[1.d0-lambdae],$
//                                [2d0/3d0*(lambdae - (p gt z)) - lambdad],$
//                                [lambdae/2d0 - etad]])
            } else {
                // limb darkened flux
                muo1[i] = (1.0 + ((1.0 - u1 - 2.0 * u2) * lambdae[i] + (u1 + 2.0 * u2) * (lambdad[i] + 2.0 / 3.0 * (p > bz0[i] ? 1 : 0)) + u2 * etad[i]) / (1.0 - u1 / 3.0 - u2 / 6.0)) * f0;
//                IJ.log("yModel["+i+"]="+muo1[i]);
                // uniform disk
//                mu0[i]=1.0+lambdae[i];

                // coeffs for quadratic limb darkening fit
//                if arg_present(d) then $
//                d = transpose([[1.d0+lambdae],$
//                                [2d0/3d0*((p gt z) - lambdae) + lambdad],$
//                                [etad - lambdae/2d0]])
            }
//            IJ.log("z["+i+"]="+zArray[i]+" => y[i]="+muo1[i]); 
        }
        return muo1;
    }

    public static double getTcPhase(double e, double omega) {
        return getTcPhase(e, omega, TransitLocation.PRIMARY);
    }

    public static double getTcPhase(double e, double omega, TransitLocation tl) {
        // 2014/05/07 Transcoded from exofast_getphase.pro by Karen Collins (University of Louisville)
        // NAME:
        //   EXOFAST_GETPHASE
        //
        // PURPOSE:
        //   Calculates the phase (mean anomaly/(2pi)) corresponding to the transit center time
        //
        // CALLING SEQUENCE:
        //   phase = exofast_getphase(eccen,omega)
        //
        // INPUTS: 
        //   ECCEN: The eccentricity of the orbit, must be between 0 and 1
        //
        //   OMEGA: The argument of periastron of the star, in radians
        //
        // Modification History:
        //  2010/06 - Rewritten by Jason Eastman (OSU)

        double trueanom = tl.trueAnon(omega);
        double eccenanom = 2.0 * atan(sqrt((1.0 - e) / (1.0 + e)) * tan((trueanom) / 2.0));
        var M = eccenanom - e * sin(e);
        var phase = M / (2*PI);
        if (phase < 0) phase += 1;
        return phase;
    }


    public static double[][] impactParameter(double[] bjd, double inclination, double ar, double tp,
                                           double P, double e, double omega, boolean useLonAscNode, double lonAscNode) {
//      From EXOFAST exofast_getb.pro
//      Transcoded to Java by Karen Collins 2014/05/05 (Universty of Louisville)
//
//      Calculate the mean anomaly corresponding to each observed time
//
//      INPUTS:
//      bjd         - Barycentric Julians dates for desired impact
//                    parameters (ideally in the target's
//                    barycentric frame; see BJD2TARGET)
//      i           - inclination of orbit (radians)
//      a           - semi-major axis (in units of R_*)
//      tp          - periastron passage time (BJD)
//      P           - Period of orbit (days)
//      
//      OPTIONAL INPUTS:  
//      e           - eccentricity of orbit (0 if not specified)
//      omega       - argument of periastron of the star's orbit, in radians
//                    -- omega_* is typically quoted from RV
//                    -- required if e is specified
//                    -- assumed to be PI/2 if e not specified
//                    -- omega_* = omega_planet + PI
//      lonascnode  - The Longitude of the ascending node
//                    (radians). Set to PI if not specified.
//         
//      OUTPUTS:
//      result      - the impact parameter for each BJD, in units of R_* 

        int len = bjd.length;
        double[][] b = new double[2][len];
        double trueanom = 0.0;
        double meananom;
        double x, y, r, z, tmp, xold, yold, eccanom;

        var cosLonAscNode = useLonAscNode ? Math.cos(lonAscNode) : 0;
        var sinLonAscNode = useLonAscNode ? Math.sin(lonAscNode) : 0;

        var b0 = b[0];
        var b1 = b[1];
        var tau = 2D * PI;
        var cosInclination = Math.cos(inclination);
        var sinInclination = Math.sin(inclination);
        var oneMinusE2 = 1.0 - e * e;

        for (int i = 0; i < len; i++) {
            meananom = (tau * (1.0 + (bjd[i] - tp) / P)) % (tau);

            //if eccentricity is given, integrate the orbit
            if (e != 0.0) {
                eccanom = solveKeplerEq(meananom, e);
                trueanom = 2.0 * atan(sqrt((1.0 + e) / (1.0 - e)) * tan(0.5 * eccanom));
            } else {
                trueanom = meananom;
            }

            // calculate the corresponding (x,y) coordinates of planet
            r = ar * (1.0 - e * e) / (1.0 + e * cos(trueanom));
                r = ar * oneMinusE2 / (1.0 + e * cos(trueanom));

            //as seen from observer
            x = -r * cos(trueanom + omega);
            tmp = r * sin(trueanom + omega);
            y = -tmp * cosInclination;
            z =  tmp * sinInclination;

            //Rotate by the Longitude of Ascending Node
//            // For transits, it is not constrained, so we assume Omega=PI)
            if (useLonAscNode) {
                xold = x;
                yold = y;
                x = -xold * cosLonAscNode + yold * sinLonAscNode;
                y = -xold * sinLonAscNode - yold * cosLonAscNode;
            }

            b0[i] = sqrt(x * x + y * y);
            b1[i] = z;
        }

        return b;
    }

    public static double solveKeplerEq(double m, double ecc) {
//         FROM EXOFAST exofast_keplereq.pro
//         TRANSCODED TO JAVA BY KAREN COLLINS 2014/05/05 (UNIVERSITY OF LOUISVILLE)
//
//         PURPOSE: 
//            Solve Kepler's Equation
//         DESCRIPTION:
//            Solve Kepler's Equation. Method by S. Mikkola (1987) Celestial
//               Mechanics, 40 , 329-334. 
//            result from Mikkola then used as starting value for
//               Newton-Raphson iteration to extend the applicability of this
//               function to higher eccentricities
//        
//         CATEGORY:
//            Celestial Mechanics
//         CALLING SEQUENCE:
//            eccanom=exofast_keplereq(m,ecc)
//         INPUTS:
//            m    - Mean anomaly (radians; can be an array)
//            ecc  - Eccentricity
//         OPTIONAL INPUT PARAMETERS:
//        
//         KEYWORD INPUT PARAMETERS:
//            thresh: stopping criterion for the Newton Raphson iteration; the
//                    iteration stops once abs(E-Eold)<thresh
//         OUTPUTS:
//            the function returns the eccentric anomaly
//         KEYWORD OUTPUT PARAMETERS:
//         COMMON BLOCKS:
//         SIDE EFFECTS:
//         RESTRICTIONS:
//         PROCEDURE:
//         MODIFICATION HISTORY:
//          2002/05/29 - Marc W. Buie, Lowell Observatory.  Ported from fortran routines
//            supplied by Larry Wasserman and Ted Bowell.
//            http://www.lowell.edu/users/buie/
//        
//          2002-09-09 -- Joern Wilms, IAA Tuebingen, Astronomie.
//            use analytical values obtained for the low eccentricity case as
//            starting values for a Newton-Raphson method to allow high
//            eccentricity values as well
//        
//          Revision 1.4  2009/11/04 Jason Eastman (OSU)
//          Changed name from keplereq to exofast_keplereq.pro
//          Fix infinite loop when diff is near 2pi +/- epsilon.
//          Fix infinite loop when oldval and eccanom[i] differ by integer
//          multiples of 2pi.
//        
//          Revision 1.3  2005/05/25 16:11:35  wilms
//          speed up: Newton Raphson is only done if necessary
//          (i.e., almost never)


        // set default values
        double thresh = 1e-10;

        if (ecc < 0.0 || ecc > 1.0)
            return m;

        double mx = m;

        // Range reduction of m to -pi < m <= pi

        // ... m > pi
        if (mx > PI) {
            mx %= 2 * PI;
            if (mx > PI) {
                mx -= 2.0 * PI;
            }
        }

        // ... m < -pi
        if (mx <= -PI) {
            mx %= 2 * PI;
            if (mx <= -PI) {
                mx += 2.0 * PI;
            }
        }

        // Bail out for circular orbits...
        if (ecc == 0.0) return mx;

        // equation 9a
        double aux = 4.0 * ecc + 0.5;
        double alpha = (1.0 - ecc) / aux;
        double beta = mx / (2.0 * aux);

        // equation 9b (except not really; is there an errata?)
        // the actual equation 9b is much much slower, but gives the same
        // answer (probably because more refinement necessary)
        aux = sqrt(beta * beta + alpha * alpha * alpha);
        double z = beta + aux;

        if (z <= 0.0) z = beta - aux;
        z = pow(z, 0.3333333333333333);

        double s0 = z - alpha / z;
        double s1 = s0 - (0.078 * s0 * s0 * s0 * s0 * s0) / (1.0 + ecc);
        double e0 = mx + ecc * (3.0 * s1 - 4.0 * s1 * s1 * s1);

        double se0 = sin(e0);
        double ce0 = cos(e0);

        double f = e0 - ecc * se0 - mx;
        double f1 = 1.0 - ecc * ce0;
        double f2 = ecc * se0;
        double f3 = ecc * ce0;
        double u1 = -f / f1;
        double u2 = -f / (f1 + 0.5 * f2 * u1);
        double u3 = -f / (f1 + 0.5 * f2 * u2 + 0.16666666666666667 * f3 * u2 * u2);
        double u4 = -f / (f1 + 0.5 * f2 * u3 + 0.16666666666666667 * f3 * u3 * u3 - 0.041666666666666667 * f2 * u3 * u3 * u3);

        double eccanom = e0 + u4;

        if (eccanom >= 2.0 * PI) eccanom -= 2.0 * PI;
        if (eccanom < 0.0) eccanom += 2.0 * PI;

        // Now get more precise solution using Newton Raphson method
        // for those times when the Kepler equation is not yet solved
        // to better than 1e-10
        // (modification J. Wilms)

        if (mx < 0.) mx += 2.0 * PI;

        // calculate the differences
        double diff = abs(eccanom - ecc * sin(eccanom) - mx);
        diff = diff < abs(diff - 2 * PI) ? diff : abs(diff - 2 * PI);
        boolean loop = diff > thresh;
        while (loop) {
            // E-e sinE-M
            double fe = (eccanom - ecc * sin(eccanom) - mx) % (2 * PI);
            // f' = 1-e*cosE
            double fs = (1.0 - ecc * cos(eccanom)) % (2 * PI);
            double oldval = eccanom;
            eccanom = (oldval - fe / fs);
            if (abs(oldval - eccanom) <= thresh) loop = false;
        }

        // range reduction
        if (eccanom >= 2.0 * PI) eccanom %= (2 * PI);
        if (eccanom < 0.0) eccanom = (eccanom % (2 * PI)) + 2 * PI;

        return eccanom;
    }


    public static double[] ellke(double k) {
        // NAME:
        //   ELLKE
        //
        // PURPOSE: 
        //   Computes Hasting's polynomial approximation for the complete 
        //   elliptic integral of the first (ek) and second (kk) kind. Combines
        //   the calculation of both so as not to duplicate the expensive
        //   calculation of alog10(1-k^2).
        //
        // CALLING SEQUENCE:
        //    ellke(k);
        //
        // INPUTS:
        //
        //    k - The elliptic modulus.
        //
        // OUTPUTS:
        //
        //    ek - The elliptic integral of the first kind
        //    kk - The elliptic integral of the second kind
        //
        // MODIFICATION HISTORY
        // 
        //  2014/04/05 -- Transcoded to Java by Karen Collins (University of Louisville)
        //  2009/04/06 -- Written by Jason Eastman (Ohio State University)

        double m1 = 1.0 - k * k;
        double logm1 = Math.log(m1);

        double ee1 = 1.0 + m1 * (a1 + m1 * (a2 + m1 * (a3 + m1 * a4)));
        double ee2 = m1 * (b1 + m1 * (b2 + m1 * (b3 + m1 * b4))) * (-logm1);
        var ek = ee1 + ee2;

        double ek1 = aa0 + m1 * (aa1 + m1 * (aa2 + m1 * (aa3 + m1 * aa4)));
        double ek2 = (bb0 + m1 * (bb1 + m1 * (bb2 + m1 * (bb3 + m1 * bb4)))) * logm1;
        var kk = ek1 - ek2;

        return new double[]{ek, kk};
    }

    /**
     * @return the solution, second value indicates convergence if it is 0
     */
    public static double ellpic_bulirsch(double n, double k) {
        // NAME:
        //   ELLPIC_BULIRSCH
        //
        // PURPOSE: 
        //   Computes the complete elliptical integral of the third kind using
        //   the algorithm of Bulirsch (1965):
        //
        //   Bulirsch 1965, Numerische Mathematik, 7, 78
        //   Bulirsch 1965, Numerische Mathematik, 7, 353
        //
        // CALLING SEQUENCE:
        //    result = ellpic_bulirsch(n, k)
        //
        // INPUTS:
        //
        //    n,k - int(dtheta/((1-n*sin(theta)^2)*sqrt(1-k^2*sin(theta)^2)),0, pi/2)
        //
        // RESULT:
        //
        //    The complete elliptical integral of the third kind
        //
        // MODIFICATION HISTORY
        //  2014/05 -- Transcoded to Java by Karen Collins (Universoty of Louisville)
        //  2009/03 -- Written by Eric Agol

        double kc = sqrt(1.0 - k * k);
        double p = n + 1.0;
        if (p < 0.0) IJ.log("ellpic_bulirsch ERROR: 'Negative p'");
        double m0 = 1.0;
        double c = 1.0;
        p = sqrt(p);
        double d = 1.0 / p;
        double e = kc;
        double f;
        double g;

        var count = 0;
        while (count <= 20) {
            f = c;
            c = d / p + c;
            g = e / p;
            d = 2.0 * (f * g + d);
            p = g + p;
            g = m0;
            m0 = kc + m0;
            if (abs(1.0 - kc / g) > 1.0e-8) {
                kc = 2.0 * sqrt(e);
                e = kc * m0;
            } else {
                break;
            }
            count++;
        }

        return 0.5 * PI * (c * m0 + d) / (m0 * (m0 + p));
    }


    /**
     * Returns a list of currently displayed images, in reversed order (presumably latest most interesting)
     */
    public static String[] listOfOpenImages(String def) {
        if (SwingUtilities.isEventDispatchThread()) {
            int off = 0;
            int[] imageList = WindowManager.getIDList();
            if (imageList == null)
                return null;
            int n = imageList.length;
            if (def != null) off = 1;
            String[] images = new String[n + off];
            for (int i = n - 1; i >= 0; i--) {
                ImagePlus im = WindowManager.getImage(imageList[i]);
                images[i + off] = im.getTitle();
            }
            if (def != null) images[0] = def;
            return images;
        } else {
            var o = new String[1][];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    int off = 0;
                    int[] imageList = WindowManager.getIDList();
                    if (imageList == null) {
                        o[0] = null;
                        return;
                    }
                    int n = imageList.length;
                    if (def != null) off = 1;
                    String[] images = new String[n + off];
                    for (int i = n - 1; i >= 0; i--) {
                        ImagePlus im = WindowManager.getImage(imageList[i]);
                        images[i + off] = im.getTitle();
                    }
                    if (def != null) images[0] = def;
                    o[0] = images;
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
            return o[0];
        }
    }

    /**
     * Handy methods for extracting directory, filename, and suffix from a path.
     */
    public static String extractDirectory(String path) {
        String slash = "/";
        if (IJ.isWindows()) slash = "\\";
        int i = path.lastIndexOf(slash);
        if (i == -1)
            return null;
        else
            return path.substring(0, i) + slash;
    }

    public static String extractFilename(String path) {
        String slash = "/";
        if (IJ.isWindows()) slash = "\\";
        int i = path.lastIndexOf(slash);
        if (i == -1)
            return path;
        else
            return path.substring(i + 1);
    }

    public static String extractFilenameWithoutSuffix(String path) {
        String filename = extractFilename(path);
        int i = filename.lastIndexOf(".");
        if (i == -1)
            return filename;
        else
            return filename.substring(0, i - 1);
    }

    public static String extractFilenameWithoutFitsSuffix(String path) {
        String filename = extractFilename(path);
        int i;
        i = filename.lastIndexOf(".fits");
        if (i > 1) return filename.substring(0, i - 1);
        i = filename.lastIndexOf(".fts");
        if (i > 1) return filename.substring(0, i - 1);
        i = filename.lastIndexOf(".fit");
        if (i > 1) return filename.substring(0, i - 1);
        return filename;
    }

    public static String extractFilenameSuffix(String path) {
        String filename = extractFilename(path);
        int i = filename.lastIndexOf(".");
        if (i == -1)
            return null;
        else
            return filename.substring(i + 1);
    }

    /**
     * Checks to see if this image name is already in use.  If so, it appends a number.
     */
    public static String uniqueDisplayedImageName(String name) {
        String[] used = listOfOpenImages(null);
        if (used == null)
            return name;

        String basic = extractFilenameWithoutSuffix(name);
        String suffix = extractFilenameSuffix(name);

        for (int i = 0; i < used.length; i++) {
            if (used[i].equals(name)) {
                int k = 1;
                int kk;
                String test = basic + "-" + k + "." + suffix;
                do {
                    kk = k;
                    for (int j = 0; j < used.length; j++) {
                        if (used[i].equals(test)) k++;
                    }
                } while (k != kk);
                return basic + "-" + k + "." + suffix;
            }
        }
        return name;
    }

    /**
     * Help routine to find minimum value in a double array.
     */
    public static double minOf(double[] arr) {
        int n = arr.length;
        if (n == 0) return Double.NaN;

        double mn = arr[0];
        for (int i = 1; i < n; i++) {
            if (!Double.isNaN(arr[i]) && arr[i] < mn) mn = arr[i];
        }
        return mn;
    }

    /**
     * Help routine to find the maximum value in a double array.
     */
    public static double maxOf(double[] arr) {
        int n = arr.length;
        if (n == 0) return Double.NaN;

        double mx = arr[0];
        for (int i = 1; i < n; i++) {
            if (!Double.isNaN(arr[i]) && arr[i] > mx) mx = arr[i];
        }
        return mx;
    }

    public static int parseInteger(String s, int defaultValue) {
        if (s == null) return defaultValue;
        try {
            defaultValue = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public static int parseInteger(String s) {
        return parseInteger(s, -1);
    }

    public enum TransitLocation {
        /**
         * Primary Transit
         */
        PRIMARY(omega -> PI/2d - omega),
        /**
         * Secondary eclipse
         */
        SECONDARY(omega -> 3*PI/2d - omega),
        /**
         * L5 Point (trailing)
         */
        L5(omega -> 5*PI/6d - omega),
        /**
         * L4 Point (leading)
         */
        L4(omega -> PI/6d - omega),
        /**
         * Periastron
         */
        PERIASTRON(omega -> 0d),
        /**
         * Ascending Node of primary (max RV)
         */
        ASCENDING_NODE(omega -> -omega),
        /**
         * Descending Node of primary (min RV)
         */
        DESCENDING_NODE(omega -> PI - omega);

        private Function<Double, Double> trueAnomCalc;
        TransitLocation(Function<Double, Double> trueAnomCalc) {
            this.trueAnomCalc = trueAnomCalc;
        }

        public double trueAnon(double omega) {
            return trueAnomCalc.apply(omega);
        }
    }

}
