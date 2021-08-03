package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.logging.AIJLogger;
import ij.astro.logging.Translation;
import ij.astro.util.ArrayBoxingUtil;
import ij.astro.util.LeapSeconds;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nom.tam.fits.*;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
	private float bzero;
	private float bscale;

	private Locale locale = Locale.US;
	private DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
	private DecimalFormat fourDigits = new DecimalFormat("0000", dfs);

	public static boolean skipTessQualCheck = Prefs.getBoolean(".aij.skipTessQualCheck", false);

	// The image data comes in different types, but in the end, we turn them all into floats.
	// So no matter what type the data is, we wrap it with a lambda that takes two indices and
	// returns a float. This uses floats as there is no DoubleProcessor from imagej
	@FunctionalInterface
	private interface TableWrapper { float valueAt(int x, int y); }

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
		try {
			hdus = getHDU(path);
		} catch (FitsException | IOException e) {
			IJ.error("Unable to open FITS file " + path + ": " + e.getMessage());
			return;
		}

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
		if (true) {//isCompressedFormat(hdus, firstImageIndex)) {  //use nom.tam.fits to open compressed files
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

			IJ.showStatus("");
		} else {   //use legacy custom fits reader to open uncompressed files
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

	/**
	 * Returns a newline-delimited concatenation of the header lines.
	 */
	private String getHeaderInfo(BasicHDU<?> displayHdu) {
		Header header = displayHdu.getHeader();
		header.setSimple(true);
		StringBuilder info = new StringBuilder();
		Cursor<String, HeaderCard> iter = header.iterator();
		while (iter.hasNext()) {
			info.append(iter.next());
			info.append('\n');
		}
		//IJ.log(info.toString());  //print header
		return info.toString();
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

				imageProcessor = makeStackFrom3DData(data, tableHDU.getNRows(), makeHeadersTessCut(hdr, tableHDU, hdus));
			}
		} else if (hdu.getHeader().getIntValue(NAXIS) == 2) {
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
					hdr.addValue("OBJECT", hdus[2].getHeader().getStringValue("OBJECT"), "Object ID");
					if (Double.isNaN(bjd1)) {
						hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
						hasErrors = true;
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
			var midTjd = header.getDoubleValue("MIDTJD");
			var jdTdb = tjdZero + midTjd;

			var leapSeconds = (new LeapSeconds()).getLeapSeconds(jdTdb);

			// JD_TDB -> JD_UTC, see "Achieving Better Than 1 Minute Accuracy in the Heliocentric and Barycentric
			// Julian Dates" Eastman et al. 2010
			var jdUtc = jdTdb - leapSeconds - 32.184;

			//FitsDate.getFitsDateString() todo convert to FITS date format and add as MID-OBS
			hdu.addValue("JD_UTC", jdUtc, "Calc by AIJ as JD_TDB - leapsecs - 32.184s");
		}
	}

	/**
	 * Take 3D fits data and open it as an {@link ImageStack}.
	 */
	private ImageProcessor process3DimensionalImage(BasicHDU<?> hdu, Data imgData) throws FitsException {
		return makeStackFrom3DData((Object[]) imgData.getKernel(),
				hdu.getHeader().getIntValue(NAXISn.n(3).key()));
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

		for (int i = 0; i < imageCount; i++) {
			String header = "";
			if (headers != null) {
				if (headers.get(i).contains("AIJ_Q")) { // For TESScut, skip bad images
					AIJLogger.log("     Skipping an image due to quality flag: " + (i+1));
					continue;
				}
				else if (headers.get(i).contains("NO_BJD")) { // For TESScut, skip if no BJD available
					AIJLogger.log("     Skipping an image due to a missing or invalid BJD time: " + (i+1));
					continue;
				}
				header = headers.get(i) + "\n";
			}
			ip = twoDimensionalImageData2Processor(data[i]);
			stack.addSlice(fileBase + "_" + (imageCount<10000 ? fourDigits.format(i+1) : (i+1))
					+ (fileType.length() > 0 ? "." + fileType : "") + "\n" + header, ip);
		}

		setStack(fileName, stack);

		return ip;
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
	 * Convert 2D image data into an ImageProcessor, all image data is converted to floats for display.
	 * <p>
	 * Data is transposed to match {@link FloatProcessor}
	 */
	private ImageProcessor twoDimensionalImageData2Processor(final Object imageData) {
		TableWrapper wrapper;
		if (imageData instanceof float[][] data) {
			wrapper = (x, y) -> data[y][x];
		} else if (imageData instanceof double[][] data) {
			wrapper = (x, y) -> (float) data[y][x];
		} else if (imageData instanceof short[][] data) {
			wrapper = (x, y) -> data[y][x];
		} else if (imageData instanceof int[][] data) {
			wrapper = (x, y) -> data[y][x];
		} else if (imageData instanceof long[][] data) {
			wrapper = (x, y) -> data[y][x];
		} else if (imageData instanceof byte[][] data) {
			// The signed-to-unsigned correction is done at this level, as FITS uses unsigned bytes
			// but Java's are signed. Other conversions are handled via BZERO and BSCALE,
			// as defined by the FITS Specification.
			wrapper = (x, y) -> Byte.toUnsignedInt(data[y][x]);
		} else {
			throw new IllegalStateException("Tried to open image data that was not a numeric. " + imageData.getClass());
		}

		return getImageProcessor(wrapper);
	}

	/**
	 * Sets the current ImageProcessor to a new FloatProcessor.
	 * <p>
	 * Take fits data as floats, scale and shift it by bscale and bzero.
	 */
	private ImageProcessor getImageProcessor(TableWrapper wrapper) {
		ImageProcessor ip;
		int idx = 0;
		float[] imgtab;
		FloatProcessor imgtmp;
		imgtmp = new FloatProcessor(wi, he);
		imgtab = new float[wi * he];
		for (int y = 0; y < he; y++) {
			for (int x = 0; x < wi; x++) {
				imgtab[idx] = bzero + bscale * wrapper.valueAt(x, y);
				idx++;
			}
		}
		ip = conditionFloatProcessor(imgtab, imgtmp);
		this.setProcessor(fileName, ip);
		return ip;
	}

	/**
	 * Set pixel and scaling data of the ImageProcessor, flip the image vertically.
	 */
	private ImageProcessor conditionFloatProcessor(float[] imgtab, FloatProcessor imgtmp) {
		ImageProcessor ip;
		imgtmp.setPixels(imgtab);
		imgtmp.resetMinAndMax();

		if (he == 1) {
			imgtmp = (FloatProcessor) imgtmp.resize(wi, 100);
		}
		if (wi == 1) {
			imgtmp = (FloatProcessor) imgtmp.resize(100, he);
		}
		ip = imgtmp;
		ip.flipVertical();
		return ip;
	}

	/**
	 * Create an {@link OpenDialog}, and read in the selected FITS file.
	 */
	private BasicHDU<?>[] getHDU(String path) throws FitsException, IOException {
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
		var hdus = fr.fits.read();

		fr.zipFile.ifPresent(z -> {
			try {
				z.close(); // Close the zip file to prevent leaking memory, needs to be done after fits file is read
			} catch (IOException ignored) {}
		});

		return hdus;
	}

	/**
	 * Opens a FITS file from the path. If it is in a zip file, it will open the zip
	 */
	private FitsRead getFitsFile(String path) throws IOException, FitsException {
		if (path.contains(".zip")) {
			var s = path.split("\\.zip");
			var zip = new ZipFile(s[0] + ".zip");

			return new FitsRead(new Fits(zip.getInputStream(zip.getEntry(s[1].substring(1)))), Optional.of(zip));
		}
		return new FitsRead(new Fits(path), Optional.empty());
	}

	/**
	 * Used to pass out the zipFile from the opening so that it may be closed.
	 */
	private record FitsRead(Fits fits, Optional<ZipFile> zipFile) {}

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
