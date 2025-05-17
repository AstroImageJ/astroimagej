package ij.plugin;

import ij.IJ;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.io.ConfigHandler;
import ij.gui.GenericDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/** This plugin implements the Edit/Options/Memory command. */
@AstroImageJ(reason = "Update for AIJ config file and defaults", modified = true)
public class Memory implements PlugIn {
	String s;
	int index1, index2;
	File f;
	boolean fileMissing;
	boolean sixtyFourBit;

	public void run(String arg) {
		getMemorySetting();
		changeMemoryAllocation();
		//IJ.log("setting="+getMemorySetting()/(1024*1024)+"MB");
		//IJ.log("maxMemory="+maxMemory()/(1024*1024)+"MB");
	}

	@AstroImageJ(reason = "Allow setting of memory when the option isn't present; Support jpackage cfg", modified = true)
	void changeMemoryAllocation() {
		IJ.maxMemory(); // forces IJ to cache old limit
		int max = (int)(getMemorySetting()/1048576L);
		boolean unableToSet = max==0;
		if (max==0) max = (int)(maxMemory()/1048576L);
		String title = "Memory "+(IJ.is64Bit()?"(64-bit)":"(32-bit)");
		GenericDialog gd = new GenericDialog(title);
		gd.addNumericField("Maximum memory:", max, 0, 6, "MB");
		gd.addNumericField("Parallel threads:", Prefs.getThreads(), 0, 6, "");
		gd.setInsets(12, 0, 0);
		gd.addCheckbox("Keep multiple undo buffers", Prefs.keepUndoBuffers);
		gd.setInsets(12, 0, 0);
		gd.addCheckbox("Run garbage collector on status bar click", !Prefs.noClickToGC);
		gd.addHelp(IJ.URL2+"/docs/menus/edit.html#memory");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		int max2 = (int)gd.getNextNumber();
		Prefs.setThreads((int)gd.getNextNumber());
		Prefs.keepUndoBuffers = gd.getNextBoolean();
		Prefs.noClickToGC = !gd.getNextBoolean();
		if (gd.invalidNumber()) {
			IJ.showMessage("Memory", "The number entered was invalid.");
			return;
		}
		/*if (unableToSet && max2!=max)
			{showError(); return;}*/
		if (max2<256 && IJ.isMacOSX()) max2 = 256;
		if (max2<32 && IJ.isWindows()) max2 = 32;
		if (max2==max) return;
		int limit = IJ.isWindows()?1600:1700;
		String OSXInfo = "";
		if (IJ.isMacOSX())
			OSXInfo = "\n \nOn Max OS X, use\n"
				+"/Applications/Utilities/Java/Java Preferences\n"
				+"to switch to a 64-bit version of Java. You may\n"
				+"also need to run \"AstroImageJ64\" instead of \"AstroImageJ\".";
		if (max2>=limit && !IJ.is64Bit()) {
			if (!IJ.showMessageWithCancel(title, 
			"Note: setting the memory limit to a value\n"
			+"greater than "+limit+"MB on a 32-bit system\n"
			+"may cause AstroImageJ to fail to start. The title of\n"
			+"the Edit>Options>Memory & Threads dialog\n"
			+"box changes to \"Memory (64-bit)\" when AstroImageJ\n"
			+"is running on a 64-bit version of Java."
			+ OSXInfo));
				return;
		}

		// Ensure that the file is writable
		if (!f.canWrite()) {
			try {
				f.setWritable(true, true);
			} catch (SecurityException e) {
				IJ.log("Could not make " + f.toString() + " writable due to permissions.");
			}
		}

		if (IJ.isMacOSX() && !f.canWrite()) {
			try {
				Process process = Runtime.getRuntime().exec(new String[]{"chmod", "+xrw", f.getAbsolutePath()});
				process.waitFor();
			} catch (InterruptedException | IOException e) {
				IJ.error(e.getMessage());
			}
		}

		int newMax = max2;
		ConfigHandler.modifyOptions(lines -> {
			ConfigHandler.setOption(lines, "JavaOptions", "java-options=-Xmx", "java-options=-Xmx" + newMax + "m");
		});

		String hint = "";
		String[] versionPieces = IJ.getAstroVersion().split("\\.");
		int majorVersion = Integer.parseInt(versionPieces[0]);
		if (IJ.isWindows() && max2>640 && max2>max)
			hint = String.format("\nIf AstroImageJ fails to start, delete the values under the [JavaOptions] section in " +
							"the \"%s\" file located in the AstroImageJ folder.",
					majorVersion > 4 ? getFileName() : "AstroImageJ.cfg");
		IJ.showMessage("Memory", "The new " + max2 +"MB limit will take effect after AstroImageJ is restarted."+hint);
	}
	
	public long getMemorySetting() {
		if (IJ.getApplet()!=null) return 0L;
		long max = 0L;

        // As of 5.0.0.0, AIJ uses a unified file for memory settings
		max = getMemorySetting(getFileName());
		return max;
	}

	void showError() {
		int max = (int)(maxMemory()/1048576L);
		String msg =
			   "AstroImageJ is unable to change the memory limit. For \n"
			+ "more information, refer to the installation notes at\n \n"
			+ "    "+IJ.URL2+"/docs/install/\n"
			+ " \n";
		if (fileMissing) {
			if (IJ.isMacOSX())
				msg += "The AstroImageJ application (AstroImageJ.app) was not found.\n \n";
			else if (IJ.isWindows())
				msg += getFileName() + " not found.\n \n";
			fileMissing = false;
		}
		if (max>0)
			msg += "Current limit: " + max + "MB";
		IJ.showMessage("Memory", msg);
	}

	@AstroImageJ(reason = "Read JPackage config file", modified = true)
	long getMemorySetting(String file) {
		String path = Prefs.getHomeDir()+File.separator+file;
		f = new File(path);
		if (!f.exists()) {
			fileMissing = true;
			return 0L;
		}

		long max = 0L;
		try {
			var maxMem = ConfigHandler.findValue(ConfigHandler.readOptions(), "JavaOptions", "java-options=-Xmx");
            if (maxMem != null) {
                maxMem = maxMem.trim();

				var unit = maxMem.charAt(maxMem.length()-1);
				var factor = 1L;

				if (!Character.isDigit(unit)) {
					factor = switch (unit) {
						case 'g', 'G' -> 1024L * 1024L * 1024L;
						case 'm', 'M' -> 1024L * 1024L;
						case 'k', 'K' -> 1024L;
						default -> {
							IJ.error("Memory", "Attempted to read invalid java option for memory! " + maxMem);
							throw new IllegalStateException("Invalid format");
						}
					};
				}

                max = factor * Integer.parseInt(maxMem.substring(0, maxMem.length() - (factor == 1 ? 0 : 1)));
            }
		}
		catch (Exception e) {
			IJ.log(""+e);
			return 0L;
		}
		return max;
	}

	/** Returns the maximum amount of memory this JVM will attempt to use. */
	public long maxMemory() {
			return Runtime.getRuntime().maxMemory();
	}

	@AstroImageJ(reason = "Get file name for config")
	private String getFileName() {
		if (Files.exists(Path.of("AstroImageJ.l4j.ini"))) {
			return "AstroImageJ.l4j.ini";
		}

		return "AstroImageJ.cfg";
	}
}
