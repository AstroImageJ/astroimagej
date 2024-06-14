package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.util.*;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import nom.tam.fits.*;
import nom.tam.fits.header.Compression;
import nom.tam.fits.header.extra.AIJExt;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.image.compression.hdu.CompressedTableHDU;
import nom.tam.util.Cursor;
import nom.tam.util.FitsOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import static ij.astro.util.FitsExtensionUtil.CompressionMode.FPACK;
import static ij.astro.util.FitsExtensionUtil.CompressionMode.GZIP;
import static nom.tam.fits.header.Standard.*;

/**
 * This plugin saves a 16 or 32 bit image in FITS format. It is a stripped-down version of the SaveAs_FITS 
 *	plugin from the collection of astronomical image processing plugins by Jennifer West at
 *	http://www.umanitoba.ca/faculties/science/astronomy/jwest/plugins.html.
 *
 * <br>Version 2010-11-23 : corrects 16-bit writing, adds BZERO & BSCALE updates (K.A. Collins, Univ. Louisville).
 * <br>Version 2008-09-07 : preserves non-minimal FITS header if already present (F.V. Hessman, Univ. Goettingen).
 * <br>Version 2008-12-15 : fixed END card recognition bug (F.V. Hessman, Univ. Goettingen).
 * <br>Version 2019-11-03 : various updates  (K.A. Collins, CfA-Harvard and Smithsonian).
 */
@AstroImageJ(reason = "Use nom.tam.fits to write images", modified = true)
public class FITS_Writer implements PlugIn {

    private int numCards = 0;
    private Calibration cal;
    private boolean unsigned16 = false;
    private double bZero = 0.0;
    private double bScale = 1.0;
	public static final ExecutorService savingThread = Executors.newSingleThreadExecutor();
            
	@AstroImageJ(reason = "commented out GET FILE...file deletion iff exists, use nom.tam for export", modified = true)
    public void run(String path) {
		ImagePlus imp = IJ.getImage();

		if (true) { // AIJ uses nom.tam for fits export
			saveImage(imp, path, imp.getCurrentSlice());
			return;
		}

		ImageProcessor ip = imp.getProcessor();
		int numImages = imp.getImageStackSize();
		int bitDepth = imp.getBitDepth();
		if (bitDepth==24) {
			IJ.error("RGB images are not supported");
			return;
		}

		// GET PATH
		if (path == null || path.trim().length() == 0) {
			String title = "image.fits";
			SaveDialog sd = new SaveDialog("Write FITS image",title,".fits");
			path = sd.getDirectory()+sd.getFileName();
		}

		/*// GET FILE
		File f = new File(path);
		String directory = f.getParent()+File.separator;
		String name = f.getName();
		if (f.exists()) f.delete();*/
		int numBytes = 0;
        
        cal = imp.getCalibration();
        unsigned16 = (bitDepth==16 && cal.getFunction()==Calibration.NONE && cal.getCoefficients()==null);

		// GET IMAGE
		if (bitDepth==8) {
            numBytes = 1;
            if (cal.getFunction()!=Calibration.NONE && cal.getCoefficients()!=null) {
                bZero = cal.getCoefficients()[0];
                if (cal.getCoefficients()[1] != 0) bScale = cal.getCoefficients()[1];
            }
        } else if (ip instanceof ShortProcessor) {
			numBytes = 2;
            if (unsigned16) {
                bZero = 32768.0;
                bScale = 1.0;
            } else {
                if (cal.getCoefficients()[1] != 0) bScale = cal.getCoefficients()[1];
                bZero = cal.getCoefficients()[0] + (32768.0*bScale);
            }
        } else if (ip instanceof FloatProcessor) {
			numBytes = 4;  //float processor does not support calibration - data values are shifted and scaled in FITS_Reader
            bZero = 0.0;   //float values are written back out without shifting
            bScale = 1.0;  //and without scaling
        }

		int fillerLength = 2880 - ( (numBytes * imp.getWidth() * imp.getHeight()) % 2880 );

		// WRITE FITS HEADER
		String[] hdr = getHeader(imp);
//		if (hdr == null)
//			createHeader(path, ip, numBytes);
//		else
		clearFile(path);
        createHeader(hdr, path, ip, numBytes);

		// WRITE DATA
		writeData(path, ip);
		char[] endFiller = new char[fillerLength];
		appendFile(endFiller, path);
    }

	public static void saveImage(ImagePlus imp, String path) {
		saveImage(imp, path, ".fits");
	}

	public static void saveImage(ImagePlus imp, String path, String extension) {
		saveImage(imp, path, -1, extension);
	}

	public static void saveImage(ImagePlus imp, String path, int specificSlice) {
		saveImage(imp, path, specificSlice, ".fits");
	}

	public static void saveImage(ImagePlus imp, String path, int specificSlice, String extension) {
		IJ.showStatus("Saving image...");

		// GET PATH
		if (path == null || path.trim().length() == 0) {
			var nm = imp.getTitle();
			if (specificSlice != -1) {
				String filename = imp.getStack().getSliceLabel(specificSlice);
				if (filename == null) {
					filename = imp.getTitle().trim();
				} else {
					int newline = filename.indexOf('\n');
					if (newline != -1) filename = filename.substring(0, newline);
				}
				nm = filename.trim();
			}
			String title = FitsExtensionUtil.fileNameWithoutExt(nm);
			SaveDialog sd = new SaveDialog("Write FITS image",title,extension);
			path = sd.getDirectory()+sd.getFileName();
		}

		// Fix save dialog screwing with the extensions
		path = path.replaceFirst("\\.fits\\.fz\\.gz\\.fz\\.gz", ".fits.fz.gz");

		var compressionModes = FitsExtensionUtil.compressionModes(path);

		var totalSize = 0L;

		try {
			// Setup for incremental writing
			Path outPath = Path.of(path);

			// Saving canceled
			if (outPath.getParent() == null) {
				return;
			}

			Files.createDirectories(outPath.getParent());
			if (!outPath.toFile().exists()) Files.createFile(outPath);

			var progressTrackingOutputStream = new ProgressTrackingOutputStream(new FileOutputStream(outPath.toFile()));
			progressTrackingOutputStream.setTotalSizeInBytes(totalSize);

			FitsOutputStream out;
			if (compressionModes.contains(GZIP)) {
				IJ.showStatus("Compressing and writing file...");
				out = new FitsOutputStream(new GZIPOutputStream(progressTrackingOutputStream));
			} else {
				out = new FitsOutputStream(progressTrackingOutputStream);
			}

			var maxImage = specificSlice == -1 ? imp.getStackSize() : 1;

			if (compressionModes.contains(FPACK)) {
				// Write a primary HDU with the minimum required header
				// The first HDU cannot be compressed when adding multiple HDUs.
				// Some programs cannot handle compressed-first HDUs
				var nullHdu = Fits.makeHDU((Object) null);
				nullHdu.addValue(EXTEND, true);
				nullHdu.write(out);
			}

			IJ.showStatus("Converting data and writing...");
			for (int slice = 1; slice <= maxImage; slice++) {
				if (specificSlice != -1 && slice != specificSlice) slice = specificSlice;
				var stack = imp.getStack();
				var ip = stack.getProcessor(slice);
				var type = ImageType.getType(ip);

				// Get the old header and check if BZERO is needed
				var useBZero = false;
				var oldHeader = getHeader(imp, slice);
				if (oldHeader != null) {
					for (String cardString : oldHeader) {
						var card = HeaderCard.create(cardString);
						if (BZERO.key().equals(card.getKey())) {
							useBZero = true;
							break;
						}
					}
				}

				var data = type.make2DArray(ip, useBZero);//todo handle color images
				var hdu = Fits.makeHDU(data);
				var header = hdu.getHeader();

				// Duplicate header for new image
				if (oldHeader != null) {
					var oh = new Header(oldHeader);
					header.mergeDistinct(oh);

					// ANNOTATE is an invalid card, but we need to support it so force it to merge
					// See the following in nom.tam if this breaks:
					// 	- nom.tam.fits.header.FitsKey.commentStyleKeys
					//  - nom.tam.fits.HeaderCard.isCommentStyleCard
					if (oh.containsKey(AIJExt.ANNOTATE)) {
						header.deleteKey(AIJExt.ANNOTATE); // Remove old value so it isn't duplicated
						header.deleteKey(END); // Writer adds this back for us
                        for (Cursor<String, HeaderCard> it = oh.iterator(); it.hasNext(); ) {
                            HeaderCard card = it.next();
							if (AIJExt.ANNOTATE.key().equals(card.getKey())) {
								header.addLine(card);
							}
                        }
					}
				}

				// Ensure scaling is correct
				if (useBZero && !type.isFloatingPoint() && type != ImageType.BYTE) {
					header.addValue(BZERO, (long)type.getBZero());
					header.addValue(BSCALE, 1);
				} else {
					header.deleteKey(BZERO);
					header.deleteKey(BSCALE);
				}

				totalSize += hdu.getSize();

				if (compressionModes.contains(FPACK)) {
					if (maxImage > 1) IJ.showStatus("FPACKing: " + slice);
					CompressedImageHDU compressedHdu = CompressedImageHDU.fromImageHDU((ImageHDU) hdu);
					FitsCompressionUtil.setCompression(compressedHdu, type.isFloatingPoint());
					compressedHdu.compress();
					hdu = compressedHdu;
				}

				if (maxImage > 1) IJ.showStatus("Writing multiHDU FITS image: " + slice);
				hdu.write(out);

				if (specificSlice == -1) IJ.showProgress(slice / (float)maxImage);
			}

			IJ.showStatus("Finishing FITS export...");

			out.close();
			IJ.showProgress(1);
		} catch (Exception e) {
			e.printStackTrace();
			IJ.error("Failed to write file.");
		}

		IJ.showStatus("");
	}

	public static void saveMPTable(ResultsTable resultsTable, boolean includePrefs, boolean includeApertures, String path, String extension) {
		IJ.showStatus("Saving image...");

		// GET PATH
		if (path == null || path.trim().isEmpty()) {
			var nm = resultsTable.getTitle();
			String title = FitsExtensionUtil.fileNameWithoutExt(nm);
			SaveDialog sd = new SaveDialog("Write FITS image",title,extension);
			path = sd.getDirectory()+sd.getFileName();
		}

		// Fix save dialog screwing with the extensions
		path = path.replaceFirst("\\.fz\\.fits", ".fz.gz");
		path = path.replaceFirst("\\.fits\\.fz\\.gz\\.fz\\.gz", ".fits.fz.gz");

		var compressionModes = FitsExtensionUtil.compressionModes(path);

		var totalSize = 0L;

		try {
			// Setup for incremental writing
			Path outPath = Path.of(path);

			// Saving canceled
			if (outPath.getParent() == null) {
				return;
			}

			Files.createDirectories(outPath.getParent());
			if (!outPath.toFile().exists()) Files.createFile(outPath);

			var progressTrackingOutputStream = new ProgressTrackingOutputStream(new FileOutputStream(outPath.toFile()));
			progressTrackingOutputStream.setTotalSizeInBytes(totalSize);

			FitsOutputStream out;
			if (compressionModes.contains(GZIP)) {
				IJ.showStatus("Compressing and writing file...");
				out = new FitsOutputStream(new GZIPOutputStream(progressTrackingOutputStream));
			} else {
				out = new FitsOutputStream(progressTrackingOutputStream);
			}

			// Write primary header
			var nullHdu = Fits.makeHDU((Object) null);
			nullHdu.addValue(EXTEND, true);
			nullHdu.addValue("AIJ_TBL", true, "AIJ Measurements table and plotcfg");
			nullHdu.write(out);

			IJ.showStatus("Converting data and writing...");

			// Write table
			var processedColumns = 0;

			BinaryTable table;
			BinaryTableHDU hdu;

			// Split into multiple HDUs to deal with column count limit of 999
			while (processedColumns <= resultsTable.getLastColumn()) {
				table = new BinaryTable();

				var maxColumns = 998; // Maximum number of TFORMn cards a FITs header may have
				if (resultsTable.hasRowLabels() && processedColumns == 0) {
					// We cannot use strings directly as funpack does not allow compression for them
					// and cannot handle NOCOMPRESS columns due to being broken
					//todo only byte[] encode Labels when fpacking?
					//table.addStringColumn(resultsTable.getColumnAsStrings("Label"));
					table.addColumn(Arrays.stream(resultsTable.bulkGetColumnAsStrings("Label"))
							.map(s -> s.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new));
					maxColumns--; // Label column takes up space
				}

				var columns = Math.min(maxColumns, resultsTable.getLastColumn() - processedColumns);

				for (int col = 0; col <= columns; col++) {
					table.addColumn(resultsTable.bulkGetColumnAsDoubles(col+processedColumns));
				}
				table.defragment();

				hdu = table.toHDU();

				if (processedColumns != 0) {
					var header = hdu.getHeader();
					header.addValue("AIJ_XTRC", true, "This table has extra columns from the previous HDU");
				}

				if (resultsTable.hasRowLabels() && processedColumns == 0) {
					hdu.setColumnName(0, "Label", null);
				}

				var iStart = resultsTable.hasRowLabels() && processedColumns == 0 ? 1 : 0;
				for (int rc = 0; rc <= columns; rc++) {
					// Heading must be in comment as they commonly contain illegal characters, but this is just a warning so...
					hdu.setColumnName(rc + iStart, resultsTable.getColumnHeading(rc + processedColumns),
							resultsTable.getColumnHeading(rc + processedColumns));
				}

				if (!resultsTable.metadata.isEmpty() && processedColumns == 0) {
					BinaryTableHDU finalHdu = hdu;
					resultsTable.metadata.forEach((key, val) -> {
						finalHdu.getHeader().addValue("AIJ_"+key, val, "AIJ MData, 0-indexed");
					});
				}

				if (compressionModes.contains(FPACK)) {
					var compressedHdu = CompressedTableHDU.fromBinaryTableHDU(hdu, -1);
					compressedHdu.compress();
					hdu = compressedHdu;
				}

				hdu.write(out);

				processedColumns += columns+1;
			}

			// Write plotcfg
			if (includePrefs || includeApertures) {
				table = new BinaryTable();

				if (includePrefs) {
					Properties plotcfg = new Properties();
					Enumeration e = Prefs.ijPrefs.keys();
					while (e.hasMoreElements()) {
						String key = (String) e.nextElement();
						if (key.indexOf(".plot.") == 0) {
							plotcfg.put(key, Prefs.ijPrefs.getProperty(key));
						}
					}

					// Store plotcfg as UTF-8 encoded byte array
					var baos = new ByteArrayOutputStream();
					plotcfg.store(new PrintStream(baos, true, StandardCharsets.UTF_8), null);
					table.addColumn(new byte[][]{baos.toByteArray()});
				}

				if (includeApertures) {
					Properties apertures = new Properties();

					if (ObjectShare.get("multiapertureKeys") instanceof Set<?> keysGeneric) {
						var keys = (Set<String>) keysGeneric;
						for (String key : keys) {
							if (Prefs.ijPrefs.containsKey(key)) {
								apertures.put(key, Prefs.ijPrefs.getProperty(key));
							}
						}
					}

					// Store apertures as UTF-8 encoded byte array
					var baos = new ByteArrayOutputStream();
					apertures.store(new PrintStream(baos, true, StandardCharsets.UTF_8), null);
					table.addColumn(new byte[][]{baos.toByteArray()});
				}

				table.defragment();

				hdu = table.toHDU();
				hdu.getHeader().insertComment("UTF-8 encoded byte arrays in the format of Java properties");
				var c = 0;
				if (includePrefs) {
					hdu.setColumnName(c++, "plotcfg", "AIJ plotcfg, Java properties format");
				}

				if (includeApertures) {
					hdu.setColumnName(c++, "apertures", "AIJ apertures");
				}

				if (compressionModes.contains(FPACK)) {
					// GZIP_1 must be used due to fpack/funpack issues
					var compressedHdu = CompressedTableHDU.fromBinaryTableHDU(hdu, -1, Compression.ZCMPTYPE_GZIP_1, Compression.ZCMPTYPE_GZIP_1);
					compressedHdu.compress();
					hdu = compressedHdu;
				}

				hdu.write(out);
			}

			IJ.showStatus("Finishing FITS export...");

			out.close();
			IJ.showProgress(1);
		} catch (Exception e) {
			e.printStackTrace();
			IJ.error("Failed to write file.");
		}

		IJ.showStatus("");
	}

	private static String normalizeColName(String name) {
		return name.replaceAll(" ", "_").replaceAll("\\.", "").replaceAll("([^\\w\\d_])", "r");
	}

	private static boolean cardsMatch(String k1, String k2) {
		k1 = k1.replaceAll("n", "\\\\d");
		return k2.matches(k1);
	}

//	/**
//	 * Creates a FITS header for an image which doesn't have one already.
//	 */	
//	void createHeader(String path, ImageProcessor ip, int numBytes) {
//
//		String bitperpix = "";
//		if      (numBytes==2) {bitperpix = "                  16";}
//		else if (numBytes==4) {bitperpix = "                 -32";}
//		else if (numBytes==1) {bitperpix = "                   8";}
// 		appendFile(writeCard("SIMPLE", "                   T", "Created by ImageJ FITS_Writer"), path);
// 		appendFile(writeCard("BITPIX", bitperpix, "number of bits per data pixel"), path);
// 		appendFile(writeCard("NAXIS", "                   2", "number of data axes"), path);
// 		appendFile(writeCard("NAXIS1", "                "+ip.getWidth(), "length of data axis 1"), path);
// 		appendFile(writeCard("NAXIS2", "                "+ip.getHeight(), "length of data axis 2"), path);
//        if (bZero != 0 || bScale != 1.0)
//            {
//            appendFile(writeCard("BZERO", ""+bZero, "data range offset"), path);
//            appendFile(writeCard("BSCALE", ""+bScale, "scaling factor"), path);
//            }
//
//        int fillerSize = 2880 - ((numCards*80+3) % 2880);
//		char[] end = new char[3];
//		end[0] = 'E'; end[1] = 'N'; end[2] = 'D';
//		char[] filler = new char[fillerSize];
//		for (int i = 0; i < fillerSize; i++)
//			filler[i] = ' ';
// 		appendFile(end, path);
// 		appendFile(filler, path);
//	}

	/**
	 * Writes one line of a FITS header
	 */ 
	char[] writeCard(String title, String value, String comment) {
		char[] card = new char[80];
		for (int i = 0; i < 80; i++)
			card[i] = ' ';
		s2ch(title, card, 0);
		card[8] = '=';
		s2ch(value, card, 10);
		card[31] = '/';
		card[32] = ' ';
		s2ch(comment, card, 33);
        numCards++;
		return card;
	}
    
	void writeCard(char[] line, String path) {    
        appendFile(line, path);
        numCards++;
    }
    
	/**
	 * Converts a String to a char[]
	 */
	void s2ch (String str, char[] ch, int offset) {
		int j = 0;
		for (int i = offset; i < 80 && i < str.length()+offset; i++)
			ch[i] = str.charAt(j++);
	}
    

	/**
	 * Appends 'line' to the end of the file specified by 'path'.
	 */
	void appendFile(char[] line, String path) {
		try {
			FileWriter output = new FileWriter(path, true);
			output.write(line);
			output.close();
		} catch (IOException e) {
			IJ.showStatus("Error writing file!");
			return;
		}
	}
			
	/**
	 * Appends the data of the current image to the end of the file specified by path.
	 */
	void writeData(String path, ImageProcessor ip) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		if (ip instanceof ByteProcessor) {
			byte[] pixels = (byte[])ip.getPixels();
			try {   
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path,true)));
				for (int i = h - 1; i >= 0; i-- )
                    for (int j = i*w; j < w*(i+1); j++)
                        dos.writeByte(pixels[j]);
				dos.close();
            } catch (IOException e) {
				IJ.showStatus("Error writing file!");
				return;
            }    
        } else if (ip instanceof ShortProcessor) {
			short[] pixels = (short[])ip.getPixels();
			try {   
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path,true)));
                for (int i = h - 1; i >= 0; i-- )
                    for (int j = i*w; j < w*(i+1); j++)
                        dos.writeShort(pixels[j]^0x8000);
				dos.close();
            } catch (IOException e) {
				IJ.showStatus("Error writing file!");
				return;
            }
		} else if (ip instanceof FloatProcessor) {
			float[] pixels = (float[])ip.getPixels();
			try {   
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path,true)));
				for (int i = h - 1; i >= 0; i-- )
                    for (int j = i*w; j < w*(i+1); j++)
					dos.writeFloat(pixels[j]);
				dos.close();
            } catch (IOException e) {
				IJ.showStatus("Error writing file!");
				return;
                }					   
            }
        }

	/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the current slice label in the case of an ImageStack)
	 * and returns it as an array of String objects representing each card.
	 *
	 * Taken from the ImageJ astroj package (www.astro.physik.uni-goettingen.de/~hessman/ImageJ/Astronomy)
	 *
 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public static String[] getHeader(ImagePlus img) {
		return getHeader(img, img.getCurrentSlice());
	}

	/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the current slice label in the case of an ImageStack)
	 * and returns it as an array of String objects representing each card.
	 *
	 * Taken from the ImageJ astroj package (www.astro.physik.uni-goettingen.de/~hessman/ImageJ/Astronomy)
	 *
	 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	@AstroImageJ(reason = "Don't assume headers start with SIMPLE", modified = true)
	public static String[] getHeader (ImagePlus img, int slice) {
		String content = null;

		int depth = img.getStackSize();
		if (depth == 1) {
			Properties props = img.getProperties();
			if (props == null)
				return null;
			content = (String)props.getProperty ("Info");
		}
		else if (depth > 1) {
			ImageStack stack = img.getStack();
			content = stack.getSliceLabel(slice);
            if (content == null) {
                Properties props = img.getProperties();
                if (props == null)
                    return null;
                content = props.getProperty ("Info");
            }
        }
		if (content == null)
			return null;

		// PARSE INTO LINES

		String[] lines = content.split("\n");

		// FIND "SIMPLE" AND "END" KEYWORDS

		int istart = 0;
		for (; istart < lines.length; istart++) {
			if (lines[istart].trim().startsWith("AIJ-HEADER-MARKER")) break;
		}
		if (istart == lines.length) return null;
		istart++;

		int iend = istart+1;
		for (; iend < lines.length; iend++) {
			String s = lines[iend].trim();
			if ( s.equals ("END") || s.startsWith ("END ") ) break;
		}
		if (iend >= lines.length) return null;

		int l = iend-istart+1;
		String header = "";
		for (int i=0; i < l; i++)
			header += lines[istart+i]+"\n";
		return header.split("\n");
	}

	/**
	 * Converts a string into an 80-char array.
	 */
	char[] eighty(String s) {
		char[] c = new char[80];
		int l=s.length();
		for (int i=0; i < l && i < 80; i++)
			c[i]=s.charAt(i);
		if (l < 80) {
			for (; l < 80; l++) c[l]=' ';
		}
		return c;
	}

	/**
	 * Copies the image header contained in the image's Info property.
	 */
	@AstroImageJ(reason = "Add AIJ version as comment in place of 'by ImageJ'", modified = true)
	void createHeader(String[] hdr, String path, ImageProcessor ip, int numBytes) {
		String bitperpix = "";
        int imw=ip.getWidth();
        int imh=ip.getHeight();
        String wbuf = "               ";
        String hbuf = "               ";
        if (imw < 10000)
            wbuf = wbuf + " ";
        if (imw < 1000)
            wbuf = wbuf + " ";
        if (imw < 100)
            wbuf = wbuf + " ";
        if (imw < 10)
            wbuf = wbuf + " ";
        if (imh < 10000)
            hbuf = hbuf + " ";
        if (imh < 1000)
            hbuf = hbuf + " ";
        if (imh < 100)
            hbuf = hbuf + " ";
        if (imh < 10)
            hbuf = hbuf + " ";        
		// THESE KEYWORDS NEED TO BE MADE CONFORMAL WITH THE PRESENT IMAGE
		if      (numBytes==2) {bitperpix = "                  16";}
		else if (numBytes==4) {bitperpix = "                 -32";}
		else if (numBytes==1) {bitperpix = "                   8";}
 		appendFile(writeCard("SIMPLE", "                   T", "Created by AstroImageJ v"+IJ.getAstroVersion()), path);
 		appendFile(writeCard("BITPIX", bitperpix, "number of bits per data pixel"), path);
 		appendFile(writeCard("NAXIS", "                   2", "number of data axes"), path);
		appendFile(writeCard("NAXIS1", wbuf + imw, "length of data axis 1"), path);
 		appendFile(writeCard("NAXIS2", hbuf + imh, "length of data axis 2"), path);
        if (bZero != 0 || bScale != 1.0) {
            appendFile(writeCard("BZERO", ""+bZero, "data range offset"), path);
            appendFile(writeCard("BSCALE", ""+bScale, "scaling factor"), path);
        }

        if (hdr != null) {
            // APPEND THE REST OF THE HEADER IF ONE EXISTS
            char[] card;
            for (int i=0; i < hdr.length; i++) {
                String s = hdr[i];
                card = eighty(s);
                if (!s.startsWith("SIMPLE") &&
                    !s.startsWith("BITPIX") &&
                    !s.startsWith("NAXIS")  &&
                    !s.startsWith("BZERO") &&
                    !s.startsWith("BSCALE") &&
                    !s.startsWith("END")   &&
                    s.trim().length() > 1) {
                        writeCard(card, path);
                    }
                }
            }

        // FINISH OFF THE HEADER
        int fillerSize = 2880 - ((numCards*80+3) % 2880);
        char[] end = new char[3];
        end[0] = 'E'; end[1] = 'N'; end[2] = 'D';
        char[] filler = new char[fillerSize];
        for (int i = 0; i < fillerSize; i++)
            filler[i] = ' ';
        appendFile(end, path);
        appendFile(filler, path);
	}

	/**
	 * Restarts file at 'path' to beginning of file
	 */
	@AstroImageJ(reason = "unknown")
	void clearFile(String path) {
		try {
			FileWriter output = new FileWriter(path, false);
			output.close();
		}
		catch (IOException e) {
			IJ.showStatus("Error writing file!");
			return;
		}
	}



    }
