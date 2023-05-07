package ij.io;

import ij.IJ;
import ij.Macro;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.astro.util.AIJFileChooser;
import ij.plugin.frame.Recorder;
import ij.util.Java2;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** This class displays a dialog box that allows the user can select a directory. */ 
 public class DirectoryChooser {
 	private String directory;
 	private String title;
	private final Container parent;

	/**
     * Display a dialog using the specified title.
     */
	@AstroImageJ(reason = "Overload to maintain API")
 	public DirectoryChooser(String title) {
		this(title, null);
	}

	/** Display a dialog using the specified title. */
	@AstroImageJ(reason = "Add parent", modified = true)
 	public DirectoryChooser(String title, Container parent) {
 		this.title = title;
		this.parent = parent;
		if (IJ.isMacOSX() && !Prefs.useJFileChooser)
			getDirectoryUsingFileDialog(title);
 		else {
			String macroOptions = Macro.getOptions();
			if (macroOptions!=null)
				directory = Macro.getValue(macroOptions, title, null);
			if (directory==null) {
 				if (EventQueue.isDispatchThread())
 					getDirectoryUsingJFileChooserOnThisThread(title);
 				else
 					getDirectoryUsingJFileChooser(title);
 			}
 		}
 	}
 	
	// runs JFileChooser on event dispatch thread to avoid possible thread deadlocks
	@AstroImageJ(reason = "Don't duplicate implementation", modified = true)
 	void getDirectoryUsingJFileChooser(final String title) {
 		LookAndFeel saveLookAndFeel = Java2.getLookAndFeel();
		Java2.setSystemLookAndFeel();
		try {
			EventQueue.invokeAndWait(() -> getDirectoryUsingJFileChooserOnThisThread(title));
		} catch (Exception e) {}
		Java2.setLookAndFeel(saveLookAndFeel);
	}
 
	// Choose a directory using JFileChooser on the current thread
	@AstroImageJ(reason = "make use of parent; allow dialog to be on different screens", modified = true)
 	void getDirectoryUsingJFileChooserOnThisThread(final String title) {
		LookAndFeel saveLookAndFeel = Java2.getLookAndFeel();
		Java2.setSystemLookAndFeel();
		try {
			AIJFileChooser chooser = new AIJFileChooser();
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDragEnabled(true);
			chooser.setTransferHandler(new DragAndDropHandler(chooser));
			String defaultDir = OpenDialog.getDefaultDirectory();
			if (defaultDir!=null) {
				File f = new File(defaultDir);
				if (IJ.debugMode)
					IJ.log("DirectoryChooser,setSelectedFile: "+f);
				chooser.setSelectedFile(f);
			}
			chooser.setApproveButtonText("Select");
			if (chooser.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				directory = file.getAbsolutePath();
				directory = IJ.addSeparator(directory);
				OpenDialog.setDefaultDirectory(directory);
			}
		} catch (Exception e) {}
		Java2.setLookAndFeel(saveLookAndFeel);
	}

 	// On Mac OS X, we can select directories using the native file open dialog
 	void getDirectoryUsingFileDialog(String title) {
 		boolean saveUseJFC = Prefs.useJFileChooser;
 		Prefs.useJFileChooser = false;
		System.setProperty("apple.awt.fileDialogForDirectories", "true");
		String dir=null, name=null;
		String defaultDir = OpenDialog.getDefaultDirectory();
		if (defaultDir!=null) {
			File f = new File(defaultDir);
			dir = f.getParent();
			name = f.getName();
		}
		if (IJ.debugMode)
			IJ.log("DirectoryChooser: dir=\""+dir+"\",  file=\""+name+"\"");
		OpenDialog od = new OpenDialog(title, dir, null);
		String odDir = od.getDirectory();
		if (odDir==null)
			directory = null;
		else {
			directory = odDir + od.getFileName() + "/";
			OpenDialog.setDefaultDirectory(directory);
		}
		System.setProperty("apple.awt.fileDialogForDirectories", "false");
 		Prefs.useJFileChooser = saveUseJFC;
	}

 	/** Returns the directory selected by the user. */
 	public String getDirectory() {
		if (IJ.debugMode)
			IJ.log("DirectoryChooser.getDirectory: "+directory);
		if (Recorder.record && !IJ.isMacOSX())
			Recorder.recordPath(title, directory);
 		return directory;
 	}
 	
    /** Sets the default directory presented in the dialog. */
    public static void setDefaultDirectory(String dir) {
    	if (dir==null || (new File(dir)).isDirectory())
			OpenDialog.setDefaultDirectory(dir);
    }

}
