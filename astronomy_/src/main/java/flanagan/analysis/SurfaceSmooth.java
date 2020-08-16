/*
*   CLASS:      SurfaceSmooth
*
*   USAGE:      Class for smoothing a surface
*               Smoothing methods: moving average window or Savitzky-Golay filter
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:       5-16 March 2012
*   AMENDED:     
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*   http://www.ee.ucl.ac.uk/~mflanaga/java/SurfaceSmooth.html
*
*   Copyright (c) 2012  Michael Thomas Flanagan
*
*   PERMISSION TO COPY:
*
* Permission to use, copy and modify this software and its documentation for NON-COMMERCIAL purposes is granted, without fee,
* provided that an acknowledgement to the author, Dr Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies
* and associated documentation or publications.
*
* Redistributions of the source code of this source code, or parts of the source codes, must retain the above copyright notice, this list of conditions
* and the following disclaimer and requires written permission from the Michael Thomas Flanagan:
*
* Redistribution in binary form of all or parts of this class must reproduce the above copyright notice, this list of conditions and
* the following disclaimer in the documentation and/or other materials provided with the distribution and requires written permission from the Michael Thomas Flanagan:
*
* Dr Michael Thomas Flanagan makes no representations about the suitability or fitness of the software for any or for a particular purpose.
* Dr Michael Thomas Flanagan shall not be liable for any damages suffered as a result of using, modifying or distributing this software
* or its derivatives.
*
***************************************************************************************/

package flanagan.analysis;

import flanagan.math.ArrayMaths;
import flanagan.math.Conv;
import flanagan.math.Fmath;
import flanagan.interpolation.BiCubicSpline;
import flanagan.math.Matrix;
import flanagan.plot.*;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SurfaceSmooth{
    
    private double[] xData = null;                                  // original x data, z = f(x,y)
    private double[] yData = null;                                  // original y data, z = f(x,y)  
    private double[][] zData = null;                                // original z data, z = f(x,y)
    private BigDecimal[] xBDdata = null;                            // original arbitrary precision x data, z = f(x,y)
    private BigDecimal[] yBDdata = null;                            // original arbitrary precision y data, z = f(x,y)
    private BigDecimal[][] zBDdata = null;                          // original arbitrary precision z data, z = f(x,y)
    private int nPointsX = 0;                                       // number of  x data points, z = f(x,y)
    private int nPointsY = 0;                                       // number of  y data points, z = f(x,y)
    private int nPoints = 0;                                        // total number of data points
    private boolean arbprec = false;                                // = true if arbitrary precision data entered
    
    private double[][] zDataMovAv = null;                           // moving average smoothed z data, double precision, z = f(x,y)
    private BigDecimal[][] zDataMovAvBD = null;                     // moving average smoothed z data, arbitrary precision, z = f(x,y)
    private double[][] zDataSavGol = null;                          // Savitzky-Golay smoothed z data, z = f(x,y)             
    private double[][] derivSavGol = null;                          // Savitzky-Golay smoothed derivative (d^mnz/dx^mdy^n), z = f(x,y)  
    private int[][] sgCoeffIndices = null;                          // indices of the fitting polynomial and Savitzky-Golay filter coefficients
    private int nSGcoeff = 0;                                       // number of coefficients
    private int lastMethod = 0;                                     // = 0: none called
                                                                    // = 1: moving window average
                                                                    // = 2: Savitzky-Golay filter                                                           
    private int nMethods = 2;                                       // number of methods
                                                                                                                                
    private int maWindowWidthx = 0;                                 // x width of the  smoothing window in the running average method, z = f(x,y)
    private int maWindowWidthy = 0;                                 // y width of the  smoothing window in the running average method, z = f(x,y)
   
    private int sgWindowWidthx = 0;                                 // x width of smoothing window in the Savitzky-Golay method, z = f(x,y)
    private int sgWindowWidthy = 0;                                 // y width of smoothing window in the Savitzky-Golay method, z = f(x,y)

    private int sgPolyDeg = 4;                                      // Savitzky-Golay smoothing polynomial degree
    private double[][] sgArrayC = null;                             // Savitzky-Golay C matrix
   
    private boolean calcSavGol = false;                             // = true when Savitzky-Golay smoothing performed
    private boolean calcMovAv = false;                              // = true when moving average smoothing performed
    
    private boolean nthSet = false;                                 // = true when any Savitzky-Golay derivative smoothing performed
    
    private double extentMovAv = -1.0;                              // Extent of the moving average window smoothing
    private double extentSavGol = -1.0;                             // Extent of the Savitzky-Golay smoothing
        
    private BiCubicSpline bcsSavGol = null;                         // BiCubic spline interpolation of the Savitzky-Golay  smoothed data
    private BiCubicSpline bcsMovAv = null;                          // BiCubic spline interpolation of the moving averagey  smoothed data
    
    private int trunc = 4;                                          // truncation value for plot title values
    
    // CONSTRUCTORS
    
    // Constructor - data as  double, z=f(x,y)
    public SurfaceSmooth(double[] x, double[] y, double[][] z){ 
        this.xData = x;
        this.yData = y;
        this.zData = z;
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as  double - no x or y data, z=f(x,y)
    public SurfaceSmooth(double[][] z){ 
        int n = z.length;
        this.zData = z;
        this.yData = new double[n];
        for(int i=0; i<n; i++)this.yData[i] = i;
        n = z[0].length;
        this.xData = new double[n];
        for(int i=0; i<n; i++)this.xData[i] = i;
        this.polyIndices();
        this.check();
    }
    
    // Constructor - data as  float, z=f(x,y)
    public SurfaceSmooth(float[] x, float[] y, float[][] z){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(z);
        this.yData = am.array();
        am = new ArrayMaths(z[0]);
        this.zData[0] = am.array();
        am = new ArrayMaths(z[1]);
        this.zData[1] = am.array();
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as  float - no x or y data, z=f(x,y)
    public SurfaceSmooth(float[][] z){ 
        int n = z.length;
        int m = z[0].length;
        this.zData = new double[n][m];
        this.xData = new double[m];
        this.yData = new double[n];
        for(int i=0; i<m; i++)this.xData[i] = i;
        for(int i=0; i<n; i++)this.yData[i] = i;
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = (double)z[i][j];
            }
        }
        this.polyIndices();
        this.check();
    }
    
    // Constructor - data as  long, z=f(x,y)
    public SurfaceSmooth(long[] x, long[] y, long[][] z){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        am = new ArrayMaths(z[0]);
        this.zData[0] = am.array();
        am = new ArrayMaths(z[1]);
        this.zData[1] = am.array();
        this.polyIndices();
        this.check();
        this.ascend();
    }
   
    // Constructor - data as  long - no x or y data, z=f(x,y)
    public SurfaceSmooth(long[][] z){ 
        int n = z.length;
        int m = z[0].length;
        this.zData = new double[n][m];
        this.xData = new double[m];
        this.yData = new double[n];
        for(int i=0; i<m; i++)this.xData[i] = i;
        for(int i=0; i<n; i++)this.yData[i] = i;
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = (double)z[i][j];
            }
        }
        this.polyIndices();
        this.check();
    }
    
    // Constructor - data as  int, z=f(x,y)
    public SurfaceSmooth(int[] x, int[] y, int[][] z){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        am = new ArrayMaths(z[0]);
        this.zData[0] = am.array();
        am = new ArrayMaths(z[1]);
        this.zData[1] = am.array();
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as  int - no x or y data, z=f(x,y))
    public SurfaceSmooth(int[][] z){ 
        int n = z.length;
        int m = z[0].length;
        this.zData = new double[n][m];
        this.xData = new double[m];
        this.yData = new double[n];
        for(int i=0; i<m; i++)this.xData[i] = i;
        for(int i=0; i<n; i++)this.yData[i] = i;
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = (double)z[i][j];
            }
        }
        this.polyIndices();
        this.check();
    }
    
    // Constructor - data as  BigDecimal, z=f(x,y)
    public SurfaceSmooth(BigDecimal[] x, BigDecimal[] y, BigDecimal[][] z){ 
        this.arbprec = true;
        this.xBDdata = x;
        int n = z.length;
        this.yBDdata = y;
        int m = z[0].length;
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = z[i][j].doubleValue();
                this.zBDdata[i][j] = z[i][j];
            }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    } 
    
    // Constructor - data as  BigDecimal - no x or y data, z=f(x,y)
    public SurfaceSmooth(BigDecimal[][] z){ 
        this.arbprec = true;
        int n = z.length;
        int m = z[0].length;
        this.zData = new double[n][m];
        this.xData = new double[m];
        this.yData = new double[n];
        this.zBDdata = new BigDecimal[n][m];
        this.xBDdata = new BigDecimal[m];
        this.yBDdata = new BigDecimal[n];
        for(int i=0; i<m; i++){
            this.xData[i] = i;
            String ii = (new Integer(i)).toString();
            this.xBDdata[i] = new BigDecimal(ii);
        }
        for(int i=0; i<n; i++){
            this.yData[i] = i;
            String ii = (new Integer(i)).toString();
            this.yBDdata[i] = new BigDecimal(ii);
        }
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = z[i][j].doubleValue();
                this.zBDdata[i][j] = z[i][j];
            }
        }
        this.polyIndices();
        this.check();
    }
    
    // Constructor - data as  BigInteger, z=f(x,y)
    public SurfaceSmooth(BigInteger[] x, BigInteger[] y,  BigInteger[][] z){ 
        this.arbprec = true;
        int n = x.length;
        int m = y.length;
        this.xData = new double[n];
        this.yData = new double[m];
        this.xBDdata = new BigDecimal[n];
        this.yBDdata = new BigDecimal[m];
        this.zBDdata = new BigDecimal[z.length][z[0].length];
        for(int i=0; i<n; i++){
            this.xBDdata[i] = new BigDecimal(x[i]);
            this.xData[i] = x[i].doubleValue();
        }
        for(int i=0; i<m; i++){
            this.yBDdata[i] = new BigDecimal(y[i]);
            this.yData[i] = y[i].doubleValue();
        }
        for(int i=0; i<z.length; i++){
            for(int j=0; j<z[0].length; j++){
                this.zBDdata[i][j] = new BigDecimal(z[i][j]);
                this.zData[i][j] = z[i][j].doubleValue();
            }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as  BigInteger - no x or y data, z=f(x,y)
    public SurfaceSmooth(BigInteger[][] z){
        this.arbprec = true;
        int n = z.length;
        int m = z[0].length;
        this.zData = new double[n][m];
        this.xData = new double[n];
        this.yData = new double[m];
        this.zBDdata = new BigDecimal[n][m];
        this.xBDdata = new BigDecimal[n];
        this.yBDdata = new BigDecimal[m];
        for(int i=0; i<m; i++){
            this.xData[i] = i;
            String xx = (new Integer(i)).toString();
            this.xBDdata[i] = new BigDecimal(xx);
        }
        for(int i=0; i<n; i++){
            this.yData[i] = i;
            String xx = (new Integer(i)).toString();
            this.yBDdata[i] = new BigDecimal(xx);
        }
        for(int i=0; i<n; i++){
            for(int j=0; j<m; j++){
                this.zData[i][j] = z[i][j].doubleValue();
                this.zBDdata[i][j] = new BigDecimal(z[i][j]);
            }
        }
        this.polyIndices();
        this.check();
    }
    
    // Constructor - x data as 1D double, y data as Matrix, z=f(x,y)
    public SurfaceSmooth(double[] x, double[] y, Matrix z){ 
        this.xData = x;
        this.yData = y;
        this.zData = z.getArrayCopy();
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - y data as Matrix, no x or y data, z=f(x,y)
    public SurfaceSmooth(Matrix z){         
        this.zData = z.getArrayCopy();
        int n = this.zData.length;
        int m = this.zData[0].length;
        for(int i=0; i<m; i++)this.xData[i] = i;
        for(int i=0; i<n; i++)this.yData[i] = i;
        this.polyIndices();
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - no data entered
    // private - for internal use
    public SurfaceSmooth(){
    }  
    
    // Store indices of the fitting polynomial
    private void polyIndices(){
        this.nSGcoeff  =  0;
        for(int i=1; i<=this.sgPolyDeg+1; i++)this.nSGcoeff +=i;
        this.sgCoeffIndices = new int[this.nSGcoeff][2];
        int counter = 0;
        for(int i = 0; i<=this.sgPolyDeg; i++){
            for(int j =0; j<=this.sgPolyDeg-i; j++){
                this.sgCoeffIndices[counter][0] = i; 
                this.sgCoeffIndices[counter++][1] = j;
            }
        }    
    }
    
    // Check for correct dimensions,   data, z=f(x,y)
    private void check(){
        // array lengths
        this.nPointsY = this.yData.length;
        this.nPointsX = this.xData.length;
        this.nPoints = this.nPointsX*this.nPointsY;
        int m1 = this.zData.length;
        int m2 = this.zData[0].length;
        
        // Check orientation
        if(this.nPointsX==m2){
            if(this.nPointsY!=m1){
                throw new IllegalArgumentException("The lengths of the x data arrays, " + this.nPointsX + " and " + this.nPointsY + ", do not match the dimensions of the  y data matrix, " + m1 + " and " + m2);       
            }
        }
        else{
            if(this.nPointsY==m2){
                if(this.nPointsX!=m1){
                    throw new IllegalArgumentException("The lengths of the x data arrays, " + this.nPointsX + " and " + this.nPointsY + ", do not match the dimensions of the  y data matrix, " + m1 + " and " + m2);       
                }
                else{
                    this.zData = this.transpose(this.zData);
                    System.out.println("zData transposed to match the dimensions of the xData and yData");
                }
            }
        }
   
        // Create an array of x values as BigDecimal if not already done
        if(!this.arbprec){
            this.xBDdata = new BigDecimal[this.nPointsX];
            this.yBDdata = new BigDecimal[this.nPointsY];
            for(int i=0; i<this.nPointsX; i++){
               this.xBDdata[i] = new BigDecimal((new Double(this.xData[i])).toString()); 
            }
            for(int i=0; i<this.nPointsY; i++){
               this.yBDdata[i] = new BigDecimal((new Double(this.yData[i])).toString()); 
            }
        }       
    }
    
    // Transpose  data
    private double[][] transpose(double[][] yy){
        int n1 = yy.length;
        int n2 = yy[0].length;
        double[][] hold = new double[n2][n1];
        for(int i=0; i<n1; i++){
           for(int j=0; j<n2; j++){
               hold[j][i] = yy[i][j];
           } 
        }
        return hold;
    }

    // order data in ascending x-values, , z=f(x,y)
    private void ascend(){
        boolean test1 = true;
        boolean test2= true;
        int ii = 1;
        while(test1){
            if(this.yData[ii]<this.yData[ii-1]){
                test1=false;
                test2=false;
            }
            else{
                ii++;
                if(ii>=this.nPointsY)test1 = false;
            }
        }
        if(!test2){
            ArrayMaths am = new ArrayMaths(this.yData);
            am =  am.sort();
            int[] indices = am.originalIndices();
            double[] holdy = new double[this.nPointsY];
            double[][] holdz = new double[this.nPointsY][this.nPointsX];
            BigDecimal[] holdyBD = new BigDecimal[this.nPointsY];
            BigDecimal[][] holdzBD = null;
            if(this.arbprec)holdzBD = new BigDecimal[this.nPointsY][this.nPointsX];
            for(int i=0; i<this.nPointsY; i++){
                holdy[i] = this.yData[indices[i]];
                holdyBD[i] = this.yBDdata[indices[i]];
                for(int j=0; j<this.nPointsX; j++){
                    holdz[i][j] = this.zData[indices[i]][j];
                    if(this.arbprec)holdzBD[i][j] = this.zBDdata[indices[i]][j];
                }
            }
            for(int i=0; i<this.nPointsY; i++){
                this.yData[i] = holdy[i];
                this.yBDdata[i] = holdyBD[i];
                for(int j=0; j<this.nPointsX; j++){
                    this.zData[i][j] = holdz[i][j];
                    if(this.arbprec)this.zBDdata[i][j] = holdzBD[i][j];
                }
            }
        } 
        test1 = true;
        test2= true;
        ii = 1;
        while(test1){
            if(this.xData[ii]<this.xData[ii-1]){
                test1=false;
                test2=false;
            }
            else{
                ii++;
                if(ii>=this.nPointsX)test1 = false;
            }
        }
        if(!test2){
            ArrayMaths am = new ArrayMaths(this.xData);
            am =  am.sort();
            int[] indices = am.originalIndices();
            double[] holdx = new double[this.nPointsX];
            double[][] holdz = new double[this.nPointsY][this.nPointsX];
            BigDecimal[] holdxBD = new BigDecimal[this.nPointsX];
            BigDecimal[][] holdzBD = null;
            if(this.arbprec)holdzBD = new BigDecimal[this.nPointsY][this.nPointsX];
            for(int i=0; i<this.nPointsX; i++){
                holdx[i] = this.xData[indices[i]];
                holdxBD[i] = this.xBDdata[indices[i]];
                for(int j=0; j<this.nPointsY; j++){
                    holdz[j][i] = this.zData[j][indices[i]];
                    if(this.arbprec)holdzBD[j][i] = this.zBDdata[j][indices[i]];
                }
            }
            for(int i=0; i<this.nPointsX; i++){
                this.xData[i] = holdx[i];
                this.xBDdata[i] = holdxBD[i];
                for(int j=0; j<this.nPointsY; j++){
                    this.zData[j][i] = holdz[j][i];
                    if(this.arbprec)this.zBDdata[j][i] = holdzBD[j][i];
                }
            }
        } 
    }
       
    
    // MOVING AVERAGE WINDOW SMOOTHING
    // Smooth with a moving average window of dimensions maWindowWidthx and maWindowWidthy
    public double[][] movingAverage(int maWindowWidthx, int maWindowWidthy){
    
        this.lastMethod = 1;
        this.zDataMovAv = new double[this.nPointsY][this.nPointsX];
        this.zDataMovAvBD = new BigDecimal[this.nPointsY][this.nPointsX];

        // adjust window width to an odd number of points
        this.maWindowWidthx = this.windowLength(maWindowWidthx);
        int wwx = (this.maWindowWidthx - 1)/2;
        this.maWindowWidthy = this.windowLength(maWindowWidthy);
        int wwy = (this.maWindowWidthy - 1)/2;

        // Set x window limits
        int lpx = 0;
        int upx = 0;
        int lpy = 0;
        int upy = 0;
        for(int i=0; i<this.nPointsX; i++){
            if(i>=wwx){
                lpx = i - wwx;
            }
            else{
                lpx = 0;               
            }
            if(i<=this.nPointsX-wwx-1){                    
                upx = i + wwx;
            }
            else{
                upx = this.nPointsX - 1;
            }
            int nw1 = upx - lpx + 1;
            
            // Set y window limits
            for(int j=0; j<this.nPointsY; j++){        
                if(j>=wwy){
                    lpy = j - wwy;
                }
                else{
                    lpy = 0;               
                }
                if(j<=this.nPointsY-wwy-1){                    
                    upy = j + wwy;
                }
                else{
                    upy = this.nPointsY - 1;
                }
                int nw2 = upy - lpy + 1;
  
                // Apply moving average window
                if(this.arbprec){
                    BigDecimal sumbd = new BigDecimal("0.0");
                    for(int k1=lpx; k1<=upx; k1++){
                        for(int k2=lpy; k2<=upy; k2++){
                            sumbd = sumbd.add(this.zBDdata[k2][k1]);
                        }
                    }
                    String xx = (new Integer(nw1*nw2)).toString();
                    this.zDataMovAvBD[j][i] = sumbd.divide(new BigDecimal(xx), BigDecimal.ROUND_HALF_UP);
                    this.zDataMovAv[j][i] = this.zDataMovAvBD[j][i].doubleValue();
                }
                else{
                    double sum = 0.0;
                    for(int k1=lpx; k1<=upx; k1++){
                        for(int k2=lpy; k2<=upy; k2++){
                            sum += this.zData[k2][k1];
                        }
                    }             
                    this.zDataMovAv[j][i] = sum/(nw1*nw2);
                    String xx = (new Double(this.zDataMovAv[j][i])).toString();
                    this.zDataMovAvBD[j][i] = new BigDecimal(xx);
                }
            }
        }
        
        // Set up interpolation
        this.bcsMovAv = new BiCubicSpline(this.yData, this.xData, this.zDataMovAv);
                
        this.calcMovAv = true;
        return Conv.copy(this.zDataMovAv);
     
    }
    
    // Smooth with a moving average window of dimensions maWindowWidth x maWindowWidth
    public double[][] movingAverage(int maWindowWidth){
        return this.movingAverage(maWindowWidth, maWindowWidth);   
    }
    
    // Smooth with a moving average window of dimensions maWindowWidthx and maWindowWidthy
    // Return values as BigDecimal
    public BigDecimal[][] movingAverageAsBigDecimal(int maWindowWidthx, int maWindowWidthy){
        this.movingAverage(maWindowWidthx, maWindowWidthy);
        return Conv.copy(this.zDataMovAvBD);
    }
    
    // Smooth with a moving average window of dimensions maWindowWidth x maWindowWidth
    // Return values as BigDecimal
    public BigDecimal[][] movingAverageAsBigDecimal(int maWindowWidth){
        this.movingAverage(maWindowWidth, maWindowWidth);
        return Conv.copy(this.zDataMovAvBD);
    }
    
    // Adust width to an odd number of points
    private int windowLength(int width){
    
        int ww = 0;
        if(Fmath.isEven(width)){
            ww = width+1;
        }
        else{
           ww = width;
        }
        return ww;
    }

    
    // SAVITZKY-GOLAY FILTER
    
    // Smooth with a Savitzky-Golay filter with a window of dimensions sgWindowWidthx and sgWindowWidthy
    public double[][] savitzkyGolay(int sgWindowWidthx, int sgWindowWidthy){              
       
        this.lastMethod = 2;
  
        this.zDataSavGol = new double[this.nPointsY][this.nPointsX];
        
        // adjust window width to an odd number of points
        this.sgWindowWidthx = this.windowLength(sgWindowWidthx);
        this.sgWindowWidthy = this.windowLength(sgWindowWidthy);
        
        // Apply filter 
        this.savitzkyGolayCommon(this.sgWindowWidthx, this.sgWindowWidthy);
                       
        // Set up interpolation
        this.bcsSavGol = new BiCubicSpline(this.yData, this.xData, Conv.copy(this.zDataSavGol));
         
                 
        this.calcSavGol = true;
        return Conv.copy(this.zDataSavGol);
    }
    
    // Smooth with a Savitzky-Golay filter with a window of dimensions sgWindowWidth x sgWindowWidth
    public double[][] savitzkyGolay(int sgWindowWidth){              
       return this.savitzkyGolay(sgWindowWidth, sgWindowWidth); 
    }
   
    // Common method for smoothing with a Savitzky-Golay filter with a window of widthx times widthy
    private double[][] savitzkyGolayCommon(int widthx, int widthy){
        
        // Set filter dimensions
        int wwx = (widthx - 1)/2;
        int wwy = (widthy - 1)/2;
        
        // Calculate filter coefficients
        double[] coeff = (this.savitzkyGolayFilter(wwx, wwx, wwy, wwy))[0];
        
        // Padout the data to solve edge effects
        double[][] psData = this.padData(this.zData, wwx, wwy);

        // Apply filter       
        for(int i=wwy; i<this.nPointsY+wwy; i++){
            for(int j=wwx; j<this.nPointsX+wwx; j++){  
                double sum = 0.0;
                int counter = 0;
                for(int k1=i-wwy; k1<=i+wwy; k1++){
                    for(int k2=j-wwx; k2<=j+wwx; k2++){               
                        sum += psData[k1][k2]*coeff[counter++];
                    }
                }
                this.zDataSavGol[i-wwy][j-wwx] = sum;
            }
        }
        return this.zDataSavGol;
    }
 
    // Pad out data to solve edge effects
    private double[][] padData(double[][] data, int wwx, int wwy){
        
        // Pad out to solve edge effects
        // Set dimensions
        int ny = data.length;
        int nx = data[0].length;
        
        // Create matrix for padding
        double[][] psData = new double[ny+2*wwy][nx+2*wwx];
        
        // fill central rectangle with true data
        for(int i=0; i<ny; i++){
           for(int j=0; j<nx; j++){ 
               psData[i+wwy][j+wwx] = data[i][j];
           }
        }
        
        // fill top side with padding
        for(int i=0; i<wwy; i++){
            for(int j=wwx; j<nx+wwx; j++){
                psData[i][j] = psData[wwy][j];
            }
        }
        
        // fill left side with padding
        for(int j=0; j<wwx; j++){
            for(int i=wwy; i<ny+wwy; i++){
                psData[i][j] = psData[i][wwx];
            }
        } 
        
        // fill right side with padding
        for(int j=wwx+nx; j<nx+2*wwx; j++){
            for(int i=wwy; i<ny+wwy; i++){
                psData[i][j] = psData[i][wwx+nx-1];
            }
        }
                
        // fill bottom side with padding
        for(int i=wwy+ny; i<ny+2*wwy; i++){
            for(int j=wwx; j<nx+wwx; j++){
                psData[i][j] = psData[ny+wwy-1][j];
            }
        }

        // fill top left corner with padding
        for(int i=0; i<wwy; i++){
            for(int j=0; j<wwx; j++){
                psData[i][j] = psData[wwy][wwx];
            }
        }
        
        // fill top right corner with padding
        for(int i=0; i<wwy; i++){
            for(int j=nx+wwx; j<nx+2*wwx; j++){
                psData[i][j] = psData[wwy][nx+wwx-1];
            }
        }
        
        // fill bottom left corner with padding
        for(int i=ny+wwy; i<ny+2*wwy; i++){
            for(int j=0; j<wwx; j++){
                psData[i][j] = psData[ny+wwy-1][wwx];
            }
        }
        
        // fill bottom right corner with padding
        for(int i=ny+wwy; i<ny+2*wwy; i++){
            for(int j=nx+wwx; j<nx+2*wwx; j++){
                psData[i][j] = psData[ny+wwy-1][nx+wwx-1];
            }
        }
        return psData;
}

    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // m.nth derivative also calculated
    public double[][][] savitzkyGolay(int sgWindowWidthx, int sgWindowWidthy, int m, int n){
        if(n+m>this.sgPolyDeg)throw new IllegalArgumentException("The sum of the derivative orders " + m + " plus " + n + ", must be less than or equal to the polynomial degree, " + this.sgPolyDeg + ".");
 
        this.lastMethod = 2;
        double[][][] ret = new double[2][this.nPointsY][this.nPointsX];
        
         // adjust window width to an odd number of points
        this.sgWindowWidthx = this.windowLength(sgWindowWidthx);
        int wwx = (this.sgWindowWidthx - 1)/2;
        this.sgWindowWidthy = this.windowLength(sgWindowWidthy);
        int wwy = (this.sgWindowWidthy - 1)/2;
        
        if(!this.calcSavGol)this.savitzkyGolay(sgWindowWidthx, sgWindowWidthy);
        ret[0] = this.zDataSavGol;
        
        // get filter coefficients
        int pointer = 0;
        boolean test = true;
        int len = this.sgCoeffIndices.length;
        while(test){
            if(this.sgCoeffIndices[pointer][0]==m && this.sgCoeffIndices[pointer][1]==n)test = false;
            pointer++;
            if(pointer>=len)throw new IllegalArgumentException("It should not have been possible to reach this situation, m = " + m + ", n = " + n);
        }
        double[] coeff = this.sgArrayC[pointer]; 
        
        // Padout the data to solve edge effects
        double[][] psData = this.padData(this.zData, wwx, wwy);

        // Apply filter       
        for(int i=wwy; i<this.nPointsY+wwy; i++){
            for(int j=wwx; j<this.nPointsX+wwx; j++){  
                double sum = 0.0;
                int counter = 0;
                for(int k1=i-wwy; k1<=i+wwy; k1++){
                    for(int k2=j-wwx; k2<=j+wwx; k2++){               
                        sum += psData[k1][k2]*coeff[counter++];
                    }
                }
                ret[1][i-wwy][j-wwx] = sum;
            }
        }
        
        this.derivSavGol = ret[1];
        this.nthSet = true;
        return ret;
    }
        
    // Savitzky-Golay filter
    // bpx = number of backward points, x direction
    // fpx = number of forward points, x direction
    // bpy = number of backward points, y direction
    // fpy = number of forward points, y direction    
    public double[][] savitzkyGolayFilter(int bpx, int fpx, int bpy, int fpy){
        
        int wx = bpx + fpx + 1;                //filter x legth 
        int wy = bpy + fpy + 1;                //filter y legth 
        int www = wx*wy;                       // number of points in the window
        double[] coeff = new double[www];      // Savitzky-Golay coefficients 

         
        int[][] values = new int[www][2];
        int counter = 0;
        for(int i = 0; i<wx; i++){
            for(int j =0; j<wy; j++){
                values[counter][0] = i-bpx;
                values[counter++][1] = j-bpy;
            }
        }
       
        double[][] x = new double[www][this.nSGcoeff];
        for(int i=0; i<www; i++){
            for(int j = 0; j<this.nSGcoeff; j++){
                x[i][j] = Math.pow(values[i][0], this.sgCoeffIndices[j][0])*Math.pow(values[i][1], this.sgCoeffIndices[j][1]);
            }
        }            
              
        Matrix matX = new Matrix(x);
        Matrix matT = matX.transpose();
        Matrix matTX = matT.times(matX);        
        Matrix matI = matTX.inverse();      
        Matrix matC = matI.times(matT);
        this.sgArrayC = matC.getArrayCopy();
 
        return this.sgArrayC;        
    }
    
    // Savitzky-Golay filter  - static method
    // bpx = number of backward points, x-direction
    // fpx = number of forward points, x-direction
    // bpy = number of backward points, y-direction
    // fpy = number of forward points, y-direction
    // deg = degree of the fitting polynomial
    public static double[][] savitzkyGolayFilter(int bpx, int fpx, int bpy, int fpy, int deg){
        SurfaceSmooth susm = new SurfaceSmooth();
        susm.setSGpolyDegree(deg);
        return susm.savitzkyGolayFilter(bpx, fpx, bpy, fpy);
    }
        
    // Get Savitzky-Golay coefficients used
    public double[][] getSGcoefficients(){
        if(this.sgArrayC==null)throw new IllegalArgumentException("No Savitzky-Golay coefficients have been calculated");
        return this.sgArrayC;
    }
    
    // Get polynomial coefficient indices
    public int[][] getSGPolyIndices(){
        return this.sgCoeffIndices;  
    }
    
    // Savitzky-Golay filter indices  - static method
    // deg = degree of the fitting polynomial
    public static int[][] filterIndices(int deg){
        SurfaceSmooth susm = new SurfaceSmooth();
        susm.setSGpolyDegree(deg);
        return susm.getSGPolyIndices();
    }
    
    // Set the Savitzky-Golay smoothing polynomial order
    // Default value is 4
    public void setSGpolyDegree(int degree){
        this.sgPolyDeg = degree;
        this.polyIndices();
                
    } 
    
    // Get the Savitzky-Golay smoothing polynomial order
    // Default value is 4
    public int getSGpolyDegree(){
        return this.sgPolyDeg;
    }

    // SMOOTHED VALUES
       
    // Return moving average smoothed y data
    public double[][] getMovingAverageValues(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.zDataMovAv);
    } 
     
    // Return moving average smoothed y data as BigDecimal
    public BigDecimal[][] getMovingAverageValuesAsBigDecimal(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.zDataMovAvBD);
    }
    
    // Return Savitzky-Golay smoothed y data
    public double[][] getSavitzkyGolaySmoothedValues(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return Conv.copy(this.zDataSavGol);
    }
   
    // Return Savitzky-Golay smoothed last calculated derivatives d^m+nz/dx^mdy^n
    public double[][] getSavitzkyDerivatives(){
        if(!this.nthSet)throw new IllegalArgumentException("No Savitzky-Golay derivative smoothing method has been called");
        return Conv.copy(this.derivSavGol);
    }
    
    // EXTENT OF THE SMOOTHING
    
    // Return the extent of the moving average window smoothing
    public double extentMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        this.extentMovAv = this.extent(this.zData, this.zDataMovAv);
        return this.extentMovAv;
    }
    
    // Return the extent of the Savitzky-Golay smoothing
    public double extentSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        this.extentSavGol = this.extent(this.zData, this.zDataSavGol);
        return this.extentSavGol;
    }
    
    // Calculate extent of smoothing
    private double extent(double[][] zOrigl, double[][] zSmooth){
                
        ArrayMaths am = new ArrayMaths(zOrigl);
        double min = am.getMinimum();
        double max = am.getMaximum();       
        double range = max - min;
        
        double sum = 0.0;
        for(int i=0; i<this.nPointsX; i++){
            for(int j=0; j<this.nPointsY; j++){
                sum += Math.abs(zOrigl[j][i] - zSmooth[j][i])/range;
            }
        }       
        sum /= this.nPoints;
        return sum;
    }
        
    // INTERPOLATION
    // interpolate the Savitzky-Golay smoothed data
    public double interpolateSavitzkyGolay(double xi, double yi){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return bcsSavGol.interpolate(yi, xi);
    }
    
    // interpolate the Savitzky-Golay smoothed data
    public double interpolateMovingAverage(double xi, double yi){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return bcsMovAv.interpolate(yi, xi);
    }
    
    
    // PLOTTING
    
    // Plot x section for a given y value index - Savitzky-Golay
    public void plotSavitzkyGolayX(int yIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        int flag = 0;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, yIndex, yValue);        
    }
    
    // Plot x section for a given y value  - Savitzky-Golay
    public void plotSavitzkyGolayX(double yValue){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int flag = 0;
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, yIndex, yValue);        
    }
    
    // Plot y section for a given x value index - Savitzky-Golay
    public void plotSavitzkyGolayY(int xIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
        int flag = 1;
        double xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, xValue);        
    }
    
    // Plot y section for a given x value  - Savitzky-Golay
    public void plotSavitzkyGolayY(double xValue){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int flag = 1;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, xValue);        
    }
    
    // Plot x section for a given y value index - Moving Average
    public void plotMovingAverageX(int yIndex){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        int flag = 2;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, yIndex, yValue);        
    }
    
    // Plot x section for a given y value  - Moving Average
    public void plotMovingAverageX(double yValue){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        int flag = 2;
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, yIndex, yValue);        
    }
    
    // Plot y section for a given x value index - Moving Average
    public void plotMovingAverageY(int xIndex){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
        int flag = 3;
        double xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, xValue);        
    }
    
    // Plot y section for a given x value  - Moving Average
    public void plotMovingAverageY(double xValue){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        int flag = 3;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, xValue);        
    }
    
    // find index for a value
    private int findValue(double[] array, double value){
        int len = array.length;
        boolean test = true;
        int counter = 0;
        while(test){
           if(Fmath.isEqualWithinLimits(array[counter], value, Math.abs(value)*0.001)){
               test = false;
           } 
           else{
               counter++;
               if(counter>=len)throw new IllegalArgumentException("The entered plot value, " + value + ",  must equal an entered data value");
           }
        }
        return counter;
    }
    
    // Common plot method
    private void commonPlot(int flag, int point, double value){

        String title0 = null;
        String title1 = null;
        String title1b = ",  Original data - circles,  Smoothed data - squares";
        String xleg = null;
        String yleg = null;
        int[] xPoints ={0, this.nPointsX/4, this.nPointsX/2, 3*this.nPointsX/4, this.nPointsX-1};
        int[] yPoints ={0, this.nPointsY/4, this.nPointsY/2, 3*this.nPointsY/4, this.nPointsY-1};
        double[][] data = new double[8][];
        double[] holdx1 = new double[5];
        double[] holdx2 = new double[5];
        double[] holdy1 = new double[5];
        double[] holdy2 = new double[5];
        
        switch(flag){
            case 0: // x plot Savitzky-Golay
                    title0 = "Savitzky-Golay smoothing with an x by y window of " + this.sgWindowWidthx + " by "+ this.sgWindowWidthy + " points";
                    title1 = "Plot of z versus x values for a y value of " + value + title1b;
                    xleg = "x values";
                    yleg = "y values";
                    data[0] = this.xData;
                    data[1] = this.zData[point];
                    data[2] = this.xData;
                    data[3] = this.zDataSavGol[point];
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.xData[xPoints[i]];
                        holdy1[i] = this.zData[point][xPoints[i]];
                        holdx2[i] = this.xData[xPoints[i]];
                        holdy2[i] = this.zDataSavGol[point][xPoints[i]];
                    }
                    break;
            case 1: // y plot Savitzky-Golay
                    title0 = "Savitzky-Golay smoothing with an x by y window of " + this.sgWindowWidthx + " by "+ this.sgWindowWidthy + " points";
                    title1 = "Plot of z versus y values for a x value of " + value + title1b;
                    xleg = "y values";
                    yleg = "x values";
                    data[0] = this.yData;
                    data[2] = this.yData;
                    data[1] = new double[this.nPointsY];
                    data[3] = new double[this.nPointsY];
                    for(int i=0; i<nPointsY; i++){
                        data[1][i] = this.zData[i][point];
                        data[3][i] = this.zDataSavGol[i][point];
                    }
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.yData[yPoints[i]];
                        holdy1[i] = data[1][yPoints[i]];
                        holdx2[i] = this.yData[yPoints[i]];
                        holdy2[i] = data[3][yPoints[i]];
                    }
                    break;
            case 2: // x plot Moving Average
                    title0 = "Moving Average smoothing with an x by y window of " + this.sgWindowWidthx + " by "+ this.sgWindowWidthy + " points";
                    title1 = "Plot of z versus x values for a y value of " + value + title1b;
                    xleg = "x values";
                    yleg = "y values";
                    data[0] = this.xData;
                    data[1] = this.zData[point];
                    data[2] = this.xData;
                    data[3] = this.zDataMovAv[point];
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.xData[xPoints[i]];
                        holdy1[i] = this.zData[point][xPoints[i]];
                        holdx2[i] = this.xData[xPoints[i]];
                        holdy2[i] = this.zDataMovAv[point][xPoints[i]];
                    }
                    break;
            case 3: // y plot Moving Average
                    title0 = "Moving Average smoothing with an x by y window of " + this.sgWindowWidthx + " by "+ this.sgWindowWidthy + " points";
                    title1 = "Plot of z versus y values for a x value of " + value + title1b;
                    xleg = "y values";
                    yleg = "x values";
                    data[0] = this.yData;
                    data[2] = this.yData;
                    data[1] = new double[this.nPointsY];
                    data[3] = new double[this.nPointsY];
                    for(int i=0; i<nPointsY; i++){
                        data[1][i] = this.zData[i][point];
                        data[3][i] = this.zDataMovAv[i][point];
                    }
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.yData[yPoints[i]];
                        holdy1[i] = data[1][yPoints[i]];
                        holdx2[i] = this.yData[yPoints[i]];
                        holdy2[i] = data[3][yPoints[i]];
                    }
                    break;
        }
        data[4] = holdx1;
        data[5] = holdy1;
        data[6] = holdx2;
        data[7] = holdy2;
     
        PlotGraph pg = new PlotGraph(data);
        int[] sp = {0,0,1,2};
        int[] sl = {3,3,0,0};
        pg.setPoint(sp);
        pg.setLine(sl);
        pg.setGraphTitle(title0);
        pg.setGraphTitle2(title1);
        pg.setXaxisLegend(xleg);
        pg.setYaxisLegend(yleg);
        pg.plot();
    }    
}