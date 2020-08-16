package Astronomy; /** Walter O'Dell PhD,  wodell@rochester.edu,   5/6/03
  * draws contour lines in the user-defined color and at the user-defined levels 
  * I modified greatly a contour plotting program 'Con'
    written in C++ by Frederick Henle (fhenle@emmy.smith.edu) and
    downloaded from: http://math.smith.edu/Local/contours.html
    These next few lines are excerpted from the header to his program:
 ** 
    Con is a program for writing 2-D contour plots in PostScript.
    Copyright (C) 1994 Frederick Henle 
    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
 **
   The basic algorithm remains the same, but the program was greatly modified 
   in converting it to Java and in the user-defined input routines.
   
   Known deficiencies:
      1. labels not always seen well and/or a separate legend would be nice
      
   Requirements:
    1. need GenericRecallableDialog class (included in this file)
    
   Also creates in the plugin directory: 
         stateClass.class, terrainClass.class, plotClass.class
   works on stacks also.
   
 ** updated 12/13/04  put a wait() in the redisplay function to give system time
    to refresh background image.  Fixes bug of first couple contours being erased
    by the system during redrawing of contour lines.
 
 ** 7/7/05  Created option to set contour as image ROI so that one can do area
    calculations, crop or fill the image based on the ROI, etc.
    Note: The original program creates multiple contour segments.  In my experience
    the initial segment actually encompasses the entire contour and contains >90% of
    all the contour points.  The remaining segments contain small contour portions that
    are disjointed from each other and overlap the first segment.  Thus the remaining 
    segments contain redundant information. To create the unique Roi polygon it is 
    necessary to consider only the main segment.
 */

// package ij.plugin;
import java.awt.*;
import java.awt.image.*;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.event.*;
import java.util.*;
import ij.plugin.frame.Recorder;

class plotClass   {   /* plot description */
   String title;            /* title of plot */
   float xu, yu;       /* half the real dimensions of cells */
   int nlevels;        /* number of contour levels */
   float[] levels;         /* array of contour levels */
   String[] colors;
   plotClass() {
      xu = yu = 0.5f; // default values == 1 mm pixels
      title = new String("ContourPlot");
      nlevels = 10;
      levels = new float[nlevels];
      colors = new String[nlevels];
   }
}

class terrainClass {   /* shape of data */
   int[][] hori;           /* array: contour passes below? */
   int[][] vert;           /* array: contour passes to the left? */
   float[][] hy;            /* array: offset of contour below */
   float[][] vx;            /* array: offset of contour to the left */
   terrainClass(int width, int height) {
      hori = new int[width+1][height+1];
      vert = new int[width+1][height+1];
      hy = new float[width+1][height+1];
      vx = new float[width+1][height+1];
   }
}

class stateClass {
   int attitude;         /* Vertical or Horizontal? */
   int direction;        /* direction of motion */
   int ii;               /* X coord of current point on path */
   int jj;               /* Y coord of current point on path */
   stateClass() {}
}


public class ContourPlotter_ implements PlugIn {
   final int DOWN=2, UP=1, RIGHT=2, LEFT=1, X=0, Y=1;
   int Direction;
   final int North=0, NorthEast=1, East=2, SouthEast=3, South=4, 
             SouthWest=5, West=6, NorthWest=7, NumDirections=8;
   int Attitude;
   final int Horizontal=0, Vertical=1;   
   String Label;
   plotClass plot;         /* plot description, reused for each */
   float[][] pixels;       
   float[][][][] contours;   // [image slice][level][pts][xy]   
   private ImagePlus imp;  // set in setup()
   private ImageCanvas Parent_Canvas; 
   private Graphics Parent_Graphics;
   int nSlices, ImgWidth, ImgHeight;
   boolean DebugOn = false;
   final String[] colors = {"green","blue","red","black","white","yellow","orange","cyan"};
   int nsegments[][], segmentStarts[][][], segmentEnds[][][]; // [slice][level][segment]
   GenericRecallableDialog gd;
   
   public void run(String arg) {
      if (arg.equals("about"))  {showAbout(); return;}
      imp = WindowManager.getCurrentImage();
      if (imp==null) {
         IJ.write("Error: no image defined..");
         return;
      }
      //ImageStack imgStack = imp.getStack(); 
      Label = new String("Contour Plotter");
      ImageProcessor ip = imp.getProcessor();
      nSlices = imp.getStack().getSize(); // == nslices
      ImgWidth = ip.getWidth();
      ImgHeight = ip.getHeight();
      Parent_Canvas = imp.getWindow().getCanvas(); 
      Parent_Graphics = Parent_Canvas.getGraphics(); 
      // create gui to enter levels
	  
      gd = new GenericRecallableDialog(
             "Contour Plotter Levels", IJ.getInstance());
      gd.addMessage("Enter level value for each contour\n"+
          "   Range: "+IJ.d2s(ip.getMin(),1)+" ... "+IJ.d2s(ip.getMax(),1)+
          "\n   (Enter -1 for no level)");
		  
      plot = new plotClass();
      contours = new float[nSlices][plot.nlevels][][]; // will be allocated fully later
      for (int i=0; i<plot.nlevels; i++) plot.levels[i] = -1f;
      plot.levels[0] = 95f;
      plot.levels[1] = 90f; 
      plot.levels[2] = 75f;
      plot.levels[3] = 50f;
      for (int i=0; i<plot.nlevels; i++) {
         gd.beginRowOfItems();
         gd.addNumericField("Level "+(i+1), (double)(plot.levels[i]), 0);
         plot.colors[i] = colors[i%colors.length];
         gd.addChoice("Color "+(i+1), colors, plot.colors[i]);
         gd.addButton("set as ROI");
         gd.endRowOfItems();
      }
      gd.addButtons("Redisplay","Help");	  
      gd.addButton("Make Contour Plot");
      gd.showDialog(); 
      int cur_slice = 0, prev_slice = 0;
      while (!(gd.wasCanceled())) { // continuously runs through loop
         if (gd.getFocusOwner() == null) IJ.wait(500); 
         cur_slice = imp.getCurrentSlice();
         if (cur_slice != prev_slice) {
            prev_slice = cur_slice;
            ReDisplay(cur_slice);
         }
         for (int i=0; i<plot.nlevels; i++) 
            if (gd.getButtonValue(i)) 
               setAsROI(cur_slice, i);
         if (gd.getButtonValue(plot.nlevels+0)) ReDisplay(cur_slice);
         if (gd.getButtonValue(plot.nlevels+1)) showAbout();
         if (gd.getButtonValue(plot.nlevels+2)) {
            for (int i=0; i<plot.nlevels; i++) {
               plot.levels[i] = (float)(gd.getNextNumber());
               plot.colors[i] = gd.getNextChoice();
            }
            IJ.showStatus(" computing contours, please wait...");
            getContours(plot);
            IJ.showStatus(" done computing contours ...");
            ReDisplay(cur_slice);
            IJ.wait(1000);
         }
      }// end while
   }
   
   void showAbout()  {
      String displayStr = Label+ ": This performs user-defined contour plots.      \n" 
        + "  To apply new levels or colors hit the 'Make Contour Plot' button.     \n"
        + "  To refresh the preexisting display hit the 'Redisplay' button.        \n"
        + "  One can set a given contour as the current ImageJ ROI, from which one \n"
        + "  can then use Analyze->Measure to get area and other results, and one  \n"
        + "  can use the ROI routines Edit-> fill, crop, clear outside, etc.       \n"
        + "  If there are multiple contour segments at the chosen level, the ROI   \n"
        + "  is generated from the largest level contour segment.";
      IJ.showMessage(displayStr);
   } // end showAbout()
  
   public String toString() {
     return (" "+Label);
   }
 
   public void setAsROI(int slice, int level) {
     // sets the largest segment as the ROI
     if (contours[slice-1][level] == null) {
       IJ.write("   error: slice and level values indicate a null contour ...");
       return;
     }
     int npts = contours[slice-1][level].length;
     if (npts == 0) {
       IJ.write("   error: slice and level values indicate contour with no points ...");
       return;
     }
     // find largest segment
     int nsegmentPts = -1;
     int longest_seg = 0;
     for (int s=0; s<nsegments[slice-1][level]; s++) {
        int nsegpts = segmentEnds[slice-1][level][s] - segmentStarts[slice-1][level][s] + 1;
        if (nsegpts > nsegmentPts) {
           nsegmentPts = nsegpts;
           longest_seg = s;
        }
     }
     if (nsegmentPts <= 0) {
       IJ.write("   error: first contour segment at this slice and level contains no points");
       return;
     }
     int[] xPoints = new int[nsegmentPts];
     int[] yPoints = new int[nsegmentPts];
     for (int i=segmentStarts[slice-1][level][longest_seg]; i<=segmentEnds[slice-1][level][longest_seg]; i++) {
        xPoints[i-segmentStarts[slice-1][level][longest_seg]] = round(contours[slice-1][level][i][X]);
        yPoints[i-segmentStarts[slice-1][level][longest_seg]] = round(contours[slice-1][level][i][Y]);
     }
     PolygonRoi thisRoi = new PolygonRoi(xPoints, yPoints, nsegmentPts, Roi.POLYGON);
     thisRoi.setImage(imp);
     imp.killRoi();
     imp.setRoi(thisRoi);
     ReDisplay(slice); // refresh the contour lines
   }
   
   // these should be in the Math class, but aren't
   public float sqr(float x) { return x*x; }

   public float sqrt(float x) { return (float) Math.sqrt((double)x); }

   public float sep(float[] pt1, float[] pt2) { 
     return sqrt( sqr(pt1[X] - pt2[X]) + sqr(pt1[Y] - pt2[Y]) );
   }
   public float min(float x, float y) {
      if (x<y) return x;
      else return y;
   }
   public static int round(float x) {
     int isign = 1;
     if (x<0f) { isign = -1; x = -x; } 
     return(isign*(int)(x+0.5f)); 
   }

   public void ReDisplay(int slice) {
     imp.updateAndDraw();
     IJ.wait(10); // give system time to clear screen
     DrawContours(slice-1);
   }
   
   public void getContours(plotClass plot) {
     if (imp.getWindow().isClosed()) { // the original image window was closed by the user
       IJ.write("  error: someone closed the original image window, restarting..");
       gd.dispose();
       run("new run"); // restart analysis on new image
     }
     terrainClass terrain;
     float[][]pixels = new float[ImgWidth][ImgHeight];
     ImageProcessor ip;
     // WO need to better align the contour segments as they jump around
     // also, segment points can be reversed ordered, so check for that
     int MaxNsegments = 100;
     nsegments = new int[nSlices][plot.nlevels];
     segmentStarts = new int[nSlices][plot.nlevels][MaxNsegments]; // max nsegments is assumed to be 100
     segmentEnds = new int[nSlices][plot.nlevels][MaxNsegments]; 
     int prevStart;
     for (int slice=0; slice<nSlices; slice++) {
        ip = imp.getStack().getProcessor(slice+1);
        for (int w=0; w<ImgWidth; w++) 
           for (int h=0; h<ImgHeight; h++) 
              pixels[w][h] = ip.getPixelValue(w, h);
        for (int k=0; k<plot.nlevels; k++) {
           contours[slice][k] = null; // erase previous versions
           prevStart = 0; // WO
           if (plot.levels[k]>=0f) { 
              nsegments[slice][k] = 0;  // WO reorder segments at end
              terrain = getTerrain (plot.levels[k], plot, pixels);
              for (int i = 0; i < ImgWidth; i++)        // columns
                for (int j = 0; j < ImgHeight; j++) {   // rows
                   if (terrain.vert[i][j] == LEFT)
                      contours[slice][k] = getContour(contours[slice][k], 
                                       Vertical, North, i, j, plot, terrain);
                   if (contours[slice][k] != null && contours[slice][k].length > prevStart) {
                      // check to see if need to increase array size
                      if (nsegments[slice][k] >= segmentStarts[slice][k].length) {
                        int[] tmpstarts = new int[segmentStarts[slice][k].length + 100];
                        int[] tmpends = new int[segmentStarts[slice][k].length + 100];
                        for (int s=0; s<segmentStarts[slice][k].length; s++) {
                          tmpstarts[s] = segmentStarts[slice][k][s];
                          tmpends[s] = segmentEnds[slice][k][s];
                        }
                        segmentStarts[slice][k] = tmpstarts;
                        segmentEnds[slice][k] = tmpends;
                      }
                      segmentStarts[slice][k][nsegments[slice][k]] = prevStart;
                      segmentEnds[slice][k][nsegments[slice][k]] = contours[slice][k].length -1;
                      nsegments[slice][k]++;
                      prevStart = contours[slice][k].length;
                   }
                   if (terrain.hori[i][j] == UP)
                      contours[slice][k] = getContour(contours[slice][k], 
                                     Horizontal, East, i, j, plot, terrain);
                   if (contours[slice][k] != null && contours[slice][k].length > prevStart) {
                      // check to see if need to increase array size
                      if (nsegments[slice][k] >= segmentStarts[slice][k].length) {
                        int[] tmpstarts = new int[segmentStarts[slice][k].length + 100];
                        int[] tmpends = new int[segmentStarts[slice][k].length + 100];
                        for (int s=0; s<segmentStarts[slice][k].length; s++) {
                          tmpstarts[s] = segmentStarts[slice][k][s];
                          tmpends[s] = segmentEnds[slice][k][s];
                        }
                        segmentStarts[slice][k] = tmpstarts;
                        segmentEnds[slice][k] = tmpends;
                      }
                      segmentStarts[slice][k][nsegments[slice][k]] = prevStart;
                      segmentEnds[slice][k][nsegments[slice][k]] = contours[slice][k].length -1;
                      nsegments[slice][k]++;
                      prevStart = contours[slice][k].length;
                   }
              } // end i,j loops  
           } // end if level value > 0, i.e. valid level
        } // end levels
     } // end slices
   }

   protected terrainClass getTerrain (float conValue, plotClass plot, float[][] pixels) {
     float xu = plot.xu;            /* half real width of cell */
     float yu = plot.yu;            /* half real height of cell */
     int width = pixels.length;        /* number of columns of data */
     int height = pixels[0].length;      /* number of rows of data */
     terrainClass tp = new terrainClass(width, height);
     
     /* map edges first */
     for (int j = 0; j < height; j++) {
        if (pixels[0][j] >= conValue) {
           tp.vert[0][j] = RIGHT; // direction you are w/r/2 contour
           tp.vx[0][j] = xu;
        }
        else  tp.vert[0][j] = -1;
        if (pixels[width-1][j] >= conValue) {
           tp.vert[width][j] = LEFT;
           tp.vx[width][j] = -xu;
        }
        else  tp.vert[width][j] = -1;
     }
     for (int i = 0; i < width; i++) {
        if (pixels[i][0] >= conValue) {
           tp.hori[i][0] = UP;
           tp.hy[i][0] = yu;
        }
        else  tp.hori[i][0] = -1;
        if (pixels[i][height-1] >= conValue) {
           tp.hori[i][height] = DOWN;
           tp.hy[i][height] = -yu;
        }
        else  tp.hori[i][height] = -1;
     }
     /* now map interior */
     for (int i = 1; i < width; i++) 
       for (int j = 0; j < height; j++) {
         if (pixels[i][j] <= conValue) {
           if (pixels[i-1][j] >= conValue && pixels[i-1][j] != pixels[i][j]) {
              tp.vert[i][j] = LEFT;
              tp.vx[i][j] = (xu * (pixels[i-1][j] + pixels[i][j] - 2 * conValue) / 
                          (pixels[i-1][j] - pixels[i][j]));
           }
           else tp.vert[i][j] = -1;
         }
         if (pixels[i][j] >= conValue) {
           if (pixels[i-1][j] <= conValue && pixels[i-1][j] != pixels[i][j]) {
              tp.vert[i][j] = RIGHT;
              tp.vx[i][j] = (xu * (pixels[i-1][j] + pixels[i][j] - 2 * conValue) / 
                           (pixels[i-1][j] - pixels[i][j]));
           }
           else tp.vert[i][j] = -1;
         }
       }
     for (int i = 0; i < width; i++) 
       for (int j = 1; j < height; j++) {
         if (pixels[i][j] <= conValue) {
           if (pixels[i][j-1] >= conValue && pixels[i][j-1] != pixels[i][j]) {
             tp.hori[i][j] = DOWN;
             tp.hy[i][j] = (yu * (pixels[i][j-1] + pixels[i][j] - 2 * conValue) / 
                          (pixels[i][j-1] - pixels[i][j]));
           }
           else tp.hori[i][j] = -1;
         }
         if (pixels[i][j] >= conValue) {
           if (pixels[i][j-1] <= conValue && pixels[i][j-1] != pixels[i][j]) {
             tp.hori[i][j] = UP;
             tp.hy[i][j] = (yu * (pixels[i][j-1] + pixels[i][j] - 2 * conValue) / 
                           (pixels[i][j-1] - pixels[i][j]));
          }
          else tp.hori[i][j] = -1;
        }
      }
      return tp;
   } // end mapterrain

   public float[][] getContour(float[][] prevpts, int verthort, int direction, int i, int j, 
                          plotClass plot, terrainClass tp) {
     boolean is_next;            /* GetNext () says contour continues */
     float xu = plot.xu;         /* half real width of cell */
     float xw = 2 * xu;          /* real width of cell */
     float yu = plot.yu;         /* half real height of cell */
     float yw = 2 * yu;          /* real height of cell */
     int npts = 0;               /* number of points on current path */
     stateClass s = new stateClass(); /* state of path search */
     s.attitude = verthort;        /* preparations */
     s.direction = direction;
     s.ii = i;
     s.jj = j;
     float[][] pts = new float[100][2];
     if (verthort==Vertical)  pts[npts][X] = i * xw + tp.vx[i][j];
     else pts[npts][X] = i * xw + xu;
     if (verthort==Vertical)  pts[npts][Y] =  j * yw + yu;
     else pts[npts][Y] =  j * yw + tp.hy[i][j];
     npts++;
     if (verthort==Vertical)  tp.vert[i][j] = -1;
     else tp.hori[i][j] = -1;      
     is_next = GetNext (ImgWidth, ImgHeight, tp, s);
     while (is_next) {
       if (s.attitude == Vertical) {
          pts[npts][X] = s.ii * xw + tp.vx[s.ii][s.jj];
          pts[npts][Y] = s.jj * yw + yu;
       }
       else {
          pts[npts][X] = s.ii * xw + xu;
          pts[npts][Y] = s.jj * yw +tp.hy[s.ii][s.jj];
       }
       if (s.attitude == Vertical)
          tp.vert[s.ii][s.jj] = -1;
       else tp.hori[s.ii][s.jj] = -1;
       npts++;
       if (npts >= pts.length) { // increase array size
         float[][] tmppts = new float[pts.length*2][2];
         for (int m=0; m<pts.length; m++)  {
            tmppts[m][X] = pts[m][X];
            tmppts[m][Y] = pts[m][Y];
         }
         pts = tmppts;
      }
      is_next = GetNext(ImgWidth, ImgHeight, tp, s);
    } // end while loop 
     
    if (verthort==Vertical) tp.vert[i][j] = -1;
    else  tp.hori[i][j] = -1;
    // addpts to existing conpts array, or start new one
    if (prevpts == null) {
      float[][] tmppts = new float[npts][2];
      for (int m=0; m<npts; m++)  {
         tmppts[m][X] = pts[m][X];
         tmppts[m][Y] = pts[m][Y];
      }
      return tmppts;
    }
    if (npts<=3) return prevpts; // this algorithum is prone to losing 1 or 2 pixels
    float[][] tmppts = new float[prevpts.length+npts][2];
    for (int m=0; m<prevpts.length; m++)  {
      tmppts[m][X] = prevpts[m][X];
      tmppts[m][Y] = prevpts[m][Y];
    }
    for (int m=0; m<npts; m++)  {
      tmppts[prevpts.length+ m][X] = pts[m][X];
      tmppts[prevpts.length+ m][Y] = pts[m][Y];
    }
    return tmppts;
  }
   
    public void DrawContours(int slice_at) {
      int ci;
      for (int i=0; i<contours[0].length; i++) {
        for(ci=0; ci<colors.length && plot.colors[i]!=colors[ci]; ci++) ;
        switch (ci) {
          case 0:// "green":
                Parent_Graphics.setColor(java.awt.Color.green); break;
          case 1:// "blue":
                Parent_Graphics.setColor(java.awt.Color.blue); break;
          case 2:// "red":
                Parent_Graphics.setColor(java.awt.Color.red); break;
          case 3:// "black":
                Parent_Graphics.setColor(java.awt.Color.black); break;
          case 4:// "white":
                Parent_Graphics.setColor(java.awt.Color.white); break;
          case 5:// "yellow":
                Parent_Graphics.setColor(java.awt.Color.yellow); break;
          case 6:// "orange":
                Parent_Graphics.setColor(java.awt.Color.orange); break;
          case 7:// "purple":
                Parent_Graphics.setColor(java.awt.Color.cyan); break;
          default:
            Parent_Graphics.setColor(java.awt.Color.green); break;
        }
        if (contours[slice_at][i]!=null && contours[slice_at][i].length>0) 
            DrawArray(contours[slice_at][i], IJ.d2s(plot.levels[i], 0));
      }
   }
   
   public void DrawArray(float[][] pts, String label) {
      // draws all the segments
      int pt_right, Npts = pts.length;
      final int X=0, Y=1;
      int pt_at;
      for(pt_at=0;pt_at < Npts;pt_at++) {
         pt_right = pt_at +1;
         if( pt_right>=Npts ) pt_right = 0;
         if (pts[pt_at]==null) break;
         if (pts[pt_right]==null) break;
         // put separation between distict regions -- defined as separated
         // by > 2 pixels
         if (Math.sqrt(sqr(pts[pt_right][X] - pts[pt_at][X]) +
                    sqr(pts[pt_right][Y] - pts[pt_at][Y]) ) < 2f)
           Parent_Graphics.drawLine(
               (int)(Parent_Canvas.screenX((int)(pts[pt_right][X]+0.5))), 
               (int)(Parent_Canvas.screenY((int)(pts[pt_right][Y]+0.5))),
               (int)(Parent_Canvas.screenX((int)(pts[pt_at][X]+0.5))), 
               (int)(Parent_Canvas.screenY((int)(pts[pt_at][Y]+0.5))));
      }
      // print text at end of contour
      if (label == null || Npts == 0) return;
      Parent_Graphics.drawString(label, 
               (int)(Parent_Canvas.screenX((int)(pts[Npts-1][X]+0.5))), 
               (int)(Parent_Canvas.screenY((int)(pts[Npts-1][Y]+0.5))) );
   }

   /* GetNext() finds the next direction for the contour path. */
   boolean GetNext(int nx, int ny, terrainClass tp, stateClass sp) {
     int next;               /* proposed next value for sp.direction */
     int turn;               /* directional offset from sp.direction */

     for (turn = 2; turn >= -2; turn--) {
       /* mod to fix abnormal termination sgi by 
        * P. B. Gharpure (gharpure@vissgi.cvrti.utah.edu)
        */
       next = (sp.direction + turn + NumDirections) % NumDirections;
       switch (next) {
         case North:
           if (sp.attitude == Vertical) {
             if (sp.jj < ny - 1 && tp.vert[sp.ii][sp.jj+1] == LEFT) {
               sp.direction = North;
               sp.jj++;
               return true;
             }
             else break;
           }
           else                  /* Horizontal */
             break;
             
         case NorthEast:
           if (sp.attitude == Vertical) {
             if (sp.ii < nx && tp.hori[sp.ii][sp.jj+1] == UP) {
               sp.direction = NorthEast;
               sp.attitude = Horizontal;
               sp.jj++;
               return true;
             }
             else  break;
           }
           else      {            /* Horizontal */
             if (sp.jj < ny && tp.vert[sp.ii+1][sp.jj] == LEFT) {
               sp.direction = NorthEast;
               sp.attitude = Vertical;
               sp.ii++;
               return true;
             }
             else break;
           }
           
         case East:
           if (sp.attitude == Vertical) 
             break;
           else {               /* Horizontal */
             if (sp.ii < nx - 1 && tp.hori[sp.ii+1][sp.jj] == UP) {
               sp.direction = East;
               sp.ii++;
               return true;
             }
             else break;
           }
         case SouthEast: 
           if (sp.attitude == Vertical) {
             if (sp.ii < nx && tp.hori[sp.ii][sp.jj] == UP) {
               sp.direction = SouthEast;
               sp.attitude = Horizontal;
               return true;
             }
             else break;
           }
           else     {             /* Horizontal */
             if (sp.jj > 0 && tp.vert[sp.ii+1][sp.jj-1] == RIGHT) {
               sp.direction = SouthEast;
               sp.attitude = Vertical;
               sp.jj--;
               sp.ii++;
               return true;
             }
             else break;
          }
          case South:
            if (sp.attitude == Vertical) {
              if (sp.jj > 0 && tp.vert[sp.ii][sp.jj-1] == RIGHT) {
                sp.direction = South;
                sp.jj--;
                return true;
              }
              else break;
            }
            else                  /* Horizontal */
            break;

         case SouthWest:
           if (sp.attitude == Vertical) {
             if (sp.ii > 0 && tp.hori[sp.ii-1][sp.jj] == DOWN) {
               sp.direction = SouthWest;
               sp.attitude = Horizontal;
               sp.ii--;
               return true;
             }
             else break;
           }
           else {                 /* Horizontal */
             if (sp.jj > 0 && tp.vert[sp.ii][sp.jj-1] == RIGHT) {
               sp.direction = SouthWest;
               sp.attitude = Vertical;
               sp.jj--;
               return true;
             }
             else break;
          }
         case West:
           if (sp.attitude == Vertical)
             break;
           else {      /* Horizontal */
              if (sp.ii > 0 && tp.hori[sp.ii-1][sp.jj] == DOWN) {
                  sp.direction = West;
                  sp.ii--;
                  return true;
              }
              else break;
           }
         case NorthWest:
           if (sp.attitude == Vertical) {
             if (sp.ii > 0 && tp.hori[sp.ii-1][sp.jj+1] == DOWN) {
               sp.direction = NorthWest;
               sp.attitude = Horizontal;
               sp.jj++;
               sp.ii--;
               return true;
             }
             else break;
           }
           else {                 /* Horizontal */
             if (sp.jj < ny && tp.vert[sp.ii][sp.jj] == LEFT) {
               sp.direction = NorthWest;
               sp.attitude = Vertical;
               return true;
             }
             else break;
           }
        default:
          IJ.write("internal error in GetNext ()\n");
          return false;
        }
      }
    return false;     /* couldn't find good next direction */
  }
} // end class ContourPlotter