package ij.plugin;

import static nom.tam.fits.header.Standard.BITPIX;
import static nom.tam.fits.header.Standard.EXTNAME;
import static nom.tam.fits.header.Standard.NAXIS;
import static nom.tam.fits.header.Standard.NAXIS1;
import static nom.tam.fits.header.Standard.NAXIS2;
import static nom.tam.fits.header.Standard.NAXISn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.gui.GenericSwingDialog;
import ij.astro.io.FitsReader;
import ij.astro.io.prefs.Property;
import ij.astro.logging.AIJLogger;
import ij.astro.logging.Translation;
import ij.astro.types.Pair;
import ij.astro.util.ImageType;
import ij.astro.util.LeapSeconds;
import ij.astro.util.SkyAlgorithmsTimeUtil;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsDate;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TableHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.image.compression.hdu.CompressedTableHDU;
import nom.tam.util.Cursor;
import nom.tam.util.FitsFile;


/** Opens and displays FITS images. The FITS format is 
 * described at "http://fits.gsfc.nasa.gov/fits_standard.html".
 * Add setOption("FlipFitsImages",false) to the
 * Edit/Options/Startup dialog to have FITS images not
 * flipped vertically.
*/
@AstroImageJ(reason = "Support for compressed FITS files via nom.tam.fits, invert flipImages to fix inverted aperture display",
		modified = true)
@Translation("Fits Reader")
public class FITS_Reader extends ImagePlus implements PlugIn {
	private static boolean flipImages = true;
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
	private DecimalFormat fiveDigits = new DecimalFormat("00000", dfs);

	public static boolean skipTessQualCheck = Prefs.getBoolean(".aij.skipTessQualCheck", false);
	private static final LeapSeconds LEAP_SECONDS = new LeapSeconds();

	// The image data comes in different types, but in the end, we turn them all into floats.
	// So no matter what type the data is, we wrap it with a lambda that takes two indices and
	// returns a float. This uses floats as there is no DoubleProcessor from imagej
	@FunctionalInterface
	private interface TableWrapper { double valueAt(int x, int y); }

	public static HeaderCardFilter filter = null;
	private static final MPTableLoadSettings MP_TABLE_LOAD_SETTINGS = new MPTableLoadSettings();
    public static final ScopedValue<Boolean> HEADER_ONLY = ScopedValue.newInstance();

	/**
	 * Main processing method for the FITS_Reader object
	 *
	 * @param path path of FITS file
	 */
	public void run(String path) {
		// wcs = null;
		imagePlus = null;

		AIJLogger.setLogAutoCloses(Prefs.getBoolean(AIJLogger.CERTAIN_LOGS_AUTO_CLOSE, true));

		if (true) {
			try (var r = FitsReader.create(path)) {
				if (r.size() <= 0) {
					if (r.isMeasurementsTable()) {
						// Read table
						r.getHeaders();
					}

					return;
				}

				setFileInfo(r.decodeFileInfo());
				var hdrs = r.getHeaders();
				setProperty("Info", hdrs[0]);

				fileName = r.fileName;
				fileType = r.fileType;
				fileBase = r.fileBase;

				if (HEADER_ONLY.orElse(false)) {
					// Needed for slice info to update
					//todo find replacement for creating IP
					setProcessor(fileName, new ByteProcessor(r.getWidth(), r.getHeight()));

					IJ.showStatus("");
					return;
				}

				var ips = r.getProcessors();

				if (r.size() == 1) {
					setProcessor(fileName, ips[0]);
					if (imagePlus == null) return;
					var imageProcessor = imagePlus.getProcessor();
					imageProcessor.flipVertical();
					setProcessor(fileName, imageProcessor);
				} else if (r.size() > 1) {
					var stack = new ImageStack();
					for (int i = 0; i < r.size(); i++) {
						stack.addSlice(fileBase + "_" +
								(r.size() < 10000 ? fourDigits.format(i + 1) : (i + 1)) +
								(!fileType.isEmpty() ? "." + fileType : "") + "\n" + hdrs[i], ips[i]);
					}

					setStack(fileName, stack);
				}
				IJ.showStatus("");
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			return;
		}

		//todo implement legacy fallback, remove old nom.tam code

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
			if (firstImageIndex<0) {
				IJ.error("Failed to find an image HDU");
				postFitsRead.close();
				return;
			}

			try {
				if (isCompressedFormat(hdus, firstImageIndex)) {
					// A side effect of this call is that wi, he, and de are set
					displayHdu = getCompressedImageData((CompressedImageHDU) hdus[firstImageIndex]);
				} else {
					displayHdu = hdus[firstImageIndex];
					wi = getHeader(displayHdu, true).getIntValue("NAXIS1");
					he = getHeader(displayHdu, true).getIntValue("NAXIS2");
					de = 1; // Use displaySingleImage
				}
			} catch (FitsException e) {
				IJ.error("AIJ does not recognized this file as a FITS file: " + e.getMessage());
				e.printStackTrace();
				postFitsRead.close();
				return;
			}
			//IJ.log("Opened with nom.tam.fits");
			bzero = displayHdu.getBZero();
			bscale = displayHdu.getBScale();

			// Create the fileInfo.
			try {
				FileInfo fileInfo = decodeFileInfo(displayHdu);
				// ImagePlus has a private member named fileInfo. This inherited method sets it.
				setFileInfo(fileInfo);
			} catch (FitsException e) {
				IJ.error("Failed to decode fileInfo: " + e.getMessage());
				postFitsRead.close();
				return;
			}

			Data imgData = displayHdu.getData();

			if ((wi < 0) || (he < 0)) {
				IJ.error("This does not appear to be a FITS file. " + wi + " " + he);
				postFitsRead.close();
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

			postFitsRead.close();

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
		Header header = getHeader(displayHdu, true);

		// Get the Header as a String
		final var baos = new ByteArrayOutputStream();
		final var utf8 = StandardCharsets.UTF_8.name();
		try (PrintStream ps = new PrintStream(baos, true, utf8)) {
			ps.println("AIJ-HEADER-MARKER");
			header.dumpHeader(ps);
		} catch (Exception ignored) {}

		return baos.toString();
	}

	/**
	 * Generate {@link FileInfo} for the FITS image for use in displaying it.
	 */
	private FileInfo decodeFileInfo(BasicHDU<?> displayHdu) throws FitsException {
		Header header = getHeader(displayHdu, true);
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
		for (int i=0; i < basicHDUs.length; i++) {
			if (getHeader(basicHDUs[i], false).getIntValue(NAXIS) > 1) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Determine if the HDU at {@param imageIndex} is a compressed image.
	 */
	private boolean isCompressedFormat(BasicHDU<?>[] basicHDU, int imageIndex) {
		return getHeader(basicHDU[imageIndex], false).getBooleanValue("ZIMAGE", false);
	}

	/**
	 * Converted a compressed ImageHDU to an ImageHDU.
	 * <p>
	 * Updates {@link FITS_Reader#wi}, {@link FITS_Reader#he}, and {@link FITS_Reader#de}
	 */
	private ImageHDU getCompressedImageData(CompressedImageHDU hdu) throws FitsException {
		wi = getHeader(hdu, false).getIntValue("ZNAXIS1");
		he = getHeader(hdu, false).getIntValue("ZNAXIS2");
		de = getHeader(hdu, false).getIntValue("ZNAXIS3", 1);

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

		if (isTessTicaFfiFull(hdu)) {
			if (getHeader(hdu, true).getIntValue("QUAL_BIT") != 0) {
				IJ.error("Skipped TICA image as QUAL_BIT is nonzero.");
				return;
			}
		}

		if (isHyperSup(hdus[0])) {
			var primaryH = getHeader(hdus[0], true);
			getHeader(hdu, false).deleteKey("MJD-OBS");
			getHeader(hdu, false).findCard("DATE-OBS")
					.setValue(primaryH.getStringValue("DATE-OBS") + "T" + primaryH.getStringValue("UT-STR"));
			getHeader(hdu, false).addLine(primaryH.findCard("EXPTIME"));
		}

		if (hdu instanceof TableHDU<?> tableHDU) {
			if (isTessSpocFfiCut(tableHDU) || isTessSpocPostageStamp(hdus) || isTessTicaFfiCut(tableHDU)) {
				var data = (Object[]) tableHDU.getColumn("FLUX");
				var hdr = convertHeaderForFfi(getHeader(hdus[2], true), tableHDU);

				if (de > 1 && FolderOpener.virtualIntended) {
					AIJLogger.log("Cannot open 'table' images as a virtual stack.", false);
				}

				if (filter != null && !filter.matchesFilter(hdr)) return;
				imageProcessor = makeStackFrom3DData(data, tableHDU.getNRows(), makeHeadersTessCut(hdr, tableHDU, hdus));
			} else {
				var mt = fitsTable2MeasurementsTable(hdus, tableHDU, Set.of());
				if (mt != null) {
					mt.show("Measurements in " + fileName);
				}
				return;
			}
		} else if (isBasic3DImage(hdus)) {
			imageProcessor = makeStackFromManyHDU(hdus);
		} else if (getHeader(hdu, true).getIntValue(NAXIS) == 2) {
			if (filter != null && !filter.matchesFilter(getHeader(hdu, true))) return;
			imageProcessor = twoDimensionalImageData2Processor(imgData.getKernel());
		} else if (getHeader(hdu, true).getIntValue(NAXIS) == 3) {
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
		// For compressed multiHDU files, the first HDU likely has no data as it
		// was added to allow for compression.
		var firstValidHdu = getHeader(hdus[0], true).getIntValue(NAXIS) == 0 ? 1 : 0;

		var isImages = (hdus.length > 1 && !isLco(hdus)) &&
				(Arrays.stream(hdus).skip(firstValidHdu)
						// We only care about images
						.allMatch(hdu -> (hdu instanceof ImageHDU || hdu instanceof CompressedImageHDU) &&
								hdu.getKernel() != null));

		var imagesAreSameSize = Arrays.stream(hdus)
				.skip(getHeader(hdus[0], true).getIntValue(NAXIS) == 0 ? 1 : 0)
				.allMatch(hdu -> {
					try {
						return Arrays.equals(hdus[firstValidHdu].getAxes(), hdu.getAxes());
					} catch (FitsException e) {
						e.printStackTrace();
						return false;
					}
				});

		return isImages && imagesAreSameSize && processBasic3DImage(hdus, hdus[firstValidHdu]);
	}

	private boolean processBasic3DImage(BasicHDU<?>[] hdus, BasicHDU<?> displayHdu) {
		if (isTessSpocFfiFull(displayHdu)) {
			var isCalImage = "cal".equals(displayHdu.getHeader().findCard("IMAGTYPE").getValue().trim());
			if (isCalImage) {
				// Copy primary header to displayHDU
				//todo improve merging
				if (displayHdu.getHeader().getBooleanValue("INHERIT", false)) {
					Cursor<String, HeaderCard> j = hdus[0].getHeader().iterator();

					while (j.hasNext()) {
						HeaderCard card = j.next();

						if (card.isCommentStyleCard()) {
							if (card.getKey().startsWith("COMMENT") ||
									card.getKey().startsWith("HISTORY") ||
									card.getKey().startsWith("END")) {
								continue;
							}
							displayHdu.getHeader().insertCommentStyle(card.getKey(), card.getComment());
						} else {
							if (!displayHdu.getHeader().containsKey(card.getKey()) && !"SIMPLE".equals(card.getKey())) {
								try {
									displayHdu.getHeader().updateLine(card.getKey(), card);
								} catch (HeaderCardException e) {
									throw new RuntimeException(e);
								}
							}
						}
					}
					// Force SIMPLE at the top of the header for header processing such as WCS
					//displayHdu.getHeader().setSimple(true);
				}
			}
			return !isCalImage;
		}

		return true;
	}

	public static TableRead handleTable(Path path, ResultsTable table, Set<Opener.OpenOption> openOptions) {
		try (var fits = new Fits(new FitsFile(path.toFile()))) {
			var hdus = fits.read();
			if (hdus.length > 1) {
				if (hdus[1] instanceof TableHDU<?> tableHDU) {
					return fitsTable2MeasurementsTable(table, hdus, tableHDU, openOptions);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
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

		var tableRead = fitsTable2MeasurementsTable(mt, hdus, tableHDU, openOptions);
		return mt;
	}

	public static TableRead fitsTable2MeasurementsTable(ResultsTable table, BasicHDU<?>[] hdus, TableHDU<?> tableHDU,
														 Set<Opener.OpenOption> openOptions) throws FitsException {
		var loadTable = true;
		var skipDialog = openOptions.contains(Opener.OpenOption.SKIP_UI);
		var onlyTable = openOptions.contains(Opener.OpenOption.SINGLE_FILE);

		// Handle AIJ Fits Tables
		//todo drag onto MP windows does not load
		if (hdus[0].getHeader().getBooleanValue("AIJ_TBL", false)) {

			// Check for what the table has
			var hasPlotCfg = false;
			var hasApertures = false;
			for (BasicHDU<?> hdu : hdus) {
				if (hdu instanceof TableHDU<?> t) {
					if (!hasPlotCfg) {
						hasPlotCfg = t.findColumn("plotcfg") >= 0;
					}

					if (!hasApertures) {
						hasApertures = t.findColumn("apertures") >= 0;
					}

					if (hasPlotCfg && hasApertures) {
						break;
					}
				}
			}

            if (!skipDialog) {
				// Dialog to control what to open
				var d = new GenericSwingDialog("FITs MP Table Reading");
				d.addMessage("Data to load (if available):");
				d.addCheckbox("Table", MP_TABLE_LOAD_SETTINGS.loadData.get(), MP_TABLE_LOAD_SETTINGS.loadData::set);
				d.addCheckbox("Plot Config", MP_TABLE_LOAD_SETTINGS.loadPlotcfg.get(),
								MP_TABLE_LOAD_SETTINGS.loadPlotcfg::set)
						.setEnabled(hasPlotCfg);
				d.addCheckbox("Apertures", MP_TABLE_LOAD_SETTINGS.loadApertures.get(),
								MP_TABLE_LOAD_SETTINGS.loadApertures::set)
						.setEnabled(hasApertures);
				d.centerDialog(true);

				d.showDialog();

				if (d.wasCanceled()) {
					return null;
				}
            }

			int totalCol = 0;
			if (skipDialog || MP_TABLE_LOAD_SETTINGS.loadData.get()) {
				if (tableHDU instanceof CompressedTableHDU compressedTableHDU) {
					tableHDU = compressedTableHDU.asBinaryTableHDU();
				}

				totalCol = tableHDU.getNCols();
				for (int c = 0; c < totalCol; c++) {
					var o = tableHDU.getColumn(c);
					var cName = tableHDU.getColumnName(c) == null ? "C" + c : tableHDU.getColumnName(c);
					if ("Label".equals(cName)) {
						continue;
					}
					setColumn(o, table, cName);
				}

				// Handle labels
				// Handled last so that the rows exist
				var li = tableHDU.findColumn("Label");
				if (li >= 0) {
					var lc = tableHDU.getColumn(li);
					if (lc instanceof String[] labels) {
						//todo bulk set methods
						IntStream.range(0, labels.length).forEachOrdered(i -> table.setLabel(labels[i], i));
					} else if (lc instanceof byte[][] bytes) {
						IntStream.range(0, bytes.length)
								.forEachOrdered(i -> table.setLabel(new String(bytes[i], StandardCharsets.UTF_8), i));
					}
				}

				// Handle metadata
				for (HeaderCard card : tableHDU.getHeader().findCards("AIJ_\\w{1,4}")) {
					table.metadata.put(card.getKey().trim().substring(4), card.getValue());
				}
			} else {
				loadTable = false;
			}

			// Load plotcfg
			byte[] plotcfg = null;
			byte[] apertures = null;
			for (BasicHDU<?> basicHDU : hdus) {
				if (basicHDU == tableHDU) continue;
				if (basicHDU instanceof TableHDU<?> t) {
					if ((skipDialog || MP_TABLE_LOAD_SETTINGS.loadData.get()) && t.getHeader().getBooleanValue("AIJ_XTRC", false)) {
						if (t instanceof CompressedTableHDU compressedTableHDU) {
							t = compressedTableHDU.asBinaryTableHDU();
						}
						for (int c = 0; c < t.getNCols(); c++) {
							var o = t.getColumn(c);
							var cName = t.getColumnName(c) == null ? "C" + totalCol : t.getColumnName(c);
							if ("Label".equals(cName)) {
								continue;
							}
							setColumn(o, table, cName);
							totalCol++;
						}
					}

					if ((skipDialog || MP_TABLE_LOAD_SETTINGS.loadPlotcfg.get()) && !onlyTable) {
						var pltcfgCol = t.findColumn("plotcfg");
                        if (pltcfgCol >= 0) {
							if (t instanceof CompressedTableHDU compressedTableHDU) {
								t = compressedTableHDU.asBinaryTableHDU();
							}
                            if (t.getColumn(pltcfgCol) instanceof byte[] bytes) {
                                plotcfg = bytes;
                            } else if (t.getColumn(pltcfgCol) instanceof byte[][] bytes) {
                                plotcfg = bytes[0];
                            }
                        }
					}
					if ((skipDialog || MP_TABLE_LOAD_SETTINGS.loadApertures.get()) && !onlyTable) {
						var aperturesCol = t.findColumn("apertures");
                        if (aperturesCol >= 0) {
							if (t instanceof CompressedTableHDU compressedTableHDU) {
								t = compressedTableHDU.asBinaryTableHDU();
							}
                            if (t.getColumn(aperturesCol) instanceof byte[] bytes) {
                                apertures = bytes;
                            } else if (t.getColumn(aperturesCol) instanceof byte[][] bytes) {
                                apertures = bytes[0];
                            }
                        }
					}
				}
			}

			if (plotcfg != null && !onlyTable) {
                try {
                    Prefs.ijPrefs.load(new ByteArrayInputStream(plotcfg));
                } catch (IOException e) {
                    e.printStackTrace();
					IJ.error("Failed to read plotcfg");
                }
			}

			if (skipDialog) {
				AIJLogger.setLogAutoCloses(true);

				AIJLogger.log("Loaded table");
				if (!onlyTable) {
					if (hasApertures) {
						AIJLogger.log("Loaded apertures");
					}

					if (hasPlotCfg) {
						AIJLogger.log("Loaded plot config");
					}
				}

				AIJLogger.log("To select components to load, Drag & Drop or use File > Open...");
			}

			if (apertures != null) {
				return new TableRead(loadTable, apertures);
			}
		} else {
			if (tableHDU instanceof CompressedTableHDU compressedTableHDU) {
				tableHDU = compressedTableHDU.asBinaryTableHDU();
			}

			for (int c = 0; c < tableHDU.getNCols(); c++) {
				var o = tableHDU.getColumn(c);
				var cName = tableHDU.getColumnName(c) == null ? "C" + c : tableHDU.getColumnName(c);
				setColumn(o, table, cName);
			}
		}

		return new TableRead(loadTable, null);
	}

	//todo check listed column type, skip image columns, or open them as stacks?
	//todo add other columns, but as NaN?
	private static void setColumn(Object colData, ResultsTable finalMt, String cName) {
		if (colData instanceof byte[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, Byte.toUnsignedInt(arr[i])));
		} else if (colData instanceof short[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, arr[i]));
		} else if (colData instanceof int[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, arr[i]));
		} else if (colData instanceof long[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, arr[i]));
		} else if (colData instanceof float[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, arr[i]));
		} else if (colData instanceof double[] arr) {
			finalMt.bulkSetColumnAsDoubles(cName, arr);
		} else if (colData instanceof String[] arr) {
			IntStream.range(0, arr.length).forEachOrdered(i -> finalMt.setValue(cName, i, arr[i]));
		}
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
		wi = header.getIntValue(NAXIS1);//todo callback for this
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
		hdr.deleteKey("DATE-OBS");
		hdr.deleteKey("DATE-END");

		try {
			//todo not sure this is a big improvement or not
			var bjdColumn = tableHDU.findColumn("TIME");
			var qualityColumn = tableHDU.findColumn("QUALITY");

			if (bjdColumn >= 0) {
				hdr.deleteKey("BJDREFI");
				hdr.deleteKey("BJDREFF");
			}

			hdr.seekTail();
			hdr.prevCard();

			// Control for logging data
			var hasErrors = false;

			for (int i = 0; i < tableHDU.getNRows(); i++) {
				//todo mark, test tessCut tables
				//hdr.setSimple(true); // Needed for MA to read the header data

				// Delete previously added keys as hdr object is not a copy and is shared for all images
				hdr.deleteKey("BJD_TDB");
				hdr.deleteKey("AIJ_Q");
				hdr.deleteKey("NO_BJD");
				hdr.deleteKey("NAXIS3");
				hdr.deleteKey("OBJECT");

				var bjd0 = 2457000d;
				var bjd1 = 0d;
				bjd1 = ((double[])tableHDU.getElement(i, bjdColumn))[0];
				if (!Double.isNaN(bjd0 + bjd1)) hdr.addValue("BJD_TDB", bjd0 + bjd1, "Calc. BJD_TDB");

				var quality = ((int[])tableHDU.getElement(i, qualityColumn))[0];
				if (isTessSpocFfiCut(tableHDU) || isTessTicaFfiFull(tableHDU)) {
					// If the image should be skipped add this card, string check for 'AIJ_Q' to skip image
					// Based on TESS Cut code by John Kielkopf
					if ((!skipTessQualCheck && quality != 0)) {
						hasErrors = true;
						hdr.addValue("AIJ_Q", true, "Skipped due to quality flag");
					} else if (Double.isNaN(bjd1)) {
						hasErrors = true;
						hdr.addValue("NO_BJD", 0, "Skipped due to invalid or missing BJD time");
					}
					if (isTessTicaFfiCut(hdus[0])) {
						hdr.deleteKey("DATE-OBS");
						hdr.deleteKey("DATE-END");
					}
				} else if (isTessSpocPostageStamp(hdus)) {
					hdr.addValue("OBJECT", hdus[0].getHeader().getStringValue("OBJECT"), "Object ID");
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

				// Get the Header as a String
				final var baos = new ByteArrayOutputStream();
				final var utf8 = StandardCharsets.UTF_8.name();
				try (PrintStream ps = new PrintStream(baos, true, utf8)) {
					ps.println("AIJ-HEADER-MARKER");
					hdr.dumpHeader(ps);
				} catch (Exception ignored) {}

				headers.add(baos.toString(utf8));
			}
			if (hasErrors) AIJLogger.log("Opening: " + fileName);
		} catch (Exception ignored) {}

		return headers;
	}

	private boolean isHyperSup(BasicHDU<?> hdu) {
		return "Hyper Suprime-Cam".equals(hdu.getHeader().getStringValue("INSTRUME"));
	}

	/**
	 * Determine if an image is from LCO.
	 * <p>
	 * Most LCO images contain 3 image HDUs, with the science image separated from the others by a table HDU.
	 * In some cases, this table is missing, which would make LCO images be treated as a multiHDU FITS file,
	 * opening an image stack. This behavior is not desired.
	 */
	private boolean isLco(BasicHDU<?>[] hdus) {
		if (hdus.length == 1) return false;
		var x = hdus[1].getHeader().getStringValue("ORIGIN");
		var d = hdus[1].getHeader().getStringValue(EXTNAME);
		return "LCOGT".equals(x == null ? null : x.trim()) && "SCI".equals(d == null ? null : d.trim());
	}

	/**
	 * Determine if a table is a TESS 2-minute postage stamp.
	 */
	private boolean isTessSpocPostageStamp(BasicHDU<?>[] hdus) {
		var hdu = hdus[0];
		return ("TESS").equals(hdu.getTelescope()) && hdu.getHeader().containsKey("CREATOR") &&
				hdu.getHeader().getStringValue("CREATOR").contains("TargetPixelExporterPipelineModule");
	}

	/**
	 * Determine if the image is a TESS FFI.
	 */
	private boolean isTessSpocFfiFull(BasicHDU<?> hdu) {
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
	 * Determine if a table is a TESS cut.
	 */
	private boolean isTessSpocFfiCut(TableHDU<?> tableHDU) {
		return "astrocut".equals(tableHDU.getHeader().getStringValue("CREATOR")) &&
				tableHDU.getHeader().findCard("TICAVER") == null;
	}

	/**
	 * Determine if the image is a TICA image
	 */
	private boolean isTessTicaFfiFull(BasicHDU<?> hdu) {
		return null != hdu.getHeader().findCard("TICAVER") && hdu.getHeader().findCard("CREATOR") == null;
	}

	/**
	 * Determine if the image is a TICA image from astrocut
	 */
	private boolean isTessTicaFfiCut(BasicHDU<?> hdu) {
		return null != hdu.getHeader().findCard("TICAVER") && "astrocut".equals(hdu.getHeader().getStringValue("CREATOR"));
	}

	/**
	 * Calculate BJD_TDB for TESS or TICA FFIs as they are missing it.
	 * <p>
	 * Note: Assumes all needed cards are present.
	 */
	private void generateTimings(BasicHDU<?> hdu) throws HeaderCardException {
		var header = hdu.getHeader();

		if (isTessSpocFfiFull(hdu)) {
			var bjdRefi = header.getIntValue("BJDREFI");
			var bjdReff = header.getDoubleValue("BJDREFF");
			var tStart = header.getDoubleValue("TSTART");
			var telapse = header.getDoubleValue("TELAPSE");

			var bjdTdb = bjdRefi + bjdReff + tStart + telapse/2.0;
			hdu.addValue("BJD_TDB", bjdTdb, "Calc by AIJ as BJDREFI+BJDREFF+TSTART+TELAPSE/2.0");
		}

		if (isTessTicaFfiFull(hdu) && !isTessTicaFfiCut(hdu)) {
			var tjdZero = header.getDoubleValue("TJD_ZERO");
			var startTjd = header.getDoubleValue("STARTTJD");
			var jdTdb = tjdZero + startTjd;

			var leapSeconds = LEAP_SECONDS.getLeapSeconds(jdTdb);

			// JD_TDB -> JD_UTC, see "Achieving Better Than 1 Minute Accuracy in the Heliocentric and Barycentric
			// Julian Dates" Eastman et al. 2010
			var jdUtc = jdTdb - leapSeconds - 32.184;

			//hdu.addValue("JD_TDB", jdTdb, "Calc by AIJ as TJD_ZERO + STARTTJD");
			//hdu.addValue("JD_UTC", jdUtc, "Calc by AIJ as JD_TDB - leapsecs - 32.184s");

			var dt = SkyAlgorithmsTimeUtil.UTDateFromJD(jdUtc);
			var hmsms = SkyAlgorithmsTimeUtil.ut2Array(dt[3]);

			// Check time
			if (!(isInRangeInc(dt[1], 1, 12) && isInRangeInc(dt[2], 1, 31) &&
					isInRangeInc(hmsms[0], 0, 23) && isInRangeInc(hmsms[1], 0, 59) &&
					isInRangeInc(hmsms[2], 0, 59))) {
				AIJLogger.log("Failed to extract timing data.");
				return;
			}

			var dateTime = LocalDateTime.of((int) dt[0], (int) dt[1], (int) dt[2], hmsms[0], hmsms[1],
					hmsms[2]).toInstant(ZoneOffset.ofTotalSeconds(0));
			dateTime = dateTime.plusMillis(hmsms[3]);
			hdu.addValue("DATE-OBS", FitsDate.getFitsDateString(Date.from(dateTime)),
					"[UTC] Start date and time of obs.");

			// Copy exposure time
			hdu.addValue("TELAPSE", header.getIntValue("INT_TIME"), "Integration time (s)");
		}
	}

	private boolean isInRangeInc(double v, double min, double max) {
		return v >= min && v <= max;
	}

	/**
	 * Take 3D fits data and open it as an {@link ImageStack}.
	 */
	private ImageProcessor process3DimensionalImage(BasicHDU<?> hdu, Data imgData) throws FitsException {
		// Get the Header as a String
		List<String> headers = new ArrayList<>(getHeader(hdu, true).getIntValue(NAXISn.n(3).key()));
		final var baos = new ByteArrayOutputStream();
		final var utf8 = StandardCharsets.UTF_8.name();
		try (PrintStream ps = new PrintStream(baos, true, utf8)) {
			ps.println("AIJ-HEADER-MARKER");
			getHeader(hdu, true).dumpHeader(ps);
			headers.add(baos.toString(utf8));
		} catch (Exception ignored) {}

		return makeStackFrom3DData((Object[]) imgData.getKernel(),
				getHeader(hdu, true).getIntValue(NAXISn.n(3).key()), headers);
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
			if (headers != null && !headers.isEmpty()) {
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
			stack.addSlice(fileBase + "_" + (imageCount<10000 ? fourDigits.format(i+1) : (imageCount > 10000) ? fiveDigits.format(i+1) : i+1)
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

		var noExtensionNames =
				Arrays.stream(hdus).allMatch(basicHDU -> Objects.isNull(basicHDU.getHeader().getStringValue(EXTNAME)));
        var noScienceImage =
                Arrays.stream(hdus).noneMatch(basicHDU -> Objects.equals("SCI", basicHDU.getHeader().getStringValue(EXTNAME)));

        if (!noExtensionNames && noScienceImage) {
            AIJLogger.log("Multi-image file must contain at least one HDU with the name of 'SCI'");
        }

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
			if (getHeader(hdu, true).getIntValue(NAXIS) == 0) continue;

			// Get the Header as a String
			var hdr = getHeader(hdu, true);

			if (filter != null && !filter.matchesFilter(hdr)) continue;
			if (!noExtensionNames && !Objects.equals("SCI", hdr.getStringValue(EXTNAME))) {
				continue;
			}

			var header = "";
			final var baos = new ByteArrayOutputStream();
			final var utf8 = StandardCharsets.UTF_8.name();
			try (PrintStream ps = new PrintStream(baos, true, utf8)) {
				ps.println("AIJ-HEADER-MARKER");
				hdr.dumpHeader(ps);
				header = baos.toString(utf8);
			} catch (Exception ignored) {}

			ip = twoDimensionalImageData2Processor(hdu.getKernel());
			stack.addSlice(fileBase + "_" + (hdus.length<10000 ? fourDigits.format(i+1) : (i+1))
					+ (!fileType.isEmpty() ? "." + fileType : "") + "\n" + header, ip);
			pm.setProgress(i);
		}

        if (stack.size() == 0) {
            return null;
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
		var type = ImageType.getType(imageData, bscale, bzero);

		var imgtmp = type.makeProcessor(wi, he);
		var pixels = type.processImageData(imageData, wi, he, bzero, bscale);
		ip = conditionImageProcessor(pixels, imgtmp);
		this.setProcessor(fileName, ip);
		return ip;
	}

	/**
	 * Set pixel and scaling data of the ImageProcessor, flip the image vertically.
	 */
	private ImageProcessor conditionImageProcessor(Object pixels, ImageProcessor imgtmp) {
		ImageProcessor ip;
		imgtmp.setPixels(pixels);
		imgtmp.resetMinAndMax();

		if (he == 1) {
			imgtmp = imgtmp.resize(wi, 100);
		}
		if (wi == 1) {
			imgtmp = imgtmp.resize(100, he);
		}
		ip = imgtmp;
		//ip.flipVertical();
		return ip;
	}

	private Header getHeader(BasicHDU<?> hdu, boolean decompress) {
		if (decompress && hdu instanceof CompressedImageHDU compressedImageHDU) {
			return compressedImageHDU.getImageHeader();
		}
		return hdu.getHeader();
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

		var fr = getFitsFile(path);
		if (fr.hasErrored) return null;
		Fits f = null;
		try {
			f = fr.fits;
			var hdus = f.read();
			fr.close();

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

	public record TableRead(boolean loadTable, byte[] apertures) {}

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

		public void close() {
			closeThing(fits);
			zipFile.ifPresent(FITS_Reader::closeThing);
		}
	}

	private record PostFitsRead(Fits fits, BasicHDU<?>[] hdus) {
		public void close() {
			closeThing(fits);
		}
	}

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

	protected static void closeThing(Closeable closeable) {
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

	private static class MPTableLoadSettings {
		Property<Boolean> loadData = new Property<>(true, this);
		Property<Boolean> loadApertures = new Property<>(true, this);
		Property<Boolean> loadPlotcfg = new Property<>(true, this);
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
		if (!line.startsWith("SIMPLE")) {f.close(); return null;}
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
			else if (key.equals("CDELT1"))
				fi.pixelWidth = parseDouble ( value );
			else if (key.equals("CDELT2"))
				fi.pixelHeight = parseDouble ( value );
			else if (key.equals("CDELT3"))
				fi.pixelDepth = parseDouble ( value );
			else if (key.equals("CTYPE1"))
				fi.unit = value;

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
