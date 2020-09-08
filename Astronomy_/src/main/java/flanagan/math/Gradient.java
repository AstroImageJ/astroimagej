/*  Class Gradient
*
*   Calculates gradients within 1D and 2D arrays 
*   using either cubic/bicubic splines or numerical difference derivatives
*    
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:	August 2012
*   UPDATES:    4 September 2012, 12 September 2012
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web page:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Gradient.html
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*
*   Copyright (c) 2012  Michael Thomas Flanagan
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

package flanagan.math;

import flanagan.interpolation.CubicSpline;
import flanagan.interpolation.BiCubicSplineFirstDerivative;

import java.util.ArrayList;

public class Gradient{
     
    private double[] xArray = null;                                             // entered 1 D x array as doubles
    private double[] yArray = null;                                             // entered 1 D y array as doubles
    
    private int[] xSampling = null;                                             // indices of sampled x points
    private int[] ySampling = null;                                             // indices of sampled y points
    
    private double[] fArray1d = null;                                           // entered 1 D f array as doubles
    private double[][] fArray2d = null;                                         // entered 2 D f array as doubles 
      
    private int xSamplingStart = 0;                                             // sampling start x index
    private int ySamplingStart = 0;                                             // sampling start y index 
    private int xSamplingEnd = 0;                                               // sampling end x index
    private int ySamplingEnd = 0;                                               // sampling end y index 
    private int xSamplingPeriod = 1;                                            // sampling period in the x direction
    private int ySamplingPeriod = 1;                                            // sampling period in the y direction
  
    private boolean sampled = false;                                            // = true when sampling interval entered
    
    private ArrayList<Object> numDiffArray = new ArrayList<Object>();           // Array of calculated numerical derivatives
    
    private double[] ndGrad1 = null;                                            // 1 D array numerical derivatives
    private double[][] ndGrad2_X = null;                                        // 2 D array numerical derivatives in the X direction
    private double[][] ndGrad2_Y = null;                                        // 2 D array numerical derivatives in the Y direction
                      
    private boolean numArrayDone = false;                                       // = true when numerical derivatives calculated
    
    private int nDim = 0;                                                       // dimension of the entered arrays
    private int xLength = 0;                                                    // dimension length of the entered x array
    private int yLength = 0;                                                    // dimension length of the entered y array
    private int xLengthS = 0;                                                   // dimension length of the sampled x array
    private int yLengthS = 0;                                                   // dimension length of the sampled y array
     
    private ArrayList<Object> splineDerivArray = new ArrayList<Object>();       // Array of cubic splinr first derivatives
     private ArrayList<Object> splineSecondDerivArray = new ArrayList<Object>();// Array of cubic splinr second derivatives
    
    private double[] csGrad1F = null;                                           // Full 1 D array cubic spline first derivatives
    private double[] csGrad1 = null;                                            // Sampled 1 D array cubic spline first derivatives 
    private double[][] bcsGrad2_XF = null;                                      // Full 2 D array bicubic spline first derivatives in the X direction
    private double[][] bcsGrad2_X = null;                                       // Sampled2 D array bicubic spline first derivatives in the X direction
    private double[][] bcsGrad2_YF = null;                                      // Full 2 D array bicubic spline first derivatives in the Y direction
    private double[][] bcsGrad2_Y = null;                                       // Sampled 2 D array bicubic spline first derivatives in the Y direction
    
    private double[] cs2Grad1 = null;                                           // 1 D array cubic spline second derivatives
    private double[][] bcs2Grad2_X2 = null;                                     // 2 D array bicubic spline second derivatives in the X direction
    private double[][] bcs2Grad2_Y2 = null;                                     // 2 D array bicubic spline second derivatives in the Y direction
    private double[][] bcs2Grad2_XY = null;                                     // 2 D array bicubic spline second derivatives df2/dxdy
   
    private CubicSpline cs = null;                                              // instance of CubicSpline for calculating first derivatives
    private BiCubicSplineFirstDerivative bcs = null;                            // instance of BiCubicSplineFirstDerivative for calculating first derivatives
    private boolean splineDone = false;                                         // = true when cubic spline or bicubic spline set up for calculating first derivatives
    
    private CubicSpline cs2 = null;                                             // instance of CubicSpline for calculating second derivatives
    private BiCubicSplineFirstDerivative bcs2x = null;                          // instance of BiCubicSplineFirstDerivative for calculating df2/dx2 derivatives
    private BiCubicSplineFirstDerivative bcs2y = null;                          // instance of BiCubicSplineFirstDerivative for calculating df2/dy2 derivatives
    private boolean spline2Done = false;                                        // = true when cubic spline or bicubic spline set up for calculating second derivatives
    
    
    // CONTRUCTOR
    public Gradient(double[] xx, double[] ff){
        this.xLength = ff.length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        this.nDim = 1;
        this.xLengthS = this.xLength;
        this.xArray = Conv.copy(xx);
        this.fArray1d = Conv.copy(ff);   
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(float[] xx, float[] ff){
        this.xLength = ff.length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        this.nDim = 1;
        this.xLengthS = this.xLength;
        float[] xHold = Conv.copy(xx);
        float[] yHold = Conv.copy(ff);
        this.xArray = new double[this.xLength];
        this.fArray1d = new double[this.xLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_float_to_double(xHold[i]);
            this.fArray1d[i] = Conv.convert_float_to_double(yHold[i]);
        }
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(long[] xx, long[] ff){
        this.xLength = ff.length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        this.nDim = 1;
        this.xLengthS = this.xLength;
        long[] xHold = Conv.copy(xx);
        long[] yHold = Conv.copy(ff);
        this.xArray = new double[this.xLength];
        this.fArray1d = new double[this.xLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_long_to_double(xHold[i]);
            this.fArray1d[i] = Conv.convert_long_to_double(yHold[i]);
        }
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.sampled = false;
    }
     
    public Gradient(int[] xx, int[] ff){
        this.xLength = ff.length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        this.nDim = 1;
        this.xLengthS = this.xLength;
        int[] xHold = Conv.copy(xx);
        int[] yHold = Conv.copy(ff);
        this.xArray = new double[this.xLength];
        this.fArray1d = new double[this.xLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_int_to_double(xHold[i]);
            this.fArray1d[i] = Conv.convert_int_to_double(yHold[i]);
        }
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(double[] xx, double[] yy, double[][] ff){
        this.xLength = ff.length;
        this.yLength = ff[0].length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        if(yy.length!=this.yLength)throw new IllegalArgumentException("y length, " + yy.length + ", does not match corresponding f length, " + yLength);
        this.nDim = 2; 
        this.xLengthS = this.xLength;
        this.yLengthS = this.yLength;
        this.xArray = Conv.copy(xx);
        this.yArray = Conv.copy(yy);
        this.fArray2d = Conv.copy(ff);
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.ySampling = new int[this.yLengthS];
        for(int i=0; i<this.yLengthS; i++)this.ySampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.ySamplingStart = 0;
        this.ySamplingEnd = this.yLengthS-1;
        this.ySamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(float[] xx, float[] yy, float[][] ff){
        this.xLength = ff.length;
        this.yLength = ff[0].length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        if(yy.length!=this.yLength)throw new IllegalArgumentException("y length, " + yy.length + ", does not match corresponding f length, " + yLength);
        this.nDim = 2; 
        this.xLengthS = this.xLength;
        this.yLengthS = this.yLength;
        float[] xHold = Conv.copy(xx);
        float[] yHold = Conv.copy(yy);
        float[][] fHold = Conv.copy(ff);
        this.xArray = new double[this.xLength];
        this.yArray = new double[this.yLength];
        this.fArray2d = new double[this.xLength][this.yLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_float_to_double(xHold[i]);
            for(int j=0; j<this.yLength; j++){
                if(i==0)this.yArray[j] = Conv.convert_float_to_double(yHold[j]);
                this.fArray2d[i][j] = Conv.convert_float_to_double(fHold[i][j]);
            }
        } 
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.ySampling = new int[this.yLengthS];
        for(int i=0; i<this.yLengthS; i++)this.ySampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.ySamplingStart = 0;
        this.ySamplingEnd = this.yLengthS-1;
        this.ySamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(long[] xx, long[] yy, long[][] ff){
        this.xLength = ff.length;
        this.yLength = ff[0].length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        if(yy.length!=this.yLength)throw new IllegalArgumentException("y length, " + yy.length + ", does not match corresponding f length, " + yLength);
        this.nDim = 2;
        this.xLengthS = this.xLength;
        this.yLengthS = this.yLength;
        long[] xHold = Conv.copy(xx);
        long[] yHold = Conv.copy(yy);
        long[][] fHold = Conv.copy(ff);        
        this.xArray = new double[this.xLength];
        this.yArray = new double[this.yLength];    
        this.fArray2d = new double[this.xLength][this.yLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_long_to_double(xHold[i]);
            for(int j=0; j<this.yLength; j++){
                if(i==0)this.yArray[j] = Conv.convert_long_to_double(yHold[j]);
                this.fArray2d[i][j] = Conv.convert_long_to_double(fHold[i][j]);
            }
        } 
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.ySampling = new int[this.yLengthS];
        for(int i=0; i<this.yLengthS; i++)this.ySampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.ySamplingStart = 0;
        this.ySamplingEnd = this.yLengthS-1;
        this.ySamplingPeriod = 1;
        this.sampled = false;
    }
    
    public Gradient(int[] xx, int[] yy, int[][] ff){
        this.xLength = ff.length;
        this.yLength = ff[0].length;
        if(xx.length!=this.xLength)throw new IllegalArgumentException("x length, " + xx.length + ", does not match corresponding f length, " + xLength);
        if(yy.length!=this.yLength)throw new IllegalArgumentException("y length, " + yy.length + ", does not match corresponding f length, " + yLength);
        this.nDim = 2;
        this.xLengthS = this.xLength;
        this.yLengthS = this.yLength;
        int[] xHold = Conv.copy(xx);
        int[] yHold = Conv.copy(yy);
        int[][] fHold = Conv.copy(ff);        
        this.xArray = new double[this.xLength];
        this.yArray = new double[this.yLength];    
        this.fArray2d = new double[this.xLength][this.yLength];
        for(int i=0; i<this.xLength; i++){
            this.xArray[i] = Conv.convert_int_to_double(xHold[i]);
            for(int j=0; j<this.yLength; j++){
                if(i==0)this.yArray[j] = Conv.convert_int_to_double(yHold[j]);
                this.fArray2d[i][j] = Conv.convert_int_to_double(fHold[i][j]);
            }
        }   
        this.xSampling = new int[this.xLengthS];
        for(int i=0; i<this.xLengthS; i++)this.xSampling[i] = i;
        this.ySampling = new int[this.yLengthS];
        for(int i=0; i<this.yLengthS; i++)this.ySampling[i] = i;
        this.xSamplingStart = 0;
        this.xSamplingEnd = this.xLengthS-1;
        this.xSamplingPeriod = 1;
        this.ySamplingStart = 0;
        this.ySamplingEnd = this.yLengthS-1;
        this.ySamplingPeriod = 1;
        this.sampled = false;
    }
    
    // SAMPLING
    
    // Common sampling period over all entered data
    public void sampling(int samlingPeriod){
        this.xSamplingStart = 0;
        this.ySamplingStart = 0;
        this.xSamplingEnd = this.xLength-1;
        this.ySamplingEnd = this.yLength-1;
        this.xSamplingPeriod = samlingPeriod;
        this.ySamplingPeriod = samlingPeriod;      
        this.samplingSelection();
    }
    
    // diferent x-direction and y-direction sampling periods over all entered data
    public void sampling(int xSamlingPeriod, int ySamlingPeriod){
        this.xSamplingStart = 0;
        this.ySamplingStart = 0;
        this.xSamplingEnd = this.xLength-1;
        this.ySamplingEnd = this.yLength-1;
        this.xSamplingPeriod = xSamlingPeriod;
        this.ySamplingPeriod = ySamlingPeriod;      
        this.samplingSelection();
    }
    
    // common sampling periods over selected data
    public void sampling(int samlingPeriod, int start, int end){
        this.xSamplingStart = start;
        this.ySamplingStart = start;
        this.xSamplingEnd = end;
        this.ySamplingEnd = end;
        this.xSamplingPeriod = samlingPeriod;
        this.ySamplingPeriod = samlingPeriod;      
        this.samplingSelection();
    }
   
    // diferent x-direction and y-direction sampling periods over selected data
    public void sampling(int xSamlingPeriod, int xStart, int xEnd, int ySamlingPeriod, int yStart, int yEnd){
        this.xSamplingStart = xStart;
        this.ySamplingStart = yStart;
        this.xSamplingEnd = xEnd;
        this.ySamplingEnd = yEnd;
        this.xSamplingPeriod = xSamlingPeriod;
        this.ySamplingPeriod = ySamlingPeriod;   
        this.samplingSelection();
    }
    
    // Core private sampling method setting sampling parameters
    private void samplingSelection(){
        switch(this.nDim){  
            case 2: this.yLengthS = (this.ySamplingEnd - this.ySamplingStart + 1)/this.ySamplingPeriod;
                    int yrem = this.yLength%this.ySamplingPeriod;
                    if(yrem!=0)this.yLengthS++;
                    this.ySampling = new int[this.yLengthS];
                    this.ySampling[0] = this.ySamplingStart;
                    for(int i=1; i<this.yLengthS; i++){
                       this.ySampling[i] =  this.ySampling[i-1] + this.ySamplingPeriod;
                    }

            case 1: this.xLengthS = (this.xSamplingEnd - this.xSamplingStart + 1)/this.xSamplingPeriod;
                    int xrem = this.xLength%this.xSamplingPeriod;
                    if(xrem!=0)this.xLengthS++;
                    this.xSampling = new int[this.xLengthS];
                    this.xSampling[0] = this.xSamplingStart;
                    for(int i=1; i<this.xLengthS; i++){
                       this.xSampling[i] =  this.xSampling[i-1] + this.xSamplingPeriod;
                    }
        }
        this.numArrayDone = false;
    }  
    
    // CUBIC SPLINE FIRST DERIVATIVES
      
    // Cubic spline derivatives of an entered one dimensional array
    public double[] splineDeriv_1D_array(){
        if(this.nDim!=1)throw new IllegalArgumentException("method splineDeriv_1D_array is only applicable if the entered data is a ONE dimensional array");        
        if(!this.splineDone)this.splineDerivativesArray();
        return this.csGrad1;
    }
    
    // Cubic spline derivatives of an entered two dimensional array in X direction
    public double[][] splineDeriv_2D_x_direction(){
        if(this.nDim!=2)throw new IllegalArgumentException("method splineDeriv_2D_x_direction is only applicable if the entered data is a TWO dimensional array");        
        if(!this.splineDone)this.splineDerivativesArray();
        return this.bcsGrad2_X;
    }
    
    // Cubic spline derivatives of an entered two dimensional array in Y direction
    public double[][] splineDeriv_2D_y_direction(){
        if(this.nDim!=2)throw new IllegalArgumentException("method splineDeriv_2D_y_direction is only applicable if the entered data is a TWO dimensional array");        
        if(!this.splineDone)this.splineDerivativesArray();
        return this.bcsGrad2_Y;
    }
    
    // Spline first derivatives at all the entered points in x and y direction
    public ArrayList<Object> splineDerivativesArray(){
        this.splineDerivArray = new ArrayList<Object>();           
        switch(this.nDim){
            case 1: this.cs = new CubicSpline(this.xArray, this.fArray1d);
                    this.csGrad1F = new double[this.xLength];
                    this.csGrad1 = new double[this.xLengthS];
                    for(int i=0; i<this.xLength; i++)this.csGrad1F[i] = (this.cs.interpolate_for_y_and_dydx(this.xArray[i]))[1];          
                    for(int i=0; i<this.xLengthS; i++)this.csGrad1[i] = this.csGrad1F[this.xSampling[i]];
                    this.splineDerivArray.add(this.csGrad1);           
                    break;
            case 2: this.bcs = new BiCubicSplineFirstDerivative(this.xArray, this.yArray, this.fArray2d);
                    this.bcsGrad2_XF = new double[this.xLength][this.yLength];
                    this.bcsGrad2_YF = new double[this.xLength][this.yLength];
                    this.bcsGrad2_X = new double[this.xLengthS][this.yLengthS];
                    this.bcsGrad2_Y = new double[this.xLengthS][this.yLengthS];
                    for(int i=0; i<this.xLength; i++){
                        for(int j=0; j<this.yLength; j++){
                            double[] holdfdxy = this.bcs.interpolate(this.xArray[i], this.yArray[j]);
                            this.bcsGrad2_XF[i][j] = holdfdxy[1];
                            this.bcsGrad2_YF[i][j] = holdfdxy[2];
                        }
                    }
                    for(int i=0; i<this.xLengthS; i++){
                        for(int j=0; j<this.yLengthS; j++){
                            this.bcsGrad2_X[i][j] = this.bcsGrad2_XF[this.xSampling[i]][this.ySampling[j]];
                            this.bcsGrad2_Y[i][j] = this.bcsGrad2_YF[this.xSampling[i]][this.ySampling[j]];
                        }
                    }
                    this.splineDerivArray.add(this.bcsGrad2_X);
                    this.splineDerivArray.add(this.bcsGrad2_Y);
                    break;
        }
        this.splineDone = true;
        return this.splineDerivArray;
    }
    
    // Interpolate spline derivative at point xx in an entered 1D array 
    public double splineDerivAtPoint(double xx){
        if(this.nDim!=1)throw new IllegalArgumentException("Only one coordinate entered for a multiple dimensioned array");
        if(xx<this.xArray[0] || xx>this.xArray[this.xLength-1])throw new IllegalArgumentException("Entered xx value, " + xx + ", is outside the x value range, " + this.xArray[0] + " to " + this.xArray[this.xLength-1]);
        if(!this.splineDone)this.cs = new CubicSpline(this.xArray, this.fArray1d);
        return (this.cs.interpolate_for_y_and_dydx(xx))[1];
    }
    
    // Interpolate spline derivative at point xx, yy in an entered 2D array 
    public double[] splineDerivAtPoint(double xx, double yy){
        if(this.nDim==1)throw new IllegalArgumentException("Two coordinates entered for a one dimension array");
        if(xx<this.xArray[0] || xx>this.xArray[this.xLength-1])throw new IllegalArgumentException("Entered xx value, " + xx + ", is outside the x value range, " + this.xArray[0] + " to " + this.xArray[this.xLength-1]);
        if(yy<this.yArray[0] || yy>this.yArray[this.yLength-1])throw new IllegalArgumentException("Entered yy value, " + yy + ", is outside the y value range, " + this.yArray[0] + " to " + this.yArray[this.yLength-1]);
        if(!this.splineDone)this.bcs = new BiCubicSplineFirstDerivative(this.xArray, this.yArray, this.fArray2d);
        double[] ret0 = new double[2];
        double[] ret1 = this.bcs.interpolate(xx, yy);
        ret0[0] = ret1[1];
        ret0[1] = ret1[2];
        return ret0;
    }
    
    
    // NUMERICAL FIRST DERIVATIVES
      
    // Numerical derivatives of an entered one dimensional array
    public double[] numDeriv_1D_array(){
        if(this.nDim!=1)throw new IllegalArgumentException("method numDeriv_1D_array is only applicable if the entered data is a ONE dimensional array");        
        if(!this.numArrayDone)this.numericalDerivativesArray();
        return this.ndGrad1;
    }
    
    // Numerical derivatives of an entered two dimensional array in X direction
    public double[][] numDeriv_2D_x_direction(){
        if(this.nDim!=2)throw new IllegalArgumentException("method numDeriv_2D_x_direction is only applicable if the entered data is a TWO dimensional array");        
        if(!this.numArrayDone)this.numericalDerivativesArray();
        return this.ndGrad2_X;
    }
    
    // Numerical derivatives of an entered two dimensional array in Y direction
    public double[][] numDeriv_2D_y_direction(){
        if(this.nDim!=2)throw new IllegalArgumentException("method numDeriv_2D_y_direction is only applicable if the entered data is a TWO dimensional array");        
        if(!this.numArrayDone)this.numericalDerivativesArray();
        return this.ndGrad2_Y;
    }
    
    
    // Numerical derivatives at all the entered points in x and y direction
    public ArrayList<Object> numericalDerivativesArray(){
        this.numDiffArray = new ArrayList<Object>();           
        switch(this.nDim){
            case 1: this.ndGrad1 = new double[this.xLengthS];
                    for(int i=0; i<this.xLengthS; i++){
                        if(this.xSampling[i]==0){
                            this.ndGrad1[i] = (this.fArray1d[this.xSampling[i]+1] - this.fArray1d[this.xSampling[i]])/(this.xArray[this.xSampling[i]+1] - this.xArray[this.xSampling[i]]);
                        }
                        else{
                            if(this.xSampling[i]==(this.xLength-1)){
                                this.ndGrad1[i] = (this.fArray1d[this.xSampling[i]] - this.fArray1d[this.xSampling[i]-1])/(this.xArray[this.xSampling[i]] - this.xArray[this.xSampling[i]-1]);
                            }
                            else{
                                this.ndGrad1[i] = ((this.fArray1d[this.xSampling[i]] - this.fArray1d[this.xSampling[i]-1])/(this.xArray[this.xSampling[i]] - this.xArray[this.xSampling[i]-1]) + (this.fArray1d[this.xSampling[i]+1] - this.fArray1d[this.xSampling[i]])/(this.xArray[this.xSampling[i]+1] - this.xArray[this.xSampling[i]]))/2.0;     
                            }
                        }
                    }
                    this.numDiffArray.add(this.ndGrad1);
                    break;
            case 2: this.ndGrad2_Y = new double[this.xLengthS][this.yLengthS];
                    this.ndGrad2_X = new double[this.xLengthS][this.yLengthS];
                    for(int i=0; i<this.xLengthS; i++){
                        for(int j=0; j<this.yLengthS; j++){
                            if(this.ySampling[j]==0){
                                this.ndGrad2_Y[i][j] = (this.fArray2d[this.xSampling[i]][this.ySampling[j]+1] - this.fArray2d[this.xSampling[i]][this.ySampling[j]])/(this.yArray[this.ySampling[j]+1] - this.yArray[this.ySampling[j]]);
                            }
                            else{
                               if(this.ySampling[j]==(this.yLength-1)){ 
                                   this.ndGrad2_Y[i][j] = (this.fArray2d[this.xSampling[i]][this.ySampling[j]] - this.fArray2d[this.xSampling[i]][this.ySampling[j]-1])/(this.yArray[this.ySampling[j]] - this.yArray[this.ySampling[j]-1]);
                                }
                               else{
                                   this.ndGrad2_Y[i][j] = ((this.fArray2d[this.xSampling[i]][this.ySampling[j]] - this.fArray2d[this.xSampling[i]][this.ySampling[j]-1])/(this.yArray[this.ySampling[j]] - this.yArray[this.ySampling[j]-1]) + (this.fArray2d[this.xSampling[i]][this.ySampling[j]+1] - this.fArray2d[this.xSampling[i]][this.ySampling[j]])/(this.yArray[this.ySampling[j]+1] - this.yArray[this.ySampling[j]]))/2.0;                              
                               }
                            }
                        }
                    }
                    this.numDiffArray.add(this.ndGrad2_Y);
                    for(int i=0; i<this.yLengthS; i++){
                        for(int j=0; j<this.xLengthS; j++){
                            if(this.xSampling[j]==0){
                                this.ndGrad2_X[j][i] = (this.fArray2d[this.xSampling[j]+1][this.ySampling[i]] - this.fArray2d[this.xSampling[j]][this.ySampling[i]])/(this.xArray[this.xSampling[j]+1] - this.xArray[this.xSampling[j]]);
                            }
                            else{
                                if(this.xSampling[j]==(this.xLength-1)){
                                    this.ndGrad2_X[j][i] = (this.fArray2d[this.xSampling[j]][this.ySampling[i]] - this.fArray2d[this.xSampling[j]-1][this.ySampling[i]])/(this.xArray[this.xSampling[j]] - this.xArray[this.xSampling[j]-1]); 
                                }
                                else{
                                    this.ndGrad2_X[j][i] = ((this.fArray2d[this.xSampling[j]][this.ySampling[i]] - this.fArray2d[this.xSampling[j]-1][this.ySampling[i]])/(this.xArray[this.xSampling[j]] - this.xArray[this.xSampling[j]-1]) + this.ndGrad2_X[j][i] + (this.fArray2d[this.xSampling[j]+1][this.ySampling[i]] - this.fArray2d[this.xSampling[j]][this.ySampling[i]])/(this.xArray[this.xSampling[j]+1] - this.xArray[this.xSampling[j]]))/2.0; 
                                }
                            }
                        }
                    }
                    this.numDiffArray.add(this.ndGrad2_X);
                    break;
               default: throw new IllegalArgumentException("Arrays of "+this.nDim+" dimensions are not handles by this class");    

        }
        this.numArrayDone = true;
        return this.numDiffArray;
    }

    
    // INTERPOLATED NUMERICAL DERIVATIVES
    
    // Interpolate numerical derivative gradient at point xx in an entered 1D array 
    public double numDerivAtPoint(double xx){
        if(this.nDim!=1)throw new IllegalArgumentException("Only one coordinate entered for a multiple dimensioned array");
        if(xx<this.xArray[0] || xx>this.xArray[this.xLength-1])throw new IllegalArgumentException("Entered xx value, " + xx + ", is outside the x value range, " + this.xArray[0] + " to " + this.xArray[this.xLength-1]);
  
        double grad = 0;
        
        boolean testx0 = true;
        int i = 0;
        while(testx0){
            if(xx<=this.xArray[i]){
                testx0 = false;
                if(i==0){
                    grad = (this.fArray1d[i+1] - this.fArray1d[i])/(this.xArray[i+1] - this.xArray[i]);
                }
                else{
                    if(xx==this.xArray[this.xLength-1]){
                        grad = (this.fArray1d[i] - this.fArray1d[i-1])/(this.xArray[i] - this.xArray[i-1]);
                    } 
                    else{
                        if(xx==this.xArray[i]){
                            grad = ((this.fArray1d[i] - this.fArray1d[i-1])/(this.xArray[i] - this.xArray[i-1]) + (this.fArray1d[i+1] - this.fArray1d[i])/(this.xArray[i+1] - this.xArray[i]))/2.0;
                        }
                        else{
                            grad = (this.fArray1d[i] - this.fArray1d[i-1])/(this.xArray[i] - this.xArray[i-1]);
                        }
                    }
                }
            }
            else{
                i++;
            }
        }
        return grad;
    }
     
    // Interpolate x-direction numerical derivative gradient and y-direction numerical derivative gradient at point xx, yy in an entered 2D array 
    public double[] numDerivAtPoint(double xx, double yy){
        if(this.nDim==1)throw new IllegalArgumentException("Two coordinates entered for a one dimension array");
        if(xx<this.xArray[0] || xx>this.xArray[this.xLength-1])throw new IllegalArgumentException("Entered xx value, " + xx + ", is outside the x value range, " + this.xArray[0] + " to " + this.xArray[this.xLength-1]);
        if(yy<this.yArray[0] || yy>this.yArray[this.yLength-1])throw new IllegalArgumentException("Entered yy value, " + yy + ", is outside the y value range, " + this.yArray[0] + " to " + this.yArray[this.yLength-1]);
         double[] grad = new double[2];
        
        boolean testx0 = true;
        boolean testx1 = false;
        int ix = 0;
        int i = 0;
        while(testx0){
            if(xx<=this.xArray[i]){
                testx0 = false;
                if(xx==this.xArray[i])testx1 = true;
                ix = i;
            }
            else{
                i++;
            }
        }
        boolean testy0 = true;
        boolean testy1 = false;
        int iy = 0;
        i = 0;
        while(testy0){
            if(yy<=this.yArray[i]){
                testy0 = false;
                if(yy==this.yArray[i])testy1 = true;
                iy = i;
            }
            else{
                i++;
            }
        }
        int iflag = 0;
        if(testx1)iflag = 1;
        if(testy1)iflag = 2;
        if(testx1 && testy1)iflag = 3;
        if(!this.numArrayDone)this.numericalDerivativesArray();
        System.out.println(ix + " " + iy);
        switch(iflag){
            case 0: double hold0a = (yArray[iy-1]-yy)/(yArray[iy-1]-yArray[iy]);
                    double hold0b = (xArray[ix-1]-xx)/(xArray[ix-1]-xArray[ix]);
                    double temp0 = this.ndGrad2_X[ix][iy-1] - (this.ndGrad2_X[ix][iy-1] - this.ndGrad2_X[ix][iy])*hold0a;
                    double temp1 = this.ndGrad2_X[ix-1][iy-1] - (this.ndGrad2_X[ix-1][iy-1] - this.ndGrad2_X[ix-1][iy])*hold0a;  
                    grad[0] = temp1 - (temp1 - temp0)*hold0b;
                    temp0 = this.ndGrad2_Y[ix-1][iy] - (this.ndGrad2_Y[ix-1][iy] - this.ndGrad2_Y[ix][iy])*hold0b;
                    temp1 = this.ndGrad2_Y[ix-1][iy-1] - (this.ndGrad2_Y[ix-1][iy-1] - this.ndGrad2_Y[ix][iy-1])*hold0b;
                    grad[1] = temp1 - (temp1 - temp0)*hold0a;   
                    break;
            case 1: double hold1 = (yArray[iy-1]-yy)/(yArray[iy-1]-yArray[iy]);
                    grad[0] = this.ndGrad2_X[ix][iy-1] - (this.ndGrad2_X[ix][iy-1] - this.ndGrad2_X[ix][iy])*hold1;
                    grad[1] = this.ndGrad2_Y[ix][iy-1] - (this.ndGrad2_Y[ix][iy-1] - this.ndGrad2_Y[ix][iy])*hold1;
                    break;
            case 2: double hold2 = (xArray[ix-1]-xx)/(xArray[ix-1]-xArray[ix]);
                    grad[0] = this.ndGrad2_X[ix-1][iy] - (this.ndGrad2_X[ix-1][iy] - this.ndGrad2_X[ix][iy])*hold2;
                    grad[1] = this.ndGrad2_Y[ix-1][iy] - (this.ndGrad2_Y[ix-1][iy] - this.ndGrad2_Y[ix][iy])*hold2;
                    break;
            case 3: grad[0] = this.ndGrad2_X[ix][iy];
                    grad[1] = this.ndGrad2_Y[ix][iy];
        }
        return grad;
    } 
    
    // DEEP COPY
    public Gradient copy(){
        double[] hold0 = null;
        double[][] hold1 = null;
        
        Gradient newGrad = null;
        if(this.nDim==1){
            newGrad = new Gradient(this.xArray, this.fArray1d); 
            
        }
        else{
            newGrad = new Gradient(this.xArray, this.yArray, this.fArray2d); 
        }
        
        newGrad.xSampling = Conv.copy(this.xSampling);                                         
        newGrad.ySampling = Conv.copy(this.ySampling);         
        newGrad.xSamplingStart = this.xSamplingStart;
        newGrad.ySamplingStart = this.ySamplingStart;
        newGrad.xSamplingEnd = this.xSamplingEnd;
        newGrad.ySamplingEnd = this.ySamplingEnd; 
        newGrad.xSamplingPeriod = this.xSamplingPeriod;
        newGrad.ySamplingPeriod = this.ySamplingPeriod;
        newGrad.sampled = this.sampled;
        if(this.numDiffArray==null){
            newGrad.numDiffArray = null;
        }
        else{
            newGrad.numDiffArray = new ArrayList<Object>();
            if(this.nDim==1){
                hold0 = (double[])this.numDiffArray.get(0);              
                newGrad.numDiffArray.add(Conv.copy(hold0));
            }
            else{
                hold1 = (double[][])this.numDiffArray.get(0);
                newGrad.numDiffArray.add(Conv.copy(hold1));
                hold1 = (double[][])this.numDiffArray.get(1);
                newGrad.numDiffArray.add(Conv.copy(hold1));
            }
        }
        newGrad.ndGrad1 = Conv.copy(this.ndGrad1);
        newGrad.ndGrad2_X = Conv.copy(this.ndGrad2_X);
        newGrad.ndGrad2_Y = Conv.copy(this.ndGrad2_Y);
        newGrad.numArrayDone = this.numArrayDone;
        newGrad.nDim = this.nDim;
        newGrad.xLength = this.xLength;
        newGrad.yLength = this.yLength;
        newGrad.xLengthS = this.xLengthS;
        newGrad.yLengthS = this.yLengthS;  
        if(this.splineDerivArray==null){
            newGrad.splineDerivArray = null;
        }
        else{
            newGrad.splineDerivArray = new ArrayList<Object>();
            if(this.nDim==1){
                hold0 = (double[])this.splineDerivArray.get(0);              
                newGrad.splineDerivArray.add(Conv.copy(hold0));
            }
            else{
                hold1 = (double[][])this.splineDerivArray.get(0);
                newGrad.splineDerivArray.add(Conv.copy(hold1));
                hold1 = (double[][])this.splineDerivArray.get(1);
                newGrad.splineDerivArray.add(Conv.copy(hold1));
            }
        }        
        newGrad.csGrad1 = Conv.copy(this.csGrad1);
        newGrad.bcsGrad2_X = Conv.copy(this.bcsGrad2_X);
        newGrad.bcsGrad2_Y = Conv.copy(this.bcsGrad2_Y);
        newGrad.cs = this.cs;
        newGrad.bcs = this.bcs;
        newGrad.splineDone = this.splineDone;
         
        return newGrad;
    }
    
    public Object clone(){
        return (Object)this.copy();
    }   
}