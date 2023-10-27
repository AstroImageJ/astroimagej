package ij.plugin;

import ij.IJ;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.gui.GenericDialog;
import ij.util.Tools;

import java.io.*;
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

	@AstroImageJ(reason = "Allow setting of memory when the option isn't present", modified = true)
	void changeMemoryAllocation() {
		IJ.maxMemory(); // forces IJ to cache old limit
		int max = (int)(getMemorySetting()/1048576L);
		boolean unableToSet = max==0;
		if (max==0) max = (int)(maxMemory()/1048576L);
		String title = "Memory "+(IJ.is64Bit()?"(64-bit)":"(32-bit)");
		GenericDialog gd = new GenericDialog(title);
		gd.addNumericField("Maximum memory:", max, 0, 5, "MB");
		gd.addNumericField("Parallel threads for stacks:", Prefs.getThreads(), 0, 5, "");
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

		try {
			String s2 = s.substring(index2);
			if (s2.startsWith("g"))
				s2 = "m"+s2.substring(1);
			String s3 = unableToSet ? "-Xmx" + max2 + s2 : s.substring(0, index1) + max2 + s2;
			FileOutputStream fos = new FileOutputStream(f);
			PrintWriter pw = new PrintWriter(fos);
			pw.print(s3);
			pw.close();
		} catch (IOException e) {
			String error = e.getMessage();
			if (error==null || error.equals("")) error = ""+e;
			String name = getFileName();
			String msg = 
				   "Unable to update the file \"" + name + "\".\n"
				+ " \n"
				+ "\"" + error + "\"";
			IJ.showMessage("Memory", msg);
			return;
		}
		String hint = "";
		String[] versionPieces = IJ.getAstroVersion().split("\\.");
		int majorVersion = Integer.parseInt(versionPieces[0]);
		if (IJ.isWindows() && max2>640 && max2>max)
			hint = String.format("\nIf AstroImageJ fails to start, delete the \"%s\" file located in the AstroImageJ folder.",
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

	long getMemorySetting(String file) {
		String path = Prefs.getHomeDir()+File.separator+file;
		f = new File(path);
		if (!f.exists()) {
			fileMissing = true;
			return 0L;
		}

		long max = 0L;
		try {
			int size = (int)f.length();
			byte[] buffer = new byte[size];
			FileInputStream in = new FileInputStream(f);
			in.read(buffer, 0, size);
			s = new String(buffer, 0, size, "ISO8859_1");
			in.close();
			index1 = s.indexOf("-mx");
			if (index1==-1) index1 = s.indexOf("-Xmx");
			if (index1==-1) return 0L;
			if (s.charAt(index1+1)=='X') index1+=4; else index1+=3;
			index2 = index1;
			while (index2<s.length()-1 && Character.isDigit(s.charAt(++index2))) {}
			String s2 = s.substring(index1, index2);
			max = (long)Tools.parseDouble(s2, 0.0)*1024*1024;
			if (index2<s.length() && s.charAt(index2)=='g')
				max = max*1024L;
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
