package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.logging.AIJLogger;
import ij.astro.logging.Translation;
import ij.astro.types.Pair;
import ij.astro.util.ArrayBoxingUtil;
import ij.astro.util.ImageType;
import ij.astro.util.LeapSeconds;
import ij.astro.util.SkyAlgorithmsTimeUtil;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import nom.tam.fits.*;
import nom.tam.image.compression.hdu.CompressedImageHDU;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import static nom.tam.fits.header.Standard.*;


/** Opens and displays FITS images. The FITS format is 
	described at "http://fits.gsfc.nasa.gov/fits_standard.html".
	Add setOption("FlipFitsImages",true) to the
	Edit/Options/Startup dialog to have images flipped vertically.
*/
@AstroImageJ(reason = "Support for compressed FITS files via nom.tam.fits, invert flipImages to fix inverted aperture display",
		modified = true)
@Translation("Fits Reader")
public class FITS_Reader extends ImagePlus implements PlugIn {
	private static boolean flipImages;
	// private WCS wcs;
	private ImagePlus imagePlus;
	private String directory;
	private String fileName;
	private String fileBase;
	private String fileType;
	private int wi;
	private int he;
	private int de;
	@AstroImageJ(reason = "make double for the images that need it", modified = true)
	private double bzero;
	@AstroImageJ(reason = "make double for the images that need it", modified = true)
	private double bscale;

	private Locale locale = Locale.US;
	private DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
	private DecimalFormat fourDigits = new DecimalFormat("0000", dfs);

	public static boolean skipTessQualCheck = Prefs.getBoolean(".aij.skipTessQualCheck", false);

	// The image data comes in different types, but in the end, we turn them all into floats.
	// So no matter what type the data is, we wrap it with a lambda that takes two indices and
	// returns a float. This uses floats as there is no DoubleProcessor from imagej
	@FunctionalInterface
	private interface TableWrapper { double valueAt(int x, int y); }

	private static HeaderCardFilter filter = null;

	/**
	 * Main processing method for the FITS_Reader object
	 *
	 * @param path path of FITS file
	 */
	public void run(String path) {
		// wcs = null;
		imagePlus = null;

		AIJLogger.setLogAutoCloses(Prefs.getBoolean(AIJLogger.CERTAIN_LOGS_AUTO_CLOSE, true));

		/*
		 * Extract array of HDU from FITS file using nom.tam.fits
		 * This also uses the old style FITS decoder to create a FileInfo.
		 */
		BasicHDU<?>[] hdus;
		boolean canUseNom = true;
		PostFitsRead postFitsRead = null;
		try {
			postFitsRead = getHDU(path);
		} catch (FitsException | IOException e) {
			canUseNom = false;
			hdus = null;
			e.printStackTrace();
		}

		if (canUseNom && postFitsRead != null) { // Use nom.tam.fits to open files
			hdus = postFitsRead.hdus;

			/*
			 * For fpacked files the image is in the second HDU. For uncompressed images
			 * it is the first HDU.
			 */
			BasicHDU<?> displayHdu;
			int firstImageIndex = firstImageHDU(hdus);
			if (firstImageIndex<0)
			{
				IJ.error("Failed to find an image HDU");
				return;
			}

			try {
				if (isCompressedFormat(hdus, firstImageIndex)) {
					// A side effect of this call is that wi, he, and de are set
					displayHdu = getCompressedImageData((CompressedImageHDU) hdus[firstImageIndex]);
				} else {
					displayHdu = hdus[firstImageIndex];
					wi = displayHdu.getHeader().getIntValue("NAXIS1");
					he = displayHdu.getHeader().getIntValue("NAXIS2");
					de = 1; // Use displaySingleImage
				}
			} catch (FitsException e) {
				IJ.error("AIJ does not recognized this file as a FITS file: " + e.getMessage());
				return;
			}
			//IJ.log("Opened with nom.tam.fits");
			bzero = (float) displayHdu.getBZero();
			bscale = (float) displayHdu.getBScale();

			// Create the fileInfo.
			try {
				FileInfo fileInfo = decodeFileInfo(displayHdu);
				// ImagePlus has a private member named fileInfo. This inherited method sets it.
				setFileInfo(fileInfo);
			} catch (FitsException e) {
				IJ.error("Failed to decode fileInfo: " + e.getMessage());
				return;
			}

			Data imgData = displayHdu.getData();

			if ((wi < 0) || (he < 0)) {
				IJ.error("This does not appear to be a FITS file. " + wi + " " + he);
				return;
			}

			if (de >= 1){
				try {
					displaySingleImage(displayHdu, imgData, hdus);
				} catch (FitsException e) {
					IJ.error("Failed to display single image: " + e.getMessage());
					return;
				}
			} else {
				displayStackedImage();
			}

			setProperty("Info", getHeaderInfo(displayHdu));

			closeThing(postFitsRead.fits);

			IJ.showStatus("");
		} else {   // Use legacy fits reader in case of failure
			AIJLogger.log("Failed to open image with nom.tam.fits, attempting with legacy mode...");

			OpenDialog od = new OpenDialog("Open FITS...", path);
			String directory = od.getDirectory();
			String fileName = od.getFileName();
			if (fileName==null)
				return;
			IJ.showStatus("Opening: " + directory + fileName);
			FitsDecoder fd = new FitsDecoder(directory, fileName);
			FileInfo fi = null;
			try {
				fi = fd.getInfo();
			} catch (IOException ignored) {}
			if (fi!=null && fi.width>0 && fi.height>0 && fi.offset>0) {
				FileOpener fo = new FileOpener(fi);
				ImagePlus imp = fo.openImage();
				if (!flipImages) {
					if (fi.nImages==1) {
						ImageProcessor ip = imp.getProcessor();
						ip.flipVertical(); // origin is at bottom left corner
						setProcessor(fileName, ip);
					} else {
						ImageStack stack = imp.getStack(); // origin is at bottom left corner
						for(int i=1; i<=stack.getSize(); i++)
							stack.getProcessor(i).flipVertical();
						setStack(fileName, stack);
					}
				}
				setStack(fileName, imp.getStack());
				Calibration cal = imp.getCalibration();
				if (fi.fileType==FileInfo.GRAY16_SIGNED && fd.bscale==1.0 && fd.bzero==32768.0)
					cal.setFunction(Calibration.NONE, null, "Gray Value");
				setCalibration(cal);
				setProperty("Info", fd.getHeaderInfo());
				setFileInfo(fi); // needed for File->Revert
				if (path.equals("")) show();
				//IJ.log("Opened with legacy fits reader");
			} else
				IJ.error("This does not appear to be a FITS file.");
			IJ.showStatus("");

		}

	}

	public static void makeHeaderCardFilter(String k1, String v1, String k2, String v2, String useAnd) {
		k1 = k1.equals("") ? null : k1.toUpperCase();
		v1 = v1.equals("") ? null : v1.toLowerCase();
		k2 = k2.equals("") ? null : k2.toUpperCase();
		v2 = v2.equals("") ? null : v2.toLowerCase();
		var kv1 = new Pair.OptionalGenericPair<>(k1, v1);
		var kv2 = new Pair.OptionalGenericPair<>(k2, v2);
		filter = new HeaderCardFilter(kv1, kv2, useAnd.equals("AND"));
	}

	public static void resetFilter() {
		filter = null;
	}

	/**
	 * Returns a newline-delimited concatenation of the header lines.
	 */
	private String getHeaderInfo(BasicHDU<?> displayHdu) {
		Header header = displayHdu.getHeader();

		// Get the Header as a String
		final var baos = new ByteArrayOutputStream();
		final var utf8 = StandardCharsets.UTF_8.name();
		try (PrintStream ps = new PrintStream(baos, true, utf8)) {
			header.dumpHeader(ps);
		} catch (Exception ignored) {}

		return baos.toString();
	}

	/**
	 * Generate {@link FileInfo} for the FITS image for use in displaying it.
	 */
	private FileInfo decodeFileInfo(BasicHDU<?> displayHdu) throws FitsException {
		Header header = displayHdu.getHeader();
		FileInfo fi = new FileInfo();
		fi.fileFormat = FileInfo.FITS;
		fi.fileName = fileName.endsWith(".fz") ? fileName.substring(0, fileName.length() - 3) : fileName;
		if (fileName.lastIndexOf('.') > 0 && fileName.lastIndexOf('.') < fileName.length()) {
			if (fileName.startsWith("tess-s")) {
				fileBase = fileName.substring(0, 14);
			} else {
				fileBase = fileName.substring(0, fileName.lastIndexOf('.'));
			}
			fileType = fileName.substring(fileName.lastIndexOf('.')+1, fileName.length());
		} else {
			fileBase = "Slice_";
			fileType = "";
		}
		fi.directory = directory;
		fi.width = wi;
		fi.height = he;
		fi.nImages = de;
		fi.pixelWidth = header.getDoubleValue("CDELT1");
		fi.pixelHeight = header.getDoubleValue("CDELT2");
		fi.pixelDepth = header.getDoubleValue("CDELT3");
		fi.unit = header.getStringValue("CTYPE1");
		int bitsPerPixel = header.getIntValue(BITPIX);
		fi.fileType = fileTypeFromBitsPerPixel(bitsPerPixel);
		fi.offset = (int)header.getOriginalSize(); // downcast because spec is allowing for a lot of headers!
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

	/**
	 * Find first HDU from the provided array with NAXIS > 1, assume it is an image.
	 */
	private int firstImageHDU(BasicHDU<?>[] basicHDUs) {
		for (int i=0; i < basicHDUs.length; i++)
		{
			if (basicHDUs[i].getHeader().getIntValue(NAXIS) > 1)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Determine if the HDU at {@param imageIndex} is a compressed image.
	 */
	private boolean isCompressedFormat(BasicHDU<?>[] basicHDU, int imageIndex) {
		return basicHDU[imageIndex].getHeader().getBooleanValue("ZIMAGE", false);
	}

	/**
	 * Converted a compressed ImageHDU to an ImageHDU.
	 * <p>
	 * Updates {@link FITS_Reader#wi}, {@link FITS_Reader#he}, and {@link FITS_Reader#de}
	 */
	private ImageHDU getCompressedImageData(CompressedImageHDU hdu) throws FitsException {
		wi = hdu.getHeader().getIntValue("ZNAXIS1");
		he = hdu.getHeader().getIntValue("ZNAXIS2");
		de = hdu.getHeader().getIntValue("ZNAXIS3", 1);

		return hdu.asImageHDU();
	}

	private void displayStackedImage() {
		ImageStack stack = imagePlus.getStack();
		for (int i = 1; i <= stack.getSize(); i++) {
			stack.getProcessor(i).flipVertical();
		}
		setStack(fileName, stack);
	}

	/**
	 * Takes single set of FITS data and opens the image.
	 */
	private void displaySingleImage(BasicHDU<?> hdu, Data imgData, BasicHDU<?>[] hdus) throws FitsException {
		ImageProcessor imageProcessor = null;

		generateTimings(hdu);

		if (isTicaImage(hdu)) {
			if (hdu.getHeader().getIntValue("QUAL_BIT") != 0) {
				IJ.error("Skipped TICA image as QUAL_BIT is nonzero.");
				return;
			}
		}

		if (hdu instanceof TableHDU<?> tableHDU) {
			if (isTessCut(tableHDU) || isTessPostageStamp(hdus)) {
				var data = (Object[]) tableHDU.getColumn("FLUX");
				var hdr = convertHeaderForFfi(hdus[2].getHeader(), tableHDU);

				if (de > 1 && FolderOpener.virtualIntended) {
					AIJLogger.log("Cannot open 'table' images as a virtual stack.", false);
				}

				if (filter != null && !filter.matchesFilter(hdr)) return;
				imageProcessor = makeStackFrom3DData(data, tableHDU.getNRows(), makeHeadersTessCut(hdr, tableHDU, hdus));
			}
		} else if (isBasic3DImage(hdus)) {
			imageProcessor = makeStackFromManyHDU(hdus);
		} else if (hdu.getHeader().getIntValue(NAXIS) == 2) {
			if (filter != null && !filter.matchesFilter(hdu.getHeader())) return;
			imageProcessor = twoDimensionalImageData2Processor(imgData.getKernel());
		} else if (hdu.getHeader().getIntValue(NAXIS) == 3) {
			if (FolderOpener.virtualIntended) {
				AIJLogger.log("Cannot open 3D images as a virtual stack.", false);
			}
			imageProcessor = process3DimensionalImage(hdu, imgData);
		}

		if (imageProcessor == null) {
			if (imagePlus == null) return;
			imageProcessor = imagePlus.getProcessor();
			imageProcessor.flipVertical();
			setProcessor(fileName, imageProcessor);
		}
	}

	private boolean isBasic3DImage(BasicHDU<?>[] hdus) {
		return hdus.length > 1 &&
				(Arrays.stream(hdus).allMatch(hdu -> (hdu instanceof ImageHDU) && hdu.getKernel() != null) ||
						(hdus[0].getHeader().getIntValue(NAXIS) == 0 && Arrays.stream(hdus).skip(1)
								.allMatch(hdu -> (hdu instanceof ImageHDU ||
										hdu instanceof CompressedImageHDU) && hdu.getKernel() != null)));
	}

	/**
	 * Determine if a table is a TESS cut.
	 */
	private boolean isTessCut(TableHDU<?> tableHDU) {
		return "astrocut".equals(tableHDU.getHeader().getStringValue("CREATOR"));
	}

	/**
	 * Determine if a table is a TESS 2-minute postage stamp.
	 */
	private boolean isTessPostageStamp(BasicHDU<?>[] hdus) {
		var hdu = hdus[0];
		return ("TESS").equals(hdu.getTelescope()) && hdu.getHeader().containsKey("CREATOR") &&
				hdu.getHeader().getStringValue("CREATOR").contains("TargetPixelExporterPipelineModule");
	}

	/**
	 * Convert base image header for TESScut images to a 3D FITS image.
	 * <p>
	 * Updates {@link FITS_Reader#wi}, {@link FITS_Reader#he}, and {@link FITS_Reader#de}
	 */
	private Header convertHeaderForFfi(Header header, TableHDU<?> tableHDU) {
		header.setNaxes(3);
		header.setNaxis(3, tableHDU.getNRows());
		header.deleteKey("EXTNAME");
		header.deleteKey("INSTRUME");
		header.deleteKey("TELESCOP");
		header.deleteKey("CHECKSUM");
		wi = header.getIntValue(NAXIS1);
		he = header.getIntValue(NAXIS2);
		de = tableHDU.getNRows();

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
	private List<String> makeHeadersTessCut(final Header hdr, final TableHDU<?> tableHDU, BasicHDU<?>[] hdus) {
		List<String> headers = new ArrayList<>(tableHDU.getNRows());

		try {
			var bjds = (Number[]) ArrayBoxingUtil.boxArray(tableHDU.getColumn("TIME"));
			var quality = (Number[]) ArrayBoxingUtil.boxArray(tableHDU.getColumn("QUALITY"));

			// Control for logging data
			var hasErrors = false;

			for (int i = 0; i < tableHDU.getNRows(); i++) {
				hdr.setSimple(true); // Needed for MA to read the header data

				// Delete previously added keys as hdr object is not a copy and is shared for all images
				hdr.deleteKey("BJD_TDB");
				hdr.deleteKey("AIJ_Q");
				hdr.deleteKey("NO_BJD");
				hdr.deleteKey("NAXIS3");
				hdr.deleteKey("OBJECT");

				var bjd0 = 2457000d;
				var bjd1 = 0d;
				bjd1 = bjds[i].doubleValue();
				if (!Double.isNaN(bjd0 + bjd1)) hdr.addValue("BJD_TDB", bjd0 + bjd1, "Calc. BJD_TDB");

				if (isTessCut(tableHDU)) {
					// If the image should be skipped add this card, string check for 'AIJ_Q' to skip image
					// Based on TESS Cut code by John Kielkopf
					if ((!skipTessQualCheck && quality[i].intValue() != 0)) {
						hasErrors = true;
						hdr.addValue("AIJ_Q", quality[i].intValue() != 0, "Skipped due to quality flag");
					} else if (Double.isNaN(bjd1)) {
						hasErrors = true;
						hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
					}
				} else if (isTessPostageStamp(hdus)) {
					hdr.addValue("OBJECT", hdus[0].getHeader().getStringValue("OBJECT"), "Object ID");
					if (quality[i].intValue() == 8) {
						hdr.addValue("AIJ_Q2", quality[i].intValue() != 0, "Null image");
					} else if ((!skipTessQualCheck && quality[i].intValue() != 0)) {
						hasErrors = true;
						hdr.addValue("AIJ_Q", quality[i].intValue() != 0, "Skipped due to quality flag");
					} else if (Double.isNaN(bjd1)) {
						hasErrors = true;
						hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
					}
				}

				// Get the Header as a String
				final var baos = new ByteArrayOutputStream();
				final var utf8 = StandardCharsets.UTF_8.name();
				try (PrintStream ps = new PrintStream(baos, true, utf8)) {
					hdr.dumpHeader(ps);
				} catch (Exception ignored) {}

				headers.add(baos.toString(utf8));
			}
			if (hasErrors) AIJLogger.log("Encountered an issue opening: " + fileName);
		} catch (Exception ignored) {}

		return headers;
	}

	/**
	 * Determine if the image is a TESS FFI.
	 */
	private boolean isTessFfi(BasicHDU<?> hdu) {
		var telescope = hdu.getHeader().findCard("TELESCOP");
		var imageType = hdu.getHeader().findCard("IMAGTYPE");

		if (telescope == null || imageType == null) {
			return false;
		}

		var tVal = Objects.requireNonNullElse(telescope.getValue(), "");
		var iCom = Objects.requireNonNullElse(imageType.getComment(), "");

		return tVal.strip().equals("TESS") && iCom.contains("FFI image type");
	}


	/**
	 * Determine if the image is a TICA image
	 */
	private boolean isTicaImage(BasicHDU<?> hdu) {
		return null != hdu.getHeader().findCard("TICAVER");
	}

	/**
	 * Calculate BJD_TDB for TESS or TICA FFIs as they are missing it.
	 * <p>
	 * Note: Assumes all needed cards are present.
	 */
	private void generateTimings(BasicHDU<?> hdu) throws HeaderCardException {
		var header = hdu.getHeader();

		if (isTessFfi(hdu)) {
			var bjdRefi = header.getIntValue("BJDREFI");
			var bjdReff = header.getDoubleValue("BJDREFF");
			var tStart = header.getDoubleValue("TSTART");
			var telapse = header.getDoubleValue("TELAPSE");

			var bjdTdb = bjdRefi + bjdReff + tStart + telapse/2.0;
			hdu.addValue("BJD_TDB", bjdTdb, "Calc by AIJ as BJDREFI+BJDREFF+TSTART+TELAPSE/2.0");
		}

		if (isTicaImage(hdu)) {
			var tjdZero = header.getDoubleValue("TJD_ZERO");
			var startTjd = header.getDoubleValue("STARTTJD");
			var jdTdb = tjdZero + startTjd;

			var leapSeconds = (new LeapSeconds()).getLeapSeconds(jdTdb);

			// JD_TDB -> JD_UTC, see "Achieving Better Than 1 Minute Accuracy in the Heliocentric and Barycentric
			// Julian Dates" Eastman et al. 2010
			var jdUtc = jdTdb - leapSeconds - 32.184;

			//hdu.addValue("JD_TDB", jdTdb, "Calc by AIJ as TJD_ZERO + STARTTJD");
			//hdu.addValue("JD_UTC", jdUtc, "Calc by AIJ as JD_TDB - leapsecs - 32.184s");

			var dt = SkyAlgorithmsTimeUtil.UTDateFromJD(jdUtc);
			var hmsms = SkyAlgorithmsTimeUtil.ut2Array(dt[3]);
			var dateTime = LocalDateTime.of((int) dt[0], (int) dt[1], (int) dt[2], hmsms[0], hmsms[1],
					hmsms[2]).toInstant(ZoneOffset.ofTotalSeconds(0));
			dateTime = dateTime.plusMillis(hmsms[3]);
			hdu.addValue("DATE-OBS", FitsDate.getFitsDateString(Date.from(dateTime)),
					"[UTC] Start date and time of obs.");

			// Copy exposure time
			hdu.addValue("TELAPSE", header.getIntValue("INT_TIME"), "Integration time (s)");
		}
	}

	/**
	 * Take 3D fits data and open it as an {@link ImageStack}.
	 */
	private ImageProcessor process3DimensionalImage(BasicHDU<?> hdu, Data imgData) throws FitsException {
		// Get the Header as a String
		List<String> headers = new ArrayList<>(hdu.getHeader().getIntValue(NAXISn.n(3).key()));
		final var baos = new ByteArrayOutputStream();
		final var utf8 = StandardCharsets.UTF_8.name();
		try (PrintStream ps = new PrintStream(baos, true, utf8)) {
			hdu.getHeader().dumpHeader(ps);
			headers.add(baos.toString(utf8));
		} catch (Exception ignored) {}

		return makeStackFrom3DData((Object[]) imgData.getKernel(),
				hdu.getHeader().getIntValue(NAXISn.n(3).key()), headers);
	}

	/**
	 * From 3D array of pixel data,create a stack.
	 * @see FITS_Reader#makeStackFrom3DData(Object[], int, List)
	 */
	private ImageProcessor makeStackFrom3DData(final Object[] data, final int imageCount) {
		return makeStackFrom3DData(data, imageCount, null);
	}

	/**
	 * From 3D array of pixel data, create a stack. Uses provided Header to set info for processes such
	 * as MultiAperture.
	 */
	private ImageProcessor makeStackFrom3DData(final Object[] data, final int imageCount, final List<String> headers) {
		ImageProcessor ip = null;
		ImageStack stack = new ImageStack();

		var pm = makeMonitor(imageCount);
		for (int i = 0; i < imageCount; i++) {
			String header = "";
			if (headers != null && headers.size() > 0) {
				var headerIndex = headers.size() == 1 ? 0 : i;
				if (headers.get(headerIndex).contains("AIJ_Q")) { // For TESScut, skip bad images
					AIJLogger.log("     Skipping an image due to quality flag: " + (i+1));
					continue;
				} else if (headers.get(headerIndex).contains("AIJ_Q2")) { // For Postage stamp, skip null images
					continue;
				} else if (headers.get(headerIndex).contains("NO_BJD")) { // For TESScut, skip if no BJD available
					AIJLogger.log("     Skipping an image due to a missing or invalid BJD time: " + (i+1));
					continue;
				}
				header = headers.get(headerIndex) + "\n";
			}
			ip = twoDimensionalImageData2Processor(data[i]);
			stack.addSlice(fileBase + "_" + (imageCount<10000 ? fourDigits.format(i+1) : (i+1))
					+ (fileType.length() > 0 ? "." + fileType : "") + "\n" + header, ip);
			pm.setProgress(i);
		}

		setStack(fileName, stack);

		return ip;
	}

	/**
	 * Create a stack from a fits file that only contains multiple images
	 */
	private ImageProcessor makeStackFromManyHDU(BasicHDU<?>[] hdus) throws FitsException {
		ImageProcessor ip = null;
		ImageStack stack = new ImageStack();

		var pm = makeMonitor(hdus.length);
		BasicHDU<?> hdu;
		for (int i = 0; i < hdus.length; i++) {
			// Handle compressed HDUs
			if (isCompressedFormat(hdus, i)) {
				// A side effect of this call is that wi, he, and de are set
				hdu = getCompressedImageData((CompressedImageHDU) hdus[i]);
			} else {
				hdu = hdus[i];
			}
			// Skip null header
			if (hdu.getHeader().getIntValue(NAXIS) == 0) continue;

			// Get the Header as a String
			var hdr = hdu.getHeader();

			if (filter != null && !filter.matchesFilter(hdr)) continue;

			var header = "";
			hdr.setSimple(true); // Needed for MA
			final var baos = new ByteArrayOutputStream();
			final var utf8 = StandardCharsets.UTF_8.name();
			try (PrintStream ps = new PrintStream(baos, true, utf8)) {
				hdr.dumpHeader(ps);
				header = baos.toString(utf8);
			} catch (Exception ignored) {}

			ip = twoDimensionalImageData2Processor(hdu.getKernel());
			stack.addSlice(fileBase + "_" + (hdus.length<10000 ? fourDigits.format(i+1) : (i+1))
					+ (fileType.length() > 0 ? "." + fileType : "") + "\n" + header, ip);
			pm.setProgress(i);
		}

		setStack(fileName, stack);

		return ip;
	}

	private ProgressMonitor makeMonitor(int size) {
		return new ProgressMonitor(IJ.getInstance(), "Processing HDUs in FITS image.", null, 0, size - 1);
	}

	// The following code excerpted from ij.process.FloatProcessor serves to document the layout
	// of the float[] that is called imgtab in getImageProcessor[]:
	//
	//    for (int y=0; y<height; y++) {
	//        for (int x=0; x<width; x++) {
	//            pixels[i++] = array[x][y];
	//        }
	//    }
	//
	// As one can see, x is in the tighter inner loop. y is in the outer loop.
	// This is a bit backwards to what might be expected. In any case, it tells us
	// that x must be the inner loop when we construct imgtab below.

	// Examine how our TableWrapper lambda is implemented:
	//
	//    TableWrapper wrapper = (x, y) -> data[y][x];
	//
	// Notice that again, the x index is the tighter loop.

	/**
	 * Convert 2D image data into an ImageProcessor, scale image data
	 * <p>
	 * Data is transposed to match {@link ImageProcessor} implementations
	 * (see {@link ImageProcessor#getPixelValue(int, int)})
	 */
	private ImageProcessor twoDimensionalImageData2Processor(final Object imageData) {
		ImageProcessor ip;
		var type = ImageType.getType(imageData);

		var imgtmp = type.makeProcessor(wi, he);
		var imgtab = type.processImageData(imageData, wi, he, bzero, bscale);
		ip = conditionImageProcessor(imgtab, imgtmp);
		this.setProcessor(fileName, ip);
		return ip;
	}

	/**
	 * Set pixel and scaling data of the ImageProcessor, flip the image vertically.
	 */
	private ImageProcessor conditionImageProcessor(Object imgtab, ImageProcessor imgtmp) {
		ImageProcessor ip;
		imgtmp.setPixels(imgtab);
		imgtmp.resetMinAndMax();

		if (he == 1) {
			imgtmp = imgtmp.resize(wi, 100);
		}
		if (wi == 1) {
			imgtmp = imgtmp.resize(100, he);
		}
		ip = imgtmp;
		ip.flipVertical();
		return ip;
	}

	/**
	 * Create an {@link OpenDialog}, and read in the selected FITS file.
	 */
	private PostFitsRead getHDU(String path) throws FitsException, IOException {
		OpenDialog od = new OpenDialog("Open FITS...", path);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName == null) {
			throw new FitsException("Null filename.");
		}
		IJ.showStatus("Opening: " + directory + fileName);
		//IJ.log("Opening: " + directory + fileName);

		FitsFactory.setAllowHeaderRepairs(true);
		fileName = fileName.endsWith(".fz") ? fileName.substring(0, fileName.length() - 3) : fileName;

		var fr = getFitsFile(path);
		if (fr.hasErrored) return null;
		Fits f = null;
		try {
			f = fr.fits;
			var hdus = f.read();
			fr.zipFile.ifPresent(this::closeThing);

			return new PostFitsRead(f, hdus);
		} catch (FitsException e) {
			closeThing(f);
			throw e;
		}
	}

	/**
	 * Opens a FITS file from the path. If it is in a zip file, it will open the zip
	 */
	private FitsRead getFitsFile(String path) {
		Fits f = null;
		if (path.contains(".zip")) {
			var s = path.split("\\.zip");

			ZipFile zip = null;
			try {
				zip = new ZipFile(s[0] + ".zip");
				var m = new ProgressMonitorInputStream(IJ.getInstance(),
						"Reading FITS image", zip.getInputStream(zip.getEntry(s[1].substring(1))));
				f = new Fits(m);
				return new FitsRead(f, Optional.of(zip));
			} catch (IOException | FitsException e) {
				closeThing(f);
				return new FitsRead(true);
			}

		}

		try  {
			var m = new ProgressMonitorInputStream(IJ.getInstance(),
					"Reading FITS image", Files.newInputStream(Path.of(path)));
			f = new Fits(m);
			return new FitsRead(f, Optional.empty());
		} catch (FitsException | IOException e) {
			closeThing(f);
			return new FitsRead(true);
		}
	}

	/**
	 * Used to pass out the zipFile from the opening so that it may be closed.
	 */
	private record FitsRead(Fits fits, Optional<ZipFile> zipFile, boolean hasErrored) {
		public FitsRead(Fits fits, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ZipFile> zipFile) {
			this(fits, zipFile, false);
		}

		public FitsRead(boolean hasErrored) {
			this(null, Optional.empty(), hasErrored);
		}
	}

	private record PostFitsRead(Fits fits, BasicHDU<?>[] hdus) {}

	public record HeaderCardFilter(Pair.OptionalGenericPair<String, String> filter1,
								   Pair.OptionalGenericPair<String, String> filter2, boolean useAnd) {
		public boolean matchesFilter(Header hdr) {
			var match1 = filter1.first().map(hdr::findCard) // Maybe get the card
					.map(v -> (filter1.second().isPresent() && filter1.second().get().equalsIgnoreCase(v.getValue())) ||
							filter1.second().isEmpty());
			var match2 = filter2.first().map(hdr::findCard) // Maybe get the card
					.map(v -> (filter2.second().isPresent() && filter2.second().get().equalsIgnoreCase(v.getValue())) ||
							filter2.second().isEmpty());

			var noFilter = filter1.first().isEmpty() && filter2.first().isEmpty();
			var matchesFilterAnd = match1.orElse(filter1.first().isEmpty()) && match2.orElse(filter2.first().isEmpty());
			var matchesFilterOr = match1.orElse(false) || match2.orElse(false);

			return (useAnd ? matchesFilterAnd : matchesFilterOr) ||
					(noFilter);
		}
	}

	private void closeThing(Closeable closeable) {
		if (closeable == null) return;
		try {
			closeable.close();
		} catch (IOException ignored) {
		}
	}

	// The following code is nice, but it is causing a dependency on skyview.geometry.WCS, so bye-bye.
	//
	//    /**
	//     * Gets the locationAsString attribute of the FITS object
	//     *
	//     * @param x Description of the Parameter
	//     * @param y Description of the Parameter
	//     * @return The locationAsString value
	//     */
	//    public String getLocationAsString(int x, int y) {
	//        String s;
	//        if (wcs != null) {
	//            double[] in = new double[2];
	//            in[0] = (double) (x);
	//            in[1] = getProcessor().getHeight() - y - 1.0;
	//            //in[2]=0.0;
	//            double[] out = wcs.inverse().transform(in);
	//            double[] coord = new double[2];
	//            skyview.geometry.Util.coord(out, coord);
	//            CoordinateFormatter cf = new CoordinateFormatter();
	//            String[] ra = cf.sexagesimal(Math.toDegrees(coord[0]) / 15.0, 8).split(" ");
	//            String[] dec = cf.sexagesimal(Math.toDegrees(coord[1]), 8).split(" ");
	//
	//            s = "x=" + x + ",y=" + y + " (RA=" + ra[0] + "h" + ra[1] + "m" + ra[2] + "s,  DEC="
	//                    + dec[0] + "\u00b0" + " " + dec[1] + "' " + dec[2] + "\"" + ")";
	//
	//        } else {
	//            s = "x=" + x + " y=" + y;
	//        }
	//        if (getStackSize() > 1) {
	//            s += " z=" + (getCurrentSlice() - 1);
	//        }
	//        return s;
	//    }

	// This code also has a dependency on skyview.geometry.WCS. Why did we need to write a temporary FITS
	// file anyway? writeTemporaryFITSFile(displayHdu) the last thing done in run().
	//
	//    private void writeTemporaryFITSFile(BasicHDU hdu) throws FileNotFoundException, FitsException {
	//        File file = new File(IJ.getDirectory("home") + ".tmp.fits");
	//        FileOutputStream fis = new FileOutputStream(file);
	//        DataOutputStream dos = new DataOutputStream(fis);
	//        fits.write(dos);
	//        try {
	//            wcs = new WCS(hdu.getHeader());
	//        } catch (Exception e) {
	//            Logger.getLogger(FITS_Reader.class.getName()).log(Level.SEVERE, null, e);
	//        } finally {
	//            try {
	//                fis.close();
	//            } catch (IOException ex) {
	//                Logger.getLogger(FITS_Reader.class.getName()).log(Level.SEVERE, null, ex);
	//            }
	//        }
	//    }

	public static void flipImages(boolean flip) {
		flipImages = flip;
	}

}

class FitsDecoder {
	private String directory, fileName;
	private DataInputStream f;
	private StringBuffer info = new StringBuffer(512);
	double bscale, bzero;

	public FitsDecoder(String directory, String fileName) {
		this.directory = directory;
		this.fileName = fileName;
	}

	FileInfo getInfo() throws IOException {
		FileInfo fi = new FileInfo();
		fi.fileFormat = FileInfo.FITS;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = 0;
		fi.height = 0;
		fi.offset = 0;

		InputStream is = new FileInputStream(directory + fileName);
		if (fileName.toLowerCase().endsWith(".gz")) is = new GZIPInputStream(is);
		f = new DataInputStream(is);
		String line = getString(80);
		info.append(line+"\n");
		if (!line.startsWith("SIMPLE"))
		{f.close(); return null;}
		int count = 1;
		while ( true ) {
			count++;
			line = getString(80);
			info.append(line+"\n");

			// Cut the key/value pair
			int index = line.indexOf ( "=" );

			// Strip out comments
			int commentIndex = line.indexOf ( "/", index );
			if ( commentIndex < 0 )
				commentIndex = line.length ();

			// Split that values
			String key;
			String value;
			if ( index >= 0 ) {
				key = line.substring ( 0, index ).trim ();
				value = line.substring ( index + 1, commentIndex ).trim ();
			} else {
				key = line.trim ();
				value = "";
			}

			// Time to stop ?
			if (key.equals ("END") ) break;

			// Look for interesting information
			if (key.equals("BITPIX")) {
				int bitsPerPixel = Integer.parseInt ( value );
				if (bitsPerPixel==8)
					fi.fileType = FileInfo.GRAY8;
				else if (bitsPerPixel==16)
					fi.fileType = FileInfo.GRAY16_SIGNED;
				else if (bitsPerPixel==32)
					fi.fileType = FileInfo.GRAY32_INT;
				else if (bitsPerPixel==-32)
					fi.fileType = FileInfo.GRAY32_FLOAT;
				else if (bitsPerPixel==-64)
					fi.fileType = FileInfo.GRAY64_FLOAT;
				else {
					IJ.error("BITPIX must be 8, 16, 32, -32 (float) or -64 (double).");
					f.close();
					return null;
				}
			} else if (key.equals("NAXIS1"))
				fi.width = Integer.parseInt ( value );
			else if (key.equals("NAXIS2"))
				fi.height = Integer.parseInt( value );
			else if (key.equals("NAXIS3")) //for multi-frame fits
				fi.nImages = Integer.parseInt ( value );
			else if (key.equals("BSCALE"))
				bscale = parseDouble ( value );
			else if (key.equals("BZERO"))
				bzero = parseDouble ( value );
			/*else if (key.equals("CDELT1"))
				fi.pixelWidth = parseDouble ( value );
			else if (key.equals("CDELT2"))
				fi.pixelHeight = parseDouble ( value );
			else if (key.equals("CDELT3"))
				fi.pixelDepth = parseDouble ( value );
			else if (key.equals("CTYPE1"))
				fi.unit = value;*/

			if (count>360 && fi.width==0)
			{f.close(); return null;}
		}
		if (fi.pixelWidth==1.0 && fi.pixelDepth==1)
			fi.unit = "pixel";

		f.close();
		fi.offset = 2880+2880*(((count*80)-1)/2880);
		return fi;
	}

	String getString(int length) throws IOException {
		byte[] b = new byte[length];
		f.readFully(b);
		if (IJ.debugMode)
			IJ.log(new String(b));
		return new String(b);
	}

	int getInteger(String s) {
		s = s.substring(10, 30);
		s = s.trim();
		return Integer.parseInt(s);
	}

	double parseDouble(String s) throws NumberFormatException {
		return Double.parseDouble(s);
	}

	String getHeaderInfo() {
		return new String(info);
	}

}
