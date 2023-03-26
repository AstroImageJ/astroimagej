// FitsJ.java

package astroj;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.util.Tools;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

/**
 * A collection of helpful static methods to read, manipulate, write, and query FITS-based images within ImageJ.
 * The header is manipulated as an array of Strings which can be read and written via the "Info" property
 * (a Properties object wouldn't keep the cards in the right order and can't be used with Stacks).
 */
public class FitsJ
	{
	static public int 	NO_CARD = -1;		// TYPES OF FITS CARDS
	static public int 	STRING_CARD = 0;
	static public int 	INTEGER_CARD = 1;
	static public int	DOUBLE_CARD = 2;
	static public int	BOOLEAN_CARD = 3;
	static public int 	COMMENT_CARD = 4;
	static public int 	HISTORY_CARD = 5;

	public static int 	KEY_PART = 0;		// PARTS OF A CARD PARSE BY THE cardParts() METHOD
	public static int 	STRING_PART= 1;
	public static int 	DOUBLE_PART = 2;
	public static int 	INTEGER_PART = 3;
	public static int 	BOOLEAN_PART = 4;
	public static int 	COMMENT_PART = 5;
	public static int 	TYPE_PART = 6;

		/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the slice label in the case of an ImageStack)
	 * and returns it as a String object representing all card.
	 *
 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public static String getHeaderString(ImagePlus img) {
		return getHeaderString(img, img.getCurrentSlice());
	}

		/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the slice label in the case of an ImageStack)
	 * and returns it as a String object representing all card.
	 *
	 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public static String getHeaderString(ImagePlus img, int slice)
		{
		String content = null;

		int depth = img.getStackSize();
		if (depth == 1)
			{
			Properties props = img.getProperties();
			if (props == null)
				return null;
			content = (String)props.getProperty ("Info");
			}
		else if (depth > 1)
			{
			ImageStack stack = img.getStack();
			content = stack.getSliceLabel(slice);
            if (content == null)
                {
                Properties props = img.getProperties();
                if (props == null)
                    return null;
                content = (String)props.getProperty ("Info");  
                }
			}
		return content;
		}

		/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the slice label in the case of an ImageStack)
	 * and returns it as an array of String objects representing each card.
	 *
 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public static String[] getHeader(ImagePlus img) {
		return getHeader(img, img.getCurrentSlice());
	}

		/**
	 * Extracts the original FITS header from the Properties object of the
	 * ImagePlus image (or from the slice label in the case of an ImageStack)
	 * and returns it as an array of String objects representing each card.
	 *
	 * @param img		The ImagePlus image which has the FITS header in it's "Info" property.
	 */
	public static String[] getHeader(ImagePlus img, int slice)
		{
		String content = getHeaderString(img, slice);
		if (content == null) return null;
		// PARSE INTO LINES

		String[] lines = content.split("\n");

		// FIND "SIMPLE" AND "END" KEYWORDS

		int istart = 0;
		for (; istart < lines.length; istart++)
			{
			if (lines[istart].trim().startsWith("SIMPLE") ) break;
			}
		if (istart == lines.length) return null;

		int iend = istart+1;
		for (; iend < lines.length; iend++)
			{
			String s = lines[iend].trim();
			if ( s.equals("END") || s.startsWith ("END ") ) break;
			}
		if (iend >= lines.length) return null;
		int l = iend-istart+1;
		String header = "";
		for (int i=0; i < l; i++)
			header += lines[istart+i]+"\n";
		return header.split("\n");
		}

	/**
	 * Checks to make sure the FITS header agrees with the ImageJ image parameters.
	 *
	 * @param img			The ImagePlus image.
	 * @param hdr			The FITS header stored in a String array.
	 */
	public static boolean isConsistent (ImagePlus img, String[] hdr)
		{
		int nx = img.getWidth();
		int ny = img.getHeight();
		int nz = img.getStackSize();
		int n = 2;
		if (nz > 1) n++;
		int naxis=0;
		int naxis1=0;
		int naxis2=0;
		int naxis3=0;
		try	{
			naxis  = FitsJ.findIntValue ("NAXIS",hdr);
			naxis1 = FitsJ.findIntValue ("NAXIS1",hdr);
			naxis2 = FitsJ.findIntValue ("NAXIS2",hdr);
			if (nz > 1) naxis3 = FitsJ.findIntValue ("NAXIS3",hdr);
			}
		catch (NumberFormatException e)
			{
			return false;
			}
		if (n != naxis) return false;
		if (nx != naxis1) return false;
		if (ny != naxis2) return false;
		if (nz > 1 && nz != naxis3) return false;
		return true;
		}

	/**
	 * Insures that the FITS header agrees with the ImageJ image parameters.
	 *
	 * @param img			The ImagePlus image.
	 * @param hdr			The FITS header stored in a String array.
	 */
	public static void makeConsistent (ImagePlus img, String[] hdr)
		{
		int nx = img.getWidth();
		int ny = img.getHeight();
		int nz = img.getStackSize();
		int n = 2;
		if (nz > 1) n++;
		int naxis=0;
		int naxis1=0;
		int naxis2=0;
		int naxis3=0;
		try	{
			naxis  = FitsJ.findIntValue ("NAXIS",hdr);
			naxis1 = FitsJ.findIntValue ("NAXIS1",hdr);
			naxis2 = FitsJ.findIntValue ("NAXIS2",hdr);
			if (nz > 1) naxis3 = FitsJ.findIntValue ("NAXIS3",hdr);
			}
		catch (NumberFormatException e)
			{
			IJ.beep(); IJ.beep(); IJ.beep();
			IJ.log("Error making FITS image header consistent with ImageJ image properties!");
			return;
			}
		FitsJ.setCard ("NAXIS",n,null,hdr);
		FitsJ.setCard ("NAXIS1",nx,null,hdr);
		FitsJ.setCard ("NAXIS2",ny,null,hdr);
		if (nz > 1)
			FitsJ.setCard ("NAXIS3",nz,null,hdr);
		}

	/**
	 * Sets a FITS card with an integer keyword
	 *
	 * @param key			The name of the FITS keyword (e.g. "NAXIS3").
	 * @param property		The integer value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an array of Strings.
	 */
	public static String[] setCard (String key, int property, String comment, String[] cards)
		{
		return set (key,""+property,comment, cards);
		}

	/**
	 * Sets a FITS card with a double keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "JD").
	 * @param property		The double value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an array of Strings.
	 */
	public static String[] setCard (String key, double property, String comment, String[] cards)
		{
		return set (key,""+property,comment, cards);
		}

	/**
	 * Sets a FITS card with a boolean keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "EXTENDED").
	 * @param property		The boolean value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an array of Strings.
	 */
	public static String[] setCard (String key, boolean property, String comment, String[] cards)
		{
		if (property)
			return set (key,"T       ",comment, cards);
		else
			return set (key,"F       ",comment, cards);
		}

	/**
	 * Sets a FITS card with a string keyword.
	 *
	 * @param key			The name of the FITS keyword (e.g. "DATE-OBS").
	 * @param property		The string value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an array of Strings.
	 */
	public static String[] setCard (String key, String property, String comment, String[] cards)
		{
		return set (key,"'"+property+"'",comment, cards);
		}

	/**
	 * Creates a FITS card in the form of a String.
	 *
	 * @param key			The name of the FITS keyword.
	 * @param val		The value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 */
	public static String createCard (String key, String val, String comment)
		{
		String k = key.trim();

		String sp = "";
		for (int l=k.length(); l < 8; l++)
			sp += " ";
		String card = k+sp+"= "+val;
		if (comment != null)
			card += " / "+comment.trim();
		return card;
		}

	/**
	 * Sets a FITS card in the form of a String to an ImagePlus image.
	 *
	 * @param key			The name of the FITS keyword.
	 * @param val		The value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an Array of strings.
	 */
	protected static String[] set (String key, String val, String comment, String[] cards)
		{
		if (cards == null) return null;
		String k = key.trim();
		String card = null;

		// GET OLD VALUE AND COMMENT FROM CARD CONTAINING KEYWORD

		int icard = findCardWithKey (k,cards);
		if (icard >= 0)
			card = cards[icard];
		String old = getCardValue(card);
		String comm = getCardComment (card);

		// IF THERE'S A NEW OR OLD COMMENT, USE IT

		String sp = "";
		for (int l=k.length(); l < 8; l++)
			sp += " ";
		String v = k+sp+"= "+val;
		if (comment != null)
			v += " / "+comment.trim();
		else if (comm != null)
			v += " / "+comm;

		// SAVE NEW HEADER

		if (icard >= 0)
			cards[icard] = v;
		else
			cards = addCard (v,cards);
		return cards;
		}
    
	/**
	 * adds a new ANNOTATE card in the form of a String to an ImagePlus image (multiple ANNOTATE keys could exist in header)
	 *
	 * @param val    		The value corresponding to this keyword.
	 * @param comment		The FITS comment string for this keyword.
	 * @param cards			The FITS header as an Array of strings.
	 */
	protected static String[] addAnnotateCard (String val, String comment, String[] cards)
		{
		if (cards == null) cards = new String[1];
		String k = "ANNOTATE";

		String v = k+"= "+val;
		if (comment != null && comment.length() > 0)
            {
            v += " / ";
            char[] c = comment.trim().toCharArray();
            for (int i=0; i<c.length; i++)
                {
                if (c[i]>=32 && c[i]<=126) v += c[i];
                else if (c[i]==0x0391) v +="ALPHA";
                else if (c[i]==0x0392) v +="BETA";
                else if (c[i]==0x0393) v +="GAMMA";
                else if (c[i]==0x0394) v +="DELTA";
                else if (c[i]==0x0395) v +="EPSILON";
                else if (c[i]==0x0396) v +="ZETA";
                else if (c[i]==0x0397) v +="ETA";
                else if (c[i]==0x0398) v +="THETA";
                else if (c[i]==0x0399) v +="IOTA";
                else if (c[i]==0x039A) v +="KAPPA";
                else if (c[i]==0x039B) v +="LAMDA";
                else if (c[i]==0x039C) v +="MU";
                else if (c[i]==0x039D) v +="NU";
                else if (c[i]==0x039E) v +="XI";
                else if (c[i]==0x039F) v +="OMICRON";
                else if (c[i]==0x03A0) v +="PI";
                else if (c[i]==0x03A1) v +="RHO";
                else if (c[i]==0x03A3) v +="TAU";
                else if (c[i]==0x03A4) v +="UPSILON";
                else if (c[i]==0x03A6) v +="PHI";
                else if (c[i]==0x03A7) v +="CHI";
                else if (c[i]==0x03A8) v +="PSI";
                else if (c[i]==0x03A9) v +="OMEGA";
                else if (c[i]==0x03B1) v +="alpha";
                else if (c[i]==0x03B2) v +="beta";
                else if (c[i]==0x03B3) v +="gamma";
                else if (c[i]==0x03B4) v +="delta";
                else if (c[i]==0x03B5) v +="epsilon";
                else if (c[i]==0x03B6) v +="zeta";
                else if (c[i]==0x03B7) v +="eta";
                else if (c[i]==0x03B8) v +="theta";
                else if (c[i]==0x03B9) v +="iota";
                else if (c[i]==0x03BA) v +="kappa";
                else if (c[i]==0x03BB) v +="lambda";
                else if (c[i]==0x03BC) v +="mu";
                else if (c[i]==0x03BD) v +="nu";
                else if (c[i]==0x03BE) v +="xi";
                else if (c[i]==0x03BF) v +="omicron";
                else if (c[i]==0x03C0) v +="pi";
                else if (c[i]==0x03C1) v +="rho";
                else if (c[i]==0x03C2) v +="stigma";
                else if (c[i]==0x03C3) v +="sigma";
                else if (c[i]==0x03C4) v +="tau";
                else if (c[i]==0x03C5) v +="upsilon";
                else if (c[i]==0x03C6) v +="phi";
                else if (c[i]==0x03C7) v +="chi";
                else if (c[i]==0x03C8) v +="psi";
                else if (c[i]==0x03C9) v +="omega";
                else if (c[i]==0x03D5) v +="PHI";
                else v +="?";
                }
            }
    	cards = addCard (v, cards);
		return cards;
		}  
 
    
	/**
	 * Updates the ANNOTATE card that contains the specified x,y location within the x,y,radius stored in a String array.
     * If no matching card is found, a new card is created.
	 *
     * @param x         The x pixel location of the annotation
     * @param y         The y pixel location of the annotation
     * @param val    	The new value corresponding to this keyword.
	 * @param comment	The new comment string for this keyword.
	 * @param key		A FITS card keyword for those cards to be removed from the FITS header String array.
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] setAnnotateCard (double x, double y, String val, String comment, String[] cards)
		{
		int l = cards.length;
        int i;
        String cardkey = null;
		for (i=0; i < l; i++)
			{
            cardkey = getCardKey(cards[i]);
			if (cardkey != null && cardkey.equals("ANNOTATE"))
                {
                String[] pieces = getCardStringValue(cards[i]).split(",");
                if (pieces.length > 2)
                    {
                    double xx = Tools.parseDouble(pieces[0]);
                    double yy = Tools.parseDouble(pieces[1]);
                    double rr = Tools.parseDouble(pieces[2]);  
                    if (!Double.isNaN(xx) && !Double.isNaN(yy) && !Double.isNaN(rr) && (x-xx)*(x-xx) + (y-yy)*(y-yy) < rr * rr)
                        {
                        String k = "ANNOTATE";
                		String v = k+"= "+val;
                        if (comment != null)
                            v += " / "+comment.trim();
                        cards[i] = v;
                        return cards;
                        }
                    }
                }
			}
		return addAnnotateCard(val, comment, cards);
		}

	/**
	 * Removes first ANNOTATE card that contains the specified x,y location within the x,y,radius stored in a String array.
	 *
	 * @param key		A FITS card keyword for those cards to be removed from the FITS header String array.
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] removeAnnotateCard (double x, double y, String[] cards)
		{
		int l = cards.length;
        int i;
        String cardkey = null;
		for (i=0; i < l; i++)
			{
            cardkey = getCardKey(cards[i]);
			if (cardkey != null && cardkey.equals("ANNOTATE"))
                {
                String[] pieces = getCardStringValue(cards[i]).split(",");
                if (pieces.length >2)
                    {
                    double xx = Tools.parseDouble(pieces[0]);
                    double yy = Tools.parseDouble(pieces[1]);
                    double rr = Tools.parseDouble(pieces[2]);  
                    if (!Double.isNaN(xx) && !Double.isNaN(yy) && !Double.isNaN(rr) && (x-xx)*(x-xx) + (y-yy)*(y-yy) < rr * rr)
                        {
                        String[] hdr = new String[l-1];
                        int cnt = 0;
                        for (int j=0; j < l; j++)
                            {
                            if (j != i)
                                {
                                hdr[cnt] = new String(cards[j]);
                                cnt++;
                                }
                            }
                        return hdr;
                        }
                    }
                }
			}
		return cards;
		}    
    
	/**
	 * Removes first ANNOTATE card that contains the specified x,y location within the x,y,radius stored in a String array.
	 *
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] removeAstrometryAnnotateCards (String[] cards)
		{
		int l = cards.length;
        String cardkey = null;
        int cnt = 0;
		for (int i=0; i < l; i++)
			{
            cardkey = getCardKey(cards[i]);
			if (cardkey != null && cardkey.equals("ANNOTATE"))
                {
                String[] pieces = getCardStringValue(cards[i]).split(",");
                if (pieces.length > 6 && pieces[6].equals("1"))
                    {
                     cnt++;
                    }
                }
			}
        if (cnt > 0)
            {
            String[] hdr = new String[l-cnt];
            cnt = 0;
            for (int i=0; i < l; i++)
                {
                cardkey = getCardKey(cards[i]);
                if (cardkey != null && cardkey.equals("ANNOTATE"))
                    {
                    String[] pieces = getCardStringValue(cards[i]).split(",");
                    if (!(pieces.length > 6 && pieces[6].equals("1")))
                        {
                        hdr[cnt] = new String(cards[i]);
                        cnt++;
                        }
                    }
                else
                    {
                    hdr[cnt] = new String(cards[i]);
                    cnt++;                    
                    }
                }  
            return hdr;            
            }
        else
            {
            return cards;
            }
		}     
    
    
	/**
	 * Removes all NOTATION cards from the FITS header stored in a String array.
	 *
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] removeAnnotateCards (String[] cards)
		{ 
        return removeCards("ANNOTATE", cards);
        }
   
    
	/**
	 * Saves a FITS header in an array of Strings back into an ImagePlus's "Info" property.
	 */
	public static void putHeader (ImagePlus img, String[] cards)
		{
		String s = unsplit(cards,"\n");
		
		int depth = img.getStackSize();
		if (depth == 1)
			img.setProperty ("Info", s);
		else if (depth > 1)
			{
			int slice = img.getCurrentSlice();
			ImageStack stack = img.getStack();
            String label = stack.getSliceLabel(slice);
            if (label == null)
                {
                img.setProperty ("Info", s);
                }
            else
                {
                int newline = label.indexOf('\n');
                if (newline != -1) label = label.substring(0, newline); 
                label = label.trim();
                stack.setSliceLabel(label+"\n"+s, slice);
                }
			}
		}

	/**
	 * Saves a FITS header in an array of Strings back into an ImageStack's current slice.
	 */
	public static void putHeader (ImageStack stack, String[] cards, int slice)
		{
        if (cards==null) return;
		String s = unsplit(cards,"\n");
		String label = stack.getSliceLabel(slice);
        if (label == null)
            {
            return;
            }
        else
            {        
            int newline = label.indexOf('\n');
            if (newline != -1) label = label.substring(0, newline); 
            label = label.trim();        
            stack.setSliceLabel(label+"\n"+s, slice);
            }
		}

	/**
	 * Adds a FITS card to the FITS header stored in a String array.  The resulting modified header is returned.
	 *
	 * @param card		A FITS card image to be added to the FITS header String array.
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] addCard (String card, String[] cards)
		{
		int l = cards.length;
		String[] hdr = new String[l+1];
		for (int i=0; i < l; i++)
			hdr[i] = new String (cards[i]);
		hdr[l-1] = new String (card);
		hdr[l] = "END";
		return hdr;
		}


	/**
	 * Inserts a blank card at a particular position.
	 *
	 * @param cards			The array of strings containing the FITS header.
	 * @param pos			The card position where a new blank card should be inserted.
	 */
	public static String[] insertBlankCard (String[] cards, int pos)
		{
		int l = cards.length;
		if (pos == l-1 && cards[l-1].equals("END"))
			return addCard ("",cards);

		String[] hdr = new String[l+1];
		for (int i=0; i < pos; i++)
			hdr[i] = new String (cards[i]);
		hdr[pos] = "";
		for (int i=pos+1; i <= l; i++)
			hdr[i] = new String (cards[i-1]);
		return hdr;
		}

	/**
	 * Removes all FITS cards with the given key from the FITS header stored in a String array.
	 *
	 * @param key		A FITS card keyword for those cards to be removed from the FITS header String array.
	 * @param cards		A String array holding the contents of a FITS header.
	 */
	public static String[] removeCards (String key, String[] cards)
		{
		int l = cards.length;
        int num = 0;
        String cardkey = null;
		for (int i=0; i < l; i++)
			{
            cardkey = getCardKey(cards[i]);
			if (cardkey != null && cardkey.equals(key))
                {
				num++;
                }
			}
		String[] hdr = new String[l-num];
        int cnt = 0;
		for (int i=0; i < l; i++)
			{
            cardkey = getCardKey(cards[i]);
			if (cardkey == null || !cardkey.equals(key))
                {
				hdr[cnt] = new String(cards[i]);
                cnt++;
                }
			}
		return hdr;
		}

	/**
	 * Finds the location of a FITS card in a String array having the FITS keyword "key".
	 *
	 * @param header	A String array holding the contents of a FITS header.
	 * @param key		A String containing the FITS keyword to be searched for.
	 */
	public static int findCardWithKey (String key, String[] header)
		{
		if (key == null) return -1;
		if (header == null) return -1;
		int n=header.length;
		String k = key.trim();
		for (int i=0; i < n; i++)
			{
			String[] l = header[i].trim().split("=");
			if (l.length > 0 && l[0].trim().equals(k)) return i;
			}

		return -1;
		}

	/**
	 * Adds a comment to the image's FITS header.  The resulting modified header is returned.
	 *
	 * @param img			The image whose comments are to be extended.
	 * @param comment		The FITS comment string.
	 */
	public static String[] addComment (String comment, String[] cards)
		{
		if (cards == null) return null;

		String[] parts = segmentString(comment,68);
		if (parts.length > 0)
			cards = addCard (pad("COMMENT "+parts[0],80),cards);
		for (int i=1; i < parts.length; i++)
			cards = addCard (pad("COMMENT ... "+parts[i],80),cards);
		return cards;
		}

	/**
	 * Adds a comment to the image's FITS header after a particular position.  The resulting modified header is returned.
	 *
	 * @param img			The image whose comments are to be extended.
	 * @param comment		The FITS comment string.
	 * @param pos			The card number of the previous card.
	 */
	public static String[] addCommentAfter (String comment, String[] cards, int pos)
		{
		if (cards == null || pos < 0 || pos >= cards.length) return null;

		String[] parts = segmentString(comment,68);	// len("COMMENT ... ")+68=80
		String[] hdr = insertBlankCard (cards, pos+1);
		hdr[pos+1] = pad("COMMENT "+parts[0],80);
		for (int i=1; i < parts.length; i++)
			{
			hdr = insertBlankCard (hdr, pos+1+i);
			hdr[pos+1+i] = pad("COMMENT ... "+parts[i],80);
			}
		return hdr;
		}

	/**
	 * Adds a FITS history card to the image's FITS header.  The resulting modified header is returned.
	 *
	 * @param img			The ImagePlus whose history entries are to be extended.
	 * @param comment		The FITS history string.
	 */
	public static String[] addHistory (String history, String[] cards)
		{
		if (cards == null) return null;

		String[] parts = segmentString(history,68);	// len("HISTORY ... ")+68 = 80
		if (parts.length > 0)
			{
			// IJ.log(parts[0]);
			cards = addCard (pad("HISTORY "+parts[0],80),cards);
			}
		for (int i=1; i < parts.length; i++)
			{
			// IJ.log(parts[i]);
			cards = addCard (pad("HISTORY ... "+parts[i],80),cards);
			}
		return cards;
		}

	/**
	 * Tells the type of FITS value in a card:
	 *	"B"=BOOLEAN,
	 *	"C"=COMMENT,
	 *	"E"=END,
	 *	"H"=HISTORY,
	 *	"I"=INTEGER NUMBER,
	 *	"R"=REAL NUMBER,
	 *	"S"=STRING,
	 *	" "=BLANK
	 *	"?"=OTHER
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardType (String card)
		{
		if (card == null) return null;
		int equals = -1;
		if (card.substring(0,card.length() < 8 ? card.length() : 8).trim().equals(""))
			return " ";
		if (card.startsWith("COMMENT"))
			return "C";
		if (card.startsWith("HISTORY"))
			return "H";
		if (card.trim().equals("END"))
			return "E";
		equals = card.indexOf("=");
		if (equals < 0)
			return "?";
		String s = card.substring(equals).trim();
		String val = getCardValue(card).trim();
		if (val.startsWith("'") && val.endsWith("'"))
			return "S";
		if (val.startsWith("\"") && val.endsWith("\""))
			return "S";
		if (val.trim().equals("T") || val.trim().equals("F"))
			return "B";
		if (val.indexOf(".") < 0)
			return "I";
		if (val.indexOf(".") >= 0)
			return "R";
		return "X";
		}

	/**
	 * Extracts the FITS keyword from a card.
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardKey (String card)
		{
		if (card == null) return null;
		int equals = -1;
		if (card.startsWith("COMMENT") || card.startsWith("HISTORY"))
			return null;
		equals = card.indexOf("=");
		if (equals < 0)
			return null;		// NO VALUE (e.g. COMMENT?)
		return card.substring(0,equals).trim();
		}

	/**
	 * Extracts the FITS value for a given keyword from a FITS image with a given name.  This special routine is meant to be used
	 * within ImageJ macros via the
	 *
	 *	exptime = call ("FitsJ.getCardFromImage","EXPTIME","myimage.fits");
	 *
	 * @param key		The FITS keyword.
	 * @param filename	The name of the file used by ImageJ (CAUTION: may be bereft of a file suffix or the name of a stack's directory!).
	 */
	public static String getCardValueFromImage (String key, String image)
		{
		ImagePlus img = null;
		if (image == null || image.equals(""))
			img = WindowManager.getCurrentImage();
		else
			img = WindowManager.getImage(image);
		if (img == null)
			{
			IJ.log("FitsJ.getCardValueFromFile: no image "+image);
			return null;
			}

		String[] cards = FitsJ.getHeader(img);
		if (cards == null)
			{
			IJ.log("FitsJ.getCardValueFromFile: no FITS header for "+image);
			return null;
			}

		int icard = FitsJ.findCardWithKey (key, cards);
		if (icard < 0) return null;

		String ctype = getCardType(cards[icard]);
		if (ctype.equals("S"))
			return getCardStringValue(cards[icard]);
		else
			return getCardValue(cards[icard]);
		}

	/**
	 * Sets the FITS value of a given keyword to an image with a given name.  This special routine is meant to be used
	 * within ImageJ macros via the
	 *
	 *	call ("FitsJ.setCardOfImage","EXPTIME","1.2345","comment","double","myimage.fits");
	 *
	 * @param key		The FITS keyword.
	 * @param filename	The name of the file used by ImageJ (CAUTION: may be bereft of a file suffix or the name of a stack's directory!).
	 */
	public static void setCardOfImage (String key, String val, String comment, String typ, String image)
		{
		ImagePlus img = null;
		if (image == null || image.equals(""))
			img = WindowManager.getCurrentImage();
		else
			img = WindowManager.getImage(image);
		if (img == null)
			{
			IJ.log("FitsJ.setCardOfImage: no image "+image);
			return;
			}

		String[] cards = FitsJ.getHeader(img);
		if (cards == null)
			{
			IJ.log("FitsJ.setCardOfImage: no FITS header for "+image);
			return;
			}
		if (typ.equals("string"))
			setCard(key,val,comment,cards);
		else if (typ.equals("boolean"))
			{
			if (val.startsWith("T") || val.startsWith("t"))
				setCard(key,true,comment,cards);
			else if (val.startsWith("F") || val.startsWith("f"))
				setCard(key,false,comment,cards);
			}
		else if (typ.equals("integer"))
			{
			try	{
				setCard(key,Integer.parseInt(val),comment,cards);
				}
			catch (NumberFormatException e)
				{
				}
			}
		else if (typ.equals("double") || typ.equals("float") || typ.startsWith("real"))
			{
			try	{
				setCard(key,Double.parseDouble(val),comment,cards);
				}
			catch (NumberFormatException e)
				{
				}
			}
		putHeader(img,cards);
		}

	/**
	 * Replaces keyword of a FITS card.
	 *
	 * @param newkey	The new FITS keyword.
	 * @param card		The FITS card image from which the key should be exchanged.
	 */
	public static String replaceKey (String key, String card)
		{
		if (key == null || card == null) return null;
		String val = FitsJ.getCardValue(card);
		String comment = FitsJ.getCardComment(card);
		return createCard(key,val,comment);
		}

	/**
	 * Extracts the FITS value from a card
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static String getCardValue (String card)
		{
		if (card == null) return null;

		int q1 = -1;
		int q2 = -1;
		int slash = -1;
		int equals = -1;

		// LOOK FOR CARDS WITH NO KEYWORD VALUE PAIR

		if (card.startsWith("COMMENT") || card.startsWith("HISTORY"))
			return card.substring(8);

		// FIND EQUALS SIGN

		equals = card.indexOf("=");
		if (equals < 0) return null;		// NO VALUE (e.g. COMMENT?)

		// LOOK FOR QUOTED VALUE

		q1 = card.indexOf("'");
		if (q1 >= 0)
			{
			q2 = card.indexOf("'",q1+1);
			if (q2 < 0) q1 = -1;
			}

		// OR FOR A DOUBLE QUOTED VALUE

		if (q1 < 0)
			{
			q1 = card.indexOf("\"");
			if (q1 > 0)
				{
				q2 = card.indexOf("\"",q1+1);
				if (q2 < 0) q1 = -1;
				}
			}

		// LOOK FOR COMMENT

		slash = card.indexOf("/");
// IJ.log(""+card);
// IJ.log("\tq1="+q1+", q2="+q2+", slash="+slash);

		if (slash < 0)			// NO COMMENT PRESENT, RETURN EVERYTHING RIGHT OF '='
			{
			if (q2 > 0)		// AS VALUE IN QUOTES
				return card.substring (q1,q2+1);
			else			// AS UNQUOTED VALUE
				return card.substring (equals+1).trim();
			}
		else if (q1 > 0 && slash < q2)		// MATCHING QUOTES IN VALUE, RETURN STRING WITH QUOTES
			return card.substring (q1,q2+1);

		// NO MATCHING QUOTES PRESENT, RETURN STRING BETWEEN '=' AND SLASH AS COMMENT
		return card.substring (equals+1,slash).trim();
		}

	/**
	 * Extracts the FITS comment from a card, including something like    "DATE = '12/34/56' / A date."
	 *
	 * @param card		The FITS card image from which the comment should be extracted.
	 */
	public static String getCardComment (String card)
		{
		if (card == null) return null;

		if (card.startsWith("COMMENT") || card.startsWith("HISTORY"))
			return null;

		int q1 = -1;
		int q2 = -1;
		int slash = -1;
		int equals = -1;

		// FIND EQUALS SIGN

		equals = card.indexOf("=");
		if (equals < 0) return null;		// NO VALUE (e.g. COMMENT?)

		// LOOK FOR SIMPLE QUOTE IN VALUE

		q1 = card.indexOf("'");
		if (q1 >= 0)
			q2 = card.indexOf("'",q1+1);

		// OR FOR A DOUBLE QUOTE IN VALUE

		if (q1 < 0)
			{
			q1 = card.indexOf("\"");
			if (q1 >= 0)
				q2 = card.indexOf("\"",q1+1);
			}

		// LOOK FOR COMMENT

		slash = card.indexOf("/");

		if (slash < 0)			// NO COMMENT PRESENT
			return null;
		else if (q2 < 0)		// NO MATCHING QUOTES PRESENT, RETURN STRING RIGHT OF SLASH AS COMMENT
			return card.substring (slash+1);
		else if (slash > q2)		// MATCHING QUOTES IN VALUE, RETURN STRING RIGHT OF SLASH AS COMMENT
			return card.substring (slash+1);

		slash = card.indexOf("/",q2+1);
		return card.substring (slash+1);
		}

	/**
	 * Extracts the double value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 * @changes		Accepts double values within strings (2009-01-10).
	 */
	public static double getCardDoubleValue (String card)
		{
		double d = Double.NaN;
		String s = getCardValue (card).trim();
		if (s == null)
			return Double.NaN;
		if (s.startsWith("\"") && s.endsWith("\""))
			s = s.substring(1, s.length()-1);
		if (s.startsWith("'") && s.endsWith("'"))
			s = s.substring(1, s.length()-1);
		d = Tools.parseDouble(s);
        if (Double.isNaN(d))
            d = getCardDoubleValueFromSexagesimal (card, 0);
		return d;
		}
    
	/**
	 * Extracts a double value from a FITS card sexagesimal string.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static double getCardDoubleValueFromSexagesimal (String card, int base)
		{
        double d = Double.NaN;
        boolean isRADegrees = false;
        boolean dNegative = false;
        String key = getCardKey(card);
		String s = getCardValue(card);
        String comment = getCardComment(card);
        if (comment == null) 
            comment = "";
        else
            comment = comment.toLowerCase();
		if (key == null || s == null)
			return Double.NaN;  
        key = key.toLowerCase();
        if (base == 0)
            {
            if (key.contains("ra"))
                {
                if (comment.contains("dd") || comment.contains("degrees"))
                    {
                    base = 360;
                    isRADegrees = true;
                    }
                else
                    {
                    base = 24;
                    }
                }
            else if (key.contains("dec"))
                {
                base = 90;
                }            
            else if (key.contains("ut"))
                {
                base = 24;
                }
            }
		int l = s.length();
		if (s.startsWith("\"") && s.endsWith("\""))
			s = s.substring(1,l-1);
		if (s.startsWith("'") && s.endsWith("'"))
			s = s.substring(1,l-1);
        String[] pieces = s.replaceAll("[\\-][^0-9\\.]{0,}", " \\-").replaceAll("[+][^0-9\\.]{0,}", " +").replaceAll("[^0-9\\.\\-+]{1,}", " ").trim().split("[^0-9\\.\\-+]{1,}");
        if (pieces.length > 0)
            {
            d = Tools.parseDouble(pieces[0]);
            if (!Double.isNaN(d) &&  pieces[0].contains("-"))
                {
                d = -d;
                dNegative = true;
                }
            if (pieces.length > 1 && !pieces[1].contains("-") && !pieces[1].contains("+"))
                {
                d += Math.abs(Tools.parseDouble(pieces[1])) / 60.0;
                if (pieces.length > 2 && !pieces[2].contains("-") && !pieces[2].contains("+"))
                    {
                    d += Math.abs(Tools.parseDouble(pieces[2])) / 3600.0;
                    }
                }
            }
        if (!Double.isNaN(d))
            {
            if (base != 0) d = mapToBase(d, base, dNegative);
            else if (dNegative) d = -d;
            }
        if (isRADegrees) d = d/15.0;
		return d;
		}    
    
    static double mapToBase(double num, int base, boolean negative)
        {
        double x = num;
        if (base==90)
            x = x>=90 ? (negative ? -90 : 90) : (negative ? -x : x);  //-89.999722 : 89.999722
        else if (base == 180 || base == 12)
            {
            x %= 2*base;
            x = x>base ? -2*base+x : x;
            x = negative ? -x : x;
            }
        else
            {
            x %= base;
            x = negative ? base-x : x;
            }
        return x;
        }    

	/**
	 * Extracts a string value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static String getCardStringValue (String card)
		{
		String s = getCardValue (card).trim();
		int l = s.length();
		if (s.startsWith("\"") && s.endsWith("\""))
			return s.substring(1,l-1);
		if (s.startsWith("'") && s.endsWith("'"))
			return s.substring(1,l-1);
		return s;
		}

	/**
	 * Extracts an int value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static int getCardIntValue (String card) throws NumberFormatException
		{
		int i = 0;
		String s = getCardValue (card);
		if (s.endsWith(".")) {
			s = s.substring(0, s.length() - 1);
		}
		i = Integer.parseInt(s);
		return i;
		}

	/**
	 * Extracts a boolean value from a FITS card.
	 *
	 * @param card		The FITS card image from which the value should be extracted.
	 */
	public static boolean getCardBooleanValue (String card)
		{
		String s = getCardValue (card);
		if (s.equals("T"))
			return true;
		else
			return false;
		}

	/**
	 * Finds and extracts a double value from a FITS header stored in a String array.
	 *
	 * @param key		The FITS keyword that should be found and parsed.
	 * @param cards		The FITS header.
	 */
	public static double findDoubleValue (String key, String[] cards) throws NumberFormatException
		{
		double d = Double.NaN;
		int icard = findCardWithKey (key, cards);
		if (icard < 0) return d;
		return getCardDoubleValue (cards[icard]);
		}

	/**
	 * Finds and extracts an integer value from a FITS header stored in a String array.
	 *
	 * @param key		The FITS keyword that should be found and parsed.
	 * @param cards		The FITS header.
	 */
	public static int findIntValue (String key, String[] cards) throws NumberFormatException
		{
		int icard = findCardWithKey (key, cards);
		if (icard < 0) throw new NumberFormatException();
		return getCardIntValue (cards[icard]);
		}

	/**
	 * Expands a Properties (key,val) pair into proper FITS format.
	 *
	 * @param key		The property key, which may contain a prefix showing that it is a FITS entry.
	 * @param val		The string value.
	 */
	protected static String expandKeyValue (String key, String val)
		{
		if (key == null || val == null) return null;

		int l = val.length();
		if (l > 70) l=70;
		String v = val.substring(0,l);

		l = key.length();
		String k = key;
		if (key.startsWith ("COMMENT"))
			return new String ("COMMENT "+v);
		else if (key.startsWith ("HISTORY"))
			return new String ("HISTORY "+v);
		while (l++ < 8) k += " ";
		return new String (k+"= "+v.trim());
		}

	/**
	 * Extracts the different parts of a FITS header card.
	 *
	 * @param card		A FITS card image.
	 */
	public static Object[] cardParts (String card)
		{
		String key = null;
		String val = null;
		String comment = null;
		double d = 0.0;
		int i = 0;
		boolean b = false;
		int typ = NO_CARD;

		// System.err.println("card="+card);

		String s = new String(card);

		// COMMENT

		if (card.startsWith ("COMMENT"))
			{
			key ="COMMENT";
			val = card.substring (7);
			comment = null;
			typ = COMMENT_CARD;
			}

		// HISTORY

		else if (card.startsWith ("HISTORY"))
			{
			key = "HISTORY";
			val = card.substring (7);
			comment = null;
			typ= HISTORY_CARD;
			}

		else
			{
			int eq = s.indexOf ("=");
			// System.err.println("eq="+eq);
			if (eq < 0) return null;
			key = s.substring (0,eq);
			// System.err.println("key="+key);
			if (key == null) return null;
			val = s.substring (eq+1);
			// System.err.println("val="+val);

			// COMMENT

			comment = getCardComment (s.substring(eq+1));
			// System.err.println ("comment="+comment);
			if (comment != null && !comment.equals(""))
				{
				int slash = s.indexOf (comment);
				// System.err.println ("slash=["+s.substring(slash-1,slash+1)+"]");
				val = s.substring (eq+1,slash-1).trim();
				// System.err.println ("val=["+val+"]");
				}

			// STRING

			if (val.startsWith("\'") || val.startsWith("\""))
				{
				s = val;
				val = s.substring(1,s.length()-1);
				// System.err.println ("val=["+val+"]");
				typ = STRING_CARD;
				}

			// BOOLEAN

			else if (val.equals("T") || val.equals("F"))
				{
				b = val.equals("T");
				typ = BOOLEAN_CARD;
				}

			// INTEGER OR DOUBLE

			else
				{
				try	{
					i = Integer.parseInt(val);
					typ = INTEGER_CARD;
					}
				catch (NumberFormatException e)
					{
					try	{
						d = Double.parseDouble(val);
						typ = DOUBLE_CARD;
						}
					catch (NumberFormatException nfe)
						{
						typ = NO_CARD;
						}
					}
				}
			}

		Object[] arr = new Object[]
				{key, val, d, i, b, comment, typ};
		return arr;
		}

	/**
	 * Copies a FITS header from one image to another.
	 */
	public static void copyHeader (ImagePlus imFrom, ImagePlus imTo)
		{
		String[] hdrFrom = getHeader (imFrom);
		if (hdrFrom != null)
			{
			hdrFrom = addHistory ("Complete FITS header copied from image "+imFrom.getShortTitle()+", slice "+imFrom.getCurrentSlice(), hdrFrom);
			putHeader (imTo, hdrFrom);
			}
		}

	/**
	 * Copies some cards from one image's FITS header to another.
	 */
	public static void copyHeader (ImagePlus imFrom, ImagePlus imTo, String[] keys)
		{
		// GET HEADERS

		String[] hdrFrom = getHeader (imFrom);
		String[] hdrTo = getHeader (imTo);

		// TRANSFER CARDS WITH THE SELECTED KEYWORDS

		hdrTo = addHistory ("FITS header cards copied from image "+imFrom.getShortTitle()+", slice "+imFrom.getCurrentSlice(), hdrTo);
		for (int i=0; i < keys.length; i++)
			{
			int icard = findCardWithKey (keys[i], hdrFrom);
			if (icard >= 0)
				hdrTo = addCard (hdrFrom[icard], hdrTo);
			}

		// SAVE RESULTING HEADER

		putHeader (imTo,hdrTo);
		}

	/**
	 * Pads a string to a given total length.
	 *
	 * @param s		The input string.
	 * @param length	The length to which the string should be padded.
	 */
	public static String pad (String s, int length)
		{
		int l = s.length();
		if (l >= length)
			return s.substring(0,length);
		else	{
			String blanks="";
			while (l++ < length) blanks += " ";
			return s+blanks;
			}
		}

	/**
	 * Separates a string into segments of a given length.
	 *
	 * @param s		The input string.
	 * @param length	The desired maximum length
	 */
	public static String[] segmentString (String s, int length)
		{
		if (s == null) return null;
		int l = s.length();
		int n = l/length;
		if (n == 0)
			n = 1;
		else if (n*length < l)
			n++;
		String[] arr = new String[n];
		int m1=0;
		for (int i=0; i < n; i++)
			{
			int m2 = m1+length;
			if (m2 >= l) m2=l;
			arr[i] = s.substring(m1,m2);
			m1 += length;
			}
		return arr;
		}

	/**
	 * Unsplits a string.
	 *
	 * @param arr		A String array
	 * @param sep		The desired separator.
	 */
	public static String unsplit (String[] arr, String sep)
		{
        if (arr==null) return "";
		String s = arr[0];
		for (int i=1; i < arr.length; i++)
			s += sep+arr[i];
		return s;
		}


	/********************************** DATE, TIME, JD ROUTINES ***********************************************/


	/**
	 * Extracts a DateTime string either from an explicit DateTime entry or builds one from
	 * separate date and time entries.
	 */
	public static String getDateTime (String[] cards)
		{
		String dt = getExplicitDateTime (cards);
		if (dt != null) return dt;

		String date = getDate (cards);
		if (date == null) return null;
		String time = getTime (cards);
		if (time == null) return null;
		dt = date+"T"+time;
		return dt;
		}

	/**
	 * Extracts explicit DateTime string from the FITS "DATE-OBS" entry.
	 */
	public static String getExplicitDateTime (String[] cards)
		{
		String datum = getDateObs(cards);
		if (datum == null) return null;

		// MAKE SURE IT'S REALLY AN ISO DATETIME WITH yyyy-{m}m-{d}dT{hh:mm:ss}

        if (datum.endsWith("Z")) datum=datum.substring(0, datum.length()-1);
		int i = datum.indexOf("T");
        datum = datum.replace('/', '-');  //accomodate / instead of - for Lowell telescopes
		int j = datum.indexOf("-");
		if (i > 7 && j == 4)
			return datum;
		return null;
		}

	/**
	 * Extracts calendar date from the FITS header stored in a String array.
	 */
	public static String getDateObs (String[] cards)
		{
		String dateobs = null;

		// TRY "DATE-OBS"

		int icard = findCardWithKey ("DATE-OBS", cards);
		if (icard > 0)
			dateobs = getCardStringValue (cards[icard]);

		// TRY "DATEOBS"

		if (dateobs == null)
			{
			icard = findCardWithKey ("DATEOBS", cards);
			if (icard > 0)
				dateobs = getCardStringValue (cards[icard]);
			}

		// TRY "DATE_OBS"

		if (dateobs == null)
			{
			icard = findCardWithKey ("DATE_OBS", cards);
			if (icard > 0)
				dateobs = getCardStringValue (cards[icard]);
			}
        
 		// TRY "UT_DATE"

		if (dateobs == null)
			{
			icard = findCardWithKey ("UT_DATE", cards);
			if (icard > 0)
				dateobs = getCardStringValue (cards[icard]);
			}       

		return dateobs;
		}

	/**
	 * Extracts calendar date from the FITS header stored in a String array.
	 */
	public static String getDate (String[] cards)
		{
		String datum = getDateObs (cards);
		if (datum == null) return null;

		// RE-ARRANGE INTO ISO FORMAT

		String dt="";

		// CHECK FOR dd/mm/yy

		if (datum.length() == 8 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String("19"+datum.substring(6,8)+"-"+datum.substring(3,5)+"-"+datum.substring(0,2));

		// CHECK FOR dd/mm/yyyy

		else if (datum.length() == 10 && datum.charAt(2) == '/' && datum.charAt(5) == '/')
			dt = new String(datum.substring(6,10)+"-"+datum.substring(3,5)+"-"+datum.substring(0,2));

		// CHECK FOR yyyy-mm-dd

		else if (datum.length() == 10 && datum.charAt(4) == '-' && datum.charAt(7) == '-')
			dt = new String(datum.substring(0,4)+"-"+datum.substring(5,7)+"-"+datum.substring(8,10));

		// CHECK FOR yy-mm-dd

		else if (datum.length() == 8 && datum.charAt(2) == '-' && datum.charAt(5) == '-')
			dt = new String("19"+datum.substring(0,2)+"-"+datum.substring(3,5)+"-"+datum.substring(6,8));

		// OR GIVE UP

		else
			{
			IJ.log ("Unable to parse date "+datum);
			return null;
			}
		return dt;
		}

	/**
	 * Extracts UT Time from a FITS header in the form of a String array.
	 */
	public static String getTimeObs (String[] cards)
		{
		String timeobs = null;

		// TRY "TIME-OBS"

		int icard = findCardWithKey ("TIME-OBS", cards);
		if (icard > 0)
			timeobs = getCardStringValue (cards[icard]);

		// TRY "TIMEOBS"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TIMEOBS", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// TRY "TIME_OBS"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TIME_OBS", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "TM-START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TM-START", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "TM_START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TM_START", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "UTC"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UTC", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "UTSTART"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UTSTART", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT-START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT-START", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}

		// OR EXTRACT FROM "UT_START"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT_START", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}
        
		// OR EXTRACT FROM "UT_TIME"

		if (timeobs == null)
			{
			icard = findCardWithKey ("UT_TIME", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}    
        
		// OR EXTRACT FROM "TAIHMS"

		if (timeobs == null)
			{
			icard = findCardWithKey ("TAIHMS", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}  
        
        // OR EXTRACT FROM "UTCSTART" (SOFIA)
        
		if (timeobs == null)
			{
			icard = findCardWithKey ("UTCSTART", cards);
			if (icard > 0)
				timeobs = getCardStringValue (cards[icard]);
			}        

		return timeobs;
		}

	/**
	 * Extracts UT Time in format hh:mm:ss from a FITS header in a String array.
	 */
	public static String getTime (String[] cards)
		{
		String datum = getTimeObs (cards);
        if (datum == null) return null;
        
		String dt="";

		// CHECK FOR hh:mm:ss.sss

		if (datum.indexOf(":") > 0)
			dt = datum;

		// OR CHECK FOR FLOATING POINT NUMBER

		else	{
			try	{
				double fp = Double.parseDouble(datum);
				int hh = (int)fp;
				int mm = (int)((fp-(double)hh)*60.0);
				double ss = (fp-(double)hh-(double)mm/60.0)/60.0;

				String sh=null;
				String sm=null;

				if (hh < 10)
					sh = "0"+hh;
				else
					sh = ""+hh;
				if (mm < 10)
					sm = "0"+mm;
				else
					sm = ""+mm;
				dt = sh+":"+sm+":"+ss;
				}
			catch (NumberFormatException e)
				{
				IJ.error("Unable to parse time "+datum);
				return null;
				}
			}
        dt=dt.replace(' ', '0');       
		return dt;
		}

	/**
	 * Returns time of day in seconds.
	 */
	public static double getDecimalTime (String[] cards)
		{
		double t = Double.NaN;
		String time = getTime(cards);
		if (time == null) return Double.NaN;

		try	{
			int i = time.indexOf(":");
			double hh = Double.parseDouble (time.substring (0,i));
			double mm = Double.parseDouble (time.substring (i+1,i+3));
			double ss = Double.parseDouble (time.substring (i+4));
			t = 3600.0*hh+60.0*mm+ss;
			}
		catch (NumberFormatException e)
			{
			IJ.error ("Unable to parse time "+time);
			}
		return  t;
		}

	/**
	 * Extracts exposure time from the FITS header in a  String array.
	 *
	 * Version 2009-01-10: accepts double values in strings via new getCardDoubleValue().
	 */
	public static double getExposureTime (String[] cards)
		{
		double tstart=0.0;
		double tend=0.0;
		int icard = 0;
		int icard2 = 0;

		try	{

			String tcomment = "";

            // CHECK FOR KEYWORD "TELAPSE" (e.g. TESS)

			icard = findCardWithKey ("TELAPSE",cards);
			if (icard >= 0)
				{
				tstart = getCardDoubleValue (cards[icard]);
				if (! Double.isNaN(tstart)) {
					tcomment = getCardComment(cards[icard]);
					if (tcomment != null && tcomment.contains("[d]")) tstart = tstart * 24.0 * 3600.0;
					}
				return tstart;
				}

			// CHECK FOR STANDARD KEYWORD "EXPTIME" (SECS)


			icard = findCardWithKey ("EXPTIME",cards);
			if (icard >= 0)
				{
				tstart = getCardDoubleValue (cards[icard]);
				if (! Double.isNaN(tstart)) {
					tcomment = getCardComment(cards[icard]);
					if (tcomment != null && tcomment.contains("[d]")) tstart = tstart * 24.0 * 3600.0;
					}
				return tstart;
				}

			// CHECK FOR KEYWORD "EXPOSURE" (e.g. Mount Stromlo)

			icard = findCardWithKey ("EXPOSURE",cards);
			if (icard >= 0)
				{
				tstart = getCardDoubleValue (cards[icard]);
				if (! Double.isNaN(tstart)) {
					tcomment = getCardComment(cards[icard]);
					if (tcomment != null && tcomment.contains("[d]")) tstart = tstart * 24.0 * 3600.0;
					}
				return tstart;
				}
            
            // CHECK FOR KEYWORD "EXP_TIME" (e.g. Mt. Laguna)
            
            icard = findCardWithKey ("EXP_TIME",cards);
			if (icard >= 0)
				{
				tstart = getCardDoubleValue (cards[icard]);
				if (! Double.isNaN(tstart)) {
					tcomment = getCardComment(cards[icard]);
					if (tcomment != null && tcomment.contains("[d]")) tstart = tstart * 24.0 * 3600.0;
					}
				return tstart;
				}

			// OR CHECK FOR 'TM-START' AND 'TM-END' (SECS)

			icard = findCardWithKey ("TM-START",cards);
			icard2 = findCardWithKey ("TM-END",cards);

			// OR CHECK FOR 'TM_START' AND 'TM_END' (SECS)

			if (icard < 0 || icard2 < 0)
				{
				icard  = findCardWithKey ("TM_START",cards);
				icard2 = findCardWithKey ("TM_END",cards);
				}

			// OR CHECK FOR 'UT-START' AND 'UT-END' (SECS)

			if (icard < 0 || icard2 < 0)
				{
				icard  = findCardWithKey ("UT-START",cards);
				icard2 = findCardWithKey ("UT-END",cards);
				}

			// OR CHECK FOR 'UT_START' AND 'UT_END' (SECS)

			if (icard < 0 || icard2 < 0)
				{
				icard  = findCardWithKey ("UT_START",cards);
				icard2 = findCardWithKey ("UT_END",cards);
				}

			// OR GIVE UP

			if (icard < 0 || icard2 < 0)
				return Double.NaN;

			tstart = getCardDoubleValue (cards[icard]);
			tend   = getCardDoubleValue (cards[icard2]);
			if (Double.isNaN(tstart) || Double.isNaN(tend))
				return Double.NaN;
			}
		catch (NumberFormatException e)
			{
			IJ.error ("Unable to extract exposure time from FITS header: "+e.getMessage());
			return Double.NaN;
			}

		// WATCH OUT FOR CHANGE OF DAYS

		if (tend < tstart) tend += 3600*24.0;

		// RETURN DIFFERENCE BETWEEN START AND END TIMES

		return (tend-tstart);
		}

	/**
	 * Returns mid-exposure dateTime using a Properties list.
	 */
	public static String getMeanDateTime (String[] cards)
		{
		String dt = getDateTime (cards);
		double t = getExposureTime (cards);
		if (dt == null || Double.isNaN(t)) return null;

		t *= 0.5;

		Duration dur;
		Date date;
		try	{
			dur = new Duration("P"+t+"S");
			date = DateParser.parse (dt);
			}
		catch (InvalidDateException e)
			{
			IJ.error(e.getMessage());
			return null;
			}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime (date);
		try	{
			Calendar result = dur.addTo(cal);
			return DateParser.getIsoDate(result.getTime());
			}
		catch (InvalidDateException e)
			{
			IJ.error ("Unable to add half of exposure time = "+t+" to datetime "+dt);
			return null;
			}
		}


/**************************************** JD METHODS **************************************************/

    public static boolean isTESS(String[] cards)
        {
		Boolean isTess = true;
        String value = "";
		int icard = findCardWithKey ("TELESCOP", cards);
		if (icard > 0)
            {
			value = getCardStringValue (cards[icard]);
            if (!value.toLowerCase().contains("tess")) isTess = false;
            }
        icard = findCardWithKey ("INSTRUME", cards);
		if (icard > 0)
            {
			value = getCardStringValue (cards[icard]);
            if (!value.toLowerCase().contains("tess photometer")) isTess = false;
            }
        return isTess;
        }

	/**
	 * Returns BJD_TDB_MID_OBS from a TESS FITS headers.
	 */
    
    public static double getMeanTESSBJD (String[] cards)
        {
        double tstart=0, tstop=0, bjdreff=0, bjdtdb=0;
        int bjdrefi=0, icard=0;
        boolean checkBJDTDB=false;
        String value = "";
        
        icard = findCardWithKey ("TSTART", cards);
		if (icard > 0)
            {
            tstart = findDoubleValue ("TSTART", cards);
            }
        else
            {
            checkBJDTDB=true;
            }
        
        icard = findCardWithKey ("TSTOP", cards);
		if (icard > 0)
            {
            tstop = findDoubleValue ("TSTOP", cards);
            }
        else
            {
            checkBJDTDB=true;
            } 
        
        icard = findCardWithKey ("BJDREFI", cards);
		if (icard > 0)
            {
            bjdrefi = findIntValue ("BJDREFI", cards);
            }
        else
            {
            checkBJDTDB=true;
            }
        
        icard = findCardWithKey ("BJDREFF", cards);
		if (icard > 0)
            {
            bjdreff = findDoubleValue ("BJDREFF", cards);
            }
        else
            {
            checkBJDTDB=true;
            } 
        
        if (!checkBJDTDB)
            {
            return tstart + ((tstop-tstart)/2.0) + bjdrefi + bjdreff;
            }

        icard = findCardWithKey ("BJD_TDB", cards);
		if (icard > 0)
            {
            bjdtdb = findDoubleValue ("BJD_TDB", cards);
            return bjdtdb;
            }
        return Double.NaN;
        }
    

	/**
	 * Returns JD from a FITS header stored in a String array.
	 */
	public static double getJD (String[] cards)
		{
		boolean modified = false;
		double julian = Double.NaN;

		// TRY TO GET JD FROM FITS HEADER

    	String dt = getDateTime (cards);
		if (dt != null) julian = JulianDate.JD (dt);
        
        if (Double.isNaN(julian))
		    julian = findDoubleValue ("JD-OBS", cards);
		if (Double.isNaN(julian))
			julian = findDoubleValue ("JD", cards);
		if (Double.isNaN(julian))
			{
			julian = findDoubleValue ("MJD-OBS", cards);
			if (!Double.isNaN(julian))
				modified = true;
			}
		if (Double.isNaN(julian))
			{
			julian = findDoubleValue ("MJD", cards);
			if (!Double.isNaN(julian))
				modified = true;
			}

		if (Double.isNaN(julian))
			return Double.NaN;
		
		if (modified) julian += 2400000.5;
		return julian;
		}

	/**
	 * Returns mid-exposure Julian Date from a FITS header stored in a String array.
	 */
	public static double getMeanJD (String[] cards)
		{
		double jd = getJD (cards);
		double texp = getExposureTime (cards);
		if (Double.isNaN(jd) || Double.isNaN(texp))
			return Double.NaN;
		else
			return jd+0.5*(texp/3600.0)/24.0;
		}

	/**
	 * Returns MJD from a FITS header stored in a String array.
	 */
	public static double getMJD (String[] cards)
		{
		double jd = getJD (cards);
		if (!Double.isNaN(jd)) jd -= 2400000.0;
		return jd;
		}

	/**
	 * Returns mid-exposure MJD from a FITS header stored in a String array.
	 */
	public static double getMeanMJD (String[] cards)
		{
		double jd = getMeanJD(cards);
		if (!Double.isNaN(jd))
			jd -= 2400000.0;
		return jd;
		}
	}
