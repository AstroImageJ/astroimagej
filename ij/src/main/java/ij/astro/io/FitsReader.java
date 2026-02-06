package ij.astro.io;

import static ij.plugin.FITS_Reader.filter;
import static nom.tam.fits.header.Standard.BITPIX;
import static nom.tam.fits.header.Standard.BSCALE;
import static nom.tam.fits.header.Standard.BZERO;
import static nom.tam.fits.header.Standard.EXTNAME;
import static nom.tam.fits.header.Standard.NAXIS;
import static nom.tam.fits.header.Standard.NAXIS1;
import static nom.tam.fits.header.Standard.NAXIS2;
import static nom.tam.fits.header.Standard.NAXISn;
import static nom.tam.fits.header.Standard.TELESCOP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

import ij.IJ;
import ij.Prefs;
import ij.astro.logging.AIJLogger;
import ij.astro.util.ImageType;
import ij.astro.util.LeapSeconds;
import ij.astro.util.SkyAlgorithmsTimeUtil;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.FITS_Reader;
import ij.plugin.FolderOpener;
import ij.process.ImageProcessor;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsDate;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TableHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.image.compression.hdu.CompressedTableHDU;
import nom.tam.util.Cursor;

public class FitsReader implements AutoCloseable {
    public static boolean skipTessQualCheck = Prefs.getBoolean(".aij.skipTessQualCheck", false);
    private static final LeapSeconds LEAP_SECONDS = new LeapSeconds();
    private final Fits fits;
    private final List<HDUDescriptor> hduDescriptors;
    private final BasicHDU<?>[] hdus;
    private int firstImageIndex = -1;
    private int width = 0;
    private int height = 0;
    private int depth = 0;
    private double bZero = 0.0;
    private double bScale = 0.0;
    private final String directory;
    public final String fileName;
    public String fileBase;
    public String fileType;
    private ImageProcessor[] cachedProcessors;
    private String[] cachedHeaders;
    private boolean isMeasurementsTable;

    private FitsReader(Path path, String directory, String fileName) throws IOException {
        FitsFactory.setAllowHeaderRepairs(true);
        this(new Fits(path.toFile()), directory, fileName);
    }

    private FitsReader(Fits fits, String directory, String fileName) throws IOException {
        this.directory = directory;
        this.fileName = fileName;
        this.fits = fits;
        this.hdus = fits.read(); // Reads to end of File, and for RandomAccess files skips reading data
        hduDescriptors = new ArrayList<>(fits.getNumberOfHDUs());

        for (int i = 0; i < fits.getNumberOfHDUs(); i++) {
            hduDescriptors.add(new HDUDescriptor(fits.getHDU(i)));
        }

        setupImageData();
    }

    public static FitsReader create(String path) throws IOException {
        OpenDialog od = new OpenDialog("Open FITS...", path);
        var directory = od.getDirectory();
        var fileName = od.getFileName();
        if (fileName == null) {
            throw new FitsException("Null filename.");
        }

        IJ.showStatus("Opening: " + directory + fileName);

        if (path.contains(".zip")) {
            var s = path.split("\\.zip");

            try (var zip = new ZipFile(s[0] + ".zip")) {
                var m = new ProgressMonitorInputStream(IJ.getInstance(),
                        "Reading FITS image", zip.getInputStream(zip.getEntry(s[1].substring(1))));
                FitsFactory.setAllowHeaderRepairs(true);
                return new FitsReader(new Fits(m), directory, fileName);
            }
        }

        return new FitsReader(Path.of(path), directory, fileName);
    }

    /**
     * Sets up dimensions for image data and find first HDU from the file with NAXIS > 1,
     * assume it is an image.
     */
    private void setupImageData() {
        for (int i = 0; i < hduDescriptors.size(); i++) {
            var descriptor = hduDescriptors.get(i);
            var header = descriptor.getFormedHeader();
            if (header.getIntValue(Standard.NAXIS) > 1) {
                firstImageIndex = i;
                width = header.getIntValue(Standard.NAXIS1);
                height = header.getIntValue(Standard.NAXIS2);
                depth = header.getIntValue(NAXISn.n(3), 1);
                bZero = header.getDoubleValue(BZERO, 0.0);
                bScale = header.getDoubleValue(BSCALE, 1.0);
                break;
            }
        }
    }

    public String[] getHeaders() throws IOException {
        if (cachedHeaders != null) {
            return cachedHeaders.clone();
        }

        var result = processFits(false);
        cachedHeaders = result.headers().toArray(new String[0]);
        return cachedHeaders.clone();
    }

    public int size() {
        var descriptor = hduDescriptors.get(firstImageIndex);

        if (isMeasurementsTable()) {
            return 0;
        }

        if (cachedProcessors != null) {
            return cachedProcessors.length;
        }

        if (cachedHeaders != null) {
            return cachedHeaders.length;
        }

        if (firstImageIndex < 0) {
            return 0;
        }

        var header = descriptor.getFormedHeader();
        if (isBasic3DImageDescriptors()) {
            return countStackableImagesFromDescriptors();
        }

        if (descriptor.isTable()) {
            return header.getIntValue(NAXIS2, 0);
        }

        if (header.getIntValue(NAXIS) == 3) {
            return header.getIntValue(NAXISn.n(3), 1);
        }

        return 1;
    }

    public boolean isMeasurementsTable() {
        if (!isMeasurementsTable && hduDescriptors.get(firstImageIndex).isTable()) {
            isMeasurementsTable = !(isTessSpocFfiCut(firstImageIndex) ||
                            isTessSpocPostageStamp() || isTessTicaFfiCut(firstImageIndex));
        }

        return isMeasurementsTable;
    }

    public ImageProcessor[] getProcessors() throws IOException {
        if (cachedProcessors != null) {
            return cachedProcessors.clone();
        }

        var result = processFits(true);
        cachedProcessors = result.processors().toArray(new ImageProcessor[0]);
        cachedHeaders = result.headers().toArray(new String[0]);
        return cachedProcessors.clone();
    }

    private ProcessedFits processFits(boolean includeProcessors) throws IOException {
        if (firstImageIndex < 0) {
            return new ProcessedFits(List.of(), List.of());
        }

        try {
            generateTimings(firstImageIndex);
        } catch (HeaderCardException e) {
            AIJLogger.log("Failed to generate FITS timing metadata: " + e.getMessage());
        }

        if (isTessTicaFfiFull(firstImageIndex)) {
            if (hduDescriptors.get(firstImageIndex).getFormedHeader().getIntValue("QUAL_BIT") != 0) {
                IJ.error("Skipped TICA image as QUAL_BIT is nonzero.");
                return new ProcessedFits(List.of(), List.of());
            }
        }

        if (isHyperSup()) {
            var primaryH = hduDescriptors.getFirst().getFormedHeader();
            primaryH.seekHead();
            primaryH.deleteKey("MJD-OBS");
            primaryH.findCard("DATE-OBS")
                    .setValue(primaryH.getStringValue("DATE-OBS") + "T" + primaryH.getStringValue("UT-STR"));
            primaryH.addLine(primaryH.findCard("EXPTIME"));
        }

        var displayDescriptor = hduDescriptors.get(firstImageIndex);

        if (displayDescriptor.isTable()) {
            var hdu = fits.getHDU(firstImageIndex);
            TableHDU<?> tableHDU = (TableHDU<?>) hdu;
            if (tableHDU instanceof CompressedTableHDU compressedTableHDU) {
                tableHDU = compressedTableHDU.asBinaryTableHDU();
            }

            if (isTessSpocFfiCut(firstImageIndex) || isTessSpocPostageStamp() || isTessTicaFfiCut(firstImageIndex)) {
                var hdr = convertHeaderForFfi(hduDescriptors.get(2).getFormedHeader(), tableHDU);
                var headers = makeHeadersTessCut(hdr, tableHDU, firstImageIndex);

                if (filter != null && !filter.matchesFilter(hdr)) {
                    return new ProcessedFits(List.of(), List.of());
                }

                if (!includeProcessors) {
                    return new ProcessedFits(List.of(), headers);
                }

                var data = (Object[]) tableHDU.getColumn("FLUX");
                return processorsFrom3DData(data, tableHDU.getNRows(), headers, true);
            } else {
                var mt = fitsTable2MeasurementsTable(hdus, tableHDU, Set.of());
                if (mt != null) {
                    mt.show("Measurements in " + fileName);
                }
            }

            return new ProcessedFits(List.of(), List.of());
        }

        if (isBasic3DImage()) {
            return processorsFromManyHdu(includeProcessors);
        }

        var header = hduDescriptors.get(firstImageIndex).getFormedHeader();
        var naxis = header.getIntValue(NAXIS);
        if (naxis == 2) {
            var headerText = headerToString(header);
            var headers = List.of(headerText);

            if (filter != null && !filter.matchesFilter(header)) {
                return new ProcessedFits(List.of(), List.of());
            }

            if (!includeProcessors) {
                return new ProcessedFits(List.of(), headers);
            }

            var processor = twoDimensionalImageData2Processor(firstImageIndex);
            return new ProcessedFits(List.of(processor), headers);
        }

        if (naxis == 3) {
            if (FolderOpener.virtualIntended) {
                AIJLogger.log("Cannot open 3D images as a virtual stack.", false);
            }

            var imageCount = header.getIntValue(NAXISn.n(3), 1);
            var headers = List.of(headerToString(header));
            if (!includeProcessors) {
                return new ProcessedFits(List.of(), repeatHeader(headers.getFirst(), imageCount));
            }

            return processorsFrom3DData(firstImageIndex, imageCount, headers, true);
        }

        return new ProcessedFits(List.of(), List.of());
    }

    private ResultsTable fitsTable2MeasurementsTable(BasicHDU<?>[] hdus, TableHDU<?> tableHDU,
                                                     Set<Opener.OpenOption> openOptions) {
        ResultsTable mt;
        try {
            // Work around access issues
            var cl = IJ.getClassLoader().loadClass("astroj.MeasurementTable");
            mt = (ResultsTable) cl.getConstructor(String.class).newInstance(fileName + " Measurements");
            var f = cl.getDeclaredField("filePath");
            f.setAccessible(true);
            f.set(mt, fileName);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
            mt = new ResultsTable(tableHDU.getNRows());
        }

        var tableRead = FITS_Reader.fitsTable2MeasurementsTable(mt, hdus, tableHDU, openOptions);
        return mt;
    }

    private List<String> repeatHeader(String header, int count) {
        var headers = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            headers.add(header);
        }
        return headers;
    }

    /**
     * From 3D array of pixel data, create a stack. Uses provided Header to set info for processes such
     * as MultiAperture.
     */
    private ProcessedFits processorsFrom3DData(int index, int imageCount, List<String> headers, boolean includeProcessors) throws IOException {
        Object[] data = null;

        if (includeProcessors) {
            var hdu = fits.getHDU(index);
            if (hdu instanceof ImageHDU) {
                data = (Object[]) hdu.getKernel();
            } else if (hdu instanceof CompressedImageHDU compressedImageHDU) {
                data = (Object[]) compressedImageHDU.asImageHDU().getKernel();
            } else {
                throw new IllegalStateException("Not supported HDU implementation for " + hdu.getClass().getName());
            }
        }

        return processorsFrom3DData(data, imageCount, headers, includeProcessors);
    }

    /**
     * From 3D array of pixel data, create a stack. Uses provided Header to set info for processes such
     * as MultiAperture.
     */
    private ProcessedFits processorsFrom3DData(Object[] data, int imageCount, List<String> headers, boolean includeProcessors) {
        var processors = new ArrayList<ImageProcessor>();
        var outputHeaders = new ArrayList<String>();

        var pm = makeMonitor(imageCount);
        for (int i = 0; i < imageCount; i++) {
            var header = "";
            if (headers != null && !headers.isEmpty()) {
                var headerIndex = headers.size() == 1 ? 0 : i;
                var headerValue = headers.get(headerIndex);
                if (headerValue.contains("AIJ_Q")) { // For TESScut, skip bad images
                    AIJLogger.log("     Skipping an image due to quality flag: " + (i+1));
                    continue;
                } else if (headerValue.contains("AIJ_Q2")) { // For Postage stamp, skip null images
                    continue;
                } else if (headerValue.contains("NO_BJD")) { // For TESScut, skip if no BJD available
                    AIJLogger.log("     Skipping an image due to a missing or invalid BJD time: " + (i+1));
                    continue;
                }
                header = headerValue + "\n";
            }

            if (includeProcessors) {
                assert data != null;
                processors.add(twoDimensionalImageData2Processor(data[i]));
            }
            outputHeaders.add(header);
            pm.setProgress(i);
        }

        return new ProcessedFits(processors, outputHeaders);
    }

    /**
     * Create a stack from a fits file that only contains multiple images
     */
    private ProcessedFits processorsFromManyHdu(boolean includeProcessors) throws IOException {
        var processors = new ArrayList<ImageProcessor>();
        var headers = new ArrayList<String>();

        var noExtensionNames =
                hduDescriptors.stream().allMatch(descriptor ->
                        Objects.isNull(descriptor.getFormedHeader().getStringValue(EXTNAME)));
        var noScienceImage =
                hduDescriptors.stream().noneMatch(descriptor ->
                        Objects.equals("SCI", descriptor.getFormedHeader().getStringValue(EXTNAME)));

        if (!noExtensionNames && noScienceImage) {
            AIJLogger.log("Multi-image file must contain at least one HDU with the name of 'SCI'");
        }

        for (int i = 0; i < hduDescriptors.size(); i++) {
            var header = hduDescriptors.get(i).getFormedHeader();
            if (header.getIntValue(NAXIS) == 0) {
                continue;
            }
            if (!noExtensionNames && !Objects.equals("SCI", header.getStringValue(EXTNAME))) {
                continue;
            }

            headers.add(headerToString(header));
            if (includeProcessors) {
                processors.add(twoDimensionalImageData2Processor(i));
            }
        }

        return new ProcessedFits(processors, headers);
    }

    /**
     * Convert 2D image data into an ImageProcessor, scale image data
     * <p>
     * Data is transposed to match {@link ImageProcessor} implementations
     * (see {@link ImageProcessor#getPixelValue(int, int)})
     */
    private ImageProcessor twoDimensionalImageData2Processor(int imageIndex) throws IOException {
        var hdu = fits.getHDU(imageIndex);
        Object imageData = null;
        if (hdu instanceof CompressedImageHDU compressedImageHDU) {
            imageData = compressedImageHDU.asImageHDU().getKernel();
        } else if (hdu instanceof ImageHDU imageHDU) {
            imageData = imageHDU.getKernel();
        } else {
            throw new IllegalStateException("Unsupported HDU type: " + hdu.getClass().getName());
        }

        return twoDimensionalImageData2Processor(imageData);
    }

    /**
     * Convert 2D image data into an ImageProcessor, scale image data
     * <p>
     * Data is transposed to match {@link ImageProcessor} implementations
     * (see {@link ImageProcessor#getPixelValue(int, int)})
     */
    private ImageProcessor twoDimensionalImageData2Processor(Object imageData) {
        var type = ImageType.getType(imageData, bScale, bZero);
        var imgtmp = type.makeProcessor(width, height);
        var pixels = type.processImageData(imageData, width, height, bZero, bScale);
        return conditionImageProcessor(pixels, imgtmp);
    }

    /**
     * Set pixel and scaling data of the ImageProcessor, flip the image vertically.
     */
    private ImageProcessor conditionImageProcessor(Object pixels, ImageProcessor imgtmp) {
        imgtmp.setPixels(pixels);
        imgtmp.resetMinAndMax();

        if (height == 1) {
            imgtmp = imgtmp.resize(width, 100);
        }
        if (width == 1) {
            imgtmp = imgtmp.resize(100, height);
        }
        return imgtmp;
    }

    private String headerToString(Header header) {
        final var baos = new ByteArrayOutputStream();
        final var utf8 = StandardCharsets.UTF_8.name();
        try (PrintStream ps = new PrintStream(baos, true, utf8)) {
            ps.println("AIJ-HEADER-MARKER");
            header.dumpHeader(ps);
        } catch (Exception ignored) {}

        return baos.toString();
    }

    private boolean isBasic3DImage() {
        // For compressed multiHDU files, the first HDU likely has no data as it
        // was added to allow for compression.
        var firstValidHdu = hduDescriptors.getFirst().getFormedHeader().getIntValue(NAXIS) == 0 ? 1 : 0;
        return isBasic3DImageDescriptors() && processBasic3DImage(firstValidHdu);
    }

    private boolean isBasic3DImageDescriptors() {
        if (hduDescriptors.size() <= 1 || isLco()) {
            return false;
        }

        var firstValidHdu = hduDescriptors.getFirst().getFormedHeader().getIntValue(NAXIS) == 0 ? 1 : 0;

        var isImages = hduDescriptors.stream()
                .skip(firstValidHdu)
                .allMatch(HDUDescriptor::isImage);

        if (!isImages) {
            return false;
        }

        var firstAxes = hduDescriptors.get(firstValidHdu).getAxesFromHeader();
        return hduDescriptors.stream()
                .skip(firstValidHdu)
                .allMatch(descriptor -> Arrays.equals(firstAxes, descriptor.getAxesFromHeader()));
    }

    private int countStackableImagesFromDescriptors() {
        var noExtensionNames = hduDescriptors.stream()
                .allMatch(descriptor -> Objects.isNull(descriptor.getFormedHeader().getStringValue(EXTNAME)));
        var noScienceImage = hduDescriptors.stream()
                .noneMatch(descriptor -> Objects.equals("SCI", descriptor.getFormedHeader().getStringValue(EXTNAME)));

        if (!noExtensionNames && noScienceImage) {
            AIJLogger.log("Multi-image file must contain at least one HDU with the name of 'SCI'");
        }

        var count = 0;
        for (var descriptor : hduDescriptors) {
            if (descriptor.hduType != HDUType.IMAGE && descriptor.hduType != HDUType.COMPRESSED_IMAGE) {
                continue;
            }
            var header = descriptor.getFormedHeader();
            if (header.getIntValue(NAXIS) == 0) {
                continue;
            }
            if (!noExtensionNames && !Objects.equals("SCI", header.getStringValue(EXTNAME))) {
                continue;
            }
            count++;
        }

        return count;
    }

    private boolean processBasic3DImage(int index) {
        if (isTessSpocFfiFull(index)) {
            var hdr = hduDescriptors.get(index).getFormedHeader();
            var isCalImage = "cal".equals(hdr.findCard("IMAGTYPE").getValue().trim());
            if (isCalImage) {
                // Copy primary header to displayHDU
                if (hdr.getBooleanValue("INHERIT", false)) {
                    Cursor<String, HeaderCard> j = hduDescriptors.getFirst().getFormedHeader().iterator();

                    while (j.hasNext()) {
                        HeaderCard card = j.next();

                        if (card.isCommentStyleCard()) {
                            if (card.getKey().startsWith("COMMENT") ||
                                    card.getKey().startsWith("HISTORY") ||
                                    card.getKey().startsWith("END")) {
                                continue;
                            }
                            hdr.insertCommentStyle(card.getKey(), card.getComment());
                        } else {
                            if (!hdr.containsKey(card.getKey()) && !"SIMPLE".equals(card.getKey())) {
                                try {
                                    hdr.updateLine(card.getKey(), card);
                                } catch (HeaderCardException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
            return !isCalImage;
        }

        return true;
    }

    /**
     * Convert base image header for TESScut images to a 3D FITS image.
     * <p>
     * Updates {@link FitsReader#width}, {@link FitsReader#height}, and {@link FitsReader#depth}
     */
    private Header convertHeaderForFfi(Header header, TableHDU<?> tableHDU) {
        /*header.setNaxes(3);
        header.setNaxis(3, tableHDU.getNRows());*/
        header.addValue(NAXIS, 3);
        header.addValue(NAXISn.n(3), tableHDU.getNRows());
        header.seekHead();
        header.deleteKey("EXTNAME");
        header.deleteKey("INSTRUME");
        header.deleteKey("TELESCOP");
        header.deleteKey("CHECKSUM");
        width = header.getIntValue(NAXIS1);
        height = header.getIntValue(NAXIS2);
        depth = tableHDU.getNRows();

        return header;
    }

    /**
     * Creates the header string for each image within a TESScut.
     * <p>
     * Adds BJD_TDB to the header, and does the evaluation of image quality.
     * Modifies the header.
     * <p>
     * Adapted from TESS Cut code by John Kielkopf.
     */
    private List<String> makeHeadersTessCut(final Header hdr, final TableHDU<?> tableHDU, int index) {
        List<String> headers = new ArrayList<>(tableHDU.getNRows());
        hdr.seekHead();
        hdr.deleteKey("DATE-OBS");
        hdr.deleteKey("DATE-END");

        try {
            var bjdColumn = tableHDU.findColumn("TIME");
            var qualityColumn = tableHDU.findColumn("QUALITY");

            if (bjdColumn >= 0) {
                hdr.deleteKey("BJDREFI");
                hdr.deleteKey("BJDREFF");
            }

            // Control for logging data
            var hasErrors = false;

            for (int i = 0; i < tableHDU.getNRows(); i++) {
                hdr.seekHead();
                // Delete previously added keys as hdr object is not a copy and is shared for all images
                hdr.deleteKey("BJD_TDB");
                hdr.deleteKey("AIJ_Q");
                hdr.deleteKey("AIJ_Q2");
                hdr.deleteKey("NO_BJD");
                hdr.deleteKey("NAXIS3");
                hdr.deleteKey("OBJECT");

                var bjd0 = 2457000d;
                var bjd1 = 0d;
                bjd1 = ((double[])tableHDU.getElement(i, bjdColumn))[0];
                if (!Double.isNaN(bjd0 + bjd1)) {
                    hdr.addValue("BJD_TDB", bjd0 + bjd1, "Calc. BJD_TDB");
                }

                var quality = ((int[])tableHDU.getElement(i, qualityColumn))[0];
                if (isTessSpocFfiCut(index) || isTessTicaFfiFull(index)) {
                    // If the image should be skipped add this card, string check for 'AIJ_Q' to skip image
					// Based on TESS Cut code by John Kielkopf
                    if ((!skipTessQualCheck && quality != 0)) {
                        hasErrors = true;
                        hdr.addValue("AIJ_Q", true, "Skipped due to quality flag");
                    } else if (Double.isNaN(bjd1)) {
                        hasErrors = true;
                        hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
                    }
                    if (isTessTicaFfiCut(0)) {
                        hdr.deleteKey("DATE-OBS");
                        hdr.deleteKey("DATE-END");
                    }
                } else if (isTessSpocPostageStamp()) {
                    hdr.addValue("OBJECT", hduDescriptors.getFirst().getFormedHeader().getStringValue("OBJECT"), "Object ID");
                    if (quality == 8) {
                        hdr.addValue("AIJ_Q2", true, "Null image");
                    } else if ((!skipTessQualCheck && quality != 0)) {
                        hasErrors = true;
                        hdr.addValue("AIJ_Q", true, "Skipped due to quality flag");
                    } else if (Double.isNaN(bjd1)) {
                        hasErrors = true;
                        hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
                    }
                }

                headers.add(headerToString(hdr));
            }
            if (hasErrors) {
                AIJLogger.log("Opening: " + fileName);
            }
        } catch (Exception ignored) {}

        return headers;
    }

    private boolean isHyperSup() {
        return "Hyper Suprime-Cam".equals(hduDescriptors.getFirst().getFormedHeader().getStringValue("INSTRUME"));
    }

    /**
     * Determine if an image is from LCO.
     * <p>
     * Most LCO images contain 3 image HDUs, with the science image separated from the others by a table HDU.
     * In some cases, this table is missing, which would make LCO images be treated as a multiHDU FITS file,
     * opening an image stack. This behavior is not desired.
     */
    private boolean isLco() {
        if (hduDescriptors.size() <= 1) {
            return false;
        }
        var x = hduDescriptors.get(1).getFormedHeader().getStringValue("ORIGIN");
        var d = hduDescriptors.get(1).getFormedHeader().getStringValue(EXTNAME);
        return "LCOGT".equals(x == null ? null : x.trim()) && "SCI".equals(d == null ? null : d.trim());
    }

    /**
     * Determine if a table is a TESS 2-minute postage stamp.
     */
    private boolean isTessSpocPostageStamp() {
        var hdr = hduDescriptors.getFirst().getFormedHeader();
        return ("TESS").equals(hdr.getStringValue(TELESCOP)) && hdr.containsKey("CREATOR") &&
                hdr.getStringValue("CREATOR").contains("TargetPixelExporterPipelineModule");
    }

    /**
     * Determine if the image is a TESS FFI.
     */
    private boolean isTessSpocFfiFull(int i) {
        var hdr = hduDescriptors.get(i).getFormedHeader();
        var telescope = hdr.findCard("TELESCOP");
        var imageType = hdr.findCard("IMAGTYPE");

        if (telescope == null || imageType == null) {
            return false;
        }

        var tVal = Objects.requireNonNullElse(telescope.getValue(), "");
        var iCom = Objects.requireNonNullElse(imageType.getComment(), "");

        return tVal.strip().equals("TESS") && iCom.contains("FFI image type");
    }

    /**
     * Determine if a table is a TESS cut.
     */
    private boolean isTessSpocFfiCut(int i) {
        var hdr = hduDescriptors.get(i).getFormedHeader();
        return "astrocut".equals(hdr.getStringValue("CREATOR")) && hdr.findCard("TICAVER") == null;
    }

    /**
     * Determine if the image is a TICA image
     */
    private boolean isTessTicaFfiFull(int i) {
        return isTessTicaFfiFull(hduDescriptors.get(i).getFormedHeader());
    }

    /**
     * Determine if a table is a TESS cut.
     */
    private boolean isTessTicaFfiCut(int i) {
        return isTessTicaFfiCut(hduDescriptors.get(i).getFormedHeader());
    }

    /**
     * Determine if the image is a TICA image
     */
    private boolean isTessTicaFfiFull(Header hdr) {
        return null != hdr.findCard("TICAVER") && hdr.findCard("CREATOR") == null;
    }

    /**
     * Determine if a table is a TESS cut.
     */
    private boolean isTessTicaFfiCut(Header hdr) {
        return null != hdr.findCard("TICAVER") && "astrocut".equals(hdr.getStringValue("CREATOR"));
    }

    /**
     * Calculate BJD_TDB for TESS or TICA FFIs as they are missing it.
     * <p>
     * Note: Assumes all needed cards are present.
     */
    private void generateTimings(int i) throws HeaderCardException {
        var header = hduDescriptors.get(i).getFormedHeader();

        if (isTessSpocFfiFull(i)) {
            var bjdRefi = header.getIntValue("BJDREFI");
            var bjdReff = header.getDoubleValue("BJDREFF");
            var tStart = header.getDoubleValue("TSTART");
            var telapse = header.getDoubleValue("TELAPSE");

            var bjdTdb = bjdRefi + bjdReff + tStart + telapse/2.0;
            header.addValue("BJD_TDB", bjdTdb, "Calc by AIJ as BJDREFI+BJDREFF+TSTART+TELAPSE/2.0");
        }

        if (isTessTicaFfiFull(i) && !isTessTicaFfiCut(i)) {
            var tjdZero = header.getDoubleValue("TJD_ZERO");
            var startTjd = header.getDoubleValue("STARTTJD");
            var jdTdb = tjdZero + startTjd;

            var leapSeconds = LEAP_SECONDS.getLeapSeconds(jdTdb);

            var jdUtc = jdTdb - leapSeconds - 32.184;

            var dt = SkyAlgorithmsTimeUtil.UTDateFromJD(jdUtc);
            var hmsms = SkyAlgorithmsTimeUtil.ut2Array(dt[3]);

            if (!(isInRangeInc(dt[1], 1, 12) && isInRangeInc(dt[2], 1, 31) &&
                    isInRangeInc(hmsms[0], 0, 23) && isInRangeInc(hmsms[1], 0, 59) &&
                    isInRangeInc(hmsms[2], 0, 59))) {
                AIJLogger.log("Failed to extract timing data.");
                return;
            }

            var dateTime = LocalDateTime.of((int) dt[0], (int) dt[1], (int) dt[2], hmsms[0], hmsms[1],
                    hmsms[2]).toInstant(ZoneOffset.ofTotalSeconds(0));
            dateTime = dateTime.plusMillis(hmsms[3]);
            header.addValue("DATE-OBS", FitsDate.getFitsDateString(Date.from(dateTime)),
                    "[UTC] Start date and time of obs.");

            header.addValue("TELAPSE", header.getIntValue("INT_TIME"), "Integration time (s)");
        }
    }

    private boolean isInRangeInc(double v, double min, double max) {
        return v >= min && v <= max;
    }

    /**
     * Generate {@link FileInfo} for the FITS image for use in displaying it.
     */
    public FileInfo decodeFileInfo() throws FitsException {
        var header = hduDescriptors.get(firstImageIndex).getFormedHeader();

        var fi = new FileInfo();
        fi.fileFormat = FileInfo.FITS;
        fi.fileName = fileName.endsWith(".fz") ? fileName.substring(0, fileName.length() - 3) : fileName;
        if (fileName.lastIndexOf('.') > 0) {
            if (fileName.startsWith("tess-s")) {
                fileBase = fileName.substring(0, 14);
            } else {
                fileBase = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            fileType = fileName.substring(fileName.lastIndexOf('.') + 1);
        } else {
            fileBase = "Slice_";
            fileType = "";
        }
        fi.directory = directory;
        fi.width = width;
        fi.height = height;
        fi.nImages = depth;
        fi.pixelWidth = header.getDoubleValue("CDELT1");
        fi.pixelHeight = header.getDoubleValue("CDELT2");
        fi.pixelDepth = header.getDoubleValue("CDELT3");
        fi.unit = header.getStringValue("CTYPE1");
        int bitsPerPixel = header.getIntValue(BITPIX);
        fi.fileType = fileTypeFromBitsPerPixel(bitsPerPixel);
        fi.offset = (int)header.getMinimumSize(); // downcast because spec is allowing for a lot of headers!
        return fi;
    }

    /**
     * Converts BITPIX to a {@link FileInfo} for image display.
     * <br></br>
     * Note: In 2005, the 64-bit signed integer was added, with BITPIX=64
     * <br></br>
     * Note: The FITS specification indicates valid BITPIX is only for BITPIX ∈ {-64, -32, 8, 16, 32,
     * 64}. Images with nonconforming values may exist and can be read via nom.tam.fits, but this will throw if an
     * invalid value is given.
     *
     * @see <a href="https://fits.gsfc.nasa.gov/fits_primer.html#:~:text=unit.-,data%20units,-The">FITS Primer</a> or
     * the <a href="https://fits.gsfc.nasa.gov/fits_standard.html">FITS specification</a>
     * for information on types stored, and the use of BITPIX card to process them.
     */
    private int fileTypeFromBitsPerPixel(int bitsPerPixel) throws FitsException {
        return switch (bitsPerPixel) {
            case 8 -> FileInfo.GRAY8;
            case 16 -> FileInfo.GRAY16_SIGNED;
            case 32 -> FileInfo.GRAY32_INT;
            case -32 -> FileInfo.GRAY32_FLOAT;
            case -64 -> {
                AIJLogger.log("Opening a double precision image as single precision... " + fileName);
                yield FileInfo.GRAY64_FLOAT;
            }
            //todo Handle 64-bit integer datatype - needs work on FileInfo side and its usages
            default -> throw new FitsException("BITPIX must be 8, 16, 32, -32 or -64, but BITPIX=" + bitsPerPixel);
        };
    }

    private ProgressMonitor makeMonitor(int size) {
        return new ProgressMonitor(IJ.getInstance(), "Processing HDUs in FITS image.", null, 0, size - 1);
    }

    @Override
    public void close() throws Exception {
        fits.close();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    private record ProcessedFits(List<ImageProcessor> processors, List<String> headers) {}

    record HDUDescriptor(Header original, Header decompressed, HDUType hduType) {
        HDUDescriptor(BasicHDU<?> hdu) {
            Header decompressed = null;
            if (hdu instanceof CompressedImageHDU compressedImageHDU) {
                decompressed = compressedImageHDU.getImageHeader();
            }

            if (hdu instanceof CompressedTableHDU compressedTableHDU) {
                decompressed = compressedTableHDU.getTableHeader();
            }
            this(hdu.getHeader(), decompressed, HDUType.fromHdu(hdu));
        }

        Header getFormedHeader() {
            if (decompressed != null) {
                return decompressed;
            }

            return original;
        }

        int[] getAxesFromHeader() {
            var header = getFormedHeader();
            var axesCount = header.getIntValue(NAXIS);
            if (axesCount <= 0) {
                return new int[0];
            }
            var axes = new int[axesCount];
            for (int i = 0; i < axesCount; i++) {
                axes[i] = header.getIntValue(NAXISn.n(i + 1).key());
            }
            return axes;
        }

        public boolean isImage() {
            return hduType == HDUType.IMAGE || hduType == HDUType.COMPRESSED_IMAGE;
        }

        public boolean isCompressed() {
            return hduType.compressed;
        }

        public boolean isTable() {
            return hduType == HDUType.TABLE || hduType == HDUType.COMPRESSED_TABLE;
        }
    }

    private enum HDUType {
        IMAGE(false),
        TABLE(false),
        COMPRESSED_IMAGE(true),
        COMPRESSED_TABLE(true),
        ;

        private final boolean compressed;

        HDUType(boolean compressed) {
            this.compressed = compressed;
        }

        static HDUType fromHdu(BasicHDU<?> hdu) {
            if (hdu instanceof CompressedImageHDU) {
                return COMPRESSED_IMAGE;
            } else if (hdu instanceof CompressedTableHDU) {
                return COMPRESSED_TABLE;
            } else if (hdu instanceof TableHDU<?>) {
                return TABLE;
            }

            return IMAGE;
        }
    }
}
