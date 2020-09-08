/*
*   CLASS:      CurveSmooth
*
*   USAGE:      Class for smoothing a curve and obtaining the maxima and minima of a curve
*               Smoothing methods: moving average window or Savitzky-Golay filter
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:       February 2012
*   AMENDED:    26-27 February 2012, 3-17 March 2012 
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Smooth.html
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
import flanagan.math.Matrix;
import flanagan.interpolation.CubicSpline;
import flanagan.plot.*;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;

public class CurveSmooth{
    
    private double[] xData = null;                                  // original x data, y = f(x)  
    private double[] yData = null;                                  // original y data, y = f(x)  
    private BigDecimal[] xBDdata = null;                            // original arbitrary precision x data, y = f(x)  
    private BigDecimal[] yBDdata = null;                            // original arbitrary precision y data, y = f(x)  
    private int nPoints = 0;                                        // number of data points
    private boolean arbprec = false;                                // = true if arbitrary precision data entered
      
    private double[] yDataMovAv = null;                             // moving average smoothed y data, double precision, y = f(x)  
    private BigDecimal[] yDataMovAvBD = null;                       // moving average smoothed y data, arbitrary precision, y = f(x)  
    private double[] yDataSavGol = null;                            // Savitzky-Golay smoothed y data, y = f(x)  
    private double[] yDataSavGolFirst = null;                       // Savitzky-Golay smoothed first derivative (dy/dx) data, y = f(x)  
    private double[] yDataSavGolSecond = null;                      // Savitzky-Golay smoothed second derivative (d^2y/dx^2) data, y = f(x)  
    private double[] yDataSavGolNth = null;                         // Savitzky-Golay smoothed nth derivative (d^ny/dx^n) data, y = f(x)  
        
    private int lastMethod = 0;                                     // = 0: none called
                                                                    // = 1: moving window average
                                                                    // = 2: Savitzky-Golay filter                                                           
    private int nMethods = 2;                                       // number of methods
    private int lastPlotMethod = 0;                                 // = 0: none called
                                                                    // = 1: moving window average
                                                                    // = 2: Savitzky-Golay filter, no derivatives
                                                                    // = 3: Savitzky-Golay filter + first derivative
                                                                    // = 4: Savitzky-Golay filter + second derivative
                                                                    // = 5: Savitzky-Golay filter + nth derivative                                                              
                                                              
    private int maWindowWidth = 0;                                  // width of the smoothing window in the running average method, y=f(x)
   
    private int sgWindowWidth = 0;                                  // width of smoothing window in the Savitzky-Golay method, y=f(x)
    private int sgPolyDeg = 4;                                      // Savitzky-Golay smoothing polynomial degree
    private double[] derivSavGol = null;                            // Savitzky-Golay smoothed derivative (d^ny/dx^n), y = f(x)  
    private int sgDerivOrderUsed = 0;                               // Savitzky-Golay derivative order used in last derivative smoothing
     
    private boolean calcSavGol = false;                             // = true when Savitzky-Golay smoothing performed
    private boolean calcMovAv = false;                              // = true when moving average smoothing performed
    
    private boolean firstDeriv = false;                             // = true when Savitzky-Golay first derivative smoothing performed
    private boolean secondDeriv = false;                            // = true when Savitzky-Golay second derivative smoothing performed
    private boolean nthSet = false;                                 // = true when any Savitzky-Golay derivative smoothing performed
    
    private double[][] sgArrayC = null;                             // Savitzky-Golay filter coefficients
    private double[] sgCoeff = null;                                // Savitzky-Golay filter coefficients: first row
    
    private double extentMovAv = -1.0;                              // Extent of the moving average window smoothing
    private double extentSavGol = -1.0;                             // Extent of the Savitzky-Golay smoothing
    
    private ArrayList<Double> almin = new ArrayList<Double>();      // ArrayList of minima points - working array
    private ArrayList<Double> almax = new ArrayList<Double>();      // ArrayList of maxima points - working array
    private ArrayList<BigDecimal> alminBD = new ArrayList<BigDecimal>();      // ArrayList of arbitrary precision minima points - working array
    private ArrayList<BigDecimal> almaxBD = new ArrayList<BigDecimal>();      // ArrayList of arbitrary precision mimaxima points - working array
    private double[][][] minima = null;                             // double[][][] array of minima points; minima[][0] - x coordinate, minima[][1] - y coordinate,
    private double[][][] maxima = null;                             // double[][][] array of maxima points; maxima[][0] - x coordinate, maxima[][1] - y coordinate,                                                               
    private BigDecimal[][][] minimaBD = null;                       // minima as arbitrary precision  
    private BigDecimal[][][] maximaBD = null;                       // maxima as arbitrary precision
                                                                    // [method][2: =0 x values, =1 y values][number of minima] 
                                                                    // [0][][]  = unsmoothed data
                                                                    // [1][][]  = moving average smoothed data
                                                                    // [2][][]  = Savitzky-Golay smoothed data       
    private int[] nMin = null;                                      // number of minima 
    private int[] nMax = null;                                      // number of maxima                                                                 
                                                                    // [0]  = unsmoothed data
                                                                    // [1]  = moving average smoothed data
                                                                    // [2]  = Savitzky-Golay smoothed data
    private boolean[] minimaCalc = null;                            // = true when minima found
    private boolean[] maximaCalc = null;                            // = true when maxima found
                                                                    // [0]  = unsmoothed data
                                                                    // [1]  = moving average smoothed data
                                                                    // [2]  = Savitzky-Golay smoothed data
       
    private CubicSpline csSavGol = null;                            // Cubic spline interpolation of the Savitzky-Golay smoothed data
    private CubicSpline csMovAv = null;                             // Cubic spline interpolation of the moving averagey smoothed data
    
    // CONSTRUCTORS
    
    // Constructor - data as double, y = f(x)
    public CurveSmooth(double[] x, double[] y){ 
        this.xData = x;
        this.yData = y;
        this.check();
        this.ascend();
    }
    
    // Constructor - data as double - no x data, y = f(x)
    public CurveSmooth(double[] y){ 
        int n = y.length;
        this.yData = y;
        this.xData = new double[n];
        for(int i=0; i<n; i++)this.xData[i] = i;
        this.check();
    }
    
    // Constructor - data as float, y = f(x)
    public CurveSmooth(float[] x, float[] y){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as float - no x data, y = f(x)
    public CurveSmooth(float[] y){ 
        int n = y.length;
        this.yData = new double[n];
        this.xData = new double[n];
        for(int i=0; i<n; i++){
            this.xData[i] = i;
            this.yData[i] = (double)y[i];
        }
        this.check();
    }
    
    // Constructor - data as long, y = f(x)
    public CurveSmooth(long[] x, long[] y){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.check();
        this.ascend();
    }
     
    // Constructor - data as long - no x data, y = f(x)
    public CurveSmooth(long[] y){ 
        int n = y.length;
        this.yData = new double[n];
        this.xData = new double[n];
        for(int i=0; i<n; i++){
            this.xData[i] = i;
            this.yData[i] = (double)y[i];
        }
        this.check();
    }
    
    // Constructor - data as int, y = f(x)
    public CurveSmooth(int[] x, int[] y){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.check();
        this.ascend();
    }

    
    // Constructor - data as int - no x data, y = f(x)
    public CurveSmooth(int[] y){ 
        int n = y.length;
        this.yData = new double[n];
        this.xData = new double[n];
        for(int i=0; i<n; i++){
            this.xData[i] = i;
            this.yData[i] = (double)y[i];
        }
        this.check();
    }
    
    // Constructor - data as BigDecimal, y = f(x)
    public CurveSmooth(BigDecimal[] x, BigDecimal[] y){ 
        this.arbprec = true;
        this.xBDdata = x;
        this.yBDdata = y;
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.check();
        this.ascend();
    } 
    
    // Constructor - data as BigDecimal - no x data, y = f(x)
    public CurveSmooth(BigDecimal[] y){ 
        this.arbprec = true;
        int n = y.length;
        this.yData = new double[n];
        this.xData = new double[n];
        this.yBDdata = new BigDecimal[n];
        this.xBDdata = new BigDecimal[n];
        for(int i=0; i<n; i++){
            this.xData[i] = i;
            this.yData[i] = y[i].doubleValue();
            String ii = (new Integer(i)).toString();
            this.xBDdata[i] = new BigDecimal(ii);
            this.yBDdata[i] = y[i];
        }
        this.check();
    }
    
    // Constructor - data as BigInteger, y = f(x)
    public CurveSmooth(BigInteger[] x, BigInteger[] y){ 
        this.arbprec = true;
        int n = x.length;
        this.xBDdata = new BigDecimal[n];
        this.yBDdata = new BigDecimal[n];
        for(int i=0; i<n; i++){
            this.xBDdata[i] = new BigDecimal(x[i]);
            this.yBDdata[i] = new BigDecimal(y[i]);
        }
       
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as BigInteger - no x data, y = f(x)
    public CurveSmooth(BigInteger[] y){ 
        this.arbprec = true;
        int n = y.length;
        this.yData = new double[n];
        this.xData = new double[n];
        this.yBDdata = new BigDecimal[n];
        this.xBDdata = new BigDecimal[n];
        for(int i=0; i<n; i++){
            this.xData[i] = i;
            this.yData[i] = y[i].doubleValue();
            String xx = (new Double(i)).toString();
            this.xBDdata[i] = new BigDecimal(xx);
            this.yBDdata[i] = new BigDecimal(y[i]);
        }
        this.check();
    }

    
    // Constructor - data as ArrayMaths, y = f(x)
    public CurveSmooth(ArrayMaths x, ArrayMaths y){ 
        this.xData = x.array();
        this.yData = y.array();
        String[] type = y.originalArrayTypes();           
        int counter = 0;
        int nn = type.length;
        for(int i=0; i<type.length; i++){
            if(type[i].equals("BigDecimal") || type[i].equals("BigInteger"))counter++;
        }
        if(counter==nn){
            this.arbprec = true;
            this.yBDdata=y.array_as_BigDecimal();
        }
        this.xData = new double[nn];
        this.xBDdata = x.array_as_BigDecimal();
        this.check();
        this.ascend();
    }
       
    // Constructor - data as ArrayMaths - no x data, y = f(x)
    public CurveSmooth(ArrayMaths y){ 
        this.yData = y.array();
        String[] type = y.originalArrayTypes();           
        int counter = 0;
        int nn = type.length;
        for(int i=0; i<type.length; i++){
            if(type[i].equals("BigDecimal") || type[i].equals("BigInteger"))counter++;
        }
        if(counter==nn){
            this.arbprec = true;
            this.yBDdata=y.array_as_BigDecimal();
        }
        this.xData = new double[nn];
        if(this.arbprec)this.xBDdata = new BigDecimal[nn];
        for(int i=0; i<nn; i++){
            this.xData[i] = i;
            if(this.arbprec)this.xBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        this.check();
    }

    // Constructor - no data entered
    // private - for internal use
    private CurveSmooth(){
    }
    
    // Check for correct dimensions, y = f(x) 
    private void check(){
        // Dimension arrays
        this.nPoints = this.xData.length;
        int m = this.yData.length;
        if(m!=this.nPoints)throw new IllegalArgumentException("The length of the x data array, " + this.nPoints + ", must be the same as the length of the y data array, " + m);       
        if(m<5)throw new IllegalArgumentException("There must be at least five data points");
        this.minima = new double[this.nMethods+1][2][];
        this.maxima = new double[this.nMethods+1][2][];
        this.minimaBD = new BigDecimal[this.nMethods+1][2][];
        this.maximaBD = new BigDecimal[this.nMethods+1][2][];
        this.nMin = new int[this.nMethods+1];
        this.nMax = new int[this.nMethods+1];
        this.minimaCalc = new boolean[this.nMethods+1];
        this.maximaCalc = new boolean[this.nMethods+1];
        for(int i=0; i<this.nMethods; i++){
            this.minimaCalc[i] = false;
            this.maximaCalc[i] = false;
        }
   
        // Create an array of x values as BigDecimal if not already done
        if(!this.arbprec){
            this.xBDdata = new BigDecimal[this.nPoints];
            for(int i=0; i<this.nPoints; i++){
               this.xBDdata[i] = new BigDecimal((new Double(this.xData[i])).toString()); 
            }
        }
        
    }
    
    // order data in ascending x-values, y = f(x)
    private void ascend(){
        boolean test1 = true;
        boolean test2= true;
        int ii = 1;
        while(test1){
            if(this.xData[ii]<this.xData[ii-1]){
                test1=false;
                test2=false;
            }
            else{
                ii++;
                if(ii>=this.nPoints)test1 = false;
            }
        }
        if(!test2){
            ArrayMaths am = new ArrayMaths(this.xData);
            am =  am.sort();
            int[] indices = am.originalIndices();
            double[] holdx = new double[this.nPoints];
            double[] holdy = new double[this.nPoints];
            BigDecimal[] holdxBD = new BigDecimal[this.nPoints];
            BigDecimal[] holdyBD = null;
            if(this.arbprec)holdyBD = new BigDecimal[this.nPoints];
            for(int i=0; i<this.nPoints; i++){
                holdx[i] = this.xData[indices[i]];
                holdy[i] = this.yData[indices[i]];
                holdxBD[i] = this.xBDdata[indices[i]];
                if(this.arbprec)holdyBD[i] = this.yBDdata[indices[i]]; 
            }
            for(int i=0; i<this.nPoints; i++){
                this.xData[i] = holdx[i];
                this.yData[i] = holdy[i];
                this.xBDdata[i] = holdxBD[i];
                if(this.arbprec)this.yBDdata[i] = holdyBD[i];
            }
        } 
    }
    
    
    // MOVING AVERAGE WINDOW SMOOTHING
    // DATA
    // Smooth with a moving average window of width maWindowWidth
    // No Plotting
    public double[] movingAverage(int maWindowWidth){
        
        this.lastMethod = 1;
        this.lastPlotMethod = 1;
        this.yDataMovAv = new double[this.nPoints];
        this.yDataMovAvBD = new BigDecimal[this.nPoints];
        
        // adjust window width to an odd number of points
        int ww = this.windowHalf(maWindowWidth);
        this.maWindowWidth = 2*ww + 1;
        
        // Apply moving average window
        int lp = 0;
        int up = 0;
        for(int i=0; i<this.nPoints; i++){
            if(i>=ww){
                lp = i - ww;
            }
            else{
                lp = 0;               
            }
            if(i<=this.nPoints-ww-1){                    
                up = i + ww;
            }
            else{
                up = this.nPoints - 1;
            }
            int nw = up - lp + 1;
            if(this.arbprec){
                BigDecimal sumbd = new BigDecimal("0.0");
                for(int j=lp; j<=up; j++){               
                    sumbd = sumbd.add(this.yBDdata[j]);
                }
                String xx = (new Integer(nw)).toString();
                this.yDataMovAvBD[i] = sumbd.divide(new BigDecimal(xx), BigDecimal.ROUND_HALF_UP);
                this.yDataMovAv[i] = this.yDataMovAvBD[i].doubleValue();
            }
            else{
                double sum = 0.0;
                for(int j=lp; j<=up; j++){               
                    sum += this.yData[j];
                }
                this.yDataMovAv[i] = sum/nw;
                String xx = (new Double(this.yDataMovAv[i])).toString();
                this.yDataMovAvBD[i] = new BigDecimal(xx);
            }
        }
        
        // Set up interpolation
        this.csMovAv = new CubicSpline(this.xData, this.yDataMovAv);
                
        this.calcMovAv = true;
        return Conv.copy(this.yDataMovAv);
     
    }
    
    
    // Smooth with a moving average window of width maWindowWidth
    // With plotting
    public double[] movingAveragePlot(int maWindowWidth){
        double[] ret = this.movingAverage(maWindowWidth);
        this.plot();
        return ret;
    }
    
    // Smooth with a moving average window of width maWindowWidth
    // Return values as BigDecimal
    // No Plotting
    public BigDecimal[] movingAverageAsBigDecimal(int maWindowWidth){
        this.movingAverage(maWindowWidth);
        return Conv.copy(this.yDataMovAvBD);
    }
    
    // Smooth with a moving average window of width maWindowWidth
    // Return values as BigDecimal
    // No Plotting
    public BigDecimal[] movingAverageAsBigDecimalPlot(int maWindowWidth){
        this.movingAverageAsBigDecimal(maWindowWidth);
        this.plot();
        return Conv.copy(this.yDataMovAvBD);
    }
    
    // Return window half extension
    private int windowHalf(int width){
    
        int ww = 0;
        if(Fmath.isEven(width)){
            ww = width/2;
        }
        else{
           ww = (width-1)/2;
        }
        return ww;
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
    public double[] savitzkyGolay(int sgWindowWidth){              
       
        this.lastMethod = 2;
        this.lastPlotMethod = 2;
        this.yDataSavGol = new double[this.nPoints];
        
        // adjust window width to an odd number of points
        this.sgWindowWidth = this.windowLength(sgWindowWidth);
        
        // Apply filter 
        this.savitzkyGolayCommon(this.sgWindowWidth);
                       
        // Set up interpolation
        this.csSavGol = new CubicSpline(this.xData, Conv.copy(this.yDataSavGol));
         
                 
        this.calcSavGol = true;
        return Conv.copy(this.yDataSavGol);
    }
    
     // Common method for smoothing with a Savitzky-Golay filter with a window width
    private double[] savitzkyGolayCommon(int width){
        
        // Set filter dimension term
        int ww = (width - 1)/2;

        
        // Calculate filter coefficients
        double[] coeff = (this.savitzkyGolayFilter(ww, ww))[0];
        
        // Padout the data to solve edge effects
        double[] psData = this.padData(this.yData, ww);

        // Apply filter       
        for(int i=ww; i<this.nPoints+ww; i++){ 
            double sum = 0.0;
            int counter = 0;
             for(int k1=i-ww; k1<=i+ww; k1++){
                sum += psData[k1]*coeff[counter++];
             }
             this.yDataSavGol[i-ww] = sum;
        }
        return this.yDataSavGol;
    }
    
    // Pad out data to solve edge effects
    private double[] padData(double[] data, int ww){
        
        // Pad out to solve edge effects
        // Set dimensions
        int nn = data.length;
        
        
        // Create array for padding
        double[] psData = new double[nn+2*ww];
        
        // fill central array with true data
        for(int i=0; i<nn; i++){ 
            psData[i+ww] = data[i];
        }
    
        // pad out leading elements
        for(int i=0; i<ww; i++){ 
            psData[i] = psData[ww];
        }
        
        // pad out trailing elements
        for(int i=nn+ww; i<nn+2*ww; i++){ 
            psData[i] = psData[nn+ww-1];
        }
        
        return psData;
    }
        
    // Savitzky-Golay filter
    // bp = number of backward points
    // fp = number of forward points 
    public double[][] savitzkyGolayFilter(int bp, int fp){
        
        int ww = bp + fp + 1;                   //filter  length 
        double[] coeff = new double[ww];        // Savitzky-Golay coefficients 

        // Assign 'x' values
        int[] values = new int[ww];
        for(int i = 0; i<ww; i++){
            values[i] = i-bp;
        }
       
        double[][] x = new double[ww][this.sgPolyDeg+1];
        for(int i=0; i<ww; i++){
            for(int j = 0; j<this.sgPolyDeg+1; j++){
                x[i][j] = Math.pow(values[i], j);
            }
        }            
              
        Matrix matX = new Matrix(x);
        Matrix matT = matX.transpose();
        Matrix matTX = matT.times(matX);        
        Matrix matI = matTX.inverse();      
        Matrix matC = matI.times(matT);
        this.sgArrayC = matC.getArrayCopy();
        this.sgCoeff = this.sgArrayC[0];
 
        return this.sgArrayC;        
    }

    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // With plotting
    public double[] savitzkyGolayPlot(int sgWindowWidth){
        double[] ret = this.savitzkyGolay(sgWindowWidth);
        this.plot();
        return ret;
    }
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // First derivative also calculated
    // No plotting
    public double[][] savitzkyGolayPlusFirstDeriv(int sgWindowWidth){
        if(1>this.sgPolyDeg)throw new IllegalArgumentException("The derivative order, " + 1 + ", must be less than or equal to the polynomial degree, " + this.sgPolyDeg + ".");
        this.lastMethod = 2;
        this.lastPlotMethod = 3;        
        double[][] ret = this.savitzkyGolay(sgWindowWidth, 1); 
        this.yDataSavGolFirst = ret[1];
        if(!this.nthSet)this.yDataSavGolNth = ret[1];
        this.nthSet = true;
        this.firstDeriv = true;
        return ret;
    }
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // First derivative also calculated
    // With plotting
    public double[][] savitzkyGolayPlusFirstDerivPlot(int sgWindowWidth){
        double[][]ret = this.savitzkyGolayPlusFirstDeriv(sgWindowWidth);
        this.plot();
        this.firstDeriv = true;
        return ret;
    }
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // Second derivative also calculated
    // No plotting
    public double[][] savitzkyGolayPlusSecondDeriv(int sgWindowWidth){
        if(2>this.sgPolyDeg)throw new IllegalArgumentException("The derivative order, " + 2 + ", must be less than or equal to the polynomial degree, " + this.sgPolyDeg + ".");
        this.lastMethod = 2;
        this.lastPlotMethod = 4;
        
        double[][] ret = this.savitzkyGolay(sgWindowWidth, 2); 
        this.yDataSavGolSecond = ret[1];
        if(!this.nthSet)this.yDataSavGolNth = ret[1];
        this.nthSet = true;
        this.secondDeriv = true;
        return ret;
    }
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // Second derivative also calculated
    // With plotting
    public double[][] savitzkyGolayPlusSecondDerivPlot(int sgWindowWidth){
        double[][] ret = this.savitzkyGolayPlusSecondDeriv(sgWindowWidth);
        this.plot();
        this.secondDeriv = true;
        return ret;
    }
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // nth derivative also calculated
    // No plotting
    public double[][] savitzkyGolay(int sgWindowWidth, int n){
        if(n>this.sgPolyDeg)throw new IllegalArgumentException("The  derivative order " +  n + ", must be less than or equal to the polynomial degree, " + this.sgPolyDeg + ".");
 
        this.sgDerivOrderUsed = n;
        double[][] ret = new double[2][this.nPoints];
        
         // adjust window width to an odd number of points
        this.sgWindowWidth = this.windowLength(sgWindowWidth);
        int ww = (this.sgWindowWidth - 1)/2;
        
        if(!this.calcSavGol)this.savitzkyGolay(sgWindowWidth);
        ret[0] = this.yDataSavGol;
        
        // get filter coefficients
        double[] coeff = this.sgArrayC[n]; 
        
        // Padout the data to solve edge effects
        double[] psData = this.padData(this.yData, ww);

        // Apply filter       
        for(int i=ww; i<this.nPoints+ww; i++){ 
            double sum = 0.0;
            int counter = 0;
             for(int k1=i-ww; k1<=i+ww; k1++){            
                sum += psData[k1]*coeff[counter++];
             }
             ret[1][i-ww] = sum;
        }
        
        this.derivSavGol = ret[1];
        if(n==1){
            this.yDataSavGolFirst = ret[1];
            this.firstDeriv = true;
        }
        if(n==2){
            this.yDataSavGolSecond = ret[1];
            this.secondDeriv = true;
        }
        this.nthSet = true;
        this.lastMethod = 2;
        this.lastPlotMethod = 5;
        if(n==1)this.lastPlotMethod = 3;
        if(n==2)this.lastPlotMethod = 4;
        return ret;
    }
    
    
    
    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidth
    // nth derivative also calculated
    // With plotting
    public double[][] savitzkyGolayPlot(int sgWindowWidth, int n){
        double[][] ret = this.savitzkyGolay(sgWindowWidth, n);
        this.plot();
        return ret;
    }
        
    
   
    // Savitzky-Golay filter  - static method
    // bp = number of backward points
    // fp = number of forward points
    // deg = degree of fitting polynomial
    public static double[][] savitzkyGolayFilter(int bp, int fp, int deg){
        CurveSmooth cusm = new CurveSmooth();
        cusm.setSGpolyDegree(deg);
        return cusm.savitzkyGolayFilter(bp, fp);
    }
    
    
    // Savitzky-Golay filter  - static method
    // bp = number of backward points
    // fp = number of forward points
    // deg = degree of fitting polynomial
    // deriv = order of the derivative
    public static double[] savitzkyGolayFilter(int bp, int fp, int deg, int deriv){
        CurveSmooth cusm = new CurveSmooth();
        cusm.setSGpolyDegree(deg);
        return cusm.savitzkyGolayFilter(bp, fp)[deriv];
    }
    
    // Get Savitzky-Golay coefficients used
    public double[] getSGcoefficientsUsed(){
        if(this.sgCoeff==null)throw new IllegalArgumentException("No Savitzky-Golay coefficients have been calculated");
        return this.sgCoeff;
    }
    
    // Get Savitzky-Golay coefficients used
    public double[][] getSGcoefficients(){
        if(this.sgArrayC==null)throw new IllegalArgumentException("No Savitzky-Golay coefficients have been calculated");
        return this.sgArrayC;
    }
    
    // Set the Savitzky-Golay smoothing polynomial order
    // Default value is 4
    public void setSGpolyDegree(int order){
        this.sgPolyDeg = order;
    }
      
    
    // Get the Savitzky-Golay smoothing polynomial order
    // Default value is 4
    public int getSGpolyDegree(){
        return this.sgPolyDeg;
    }

    
  
    // PLOTTING
    
    // Plot smoothed data for last called method
    private void plot(){
            
        double[][] data = null;
        String method = null;
        String[] ex = {"  ", "first", "second", "third", "fourth", "th"};
System.out.println("lm " +this.lastPlotMethod);
        switch(this.lastPlotMethod){         
            case 1: data = new double[8][];
                    data[3] = this.yDataMovAv;
                    method = "Moving average of " + this.maWindowWidth + " points";
                    break;
            case 2: data = new double[8][];
                    data[3] = this.yDataSavGol;
                    method = "Savitzky-Golay filter with a window of "+ this.sgWindowWidth + " points";
                    break;
            case 3: data = new double[10][];
                    data[3] = this.yDataSavGol;
                    data[9] = this.yDataSavGolFirst;
                    method = "Savitzky-Golay filter with a window of "+ this.sgWindowWidth + " points  plus smoothed first derivative";
                    break;
            case 4: data = new double[10][];
                    data[3] = this.yDataSavGol;
                    data[9] = this.yDataSavGolSecond;
                    method = "Savitzky-Golay filter with a window of "+ this.sgWindowWidth + " points  plus smoothed second derivative";
                    break;
            case 5: data = new double[10][];
                    data[3] = this.yDataSavGol;
                    data[9] = this.yDataSavGolNth;
                    if(this.sgDerivOrderUsed<5){
                         method = "Savitzky-Golay filter with a window of "+ this.sgWindowWidth + " points  plus smoothed " + ex[this.sgDerivOrderUsed] + " derivative";
                    }
                    else{
                        method = "Savitzky-Golay filter with a window of "+ this.sgWindowWidth + " points  plus smoothed " + this.sgDerivOrderUsed + "th derivative";
                    }
                    break;
        }
        data[0] = this.xData;
        data[1] = this.yData;
        data[2] = this.xData;
        double[] holdx1 = new double[5];
        double[] holdx2 = new double[5];
        double[] holdy1 = new double[5];
        double[] holdy2 = new double[5];
        holdx1[0] = this.xData[0];
        holdx1[1] = this.xData[this.nPoints/4];
        holdx1[2] = this.xData[this.nPoints/2];
        holdx1[3] = this.xData[3*this.nPoints/4];
        holdx1[4] = this.xData[this.nPoints-1];
        holdy1[0] = this.yData[0];
        holdy1[1] = this.yData[this.nPoints/4];
        holdy1[2] = this.yData[this.nPoints/2];
        holdy1[3] = this.yData[3*this.nPoints/4];
        holdy1[4] = this.yData[this.nPoints-1];
        holdx2[0] = this.xData[0];
        holdx2[1] = this.xData[this.nPoints/4-1];
        holdx2[2] = this.xData[this.nPoints/2-1];
        holdx2[3] = this.xData[3*this.nPoints/4-1];
        holdx2[4] = this.xData[this.nPoints-1];
        holdy2[0] = data[3][0];
        holdy2[1] = data[3][this.nPoints/4-1];
        holdy2[2] = data[3][this.nPoints/2-1];
        holdy2[3] = data[3][3*this.nPoints/4-1];
        holdy2[4] = data[3][this.nPoints-1];
        data[4] = holdx1;
        data[5] = holdy1;
        data[6] = holdx2;
        data[7] = holdy2;
        int[] sph = {0,0,1,2};
        int[] slh = {3,3,0,0};
        int[] sp = sph;
        int[] sl = slh;
        if(this.lastPlotMethod>2){
            data[8] = this.xData;
            sp = new int[5];
            sl = new int[5];
            sp[4] = 0;
            sl[4] = 3;
            for(int i=0;i<4; i++){
                sp[i] = sph[i];
                sl[i] = slh[i];
            }
        }
        data[0] = this.xData;
        data[1] = this.yData;
        data[2] = this.xData;
        
        PlotGraph pg = new PlotGraph(data);
            
        pg.setPoint(sp);
        pg.setLine(sl);
        pg.setGraphTitle("Original (circles) and smoothed (squares) data");
        pg.setGraphTitle2(method);
        pg.setXaxisLegend("x values");
        pg.setYaxisLegend("y values");
        pg.plot();
    }
  
    // SMOOTHED VALUES
    
    // Return last method smoothed y data
    public double[] getSmoothedValues(){
        double[] ret = null;
        switch(this.lastMethod){
            case 0: throw new IllegalArgumentException("No smoothing method has been called");
            case 1: ret = this.yDataMovAv;
                    break;
            case 2: ret = this.yDataSavGol;
                    break;
        }
        return Conv.copy(ret);
    }
    
    // Return moving average smoothed y data
    public double[] getMovingAverageValues(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.yDataMovAv);
    } 
    
    // Return moving average smoothed y data as BigDecimal
    public BigDecimal[] getMovingAverageValuesAsBigDecimal(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.yDataMovAvBD);
    }
    
    // Return Savitzky-Golay smoothed y data
    public double[] getSavitzkyGolaySmoothedValues(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return Conv.copy(this.yDataSavGol);
    }
        
    // Return Savitzky-Golay smoothed first derivative dy/dx data
    public double[] getSavitzkyGolayFirstDerivatives(){
        return getSavitzkyGolaySmoothedFirstDerivValues();
    }
    
    // Return Savitzky-Golay smoothed first derivative dy/dx data
    public double[] getSavitzkyGolaySmoothedFirstDerivValues(){
        if(!this.firstDeriv)throw new IllegalArgumentException("No Savitzky-Golay first derivative smoothing method has been called");
        return Conv.copy(this.yDataSavGolFirst);
    }
    
    // Return Savitzky-Golay smoothed second derivative dy/dx data
    public double[] getSavitzkyGolaySecondDerivatives(){
        return getSavitzkyGolaySmoothedSecondDerivValues();
    }
    
    // Return Savitzky-Golay smoothed second derivative d^2y/dx^2 data
    public double[] getSavitzkyGolaySmoothedSecondDerivValues(){
        if(!this.secondDeriv)throw new IllegalArgumentException("No Savitzky-Golay second derivative smoothing method has been called");
        return Conv.copy(this.yDataSavGolSecond);
    }
    
    // Return last Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolayNthDerivatives(){
        return getSavitzkyGolaySmoothedNthDerivValues();
    }
    
    // Return last Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolaySmoothedNthDerivValues(){
        if(!this.nthSet)throw new IllegalArgumentException("No Savitzky-Golay derivative smoothing method has been called");
        return Conv.copy(this.yDataSavGolNth);
    }
    
    // Return last Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolayDerivatives(){
        if(!this.nthSet)throw new IllegalArgumentException("No Savitzky-Golay derivative smoothing method has been called");
        return Conv.copy(this.yDataSavGolNth);
    }
    
     // Return  Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolayNthDerivatives(int n){
        return getSavitzkyGolaySmoothedNthDerivValues(n);
    }
    
    // Return  Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolayDerivatives(int n){
        return getSavitzkyGolaySmoothedNthDerivValues(n);
    }
    
    // Return Savitzky-Golay smoothed nth derivative d^ny/dx^n data
    public double[] getSavitzkyGolaySmoothedNthDerivValues(int n){
        if(!this.nthSet)throw new IllegalArgumentException("No Savitzky-Golay derivative smoothing method has been called");
        double[] ret = null;
        switch(n){
            case 0: ret = this.yDataSavGol;
                    break;
            case 1: if(!this.firstDeriv)throw new IllegalArgumentException("No Savitzky-Golay first derivative smoothing method has been called");
                    ret = this.yDataSavGolFirst;
                    break;
            case 2: if(!this.secondDeriv)throw new IllegalArgumentException("No Savitzky-Golay second derivative smoothing method has been called");
                    ret = this.yDataSavGolSecond;
                    break;
            default: ret = this.yDataSavGolNth;
                    break;
        }
        return Conv.copy(ret);
    }
    
    // EXTENT OF THE SMOOTHING
    
    // Return the extent of the moving average window smoothing
    public double extentMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        this.extentMovAv = this.extent(this.yData, this.yDataMovAv);
        return this.extentMovAv;
    }
    
    // Return the extent of the Savitzky-Golay smoothing
    public double extentSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        this.extentSavGol = this.extent(this.yData, this.yDataSavGol);
        return this.extentSavGol;
    }
    
    // Calculate extent of smoothing
    private double extent(double[] yOrigl, double[] ySmooth){
        
        double range = Fmath.maximum(yOrigl) - Fmath.minimum(yOrigl);
        
        double sum = 0.0;
        for(int i=0; i<this.nPoints; i++){
            sum += Math.abs(yOrigl[i] - ySmooth[i])/range;
        }
        
        sum /= this.nPoints;
        return sum;
    }
    
    // Return the reduction in the number of extrema of the moving average window smoothing as a fraction
    // number extrema smoothed  / number extrema unsmoothed
    public double extremaReductionMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        int nExtU =  this.getNumberOfExtremaUnsmoothed();
        int nExtS =  this.getNumberOfExtremaMovingAverage();
        return (double)nExtS/(double)nExtU;
    }
    
    // Return the reduction in the number of extrema of the Savitzky Golay smoothing as a fraction
    // number extrema smoothed  / number extrema unsmoothed
    public double extremaReductionSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int nExtU =  this.getNumberOfExtremaUnsmoothed();
        int nExtS =  this.getNumberOfExtremaSavitzkyGolay();
        return (double)nExtS/(double)nExtU;
    }
    
    // INTERPOLATION
    // interpoloate the Savitzky-Golay smoothed data
    public double interpolateSavitzkyGolay(double xi){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return csSavGol.interpolate(xi);
    }
    
    // interpoloate the Savitzky-Golay smoothed data
    public double interpolateMovingAverage(double xi){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return csMovAv.interpolate(xi);
    }
    
    // MINIMA AND MAXIMA
    
    // Get the minima of the original data
    public double[][] getMinimaUnsmoothed(){
        if(!this.minimaCalc[0]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        double[][] ret = new double[2][this.nMin[0]];
        ret[0] = Conv.copy(this.minima[0][0]);
        ret[1] = Conv.copy(this.minima[0][1]);
        return ret;
    }
    
    // Get the minima of the original data as BigDecimal
    public BigDecimal[][] getMinimaUnsmoothedAsBigDecimal(){
        if(!this.minimaCalc[0]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        BigDecimal[][] ret = new BigDecimal[2][this.nMin[0]];
        ret[0] = Conv.copy(this.minimaBD[0][0]);
        ret[1] = Conv.copy(this.minimaBD[0][1]);
        return ret;
    }
    
    // Get the maxima of the original data
    public double[][] getMaximaUnsmoothed(){
        if(!this.maximaCalc[0]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        double[][] ret = new double[2][this.nMax[0]];
        ret[0] = Conv.copy(this.maxima[0][0]);
        ret[1] = Conv.copy(this.maxima[0][1]);
        return ret;
    }
    
    // Get the maxima of the original data as BigDecimal
    public BigDecimal[][] getMaximaUnsmoothedAsBigDecimal(){
        if(!this.maximaCalc[0]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        BigDecimal[][] ret = new BigDecimal[2][this.nMax[0]];
        ret[0] = Conv.copy(this.maximaBD[0][0]);
        ret[1] = Conv.copy(this.maximaBD[0][1]);
        return ret;
    }
    
    // Get the number of minima for the original data
    public int getNumberOfMinimaUnsmoothed(){
        if(!this.minimaCalc[0]){                    
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        return this.nMin[0];
    }
    
    
    // Get the number of maxima for the original data
    public int getNumberOfMaximaUnsmoothed(){
        if(!this.maximaCalc[0]){                    
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        return this.nMax[0];
    }
    
    // Get the number of extremmes for the original data
    public int getNumberOfExtremaUnsmoothed(){
        if(!this.minimaCalc[0]){                    
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yBDdata, 0);
            }
            else{
               this.findMinAndMax(this.xData, this.yData, 0); 
            }
        }
        return this.nMin[0] + this.nMax[0];
    }
        
    
    // Get the minima of the moving average smoothed data
    public double[][] getMinimaMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.minimaCalc[1]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        double[][] ret = new double[2][this.nMin[1]];
        ret[0] = Conv.copy(this.minima[1][0]);
        ret[1] = Conv.copy(this.minima[1][1]);
        return ret;
    }
        
    // Get the minima of the moving average smoothed data as BigDecimal
    public BigDecimal[][] getMinimaMovingAverageAsBigDecimal(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.minimaCalc[1]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        BigDecimal[][] ret = new BigDecimal[2][this.nMin[1]];
        ret[0] = Conv.copy(this.minimaBD[1][0]);
        ret[1] = Conv.copy(this.minimaBD[1][1]);
        return ret;
    }
    
    // Get the maxima of the moving average smoothed data
    public double[][] getMaximaMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.maximaCalc[1])this.findMinAndMax(this.xData, this.yDataMovAv, 1);
        double[][] ret = new double[2][this.nMax[1]];
        ret[0] = Conv.copy(this.maxima[1][0]);
        ret[1] = Conv.copy(this.maxima[1][1]);
        return ret;
    }
    
    // Get the maxima of the moving average smoothed data as BigDecimal
    public BigDecimal[][] getMaximaMovingAverageAsBigDecimal(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.maximaCalc[1]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        BigDecimal[][] ret = new BigDecimal[2][this.nMax[1]];
        ret[0] = Conv.copy(this.maximaBD[1][0]);
        ret[1] = Conv.copy(this.maximaBD[1][1]);
        return ret;
    }
    
    // Get the number of minima for moving average smoothed data
    public int getNumberOfMinimaMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.minimaCalc[1]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        return this.nMin[1];
    }
       
    // Get the number of maxima for moving average smoothed data
    public int getNumberOfMaximaMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.maximaCalc[1]){
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        return this.nMax[1];
    }
    
    // Get the number of extrema for the moving average smoothed data
    public int getNumberOfExtremaMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(!this.minimaCalc[1]){                    
            if(arbprec){
                this.findMinAndMaxBD(this.xBDdata, this.yDataMovAvBD, 1);
            }
            else{
               this.findMinAndMax(this.xData, this.yDataMovAv, 1); 
            }
        }
        return this.nMin[1] + this.nMax[1];
    }
    
    // Get the minima of the Savitzky-Golay smoothed data
    public double[][] getMinimaSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(!this.minimaCalc[1])this.findMinAndMax(this.xData, this.yDataSavGol, 2);
        double[][] ret = new double[2][this.nMin[2]];
        ret[0] = Conv.copy(this.minima[2][0]);
        ret[1] = Conv.copy(this.minima[2][1]);
        return ret;
    }
     
    // Get the maxima of the Savitzky-Golay smoothed data
    public double[][] getMaximaSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(!this.minimaCalc[1])this.findMinAndMax(this.xData, this.yDataSavGol, 2);
        double[][] ret = new double[2][this.nMax[2]];
        ret[0] = Conv.copy(this.maxima[2][0]);
        ret[1] = Conv.copy(this.maxima[2][1]);
        return ret;
    }
    
    // Get the number of minima for Savitzky-Golay smoothed data
    public int getNumberOfMinimaSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(!this.minimaCalc[2])this.findMinAndMax(this.xData, this.yDataSavGol, 2);
        return this.nMin[2];
    }
    
    // Get the number of maxima for Savitzky-Golay smoothed data
    public int getNumberOfMaximaSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(!this.maximaCalc[2])this.findMinAndMax(this.xData, this.yDataSavGol, 2);
        return this.nMax[2];
    }
    
    // Get the number of extrema for Savitzky-Golay smoothed data
    public int getNumberOfExtremaSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(!this.maximaCalc[2])this.findMinAndMax(this.xData, this.yDataSavGol, 2);
        return this.nMin[2] + this.nMax[2];
    }
    
    // Finds minima and maxima for the argument data arrays
    // calculation in double precision
    private void findMinAndMax(double[] xx, double[] yy, int flag){
        
        boolean plateau = false;
        int iPlat = 0;
         
        this.almin.clear();
        this.almax.clear();
        
        int nnMin = 0;
        int nnMax = 0;
        
        // minima
        for(int i=2; i<this.nPoints-1; i++){
            if(plateau){
                if(yy[i]<yy[i-1]){
                    plateau = false;
                    if(yy[i]<yy[i+1]){
                        nnMin++;
                        this.almin.add(xx[i]);
                        this.almin.add(yy[i]); 
                    }
                    else{
                        if(yy[i]==yy[i+1]){
                            plateau = true;
                            iPlat = i;
                        }
                    }
                }
                else{
                    if(yy[i]==yy[i-1]){
                        if(yy[i]<yy[i+1]){
                            plateau = false;
                            nnMin++;
                            double xxp = (xx[i]+xx[iPlat])/2.0;
                            this.almin.add(xxp);
                            this.almin.add(yy[i]);
                        }
                        else{
                           if(yy[i]>yy[i+1]){
                               plateau = false;
                           }
                        }
                    }
                }
            }
            else{
                if(yy[i]<yy[i-1] && yy[i]<yy[i+1]){
                    nnMin++;
                    this.almin.add(xx[i]);
                    this.almin.add(yy[i]);
                }
                else{
                    if(yy[i]<yy[i-1] && yy[i]==yy[i+1]){
                        plateau = true;
                        iPlat = i;
                    }
                }
            }
        }   
    
        this.nMin[flag] = nnMin;
        double[] holdx = new double[nnMin];
        double[] holdy = new double[nnMin];
        BigDecimal[] holdxBD = new BigDecimal[nnMin];
        BigDecimal[] holdyBD = new BigDecimal[nnMin];
        int k = 0;
        for(int i=0; i<nnMin; i++){
           holdx[i] = this.almin.get(k++);
           holdy[i] = this.almin.get(k++);
           String xh = (new Double(holdx[i])).toString();
           holdxBD[i] = new BigDecimal(xh);
           String yh = (new Double(holdy[i])).toString();
           holdyBD[i] = new BigDecimal(yh);
        }
        this.minima[flag][0] = holdx;
        this.minima[flag][1] = holdy; 
        this.minimaBD[flag][0] = holdxBD;
        this.minimaBD[flag][1] = holdyBD;
        this.minimaCalc[flag] = true;
       
        // maxima
        plateau = false;
        iPlat = 0;
        for(int i=2; i<this.nPoints-1; i++){
            if(plateau){
                if(yy[i]>yy[i-1]){
                    plateau = false;
                    if(yy[i]>yy[i+1]){
                        nnMax++;
                        this.almax.add(xx[i]);
                        this.almax.add(yy[i]); 
                    }
                    else{
                        if(yy[i]==yy[i+1]){
                            plateau = true;
                            iPlat = i;
                        }
                    }
                }
                else{
                    if(yy[i]==yy[i-1]){
                        if(yy[i]>yy[i+1]){
                            plateau = false;
                            nnMax++;
                            double xxp = (xx[i]+xx[iPlat])/2.0;
                            this.almax.add(xxp);
                            this.almax.add(yy[i]);
                        }
                        else{
                           if(yy[i]<yy[i+1]){
                               plateau = false;
                           }
                        }
                    }
                }
            }
            else{
                if(yy[i]>yy[i-1] && yy[i]>yy[i+1]){
                    nnMax++;
                    this.almax.add(xx[i]);
                    this.almax.add(yy[i]);
                }
                else{
                    if(yy[i]>yy[i-1] && yy[i]==yy[i+1]){
                        plateau = true;
                        iPlat = i;
                    }
                }
            }
        }   
    
        this.nMax[flag] = nnMax;
        holdx = new double[nnMax];
        holdy = new double[nnMax];
        holdxBD = new BigDecimal[nnMax];
        holdyBD = new BigDecimal[nnMax];
        k = 0;
        for(int i=0; i<nnMax; i++){
           holdx[i] = this.almax.get(k++);
           holdy[i] = this.almax.get(k++);
           String xh = (new Double(holdx[i])).toString();
           holdxBD[i] = new BigDecimal(xh);
           String yh = (new Double(holdy[i])).toString();
           holdyBD[i] = new BigDecimal(yh);
        }
        this.maxima[flag][0] = holdx;
        this.maxima[flag][1] = holdy; 
        this.maximaBD[flag][0] = holdxBD;
        this.maximaBD[flag][1] = holdyBD;
        this.maximaCalc[flag] = true;
        
    }
    
    // Finds minima and maxima for the argument data arrays
    // calculation in arbitrary precision
    private void findMinAndMaxBD(BigDecimal[] xx, BigDecimal[] yy, int flag){
        
        boolean plateau = false;
        int iPlat = 0;
         
        this.almin.clear();
        this.almax.clear();
        
        int nnMin = 0;
        int nnMax = 0;
        
        // minima
        for(int i=2; i<this.nPoints-1; i++){
            if(plateau){
                if(yy[i].compareTo(yy[i-1])==-1){
                    plateau = false;
                    if(yy[i].compareTo(yy[i+1])==-1){
                        nnMin++;
                        this.alminBD.add(xx[i]);
                        this.alminBD.add(yy[i]); 
                    }
                    else{
                        if(yy[i]==yy[i+1]){
                            plateau = true;
                            iPlat = i;
                        }
                    }
                }
                else{
                    if(yy[i].compareTo(yy[i-1])==0){
                        if(yy[i].compareTo(yy[i+1])==-1){
                            plateau = false;
                            nnMin++;
                            BigDecimal xxp = (xx[i].add(xx[iPlat])).divide(new BigDecimal("2.0"), BigDecimal.ROUND_HALF_UP);
                            this.alminBD.add(xxp);
                            this.alminBD.add(yy[i]);
                        }
                        else{
                           if(yy[i].compareTo(yy[i+1])==1){
                               plateau = false;
                           }
                        }
                    }
                }
            }
            else{
                if(yy[i].compareTo(yy[i-1])==-1 && yy[i].compareTo(yy[i+1])==-1){
                    nnMin++;
                    this.alminBD.add(xx[i]);
                    this.alminBD.add(yy[i]);
                }
                else{
                    if(yy[i].compareTo(yy[i-1])==-1 && yy[i].compareTo(yy[i+1])==0){
                        plateau = true;
                        iPlat = i;
                    }
                }
            }
        }   
    
        this.nMin[flag] = nnMin;
        double[] holdx = new double[nnMin];
        double[] holdy = new double[nnMin];
        BigDecimal[] holdxBD = new BigDecimal[nnMin];
        BigDecimal[] holdyBD = new BigDecimal[nnMin];
        int k = 0;
        for(int i=0; i<nnMin; i++){
           holdxBD[i] = this.alminBD.get(k++);
           holdyBD[i] = this.alminBD.get(k++);
           holdx[i] = holdxBD[i].doubleValue();
           holdy[i] = holdyBD[i].doubleValue();
        }
        this.minima[flag][0] = holdx;
        this.minima[flag][1] = holdy; 
        this.minimaBD[flag][0] = holdxBD;
        this.minimaBD[flag][1] = holdyBD;      
        this.minimaCalc[flag] = true;
       
        // maxima
        plateau = false;
        iPlat = 0;
        for(int i=2; i<this.nPoints-1; i++){
            if(plateau){
                if(yy[i].compareTo(yy[i-1])==1){
                    plateau = false;
                    if(yy[i].compareTo(yy[i+1])==1){
                        nnMax++;
                        this.almaxBD.add(xx[i]);
                        this.almaxBD.add(yy[i]); 
                    }
                    else{
                        if(yy[i].compareTo(yy[i+1])==0){
                            plateau = true;
                            iPlat = i;
                        }
                    }
                }
                else{
                    if(yy[i]==yy[i-1]){
                        if(yy[i].compareTo(yy[i+1])==1){
                            plateau = false;
                            nnMax++;
                            BigDecimal xxp = ((xx[i].add(xx[iPlat]))).divide(new BigDecimal("2.0"), BigDecimal.ROUND_HALF_UP);
                            this.almaxBD.add(xxp);
                            this.almaxBD.add(yy[i]);
                        }
                        else{
                           if(yy[i].compareTo(yy[i+1])==-1){
                               plateau = false;
                           }
                        }
                    }
                }
            }
            else{
                if(yy[i].compareTo(yy[i-1])==1 && yy[i].compareTo(yy[i+1])==1){
                    nnMax++;
                    this.almaxBD.add(xx[i]);
                    this.almaxBD.add(yy[i]);
                }
                else{
                    if(yy[i].compareTo(yy[i-1])==1 && yy[i].compareTo(yy[i+1])==0){
                        plateau = true;
                        iPlat = i;
                    }
                }
            }
        }   
      
        this.nMax[flag] = nnMax;
        holdx = new double[nnMax];
        holdy = new double[nnMax];
        holdxBD = new BigDecimal[nnMax];
        holdyBD = new BigDecimal[nnMax];
        k = 0;
        for(int i=0; i<nnMax; i++){
           holdxBD[i] = this.alminBD.get(k++);
           holdyBD[i] = this.alminBD.get(k++);
           holdx[i] = holdxBD[i].doubleValue();
           holdy[i] = holdyBD[i].doubleValue();
        }
        this.maxima[flag][0] = holdx;
        this.maxima[flag][1] = holdy; 
        this.maximaBD[flag][0] = holdxBD;
        this.maximaBD[flag][1] = holdyBD;      
        this.maximaCalc[flag] = true;
    }    
}
