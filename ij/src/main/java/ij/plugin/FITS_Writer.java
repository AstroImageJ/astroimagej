package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.astro.AstroImageJ;
import ij.astro.util.ImageType;
import ij.astro.util.ProgressTrackingOutputStream;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.util.FitsOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

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
		saveImage(imp, path, -1);
	}

	public static void saveImage(ImagePlus imp, String path, int specificSlice) {
		IJ.showStatus("Saving image...");
		// GET PATH
		if (path == null || path.trim().length() == 0) {
			String title = "image.fits";
			SaveDialog sd = new SaveDialog("Write FITS image",title,".fits");
			path = sd.getDirectory()+sd.getFileName();
		}

		var doFz = path.endsWith(".fz");
		var doGz = path.endsWith(".gz");

		var totalSize = 0L;

		try {
			// Setup for incremental writing
			Path outPath = Path.of(path);
			Files.createDirectories(outPath.getParent());
			if (!outPath.toFile().exists()) Files.createFile(outPath);

			var progressTrackingOutputStream = new ProgressTrackingOutputStream(new FileOutputStream(outPath.toFile()));
			progressTrackingOutputStream.setTotalSizeInBytes(totalSize);

			FitsOutputStream out;
			if (doGz) {
				IJ.showStatus("Compressing and writing file...");
				out = new FitsOutputStream(new GZIPOutputStream(progressTrackingOutputStream));
			} else {
				out = new FitsOutputStream(progressTrackingOutputStream);
			}

			var maxImage = specificSlice == -1 ? imp.getStackSize() : 1;
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
					for (String cardString : oldHeader) {
						if (cardString.startsWith("NAXIS")) continue;
						var card = HeaderCard.create(cardString);
						if (header.containsKey(card.getKey()) && // No overwriting of old header values
								Arrays.stream(values()).anyMatch(s -> card.getKey().equals(s.key())) && // Use auto-genned HDU header info
								!card.isCommentStyleCard()) { // Allow duplicate comment-style cards
							continue;
						}
						header.addLine(card);
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
				hdu.write(out);

				if (specificSlice == -1) IJ.showProgress(slice / (float)maxImage);
			}

			out.close();
			IJ.showProgress(1);
		} catch (IOException | FitsException e) {
			e.printStackTrace();
			IJ.error("Failed to write file.");
		}

		IJ.showStatus("");
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
			if (lines[istart].startsWith("SIMPLE") ) break;
		}
		if (istart == lines.length) return null;

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
