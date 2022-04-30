package ij.plugin;

import ij.*;
import ij.astro.AstroImageJ;
import ij.astro.types.Pair;
import ij.astro.util.ZipOpenerUtil;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import ij.util.DicomTools;
import ij.util.StringSorter;
import ij.util.Tools;

import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

/** Implements the File/Import/Image Sequence command, which
	opens a folder of images as a stack. */
public class FolderOpener implements PlugIn {
	private static final int MAX_SEPARATE = 40;
	private static final String DIR_KEY = "import.sequence.dir";
	private static final String[] types = {"default", "16-bit", "32-bit", "RGB"};
	private static String[] excludedTypes = {".txt", ".lut", ".roi", ".pty", ".hdr", ".java", ".ijm", ".py", ".js", ".bsh", ".xml"};
	private static boolean staticSortFileNames = true;
	private static boolean staticOpenAsVirtualStack;
	private boolean convertToRGB;
	private boolean convertToGrayscale;  //unused
	private boolean sortFileNames = true;
	private boolean sortByMetaData = true;
	private boolean openAsVirtualStack;
	private String directory;
	private boolean directorySet;
	private String filter;
	private String legacyRegex;
	private FileInfo fi;
	private String info1;
	private ImagePlus image;
	private boolean saveImage;
	private long t0;
	private int stackWidth, stackHeight;
	private int bitDepth;
	private int defaultBitDepth;
	private int nFiles = 0;
	private int start = 1;
	private int step = 1;
	private double scale = 100.0;
	private boolean openAsSeparateImages;
	@AstroImageJ(reason = "Allow FITS reader to track virtual stack")
	public static boolean virtualIntended;

	
	/** Opens the images in the specified directory as a stack. Displays
		directory chooser and options dialogs if the argument is null. */
	public static ImagePlus open(String path) {
		return open(path, null);
	}

	/** Opens the images in the specified directory as a stack. Opens
		the images as a virtual stack if the 'options' string contains
		'virtual' or 'use'. Add ' file=abc' to the options string to only open
		images with, for example, 'abc' in their name. Add ' noMetaSort' to
		disable sorting of DICOM stacks by series number (0020,0011).
		Displays directory chooser and options dialogs if the the 'path'
		argument is null. */
	public static ImagePlus open(String path, String options) {
		FolderOpener fo = new FolderOpener();
		fo.saveImage = true;
		fo.setOptions(options);
		fo.run(path);
		return fo.image;
	}

	/** Opens the images in the specified directory as a widthxheight stack.
 		Opens the images as a virtual stack if the 'options' string contains
		'virtual' or 'use'. Add ' file=abc' to the options string to only open
		images with, for example, 'abc' in their name. Add ' noMetaSort' to
		disable sorting of DICOM stacks by series number (0020,0011).
	*/
	public static ImagePlus open(String path, int width, int height, String options) {
		FolderOpener fo = new FolderOpener();
		fo.saveImage = true;
		fo.stackWidth = width;
		fo.stackHeight = height;
		fo.setOptions(options);
		fo.run(path);
		return fo.image;
	}
	
	private void setOptions(String options) {
		if (options==null)
			return;
		openAsVirtualStack = options.contains("virtual") || options.contains("use");
		if (options.contains("noMetaSort")) 
			sortByMetaData = false;
		bitDepth = (int)Tools.getNumberFromList(options,"bitdepth=",0);
		filter = Macro.getValue(options, "filter", "");
		this.start = (int)Tools.getNumberFromList(options,"start=",1);
		this.step = (int)Tools.getNumberFromList(options,"step=",1);
		this.scale = Tools.getNumberFromList(options,"scale=",100);
	}

	/** Opens the images in the specified directory as a stack. Displays
		directory chooser and options dialogs if the argument is null. */
	public ImagePlus openFolder(String path) {
		saveImage = true;
		run(path);
		return image;
	}

	@AstroImageJ(reason = "When opening images that individually go to a stack, preserve stack title. This allows" +
			" MultiAperture to run on a folder of 3D fits images, otherwise WCS and other information is lost;" +
			" If filter fails to match any files, after closing the error reopen dialog.",
			modified = true)
	public void run(String arg) {
		boolean isMacro = Macro.getOptions()!=null;
		if (!directorySet)
			directory = null;
		if (arg!=null && !arg.equals("")) {
			directory = arg;
		} else {
			if (!isMacro) {
				sortFileNames = staticSortFileNames;
				openAsVirtualStack = staticOpenAsVirtualStack;
			}
			arg = null;
			String title = "Open Image Sequence...";
			String macroOptions = Macro.getOptions();
			if (macroOptions!=null) {
				directory = Macro.getValue(macroOptions, title, null);
				if (directory!=null) {
					directory = OpenDialog.lookupPathVariable(directory);
					File f = new File(directory);
					if (!f.isDirectory() && (f.exists()||directory.lastIndexOf(".")>directory.length()-5))
						directory = f.getParent();
				}
				legacyRegex = Macro.getValue(macroOptions, "or", "");
				if (legacyRegex.equals(""))
					legacyRegex = null;
			}
		}
		if (arg==null) {
			if (!showDialog()) return;
		}
		File file = new File(directory);
		String[] list = file.list();

		// Zip as folder
		if (list == null) {
			list = ZipOpenerUtil.getFilePathsInZip(directory);
			if (list.length == 0) list = null;
		} // End zip as folder

		if (list==null) {
			String parent = file.getParent();
			file = new File(parent);
			list = file.list();
			if (list!=null)
				directory = parent;
			else {
				IJ.error("File>Import>Image Sequence", "Directory not found: "+directory);
				return;
			}
		}
		//remove subdirectories from list
		ArrayList fileList = new ArrayList();
		for (int i=0; i<list.length; i++) {
			File f = (new File(directory+list[i]));
			if (!f.isDirectory())
				fileList.add(list[i]);
		}
		if (fileList.size()<list.length)
			list = (String[])fileList.toArray(new String[fileList.size()]);

		String title = directory;
		if (title.endsWith(File.separator) || title.endsWith("/"))
			title = title.substring(0, title.length()-1);
		int index = title.lastIndexOf(File.separatorChar);
		if (index!=-1)
			title = title.substring(index + 1);
		else {
			index = title.lastIndexOf("/");
			if (index!=-1)
				title = title.substring(index + 1);
		}
		if (title.endsWith(":"))
			title = title.substring(0, title.length()-1);
		
		list = trimFileList(list);
		if (list==null) return;
		if (IJ.debugMode) IJ.log("FolderOpener: "+directory+" ("+list.length+" files)");
		int width=0, height=0, stackSize=1;
		ImageStack stack = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		Calibration cal = null;
		boolean allSameCalibration = true;
		IJ.resetEscape();		
		Overlay overlay = null;
		if (this.nFiles==0)
			this.nFiles = list.length;
		boolean dicomImages = false;
		try {
			for (int i=0; i<list.length; i++) {
				Opener opener = new Opener();
				opener.setSilentMode(true);
				IJ.redirectErrorMessages(true);
				ImagePlus imp = opener.openImage(directory, list[i]);
				IJ.redirectErrorMessages(false);
				if (imp!=null) {
					width = imp.getWidth();
					height = imp.getHeight();
					if (this.bitDepth==0) {
						this.bitDepth = imp.getBitDepth();
						this.defaultBitDepth = bitDepth;
					}
					String info = (String)imp.getProperty("Info");
					if (info!=null && info.contains("7FE0,0010"))
						dicomImages = true;
					break;
				}
			}
			if (width==0) {
				IJ.error("Sequence Reader", "This folder does not appear to contain\n"
				+ "any TIFF, JPEG, BMP, DICOM, GIF, FITS or PGM files that match the specified filters.\n \n"
				+ "   \""+directory+"\"");
				return;
			}
			String pluginName = "Sequence Reader";
			if (legacyRegex!=null)
				pluginName += "(legacy)";
			list = getFilteredList(list, filter, pluginName);
			if (list==null) {
				run(arg); // Reopen dialog if filter fails
				return;
			}
			IJ.showStatus("");
			t0 = System.currentTimeMillis();
			if (sortFileNames || dicomImages || IJ.isMacOSX())
				list = StringSorter.sortNumerically(list);

			if (this.nFiles<1)
				this.nFiles = list.length;
			if (this.start<1 || this.start>list.length)
				this.start = 1;
			if (this.start+this.nFiles-1>list.length)
				this.nFiles = list.length-this.start+1;
			int count = 0;
			int counter = 0;
			ImagePlus imp = null;
			boolean firstMessage = true;
			boolean fileInfoStack = false;
			
			// open images as stack
			for (int i=this.start-1; i<list.length; i++) {
				if ((counter++%this.step)!=0)
					continue;
				Opener opener = new Opener();
				opener.setSilentMode(true);
				IJ.redirectErrorMessages(true);
				if ("RoiSet.zip".equals(list[i])) {
					IJ.open(directory+list[i]);
					imp = null;
				} else if (!openAsVirtualStack||stack==null) {
					imp = opener.openImage(directory, list[i]);
					stackSize = imp!=null?imp.getStackSize():1;
				}
				IJ.redirectErrorMessages(false);
				if (imp!=null && stack==null) {
					width = imp.getWidth();
					height = imp.getHeight();
					if (stackWidth>0 && stackHeight>0) {
						width = stackWidth;
						height = stackHeight;
					}
					if (bitDepth==0) bitDepth = imp.getBitDepth();
					fi = imp.getOriginalFileInfo();
					ImageProcessor ip = imp.getProcessor();
					min = ip.getMin();
					max = ip.getMax();
					cal = imp.getCalibration();
					if (convertToRGB) bitDepth = 24;
					ColorModel cm = imp.getProcessor().getColorModel();
					if (openAsVirtualStack) {
						if (stackSize>1) {
							stack = new FileInfoVirtualStack();
							fileInfoStack = true;
						} else {
							if (stackWidth>0 && stackHeight>0)
								stack = new VirtualStack(stackWidth, stackHeight, cm, directory);
							else
								stack = new VirtualStack(width, height, cm, directory);
						}
					}  else if (this.scale<100.0)						
						stack = new ImageStack((int)(width*this.scale/100.0), (int)(height*this.scale/100.0), cm);
					else
						stack = new ImageStack(width, height, cm);
					if (bitDepth!=0)
						stack.setBitDepth(bitDepth);
					info1 = (String)imp.getProperty("Info");
				}
				if (imp==null)
					continue;
				if (imp.getWidth()!=width || imp.getHeight()!=height) {
					if (stackWidth>0 && stackHeight>0) {
						ImagePlus imp2 = imp.createImagePlus();
						ImageProcessor ip = imp.getProcessor();
						ImageProcessor ip2 = ip.createProcessor(width,height);
						ip2.insert(ip, 0, 0);
						imp2.setProcessor(ip2);
						imp = imp2;
					} else {
						IJ.log(list[i] + ": wrong size; "+width+"x"+height+" expected, "+imp.getWidth()+"x"+imp.getHeight()+" found");
						continue;
					}
				}
				String label = imp.getTitle();
				if (stackSize==1) {
					String info = (String)imp.getProperty("Info");
					if (info!=null) {
						if (useInfo(info))
							label += "\n" + info;
					} else if (imp.getStackSize()>0) {
						String sliceLabel = imp.getStack().getSliceLabel(1);
						if (useInfo(sliceLabel))
							label =  sliceLabel;
					}
				}
				if (Math.abs(imp.getCalibration().pixelWidth-cal.pixelWidth)>0.0000000001)
					allSameCalibration = false;
				ImageStack inputStack = imp.getStack();
				Overlay overlay2 = imp.getOverlay();
				if (overlay2!=null && !openAsVirtualStack) {
					if (overlay==null)
						overlay = new Overlay();
					for (int j=0; j<overlay2.size(); j++) {
						Roi roi = overlay2.get(j);
						int position = roi.getPosition();
						if (position==0)
							roi.setPosition(count+1);
						overlay.add(roi);
					}
				}				
				if (openAsVirtualStack) { 
					if (fileInfoStack)
						openAsFileInfoStack((FileInfoVirtualStack)stack, directory+list[i]);
					else
						((VirtualStack)stack).addSlice(list[i]);
				} else {
					for (int slice=1; slice<=stackSize; slice++) {
						int bitDepth2 = imp.getBitDepth();
						String label2 = label;
						ImageProcessor ip = null;
						if (stackSize>1) {
							String sliceLabel = inputStack.getSliceLabel(slice);
							label2 = "Image " + (i+1) + " : " + sliceLabel;
						}
						ip = inputStack.getProcessor(slice);
						if (convertToRGB) {
							ip = ip.convertToRGB();
							bitDepth2 = 24;
						}
						if (bitDepth2!=bitDepth) {
							if (dicomImages && bitDepth==16 && bitDepth2==32 && this.scale==100) {
								ip = ip.convertToFloat();
								bitDepth = 32;
								ImageStack stack2 = new ImageStack(width, height, stack.getColorModel());
								for (int n=1; n<=stack.size(); n++) {
									ImageProcessor ip2 = stack.getProcessor(n);
									ip2 = ip2.convertToFloat();
									ip2.subtract(32768);
									String sliceLabel = stack.getSliceLabel(n);
									stack2.addSlice(sliceLabel, ip2.convertToFloat());
								}
								stack = stack2;
							}
						}
						if (this.scale<100.0)
							ip = ip.resize((int)(width*this.scale/100.0), (int)(height*this.scale/100.0));
						if (ip.getMin()<min) min = ip.getMin();
						if (ip.getMax()>max) max = ip.getMax();
						stack.addSlice(label2, ip);
					}
				}
				count++;
				IJ.showStatus("!"+count+"/"+this.nFiles);
				IJ.showProgress(count, this.nFiles);
				if (count>=this.nFiles) 
					break;
				if (IJ.escapePressed())
					{IJ.beep(); break;}
			}  // open images as stack
			
		} catch(OutOfMemoryError e) {
			IJ.outOfMemory("FolderOpener");
			if (stack!=null) stack.trim();
		}
		if (stack!=null && stack.size()>0) {
			ImagePlus imp2 = new ImagePlus(title, stack);
			if (imp2.getType()==ImagePlus.GRAY16 || imp2.getType()==ImagePlus.GRAY32)
				imp2.getProcessor().setMinAndMax(min, max);
			if (fi==null)
				fi = new FileInfo();
			fi.fileFormat = FileInfo.UNKNOWN;
			fi.fileName = "";
			fi.directory = directory;
			imp2.setFileInfo(fi); // saves FileInfo of the first image
			imp2.setOverlay(overlay);
			if (stack instanceof VirtualStack) {
				Properties props = ((VirtualStack)stack).getProperties();
				if (props!=null)
					imp2.setProperty("FHT", props.get("FHT"));
			}
			if (allSameCalibration) {
				// use calibration from first image
				if (this.scale!=100.0 && cal.scaled()) {
					cal.pixelWidth /= this.scale/100.0;
					cal.pixelHeight /= this.scale/100.0;
				}
				if (cal.pixelWidth!=1.0 && cal.pixelDepth==1.0)
					cal.pixelDepth = cal.pixelWidth;
				imp2.setCalibration(cal);
			}
			if (info1!=null && info1.lastIndexOf("7FE0,0010")>0) { //DICOM
				if (sortByMetaData)
					stack = DicomTools.sort(stack);
				imp2.setStack(stack);
				double voxelDepth = DicomTools.getVoxelDepth(stack);
				if (voxelDepth>0.0) {
					if (IJ.debugMode) IJ.log("DICOM voxel depth set to "+voxelDepth+" ("+cal.pixelDepth+")");
					cal.pixelDepth = voxelDepth;
					imp2.setCalibration(cal);
				}
				if (imp2.getType()==ImagePlus.GRAY16 || imp2.getType()==ImagePlus.GRAY32) {
					imp2.getProcessor().setMinAndMax(min, max);
					imp2.updateAndDraw();
				}
			}
			if (imp2.getStackSize()==1) {
				imp2.setProperty("Label", list[0]);
				if (info1!=null)
					imp2.setProperty("Info", info1);
			}
			if (arg==null && !saveImage) {
				String time = (System.currentTimeMillis()-t0)/1000.0 + " seconds";
				if (openAsSeparateImages && imp2.getStackSize()<=MAX_SEPARATE)
					openAsSeparateImages(imp2);
				else
					imp2.show(time);
				if (stack.isVirtual()) {
					overlay = stack.getProcessor(1).getOverlay();
					if (overlay!=null)
						imp2.setOverlay(overlay);
				}
			}
			if (saveImage)
				image = imp2;
			if (openAsSeparateImages && imp2.getStackSize()>MAX_SEPARATE)
				IJ.error("Import>Image Sequence", "A maximum of "+MAX_SEPARATE+" images can be opened separately.");
		}
		IJ.showProgress(1.0);
		if (Recorder.record) {
			String options = openAsVirtualStack?"virtual":"";
			if (bitDepth!=defaultBitDepth)
				options = options + " bitdepth=" + bitDepth;				
			if (filter!=null && filter.length()>0) {
				if (filter.contains(" "))
					filter = "["+filter+"]";
				options = options + " filter=" + filter;
			}
			if (start!=1)
				options = options + " start=" + start;				
			if (step!=1)
				options = options + " step=" + step;				
			if (scale!=100)
				options = options + " scale=" + scale;				
			if (!sortByMetaData)
				options = options + " noMetaSort";
			String dir = Recorder.fixPath(directory);
   			Recorder.recordCall("imp = FolderOpener.open(\""+dir+"\", \""+options+"\");");
		}
		virtualIntended = false;
		FITS_Reader.resetFilter();
	}
	
	private void openAsSeparateImages(ImagePlus imp) {
		VirtualStack stack = (VirtualStack)imp.getStack();
		String dir = stack.getDirectory();
		for (int n=1; n<=stack.size(); n++)
			IJ.open(dir+stack.getFileName(n));	
	}
	
	public static boolean useInfo(String info) {
		return info!=null && !(info.startsWith("Software")||info.startsWith("ImageDescription"));
	 }
	
	private void openAsFileInfoStack(FileInfoVirtualStack stack, String path) {
		FileInfo[] info = Opener.getTiffFileInfo(path);
		if (info==null || info.length==0)
			return;
		int n =info[0].nImages;
		if (info.length==1 && n>1) {
			long size = fi.width*fi.height*fi.getBytesPerPixel();
			for (int i=0; i<n; i++) {
				FileInfo fi = (FileInfo)info[0].clone();
				fi.nImages = 1;
				fi.longOffset = fi.getOffset() + i*(size + fi.getGap());
				stack.addImage(fi);
			}
		} else {
			FileInfo fi = info[0];
			if (fi.fileType==FileInfo.RGB48) {
				for (int slice=1; slice<=3; slice++) {
					FileInfo fi2 = (FileInfo)fi.clone();
					fi2.sliceNumber = slice;
					stack.addImage(fi2);
				}
			} else
				stack.addImage(fi);
		}
	}

	@AstroImageJ(reason = "Save preference option to open as virtual stack; widen access; support zip files as folder;" +
			"Add filter count",
			modified = true)
	public boolean showDialog() {
		String options = Macro.getOptions();
		if  (options!=null) {  //macro
			String optionsOrig = options;
			options = options.replace("open=", "dir=");
			options = options.replace("file=", "filter=");
			options =  options.replace("starting=","start=");
			options =  options.replace("number=","count=");
			options =  options.replace("increment=","step=");
			options =  options.replace("inc=","step=");
			if (!options.equals(optionsOrig))
				Macro.setOptions(options);
			if (options.contains("convert_to_rgb"))
				this.bitDepth = 24;
		}
		String countStr = "---";
		if (directorySet) {
			File f = new File(directory);
			String[] names = f.list();
			// Zip as folder
			if (names == null || names.length == 0) {
				names = ZipOpenerUtil.getFilePathsInZip(directory);
			} // End zip as folder
			names = (new FolderOpener()).trimFileList(names);
			countStr = ""+names.length;
		} else
			directory = Prefs.get(DIR_KEY, IJ.getDir("downloads")+"stack/");
		GenericDialog gd = new GenericDialog("Import Image Sequence");
		gd.setInsets(5, 0, 0);
		gd.addDirectoryField("Dir:", directory);		
		gd.setInsets(2, 55, 10);
		gd.addMessage("drag and drop target", IJ.font10, Color.darkGray);
		gd.addChoice("Type:", types, bitDepthToType(bitDepth));
		gd.addStringField("Filter:", "", 10);
		gd.setInsets(0,55,0);
		gd.addMessage("file name filtering text (can also enclose regex in parens)", IJ.font10, Color.darkGray);
		gd.addNumericField("Start:", this.start, 0, 6, "");
		gd.addStringField("Count:", countStr, 6);
		gd.addNumericField("Step:", this.step, 0, 6, "");
		gd.addNumericField("Scale:", this.scale, 0, 6, "%");

		gd.addMessage("Filter based on FITS header keywords and values:");
		gd.addMessage("(filtering not accounted for in file count and stack size below)", IJ.font10, Color.darkGray);
		gd.addStringField("Keyword 1:", "");
		gd.addToSameRow();
		gd.addStringField("Value 1:", "");
		gd.addRadioButtonGroup("", new String[]{"AND", "OR"}, 1, 2, "AND");
		gd.addToSameRow();
		gd.addMessage("(blank Keyword and Value boxes are ignored)");
		gd.addStringField("Keyword 2:", "");
		gd.addToSameRow();
		gd.addStringField("Value 2:", "");

		gd.addCheckbox("Sort names numerically", sortFileNames);
		gd.addCheckbox("Use virtual stack", Prefs.get("folderopener.openAsVirtualStack", openAsVirtualStack));
		gd.addCheckbox("Open as separate images", false);		
		gd.addHelp(IJ.URL+"/docs/menus/file.html#seq1");

		// Add display of stack size
		var initialSizes = getFileCount(gd);
		gd.addMessage("Matched files: " + initialSizes.first());
		var filterCountDisplay = (Label) gd.getComponent(gd.getComponentCount() - 1);
		gd.addMessage("Estimated stack size: " + initialSizes.second() + " MB");
		var filterSizeDisplay = (Label) gd.getComponent(gd.getComponentCount() - 1);

		for (Object stringField : gd.getStringFields()) {
			((TextField) stringField).addTextListener($ -> {
				filterCountDisplay.setText("Matched files: " + 0);
				filterSizeDisplay.setText("Estimated stack size: " + 0 + " MB");
				var x = getFileCount(gd);
				filterCountDisplay.setText("Matched files: " + x.first());
				filterSizeDisplay.setText("Estimated stack size: " + IJ.d2s(x.second(), 1) + " MB");
			});
		}
		// End display stack size

		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		directory = gd.getNextString();
		Prefs.set(DIR_KEY, directory);
		gd.setSmartRecording(true);
		int index = gd.getNextChoiceIndex();
		bitDepth = typeToBitDepth(types[index]);
		filter = gd.getNextString();
		if (legacyRegex!=null)
			filter = "("+legacyRegex+")";			
		gd.setSmartRecording(true);
		this.start = (int)gd.getNextNumber();
		countStr = gd.getNextString();
		double count = Tools.parseDouble(countStr);
		if (!Double.isNaN(count))
			nFiles = (int)count;
		FITS_Reader.makeHeaderCardFilter(gd.getNextString(), gd.getNextString(), gd.getNextString(), gd.getNextString(), gd.getNextRadioButton());
		this.step = (int)gd.getNextNumber();
		if (this.step<1)
			this.step = 1;
		this.scale = gd.getNextNumber();
		if (this.scale<5.0) this.scale = 5.0;
		if (this.scale>100.0) this.scale = 100.0;
		sortFileNames = gd.getNextBoolean();
		if (!sortFileNames)
			sortByMetaData = false;
		openAsVirtualStack = gd.getNextBoolean();
		Prefs.set("folderopener.openAsVirtualStack", openAsVirtualStack);
		if (openAsVirtualStack)
			scale = 100.0;
		openAsSeparateImages = gd.getNextBoolean();
		if (openAsSeparateImages)
			openAsVirtualStack = true;
		if (!IJ.macroRunning()) {
			staticSortFileNames = sortFileNames;
			if (!openAsSeparateImages)
				staticOpenAsVirtualStack = openAsVirtualStack;
		}
		virtualIntended = openAsVirtualStack;
		return true;
	}
	
	public static String[] getFilteredList(String[] list, String filter, String title) {
		boolean isRegex = false;
		if (filter!=null && (filter.equals("") || filter.equals("*")))
			filter = null;
		if (list==null || filter==null)
			return list;
		if (title==null) {
			String[] list2 = new String[list.length];
			for (int i=0; i<list.length; i++)
				list2[i] = list[i];
			list = list2;
		}
		if (filter.length()>=2 && filter.startsWith("(")&&filter.endsWith(")")) {
			filter = filter.substring(1,filter.length()-1);
			isRegex = true;
		}
		int filteredImages = 0;
		for (int i=0; i<list.length; i++) {
			if (isRegex && containsRegex(list[i],filter,title!=null&&title.contains("(legacy)")))
				filteredImages++;
			else if (list[i].contains(filter))
				filteredImages++;
			else
				list[i] = null;
		}
		if (filteredImages==0) {
			if (title!=null) {
				if (isRegex)
					IJ.error(title, "None of the file names contain the regular expression '"+filter+"'.");
				else
					IJ.error(title, "None of the "+list.length+" files contain '"+filter+"' in the name.");
			}
			return null;
		}
		String[] list2 = new String[filteredImages];
		int j = 0;
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				list2[j++] = list[i];
		}
		list = list2;
		return list;
	}
	
	public static boolean containsRegex(String name, String regex, boolean legacy) {
		boolean contains = false;
		try {
			if (legacy)
				contains = name.matches(regex);
			else
				contains = name.replaceAll(regex,"").length()!=name.length();
			IJ.showStatus("");
		} catch(Exception e) {
			String msg = e.getMessage();
			int index = msg.indexOf("\n");
			if (index>0)
				msg = msg.substring(0,index);
			IJ.showStatus("Regex error: "+msg);
			contains = true;
		}
		return contains;
	}
	
	private int typeToBitDepth(String type) {
		int depth = 0;
		if (type.equals("16-bit")) depth=16;
		else if (type.equals("32-bit")) depth=32;
		else if (type.equals("RGB")) depth=24;
		return depth;
	}

	private String bitDepthToType(int bitDepth) {
		switch (bitDepth) {
			case 0: return types[0];
			case 16: return types[1];
			case 32: return types[2];
			case 24: return types[3];
		}
		return types[0];
	}

	/** Removes names that start with "." or end with ".db", ".txt", ".lut", "roi", ".pty", ".hdr", ".py", etc. */
	public String[] trimFileList(String[] rawlist) {
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if (name.startsWith(".")||name.equals("Thumbs.db")||excludedFileType(name))
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;
		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}
	
	/* Returns true if 'name' ends with ".txt", ".lut", ".roi", ".pty", ".hdr", ".java", ".ijm", ".py", ".js" or ".bsh. */
	public static boolean excludedFileType(String name) {
		if (name==null) return true;
		for (int i=0; i<excludedTypes.length; i++) {
			if (name.endsWith(excludedTypes[i]))
				return true;
		}
		return false;
	}
			
	public void openAsVirtualStack(boolean b) {
		openAsVirtualStack = b;
	}
	
	public void sortFileNames(boolean b) {
		sortFileNames = b;
	}
	
	public void sortByMetaData(boolean b) {
		sortByMetaData = b;
	}

	public void setDirectory(String path) {
		directory = path;
		directorySet = true;
	}

	/** Sorts file names containing numerical components.
	* @see ij.util.StringSorter#sortNumerically
	* Author: Norbert Vischer
	*/
	public String[] sortFileList(String[] list) {
		return StringSorter.sortNumerically(list);
	}

	/**
	 * @return first = file count, second = size in MB
	 */
	@AstroImageJ(reason = "Obtain filtered file count")
	private Pair.IntFloatPair getFileCount(GenericDialog gd) {
		var filter = ((TextField) gd.getStringFields().get(1)).getText();
		if (legacyRegex!=null) filter = "("+legacyRegex+")";

		var directory = this.directory;
		directory = ((TextField) gd.getStringFields().get(0)).getText();
		File file = new File(directory);
		String[] list = file.list();
		// Zip as folder
		ZipOpenerUtil.InternalZipFile[] zipEntries = null;
		if (list == null) {
			zipEntries = ZipOpenerUtil.getFilesInZip(directory);
			list = ZipOpenerUtil.InternalZipFile.getPaths(zipEntries);
			if (list.length == 0) list = null;
		} // End zip as folder

		if (list==null) {
			String parent = file.getParent();
			file = new File(parent);
			list = file.list();
			if (list!=null)
				directory = parent;
			else {
				return Pair.IntFloatPair.empty();
			}
		}

		//remove subdirectories from list
		ArrayList<String> fileList = new ArrayList<>();
		for (String s : list) {
			File f = (new File(directory + s));
			if (!f.isDirectory())
				fileList.add(s);
		}

		if (fileList.size()<list.length)
			list = fileList.toArray(new String[fileList.size()]);

		list = trimFileList(list);
		if (list==null) return Pair.IntFloatPair.empty();

		// Dynamic filter does not support legacy regex
		list = getFilteredList(list, filter, null);
		if (list == null) return Pair.IntFloatPair.empty();

		// Get file sizes by locating a prototypical image file manually calculating its byte size in memory
		var width = 0;
		var height = 0;
		var bitDepth = 0;
		long sizeInBytes = 0;
		var stackCountPerImage = 1;
		for (String sf : list) {
			Opener opener = new Opener();
			opener.setSilentMode(true);
			IJ.redirectErrorMessages(true);
			ImagePlus imp = opener.openImage(directory, sf);
			IJ.redirectErrorMessages(false);

			if (imp != null) {
				width = imp.getWidth();
				height = imp.getHeight();
				bitDepth = imp.getBytesPerPixel();
				stackCountPerImage = imp.getImageStackSize();
				break;
			}
		}

		var increment = safeParse(((TextField) gd.getNumericFields().get(1)).getText(), 1);
		var count = Math.min(safeParse(((TextField) gd.getStringFields().get(2)).getText(), list.length), list.length);
		var start = safeParse(((TextField) gd.getNumericFields().get(0)).getText(), 1);

		sizeInBytes = (long) bitDepth * width * height * ((long) ((count - start + 1) / increment) * stackCountPerImage);

		return new Pair.IntFloatPair(list.length, sizeInBytes / 1_000_000F);
	}

	@AstroImageJ(reason = "Don't error")
	private static int safeParse(String s, int fallback) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ignored) {}
		return fallback;
	}

} // FolderOpener


