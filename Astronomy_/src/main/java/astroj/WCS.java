// WCS.java

package astroj;

import ij.IJ;
import ij.ImagePlus;

import java.text.DecimalFormat;

/**
 * A simple WCS object for the world coordinate systems used in most astronomical FITS files.
 * Uses the FitsJ class methods.
 *
 * The processing follows the general guidelines in
 *	Greisen, E.W. & Calabretta M.R. 2002, Astron. & Astrophys. Vol. 395, 1061-1075
 *	Calabretta M.R. & Greisen, E.W. 2002, Astron. & Astrophys. Vol. 395, 1077-1122
 *
 * Modifications by K. Collins 7/2010
 * Fixed three issues related to proper RA and DEC calculations for the TAN projection
 * - The "RA---TAN-SIP" extension caused problems with the "-TAN" substring matching
 *		changed: int i = CTYPE[0].lastIndexOf("-");
 *			 projection = CTYPE[0].substring(i+1);
 *		to:      projection = CTYPE[0].substring(5, 8);
 *
 * - Reversed the indices when loading the PC and CD Matrix coefficients
 *		changed: CD[k-1][i-1]  and PC[k-1][i-1]
 *		to:      CD[i-1][k-1]  and PC[i-1][k-1]
 *
 * - Reversed the order of the arguments in one statement when calculating the TAN projection
 *		changed: s[0] = R2D*Math.atan2(-x[1],x[0]);
 *		to:      s[0] = R2D*Math.atan2(x[0],-x[1]);
 *
 * Added code to support the SIP non-linear distortion corrections following
 *  SHUPE, D.L. ET AL. 2008
 *
 * Modifications by F. Hessman 9/2012
 * - Added wcs2pixel(), but no SIP corrections supported!!!!
 */
public class WCS
	{
    static int SIP_MAX = 10;
	int NAXIS=2;
	int WCSAXES=2;
	int[] NAXES;
	double[] CDELT;
    double[] CROTA;
    double[] PIXSZ;
    double FOCALLEN;
    double PA;
	double[] CRPIX;
	double[] CRVAL;
	double[][] PC;
    

	double[][] CD;
	double[][] CDinv = null;
    double[][] PV;
	double[][] A, AP;
	double[][] B, BP;
    double[] S = new double[SIP_MAX];
    double[] SP = new double[SIP_MAX];
    String prefix;
    double northPA = 90;
    double eastPA = 180;
    double xScale = 1.0;
    double yScale = 1.0;
    double epoch = 2000.0;
    double FFIScale = 1.0;
	String[] CTYPE;
	String[] CUNIT;
	double LONPOLE;
	int A_ORDER, AP_ORDER;
	int B_ORDER, BP_ORDER;
    double[] eul;  //used to forward and reverse transform celestial to/from native coordinates of a projection -- from WCStools

	public String coordsys = "No WCS";
	String projection;
	boolean hasCD=false;
    boolean hasPC=false;
	boolean[] hasSIP, hasSIPinv;
    boolean[] hasCDELT;
    boolean[] hasCROTA;
    boolean[] hasPIXSZ;
    boolean hasFOCALLEN = false;
    boolean hasPAkeyword = false;
    boolean hasRADEC=false;
    boolean hasRA=false;
    boolean hasCDELTOnly = false;
    boolean hasDEC=false;
    boolean hasDELROT=false;
	boolean typeContainsSIP = false;
    boolean useSIPAlways = true;
    boolean zenithal = false;
    public boolean hasPA = false;
    public boolean hasScale = false;

	boolean enoughInfo=false;
	public String logInfo = new String("");
    DecimalFormat threeToLeft = new DecimalFormat("000");

	public static String PREFS_NPIX1    = new String("wcs.npix1");
	public static String PREFS_NPIX2    = new String("wcs.npix2");
	public static String PREFS_CRPIX1   = new String("wcs.crpix1");
	public static String PREFS_CRPIX2   = new String("wcs.crpix2");
	public static String PREFS_CRVAL1   = new String("wcs.crval1");
	public static String PREFS_CRVAL2   = new String("wcs.crval2");
	public static String PREFS_CDELT1   = new String("wcs.cdelt1");
	public static String PREFS_CDELT2   = new String("wcs.cdelt2");
	public static String PREFS_CTYPE1   = new String("wcs.ctype1");
	public static String PREFS_CTYPE2   = new String("wcs.ctype2");
	public static String PREFS_CD1_1    = new String("wcs.cd11");
	public static String PREFS_CD1_2    = new String("wcs.cd12");
	public static String PREFS_CD2_1    = new String("wcs.cd21");
	public static String PREFS_CD2_2    = new String("wcs.cd22");
	public static String PREFS_PC1_1    = new String("wcs.pc11");
	public static String PREFS_PC1_2    = new String("wcs.pc12");
	public static String PREFS_PC2_1    = new String("wcs.pc21");
	public static String PREFS_PC2_2    = new String("wcs.pc22");

	public static String PREFS_USENPIX  = new String("wcs.usenpix");
	public static String PREFS_USECRPIX = new String("wcs.usecrpix");
	public static String PREFS_USECRVAL = new String("wcs.usecrval");
	public static String PREFS_USECDELT = new String("wcs.usecdelt");
	public static String PREFS_USECTYPE = new String("wcs.usectype");
	public static String PREFS_USECD    = new String("wcs.usecd");
	public static String PREFS_USEPC    = new String("wcs.usepc");
    public static double R2D = 180.0/Math.PI;
    public static double D2R = Math.PI/180.0;
    static double WCSTRIG_TOL = 1e-10;
    
    
    
	public WCS (int naxis)
		{
		initialize(naxis);
		}

	public WCS (ImagePlus img)
		{
		NAXIS=2;
		var hdr = FitsJ.getHeader (img, false);
		if (hdr == null) return;
		process(img.getShortTitle(),img.getWidth(),img.getHeight(),img.getStackSize(),hdr);
		}

	public WCS (FitsJ.Header hdr)
		{
		NAXIS=2;
		if (hdr == null) return;
		process(null,-1,-1,-1, hdr);
		}

	protected void process (String title, int nx, int ny, int nz, FitsJ.Header hdr)
		{
		// FITS HEADER PRESENT?
		if (hdr == null) return;

//		int naxis=-1;
		hasRADEC = false;

		// GET NUMBER OF AXES
        
        int nax = -1;
		int icard = FitsJ.findCardWithKey ("NAXIS",hdr);
		if (icard >= 0)
			nax = FitsJ.getCardIntValue (hdr.cards()[icard]);
		if (nax <= 0)
			{
			logInfo += "Can not read NAXIS keyword in FITS header of "+title+"\n";
            writeLog();
			return;
			}
		if (nax > 2) NAXIS = 2;   //limit for AIJ FITS files is 2 AXES at this time
        WCSAXES = NAXIS;
		// CHECK IF THE NUMBER OF WCS AXES IS KNOWN AND DIFFERENT

		icard = FitsJ.findCardWithKey ("WCSAXES",hdr);
		if (icard >= 0)
			{
			WCSAXES = FitsJ.getCardIntValue (hdr.cards()[icard]);  //retain to create CD/PC matrix subscripts for WCSAXES > AIJ AXES (NAXIS)
			}
        if (WCSAXES < NAXIS) NAXIS = WCSAXES;
		if (NAXIS < 1)
			{
			logInfo += "No WCS axes defined in FITS header of "+title+"\n";
            writeLog();
			return;
			}        
		// GIVEN NUMBER OF WCS AXES, RE-INITIALIZE MATRICES, ETC.

		initialize (NAXIS);

		// GET SIZES OF AXES

//		for (int j=1; j <= NAXIS; j++)
//			{
//			icard = FitsJ.findCardWithKey ("NAXIS"+j, hdr);
//			if (icard < 0)
//				{
//				logInfo += "Cannot find keyword NAXIS"+j+" in FITS header of "+title+"\n";
//              writeLog();
//				return;
//				}
//			NAXES[j-1] = FitsJ.getCardIntValue (hdr.cards()[icard]);
//			}
        
        // GET SIZES OF AXES
        
        icard = FitsJ.findCardWithKey ("IMAGEW", hdr);
        if (icard < 0)
            {
            icard = FitsJ.findCardWithKey ("NAXIS1", hdr);
            if (icard < 0)
                {            
                logInfo += "Cannot find keyword IMAGEW or NAXIS1 in FITS header of "+title+"\n";
                writeLog();
                return;
                }
            }
        NAXES[0] = FitsJ.getCardIntValue (hdr.cards()[icard]);  
        
        icard = FitsJ.findCardWithKey ("IMAGEH", hdr);
        if (icard < 0)
            {
            icard = FitsJ.findCardWithKey ("NAXIS2", hdr);
            if (icard < 0)
                {            
                logInfo += "Cannot find keyword IMAGEH or NAXIS2 in FITS header of "+title+"\n";
                writeLog();
                return;
                }
            }
        NAXES[1] = FitsJ.getCardIntValue (hdr.cards()[icard]);        

		// MAKE SURE THEY AGREE WITH THE ImageJ NUMBERS

		if (NAXIS >= 1 && nx >= 1 && NAXES[0] != nx)
			{
			logInfo += "Horizontal axis size="+nx+" does not match FITS header! ("+NAXES[0]+")\n";
            writeLog();
			NAXES[0] = nx;
			}
		if (NAXIS >= 2 && ny >= 1 && NAXES[1] != ny)
			{
			logInfo += "Vertial axis size="+ny+" does not match FITS header! ("+NAXES[1]+")\n";
            writeLog();
			NAXES[1] = ny;
			}
		if (NAXIS >= 3 && nz >= 1 && NAXES[2] != nz)
			{
			logInfo += "Stack size="+nz+" does not match FITS header! ("+NAXES[2]+")\n";
            writeLog();
			return;
			}

		// TRY TO FIND RA-- and DEC- COORDINATE TYPES

		CTYPE[0] = null;
        if (NAXIS > 1) CTYPE[1] = null;
		String typ = "";
		prefix = "";
		String abc[] = new String[] {	"",
						"A","B","C","D","E","F","G","H","I","J","K","L","M",
						"N","O","P","Q","R","S","T","U","V","W","X","Y","Z" };
		for (int k=0; k < abc.length; k++)
			{
			icard = FitsJ.findCardWithKey ("CTYPE1"+abc[k], hdr);
			if (icard > 0)
				{
				typ = FitsJ.getCardStringValue(hdr.cards()[icard]);
				if (typ != null && (typ.startsWith("RA--")))
					{
					CTYPE[0] = typ;
					prefix = abc[k];
					if (CTYPE[0].length() >=8) projection = CTYPE[0].substring(5, 8);
					if (CTYPE[0].length() >= 12 && CTYPE[0].substring(9,12).equals("SIP"))
                        {
                        typeContainsSIP = true;
                        }
					break;
					}
				}
			}

		// GET MATCHING COORDINATE TYPES
        if (CTYPE[0] != null && NAXIS > 1)
            {
            for (int k=2; k <= NAXIS; k++)
                {
                icard = FitsJ.findCardWithKey ("CTYPE"+k+prefix, hdr);
                if (icard > 0)
                    CTYPE[k-1] = FitsJ.getCardStringValue(hdr.cards()[icard]);
                }

            // CHECK IF CTYPE2n IS "DEC" AND COORDINATE SYSTEMS MATCH

            if (CTYPE[0].startsWith("RA--") && CTYPE[1] != null && CTYPE[1].startsWith("DEC-"))
                hasRADEC=true;
            }
		// GET TRANSFORMATION COEFFICIENTS (SCALE, TRANSLATION)

        for (int k=1; k <= NAXIS; k++)
            {
            CRPIX[k-1] = NAXES[k-1]/2f;
            icard = FitsJ.findCardWithKey ("CDELT"+k+prefix, hdr);
            if (icard > 0)
                {
                CDELT[k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                if (!Double.isNaN(CDELT[k-1])) hasCDELT[k-1] = true;
                }
            icard = FitsJ.findCardWithKey ("CROTA"+k+prefix, hdr);
            if (icard > 0)
                {
                CROTA[k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                if (!Double.isNaN(CROTA[k-1])) hasCROTA[k-1] = true;
                }            
            icard = FitsJ.findCardWithKey ("CRPIX"+k+prefix, hdr);
            if (icard > 0)
                CRPIX[k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
            icard = FitsJ.findCardWithKey ("CRVAL"+k+prefix, hdr);
            if (icard > 0)
                CRVAL[k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
            else
                {
                if (k==1)
                    {
                    icard = FitsJ.findCardWithKey ("RA", hdr);
                    if (icard > 0)
                        {
                        double val = 15 * FitsJ.getCardDoubleValueFromSexagesimal(hdr.cards()[icard], 24);  
                        if (!Double.isNaN(val))
                            {
                            CRVAL[k-1] = val;
                            hasRA = true;
                            }
                        }
                    }
                else if (k==2)
                    {
                    icard = FitsJ.findCardWithKey ("DEC", hdr);
                    if (icard > 0)
                        {
                        double val = FitsJ.getCardDoubleValueFromSexagesimal(hdr.cards()[icard], 90);  
                        if (!Double.isNaN(val)) 
                            {
                            CRVAL[k-1] = val;  
                            hasDEC = true;
                            }
                        }
                    }
                }
            }
		
        if (hasRA && hasDEC) hasRADEC = true;

        icard = FitsJ.findCardWithKey ("EPOCH"+prefix, hdr);
        if (icard > 0)
            {
            double EPOCH = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
            if (!Double.isNaN(EPOCH)) 
                {
                epoch = EPOCH;
                }
            }
        
//        icard = FitsJ.findCardWithKey ("FFIINDEX", hdr);
//        if (icard > 0)
//            {
//            FFIScale=0.35;
//            }
//        
//        icard = FitsJ.findCardWithKey ("WCS_MSEP", hdr);
//        if (icard > 0)
//            {
//            String comment = FitsJ.getCardComment (hdr.cards()[icard]);
//            if (comment.contains("FFI")) FFIScale=0.35;
//            }
//        
		try	{
            boolean hasOldPC = false;
            double[][] oldPC = PC.clone();
			for (int k=1; k <= NAXIS; k++)
				{
				for (int i=1; i <= NAXIS; i++)
					{
					icard = FitsJ.findCardWithKey ("CD"+i+"_"+k+prefix, hdr);
					if (icard > 0)
						{
						hasCD=true;
						CD[i-1][k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
						} 
                    if (!hasCD)
                        {
                        icard = FitsJ.findCardWithKey ("PC"+i+"_"+k+prefix, hdr);
                        if (icard > 0)
                            {
                            hasPC=true;
                            PC[i-1][k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                            }
                        if (!hasPC)
                            {
                            icard = FitsJ.findCardWithKey ("PC"+threeToLeft.format(i-1)+threeToLeft.format(k-1)+prefix, hdr);
                            if (icard > 0)
                                {
                                hasOldPC=true;
                                oldPC[i-1][k-1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                                }                            
                            
                            }
                        }
                    }
				}
            if (!hasPC && hasOldPC)
                {
                hasPC = true;
                PC = oldPC;
                }
			}
		catch (NumberFormatException e)
			{
			logInfo += "Cannot read transformation matrices PC/CD in FITS header!\n";
            writeLog();
			return;
			}

        // IF AVAILABLE, GET SIP NON-LINEAR DISTORTION COEFFICIENTS
		// SEE SHUPE, D.L. ET AL., 2005, ASPC 347, 491 (http://adsabs.harvard.edu/abs/2005ASPC..347..491S) 
		if (typeContainsSIP)
			{
			try	{
				// LOOK FOR A, B, AP, and BP POLYNOMIAL ORDERS
				if (NAXIS > 1)
					{
					A_ORDER = -1;
					icard = FitsJ.findCardWithKey ("A_ORDER",hdr);
					if (icard >= 0)
						{
						A_ORDER = FitsJ.getCardIntValue (hdr.cards()[icard]);
						if (A_ORDER < 2 || A_ORDER > 9)
							{
							A_ORDER = -1;
							logInfo += "SIP A_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP A_ORDER not found in FITS header!\n";

					B_ORDER = -1;
					icard = FitsJ.findCardWithKey ("B_ORDER",hdr);
					if (icard >= 0)
						{
						B_ORDER = FitsJ.getCardIntValue (hdr.cards()[icard]);
						if (B_ORDER < 2 || B_ORDER > 9)
							{
							B_ORDER = -1;
							logInfo += "SIP B_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP B_ORDER not found in FITS header!\n";
                    
					AP_ORDER = A_ORDER; //in case there is no AP_ORDER (e.g. TICA FFIs)
					icard = FitsJ.findCardWithKey ("AP_ORDER",hdr);
					if (icard >= 0)
						{
						AP_ORDER = FitsJ.getCardIntValue (hdr.cards()[icard]);
						if (AP_ORDER < 2 || AP_ORDER > 9)
							{
							A_ORDER = -1;
							logInfo += "SIP AP_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP AP_ORDER not found in FITS header!\n";

					BP_ORDER = B_ORDER; //in case there is no BP_ORDER (e.g. TICA FFIs)
					icard = FitsJ.findCardWithKey ("BP_ORDER",hdr);
					if (icard >= 0)
						{
						BP_ORDER = FitsJ.getCardIntValue (hdr.cards()[icard]);
						if (BP_ORDER < 2 || BP_ORDER > 9)
							{
							BP_ORDER = -1;
							logInfo += "SIP BP_ORDER out of range in FITS header!\n";
							}
						}
					else
						logInfo += "SIP BP_ORDER not found in FITS header!\n";                    
					}
				}
			catch (NumberFormatException e)
				{
				logInfo += "Cannot read number in at least one of SIP A_ORDER, AP_ORDER, B_ORDER, BP_ORDER in FITS header!\n";
				A_ORDER = -1;
				B_ORDER = -1;
                AP_ORDER = -1;
                BP_ORDER = -1;
				}

			//INITIALIZE A, AP, B, and BP MATRICES
			if (A_ORDER >= 2 && A_ORDER <= 9)
				{
				A = new double[A_ORDER+1][A_ORDER+1];
				for (int q=0; q <= A_ORDER; q++)
					{
					for (int p=0; p <= A_ORDER; p++)
						{
						A[p][q] = 0.0;
						}
					}
				}

			if (B_ORDER >= 2 && B_ORDER <= 9)
				{
				B = new double[B_ORDER+1][B_ORDER+1];
				for (int q=0; q <= B_ORDER; q++)
					{
					for (int p=0; p <= B_ORDER; p++)
						{
						B[p][q] = 0.0;
						}
					}
				}
            
			if (AP_ORDER >= 2 && AP_ORDER <= 9)
				{
				AP = new double[AP_ORDER+1][AP_ORDER+1];
				for (int q=0; q <= AP_ORDER; q++)
					{
					for (int p=0; p <= AP_ORDER; p++)
						{
						AP[p][q] = 0.0;
						}
					}
				}

			if (BP_ORDER >= 2 && BP_ORDER <= 9)
				{
				BP = new double[BP_ORDER+1][BP_ORDER+1];
				for (int q=0; q <= BP_ORDER; q++)
					{
					for (int p=0; p <= BP_ORDER; p++)
						{
						BP[p][q] = 0.0;
						}
					}
				}            

			//LOOK FOR SIP X-COORDINATE MATRIX COEFICIENTS A_p_q IN FITS HEADER
			if (A_ORDER >= 2 && A_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= A_ORDER; q++)
						{
						for (int p=0; p <= A_ORDER; p++)
							{
							if (q + p <= A_ORDER)
								{
								icard = FitsJ.findCardWithKey ("A_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									A[p][q] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
//									IJ.write("A["+p+"]["+q+"] = "+A[p][q]);
									hasSIP[0]=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix A in FITS header!\n";
                    writeLog();
					return;
					}
				}
			
			//LOOK FOR SIP Y-COORDINATE MATRIX COEFICIENTS B_p_q IN FITS HEADER
			if (B_ORDER >= 2 && B_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= B_ORDER; q++)
						{
						for (int p=0; p <= B_ORDER; p++)
							{
							if (q + p <= B_ORDER)
								{
								icard = FitsJ.findCardWithKey ("B_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									B[p][q] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
//									IJ.write("B["+p+"]["+q+"] = "+B[p][q]);
									hasSIP[1]=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP matrix B in FITS header!\n";
                    writeLog();
					return;
					}
				}
			}
        

			//LOOK FOR SIP X-COORDINATE INVERSE MATRIX COEFICIENTS AP_p_q IN FITS HEADER
			if (AP_ORDER >= 2 && AP_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= AP_ORDER; q++)
						{
						for (int p=0; p <= AP_ORDER; p++)
							{
							if (q + p <= AP_ORDER)
								{
								icard = FitsJ.findCardWithKey ("AP_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									AP[p][q] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
//									IJ.write("AP["+p+"]["+q+"] = "+AP[p][q]);
									hasSIPinv[0]=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read SIP inverse matrix AP in FITS header!\n";
                    writeLog();
					return;
					}
				}
			
			//LOOK FOR SIP Y-COORDINATE INVERSE MATRIX COEFICIENTS BP_p_q IN FITS HEADER
			if (BP_ORDER >= 2 && BP_ORDER <= 9)
				{
				try	{
					for (int q=0; q <= BP_ORDER; q++)
						{
						for (int p=0; p <= BP_ORDER; p++)
							{
							if (q + p <= BP_ORDER)
								{
								icard = FitsJ.findCardWithKey ("BP_"+p+"_"+q+prefix, hdr);
								if (icard > 0)
									{
									BP[p][q] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
//									IJ.write("BP["+p+"]["+q+"] = "+BP[p][q]);
									hasSIPinv[1]=true;
									}
								}
							}
						}
					}
				catch (NumberFormatException e)
					{
					logInfo += "Cannot read number in SIP inverse matrix BP in FITS header!\n";
                    writeLog();
					return;
					}
				}
        
        
        if (projection.equals("SIN")) 
            {
            icard = FitsJ.findCardWithKey ("PV2_1"+prefix, hdr);
            if (icard > 0) PV[1][1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
            icard = FitsJ.findCardWithKey ("PV2_2"+prefix, hdr);
            if (icard > 0) PV[1][2] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);           
            }
        
		LONPOLE = 180.0;// DEGS
		icard = FitsJ.findCardWithKey ("LONPOLE"+prefix, hdr);
		if (icard > 0)
			LONPOLE = FitsJ.getCardDoubleValue(hdr.cards()[icard]);

        // CALCULATE ROTATION, SCALE, AND INVERSE MATRICES
        if (NAXIS==2)
            {
            if (hasCD)
                {
                CDELT[0] = Math.sqrt(CD[0][0]*CD[0][0] + CD[1][0]*CD[1][0]);
                CDELT[1] = Math.sqrt(CD[0][1]*CD[0][1] + CD[1][1]*CD[1][1]);
                }
            else if (!hasCD && hasPC)
                {
                if (hasCDELT[0] && !hasCDELT[1]) CDELT[1] = CDELT[0];
                else if (hasCDELT[1] && !hasCDELT[0]) CDELT[0] = CDELT[1];                
                for (int j=0; j < NAXIS; j++)
                    {
                    for (int i=0; i < NAXIS; i++)
                        {
                        CD[j][i] = CDELT[j]*PC[j][i];
                        }
                    }
                CDELT[0] = Math.sqrt(CD[0][0]*CD[0][0] + CD[1][0]*CD[1][0]);
                CDELT[1] = Math.sqrt(CD[0][1]*CD[0][1] + CD[1][1]*CD[1][1]);
                }
            else if (!hasCD && !hasPC)
                {
                if (!hasCROTA[1])
                    {
                    icard = FitsJ.findCardWithKey ("BPA", hdr);
                    if (icard > 0)
                        {
                        CROTA[1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                        if (!Double.isNaN(CROTA[1])) hasPAkeyword = true;
                        }
                    if (!hasPAkeyword)
                        {
                        icard = FitsJ.findCardWithKey ("PA", hdr);
                        if (icard > 0)
                            {
                            CROTA[1] = -FitsJ.getCardDoubleValue(hdr.cards()[icard])+180.0;
                            if (!Double.isNaN(CROTA[1])) hasPAkeyword = true;
                            } 
                        }
                    }
                if (hasCROTA[1] || hasPAkeyword)
                    {
                    if (hasCDELT[0] || hasCDELT[1])
                        {
                        if (hasCDELT[0] && !hasCDELT[1]) CDELT[1] = CDELT[0];
                        else if (hasCDELT[1] && !hasCDELT[0]) CDELT[0] = CDELT[1];
                        }
                    else
                        {
                        icard = FitsJ.findCardWithKey ("XPIXSZ", hdr);
                        if (icard > 0)
                            {
                            PIXSZ[0] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                            if (!Double.isNaN(PIXSZ[0])) hasPIXSZ[0] = true;
                            }
                        icard = FitsJ.findCardWithKey ("YPIXSZ", hdr);
                        if (icard > 0)
                            {
                            PIXSZ[1] = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                            if (!Double.isNaN(PIXSZ[1])) hasPIXSZ[1] = true;
                            }   
                        if (hasPIXSZ[0] && !hasPIXSZ[1]) PIXSZ[1] = PIXSZ[0];
                        else if (hasPIXSZ[1] && !hasPIXSZ[0]) PIXSZ[0] = PIXSZ[1];
                        icard = FitsJ.findCardWithKey ("FOCALLEN", hdr);
                        if (icard > 0)
                            {
                            FOCALLEN = FitsJ.getCardDoubleValue(hdr.cards()[icard]);
                            if (!Double.isNaN(FOCALLEN)) hasFOCALLEN = true;
                            }
                        if ((hasPIXSZ[0] || hasPIXSZ[1]) && hasFOCALLEN)
                            {
                            CDELT[0] = 206.3*PIXSZ[0]/(FOCALLEN*3600);
                            CDELT[1] = 206.3*PIXSZ[1]/(FOCALLEN*3600);
                            }
                        }

                    if (CROTA[1] < 0.0)
                        CROTA[1] += 360.0;
                    if (CROTA[1] >= 360.0)
                        CROTA[1] -= 360.0;
                    double crot = Math.cos(CROTA[1]*D2R);
                    double srot;
                    if (CDELT[0]* CDELT[1] > 0)
                        srot = Math.sin(-CROTA[1]*D2R);
                    else
                        srot = Math.sin(CROTA[1]*D2R);                    
                    
                    
                    CD[0][0] = CDELT[0]*crot;
                    if (CDELT[0] < 0)
                        CD[0][1] = -Math.abs(CDELT[1])*srot;
                    else
                        CD[0][1] =  Math.abs(CDELT[1])*srot;
                    if (CDELT[1] < 0)
                        CD[1][0] =  Math.abs(CDELT[0])*srot;
                    else
                        CD[1][0] = -Math.abs(CDELT[0])*srot;
                    CD[1][1] =  CDELT[1]*crot;
                    hasDELROT = true;
                    }
                else if ((hasCDELT[0] || hasCDELT[1]) && hasRADEC)
                    {
                    if (hasCDELT[0] && !hasCDELT[1]) CDELT[1] = CDELT[0];
                    else if (hasCDELT[1] && !hasCDELT[0]) CDELT[0] = CDELT[1];  
                    CROTA[1] = 0.0;
                                        
                    CD[0][0] = CDELT[0];
                    CD[0][1] = 0.0;
                    CD[1][0] = 0.0;
                    CD[1][1] = CDELT[1];
                    hasCDELTOnly = true;                    
                    }
                }
            }
        
        //Assemble values used to forward and reverse transform celestial to/from native coordinates of a projection -- see WCStools
            // Euler angles for the transformation:
            // 0: Celestial longitude of the native pole, in degrees.
            // 1: Celestial colatitude of the native pole, or native colatitude of the celestial pole, in degrees.
            // 2: Native longitude of the celestial pole, in degrees.
            // 3: cos(eul[1])
            // 4: sin(eul[1])  

        eul[0] = CRVAL[0];
        eul[1] = 90.0 - CRVAL[1];
        eul[2] = LONPOLE;
        eul[3] = cosdeg(90.0 - CRVAL[1]);
        eul[4] = sindeg(90.0 - CRVAL[1]);  
        
        if (projection.equals("TAN") || projection.equals("TPV")||projection.equals("SIN")) zenithal = true;
          
        if (hasCD || hasPC || hasDELROT || hasCDELTOnly)
            {
            
            CDinv = invert(CD,NAXIS);
            if (CDinv == null)
                {
//                IJ.beep();
//                IJ.log("Error: Cannot invert WCS CD/PC matrix!");
                return;
                }     
            enoughInfo = true;

            
            xScale = Math.abs(CDELT[0]);
            yScale = Math.abs(CDELT[1]);
            
            
            
            /* Compute position angles of North and East in image */
            
            if (zenithal)
                {
                double[] cxy = {NAXES[0]/2f, NAXES[1]/2f};
                double[] cradec = pixels2wcs(cxy);
                double[] exy = wcs2pixels(new double[] {cradec[0]+cxy[0]*xScale, cradec[1]});
                double[] nxy = wcs2pixels(new double[] {cradec[0], cradec[1]+cxy[1]*yScale});                
                eastPA  = -90.0 + R2D*Math.atan2(-(exy[1]-cxy[1]), exy[0]-cxy[0]);
                northPA = -90.0 + R2D*Math.atan2(-(nxy[1]-cxy[1]), nxy[0]-cxy[0]);
                }
            else
                {
                eastPA = R2D*Math.atan2( CD[0][1], CD[0][0]) - 90.0 + (180.0 - LONPOLE);
                northPA = R2D*Math.atan2( CD[1][1], CD[1][0]) - 90.0 + (180.0 - LONPOLE);
                }           

            hasPA = true;
            hasScale = true;

            }

		// COORDINATE SYSTEM
//        IJ.log(""+enoughInfo);
        if (enoughInfo)
            {
            coordsys = (hasRADEC?"WCS=RA"+(hasSIP[0]?"-SIP":"")+",DEC"+(hasSIP[1]?"-SIP":""):"WCS RA/DEC not found")+
                    (!projection.equals("")?","+(zenithal?projection:projection+" projection not supported"):"")+
                    (hasCD?",CD matrix":(hasPC?",PC matrix":(hasDELROT?",PA+Scale":(hasCDELTOnly?",CDELT Only":""))))+
					("TPV".equals(projection) ? "PV distort not supported" : "");
            }
        else
            {
            coordsys = "No WCS";
            }
        writeLog();
		}


	/**
	 * Converts ImageJ pixel positions to coordinate system values.  Note that ImageJ pixels are assumed to
	 * be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] pixels2wcs (double[] pixels)
		{
		if (!enoughInfo)
			{
//			IJ.beep();
//			IJ.log ("Not enough info: "+logInfo);
			return null;
			}
        if (!hasRADEC) return new double[] {0.0, 0.0};

		int n=pixels.length;
		if (n != NAXIS)
			{
			IJ.beep();
			IJ.log ("Number of axes doesn't match: n="+n+", NAXIS="+NAXIS);
			return null;
			}

		// TRANSLATE IJ COORDINATES TO FITS COORDINATES
		double[] p = new double[n];	// IN PIXELS
		for (int j=0; j < n; j++)
			{
			if (j == 1)
				p[1] = NAXES[1] - (pixels[1] - Centroid.PIXELCENTER) - CRPIX[1]; 	
			else
				p[j] = (pixels[j] - Centroid.PIXELCENTER) + 1.0 - CRPIX[j];		
			}

		// NON-LINEAR DISTORTION CORRECTION USING SIP CORRECTION MATRICES (SEE SHUPE, D.L. ET AL. 2008)
		double px = p[0];
        double py = p[1];
//		if (useSIPA)
//			{
//			//CALCULATE SIP X-COORDINATE CORRECTION
//			double xCorrection = 0.0;
//			for (int qq=0; qq <= A_ORDER; qq++)
//				{
//				for (int pp=0; pp <= A_ORDER; pp++)
//					{
//					if (pp + qq <= A_ORDER)
//						{
//						xCorrection += A[pp][qq]*Math.pow(p0,(double)pp)*Math.pow(p[1],(double)qq);
//						}
//					}
//				}
//			p[0] += xCorrection;
//			}
		if (hasSIP[0] && (useSIPAlways || projection.equals("TAN") || projection.equals("TPV")))  //transcoded from WCSTools - avoids Math.pow operation
			{
            for (int j = 0; j <= A_ORDER; j++) 
                {
                S[j] = A[A_ORDER-j][j];
                for (int k = j-1; k >= 0; k--) 
                    {
                    S[j] = (py * S[j]) + A[A_ORDER-j][k];
                    }
                }

            double sum = S[0];
            for (int i=A_ORDER; i>=1; i--){
                sum = px*sum + S[A_ORDER-i+1];
                }
            p[0] = px + sum;
            }

//		if (useSIPB)
//			{
//			//CALCULATE SIP Y-COORDINATE CORRECTION
//			double yCorrection = 0.0;
//			for (int qq=0; qq <= B_ORDER; qq++)
//				{
//				for (int pp=0; pp <= B_ORDER; pp++)
//					{
//					if (pp + qq <= B_ORDER)
//						{
//						yCorrection += B[pp][qq]*Math.pow(p0,(double)pp)*Math.pow(p[1],(double)qq);
//						}
//					}
//				}
//			p[1] += yCorrection;
//			}
        
		if (hasSIP[1] && (useSIPAlways || projection.equals("TAN") || projection.equals("TPV")))  //transcoded from WCSTools - avoids Math.pow operation
			{
            for (int j=0; j<=B_ORDER; j++)
                {
                S[j] = B[B_ORDER-j][j];
                for (int k=j-1; k>=0; k--)
                    {
                    S[j] = py*S[j] + B[B_ORDER-j][k];
                    }
                }

                double sum = S[0];
                for (int i=B_ORDER; i>=1; i--)
                    sum = px*sum + S[B_ORDER-i+1];

                p[1] = py + sum;        
        
            }
		// CORRECT FOR ROTATION, SKEWNESS, SCALE

		double[] x = new double[n];	// IN PSUEDO DEGREES
		if (hasCD || hasPC || hasDELROT || hasCDELTOnly)
			{
			for (int j=0; j < n; j++)
				{
				for (int i=0; i < n; i++)
					x[j] += CD[j][i]*p[i];
				}
			}

		// PROJECTION PLANE COORDINATES x TO NATIVE SPHERICAL COORDINATES s
		double[] s = new double[n];	// IN PSEUDO DEGREES
		for (int j=0; j < n; j++)
			s[j] = x[j];
		if (projection.equals("TAN") || projection.equals("TPV"))
			{
			double Rtheta = Math.sqrt(x[0]*x[0]+x[1]*x[1]);
            if (Rtheta == 0.0)
                s[0] = 0.0;
            else
                s[0] = R2D*Math.atan2(x[0],-x[1]);		// NATIVE phi (E.G. R.A.) IN DEGS
			s[1] = R2D*Math.atan2(R2D,Rtheta);	// NATIVE theta~90 DEG (E.G. DEC.) IN DEGS
			}
		else if (projection.equals("SIN"))
			{            
            double tol = 1.0e-13;  //transcoded from WCSTools to include better handling of odd cases
            double x0 = x[0]/R2D;
            double y0 = x[1]/R2D;
            double p1 = PV[1][1];  //xi
            double p2 = PV[1][2];  //eta
            double r2 = x0*x0+y0*y0;
            double w1 = p1*p1+p2*p2;
            double w2 = w1+1.0;
            double w3 = w1-1.0;
            if (w1 == 0.0)  // Orthographic projection.
                {
                if (r2 != 0)
                    s[0] = atan2deg(x0, -y0); //phi
                else
                    s[0] = 0.0;
                
                if (r2 < 0.5)
                    s[1] = acosdeg(Math.sqrt(r2));  //theta
                else if (r2 <= 1.0)
                    s[1] = asindeg(Math.sqrt(1.0 - r2));
                else
                    s[1] = Double.NaN;
                }
            else          // "Synthesis" projection.
                {
                double x1 = p1;
                double y1 = p2;
                double sxy = x0*x1 + y0*y1;
                double sthe;
                double z;
                if (r2 < 1.0e-10) // Use small angle formula.
                    {
                    z = r2/2.0;
                    s[1] = 90.0 - R2D*Math.sqrt(r2/(1.0 + sxy));  //theta
                    }
                else 
                    {                
                    double a = w2;
                    double b = sxy - w1;
                    double c = r2 - sxy - sxy + w3;
                    double d = b*b - a*c;

                    /* Check for a solution. */
                    if (d < 0.0)
                        {
                        z = Double.NaN;
                        s[1] = Double.NaN;
                        }
                    else
                        {
                        d = Math.sqrt(d);

                        /* Choose solution closest to pole. */
                        double sth1 = (-b + d)/a;
                        double sth2 = (-b - d)/a;
                        sthe = (sth1 > sth2) ? sth1 : sth2;
                        if (sthe > 1.0)
                            {
                            if (sthe-1.0 < tol)
                                sthe = 1.0;
                            else
                                sthe = (sth1 < sth2) ? sth1 : sth2;
                            }
                        if (sthe < -1.0 && sthe+1.0 > -tol) sthe = -1.0;
                        if (sthe > 1.0 || sthe < -1.0) sthe = Double.NaN;
                        s[1] = asindeg(sthe);
                        z = 1.0 - sthe;
                        }
                    }
                double xp = -y0 + p2*z;
                double yp =  x0 - p1*z;
                if (xp == 0.0 && yp == 0.0)
                    s[0] = 0.0;
                else
                    s[0] = atan2deg(yp,xp);  //phi
                }
            }

        
        
		// NATIVE SPHERICAL COORDINATES s TO CELESTIAL COORDINATES c : SEE EQN.2 OF Calabretta & Greisen 2002, P. 1079
		double[] c = new double[n];
		for (int j=0; j < n; j++)  //default linear transform
			c[j] = s[j]+CRVAL[j];

		if (zenithal)
			{            
            //transcoded from WCSTools to include better handling of odd cases
			double phi = s[0];		// NATIVE PHI IN DEGREES
			double theta = s[1];	// NATIVE THETA IN DEGREES          
            double tol = 1.0e-5;
            double lng, lat;
            double cosphi, costhe, dlng, dphi, sinphi, sinthe, xx, yy, zz;
            
            costhe = cosdeg (theta);
            sinthe = sindeg (theta);

            dphi = phi - eul[2];
            cosphi = cosdeg (dphi);
            sinphi = sindeg (dphi);

            /* Compute the celestial longitude. */
            xx = sinthe*eul[4] - costhe*eul[3]*cosphi;
            if (Math.abs(xx) < tol)  /* Rearrange formula to reduce roundoff errors. */
                xx = -cosdeg (theta+eul[1]) + costhe*eul[3]*(1.0 - cosphi);

            yy = -costhe*sinphi;
            if (xx != 0.0 || yy != 0.0)
                dlng = atan2deg (yy, xx);
            else
                /* Change of origin of longitude. */
                dlng = dphi + 180.0;

            lng = eul[0] + dlng;

            /* Normalize the celestial longitude. */
            if (eul[0] >= 0.0)
                {
                if (lng < 0.0) lng += 360.0;
                }
            else
                {
                if (lng > 0.0) lng -= 360.0;
                }

            if (lng > 360.0)
                lng -= 360.0;
            else if (lng < -360.0)
                lng += 360.0;

            /* Compute the celestial latitude. */
            if (dphi%180.0 == 0.0) 
                {
                lat = theta + cosphi*eul[1];
                if (lat >  90.0) lat =  180.0 - lat;
                if (lat < -90.0) lat = -180.0 - lat;
                }
            else
                {
                zz = sinthe*eul[3] + costhe*eul[4]*cosphi;

                /* Use an alternative formula for greater numerical accuracy. */
                if (Math.abs(zz) > 0.99) 
                    {
                    if (zz < 0)
                            lat = -acosdeg (Math.sqrt(xx*xx+yy*yy));
                    else
                            lat =  acosdeg (Math.sqrt(xx*xx+yy*yy));
                    }
                else
                    {
                    lat = asindeg(zz);
                    }
                }

            c[0] = lng;
            c[1] = lat;
			}
        if (epoch != 2000.0)
            {
            double[] radecJ2000;
            if (epoch == 1950)
                {
                radecJ2000 = SkyAlgorithms.B1950toJ2000(c[0]/15.0, c[1], 0, 0, false); 
                }
            else
                {
                radecJ2000 = SkyAlgorithms.Convert(2000.0, SkyAlgorithms.CalcJD((int)epoch, 1, 1, 0), c[0]/15.0, c[1], 0, 0, 32,
                                false, false, true, true, true);
                }
            c[0] = radecJ2000[0]*15.0;
            c[1] = radecJ2000[1];
            }
   
		return c;
		}

	/**
	 * Converts coordinate system values to ImageJ pixel positions.  Note that ImageJ pixels are assumed to
	 * be shifted by Centroid.PIXELCENTER pixels relative to the FITS standard.
	 */
	public double[] wcs2pixels (double[] coordPair)
		{

		if (! enoughInfo || coordPair.length != 2)
			{
			IJ.beep();
			IJ.log ("Not enough info: "+logInfo);
			return null;
			}
        double c[] = new double[2];
        c[0] = coordPair[0];
        c[1] = coordPair[1];
        if (!hasRADEC) return new double[] {0.0, 0.0};

		int n=c.length;
		if (n != NAXIS)
			{
			IJ.beep();
			IJ.log ("Number of axes doesn't match: n="+n+", NAXIS="+NAXIS);
			return null;
			}
        
        if (epoch != 2000.0)
            {
            double[] radecJ2000;
            if (epoch == 1950.0)
                {
                radecJ2000 = SkyAlgorithms.J2000toB1950(c[0]/15.0, c[1], 0, 0, false); 
                }
            else
                {
                radecJ2000 = SkyAlgorithms.Convert(2000.0, SkyAlgorithms.CalcJD((int)epoch, 1, 1, 0), c[0]/15.0, c[1], 0, 0, 32,
                                true, false, true, true, true);
                }
            c[0] = radecJ2000[0]*15.0;
            c[1] = radecJ2000[1];
            }        

		// CELESTIAL c to NATIVE SPHERICAL s: EQN.6 OF Calabretta & Greisen 2002, P. 1079
		double[] s = new double[n];
		for (int j=0; j < n; j++)
			s[j] = c[j];				// RA,DEC IN DEG
        
		if (zenithal)
			{
            double lng = c[0];
            double lat = c[1]; 
            double coslat, coslng, dlng, dphi, sinlat, sinlng, xx, yy, zz;
            double tol = 1.0e-5;

            coslat = cosdeg (lat);
            sinlat = sindeg (lat);

            dlng = lng - eul[0];
            coslng = cosdeg (dlng);
            sinlng = sindeg (dlng);

            /* Compute the native longitude. */
            xx = sinlat*eul[4] - coslat*eul[3]*coslng;
            if (Math.abs(xx) < tol) {
                /* Rearrange formula to reduce roundoff errors. */
                xx = -cosdeg (lat+eul[1]) + coslat*eul[3]*(1.0 - coslng);
            }
            yy = -coslat*sinlng;
            if (xx != 0.0 || yy != 0.0)
                dphi = atan2deg (yy, xx);
            else /* Change of origin of longitude. */
                dphi = dlng - 180.0;

            s[0] = eul[2] + dphi;  // NATIVE PHI - longitude

            /* Normalize the native longitude. */
            if (s[0] > 180.0)
                s[0] -= 360.0;
            else if (s[0] < -180.0)
                s[0] += 360.0;

            /* Compute the native latitude. */
            if (dlng % 180.0 == 0.0) 
                {
                s[1] = lat + coslng*eul[1];    // NATIVE THETA - latitude
                if (s[1] >  90.0) s[1] =  180.0 - s[1];
                if (s[1] < -90.0) s[1] = -180.0 - s[1];
                }
            else 
                {
                zz = sinlat*eul[3] + coslat*eul[4]*coslng;
                /* Use an alternative formula for greater numerical accuracy. */
                if (Math.abs(zz) > 0.99)
                    {
                    if (zz < 0)
                        s[1] = -acosdeg (Math.sqrt(xx*xx+yy*yy));
                    else
                        s[1] =  acosdeg (Math.sqrt(xx*xx+yy*yy));
                    } 
                else 
                    {
                    s[1] = asindeg (zz);
                    }
                }
            }

		// NATIVE SPHERICAL s TO PROJECTION PLANE x
		double[] x = new double[n];
		for (int j=0; j < n; j++)
			x[j] = s[j];				// IN DEGS
		if (projection.equals("TAN") || projection.equals("TPV"))
			{
			double Rtheta = R2D/Math.tan(s[1]/R2D);	// IN DEGS (EQN. 54)
			x[0] =  Rtheta*Math.sin(s[0]/R2D);		// IN DEGS (EQN. 12)
			x[1] = -Rtheta*Math.cos(s[0]/R2D);		// IN DEGS (EQN. 13)
			}
        else if (projection.equals("SIN"))
            {
            double phi = s[0];
            double theta = s[1];
            double cphi, cthe, sphi, t, z;

            t = (90.0 - Math.abs(theta))*D2R;
            if (t < 1.0e-5) {
                if (theta > 0.0) {
                    z = t*t/2.0;
                } else {
                    z = 2.0 - t*t/2.0;
                }
                cthe = t;
            } else {
                z =  1.0 - sindeg (theta);
                cthe = cosdeg (theta);
            }

            cphi = cosdeg (phi);
            sphi = sindeg (phi);
            x[0] =  R2D*(cthe*sphi + PV[1][1]*z);
            x[1] = -R2D*(cthe*cphi - PV[1][2]*z);
            }
       

		// FINAL COORDINATES CORRECTED FOR ROTATION, SKEWNESS, SCALE
		double[] p = new double[n];	// IN PSEUDO DEGREES
		if ((hasCD || hasPC || hasDELROT || hasCDELTOnly) && CDinv != null)
			{
			for (int j=0; j < n; j++)
				{
				for (int i=0; i < n; i++)
					p[j] += CDinv[j][i]*x[i];
				}
			}
		double px = p[0];
        double py = p[1];        
        
		// NON-LINEAR DISTORTION CORRECTION USING SIP INVERSE CORRECTION MATRICES (SEE SHUPE, D.L. ET AL. 2008)

		if (hasSIPinv[0] && (useSIPAlways || projection.equals("TAN") || projection.equals("TPV")))  //transcoded from WCSTools - avoids Math.pow operation
			{
            for (int j = 0; j <= AP_ORDER; j++) 
                {
                SP[j] = AP[AP_ORDER-j][j];
                for (int k = j-1; k >= 0; k--) 
                    {
                    SP[j] = (py * SP[j]) + AP[AP_ORDER-j][k];
                    }
                }

            double sum = SP[0];
            for (int i=AP_ORDER; i>=1; i--){
                sum = px*sum + SP[AP_ORDER-i+1];
                }
            p[0] = px + sum;
			//IJ.log("AP_ORDER="+AP_ORDER);
            }

		if (hasSIPinv[1] && (useSIPAlways || projection.equals("TAN") || projection.equals("TPV")))  //transcoded from WCSTools - avoids Math.pow operation
			{
            for (int j=0; j<=BP_ORDER; j++)
                {
                SP[j] = BP[BP_ORDER-j][j];
                for (int k=j-1; k>=0; k--)
                    {
                    SP[j] = py*SP[j] + BP[BP_ORDER-j][k];
                    }
                }

			double sum = SP[0];
			for (int i=BP_ORDER; i>=1; i--)
				sum = px*sum + SP[BP_ORDER-i+1];

			p[1] = py + sum;
        	//IJ.log("BP_ORDER="+BP_ORDER);
            }        

		// TRANSLATE FITS COORDINATES TO ImageJ COORDINATES
		double[] pixels = new double[n];	// IN PIXELS
		for (int j=0; j < n; j++)
			{
			if (j == 1)			// Y-AXIS IS UPSIDE-DOWN!
				pixels[1] = NAXES[1] - p[1] + Centroid.PIXELCENTER - CRPIX[1]; 	
			else
				pixels[j] = p[j] + Centroid.PIXELCENTER - 1.0 + CRPIX[j];		
			}
		return pixels;
		}
    

	public double[][] invert (double[][] m, int n)
		{
		if (m == null || n != 2) return null;
		double detm = m[0][0]*m[1][1]-m[0][1]*m[1][0];
        double[][] inv = new double[2][2];
		if (detm == 0.0)
            {
            inv[0][0] = 0;
            inv[0][1] = 0;
            inv[1][0] = 0;
            inv[1][1] = 0;
            }
		else
            {
            inv[0][0] =  m[1][1]/detm;
            inv[0][1] = -m[0][1]/detm;
            inv[1][0] = -m[1][0]/detm;
            inv[1][1] =  m[0][0]/detm;
            }
		return inv;
		}

	public void initialize (int naxis)
		{
		NAXIS = naxis;
        WCSAXES=naxis;
		if (naxis == 0) return;
		NAXES = new int[NAXIS];
		CDELT = new double[NAXIS];
        CROTA = new double[NAXIS];
        PIXSZ = new double[NAXIS];
        hasCDELT = new boolean[NAXIS];
        hasCROTA = new boolean[NAXIS];
        hasPIXSZ = new boolean[NAXIS];
        hasSIP = new boolean[NAXIS];
        hasSIPinv = new boolean[NAXIS];
		CRPIX = new double[NAXIS];
		CRVAL = new double[NAXIS];
		PC = new double[NAXIS][NAXIS];
		CD = new double[NAXIS][NAXIS];
        PV = new double[NAXIS][100];
		CTYPE = new String[NAXIS];
		CUNIT = new String[NAXIS];
        eul = new double[5];
		for (int i=0; i < NAXIS; i++)
			{
			NAXES[i]=0;
			CDELT[i]=1.0;
            CROTA[i]=0.0;
            PIXSZ[i]=1.0;
            hasCDELT[i]=false;
            hasCROTA[i]=false;
            hasPIXSZ[i]=false;
            hasSIP[i]=false;
            hasSIPinv[i]=false;
			CRPIX[i]=0.0;
			CRVAL[i]=0.0;
			PC[i][i]=1.0;
			CD[i][i]=1.0;
			}
        for (int j=0; j < SIP_MAX; j++)
            {
            S[j] = 0.0;
            SP[j] = 0.0;
            }        
        northPA = 90;
        eastPA = 180;
        xScale = 1.0;
        yScale = 1.0;
        epoch = 2000.0;
        FOCALLEN = 1.0;
        PA=0.0;
        hasPA=false;
		typeContainsSIP=false;
		hasCD=false;
        hasPC=false;
        hasDELROT=false;
        hasCDELTOnly=false;
        hasFOCALLEN=false;
        hasPAkeyword=false;
        zenithal=false;
		coordsys = "No WCS";
		projection = "";
		}    
    
    void writeLog()
        {
        //IJ.log(logInfo);
        }
    
    public double getWCSDistance(double x1pix, double y1pix, double x2pix, double y2pix)
        {
        return getWCSDistance(pixels2wcs(new double[] {x1pix, y1pix}), pixels2wcs(new double[] {x2pix, y2pix}));
        }    
    
    public double getWCSDistance(double[] startRaDec, double[] endRaDec)
        {  // transcoded from wcs tools
        double distance = 0.0;
        double ra1 = startRaDec[0]*D2R;
        double dec1 = startRaDec[1]*D2R;
        double ra2 = endRaDec[0]*D2R;
        double dec2 = endRaDec[1]*D2R;        
        double dra = ra2-ra1;
        if (dra >  Math.PI/2.0) dra -= Math.PI;
        if (dra < -Math.PI/2.0) dra += Math.PI;
        double cosdec1 = Math.cos(dec1);
        double sindec1 = Math.sin(dec1);
        double cosdec2 = Math.cos(dec2);
        double sindec2 = Math.sin(dec2);
        double sindra = Math.sin(dra);
        double cosdra = Math.cos(dra);
        double topL2 = cosdec2*cosdec2*sindra*sindra;
        double topR2 = (cosdec1*sindec2-sindec1*cosdec2*cosdra)*(cosdec1*sindec2-sindec1*cosdec2*cosdra);
        distance = R2D*Math.atan2(Math.sqrt(topL2+topR2), sindec1*sindec2+cosdec1*cosdec2*cosdra);        
        return distance;
        }
    
    
    public double getWCSDistance2(double[] startRaDec, double[] endRaDec)
        {  // transcoded from wcs tools
        double distance = 0.0;
        double d1, d2, diffi;
        double[] pos1 = new double[3];
        double[] pos2 = new double[3];
        double w = 0.0;


        /* Convert two vectors to direction cosines */

        pos1[0] = Math.cos(startRaDec[0]*D2R) * Math.cos(startRaDec[1]*D2R);
        pos1[1] = Math.sin(startRaDec[0]*D2R) * Math.cos(startRaDec[1]*D2R);
        pos1[2] = Math.sin(startRaDec[1]*D2R);  
        
        pos2[0] = Math.cos(endRaDec[0]*D2R) * Math.cos(endRaDec[1]*D2R);
        pos2[1] = Math.sin(endRaDec[0]*D2R) * Math.cos(endRaDec[1]*D2R);
        pos2[2] = Math.sin(endRaDec[1]*D2R);        

        /* Modulus squared of half the difference vector */

        for (int i = 0; i < 3; i++) {
            diffi = pos1[i] - pos2[i];
            w += (diffi * diffi);
            }
        w /= 4.0;
        if (w > 1.0) w = 1.0;

        /* Angle beween the vectors */
        distance = R2D*2.0*Math.atan2 (Math.sqrt(w), Math.sqrt(1.0 - w));
        return distance;
        }
    
    public double getWCSPA(double[] startRaDec, double[] endRaDec)
        {  
        // Transcoded from idlastro posang.pro
        //
        // PURPOSE:
        //       Computes rigorous position angle of end ra/dec relative to start ra/dec.
        //       The input ra and dec and output angle are in decimal degrees. 
        //
        //   OUTPUTS:
        //       ANGLE-- Angle of the great circle containing [ra2, dec2] from
        //               the meridian containing [ra1, dec1], in the sense north
        //               through east rotating about [ra1, dec1].  
        //
        //   PROCEDURE:
        //       The "four-parts formula" from spherical trig (p. 12 of Smart's
        //       Spherical Astronomy or p. 12 of Green' Spherical Astronomy).
        //
        //   EXAMPLE:
        //       For the star 56 Per, the Hipparcos catalog gives a position of 
        //       RA = 66.15593384, Dec = 33.94988843 for component A, and 
        //       RA = 66.15646079, Dec =  33.96100069 for component B.   What is the
        //       position angle of B relative to A?
        //
        //       RA1 = 66.15593384/15.d, Dec1 = 33.95988843,
        //       RA2 = 66.15646079/15.d, Dec2 = 33.96100069,
        //       gives angle = 21.4 degrees.
        //       Note that angle is not commutative -- if the position angle between
        //       A and B is theta, then the position angle between B and A is 180+theta 
        //   HISTORY:
        //       Modified from GCIRC, R. S. Hill, RSTX, 1 Apr. 1998
        //       Use V6.0 notation W.L. Mar 2011

        double angle;
        double ra1    = startRaDec[0];
        double dc1    = startRaDec[1];
        double ra2    = endRaDec[0];
        double dc2    = endRaDec[1];
        double d2r    = Math.PI/180.0;
        double rarad1 = ra1*d2r;
        double rarad2 = ra2*d2r;
        double dcrad1 = dc1*d2r;
        double dcrad2 = dc2*d2r;
        double radif  = rarad2-rarad1;        
        if ((ra1 == ra2) && (dc1 == dc2))
            {
            angle = 0.0;
            }
        else
            {
            angle  = (360 + Math.atan2(Math.sin(radif),Math.cos(dcrad1)*Math.tan(dcrad2)-Math.sin(dcrad1)*Math.cos(radif))/d2r) % 360;
            }
        return angle;
        }
    
    
	public double getEastPA()
		{
		return eastPA;
		}  
    
    public double getNorthPA()
		{
		return northPA;
		}
    
    public double getXScale()
		{
        return xScale*FFIScale;
		}    

    public double getYScale()
		{
        return yScale*FFIScale;
		}
   
    public double getXScaleArcSec()
		{
        return xScale*3600.0*FFIScale;
        }
    
    public double getYScaleArcSec()
		{
        return yScale*3600.0*FFIScale;
        }

    
//    public double getRotation()
//        {
//        double rotA = 0;
//        double rotB = 0;
//        double rot = 0;
//        double[][] c = CD;
//        
//        if (!useCD)
//            {
//            c[0][0] = PC[0][0]*CDELT[0];
//            c[0][1] = PC[0][1]*CDELT[0];
//            c[1][0] = PC[1][0]*CDELT[1];
//            c[1][1] = PC[1][1]*CDELT[1];
//            }
//
//        if      (c[1][0] > 0) rotA = Math.atan2( c[1][0], c[0][0])*R2D;
//        else if (c[1][0] < 0) rotA = Math.atan2(-c[1][0],-c[0][0])*R2D;
//        else                  rotA = 0;
////        IJ.log("rotA = "+rotA);
//        if      (c[0][1] > 0) rotB = Math.atan2( c[0][1],-c[1][1])*R2D;
//        else if (c[0][1] < 0) rotB = Math.atan2(-c[0][1], c[1][1])*R2D;
//        else                  rotB = 0;
//        IJ.log("rotB = "+rotB);
////        rot = (rotA + rotB)/2.0;
//
////        IJ.log("rot = "+rot);
//        return rot;
//        }
    

	public boolean hasRaDec ()
		{
		return hasRADEC;
		}
    
	public boolean hasWCS ()
		{
		return enoughInfo;
		}
    
    public void setUseSIPAlways(boolean useSIP)
        {
        useSIPAlways = useSIP;
        }

	public void repair (ImagePlus img, double[] crpix, double[][] cd)
		{
		NAXIS=0;
		var hdr = FitsJ.getHeader (img);
		if (hdr == null) return;

		// FIX CTYPEn KEYWORDS
		int icard=FitsJ.findCardWithKey("CTYPE1",hdr);
		int jcard=FitsJ.findCardWithKey("CTYPE2",hdr);
		if (icard < 0 && jcard < 0)
			{
			hdr = FitsJ.setCard("CTYPE1","RA---TAN","coordinate type (WCS repair)",hdr);
			hdr = FitsJ.setCard("CTYPE2","DEC--TAN","coordinate type (WCS repair)",hdr);
			}

		// FIX CRVALn
		icard=FitsJ.findCardWithKey("CRVAL1",hdr);
		jcard=FitsJ.findCardWithKey("CRVAL2",hdr);
		if (icard < 0 && jcard < 0)
			{
			icard=FitsJ.findCardWithKey("RA",hdr);
			jcard=FitsJ.findCardWithKey("DEC",hdr);
			if (icard > 0 && jcard > 0)
				{
				try	{
					double ra = DmsFormat.unformat(FitsJ.getCardStringValue(hdr.cards()[icard]));
					double dec = DmsFormat.unformat(FitsJ.getCardStringValue(hdr.cards()[jcard]));
					hdr = FitsJ.setCard("CRVAL1",ra*15.0,"Right Ascension in decimal degrees (WCS repair)",hdr);
					hdr = FitsJ.setCard("CRVAL2",dec,"Declination in decimal degrees (WCS repair)",hdr);
					}
				catch (NumberFormatException e)
					{
					IJ.error("Cannot parse RA, DEC entries in FITS header!");
					return;
					}
				}
			else	{
				IJ.error("Cannot find RA,DEC in FITS header!");
				return;
				}
			}
		
		// FIX CRPIXn
		icard=FitsJ.findCardWithKey("CRPIX1",hdr);
		jcard=FitsJ.findCardWithKey("CRPIX2",hdr);
		if (icard < 0 && jcard < 0)
			{
			hdr = FitsJ.setCard("CRPIX1",crpix[0],"reference pixel coordinate (WCS repair)",hdr);
			hdr = FitsJ.setCard("CRPIX2",crpix[1],"reference pixel coordinate (WCS repair)",hdr);
			}

		// FIX CDij
		icard=FitsJ.findCardWithKey("CD1_1",hdr);
		if (icard < 0)
			{
			hdr = FitsJ.setCard("CD1_1",cd[0][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD1_2",cd[0][1],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD2_1",cd[1][0],"transformation matrix element (WCS repair)",hdr);
			hdr = FitsJ.setCard("CD2_2",cd[1][1],"transformation matrix element (WCS repair)",hdr);
			}

		// SAVE RESULT
		FitsJ.putHeader(img,hdr);

		// TRY AGAIN
		process(img.getShortTitle(),img.getWidth(),img.getHeight(),img.getStackSize(),hdr);
		}
    
double cosdeg(double angle)
    {
    double resid = Math.abs(angle%360.0);
    if (resid == 0.0)
        return 1.0;
    else if (resid == 90.0)
        return 0.0;
    else if (resid == 180.0)
        return -1.0;
    else if (resid == 270.0)
        return 0.0;
    return Math.cos(angle*D2R);
    }

/*--------------------------------------------------------------------------*/

double sindeg(double angle)
    {
    double resid = (angle-90.0)%360.0;
    if (resid == 0.0)
        return 1.0;
    else if (resid == 90.0)
        return 0.0;
    else if (resid == 180.0)
        return -1.0;
    else if (resid == 270.0)
        return 0.0;
    return Math.sin(angle*D2R);
    }

/*--------------------------------------------------------------------------*/

double tandeg(double angle)
    {
    double resid = angle%360.0;
    if (resid == 0.0 || Math.abs(resid) == 180.0)
        return 0.0;
    else if (resid == 45.0 || resid == 225.0)
        return 1.0;
    else if (resid == -135.0 || resid == -315.0)
        return -1.0;
    return Math.tan(angle*D2R);
    }

/*--------------------------------------------------------------------------*/

double acosdeg(double v)
    {
    if (v >= 1.0)
        {
        if (v-1.0 < WCSTRIG_TOL) return 0.0;
        }
    else if (v == 0.0)
        {
        return 90.0;
        }
    else if (v <= -1.0)
        {
        if (v+1.0 > -WCSTRIG_TOL) return 180.0;
        }
    return Math.acos(v)*R2D;
    }

/*--------------------------------------------------------------------------*/

double asindeg(double v)
    {
    if (v <= -1.0) 
        {
        if (v+1.0 > -WCSTRIG_TOL) return -90.0;
        }
    else if (v == 0.0)
        {
        return 0.0;
        }
    else if (v >= 1.0)
        {
        if (v-1.0 <  WCSTRIG_TOL) return 90.0;
        }
    return Math.asin(v)*R2D;
    }

/*--------------------------------------------------------------------------*/

double atandeg(double v)
    {
    if (v == -1.0) 
        {
        return -45.0;
        }
    else if (v == 0.0) 
        {
        return 0.0;
        }
    else if (v == 1.0)
        {
        return 45.0;
        }
    return Math.atan(v)*R2D;
    }

/*--------------------------------------------------------------------------*/

double atan2deg(double y, double x)
    {
    if (y == 0.0) 
        {
        if (x >= 0.0)
            return 0.0;
        else if (x < 0.0)
            return 180.0;
        }
    else if (x == 0.0) 
        {
        if (y > 0.0)
            return 90.0;
        else if (y < 0.0)
            return -90.0;
        }
    return Math.atan2(y,x)*R2D;
	}
}
