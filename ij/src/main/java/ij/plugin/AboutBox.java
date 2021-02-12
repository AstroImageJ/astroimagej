package ij.plugin;
import ij.*;
import ij.astro.AstroImageJ;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;
import java.net.URL;
import java.awt.image.*;

	/** This plugin implements the Help/About AstroImageJ command by opening
	 * about.jpg in ij.jar, scaling it 600% and adding text to an overlay.
	*/
	public class AboutBox implements PlugIn {
		private static final int SMALL_FONT=20, LARGE_FONT=45;
		private static final Color TEXT_COLOR = new Color(255,255,80);

	@AstroImageJ(modified = true, reason = "Adding AIJ version and authors to about page; fixing class loading issue.")
	public void run(String arg) {
		System.gc();
		int lines = 12;
		String[] text = new String[lines];
		text[0] = "ImageJ "+ImageJ.VERSION+ImageJ.BUILD;
		text[1] = "Wayne Rasband and contributors";
		text[2] = "National Institutes of Health, USA";
		text[3] = IJ.URL;
		text[4] = "Java "+System.getProperty("java.version")+(IJ.is64Bit()?" (64-bit)":" (32-bit)");
		text[5] = IJ.freeMemory();
		text[6] = "ImageJ is in the public domain";
		text[7] = "════════════════════════════════";
		text[8] = "AstroImageJ "+IJ.getAstroVersion();
		text[9] = "Karen Collins, John Kielkopf";
		text[10] = "University of Louisville";
		text[11] = "AstroImageJ is Licensed under GPL 3.0";
		ImageProcessor ip = null;
		ImageJ ij = IJ.getInstance();
		URL url = ij.getClass().getClassLoader().getResource("about.jpg");
		if (url!=null) {
			Image img = null;
			try {img = ij.createImage((ImageProducer)url.getContent());}
			catch(Exception e) {}
			if (img!=null) {
				ImagePlus sImp = new ImagePlus("", img);
				ip = sImp.getProcessor();
			}
		}
		if (ip==null) 
			ip =  new ColorProcessor(55,45);
		ip = ip.resize(ip.getWidth()*6, ip.getHeight()*6);
		ImagePlus imp = new ImagePlus("About AstroImageJ", ip);
		int width = imp.getWidth();
		Overlay overlay = new Overlay();
		Font font = new Font("SansSerif", Font.PLAIN, LARGE_FONT);
		int y  = 60;
		add(text[0], width-20, y, font, TextRoi.RIGHT, overlay);
		int xcenter = 410;
		font = new Font("SansSerif", Font.PLAIN, SMALL_FONT);
		y += 45;
		add(text[1], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[2], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[3], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[4], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[5], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[6], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[7], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[8], xcenter, y, font, TextRoi.CENTER, overlay);
		y += 27;
		add(text[9], xcenter, y, font, TextRoi.CENTER, overlay);
		if (IJ.maxMemory()>0L) {
			y += 27;
			add(text[10], xcenter, y, font, TextRoi.CENTER, overlay);
		}
		add(text[11], width-10, ip.getHeight()-10, font, TextRoi.RIGHT, overlay);
		imp.setOverlay(overlay);
		ImageWindow.centerNextImage();
		imp.show();
	}
	
	private void add(String text, int x, int y, Font font, int justification, Overlay overlay) {
		TextRoi roi = new TextRoi(text, x, y, font);
		roi.setStrokeColor(TEXT_COLOR);
		roi.setJustification(justification);
		overlay.add(roi);
	}

}
