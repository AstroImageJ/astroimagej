package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.astro.AstroImageJ;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.util.ArrayBoxingUtil;
import nom.tam.fits.*;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static nom.tam.fits.header.Standard.*;


/** Opens and displays FITS images. The FITS format is 
	described at "http://fits.gsfc.nasa.gov/fits_standard.html".
	Add setOption("FlipFitsImages",true) to the
	Edit/Options/Startup dialog to have images flipped vertically.
*/
@AstroImageJ(reason = "Support for compressed FITS files via nom.tam.fits, invert flipImages to fix inverted aperture display",
		modified = true)
public class FITS_Reader extends ImagePlus implements PlugIn {
	private static boolean flipImages;
	// private WCS wcs;
	private ImagePlus imagePlus;
	private String directory;
	private String fileName;
	private int wi;
	private int he;
	private int de;
	private float bzero;
	private float bscale;

	// The image data comes in different types, but in the end, we turn them all into floats.
	// So no matter what type the data is, we wrap it with a lambda that takes two indices and
	// returns a float.
	private interface TableWrapper { float valueAt(int x, int y); }

	/**
	 * Main processing method for the FITS_Reader object
	 *
	 * @param path path of FITS file
	 */
	public void run(String path) {
		// wcs = null;
		imagePlus = null;

		/*
		 * Extract array of HDU from FITS file using nom.tam.fits
		 * This also uses the old style FITS decoder to create a FileInfo.
		 */
		BasicHDU[] hdus;
		try {
			hdus = getHDU(path);
		} catch (FitsException e) {
			IJ.error("Unable to open FITS file " + path + ": " + e.getMessage());
			return;
		}

		/*
		 * For fpacked files the image is in the second HDU. For uncompressed images
		 * it is the first HDU.
		 */
		BasicHDU displayHdu;
		int firstImageIndex = firstImageHDU(hdus);
		if (firstImageIndex<0)
		{
			IJ.error("Failed to find an image HDU");
			return;
		}
		if (true) {//isCompressedFormat(hdus, firstImageIndex)) {  //use nom.tam.fits to open compressed files
			int imageIndex = firstImageIndex;
			try {
				if (isCompressedFormat(hdus, firstImageIndex)) {
					// A side effect of this call is that wi, he, and de are set
					displayHdu = getCompressedImageData((CompressedImageHDU) hdus[imageIndex]);

				} else {
					displayHdu = hdus[imageIndex];
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
					displaySingleImage(displayHdu, imgData);
				} catch (FitsException e) {
					IJ.error("Failed to display single image: " + e.getMessage());
					return;
				}
			} else {
				displayStackedImage();
			}

			setProperty("Info", getHeaderInfo(displayHdu));

			IJ.showStatus("");
//        } else {
//            int imageIndex = firstImageIndex;
//            displayHdu = hdus[imageIndex];
//            try {
//                // wi, he, and de are set
//                fixDimensions(displayHdu, displayHdu.getAxes().length);
//            } catch (FitsException e) {
//                IJ.error("Failed to set image dimensions: " + e.getMessage());
//                return;
//            }
//        }
		}
		else {   //use legacy custom fits reader to open uncompressed files
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
			} catch (IOException e) {}
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
	// Returns a newline-delimited concatenation of the header lines
	private String getHeaderInfo(BasicHDU displayHdu) {
		Header header = displayHdu.getHeader();
		header.setSimple(true);
		StringBuffer info = new StringBuffer();
		Cursor<String, HeaderCard> iter = header.iterator();
		while (iter.hasNext()) {
			info.append(iter.next());
			info.append('\n');
		}
		//IJ.log(info.toString());  //print header
		return info.toString();
	}

	private FileInfo decodeFileInfo(BasicHDU displayHdu) throws FitsException {
		Header header = displayHdu.getHeader();
		FileInfo fi = new FileInfo();
		fi.fileFormat = FileInfo.FITS;
		fi.fileName = fileName.endsWith(".fz") ? fileName.substring(0, fileName.length() - 3) : fileName;
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

	private int fileTypeFromBitsPerPixel(int bitsPerPixel) throws FitsException {
		switch (bitsPerPixel) {
			case 8:
				return FileInfo.GRAY8;
			case 16:
				return FileInfo.GRAY16_SIGNED;
			case 32:
				return FileInfo.GRAY32_INT;
			case -32:
				return FileInfo.GRAY32_FLOAT;
			case -64:
				return FileInfo.GRAY64_FLOAT;
			default:
				throw new FitsException("BITPIX must be 8, 16, 32, -32 or -64, but BITPIX=" + bitsPerPixel);
		}
	}

	private int firstImageHDU(BasicHDU[] basicHDU) {
		for (int i=0; i < basicHDU.length; i++)
		{
			if (basicHDU[i].getHeader().getIntValue(NAXIS) > 1)
			{
				return i;
			}
		}
		return -1;
	}

	private boolean isCompressedFormat(BasicHDU[] basicHDU, int imageIndex) {
		return basicHDU[imageIndex].getHeader().getBooleanValue("ZIMAGE", false);
	}

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

	private void displaySingleImage(BasicHDU hdu, Data imgData)
			throws FitsException {
		ImageProcessor imageProcessor = null;
		int dim = hdu.getAxes().length;  //NAXIS

		if (isTessFfi(hdu)) {
			hdu.addValue("BJD_TDB", generateBjd(hdu), "AIJ-calc. BJD_TDB");
		}

		if (hdu.getHeader().getIntValue(NAXIS) == 2) {
			imageProcessor = process2DimensionalImage(hdu, imgData);
		} else if (hdu.getHeader().getIntValue(NAXIS) == 3) {
			imageProcessor = process3DimensionalImage(hdu, imgData);
		}

		if (imageProcessor == null) {
			imageProcessor = imagePlus.getProcessor();
			imageProcessor.flipVertical();
			setProcessor(fileName, imageProcessor);
		}
	}

	/**
	 * Determine if the image is a TESS FFI
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

	private double generateBjd(BasicHDU<?> hdu) {
		var header = hdu.getHeader();
		var bjdRefi = header.getIntValue("BJDREFI");
		var bjdReff = header.getDoubleValue("BJDREFF");
		var tStart = header.getDoubleValue("TSTART");
		var telapse = header.getDoubleValue("TELAPSE");

		return bjdRefi + bjdReff + tStart + telapse/2.0;
	}

	//todo fix for compressed 3d images - currently only the 1 image is read, the others return all 0
	private ImageProcessor process3DimensionalImage(BasicHDU hdu, Data imgData)
			throws FitsException {
		ImageProcessor ip = null;
		ImageStack stack = new ImageStack();

		for (int i = 0; i < hdu.getHeader().getIntValue(NAXISn.n(3).key()); i++) {
			final Number[][] itab = ((Number[][][]) ArrayBoxingUtil.boxArray(imgData.getKernel()))[i];

			// Same as what process2DimensionalImage, but via (un)boxing to simplify code
			ip = switch (hdu.getBitPix()) {
				case 16, -32, 32, -64 -> { // No 64 case?
					TableWrapper wrapper = (x, y) -> itab[y][x].floatValue();
					yield getImageProcessor(wrapper);
				}
				case 8 -> {
					// Only in the 8 case is the signed-to-unsigned correction done -- oversight?!?
					TableWrapper wrapper = (x, y) -> itab[y][x].floatValue() < 0 ?
							itab[y][x].floatValue() + 256 : itab[y][x].floatValue();
					yield getImageProcessor(wrapper);
				}
				default -> imagePlus.getProcessor();
			};

			stack.addSlice(ip);
		}
		setStack(fileName, stack);

		return ip;
	}

	private ImageProcessor process2DimensionalImage(BasicHDU hdu, Data imgData)
			throws FitsException {
		ImageProcessor ip;
//        ///////////////////////////// 16 BITS ///////////////////////
//        if (hdu.getBitPix() == 16) {
//            short[][] itab = (short[][]) imgData.getKernel();
//            TableWrapper wrapper = (x, y) -> itab[y][x];
//            ip = getImageProcessor(wrapper);
//
//        } // 8 bits
//        else if (hdu.getBitPix() == 8) {
//            // Only in the 8 case is the signed-to-unsigned correction done -- oversight?!?
//            byte[][] itab = (byte[][]) imgData.getKernel();
//            TableWrapper wrapper = (x, y) -> (float)(itab[y][x] < 0 ? itab[y][x] + 256 : itab[y][x]);
//            ip = getImageProcessor(wrapper);
//        } // 16-bits
//        ///////////////// 32 BITS ///////////////////////
//        else if (hdu.getBitPix() == 32) {
//            int[][] itab = (int[][]) imgData.getKernel();
//            TableWrapper wrapper = (x, y) -> (float)itab[y][x];
//            ip = getImageProcessor(wrapper);
//        } // 32 bits
//        /////////////// -32 BITS ?? /////////////////////////////////
//        else if (hdu.getBitPix() == -32) {
//            float[][] itab = (float[][]) imgData.getKernel();
//            TableWrapper wrapper = (x, y) -> (float)itab[y][x];
//            ip = getImageProcessor(wrapper);

		if (hdu.getBitPix() == 16) {
			final short[][] itab = (short[][]) imgData.getKernel();
			TableWrapper wrapper = new TableWrapper() {
				@Override
				public float valueAt(int x, int y) {
					return itab[y][x];
				}
			};
			ip = getImageProcessor(wrapper);

		} // 8 bits
		else if (hdu.getBitPix() == 8) {
			// Only in the 8 case is the signed-to-unsigned correction done -- oversight?!?
			final byte[][] itab = (byte[][]) imgData.getKernel();
			TableWrapper wrapper = new TableWrapper() {
				@Override
				public float valueAt(int x, int y) {
					return (float) (itab[y][x] < 0 ? itab[y][x] + 256 : itab[y][x]);
				}
			};
			ip = getImageProcessor(wrapper);
		} // 16-bits
		///////////////// 32 BITS ///////////////////////
		else if (hdu.getBitPix() == 32) {
			final int[][] itab = (int[][]) imgData.getKernel();
			TableWrapper wrapper = new TableWrapper() {
				@Override
				public float valueAt(int x, int y) {
					return (float) itab[y][x];
				}
			};
			ip = getImageProcessor(wrapper);
		} // 32 bits
		/////////////// -32 BITS ?? /////////////////////////////////
		else if (hdu.getBitPix() == -32) {
			final float[][] itab = (float[][]) imgData.getKernel();
			TableWrapper wrapper = new TableWrapper() {
				@Override
				public float valueAt(int x, int y) {
					return (float) itab[y][x];
				}
			};
			ip = getImageProcessor(wrapper);

			// special spectre optique transit
//            if ((hdu.getHeader().getStringValue("STATUS") != null) && (hdu
//                    .getHeader().getStringValue("STATUS")
//                    .equals("SPECTRUM")) && (
//                    hdu.getHeader().getIntValue(NAXIS) == 2)) {
//                //IJ.log("spectre optique");
//                float[] xValues = new float[wi];
//                float[] yValues = new float[wi];
//                for (int y = 0; y < wi; y++) {
//                    yValues[y] = itab[0][y];
//                    if (yValues[y] < 0) {
//                        yValues[y] = 0;
//                    }
//                }
//                String unitY = "IntensityRS ";
//                String unitX = "WavelengthRS ";
//                float CRPIX1 = getCRPIX1(hdu);
//                float CDELT1 = getCDELT1(hdu);
//                float odiv = 1;
//                float CRVAL1 = getCRVAL1ProcessX(hdu, xValues, CRPIX1, CDELT1);
//                if (CRVAL1 < 0.000001) {
//                    odiv = 1000000;
//                    unitX += "(" + "\u00B5" + "m)";
//                } else {
//                    unitX += "ADU";
//                }
//
//                for (int x = 0; x < wi; x++) {
//                    xValues[x] = xValues[x] * odiv;
//                }
//
//                @SuppressWarnings("deprecation") Plot P = new Plot(
//                        "PlotWinTitle "
//                                + fileName, "X: " + unitX, "Y: " + unitY,
//                        xValues, yValues);
//                P.draw();
//            } //// end of special optique
		} // -32 bits
		/////////////// -64 BITS ?? /////////////////////////////////
		else if (hdu.getBitPix() == -64) {
			final double[][] itab = (double[][]) imgData.getKernel();
			TableWrapper wrapper = new TableWrapper() {
				@Override
				public float valueAt(int x, int y) {
					return (float) itab[y][x];
				}
			};
			ip = getImageProcessor(wrapper);
		}
		else {
			ip = imagePlus.getProcessor();
		}
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
	//    TableWrapper wrapper = (x, y) -> itab[y][x];
	//
	// Notice that again, the x index is the tighter loop.

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
		ip = getImageProcessor2(imgtab, imgtmp);
		this.setProcessor(fileName, ip);
		return ip;
	}

	private ImageProcessor getImageProcessor2(float[] imgtab, FloatProcessor imgtmp) {
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

	private BasicHDU[] getHDU(String path) throws FitsException {
		OpenDialog od = new OpenDialog("Open FITS...", path);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName == null) {
			throw new FitsException("Null filename.");
		}
		IJ.showStatus("Opening: " + directory + fileName);
		//IJ.log("Opening: " + directory + fileName);

		FitsFactory.setAllowHeaderRepairs(true);
		Fits fits = new Fits(directory + fileName);
		fileName = fileName.endsWith(".fz") ? fileName.substring(0, fileName.length() - 3) : fileName;
		return fits.read();
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
