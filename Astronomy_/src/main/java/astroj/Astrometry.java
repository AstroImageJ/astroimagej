package astroj;

import astroj.astrometry.solvers.HttpAstrometryNet;
import astroj.astrometry.solvers.PlateSolver;
import astroj.astrometry.solvers.WasmAstrometryNet;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.astro.io.prefs.Property;
import ij.astro.util.FitsExtensionUtil;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.util.Tools;
import util.prefs.RegionExclusion;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Pattern;


/**
 *
 */
public class Astrometry { //implements KeyListener
    public static final Property<Integer> ASTRO_NET_POLLING = new Property<>(2, Astrometry.class);
    public static final Property<Integer> CUSTOM_POLLING = new Property<>(1, Astrometry.class);
    public static final Property<PlateSolver.SolverType> PLATE_SOLVER = new Property<>(PlateSolver.SolverType.ASTROMETRY_NET_HTTP, Astrometry.class);
    public static final Property<Boolean> REUSE_PREVIOUS_RADEC = new Property<>(false, Astrometry.class);
    final static int[] DIR_X_OFFSET = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
    final static int[] DIR_Y_OFFSET = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};
    final static byte MAXIMUM = (byte) 1;            // marks local maxima (irrespective of noise tolerance)
    final static byte LISTED = (byte) 2;             // marks points currently in the list
    final static byte PROCESSED = (byte) 4;          // marks points processed previously
    final static byte MAX_AREA = (byte) 8;           // marks areas near a maximum, within the tolerance
    final static byte EQUAL = (byte) 16;             // marks contigous maximum points of equal level
    final static byte MAX_POINT = (byte) 32;         // marks a single point standing for a maximum
    final static int FAILED = 0;
    final static int SUCCESS = 1;
    final static int SKIPPED = 2;
    final static int CANCELED = 3;
    private final Color colorWCS = new Color(255, 190, 0);//(226,105,11);
    private final Color sourceColor = new Color(33, 120, 181);
    public AstrometrySetup astrometrySetup;
    ImagePlus imp, imp2, impOriginal, impRaw;
    int width, height;
    boolean useAlternateAstrometryServer = false;
    String alternateAstrometryUrlBase = "http://127.0.0.1:8080";
    String slash = "/";
    boolean showLog = true;
    boolean showLogDateTime = true;
    boolean resaveRaw = false;
    boolean removeBackStars = true;
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    Centroid centroid = new Centroid();
    boolean processStack = true;
    int startSlice = 1;
    int endSlice = 1;
    int maxRetries = 1; // 1 -> 2 tries
    String userKey = "";
    boolean autoSave = false;
    boolean DPSaveRawWithWCS = false;
    boolean skipIfHasWCS = true;
    boolean annotate = true;
    double annotateRadius = 30;
    boolean addAnnotationsToHeader = true;
    boolean useMedianFilter = true;
    int medianFilterRadius = 2;
    double minPeakFindToleranceSTDEV = 1;
    boolean useMaxPeakFindValue = false;
    double maxPeakFindValue = 50000;
    int maxNumStars = 50;
    boolean useCentroid = false;
    boolean backPlane = false;
    double apertureRadius = 20;
    double apertureBack1 = 30;
    double apertureBack2 = 40;
    boolean useScale = false;
    double scaleEstimate = 1;  //arcsecperpixel
    double scaleError = 0.1;   //arcsecperpixel
    boolean useRaDec = false;
    double ra = 0;  //degrees
    double dec = 0;  //degrees
    double raDecRadius = 40.0;  //degrees
    boolean useDistortionOrder = true;
    int distortionOrder = 2;  //number of SIP distortion coefficients requested
    boolean canceled = false;
    boolean setupCanceled = false;
    boolean setupActive = false;
    double scale_lower = 0.9;
    double scale_upper = 1.1;
    String defaultAnnotationColor = "orange";
    String[] colors = IJU.colors;
    int npoints, slice;
    boolean saveActive = false;
    boolean notDP = true;
    double[] xdpoints;
    double[] ydpoints;
    OverlayCanvas ocanvas;
    DecimalFormat uptoTwoPlaces = new DecimalFormat("0.##", IJU.dfs);
    private int[] dirOffset;                        // pixel offsets of neighbor pixels for direct addressing
    private boolean compress = false, fpack = false;

    public Astrometry() {
        Locale.setDefault(IJU.locale);
        if (IJ.isWindows()) slash = "\\";
    }

    public int solve(ImagePlus impIn, boolean runSetup, AstroConverter acc, boolean useSexagesimal, boolean useLog, boolean useLogDateTime, ImagePlus impRawIn, String rawPath) {
        notDP = (acc == null);

        canceled = false;
        impOriginal = impIn;
        impRaw = impRawIn;
        ocanvas = OverlayCanvas.getOverlayCanvas(impOriginal);
        startSlice = 1;
        endSlice = impOriginal.getStackSize();

        width = impOriginal.getWidth();
        height = impOriginal.getHeight();

        setupCanceled = false;
        setupActive = false;
        if (runSetup) {
            setupActive = true;

            var modes = FitsExtensionUtil.compressionModes(IJU.getSliceFilename(impOriginal, startSlice));
            Prefs.set("astrometry.gzip", modes.contains(FitsExtensionUtil.CompressionMode.GZIP));
            Prefs.set("astrometry.fpack", modes.contains(FitsExtensionUtil.CompressionMode.FPACK));

            astrometrySetup = new AstrometrySetup();
            setupCanceled = astrometrySetup.start(1, impOriginal.getCurrentSlice(), impOriginal.getStackSize(), "START", acc, useSexagesimal);
            if (setupCanceled) {
                canceled = true;
                return CANCELED;
            }
            setupActive = false;
        }

        getPrefs();

        if (!useAlternateAstrometryServer && userKey.trim().isEmpty() &&
                PLATE_SOLVER.get() == PlateSolver.SolverType.ASTROMETRY_NET_HTTP) {
            IJ.showMessage("Login Error", """
                    <html>User Key is empty.<br>\
                    Obtain a user key at nova.astrometry.net and enter it<br>\
                    into the "User Key:" box in the Astrometry Settings panel.""");
            return FAILED;
        }

        resaveRaw = !notDP && DPSaveRawWithWCS && impRaw != null && impRaw.getStackSize() == 1 && rawPath != null && !rawPath.equals("");

        if (!notDP) {
            ra = acc.getRAJ2000();
            dec = acc.getDecJ2000();
            showLog = useLog;
            showLogDateTime = useLogDateTime;
        }

        minPeakFindToleranceSTDEV *= impOriginal.getStatistics().stdDev;
        scale_lower = scaleEstimate - scaleError;
        scale_upper = scaleEstimate + scaleError;

        if (!processStack) {
            startSlice = impOriginal.getCurrentSlice();
            endSlice = impOriginal.getCurrentSlice();
        }
        if (startSlice < 1) startSlice = 1;
        if (startSlice > impOriginal.getStackSize()) startSlice = impOriginal.getStackSize();
        if (endSlice > impOriginal.getStackSize()) endSlice = impOriginal.getStackSize();
        if (endSlice < startSlice) endSlice = startSlice;

        try (var solver = getSolver()) {
            return processStack(solver, rawPath);
        } catch (Exception e) {
            e.printStackTrace();
            return FAILED;
        }
    }

    private PlateSolver getSolver() {
        return switch (PLATE_SOLVER.get()) {
            case ASTROMETRY_NET_HTTP -> new HttpAstrometryNet(annotate, () -> canceled, maxRetries,
                    notDP ? HttpAstrometryNet.DEFAULT_JOB_MAX_POLLS : 120);
            case ASTROMETRY_NET_WASM -> new WasmAstrometryNet(annotate, () -> canceled, maxRetries,
                    notDP ? HttpAstrometryNet.DEFAULT_JOB_MAX_POLLS : 120);
        };
    }

    private int processStack(PlateSolver solver, String rawPath) {
        var lastRa = Double.NaN;
        var lastDec = Double.NaN;
        var lastRadius = Double.NaN;
        for (slice = startSlice; slice <= endSlice; slice++) {
            if (canceled) return CANCELED;

            impOriginal.setSlice(slice);

            if (skipIfHasWCS) {
                WCS wcs = new WCS(impOriginal);
                if (wcs.hasWCS()) {
                    if (notDP) {
                        log("Astrometry status for " + (impOriginal.getStackSize() == 1 ? impOriginal.getTitle() : "slice " + slice) + ": SKIPPED (already has valid WCS headers)");
                        continue;
                    } else {
                        return SKIPPED;
                    }
                }
            }
//            imp = new ImagePlus(impOriginal.getStack().getSliceLabel(slice), impOriginal.getStack().getProcessor(slice) );
            imp = new ImagePlus("Working_" + IJU.getSliceFilename(impOriginal, slice), impOriginal.getStack().getProcessor(slice).duplicate());
            imp.setCalibration(impOriginal.getCalibration());
            imp.setFileInfo(impOriginal.getFileInfo());

//            ocanvas.removeAnnotateRois();
            ocanvas.removeAstrometryAnnotateRois();
            impOriginal.updateAndDraw();

//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
//            imp = impOriginal..duplicate();
//            IJ.log("starting median");
            if (useMedianFilter) {
                IJ.showStatus("Median filter image");
                IJ.run(imp, "Median...", "radius=" + medianFilterRadius);
            }
//            IJ.log("finished median");
//            IJ.run(imp,"Remove Outliers...","radius=2 threshold=50 which=Bright stack");
            IJ.showStatus("Finding sources");
            if (canceled) return CANCELED;
            findMaxima(minPeakFindToleranceSTDEV);
//            if (IJ.escapePressed())return false;
            if (canceled) return CANCELED;

            String imageLabel = impOriginal.getStackSize() == 1 ? impOriginal.getTitle() : "slice " + slice;

            double[] xs = new double[npoints];
            double[] ys = new double[npoints];
            int nSources = 0;
            for (int i = 0; i < npoints; i++) {
                var x = xdpoints[i];
                var y = ydpoints[i];

                if (RegionExclusion.EXCLUDE_BORDERS.get()) {
                    if (x < RegionExclusion.BORDER_EXCLUSION_LEFT.get() || x > width - RegionExclusion.BORDER_EXCLUSION_RIGHT.get()
                            || y < RegionExclusion.BORDER_EXCLUSION_TOP.get() || y > height - RegionExclusion.BORDER_EXCLUSION_BOTTOM.get()) {
                        continue;
                    }
                }

                xs[nSources] = x;
                ys[nSources] = y;
                nSources++;
            }
            xs = Arrays.copyOf(xs, nSources);
            ys = Arrays.copyOf(ys, nSources);

            if (canceled) return CANCELED;

            var useRaDec = this.useRaDec;
            if (REUSE_PREVIOUS_RADEC.get() && slice > startSlice &&
                    Double.isFinite(lastRadius) && Double.isFinite(lastRa) && Double.isFinite(lastDec)) {
                useRaDec = true;
                ra = lastRa;
                dec = lastDec;
                raDecRadius = lastRadius;
            }//todo first slice, read from header RA and DEC headers, solver can take hms as string

            // NaN tells the solver that the corresponding hint is not in use.
            PlateSolver.Result result = solver.solve(xs, ys, width, height,
                    useScale ? scale_lower : Double.NaN,
                    useScale ? scale_upper : Double.NaN,
                    useRaDec ? ra : Double.NaN,
                    useRaDec ? dec : Double.NaN,
                    useRaDec ? raDecRadius : Double.NaN,
                    useDistortionOrder, distortionOrder);

            if (result instanceof PlateSolver.Result.Failure(String message, boolean cancelled)) {
                if (cancelled || canceled) {
                    canceled = true;
                    return CANCELED;
                }
                if (slice >= endSlice) {
                    log("Astrometry status for " + imageLabel + ": FAILED. " + message);
                    return FAILED;
                }
                log("Astrometry status for " + imageLabel + ": FAILED. " + message + " Skipping image.");
                continue;
            }
            var success = (PlateSolver.Result.Success) result;
            if (notDP) log("Astrometry status for " + imageLabel + ": SOLVED");

            FitsJ.Header wcsHeader = parseWcsHeader(success.wcsHeader());
            if (canceled) return CANCELED;

            if (REUSE_PREVIOUS_RADEC.get()) {
                lastRa = FitsJ.findDoubleValue("CRVAL1", wcsHeader) * 15;
                lastDec = FitsJ.findDoubleValue("CRVAL2", wcsHeader);
                var reg = Pattern.compile("COMMENT Field scale upper: (\\d+\\.\\d+) arcsec/pixel");
                var m = reg.matcher(success.wcsHeader());
                if (m.find()) {
                    lastRadius = Tools.parseDouble(m.group(1), 0) * Math.hypot(width, height) * 2;
                    lastRadius /= 3600;
                }
            }

            impOriginal.setSlice(slice);
            FitsJ.Header header = FitsJ.getHeader(impOriginal);
            if (header == null) {
                header = FitsJ.Header.build(new String[5]);
                header.cards()[0] = FitsJ.createCard("SIMPLE", "T", "Created by AIJ");
                header.cards()[1] = FitsJ.createCard("NAXIS", "2", "number of data axes");
                header.cards()[2] = FitsJ.createCard("NAXIS1", "" + width, "length of data axis 1");
                header.cards()[3] = FitsJ.createCard("NAXIS2", "" + height, "length of data axis 2");
                header.cards()[4] = FitsJ.createCard("END", "", "");
            }
            FitsJ.Header headerRaw = null;
            if (resaveRaw) {
                headerRaw = FitsJ.getHeader(impRaw);
                if (headerRaw == null) {
                    headerRaw = FitsJ.Header.build(new String[5]);
                    header.cards()[0] = FitsJ.createCard("SIMPLE", "T", "Created by AIJ");
                    header.cards()[1] = FitsJ.createCard("NAXIS", "2", "number of data axes");
                    header.cards()[2] = FitsJ.createCard("NAXIS1", "" + width, "length of data axis 1");
                    header.cards()[3] = FitsJ.createCard("NAXIS2", "" + height, "length of data axis 2");
                    header.cards()[4] = FitsJ.createCard("END", "", "");
                }
            }
//            if (header == null) header = new String[]{FitsJ.pad("END", 80)};
            int wcsCardNum = -1;
            int cardNum = -1;
            String cardString = "";
            String keyword = "";
            String[] keywords = {
                    "WCSAXES",
                    "CTYPE1",
                    "CTYPE2",
                    "CUNIT1",
                    "CUNIT2",
                    "EQUINOX",
                    "EPOCH",
                    "LONPOLE",
                    "LATPOLE",
                    "CRVAL1",
                    "CRVAL2",
                    "CRPIX1",
                    "CRPIX2",
                    "CD1_1",
                    "CD1_2",
                    "CD2_1",
                    "CD2_2",
                    "IMAGEW",
                    "IMAGEH",
                    "A_ORDER",
                    "B_ORDER",
                    "AP_ORDER",
                    "BP_ORDER"};

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            header = FitsJ.addHistory("WCS created by AIJ link to Astronomy.net website", header);
            if (resaveRaw) headerRaw = FitsJ.addHistory("WCS created by AIJ link to Astronomy.net website", headerRaw);
            header = FitsJ.addHistory("WCS created on " + sdf.format(cal.getTime()), header);
            if (resaveRaw) headerRaw = FitsJ.addHistory("WCS created on " + sdf.format(cal.getTime()), headerRaw);


            for (int i = 0; i < keywords.length; i++) {
                cardNum = FitsJ.findCardWithKey(keywords[i], header);
                if (cardNum != -1) {
                    header = FitsJ.removeCards(keywords[i], header);
                }
                if (resaveRaw) {
                    cardNum = FitsJ.findCardWithKey(keywords[i], headerRaw);
                    if (cardNum != -1) {
                        headerRaw = FitsJ.removeCards(keywords[i], headerRaw);
                    }
                }
                wcsCardNum = FitsJ.findCardWithKey(keywords[i], wcsHeader);
                if (wcsCardNum != -1) {
                    header = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], header);
                    if (resaveRaw) {
                        headerRaw = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], headerRaw);
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    cardNum = FitsJ.findCardWithKey("A_" + i + "_" + j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("A_" + i + "_" + j, header);
                    }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("A_" + i + "_" + j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("A_" + i + "_" + j, headerRaw);
                        }
                    }
                    cardNum = FitsJ.findCardWithKey("B_" + i + "_" + j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("B_" + i + "_" + j, header);
                    }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("B_" + i + "_" + j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("B_" + i + "_" + j, headerRaw);
                        }
                    }
                    cardNum = FitsJ.findCardWithKey("AP_" + i + "_" + j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("AP_" + i + "_" + j, header);
                    }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("AP_" + i + "_" + j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("AP_" + i + "_" + j, headerRaw);
                        }
                    }
                    cardNum = FitsJ.findCardWithKey("BP_" + i + "_" + j, header);
                    if (cardNum != -1) {
                        header = FitsJ.removeCards("BP_" + i + "_" + j, header);
                    }
                    if (resaveRaw) {
                        cardNum = FitsJ.findCardWithKey("BP_" + i + "_" + j, headerRaw);
                        if (cardNum != -1) {
                            headerRaw = FitsJ.removeCards("BP_" + i + "_" + j, headerRaw);
                        }
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("A_" + i + "_" + j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("B_" + i + "_" + j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("AP_" + i + "_" + j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    wcsCardNum = FitsJ.findCardWithKey("BP_" + i + "_" + j, wcsHeader);
                    if (wcsCardNum != -1) {
                        header = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], header);
                        if (resaveRaw) {
                            headerRaw = FitsJ.addCard(wcsHeader.cards()[wcsCardNum], headerRaw);
                        }
                    }
                }
            }

            if (canceled) return CANCELED;
            saveActive = true;

            impOriginal.setSlice(slice);
            FitsJ.putHeader(impOriginal, header);
            if (resaveRaw) FitsJ.putHeader(impRaw, headerRaw);
            saveActive = false;

            if (impOriginal.getCanvas() instanceof AstroCanvas) {
                ((AstroStackWindow) impOriginal.getWindow()).setAstroProcessor(true);
                impOriginal.getWindow().repaint();
            }
            if (canceled) return CANCELED;

            ocanvas.removeAstrometryAnnotateRois();
//            ocanvas.clearRois();
            impOriginal.updateAndDraw();

//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
//            IJ.runPlugIn(impOriginal, "Clear_Overlay", "");
            if (annotate) {

                FitsJ.putHeader(impOriginal, FitsJ.removeAstrometryAnnotateCards(FitsJ.getHeader(impOriginal)));
                if (resaveRaw) FitsJ.putHeader(impRaw, FitsJ.removeAstrometryAnnotateCards(FitsJ.getHeader(impRaw)));

                var annotationColor = IJU.colorOf(defaultAnnotationColor);
                for (var obj : success.objectsInField()) {
                    if (canceled) return CANCELED;
                    if (obj.names().isEmpty()) continue;
                    // Astrometry.net reports pixel positions with a bottom-left origin.
                    addAnnotateRoi(impOriginal, true, false, true, false,
                            obj.x() - 0.5, height - (obj.y() - 0.5), annotateRadius,
                            obj.names().iterator().next(), annotationColor);
                }
                impOriginal.draw();
            }
            if (notDP && (autoSave || impOriginal.getStack().isVirtual())) {
                boolean saveSuccess = false;
                if (canceled) return CANCELED;
                saveActive = true;
                impOriginal.setSlice(slice);
                String imageDirname = impOriginal.getOriginalFileInfo().directory;
                String imageFilename = FitsExtensionUtil.makeFitsSave(IJU.getSliceFilename(impOriginal, slice), fpack, compress);

                if (impOriginal.getStack().isVirtual()) {
//                    imp2 = new ImagePlus(impOriginal.getStack().getSliceLabel(slice), impOriginal.getStack().getProcessor(slice) );
                    imp2 = new ImagePlus("WCS_" + imageFilename, impOriginal.getStack().getProcessor(slice).duplicate());
                    imp2.setCalibration(impOriginal.getCalibration());
                    imp2.setFileInfo(impOriginal.getFileInfo());
                    FitsJ.putHeader(imp2, header);
//                    IJ.saveAs(imp2,""+imageFilename.substring(imageFilename.lastIndexOf('.')),imageDirname+imageFilename);
                    if (autoSave) {
                        saveSuccess = IJU.saveFile(imp2, imageDirname + imageFilename, showLog, showLogDateTime, "");
                    } else {
                        String wcsPath = imageDirname + "wcs";
                        File wcsDirectory = new File(wcsPath);
                        if (wcsDirectory.isFile()) {
                            if (showLog)
                                IJU.log("ERROR: Save directory \"" + wcsPath + "\" is a file, not a directory.", showLogDateTime);
                            return FAILED;
                        }
                        if (!wcsDirectory.isDirectory()) {
                            if (!wcsDirectory.mkdir()) {
                                if (showLog) IJU.log("ERROR: Could not create save-to directory \"" + wcsPath + "\".");
                                return FAILED;
                            }
                        }
                        var p = Path.of(wcsPath).resolve(imageFilename);
                        var pn = Path.of(imageDirname).resolve(imageFilename);
                        saveSuccess = IJU.saveFile(imp2, p.toString(), showLog, showLogDateTime, "");
                        try {
                            Files.move(p, pn, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
//                    IJ.saveAs(impOriginal,""+imageFilename.substring(imageFilename.lastIndexOf('.')),imageDirname+imageFilename);
                    saveSuccess = IJU.saveFile(impOriginal, imageDirname + imageFilename, showLog, showLogDateTime, "");
                }
                saveActive = false;
                if (!saveSuccess) {
                    return FAILED;
                }
            } else if (resaveRaw) {
                if (!IJU.saveFile(impRaw, rawPath, showLog, showLogDateTime, "raw science")) {
                    return FAILED;
                }
            } else if (annotate && (slice != endSlice || !notDP)) {
                if (canceled) return CANCELED;
                if (solver.requiresAwait()) IJ.wait(getPollingTimeout());
            }
        }

        return SUCCESS;
    }

    /**
     * Converts the header text returned by a {@link PlateSolver} into FITS cards.
     * Solvers may strip trailing padding, so the final card can be shorter than 80 characters.
     */
    private static FitsJ.Header parseWcsHeader(String text) {
        int count = (text.length() + 79) / 80;
        String[] cards = new String[count];
        for (int i = 0; i < count; i++) {
            cards[i] = String.format("%-80s", text.substring(i * 80, Math.min(text.length(), (i + 1) * 80)));
        }
        return FitsJ.Header.build(cards);
    }


    public void setShowLog(boolean show) {
        showLog = show;
    }


    void log(String message) {
        if (showLog) {
            if (showLogDateTime) {
                cal = Calendar.getInstance();
                IJ.log("[" + sdf.format(cal.getTime()) + "]      " + message);
            } else {
                IJ.log(message);
            }
        }
    }

    public void findMaxima(double tolerance) {
        ImageProcessor ip = imp.getProcessor();
        if (dirOffset == null) makeDirectionOffsets(ip);
        Rectangle roi = ip.getRoi();
        if (RegionExclusion.EXCLUDE_BORDERS.get()) {
            roi.width -= (RegionExclusion.BORDER_EXCLUSION_LEFT.get() + RegionExclusion.BORDER_EXCLUSION_RIGHT.get());
            roi.height -= (RegionExclusion.BORDER_EXCLUSION_TOP.get() + RegionExclusion.BORDER_EXCLUSION_BOTTOM.get());
            roi.translate(RegionExclusion.BORDER_EXCLUSION_LEFT.get(), RegionExclusion.BORDER_EXCLUSION_TOP.get());
        }
        ip.setRoi(roi);
        byte[] mask = ip.getMaskArray();
        ByteProcessor typeP = new ByteProcessor(width, height);     //will be a notepad for pixel types
        byte[] types = (byte[]) typeP.getPixels();
        float globalMin = Float.MAX_VALUE;
        float globalMax = -Float.MAX_VALUE;
        for (int y = roi.y; y < roi.y + roi.height; y++) {         //find local minimum/maximum now
            for (int x = roi.x; x < roi.x + roi.width; x++) {      //ImageStatistics won't work if we have no ImagePlus
                float v = ip.getPixelValue(x, y);
                if (globalMin > v) globalMin = v;
                if (globalMax < v) globalMax = v;
            }
        }

        if (canceled) return;
        IJ.showStatus("Getting sorted maxima...");
        long[] maxPoints = getSortedMaxPoints(ip, typeP, true, false, globalMin, globalMax, ImageProcessor.NO_THRESHOLD);
        if (canceled) return;
        IJ.showStatus("Analyzing  maxima...");
        float maxSortingError = 0;
        if (ip instanceof FloatProcessor)   //sorted sequence may be inaccurate by this value
            maxSortingError = 1.1f * ((globalMax - globalMin) / 2e9f);
        analyzeAndMarkMaxima(ip, typeP, maxPoints, true, false, globalMin, tolerance, maxSortingError);
    } // findMaxima


    /**
     * Find all local maxima (irrespective whether they finally qualify as maxima or not)
     *
     * @param ip              The image to be analyzed
     * @param typeP           A byte image, same size as ip, where the maximum points are marked as MAXIMUM
     *                        (do not use it as output: for rois, the points are shifted w.r.t. the input image)
     * @param excludeEdgesNow Whether to exclude edge pixels
     * @param isEDM           Whether ip is a float Euclidian distance map
     * @param globalMin       The minimum value of the image or roi
     * @param threshold       The threshold (calibrated) below which no pixels are processed. Ignored if ImageProcessor.NO_THRESHOLD
     * @return Maxima sorted by value. In each array element (long, i.e., 64-bit integer), the value
     * is encoded in the upper 32 bits and the pixel offset in the lower 32 bit
     * Note: Do not use the positions of the points marked as MAXIMUM in typeP, they are invalid for images with a roi.
     */
    long[] getSortedMaxPoints(ImageProcessor ip, ByteProcessor typeP, boolean excludeEdgesNow,
                              boolean isEDM, float globalMin, float globalMax, double threshold) {
        Rectangle roi = ip.getRoi();
        byte[] types = (byte[]) typeP.getPixels();
        int nMax = 0;  //counts local maxima
        boolean checkThreshold = threshold != ImageProcessor.NO_THRESHOLD;
        //long t0 = System.currentTimeMillis();
        for (int y = roi.y; y < roi.y + roi.height; y++) {         // find local maxima now
            if (y % 50 == 0 && canceled) return null;
            for (int x = roi.x, i = x + y * width; x < roi.x + roi.width; x++, i++) {      // for better performance with rois, restrict search to roi
                float v = ip.getPixelValue(x, y);
                float vTrue = v;
                if (v == globalMin) continue;
                if (excludeEdgesNow && (x == 0 || x == width - 1 || y == 0 || y == height - 1)) continue;
                if (checkThreshold && v < threshold) continue;
                boolean isMax = true;
                    /* check wheter we have a local maximum.
                    Note: For an EDM, we need all maxima: those of the EDM-corrected values
                    (needed by findMaxima) and those of the raw values (needed by cleanupMaxima) */
                boolean isInner = (y != 0 && y != height - 1) && (x != 0 && x != width - 1); //not necessary, but faster than isWithin
                for (int d = 0; d < 8; d++) {                         // compare with the 8 neighbor pixels
                    if (isInner || isWithin(x, y, d)) {
                        float vNeighbor = ip.getPixelValue(x + DIR_X_OFFSET[d], y + DIR_Y_OFFSET[d]);
                        float vNeighborTrue = vNeighbor;
                        if (vNeighbor > v && vNeighborTrue > vTrue) {
                            isMax = false;
                            break;
                        }
                    }
                }
                if (isMax) {
                    types[i] = MAXIMUM;
                    nMax++;
                }
            } // for x
        } // for y
        if (canceled) return null;
        //long t1 = System.currentTimeMillis();IJ.log("markMax:"+(t1-t0));

        float vFactor = (float) (2e9 / (globalMax - globalMin)); //for converting float values into a 32-bit int
        long[] maxPoints = new long[nMax];                  //value (int) is in the upper 32 bit, pixel offset in the lower
        int iMax = 0;
        for (int y = roi.y; y < roi.y + roi.height; y++)           //enter all maxima into an array
            for (int x = roi.x, p = x + y * width; x < roi.x + roi.width; x++, p++)
                if (types[p] == MAXIMUM) {
                    float fValue = ip.getPixelValue(x, y);
                    int iValue = (int) ((fValue - globalMin) * vFactor); //32-bit int, linear function of float value
                    maxPoints[iMax++] = (long) iValue << 32 | p;
                }
        //long t2 = System.currentTimeMillis();IJ.log("makeArray:"+(t2-t1));
        if (canceled) return null;
        Arrays.sort(maxPoints);                                 //sort the maxima by value
        //long t3 = System.currentTimeMillis();IJ.log("sort:"+(t3-t2));
        return maxPoints;
    } //getSortedMaxPoints


    /**
     * Check all maxima in list maxPoints, mark type of the points in typeP
     *
     * @param ip              the image to be analyzed
     * @param typeP           8-bit image, here the point types are marked by type: MAX_POINT, etc.
     * @param maxPoints       input: a list of all local maxima, sorted by height. Lower 32 bits are pixel offset
     * @param excludeEdgesNow whether to avoid edge maxima
     * @param isEDM           whether ip is a (float) Euclidian distance map
     * @param globalMin       minimum pixel value in ip
     * @param tolerance       minimum pixel value difference for two separate maxima
     * @param maxSortingError sorting may be inaccurate, sequence may be reversed for maxima having values
     *                        not deviating from each other by more than this (this could be a result of
     *                        precision loss when sorting ints instead of floats, or because sorting does not
     *                        take the height correction in 'trueEdmHeight' into account
     */
    void analyzeAndMarkMaxima(ImageProcessor ip, ByteProcessor typeP, long[] maxPoints, boolean excludeEdgesNow,
                              boolean isEDM, float globalMin, double tolerance, float maxSortingError) {
        byte[] types = (byte[]) typeP.getPixels();
        int nMax = maxPoints.length;
        int[] pList = new int[width * height];       //here we enter points starting from a maximum
        Vector<int[]> xyVector = new Vector<>();
        Roi roi = null;
        if (imp != null) {
            roi = imp.getRoi();
        }

        for (int iMax = nMax - 1; iMax >= 0; iMax--) {    //process all maxima now, starting from the highest
            if (iMax % 100 == 0 && canceled) return;
            int offset0 = (int) maxPoints[iMax];     //type cast gets 32 lower bits, where pixel index is encoded
            //int offset0 = maxPoints[iMax].offset;
            if ((types[offset0] & PROCESSED) != 0)      //this maximum has been reached from another one, skip it
                continue;
            //we create a list of connected points and start the list at the current maximum
            int x0 = offset0 % width;
            int y0 = offset0 / width;
            float v0 = ip.getPixelValue(x0, y0);
            boolean sortingError;
            do {                                    //repeat if we have encountered a sortingError
                if (canceled) return;
                pList[0] = offset0;
                types[offset0] |= (EQUAL | LISTED);   //mark first point as equal height (to itself) and listed
                int listLen = 1;                    //number of elements in the list
                int listI = 0;                      //index of current element in the list
                boolean isEdgeMaximum = (x0 == 0 || x0 == width - 1 || y0 == 0 || y0 == height - 1);
                sortingError = false;       //if sorting was inaccurate: a higher maximum was not handled so far
                boolean maxPossible = true;         //it may be a true maximum
                double xEqual = x0;                 //for creating a single point: determine average over the
                double yEqual = y0;                 //  coordinates of contiguous equal-height points
                int nEqual = 1;                     //counts xEqual/yEqual points that we use for averaging
                do {                                //while neigbor list is not fully processed (to listLen)
                    int offset = pList[listI];
                    int x = offset % width;
                    int y = offset / width;
                    //if(x==18&&y==20)IJ.write("x0,y0="+x0+","+y0+"@18,20;v0="+v0+" sortingError="+sortingError);
                    boolean isInner = (y != 0 && y != height - 1) && (x != 0 && x != width - 1); //not necessary, but faster than isWithin
                    for (int d = 0; d < 8; d++) {       //analyze all neighbors (in 8 directions) at the same level
                        int offset2 = offset + dirOffset[d];
                        if ((isInner || isWithin(x, y, d)) && (types[offset2] & LISTED) == 0) {
                            if ((types[offset2] & PROCESSED) != 0) {
                                maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                                //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at processed neighbor from x,y="+x+","+y+", dir="+d);
                                break;
                            }
                            int x2 = x + DIR_X_OFFSET[d];
                            int y2 = y + DIR_Y_OFFSET[d];
                            float v2 = ip.getPixelValue(x2, y2);
                            if (v2 > v0 + maxSortingError) {
                                maxPossible = false;    //we have reached a higher point, thus it is no maximum
                                //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at higher neighbor from x,y="+x+","+y+", dir="+d+",value,value2,v2-v="+v0+","+v2+","+(v2-v0));
                                break;
                            } else if (v2 >= v0 - (float) tolerance) {
                                if (v2 > v0) {          //maybe this point should have been treated earlier
                                    sortingError = true;
                                    offset0 = offset2;
                                    v0 = v2;
                                    x0 = x2;
                                    y0 = y2;

                                }
                                pList[listLen] = offset2;
                                listLen++;              //we have found a new point within the tolerance
                                types[offset2] |= LISTED;
                                if (x2 == 0 || x2 == width - 1 || y2 == 0 || y2 == height - 1) {
                                    isEdgeMaximum = true;
                                    if (excludeEdgesNow) {
                                        maxPossible = false;
                                        break;          //we have an edge maximum;
                                    }
                                }
                                if (v2 == v0) {           //prepare finding center of equal points (in case single point needed)
                                    types[offset2] |= EQUAL;
                                    xEqual += x2;
                                    yEqual += y2;
                                    nEqual++;
                                }
                            }
                        } // if isWithin & not LISTED
                    } // for directions d
                    listI++;
                } while (listI < listLen);

                if (sortingError) {                  //if x0,y0 was not the true maximum but we have reached a higher one
                    for (listI = 0; listI < listLen; listI++)
                        types[pList[listI]] = 0;    //reset all points encountered, then retry
                } else {
                    int resetMask = ~(maxPossible ? LISTED : (LISTED | EQUAL));
                    xEqual /= nEqual;
                    yEqual /= nEqual;
                    double minDist2 = 1e20;
                    int nearestI = 0;
                    for (listI = 0; listI < listLen; listI++) {
                        int offset = pList[listI];
                        int x = offset % width;
                        int y = offset / width;
                        types[offset] &= resetMask;        //reset attributes no longer needed
                        types[offset] |= PROCESSED;        //mark as processed
                        if (maxPossible) {
                            types[offset] |= MAX_AREA;
                            if ((types[offset] & EQUAL) != 0) {
                                double dist2 = (xEqual - x) * (xEqual - x) + (yEqual - y) * (yEqual - y);
                                if (dist2 < minDist2) {
                                    minDist2 = dist2;    //this could be the best "single maximum" point
                                    nearestI = listI;
                                }
                            }
                        }
                    } // for listI
                    if (maxPossible) {
                        int offset = pList[nearestI];
                        types[offset] |= MAX_POINT;
                        if (!isEdgeMaximum) {
                            int x = offset % width;
                            int y = offset / width;
                            if ((roi == null || roi.contains(x, y)) && (!useMaxPeakFindValue || ip.getPixelValue(x, y) < maxPeakFindValue))
                                xyVector.addElement(new int[]{x, y});
                        }
                    }
                } //if !sortingError
            } while (sortingError);                //redo if we have encountered a higher maximum: handle it now.
        } // for all maxima iMax

        if (canceled) return;
        if (xyVector != null) {

            npoints = xyVector.size();

            if (npoints > 0) {
                if (npoints > maxNumStars) npoints = maxNumStars;
                xdpoints = new double[npoints];
                ydpoints = new double[npoints];
                int[] xy;

                for (int i = 0; i < npoints; i++) {
                    xy = xyVector.elementAt(i);
                    if (useCentroid && centroid.measure(imp, xy[0], xy[1], apertureRadius, apertureBack1, apertureBack2, useCentroid, backPlane, removeBackStars)) {
                        xdpoints[i] = centroid.xCenter;
                        ydpoints[i] = centroid.yCenter;
                        if (i % 100 == 0) {
                            if (canceled) return;
                            String status = "Finding centroids";
                            for (int n = 0; n < (i / 100) % 10; n++) {
                                status += " .";
                            }
                            IJ.showStatus(status);
                        }
                    } else {
                        xdpoints[i] = xy[0] + 0.5;
                        ydpoints[i] = xy[1] + 0.5;
                    }
                }
                if (impOriginal != null) {
                    for (int i = 0; i < npoints; i++) {
                        addAnnotateRoi(impOriginal, true, useCentroid, false, true, xdpoints[i], ydpoints[i], useCentroid ? apertureRadius : annotateRadius, "", sourceColor);
                    }
                    impOriginal.draw();
                }
            }
        }
    } //void analyzeAndMarkMaxima


    /**
     * create an array of offsets within a pixel array for directions in clockwise order:
     * 0=(x,y-1), 1=(x+1,y-1), ... 7=(x-1,y)
     * Also creates further class variables:
     * width, height, and the following three values needed for storing coordinates in single ints for watershed:
     * intEncodeXMask, intEncodeYMask and intEncodeShift.
     * E.g., for width between 129 and 256, xMask=0xff and yMask = 0xffffff00 are bitwise masks
     * for x and y, respectively, and shift=8 is the bit shift to get y from the y-masked value
     * Returns as class variables: the arrays of the offsets to the 8 neighboring pixels
     * and the array maskAndShift for watershed
     */
    void makeDirectionOffsets(ImageProcessor ip) {
        int shift = 0, mult = 1;
        do {
            shift++;
            mult *= 2;
        } while (mult < width);
//            intEncodeXMask = mult-1;
//            intEncodeYMask = ~intEncodeXMask;
//            intEncodeShift = shift;
        //IJ.log("masks (hex):"+Integer.toHexString(xMask)+","+Integer.toHexString(xMask)+"; shift="+shift);
        dirOffset = new int[]{-width, -width + 1, +1, +width + 1, +width, +width - 1, -1, -width - 1};
        //dirOffset is created last, so check for it being null before makeDirectionOffsets
        //(in case we have multiple threads using the same MaximumFinder)
    }


    /**
     * returns whether the neighbor in a given direction is within the image
     * NOTE: it is assumed that the pixel x,y itself is within the image!
     * Uses class variables width, height: dimensions of the image
     *
     * @param x         x-coordinate of the pixel that has a neighbor in the given direction
     * @param y         y-coordinate of the pixel that has a neighbor in the given direction
     * @param direction the direction from the pixel towards the neighbor (see makeDirectionOffsets)
     * @return true if the neighbor is within the image (provided that x, y is within)
     */
    boolean isWithin(int x, int y, int direction) {
        int xmax = width - 1;
        int ymax = height - 1;
        switch (direction) {
            case 0:
                return (y > 0);
            case 1:
                return (x < xmax && y > 0);
            case 2:
                return (x < xmax);
            case 3:
                return (x < xmax && y < ymax);
            case 4:
                return (y < ymax);
            case 5:
                return (x > 0 && y < ymax);
            case 6:
                return (x > 0);
            case 7:
                return (x > 0 && y > 0);
        }
        return false;   //to make the compiler happy :-)
    } // isWithin

    protected void addAnnotateRoi(ImagePlus implus, boolean showCir, boolean isCentered, boolean showLab, boolean isSourceROI, double x, double y, double rad, String labelText, Color col) {
        AnnotateRoi roi = new AnnotateRoi(showCir, isCentered, showLab, true, x, y, rad, labelText, col);
        roi.setIsSourceROI(isSourceROI);
        roi.setImage(implus);
        OverlayCanvas overlayCanvas = OverlayCanvas.getOverlayCanvas(implus);
        overlayCanvas.add(roi);
        if (!isSourceROI && addAnnotationsToHeader) {
            String value = "'" + uptoTwoPlaces.format(IJU.ijX2fitsX(x)) + "," + uptoTwoPlaces.format(IJU.ijY2fitsY(impOriginal.getHeight(), y)) + "," + uptoTwoPlaces.format(rad) + "," + (showCir ? "1" : "0") + "," +
                    (isCentered ? "1" : "0") + "," + (showLab ? "1" : "0") + ",1," + IJU.colorNameOf(col) + "'";  //the last '1' indicates from astrometry
            FitsJ.putHeader(impOriginal, FitsJ.addAnnotateCard(value, labelText, FitsJ.getHeader(impOriginal)));
            if (resaveRaw) FitsJ.putHeader(impRaw, FitsJ.addAnnotateCard(value, labelText, FitsJ.getHeader(impRaw)));
        }
    }

    public void setAstrometryCanceled() {
        canceled = true;
    }

    public boolean isSetupActive() {
        return setupActive;
    }

    private int getPollingTimeout() {
        if (useAlternateAstrometryServer) {
            return CUSTOM_POLLING.get() * 1000;
        }

        return ASTRO_NET_POLLING.get() * 1000;
    }

    public boolean getAstrometryCanceled() {
        return canceled;
    }

//        /** Handle the key typed event from the image canvas. */
//    public void keyTyped(KeyEvent e) {
//
//    }
//
//    /** Handle the key-pressed event from the image canvas. */
//    public void keyPressed(KeyEvent e) {
//        int keyCode = e.getKeyCode();
//        if (keyCode == KeyEvent.VK_ESCAPE)//IJ.escapePressed())
//            {
//            IJ.beep();
//            canceled = true;
//            }
//        else if (keyCode == KeyEvent.VK_ENTER)
//            {
//            }
//    }
//
//    /** Handle the key-released event from the image canvas. */
//    public void keyReleased(KeyEvent e) {
//
//    }

    public void getPrefs() {
        userKey = Prefs.get("astrometry.userKey", userKey);
        useAlternateAstrometryServer = Prefs.get("astrometry.useAlternateAstrometryServer", useAlternateAstrometryServer);
        alternateAstrometryUrlBase = Prefs.get("astrometry.alternateAstrometryUrlBase", alternateAstrometryUrlBase);
        processStack = Prefs.get("astrometry.processStack", processStack);
        startSlice = (int) Prefs.get("astrometry.startSlice", startSlice);
        endSlice = (int) Prefs.get("astrometry.endSlice", endSlice);
        defaultAnnotationColor = Prefs.get("Astronomy_Tool.defaultAnnotationColor", defaultAnnotationColor);
        autoSave = Prefs.get("astrometry.autoSave", autoSave);
        DPSaveRawWithWCS = Prefs.get("astrometry.DPSaveRawWithWCS", DPSaveRawWithWCS);
        skipIfHasWCS = Prefs.get("astrometry.skipIfHasWCS", skipIfHasWCS);
        compress = Prefs.get("astrometry.gzip", compress);
        fpack = Prefs.get("astrometry.fpack", fpack);

        annotate = Prefs.get("astrometry.annotate", annotate);
        annotateRadius = Prefs.get("astrometry.annotateRadius", annotateRadius);
        addAnnotationsToHeader = Prefs.get("astrometry.addAnnotationsToHeader", addAnnotationsToHeader);

        useMedianFilter = Prefs.get("astrometry.useMedianFilter", useMedianFilter);
        medianFilterRadius = (int) Prefs.get("astrometry.medianFilterRadius", medianFilterRadius);

        minPeakFindToleranceSTDEV = Prefs.get("astrometry.minPeakFindToleranceSTDEV", minPeakFindToleranceSTDEV);
        useMaxPeakFindValue = Prefs.get("astrometry.useMaxPeakFindValue", useMaxPeakFindValue);
        maxPeakFindValue = Prefs.get("astrometry.maxPeakFindValue", maxPeakFindValue);

        maxNumStars = (int) Prefs.get("astrometry.maxNumStars", maxNumStars);
        useDistortionOrder = Prefs.get("astrometry.useDistortionOrder", useDistortionOrder);
        distortionOrder = (int) Prefs.get("astrometry.distortionOrder", distortionOrder);

        useCentroid = Prefs.get("astrometry.useCentroid", useCentroid);
        removeBackStars = Prefs.get("aperture.removebackstars", removeBackStars);
        backPlane = Prefs.get("aperture.backplane", backPlane);
        apertureRadius = Prefs.get("astrometry.apertureRadius", apertureRadius);
        apertureBack1 = Prefs.get("astrometry.apertureBack1", apertureBack1);
        apertureBack2 = Prefs.get("astrometry.apertureBack2", apertureBack2);

        useScale = Prefs.get("astrometry.useScale", useScale);
        scaleEstimate = Prefs.get("astrometry.scaleEstimate", scaleEstimate);
        scaleError = Prefs.get("astrometry.scaleError", scaleError);


        useRaDec = Prefs.get("astrometry.useRaDec", useRaDec);
        ra = Prefs.get("astrometry.ra", ra);
        dec = Prefs.get("astrometry.dec", dec);
        raDecRadius = Prefs.get("astrometry.raDecRadius", raDecRadius);

        showLog = Prefs.get("astrometry.showLog", showLog);
    }
}

