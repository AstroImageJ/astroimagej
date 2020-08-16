package Astronomy; /**Adapted by Karen Collins from plugin generating contour lines with equal spacing
* on top of an image (using overlay). Originally written by Eugene Katrukha. 
* see https://imagej.net/Contour_Lines
* Based/inspired by streamlines project by Andrei Kashcha and published at
* http://web.cs.ucdavis.edu/~ma/SIGGRAPH02/course23/notes/papers/Jobard.pdf
*/

import astroj.ContourPoint;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class ContourLines_ implements PlugIn {
	/** main image window */
	public ImagePlus imp; 	
	/** currently active processor */
	public ImageProcessor ip;
	/** smoothing radius used to calculate gradient **/
	float dSmoothR;
	/** derivatives of gaussian (first order in x and y)**/
	ImageProcessor fx,fy;
	/**normalized vector field of gradient**/
	float [][][] fNormalizedField;
	/** integration timestep **/
	float dTimeStep;
	
	
	/** grid of occupied spaces **/
	List[][] Grid;// = new List<String>[2];
	
	/** grid size **/
	int nWG,nHG;

	/** new seed points coordinates **/
	ArrayList<Float []> fSeedPoints = new ArrayList<Float []>(); 
	
	/** separation distance between contour lines **/
	float dSep;
	/** stop line integration when it is closer than (dStopLineSep * dSep) **/
	float dStopLineSep;
	
	/** ignore open contours **/
	boolean bRemoveOpen;
	float dGridSize;
	/** image size **/
	int nW,nH;

	/** chosen LUT name**/
	String sLUTName;

	/** LUT colors array **/	
	int [][] RGBLutTable;
	
	/** whether to invert LUT**/
	boolean bInvertLUT;
	
	/** whether to use single color or LUT **/
	boolean bSingleColor;
	
	/** selected color from Color Picker/Toolbar **/
	Color systemColor;
	
	//double dStrokeWidth;
	
	/** overlay for render **/
	Overlay image_overlay; 

	/** min and max intensities of the image taken from Contrast/Brightness**/
	float fInMin;
	float fInRange;
	float fInMax;
	
	/** initial seed for the first line,
	 * point with the minimum gradient **/
	ContourPoint iniSeed;
	
	/** total number of contours **/
	int nLines;
	
	@Override
	public void run(String arg) {
		
		PolygonRoi new_line;
		
		/** time measuring variables**/
		long startTime;
		long contourTime=0;
		
		
		imp = IJ.getImage();
		if(null == imp)
		{
			IJ.noImage();
			
			return;
		}
		else if (imp.getType() != ImagePlus.GRAY8 && imp.getType() != ImagePlus.GRAY16 && imp.getType() != ImagePlus.GRAY32) 
		{
		    IJ.error("8, 16 or 32-bit grayscale image  required");
		    return;
		}	
		//ask for parameters
		if(!showParametersDialog())
			return;
		
		IJ.log("Countour Lines v.0.0.4 plugin");
		IJ.log("Parameters:");
		IJ.log("Smoothing radius: "+Float.toString(dSmoothR)+" pixels");
		IJ.log("Line integration step: "+Float.toString(dTimeStep)+" pixels");
		IJ.log("Distance between lines: "+Float.toString(dSep)+" pixels");
		IJ.log("Stop line at fraction of distance: "+Float.toString(dStopLineSep));
		
		if(bSingleColor)
			IJ.log("Using single color");
		else
		{
			if(bInvertLUT)
				IJ.log("Using "+ sLUTName +" LUT (inverted)");
			else
				IJ.log("Using "+ sLUTName);
		}
		if(bRemoveOpen)
		{
			IJ.log("Open contours are removed");
		}
		//let's start measuring time		
		startTime = System.nanoTime();
				
		nW=imp.getWidth();
		nH=imp.getHeight();
		
		
		ip=imp.getProcessor();

		/**grid size **/
		dGridSize=0.5f*dSep;
		nWG=(int)Math.ceil(nW/(float)dGridSize);
		nHG=(int)Math.ceil(nH/(float)dGridSize);

		Grid = new ArrayList[nWG][nHG];

		/**derivatives of gaussian calculation **/
		
		double[][] kernel = computeKernelGaussian2D_du(dSmoothR,dSmoothR, 0.0f);		
		fx= convolve(ip, kernel);
		//new ImagePlus("dx", fx).show();
		kernel = computeKernelGaussian2D_dv(dSmoothR,dSmoothR, 0.0f);
		fy= convolve(ip, kernel);		
		//new ImagePlus("dy", fy).show();
		
		//normalizing gradient field
		fNormalizedField  = new float [nW][nH][2];
		fillNormalizedField();
		
		image_overlay = new Overlay();
		
		//setting up contours colors
		if (bSingleColor)
			systemColor = Toolbar.getForegroundColor();
		else
		{

			getRGBLutTable();
			fInMin = (float) ip.getMin();
			fInMax=(float) ip.getMax();
			fInRange = fInMax -fInMin;
			
		}
		
		nLines=0;
		//first line
		new_line=buildLine(iniSeed.getX(),iniSeed.getY());
		if(new_line!=null)
		{
			//add to overlay
			image_overlay.add(new_line);		
			nLines++;
			IJ.log("Line "+ Integer.toString(nLines)+" added.");
		}

		//show
		imp.setOverlay(image_overlay);
		imp.updateAndRepaintWindow();
		imp.show();
		
		//using seeds from the first contour line
		if (generateLinesFromSeedList())
		{
			//check seeds from empy cells
			refillSeedList();
			//another try! hey, hey!
			generateLinesFromSeedList();
		}
		
		IJ.log("Done");
		contourTime = System.nanoTime() - startTime;
		IJ.log("Total time: " + String.format("%.2f",((double)Math.abs(contourTime))*0.000000001) + " s");
		
	}
	
	/** generate lines using fSeedPoints queue **/
	boolean generateLinesFromSeedList()
	{
		
		//going through the queue of seed points
		//generated by lines
		PolygonRoi new_line;
		boolean goOn=true;
		boolean bFoundSeed; 
		Float [] seed_point = new Float [2];
		// until queue is empty
		while (goOn)
		{
			if(fSeedPoints.size()==0)
			{
				goOn=false;
			}
			else
			{
				bFoundSeed = false;
			
				while (!bFoundSeed)
				{
					//queue is empty, finish
					if(fSeedPoints.size()==0)
					{
						goOn = false;
						break;
					}
					seed_point= fSeedPoints.get(0);

					if(isBusy(seed_point[0],seed_point[1],0.9f*dSep))
					{
						//seed point is too close to another contour line,
						//kick out the seed from queue
						fSeedPoints.remove(0);
					}
					else
					{
						//seed point is fine
						bFoundSeed=true;				
						fSeedPoints.remove(0);
					}
				}
				//generate a contour line from seed point
				if(goOn && bFoundSeed)
				{
					new_line=buildLine(seed_point[0],seed_point[1]);
					if (new_line!=null)
					{
						if(new_line.size()>2)
						{
							image_overlay.add(new_line);
						
							nLines++;
							imp.setOverlay(image_overlay);
							imp.updateAndRepaintWindow();
							imp.show();
							IJ.log("Line "+ Integer.toString(nLines)+" added.");
							if(!imp.isProcessor())
							{
								IJ.log("Image closed. Terminating.");
								return false;
							}
						}
					}
				}
			}
		}
		return true;

	}
	/** line integration routine
	 *  uses simple Newton method with step defined by user (integration step)**/
	public PolygonRoi buildLine(float xstart, float ystart)
	{
		/**Own grid of current line to check for self-intersections.
		 * It is equal to image size. Precision of more than 1 pixel seems unnecessary **/
		int [][] ownGrid  = new int [nW][nH];
		
		PolygonRoi polyline;
		float xcurr, ycurr;
		float xvel,yvel;
		ArrayList<Float> px = new ArrayList<Float>(); 
		ArrayList<Float> py = new ArrayList<Float>();

		int gx, gy;
		int gxn, gyn;
		boolean bEnd;
		
		px.add(xstart);
		py.add(ystart);
		int nDirection;
		//The line will be grown in forward and backward direction.
		// This variable specifies it.
		nDirection =1;
		
		for(nDirection=1;nDirection>-2;nDirection-=2)
		{
				
			bEnd = false;
			xcurr=xstart;
			ycurr=ystart;
			//mark OwnGrid pixel as current (1)
			//it means we can grow line within it
			gx = (int) Math.floor(xstart);
			gy = (int) Math.floor(ystart);
			ownGrid[gx][gy]=1;

			while (!bEnd)
			{
				//gradient at current position (+/- depends on nDirection)
				xvel = ((float)nDirection)*fNormalizedField[Math.round(xcurr)][Math.round(ycurr)][0];
				yvel = ((float)nDirection)*fNormalizedField[Math.round(xcurr)][Math.round(ycurr)][1];
				//check it is not a sink or source or just zero pixelk
				if(Float.isNaN((Float)xvel))
				{
					bEnd=true;
					ownGrid[gx][gy]=2;
				}
				else
				{
				if(Float.isNaN((Float)xcurr))
				{
					bEnd = true;
					ownGrid[gx][gy]=2;
				}
				else
				{
					//grow a line by integration step											
					xcurr = xcurr +xvel * dTimeStep;
					ycurr = ycurr +yvel * dTimeStep;

					//line is close to another line, abort growth
					if(isBusy(xcurr,ycurr,dSep*dStopLineSep))
					{
						bEnd = true;
						ownGrid[gx][gy]=2;
					}
					else
					{
						//let's check self-intersection
						gxn = (int) Math.floor(xcurr);
						gyn = (int) Math.floor(ycurr);
						//out of image, abort!
						if(gxn<0 || gyn<0 || gxn>(nW-1)|| gyn>(nH-1))
						{
							bEnd = true;
						}
						else
						{
							//self-intersection, abort!
							if(ownGrid[gxn][gyn]==2)
							{
								bEnd = true;
							}
							else
							{
								//we moved out of current "growth" pixel
								//let's mark it with (2) = visited
								// and mark new pixel as current (1)
								if(gxn!=gx || gyn!=gy)
								{
									ownGrid[gx][gy]=2;
									gx=gxn;
									gy=gyn;
									ownGrid[gx][gy]=1;
								}
								//add point to the end of line array (forward growth)
								if(nDirection==1)
								{
									px.add(xcurr);
									py.add(ycurr);
									
								}
								//add point to the beginning of line array (backward growth)
								//in this way points in array are indexed continuously 
								else
								{
									px.add(0,xcurr);
									py.add(0,ycurr);								
								}
		
							}
						}
					}
				}
				}
					
			}
			//been here too
			ownGrid[gx][gy]=2;
		}
	
		//ok, line is generated		
		float[] floatX = new float[px.size()];
		float[] floatY = new float[px.size()];
		float nAverVal=0;
		int i = 0;
		int nPoints =px.size();
		for (i=0;i<nPoints;i++) 
		{
			//convert it to two arrays that could be fed to PolygonRoi
			floatX[i]=px.get(i);
			floatY[i]=py.get(i);
			//add seed points on both sides of every point in line
			addSeedPoint(floatX[i],floatY[i]);
			//add new line to the grid which tracks occupied space
			markOccupied(px.get(i), py.get(i));
		}
		
		 //contour line ROI		
		//let's check for the distance from the beginning to the end
		//if it is less than 1.5 pixels, make it a closed contour
		 if(Math.sqrt(Math.pow(floatX[0]-floatX[nPoints-1], 2)+Math.pow(floatY[0]-floatY[nPoints-1], 2))<1.5)
		 {
			 polyline = new PolygonRoi(floatX, floatY, Roi.POLYGON);
		 }
		 else //polyline
		 {
			 if(bRemoveOpen)
			 {	 return null;}
			 else
			 {polyline = new PolygonRoi(floatX, floatY, Roi.POLYLINE);}
		 }
		 //well, single color
		 if(bSingleColor)
		 {
			 polyline.setStrokeColor(systemColor);
		 }
		 //use LUT colors to color code depth
		 else
		 {
			//calculating average intensity per line
			nAverVal=0;
			for (i=0;i<nPoints;i++) 
			{
				nAverVal += ip.getf((int)Math.floor(floatX[i]), (int)Math.floor(floatY[i]));
			}
			nAverVal/=px.size();	
			
			//picking current contrast/brightness settings from the image
			if(nAverVal>fInMax)
				nAverVal=255;
			else
			{
				if(nAverVal<fInMin)
					nAverVal=0;
				else
						nAverVal=Math.round(255.0f*(nAverVal-fInMin)/fInRange);
			}
			
			polyline.setStrokeColor(new Color(RGBLutTable[(int)nAverVal][0],RGBLutTable[(int)nAverVal][1],RGBLutTable[(int)nAverVal][2]));
		 }
		 
		//polyline.setStrokeWidth(dStrokeWidth);
		
		return polyline;
		
		
	}
	/** function adds a point to the Grid variable
	 * that track local density of contour lines in the image **/
	void markOccupied(float xin, float yin)
	{
		int gx,gy;
		
		ArrayList<ContourPoint> currarr;		
		
		//grid's cell coordinate
		gx = (int) Math.floor(xin/dGridSize);
		gy = (int) Math.floor(yin/dGridSize);
		currarr =(ArrayList<ContourPoint>) Grid[gx][gy];
		if(currarr==null)
		{
			Grid[gx][gy] = new ArrayList<ContourPoint>();
			currarr =(ArrayList<ContourPoint>) Grid[gx][gy];
		}
		currarr.add(new ContourPoint(xin,yin));
	}
	
	/** function adds two seed point to the fSeedPoints queue
	 * (if they are not in a way of another line, i.e., further 
	 * than dSep value)**/
	void addSeedPoint(float xs, float ys)
	{
		float xn,yn;
		float xvel, yvel;

		Float[] spoint;
		
		int nDirection;
		nDirection =1;
		for(nDirection=1;nDirection>-2;nDirection-=2)
		{
			//direction perpendicular
			xvel = fNormalizedField[Math.round(xs)][Math.round(ys)][1]*((float)nDirection);
			yvel = -fNormalizedField[Math.round(xs)][Math.round(ys)][0]*((float)nDirection);
			//special point (flat or source or sink)
			if(Float.isNaN((Float)xvel) ||Float.isNaN((Float)yvel))
			{
				return;
			}
			xn=xs+xvel*dSep*1.1f;
			yn=ys+yvel*dSep*1.1f;
	
			if(xn<0 || yn<0 || xn>(nW-1)|| yn>(nH-1))
			{
			}
			else
			{
				spoint = new Float [2];
				spoint[0]=xn;
				spoint[1]=yn;
				fSeedPoints.add(spoint);
			}
		}		
		
	}
	
	/** function generates extra seed points at cells of Grid
	 * where there are no points (to fill the whole picture **/
	void refillSeedList()
	{
		int gx,gy;

		
		ArrayList<ContourPoint> currarr;		
		Float[] spoint;
		
		//grid's cell coordinate
		for (gx=0;gx<nWG;gx++)
			for (gy=0;gy<nHG;gy++)
			{
				currarr =(ArrayList<ContourPoint>) Grid[gx][gy];
				//empty cell
				if(currarr==null)
				{
					//add seed point in the middle of the cell
					spoint = new Float [2];
					spoint[0]=((float)gx)*dGridSize+0.5f*dGridSize;
					spoint[1]=((float)gy)*dGridSize+0.5f*dGridSize;
					fSeedPoints.add(spoint);
					
				}
			}
	}
	/** function checking if point xc, yc close to any other existing
	 * points (stored in Grid) by the distance dDist **/
	public boolean isBusy(float xc, float yc, float dDist)
	{
		
		int gx, gy;
		int x,y;
		int i,j,k;

		ArrayList<ContourPoint> currarr;
		ContourPoint point;
		int nRange=(int) Math.ceil(dDist/dGridSize);
		
		x= Math.round(xc);
		y= Math.round(yc);
		//out of image
		if(x<0 || y<0 || x>(nW-1)|| y>(nH-1))
		{
			return true;
		}
		//grid's cell index
		gx = (int) Math.floor(xc/dGridSize);
		gy = (int) Math.floor(yc/dGridSize);
		
		//checking up neighbor cells too
		for (i=gx-nRange;i<=gx+nRange;i++)
			for (j=gy-nRange;j<=gy+nRange;j++)
			{
				if(i<0 || j<0 || i>(nWG-1)|| j>(nHG-1))
				{
								
				}
				else
				{
					currarr =(ArrayList<ContourPoint>) Grid[i][j];
					if(currarr==null)
					{
					
					}
					else
					{
						for (k=0;k<currarr.size();k++)
						{
							point= currarr.get(k);
							if(point.distance(xc, yc)<dDist)
								return true;
						}
					}											
				
				}
			}
	
		return false;
		
	}
	/** Calculates normalized vector field that is orthogonal to gradient field calculated in fx and fy image processors. 
	 * It is stored it in fNormalizedField variable.
	 * In addition, stores point with the smallest absolute value gradient in iniSeed variable **/
	public void fillNormalizedField()
	{
		int i,j;
		float dx,dy,len;
		int imin,jmin;
		float minlen=Float.MAX_VALUE;
		imin=-1;
		jmin=-1;
		
		for (i=0;i<nW;i++)
			for (j=0;j<nH;j++)
			{
				dx = fx.getf(i, j);
				dy = fy.getf(i, j);
				len = (float) Math.sqrt(dx*dx+dy*dy);
				if(len>0.00000001f)
				{
					fNormalizedField[i][j][0]=dy/len;
					fNormalizedField[i][j][1]=-dx/len;
					if (len<minlen)
					{
						minlen=len;
						imin=i;
						jmin=j;
					}
				}
				else
				{
					fNormalizedField[i][j][0]=Float.NaN;
					fNormalizedField[i][j][1]=Float.NaN;					
				}
			}
		//trivial case, image is completely wrong
		//let's pick the middle point of the image
		if(imin==-1)
		{
			iniSeed=new ContourPoint((float)nW*0.5f,(float)nH*0.5f);
		}
		else
		{
			iniSeed=new ContourPoint((float)imin,(float)jmin);
		}
		
	}
	/** Dialog with linking parameters **/
	public boolean showParametersDialog()
	{
		int nLutChoice;
		GenericDialog contourlinesD = new GenericDialog("Rendering parameters");
		String [] luts = IJ.getLuts();
		
		contourlinesD.addMessage("For instructions, see https://imagej.net/Contour_Lines");
        contourlinesD.addMessage("");
		//linkingD.addChoice("Linking sequence: ",sLinkType, Prefs.get("CurveTrace.nLinkingType", "Incremental"));
		contourlinesD.addNumericField("Smoothing radius:", Prefs.get("ContourLines.dSmoothR", 2.0), 1, 3,"pixels");
		contourlinesD.addNumericField("Line integration step (0.01-1):", Prefs.get("ContourLines.dTimeStep", 0.5), 2, 4,"pixels");
		contourlinesD.addNumericField("Distance between lines:", Prefs.get("ContourLines.dSep", 5), 1, 3,"pixels");
		contourlinesD.addNumericField("Stop line at fraction of distance:", Prefs.get("ContourLines.dStopLineSep", 0.5), 1, 3,"fraction");
		//contourlinesD.addNumericField("Stroke width:", Prefs.get("ContourLines.dStrokeWidth", 1), 0, 2,"pixels");
		contourlinesD.addCheckbox("Use single color (current) to draw lines?", Prefs.get("ContourLines.bSingleColor", true));
		contourlinesD.addChoice("Color code contours with LUT:",luts,Prefs.get("ContourLines.sLutChoice","Fire"));
        contourlinesD.addMessage("Note: the LUT color range used is determined by the histogram contrast setting.");
        contourlinesD.addMessage("");
		contourlinesD.addCheckbox("Invert LUT?", Prefs.get("ContourLines.bInvertLUT", false));
		contourlinesD.addCheckbox("Remove open contours?", Prefs.get("ContourLines.bRemoveOpen", false));
		contourlinesD.setResizable(false);
		contourlinesD.showDialog();	
		if (contourlinesD.wasCanceled())
            return false;

		dSmoothR = (float) contourlinesD.getNextNumber();
		Prefs.set("ContourLines.dSmoothR", dSmoothR);
		dTimeStep = (float) contourlinesD.getNextNumber();
		Prefs.set("ContourLines.dTimeStep", dTimeStep);
		dSep = (float) contourlinesD.getNextNumber();
		Prefs.set("ContourLines.dSep", dSep);
		dStopLineSep = (float) contourlinesD.getNextNumber();
		Prefs.set("ContourLines.dStopLineSep", dStopLineSep);

		//dStrokeWidth = contourlinesD.getNextNumber();
		//Prefs.set("ContourLines.dStrokeWidth", dStrokeWidth);
		bSingleColor = contourlinesD.getNextBoolean();
		Prefs.set("ContourLines.bSingleColor", bSingleColor);
		nLutChoice = contourlinesD.getNextChoiceIndex();
		Prefs.set("ContourLines.sLutChoice", luts[nLutChoice]);
		sLUTName = luts[nLutChoice];
		bInvertLUT = contourlinesD.getNextBoolean();
		Prefs.set("ContourLines.bInvertLUT", bInvertLUT);
		bRemoveOpen = contourlinesD.getNextBoolean();
		Prefs.set("ContourLines.bRemoveOpen", bRemoveOpen);

		return true;
	}
	/** 2D Derivative of Gaussian normalized kernel in x (taken from Jalmar code, could be excessive) **/
	public static double[][] computeKernelGaussian2D_du(double sigma_x, double sigma_y, float theta)
	{
		// calculate required kernel size (2*3*sigma~99%)
		int kernel_radius = (int)Math.round(3*Math.max(sigma_x, sigma_y)); // RSLV: use floor instead of round?
		int kernel_size = 1+2*kernel_radius;
		
		// compute kernel
		double[][] kernel = new double[kernel_size][kernel_size];
		for(int ky = 0; ky < kernel_size; ++ky)
		{
			int y = ky - kernel_radius;
			for(int kx = 0; kx < kernel_size; ++kx)
			{
				int x = kx - kernel_radius;
				double u = x * Math.cos(theta) - y * Math.sin(theta);
				double v = x * Math.sin(theta) + y * Math.cos(theta);
				kernel[kx][ky] = gaussian2D_dx(u, v, sigma_x, sigma_y);
			}
		}
		
		// normalize kernel
		kernel = normalize_kernel(kernel);
		
		// return kernel
		return kernel;
	}
	/** 2D Derivative of Gaussian normalized kernel in y (taken from Jalmar code, could be excessive) **/
	public static double[][] computeKernelGaussian2D_dv(double sigma_x, double sigma_y, float theta)
	{
		// calculate required kernel size (2*3*sigma~99%)
		int kernel_radius = (int)Math.round(3*Math.max(sigma_x, sigma_y)); // RSLV: use floor instead of round?
		int kernel_size = 1+2*kernel_radius;
		
		// compute kernel
		double[][] kernel = new double[kernel_size][kernel_size];
		for(int ky = 0; ky < kernel_size; ++ky)
		{
			int y = ky - kernel_radius;
			for(int kx = 0; kx < kernel_size; ++kx)
			{
				int x = kx - kernel_radius;
				double u = x * Math.cos(theta) - y * Math.sin(theta);
				double v = x * Math.sin(theta) + y * Math.cos(theta);
				kernel[kx][ky] = gaussian2D_dy(u, v, sigma_x, sigma_y);
			}
		}
		
		// normalize kernel
		kernel = normalize_kernel(kernel);
		
		// return kernel
		return kernel;
	}
	/** Derivative of Gaussian function in x **/
	public static double gaussian2D_dx(double x, double y, double sigma_x, double sigma_y)
	{
		return ((-x)/(2*Math.PI*Math.pow(sigma_x, 3)*sigma_y))*Math.exp(-0.5*((x*x)/(sigma_x*sigma_x)+(y*y)/(sigma_y*sigma_y)));
	}
	/** Derivative of Gaussian function in y **/
	public static double gaussian2D_dy(double x, double y, double sigma_x, double sigma_y)
	{
		//return gaussian2D_dx(y, x, sigma); // NOTE x and y are swapped
		return ((-y)/(2*Math.PI*sigma_x*Math.pow(sigma_y, 3)))*Math.exp(-0.5*((x*x)/(sigma_x*sigma_x)+(y*y)/(sigma_y*sigma_y)));
	}
	/** normalization of DoG kernel**/
	public static double[][] normalize_kernel(double[][] kernel)
	{
		// calculate sum of components
		double sum = 0.0;
		for(int kx = 0; kx < kernel.length; ++kx)
		{
			for(int ky = 0; ky < kernel[kx].length; ++ky)
			{
				sum += Math.abs(kernel[kx][ky]); // NOTE: use abs to normalize symmetrical kernel with a positive and negative lobe
			}
		}
		
		// avoid division by zero
		if(sum == 0.0) { return kernel; }
		
		// calculate scale factor
		double scale_factor = 1 / sum;
		
		// scale components
		for(int kx = 0; kx < kernel.length; ++kx)
		{
			for(int ky = 0; ky < kernel[kx].length; ++ky)
			{
				kernel[kx][ky] *= scale_factor;
			}
		}
		
		return kernel;
	}
	/** convolution of image with  DoG kernel**/
	public static ImageProcessor convolve(ImageProcessor ip, double[][] kernel)
	{
		// get image and kernel sizes
		int image_width = ip.getWidth();
		int image_height = ip.getHeight();
		
		int kernel_width = kernel.length;
		int kernel_height = kernel_width; // NOTE: assume square kernel
		int kernel_half_width = (int)Math.floor(0.5 * kernel_width);
		int kernel_half_height = kernel_half_width;
		
		// convert input image processor to float
		ImageProcessor ip_inp = ip.convertToFloat(); // RSLV: duplicate first?
		
		// create new empty output float processor
		ImageProcessor ip_res = new FloatProcessor(image_width, image_height);
		
		// convolve input image with kernel
		for(int py = 0; py < image_height; ++py)
		{
			for(int px = 0; px < image_width; ++px)
			{
				double kernel_product = 0.0;
				for(int ky = 0; ky < kernel_height; ++ky)
				{
					int ppy = py + ky - kernel_half_height;
					if(ppy < 0) ppy = 0; // clamp at border
					if(ppy >= image_height) ppy = image_height - 1; // clamp at border
					for(int kx = 0; kx < kernel_width; ++kx)
					{
						int ppx = px + kx - kernel_half_width;
						if(ppx < 0) ppx = 0; // clamp at border
						if(ppx >= image_width) ppx = image_width - 1; // clamp at border
						kernel_product += ip_inp.getf(ppx, ppy) * kernel[kx][ky];
					}
				}
				ip_res.setf(px, py, (float)kernel_product);
			}
		}

		return ip_res;
	}
	
	/** function gets LUT specified by sZLUTName in settings
	 * and returns 256x3 table map in RGB format.
	 * This is mostly done since ImageJ's LutLoader generates
	 * some LUT's on the fly (fire, etc) and some are loaded
	 * from the harddrive. Plus it automatically applies LUT to current image.
	 * So to get a table of LUT, I make a dummy image,
	 * colorcode it with all colors of LUT and read it pixel color (and close image).
	 * This trick is taken from DoM plugin*/
	void  getRGBLutTable()
	{
		int i;
		int j;
		int [] onepix; 
		RGBLutTable = new int[256][3];
		ByteProcessor ish = new ByteProcessor(256,10);
		for (i=0; i<256; i++)
			for (j=0; j<10; j++)
				ish.putPixel(i, j, i);
		ImagePlus imLUT = new ImagePlus("test",ish);
		imLUT.show();
		IJ.run(sLUTName);
		IJ.run("RGB Color");

		imLUT.setSlice(1);
		for(i=0;i<256;i++)
		{			
			onepix= imLUT.getPixel(i, 2);		
			if(!bInvertLUT)
			{
				for (j=0;j<3;j++)
					{RGBLutTable[i][j]=onepix[j];}
			}
			else
			{
				for (j=0;j<3;j++)
					{RGBLutTable[255-i][j]=onepix[j];}
			}
		}
		imLUT.changes=false;
		imLUT.close();
		
		return;
	}

}