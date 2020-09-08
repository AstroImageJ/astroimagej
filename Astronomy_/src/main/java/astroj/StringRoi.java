// StringRoi.java

package astroj;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class StringRoi extends Roi
	{
	protected int xSave;
	protected int ySave;
	protected String theText;
	protected Font font = null;

	public StringRoi (int x, int y, String text)
		{
		super (x,y,1,1);

		xSave = x;		// Roi CLASS HAS PRIVATE COORDINATES
		ySave = y;
		font = new Font ("SansSerif", Font.PLAIN, 12);
		theText = text;
		}

	public void draw (Graphics g)
		{
		g.setColor (Color.RED);

		int sx = ic.screenX(xSave);
		int sy = ic.screenY(ySave);
		
		FontMetrics metrics = g.getFontMetrics (font);
		int h = metrics.getHeight();
		int w = metrics.stringWidth(theText)+3;
		int descent = metrics.getDescent();
		g.setFont (font);

//		Rectangle r = g.getClipBounds();
//		g.setClip (sx,sy,w,h);

		g.drawString (theText, sx, sy+h/2);	// -descent);

//		if (r != null)
//			g.setClip (r.x, r.y, r.width, r.height);
		}
	}
