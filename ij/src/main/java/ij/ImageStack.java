package ij;

import ij.process.*;

import java.awt.*;
import java.awt.image.ColorModel;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;

/**
This class represents an expandable array of images.
@see ImagePlus
*/
public class ImageStack {
	static final int INITIAL_SIZE = 25;
	static final String outOfRange = "Stack argument out of range: ";
	private int bitDepth = 0; //0=unknown
	private int nSlices = 0;
	private NativeArray[] stack;
	private String[] label;
	private int width, height;
	private Rectangle roi;
	private ColorModel cm;
	private double min=Double.MAX_VALUE;
	private double max;
	protected float[] cTable;
	private int viewers;
	private boolean signedInt;
	private final Arena arena = Arena.ofShared();
	private static final Cleaner cleaner = Cleaner.create();
	
	/** Default constructor. */
	public ImageStack() { }

	/** Creates a new, empty image stack. */
	public ImageStack(int width, int height) {
		this(width, height, null);
	}
	
	/** Creates a new, empty image stack with a capacity of 'size'.  All
	     'size' slices and labels of this image stack are initially null. */
	public ImageStack(int width, int height, int size) {
		this.width = width;
		this.height = height;
		stack = new NativeArray[size];
		label = new String[size];
		nSlices = size;
	}

	/** Creates a new, empty image stack using the specified color model. */
	public ImageStack(int width, int height, ColorModel cm) {
		this.width = width;
		this.height = height;
		this.cm = cm;
		stack = new NativeArray[INITIAL_SIZE];
		label = new String[INITIAL_SIZE];
		nSlices = 0;
	}

	/** Adds an image in the form of a pixel array to the end of the stack. */
	public void addSlice(String sliceLabel, Object pixels) {
		if (pixels==null) 
			throw new IllegalArgumentException("'pixels' is null!");
		if (!pixels.getClass().isArray()) 
			throw new IllegalArgumentException("'pixels' is not an array");
		int size = stack.length;
		nSlices++;
		if (nSlices>=size) {
			NativeArray[] tmp1 = new NativeArray[size*2];
			System.arraycopy(stack, 0, tmp1, 0, size);
			stack = tmp1;
			String[] tmp2 = new String[size*2];
			System.arraycopy(label, 0, tmp2, 0, size);
			label = tmp2;
		}
		stack[nSlices-1] = NativeArray.create(pixels);
		this.label[nSlices-1] = sliceLabel;
		if (this.bitDepth==0)
			setBitDepth(pixels);
	}
	
	private void setBitDepth(Object pixels) {
		if (pixels==null)
			return;
		if (pixels instanceof byte[])
			this.bitDepth = 8;
		else if (pixels instanceof short[])
			this.bitDepth = 16;
		else if (pixels instanceof float[])
			this.bitDepth = 32;
		else if (pixels instanceof int[])
			this.bitDepth = 24;
	}
	
	/**
	* @deprecated
	* Short images are always unsigned.
	*/
	public void addUnsignedShortSlice(String sliceLabel, Object pixels) {
		addSlice(sliceLabel, pixels);
	}
	
	/** Adds the image in 'ip' to the end of the stack. */
	public void addSlice(ImageProcessor ip) {
		addSlice(null, ip);
	}

	/** Adds the image in 'ip' to the end of the stack, setting
		the string 'sliceLabel' as the slice metadata. */
	public void addSlice(String sliceLabel, ImageProcessor ip) {
		ip = convertType(ip);
		if (ip.getWidth()!=this.width || ip.getHeight()!=this.height) {
			if (this.width==0 && this.height==0)
				init(ip.getWidth(), ip.getHeight());
			else {
				ImageProcessor ip2 = ip.createProcessor(this.width,this.height);
				ip2.insert(ip, 0, 0);
				ip = ip2;
			}
		}
		if (nSlices==0) {
			cm = ip.getColorModel();
			min = ip.getMin();
			max = ip.getMax();
		}
		addSlice(sliceLabel, ip.getPixels());
	}
	
	private void init(int width, int height) {	
		this.width = width;
		this.height = height;
		stack = new NativeArray[INITIAL_SIZE];
		label = new String[INITIAL_SIZE];
	}
	
	private ImageProcessor convertType(ImageProcessor ip) {
		int newBitDepth = ip.getBitDepth();
		if (this.bitDepth==0)
			this.bitDepth = newBitDepth;
		if (this.bitDepth!=newBitDepth) {
			switch (this.bitDepth) {
				case 8: ip=ip.convertToByte(true); break;
				case 16: ip=ip.convertToShort(true); break;
				case 24:  ip=ip.convertToRGB(); break;
				case 32: ip=ip.convertToFloat(); break;
			}
		}
		return ip;
	}
	
	/** Adds the image in 'ip' to the stack following slice 'n'. Adds
		the slice to the beginning of the stack if 'n' is zero. */
	public void addSlice(String sliceLabel, ImageProcessor ip, int n) {
		if (n<0 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		addSlice(sliceLabel, ip);
		var tempSlice = stack[nSlices-1];
		String tempLabel = label[nSlices-1];
		int first = n>0?n:1;
		for (int i=nSlices-1; i>=first; i--) {
			stack[i] = stack[i-1];
			label[i] = label[i-1];
		}
		stack[n] = tempSlice;
		label[n] = tempLabel;
	}
	
	/** Deletes the specified slice, where {@literal 1<=n<=nslices}. */
	public void deleteSlice(int n) {
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		if (nSlices<1)
			return;
		for (int i=n; i<nSlices; i++) {
			stack[i-1] = stack[i];
			label[i-1] = label[i];
		}
		stack[nSlices-1] = null;
		label[nSlices-1] = null;
		nSlices--;
	}
	
	/** Deletes the last slice in the stack. */
	public void deleteLastSlice() {
		if (nSlices>0)
			deleteSlice(nSlices);
	}
	
    public int getWidth() {
    	return width;
    }

    public int getHeight() {
    	return height;
    }
    
	public void setRoi(Rectangle roi) {
		this.roi = roi;
	}
	
	public Rectangle getRoi() {
		if (roi==null)
			return new Rectangle(0, 0, width, height);
		else
			return(roi);
	}
	
	/** Updates this stack so its attributes, such as min, max,
		calibration table and color model, are the same as 'ip'. */
	public void update(ImageProcessor ip) {
		if (ip!=null) {
			min = ip.getMin();
			max = ip.getMax();
			cTable = ip.getCalibrationTable();
			cm = ip.getColorModel();
		}
	}
	
	/** Returns the pixel array for the specified slice, where {@literal 1<=n<=nslices}. */
	public Object getPixels(int n) {
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		return stack[n-1].toArray();
	}
	
	/** Assigns a pixel array to the specified slice, where {@literal 1<=n<=nslices}. */
	public void setPixels(Object pixels, int n) {
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
        if (stack[n-1] != null) {
			var na = stack[n-1];
			if (pixels instanceof NativeArray nativeArray) {
				stack[n-1] = nativeArray;
			} else if (!na.setAll(pixels)) {
				stack[n-1] = NativeArray.create(pixels);
				//todo manually clean na here
			}
        } else {
			if (pixels instanceof NativeArray nativeArray) {
				stack[n-1] = nativeArray;
			} else {
				stack[n-1] = NativeArray.create(pixels);
			}
		}
		if (this.bitDepth==0)
			setBitDepth(pixels);
	}
	
	/** Returns the stack as an array of 1D pixel arrays. Note
		that the size of the returned array may be greater than
		the number of slices currently in the stack, with
		unused elements set to null. */
	public Object[] getImageArray() {
		//todo how to handle - getFileInfo seems like only serious usages in getFileInfo
		//todo virtualstack returns null here
		return stack;
	}
	
	/** Returns the number of slices in this stack. */
	public int size() {
		return getSize();
	}

	public int getSize() {
		return nSlices;
	}

	/** Returns the slice labels as an array of Strings. Note
		that the size of the returned array may be greater than
		the number of slices currently in the stack. Returns null
		if the stack is empty or the label of the first slice is null.  */
	public String[] getSliceLabels() {
		if (nSlices==0)
			return null;
		else
			return label;
	}
	
	/** Returns the label of the specified slice, where {@literal 1<=n<=nslices}.
		Returns null if the slice does not have a label or 'n';
		is out of range. For DICOM and FITS stacks, labels may
		contain header information.
	*/
	public String getSliceLabel(int n) {
		if (n<1 || n>nSlices)
			return null;
		else
			return label[n-1];
	}
	
	/** Returns a shortened version (up to the first 60 characters
	 * or first newline), with the extension removed, of the specified
	 * slice label, or null if the slice does not have a label.
	*/
	public String getShortSliceLabel(int n) {
		return getShortSliceLabel(n, 60);
	}

	/** Returns a shortened version (up to the first 'max' characters
	 * or first newline), with the extension removed, of the specified
	 * slice label, or null if the slice does not have a label.
	*/
	public String getShortSliceLabel(int n, int max) {
		String shortLabel = getSliceLabel(n);
		if (shortLabel==null) return null;
    	int newline = shortLabel.indexOf('\n');
    	if (newline==0) return null;
    	if (newline>0)
    		shortLabel = shortLabel.substring(0, newline);
    	int len = shortLabel.length();
		if (len>4 && shortLabel.charAt(len-4)=='.' && !Character.isDigit(shortLabel.charAt(len-1)))
			shortLabel = shortLabel.substring(0,len-4);
		if (shortLabel.length()>max)
			shortLabel = shortLabel.substring(0, max);
		return shortLabel;
	}
	
	/** Sets the label of the specified slice, where {@literal 1<=n<=nslices}. */
	public void setSliceLabel(String label, int n) {
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		this.label[n-1] = label;
	}
	
	/** Returns an ImageProcessor for the specified slice,
	 * where {@literal 1<=n<=nslices}.
 	 * Returns null if the stack is empty.
 	*/
 	public ImageProcessor getProcessor(int n) {
		ImageProcessor ip;
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		if (nSlices==0)
			return null;
		if (stack[n-1]==null)
			throw new IllegalArgumentException("Pixel array is null");
		var pixels = stack[n - 1];//.toArray();//todo IP works with NativeArray directly, rather than subclassing IP make it work directly with the offheap data
		if (pixels.layout == ValueLayout.JAVA_BYTE)
			ip = new ByteProcessor(width, height, null, cm);
		else if (pixels.layout == ValueLayout.JAVA_SHORT)
			ip = new ShortProcessor(width, height, null, cm);
		else if (pixels.layout == ValueLayout.JAVA_INT) {
			if (signedInt)
				ip = new IntProcessor(width, height);
			else
				ip = new ColorProcessor(width, height, null);
		} else if (pixels.layout == ValueLayout.JAVA_FLOAT)
			ip = new FloatProcessor(width, height, cm);
		else
			throw new IllegalArgumentException("Unknown stack type");
		ip.setPixels(pixels);
		if (min!=Double.MAX_VALUE && ip!=null && !(ip instanceof ColorProcessor))
			ip.setMinAndMax(min, max);
		if (cTable!=null)
			ip.setCalibrationTable(cTable);
		ip.setSliceNumber(n);
		return ip;
	}
	
	/** Assigns the pixel array of an ImageProcessor to the specified slice,
	 * where {@literal 1<=n<=nslices}.
	*/
	public void setProcessor(ImageProcessor ip, int n) {
		if (n<1 || n>nSlices)
			throw new IllegalArgumentException(outOfRange+n);
		ip = convertType(ip);
		if (ip.getWidth()!=width || ip.getHeight()!=height)
			throw new IllegalArgumentException("Wrong dimensions for this stack");
		if (stack[n-1] != null) {
			var na = stack[n-1];
			if (!na.setAll(ip.getPixels())) {
				stack[n-1] = NativeArray.create(ip.getPixels());
				//todo manually clean na
			}
		} else {
			stack[n-1] = NativeArray.create(ip.getPixels());
		}
	}

	/** Assigns a new color model to this stack. */
	public void setColorModel(ColorModel cm) {
		this.cm = cm;
	}
	
	/** Returns this stack's color model. May return null. */
	public ColorModel getColorModel() {
		return cm;
	}
	
	/** Returns true if this is a 3-slice, 8-bit RGB stack. */
	public boolean isRGB() {
    	return nSlices==3 && (stack[0] != null && stack[0].layout == ValueLayout.JAVA_BYTE) && getSliceLabel(1)!=null && getSliceLabel(1).equals("Red");
	}
	
	/** Returns true if this is a 3-slice HSB stack. */
	public boolean isHSB() {
    	return nSlices==3 && bitDepth==8 && getSliceLabel(1)!=null && getSliceLabel(1).equals("Hue");
	}

	/** Returns true if this is a 3-slice 32-bit HSB stack. */
	public boolean isHSB32() {
    	return nSlices==3 && bitDepth==32 && getSliceLabel(1)!=null && getSliceLabel(1).equals("Hue");
	}

	/** Returns true if this is a Lab stack. */
	public boolean isLab() {
    	return nSlices==3 && getSliceLabel(1)!=null && getSliceLabel(1).equals("L*");	
	}

	/** Returns true if this is a virtual (disk resident) stack. 
		This method is overridden by the VirtualStack subclass. */
	public boolean isVirtual() {
		return false;
	}

	/** Frees memory by deleting a few slices from the end of the stack. */
	public void trim() {
		int n = (int)Math.round(Math.log(nSlices)+1.0);
		for (int i=0; i<n; i++) {
			deleteLastSlice();
			System.gc();
		}
	}

	public String toString() {
		String v = isVirtual()?"(V)":"";
		return ("stack["+getWidth()+"x"+getHeight()+"x"+size()+v+"]");
	}
	
	/** Returns, as a double, the value of the specified voxel.
	 * Throws an IndexOutOfBoundsException if x, y or z are
	 * beyond the stack limits. Use the ImagePlus.getStackIndex()
	 * method to convert a C,Z,T hyperstack position (one-based)
	 * into a z index (zero-based).
	 * @see ImagePlus#getStackIndex
	*/
	public final double getVoxel(int x, int y, int z) {
		if (x>=0 && x<width && y>=0 && y<height && z>=0 && z<nSlices) {
			switch (bitDepth) {
				case 8:
					return stack[z].getByte((long) y*width+x)&0xff;
				case 16:
					return stack[z].getShort((long) y*width+x)&0xffff;
				case 32:
					return stack[z].getFloat((long) y*width+x);
				case 24:
					return stack[z].getInt((long) y*width+x)&0xffffffff;
				default: return Double.NaN;
			}
		} else
			throw new IndexOutOfBoundsException();
	}
		
	/* Sets the value of the specified voxel. */
	public final void setVoxel(int x, int y, int z, double value) {
		if (x>=0 && x<width && y>=0 && y<height && z>=0 && z<nSlices) {
			switch (bitDepth) {
				case 8:
					if (value>255.0)
						value = 255.0;
					else if (value<0.0)
						value = 0.0;
					stack[z].setByte((long) y*width+x, (byte)(value+0.5));
					break;
				case 16:
					if (value>65535.0)
						value = 65535.0;
					else if (value<0.0)
						value = 0.0;
					stack[z].setShort((long) y*width+x, (short)(value+0.5));
					break;
				case 32:
					stack[z].setFloat((long) y*width+x, (float)(value));
					break;
				case 24:
					stack[z].setInt((long) y*width+x, (int)(value));
					break;
			}
		}
	}
	
	public float[] getVoxels(int x0, int y0, int z0, int w, int h, int d, float[] voxels) {
		boolean inBounds = x0>=0 && x0+w<=width && y0>=0 && y0+h<=height && z0>=0 && z0+d<=nSlices;
		if (voxels==null || voxels.length!=w*h*d)
			voxels = new float[w*h*d];
		int i = 0;
		int offset;
		for (int z=z0; z<z0+d; z++) {
			for (int y=y0; y<y0+h; y++) {
				if (inBounds) {
					switch (bitDepth) {
						case 8:
							for (int x=x0; x<x0+w; x++)
								voxels[i++] = stack[z].getByte((long) y*width+x)&0xff;
							break;
						case 16:
							for (int x=x0; x<x0+w; x++)
								voxels[i++] = stack[z].getShort((long) y*width+x)&0xffff;
							break;
						case 32:
							for (int x=x0; x<x0+w; x++)
								voxels[i++] = stack[z].getFloat((long) y*width+x);
							break;
						case 24:
							for (int x=x0; x<x0+w; x++)
								voxels[i++] = stack[z].getInt((long) y*width+x)&0xffffffff;
							break;
						default:
							for (int x=x0; x<x0+w; x++)
								voxels[i++] = 0f;
					}
				} else {
					for (int x=x0; x<x0+w; x++)
						voxels[i++] = (float)getVoxel(x, y, z);
				}
			}
		}
		return voxels;
	}

	public float[] getVoxels(int x0, int y0, int z0, int w, int h, int d, float[] voxels, int channel) {
		if (bitDepth!=24)
			return getVoxels(x0, y0, z0, w, h, d, voxels);
		boolean inBounds = x0>=0 && x0+w<=width && y0>=0 && y0+h<=height && z0>=0 && z0+d<=nSlices;
		if (voxels==null || voxels.length!=w*h*d)
			voxels = new float[w*h*d];
		int i = 0;
		for (int z=z0; z<z0+d; z++) {
			for (int y=y0; y<y0+h; y++) {
				for (int x=x0; x<x0+w; x++) {
					int value = inBounds?stack[z].getInt((long) y*width+x)&0xffffffff:(int)getVoxel(x, y, z);
					switch (channel) {
						case 0: value=(value&0xff0000)>>16; break;
						case 1: value=(value&0xff00)>>8; break;
						case 2: value=value&0xff;; break;
					}
					voxels[i++] = (float)value;
				}
			}
		}
		return voxels;
	}

	/** Experimental */
	public void setVoxels(int x0, int y0, int z0, int w, int h, int d, float[] voxels) {
		boolean inBounds = x0>=0 && x0+w<=width && y0>=0 && y0+h<=height && z0>=0 && z0+d<=nSlices;
		if (voxels==null || voxels.length!=w*h*d)
			;
		int i = 0;
		float value;
		for (int z=z0; z<z0+d; z++) {
			for (int y=y0; y<y0+h; y++) {
				if (inBounds) {
					switch (bitDepth) {
						case 8:
							for (int x=x0; x<x0+w; x++) {
								value = voxels[i++];
								if (value>255f)
									value = 255f;
								else if (value<0f)
									value = 0f;
								stack[z].setByte((long) y*width+x, (byte)(value+0.5f));
							}
							break;
						case 16:
							for (int x=x0; x<x0+w; x++) {
								value = voxels[i++];
								if (value>65535f)
									value = 65535f;
								else if (value<0f)
									value = 0f;
								stack[z].setShort((long) y*width+x, (short)(value+0.5f));
							}
							break;
						case 32:
							for (int x=x0; x<x0+w; x++) {
								value = voxels[i++];
								stack[z].setFloat((long) y*width+x, value);
							}
							break;
						case 24:
							for (int x=x0; x<x0+w; x++) {
								value = voxels[i++];
								stack[z].setInt((long) y*width+x, (int)(value));
							}
							break;
					}
				} else {
					for (int x=x0; x<x0+w; x++)
						setVoxel(x, y, z, voxels[i++]);
				}
			}
		}
	}
	
	/** Experimental */
	public void setVoxels(int x0, int y0, int z0, int w, int h, int d, float[] voxels, int channel) {
		if (bitDepth!=24) {
			setVoxels(x0, y0, z0, w, h, d, voxels);
			return;
		}
		boolean inBounds = x0>=0 && x0+w<=width && y0>=0 && y0+h<=height && z0>=0 && z0+d<=nSlices;
		if (voxels==null || voxels.length!=w*h*d)
			;
		int i = 0;
		for (int z=z0; z<z0+d; z++) {
			for (int y=y0; y<y0+h; y++) {
				for (int x=x0; x<x0+w; x++) {
					int value = inBounds?stack[z].getInt((long) y*width+x)&0xffffffff:(int)getVoxel(x, y, z);
					int color = (int)voxels[i++];
					switch (channel) {
						case 0: value=(value&0xff00ffff) | ((color&0xff)<<16); break;
						case 1: value=(value&0xffff00ff) | ((color&0xff)<<8); break;
						case 2: value=(value&0xffffff00) | (color&0xff); break;
					}
					if (inBounds)
						stack[z].setInt((long) y*width+x, value);
					else
						setVoxel(x, y, z, value);
				}
			}
		}
	}

	/** Experimental */
	public void drawSphere(double radius, int xc, int yc, int zc) {
		int diameter = (int)Math.round(radius*2);
	    double r = radius;
		int xmin=(int)(xc-r+0.5), ymin=(int)(yc-r+0.5), zmin=(int)(zc-r+0.5);
		int xmax=xmin+diameter, ymax=ymin+diameter, zmax=zmin+diameter;
		double r2 = r*r;
		r -= 0.5;
		double xoffset=xmin+r, yoffset=ymin+r, zoffset=zmin+r;
		double xx, yy, zz;
		for (int x=xmin; x<=xmax; x++) {
			for (int y=ymin; y<=ymax; y++) {
				for (int z=zmin; z<=zmax; z++) {
					xx = x-xoffset; yy = y-yoffset;  zz = z-zoffset;
					if (xx*xx+yy*yy+zz*zz<=r2)
						setVoxel(x, y, z, 255);
				}
			}
		}
	}

	
	/** Returns the bit depth (8=byte, 16=short, 24=RGB, 32=float). */
	public int getBitDepth() {
		if (this.bitDepth==0 && stack!=null && stack.length>0)
			setBitDepth(stack[0]);
		return this.bitDepth;
	}
	
	/** Sets the bit depth (8=byte, 16=short, 24=RGB, 32=float). */
	public void setBitDepth(int depth) {
		if (size()==0 && (depth==8||depth==16||depth==24||depth==32))
			this.bitDepth = depth;
	}

	/** Creates a new ImageStack.
	*  @param width  width in pixels
	*  @param height height in pixels
	*  @param depth number of images
	*  @param bitdepth  8, 16, 32 (float) or 24 (RGB)
	*/
	 public static ImageStack create(int width, int height, int depth, int bitdepth) {
		ImageStack stack =  IJ.createImage("", width, height, depth, bitdepth).getStack();
		if (bitdepth==16 || bitdepth==32) {
			stack.min = Double.MAX_VALUE;
			stack.max = 0.0;
		}
		return stack;
	 }
	 
	/** Creates an ImageStack from an ImagePlus array. */
	 public static ImageStack create(ImagePlus[] images) {
		int w = 0;
		int h = 0;
		for (int i=0; i<images.length; i++) {
			if (images[i].getWidth()>w) w=images[i].getWidth();
			if (images[i].getHeight()>h) h=images[i].getHeight();
		}
		ImageStack stack =  new ImageStack(w, h);
		stack.init(w, h);
		for (int i=0; i<images.length; i++) {
			stack.addSlice(images[i].getProcessor());
		}
		int depth = images[0].getBitDepth();
		if (depth==16 || depth==32) {
			stack.min = Double.MAX_VALUE;
			stack.max = 0.0;
		}
		return stack;
	 }

	/** Duplicates this stack. */
	 public ImageStack duplicate() {
	 	return crop(0, 0, 0, width, height, size());
	 }
	 
	/** Creates a new stack by cropping this one. */
	 public ImageStack crop(int x, int y, int z, int width, int height, int depth) {
	 	if (x<0||y<0||z<0||x+width>this.width||y+height>this.height||z+depth>size())
	 		throw new IllegalArgumentException("Argument out of range");
		ImageStack stack2 = new ImageStack(width, height, getColorModel());
		for (int i=z; i<z+depth; i++) {
			ImageProcessor ip2 = this.getProcessor(i+1);
			ip2.setRoi(x, y, width, height);
			ip2 = ip2.crop();
			stack2.addSlice(this.getSliceLabel(i+1), ip2);
		}
		return stack2;
	 }
	 
	/** Creates a float version of this stack. */
	 public ImageStack convertToFloat() {
		ImageStack stack2 = new ImageStack(width, height, getColorModel());
		for (int i=1; i<=size(); i++) {
			ImageProcessor ip2 = this.getProcessor(i);
			ip2 = ip2.convertToFloat();
			stack2.addSlice(this.getSliceLabel(i), ip2);
		}
		return stack2;
	 }
	 
	 int viewers(int inc) {
	 	viewers += inc;
	 	return viewers;
	 }
	 
	 public void setOptions(String options) {
	 	if (options==null) return;
	 	signedInt = options.contains("32-bit int");
	 }

	/**
	 * A thin wrapper over a MemorySegment, providing an off-heap array
	 * @param segment the off-heap memory segment
	 * @param layout  the element layout (e.g. ValueLayout.JAVA_FLOAT)
	 * @param size    number of elements
	 */
	public record NativeArray(MemorySegment segment, ValueLayout layout, long size) {
		public static NativeArray create(ValueLayout layout, long size) {
			var arena = Arena.ofShared();
			var na = new NativeArray(arena.allocate(layout, size), layout, size);
			Arena finalArena = arena;
			cleaner.register(na, finalArena::close);
			//todo store Cleaner and call #clean when removing a slice and in ImagePlus#flush?
			return na;
		}

		public static NativeArray create(Object arr) {
			return switch (arr) {
				case byte[] a -> {
					var na = create(ValueLayout.JAVA_BYTE, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case char[] a -> {
					var na = create(ValueLayout.JAVA_CHAR, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case short[] a -> {
					var na = create(ValueLayout.JAVA_SHORT, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case int[] a -> {
					var na = create(ValueLayout.JAVA_INT, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case long[] a -> {
					var na = create(ValueLayout.JAVA_LONG, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case float[] a -> {
					var na = create(ValueLayout.JAVA_FLOAT, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case double[] a -> {
					var na = create(ValueLayout.JAVA_DOUBLE, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case boolean[] a -> {
					var na = create(ValueLayout.JAVA_BOOLEAN, a.length);
					MemorySegment.copy(a, 0, na.segment, na.layout, 0, a.length);
					yield na;
				}
				case NativeArray nativeArray -> nativeArray;
				case null -> null;
				default -> throw new IllegalArgumentException(String.valueOf(arr.getClass()));
			};
		}

		public boolean getBoolean(long index) {
			return segment.getAtIndex((ValueLayout.OfBoolean) layout, index);
		}

		public void setBoolean(long index, boolean value) {
			segment.setAtIndex((ValueLayout.OfBoolean) layout, index, value);
		}

		public byte getByte(long index) {
			return segment.getAtIndex((ValueLayout.OfByte) layout, index);
		}

		public void setByte(long index, byte value) {
			segment.setAtIndex((ValueLayout.OfByte) layout, index, value);
		}

		public char getChar(long index) {
			return segment.getAtIndex((ValueLayout.OfChar) layout, index);
		}

		public void setChar(long index, char value) {
			segment.setAtIndex((ValueLayout.OfChar) layout, index, value);
		}

		public short getShort(long index) {
			return segment.getAtIndex((ValueLayout.OfShort) layout, index);
		}

		public void setShort(long index, short value) {
			segment.setAtIndex((ValueLayout.OfShort) layout, index, value);
		}

		public int getInt(long index) {
			return segment.getAtIndex((ValueLayout.OfInt) layout, index);
		}

		public void setInt(long index, int value) {
			segment.setAtIndex((ValueLayout.OfInt) layout, index, value);
		}

		public long getLong(long index) {
			return segment.getAtIndex((ValueLayout.OfLong) layout, index);
		}

		public void setLong(long index, long value) {
			segment.setAtIndex((ValueLayout.OfLong) layout, index, value);
		}

		public float getFloat(long index) {
			return segment.getAtIndex((ValueLayout.OfFloat) layout, index);
		}

		public void setFloat(long index, float value) {
			segment.setAtIndex((ValueLayout.OfFloat) layout, index, value);
		}

		public double getDouble(long index) {
			return segment.getAtIndex((ValueLayout.OfDouble) layout, index);
		}

		public void setDouble(long index, double value) {
			segment.setAtIndex((ValueLayout.OfDouble) layout, index, value);
		}

		public void setAll(int dstOffset, Object src, int srcOffset, int length) {
			MemorySegment.copy(src, srcOffset, segment, layout, dstOffset, length);
		}

		public boolean setAll(Object src) {
			int s;
			switch (src) {
				case byte[] a:
					if (layout != ValueLayout.JAVA_BYTE) {
						return false;
					}
					s = a.length;
					break;
				case char[] a:
					if (layout != ValueLayout.JAVA_SHORT) {
						return false;
					}
					s = a.length;
					break;
				case short[] a:
					if (layout != ValueLayout.JAVA_SHORT) {
						return false;
					}
					s = a.length;
					break;
				case int[] a:
					if (layout != ValueLayout.JAVA_INT) {
						return false;
					}
					s = a.length;
					break;
				case long[] a:
					if (layout != ValueLayout.JAVA_LONG) {
						return false;
					}
					s = a.length;
					break;
				case float[] a:
					if (layout != ValueLayout.JAVA_FLOAT) {
						return false;
					}
					s = a.length;
					break;
				case double[] a:
					if (layout != ValueLayout.JAVA_DOUBLE) {
						return false;
					}
					s = a.length;
					break;
				case boolean[] a:
					if (layout != ValueLayout.JAVA_BOOLEAN) {
						return false;
					}
					s = a.length;
					break;
				case NativeArray nativeArray: {
					if (nativeArray.layout != layout) {
						return false;
					}
					if (nativeArray == this) {
						return true;
					}
					MemorySegment.copy(nativeArray.segment, 0, segment, 0, segment.byteSize());
					return true;
				}
				default:
					throw new IllegalArgumentException(String.valueOf(src.getClass()));
			}

			if (s != size) {
				return false;
			}

			setAll(0, src, 0, s);

			return true;
		}

		public Object toArray() {
			return switch (layout) {
				case ValueLayout.OfBoolean ofBoolean -> {
					var a = new boolean[(int) size];
					MemorySegment.copy(segment, ofBoolean, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfByte ofByte -> {
					var a = new byte[(int) size];
					MemorySegment.copy(segment, ofByte, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfChar ofChar -> {
					var a = new char[(int) size];
					MemorySegment.copy(segment, ofChar, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfDouble ofDouble -> {
					var a = new double[(int) size];
					MemorySegment.copy(segment, ofDouble, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfFloat ofFloat -> {
					var a = new float[(int) size];
					MemorySegment.copy(segment, ofFloat, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfInt ofInt -> {
					var a = new int[(int) size];
					MemorySegment.copy(segment, ofInt, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfLong ofLong -> {
					var a = new long[(int) size];
					MemorySegment.copy(segment, ofLong, 0L, a, 0, (int) size);
					yield a;
				}
				case ValueLayout.OfShort ofShort -> {
					var a = new short[(int) size];
					MemorySegment.copy(segment, ofShort, 0L, a, 0, (int) size);
					yield a;
				}
				default -> throw new UnsupportedOperationException(layout.toString());
			};
		}
	}
}
