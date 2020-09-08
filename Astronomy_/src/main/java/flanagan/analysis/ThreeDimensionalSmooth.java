/*
*   CLASS:      ThreeDimensionalSmooth
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
*   http://www.ee.ucl.ac.uk/~mflanaga/java/ThreeDimensionalSmooth.html
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
import flanagan.interpolation.TriCubicSpline;
import flanagan.math.Matrix;
import flanagan.plot.*;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ThreeDimensionalSmooth{
    
    private double[] xData = null;                                  // original x data, v = f(x,y,z)
    private double[] yData = null;                                  // original y data, v = f(x,y,z)  
    private double[] zData = null;                                  // original z data, v = f(x,y,z)
    private double[][][] vData = null;                              // original v data, v = f(x,y,z)
    private BigDecimal[] xBDdata = null;                            // original arbitrary precision x data, v = f(x,y,z)
    private BigDecimal[] yBDdata = null;                            // original arbitrary precision y data, v = f(x,y,z)
    private BigDecimal[] zBDdata = null;                            // original arbitrary precision y data, v = f(x,y,z)
    private BigDecimal[][][] vBDdata = null;                        // original arbitrary precision v data, v = f(x,y,z)
    private int nPointsX = 0;                                       // number of  x data points, v = f(x,y,z)
    private int nPointsY = 0;                                       // number of  y data points, v = f(x,y,z)
    private int nPointsZ = 0;                                       // number of  z data points, v = f(x,y,z)
    private int nPoints = 0;                                        // total number of data points
    private boolean arbprec = false;                                // = true if arbitrary precision data entered
    
    private double[][][]vDataMovAv = null;                          // moving average smoothed v data, double precision, v = f(x,y,z)
    private BigDecimal[][][] vDataMovAvBD = null;                   // moving average smoothed v data, arbitrary precision, v = f(x,y,z)
    private double[][][] vDataSavGol = null;                        // Savitzky-Golay smoothed v data, v = f(x,y,z)             
    private double[][][] derivSavGol = null;                        // Savitzky-Golay smoothed derivative (d^mnv/dx^mdy^n), v = f(x,y,z)  
    private int[][] sgCoeffIndices = null;                          // indices of the fitting polynomial and Savitzky-Golay filter coefficients
    private int nSGcoeff = 0;                                       // number of indices
    private int lastMethod = 0;                                     // = 0: none called
                                                                    // = 1: moving window average
                                                                    // = 2: Savitzky-Golay filter                                                           
    private int nMethods = 2;                                       // number of methods
                                                                                                                               
    private int maWindowWidthx = 0;                                 // x width of the smoothing cube in the running average method, v = f(x,y,z)
    private int maWindowWidthy = 0;                                 // y width of the smoothing cube in the running average method, v = f(x,y,z)
    private int maWindowWidthz = 0;                                 // y width of the smoothing cube in the running average method, z = f(x,y,z)

    private int sgWindowWidthx = 0;                                 // x width of smoothing window in the Savitzky-Golay method, v = f(x,y,z)
    private int sgWindowWidthy = 0;                                 // y width of smoothing window in the Savitzky-Golay method, v = f(x,y,z)
    private int sgWindowWidthz = 0;                                 // z width of smoothing window in the Savitzky-Golay method, v = f(x,y,z)

    private int sgPolyDeg = 4;                                      // Savitzky-Golay smoothing polynomial degree
    private double[][] sgArrayC = null;                             // Savitzky-Golay C matrix
   
    private boolean calcSavGol = false;                             // = true when Savitzky-Golay smoothing performed
    private boolean calcMovAv = false;                              // = true when moving average smoothing performed
    
    private boolean nthSet = false;                                 // = true when any Savitzky-Golay derivative smoothing performed
    
    private double extentMovAv = -1.0;                              // Extent of the moving average window smoothing
    private double extentSavGol = -1.0;                             // Extent of the Savitzky-Golay smoothing
        
    private TriCubicSpline tcsSavGol = null;                        // TriCubic spline interpolation of the Savitzky-Golay smoothed data
    private TriCubicSpline tcsMovAv = null;                         // TriCubic spline interpolation of the moving averagey smoothed data
    
    private int trunc = 4;                                          // truncation value for plot title values
    
    
    // CONSTRUCTORS
    
    // Constructor - data as  double, v=f(x,y,z)
    public ThreeDimensionalSmooth(double[] x, double[] y, double[] z, double[][][] v){ 
        this.xData = x;
        this.yData = y;
        this.zData = z;
        this.vData = v;
        this.polyIndices();
        this.check();
        this.ascend();
    }
    
    // Constructor - data as  double - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(double[][][] v){ 
        int n = v.length;
        this.vData = v;
        this.yData = new double[n];
        for(int i=0; i<n; i++)this.yData[i] = i;
        n = v[0].length;
        this.xData = new double[n];
        for(int i=0; i<n; i++)this.xData[i] = i;
        n = v[0][0].length;
        this.zData = new double[n];
        for(int i=0; i<n; i++)this.zData[i] = i;
        this.polyIndices();
        this.check();
    }
   
    // Constructor - data as  float, v=f(x,y,z)
    public ThreeDimensionalSmooth(float[] x, float[] y, float[] z, float[][][] v){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        am = new ArrayMaths(x);
        this.zData = am.array();
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_float_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
     
    // Constructor - data as  float - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(float[][][] v){ 
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.xData = new double[a];
        this.yData = new double[b];
        this.zData = new double[c];
        for(int i=0; i<a; i++)this.xData[i] = i;
        for(int i=0; i<b; i++)this.yData[i] = i;
        for(int i=0; i<c; i++)this.zData[i] = i;
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_float_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
    }

    // Constructor - data as  long, v=f(x,y,z)
    public ThreeDimensionalSmooth(long[] x, long[] y, long[] z, long[][][] v){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        am = new ArrayMaths(x);
        this.zData = am.array();
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_long_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
     
    // Constructor - data as  long - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(long[][][] v){ 
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.xData = new double[a];
        this.yData = new double[b];
        this.zData = new double[c];
        for(int i=0; i<a; i++)this.xData[i] = i;
        for(int i=0; i<b; i++)this.yData[i] = i;
        for(int i=0; i<c; i++)this.zData[i] = i;
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_long_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
    } 
 
    // Constructor - data as int, v=f(x,y,z)
    public ThreeDimensionalSmooth(int[] x, int[] y, int[] z, int[][][] v){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        am = new ArrayMaths(y);
        this.yData = am.array();
        am = new ArrayMaths(x);
        this.zData = am.array();
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_long_to_int(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
     
    // Constructor - data as  long - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(int[][][] v){ 
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.xData = new double[a];
        this.yData = new double[b];
        this.zData = new double[c];
        for(int i=0; i<a; i++)this.xData[i] = i;
        for(int i=0; i<b; i++)this.yData[i] = i;
        for(int i=0; i<c; i++)this.zData[i] = i;
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vData[i][j][k] = Conv.convert_int_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
    }    
     
    // Constructor - data as BigDecimal, v=f(x,y,z)
    public ThreeDimensionalSmooth(BigDecimal[] x, BigDecimal[] y, BigDecimal[] z, BigDecimal[][][] v){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        this.xBDdata = x;
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.yBDdata = y;
        am = new ArrayMaths(z);
        this.zData = am.array();
        this.zBDdata = z;
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.vData = new double[a][b][c];
        this.vBDdata = new BigDecimal[a][b][c];
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vBDdata[i][j][k] = v[i][j][k];
                    this.vData[i][j][k] = this.vBDdata[i][j][k].doubleValue();
               }
           }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
     
    // Constructor - data as BigDecimal - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(BigDecimal[][][] v){ 
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.vBDdata = new BigDecimal[a][b][c];
        this.xData = new double[a];
        this.yData = new double[b];
        this.zData = new double[c];
        this.xBDdata = new BigDecimal[a];
        this.yBDdata = new BigDecimal[b];
        this.zBDdata = new BigDecimal[c];
        for(int i=0; i<a; i++){
            this.xData[i] = i;
            this.xBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<b; i++){
            this.yData[i] = i;
            this.yBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<c; i++){
            this.zData[i] = i;
            this.zBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                   this.vBDdata[i][j][k] = v[i][j][k]; 
                   this.vData[i][j][k] = Conv.convert_BigDecimal_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
    } 
            
    // Constructor - data as BigInteger, v=f(x,y,z)
    public ThreeDimensionalSmooth(BigInteger[] x, BigInteger[] y, BigInteger[] z, BigInteger[][][] v){ 
        ArrayMaths am = new ArrayMaths(x);
        this.xData = am.array();
        this.xBDdata = am.array_as_BigDecimal();
        am = new ArrayMaths(y);
        this.yData = am.array();
        this.yBDdata = am.array_as_BigDecimal();
        am = new ArrayMaths(z);
        this.zData = am.array();
        this.zBDdata = am.array_as_BigDecimal();
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.vBDdata = new BigDecimal[a][b][c];
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                    this.vBDdata[i][j][k] = new BigDecimal(v[i][j][k]);
                    this.vData[i][j][k] = this.vBDdata[i][j][k].doubleValue();
               }
           }
        }
        this.polyIndices();
        this.check();
        this.ascend();
    }
 
    // Constructor - data as BigInteger - no x, y or z data, v=f(x,y,z)
    public ThreeDimensionalSmooth(BigInteger[][][] v){ 
        int a = v.length;
        int b = v[0].length;
        int c = v[0][0].length;
        this.vData = new double[a][b][c];
        this.vBDdata = new BigDecimal[a][b][c];
        this.xData = new double[a];
        this.yData = new double[b];
        this.zData = new double[c];
        this.xBDdata = new BigDecimal[a];
        this.yBDdata = new BigDecimal[b];
        this.zBDdata = new BigDecimal[c];
        for(int i=0; i<a; i++){
            this.xData[i] = i;
            this.xBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<b; i++){
            this.yData[i] = i;
            this.yBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<c; i++){
            this.zData[i] = i;
            this.zBDdata[i] = new BigDecimal((new Integer(i)).toString());
        }
        for(int i=0; i<a; i++){
           for(int j=0; j<b; j++){
               for(int k=0; k<c; k++){
                   this.vBDdata[i][j][k] = new BigDecimal(v[i][j][k]); 
                   this.vData[i][j][k] = Conv.convert_BigInteger_to_double(v[i][j][k]);
               }
           }
        }
        this.polyIndices();
        this.check();
    } 
    
    // Constructor - no data entered
    // private - for internal use
    public ThreeDimensionalSmooth(){
    }  
    
    
    // Store indices of the fitting polynomial
    private void polyIndices(){
        this.nSGcoeff  =  0;
        for(int i=1; i<=this.sgPolyDeg+1; i++){
            for(int j=1; j<=i; j++){
                this.nSGcoeff +=j;
            }
        }
        
        this.sgCoeffIndices = new int[this.nSGcoeff][3];
        int counter = 0;
        for(int i=0; i<=this.sgPolyDeg; i++){
            for(int j=0; j<=this.sgPolyDeg-i; j++){
                for(int k=0; k<=this.sgPolyDeg-j-i; k++){
                    this.sgCoeffIndices[counter][0] = i; 
                    this.sgCoeffIndices[counter][1] = j;
                    this.sgCoeffIndices[counter++][2] = k;
                }
            }
        }
    }
    
    // Check for correct dimensions,   data, v=f(x,y,z)
    private void check(){
        // array lengths
        this.nPointsZ = this.zData.length;
        this.nPointsY = this.yData.length;
        this.nPointsX = this.xData.length;
        this.nPoints = this.nPointsX*this.nPointsY*this.nPointsY;
        int m1 = this.vData.length;
        int m2 = this.vData[0].length;
        int m3 = this.vData[0][0].length;
        int mh = -1;
        
        // Check orientation 0f vData matches x, y and z dimensions
        boolean test = false;
        
        if(this.nPointsZ==m1 && this.nPointsY==m2 &&this.nPointsX==m3){
            // check all dimensions match
            test = true;
        }
        else{
            // check possible swap of y and z
            if(this.nPointsZ==m2 && this.nPointsY==m1 &&this.nPointsX==m3){
                double[][][] holdv = this.copy3D(vData);
                this.vData = new double[m2][m1][m3];
                for(int k=0; k<m3; k++){
                    for(int i=0; i<m2; i++){
                        for(int j=0; j<m1; j++){
                            this.vData[i][j][k] = holdv[j][i][k];
                        }
                    }
                }
                
                test = true;
                System.out.println("vData transposed to match the dimensions of the yData and zData");
            }
            else{
                // check possible swap of x and y
                if(this.nPointsZ==m1 && this.nPointsY==m3 &&this.nPointsX==m2){
                    double[][][] holdv = this.copy3D(vData);
                    this.vData = new double[m1][m3][m2];               
                    for(int i=0; i<m1; i++){
                        for(int j=0; j<m3; j++){
                            for(int k=0; k<m2; k++){
                            this.vData[i][j][k] = holdv[i][k][j];
                        }
                    }
                }
                test = true;
                System.out.println("vData transposed to match the dimensions of the xData and yData");
                }
                else{
                    // check possible swap of x and z
                    if(this.nPointsZ==m3 && this.nPointsY==m2 &&this.nPointsX==m1){
                        double[][][] holdv = this.copy3D(vData);
                        this.vData = new double[m3][m2][m1];      
                        for(int j=0; j<m3; j++){
                            for(int i=0; i<m1; i++){
                                for(int k=0; k<m2; k++){
                                    this.vData[i][j][k] = holdv[j][k][i];
                                }
                            }
                        }
                    }
                    test = true;
                    System.out.println("vData transposed to match the dimensions of the xData and zData");
                }
            }
        }
        if(!test){
                throw new IllegalArgumentException("The lengths of the x, y, and z data arrays, " + this.nPointsX + ", " + this.nPointsY + " and " + this.nPointsZ + ", do not match the dimensions of the z data matrix, " + m3 + ", " + m2 + " and " + m1);                
        }
        
        // Create an array of x, y and z values as BigDecimal if not already done
        if(!this.arbprec){
            this.xBDdata = new BigDecimal[this.nPointsX];
            this.yBDdata = new BigDecimal[this.nPointsY];
            this.zBDdata = new BigDecimal[this.nPointsZ];
            for(int i=0; i<this.nPointsX; i++){
               this.xBDdata[i] = new BigDecimal((new Double(this.xData[i])).toString()); 
            }
            for(int i=0; i<this.nPointsY; i++){
               this.yBDdata[i] = new BigDecimal((new Double(this.yData[i])).toString()); 
            }
            for(int i=0; i<this.nPointsZ; i++){
               this.zBDdata[i] = new BigDecimal((new Double(this.zData[i])).toString()); 
            }
        }
    }
    
    // Deep copy 3D array
    private double[][][] copy3D(double[][][] array){
        int mm1 = array.length;
        int mm2 = array[0].length;
        int mm3 = array[0][0].length;
    
        double[][][] holdv = new double[mm1][mm2][mm3];
    
        for(int i=0; i<mm1; i++){
            for(int j=0; j<mm2; j++){
                for(int k=0; k<mm3; k++){
                    holdv[i][j][k] = array[i][j][k];
                }
            }
        }
        return holdv;
    }
                
    // order data in ascending x-values,  v=f(x,y,z)
    private void ascend(){
        boolean test1 = true;
        boolean test2 = true;
        int ii = 1;
        while(test1){
            if(this.zData[ii]<this.zData[ii-1]){
                test1=false;
                test2=false;
            }
            else{
                ii++;
                if(ii>=this.nPointsZ)test1 = false;
            }
        }
        if(!test2){
            ArrayMaths am = new ArrayMaths(this.zData);
            am =  am.sort();
            int[] indices = am.originalIndices();
            double[] holdz = new double[this.nPointsZ];
            double[][][] holdv = new double[this.nPointsZ][this.nPointsY][this.nPointsX];
            BigDecimal[] holdzBD = new BigDecimal[this.nPointsZ];
            BigDecimal[][][] holdvBD = null;
            if(this.arbprec)holdvBD = new BigDecimal[this.nPointsZ][this.nPointsY][this.nPointsX];
            for(int i=0; i<this.nPointsZ; i++){
                holdz[i] = this.zData[indices[i]];
                holdzBD[i] = this.zBDdata[indices[i]];
                for(int j=0; j<this.nPointsY; j++){
                    for(int k=0; k<this.nPointsX; k++){
                        holdv[i][j][k] = this.vData[indices[i]][j][k];
                        if(this.arbprec)holdvBD[i][j][k] = this.vBDdata[indices[i]][j][k];
                    }
                }
            }
            for(int i=0; i<this.nPointsZ; i++){
                this.zData[i] = holdz[i];
                this.zBDdata[i] = holdzBD[i];
                for(int j=0; j<this.nPointsY; j++){
                    for(int k=0; k<this.nPointsX; k++){
                        this.vData[i][j][k] = holdv[i][j][k];
                        if(this.arbprec)this.vBDdata[i][j][k] = holdvBD[i][j][k];
                    }
                }
            }
        } 
        
        test1 = true;
        test2 = true;
        ii = 1;
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
            double[][][] holdv = new double[this.nPointsZ][this.nPointsY][this.nPointsX];
            BigDecimal[] holdyBD = new BigDecimal[this.nPointsY];
            BigDecimal[][][] holdvBD = null;
            if(this.arbprec)holdvBD = new BigDecimal[this.nPointsZ][this.nPointsY][this.nPointsX];
            for(int i=0; i<this.nPointsY; i++){
                holdy[i] = this.yData[indices[i]];
                holdyBD[i] = this.yBDdata[indices[i]];
                for(int j=0; j<this.nPointsZ; j++){
                    for(int k=0; k<this.nPointsX; k++){
                        holdv[j][i][k] = this.vData[j][indices[i]][k];
                        if(this.arbprec)holdvBD[j][i][k] = this.vBDdata[j][indices[i]][k];
                    }
                }
            }
            for(int i=0; i<this.nPointsY; i++){
                this.yData[i] = holdy[i];
                this.yBDdata[i] = holdyBD[i];
                for(int j=0; j<this.nPointsZ; j++){
                    for(int k=0; k<this.nPointsX; k++){
                        this.vData[j][i][k] = holdv[j][i][k];
                        if(this.arbprec)this.vBDdata[j][i][k] = holdvBD[j][i][k];
                    }
                }
            }
        }
               
        test1 = true;
        test2 = true;
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
            double[][][] holdv = new double[this.nPointsZ][this.nPointsY][this.nPointsX];
            BigDecimal[] holdxBD = new BigDecimal[this.nPointsX];
            BigDecimal[][][] holdvBD = null;
            if(this.arbprec)holdvBD = new BigDecimal[this.nPointsZ][this.nPointsY][this.nPointsX];
            for(int i=0; i<this.nPointsX; i++){
                holdx[i] = this.xData[indices[i]];
                holdxBD[i] = this.xBDdata[indices[i]];
                for(int j=0; j<this.nPointsZ; j++){
                    for(int k=0; k<this.nPointsY; k++){
                        holdv[j][k][indices[i]] = this.vData[j][k][indices[i]];
                        if(this.arbprec)holdvBD[j][k][indices[i]] = this.vBDdata[j][k][indices[i]];
                    }
                }
            }
            for(int i=0; i<this.nPointsX; i++){
                this.xData[i] = holdx[i];
                this.xBDdata[i] = holdxBD[i];
                for(int j=0; j<this.nPointsZ; j++){
                    for(int k=0; k<this.nPointsY; k++){
                        this.vData[j][k][i] = holdv[j][k][i];
                        if(this.arbprec)this.vBDdata[j][k][i] = holdvBD[j][k][i];
                    }
                }
            }
        } 
    }
       
    
    // MOVING AVERAGE WINDOW SMOOTHING
    // Smooth with a moving average window of dimensions maWindowWidthx, maWindowWidthy and maWindowWidthz
    public double[][][] movingAverage(int maWindowWidthx, int maWindowWidthy, int maWindowWidthz){
    
        this.lastMethod = 1;
        this.vDataMovAv = new double[this.nPointsZ][this.nPointsY][this.nPointsX];
        this.vDataMovAvBD = new BigDecimal[this.nPointsZ][this.nPointsY][this.nPointsX];

        // adjust window width to an odd number of points
        this.maWindowWidthx = this.windowLength(maWindowWidthx);
        int wwx = (this.maWindowWidthx - 1)/2;
        this.maWindowWidthy = this.windowLength(maWindowWidthy);
        int wwy = (this.maWindowWidthy - 1)/2;
        this.maWindowWidthz = this.windowLength(maWindowWidthz);
        int wwz = (this.maWindowWidthz - 1)/2;
        
        // Set x window limits
        int lpx = 0;
        int upx = 0;
        int lpy = 0;
        int upy = 0;
        int lpz = 0;
        int upz = 0;
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
                
                // Set z window limits
                for(int k=0; k<this.nPointsZ; k++){        
                    if(k>=wwz){
                        lpz = k - wwz;
                    }
                    else{
                        lpz = 0;               
                    }
                    if(k<=this.nPointsZ-wwy-1){                    
                        upz = k + wwz;
                    }
                    else{
                        upz = this.nPointsZ - 1;
                    }
                    int nw3 = upz - lpz + 1;
  
                    // Apply moving average window
                    if(this.arbprec){
                        BigDecimal sumbd = new BigDecimal("0.0");
                        for(int k1=lpx; k1<=upx; k1++){
                            for(int k2=lpy; k2<=upy; k2++){
                                for(int k3=lpz; k3<=upz; k3++){
                                    sumbd = sumbd.add(this.vBDdata[k3][k2][k1]);
                                }
                            }
                        }
                        String xx = (new Integer(nw1*nw2+nw3)).toString();
                        this.vDataMovAvBD[k][j][i] = sumbd.divide(new BigDecimal(xx), BigDecimal.ROUND_HALF_UP);
                        this.vDataMovAv[k][j][i] = this.vDataMovAvBD[k][j][i].doubleValue();
                    }
                    else{
                        double sum = 0.0;
                        for(int k1=lpx; k1<=upx; k1++){
                            for(int k2=lpy; k2<=upy; k2++){
                                for(int k3=lpz; k3<=upz; k3++){
                                    sum += this.vData[k3][k2][k1];
                                }
                            }
                        }
                        this.vDataMovAv[k][j][i] = sum/(nw1*nw2*nw3);
                        String xx = (new Double(this.vDataMovAv[k][j][i])).toString();
                        this.vDataMovAvBD[k][j][i] = new BigDecimal(xx);
                    }
                }
            }
        }
        
        // Set up interpolation
        this.tcsMovAv = new TriCubicSpline(this.zData, this.yData, this.xData, this.vDataMovAv);
                
        this.calcMovAv = true;
        return Conv.copy(this.vDataMovAv);
    }
    
    // Smooth with a moving average symmetric window of dimensions maWindowWidth, maWindowWidth and maWindowWidtz
    public double[][][] movingAverage(int maWindowWidth){
          return this.movingAverage(maWindowWidth, maWindowWidth, maWindowWidth);
    }
       
    // Smooth with a moving average window of dimensions maWindowWidthx and maWindowWidthy
    // Return values as BigDecimal
    public BigDecimal[][][] movingAverageAsBigDecimal(int maWindowWidthx, int maWindowWidthy, int maWindowWidthz){
        this.movingAverage(maWindowWidthx, maWindowWidthy, maWindowWidthx);
        return Conv.copy(this.vDataMovAvBD);
    }
          
    // Smooth with a moving average window of dimensions maWindowWidthx and maWindowWidthy
    // Return values as BigDecimal
    public BigDecimal[][][] movingAverageAsBigDecimal(int maWindowWidth){
        this.movingAverage(maWindowWidth, maWindowWidth, maWindowWidth);
        return Conv.copy(this.vDataMovAvBD);
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
    
    // Smooth with a Savitzky-Golay filter with a window of dimensions sgWindowWidthx, sgWindowWidthy and sgWindowWidthz
    public double[][][] savitzkyGolay(int sgWindowWidthx, int sgWindowWidthy, int sgWindowWidthz){              
       
        this.lastMethod = 2;
        
        this.vDataSavGol = new double[this.nPointsZ][this.nPointsY][this.nPointsX];
        
        // adjust window width to an odd number of points
        this.sgWindowWidthx = this.windowLength(sgWindowWidthx);
        this.sgWindowWidthy = this.windowLength(sgWindowWidthy);
        this.sgWindowWidthz = this.windowLength(sgWindowWidthz);
        
        // Apply filter 
        this.savitzkyGolayCommon(this.sgWindowWidthx, this.sgWindowWidthy, this.sgWindowWidthz);
                       
        // Set up interpolation
        this.tcsSavGol = new TriCubicSpline(this.zData, this.yData, this.xData, Conv.copy(this.vDataSavGol));
         
                 
        this.calcSavGol = true;
        return Conv.copy(this.vDataSavGol);
    }
    
    // Smooth with a Savitzky-Golay filter with a window of dimensions sgWindowWidth x sgWindowWidthy x sgWindowWidthz
    public double[][][] savitzkyGolay(int sgWindowWidth){ 
        return this.savitzkyGolay(sgWindowWidth, sgWindowWidth, sgWindowWidth);              
    }             
   
    // Common method for smoothing with a Savitzky-Golay filter with a window of width sgWindowWidth
    private double[][] []savitzkyGolayCommon(int widthx, int widthy, int widthz){
        
        // Set filter dimensions
        int wwx = (widthx - 1)/2;
        int wwy = (widthy - 1)/2;
        int wwz = (widthz - 1)/2;
  
        // Calculate filter coefficients
        double[] coeff = (this.savitzkyGolayFilter(wwx, wwx, wwy, wwy, wwz, wwz))[0];
        
        // Padout the data to solve edge effects
        double[][][] psData = this.padData(this.vData, wwx, wwy, wwz);

        // Apply filter       
        for(int i=wwz; i<this.nPointsZ+wwz; i++){
            for(int j=wwy; j<this.nPointsY+wwy; j++){  
                for(int k=wwx; k<this.nPointsX+wwx; k++){ 
                    double sum = 0.0;
                    int counter = 0;
                    for(int k1=i-wwz; k1<=i+wwz; k1++){
                        for(int k2=j-wwy; k2<=j+wwy; k2++){   
                            for(int k3=k-wwx; k3<=k+wwx; k3++){   
                                sum += psData[k1][k2][k3]*coeff[counter++];
                            }
                        }
                    }
                    this.vDataSavGol[i-wwz][j-wwy][k-wwx] = sum;
                }
            }
        }
        return this.vDataSavGol;
    }
 
    // Pad out data to solve edge effects
    private double[][][] padData(double[][][] data, int wwx, int wwy, int wwz){
        
        // Pad out to solve edge effects
        // Set dimensions
        int nz = data.length;
        int ny = data[0].length;
        int nx = data[0][0].length;
        
        // Create matrix for padding
        double[][][] psData = new double[nz+2*wwz][ny+2*wwy][nx+2*wwx];
        
        // fill central rectangle with true data
        for(int i=0; i<nz; i++){
           for(int j=0; j<ny; j++){
               for(int k=0; k<nx; k++){
                    psData[i+wwz][j+wwy][k+wwx] = data[i][j][k];
               }
           }
        }
        
        // x dimension side panels
        for(int i=0; i<wwx; i++){              
            for(int j=wwy; j<ny+wwy; j++){
                for(int k=wwz; k<nz+wwz; k++){
                    psData[k][j][i] = psData[k][j][wwx];
                }
            }
        }
        
        for(int i=nx+wwx; i<nx+2*wwx; i++){              
            for(int j=wwy; j<ny+wwy; j++){
                for(int k=wwz; k<nz+wwz; k++){
                    psData[k][j][i] = psData[k][j][nx+wwx-1];
                }
            }
        }
        
        // y dimension side panels
        for(int i=0; i<wwy; i++){              
            for(int j=wwx; j<nx+wwx; j++){
                for(int k=wwz; k<nz+wwz; k++){
                    psData[k][i][j] = psData[k][wwy][j];
                }
            }
        }
        
        for(int i=ny+wwy; i<ny+2*wwy; i++){              
            for(int j=wwx; j<nx+wwx; j++){
                for(int k=wwz; k<nz+wwz; k++){
                    psData[k][i][j] = psData[k][ny+wwy-1][j];
                }
            }
        }
    
        // z dimension side panels
        for(int i=0; i<wwz; i++){              
            for(int j=wwx; j<nx+wwx; j++){
                for(int k=wwy; k<ny+wwy; k++){
                    psData[i][k][j] = psData[wwz][k][j];
                }
            }
        } 
        
       for(int i=nz+wwz; i<nz+2*wwz; i++){              
            for(int j=wwx; j<nx+wwx; j++){
                for(int k=wwy; k<ny+wwy; k++){
                    psData[i][k][j] = psData[nz+wwz-1][k][j];
                }
            }
        } 
        
                
        // fill corners
        for(int i=0; i<wwz; i++){
            for(int j=0; j<wwy; j++){
                for(int k=0; k<wwx; k++){
                    psData[i][j][k] = psData[wwz][wwy][wwx];
                }
            }
        }
        
        for(int i=nz+wwz; i<nz+2*wwz; i++){
            for(int j=0; j<wwy; j++){
                for(int k=0; k<wwx; k++){
                    psData[i][j][k] = psData[nz+wwz-1][wwy][wwx];
                }
            }
        }
                
        for(int i=0; i<wwz; i++){
            for(int j=ny+wwy; j<ny+2*wwy; j++){
                for(int k=0; k<wwx; k++){
                    psData[i][j][k] = psData[wwz][ny+wwy-1][wwx];
                }
            }
        }
        
        for(int i=0; i<wwz; i++){
            for(int j=0; j<wwy; j++){
                for(int k=nx+wwx; k<nx+2*wwx; k++){
                    psData[i][j][k] = psData[wwz][wwy][nx+wwx-1];
                }
            }
        }
        
        for(int i=nz+wwz; i<nz+2*wwz; i++){
            for(int j=ny+wwy; j<ny+2*wwy; j++){
                for(int k=nx+wwx; k<nx+2*wwx; k++){
                    psData[i][j][k] = psData[nz+wwz][ny+wwy][nx+wwx];
                }
            }
        }
        
        for(int i=0; i<wwz; i++){
            for(int j=ny+wwy; j<ny+2*wwy; j++){
                for(int k=nx+wwx; k<nx+2*wwx; k++){
                    psData[i][j][k] = psData[wwz][ny+wwy][nx+wwx];
                }
            }
        }
        
        for(int i=nz+wwz; i<nz+2*wwz; i++){
            for(int j=0; j<wwy; j++){
                for(int k=nx+wwx; k<nx+2*wwx; k++){
                    psData[i][j][k] = psData[nz+wwz][wwy][nx+wwx];
                }
            }
        }
        
        for(int i=nz+wwz; i<nz+2*wwz; i++){
            for(int j=ny+wwy; j<ny+2*wwy; j++){
                for(int k=0; k<wwx; k++){
                    psData[i][j][k] = psData[nz+wwz][ny+wwy][wwx];
                }
            }
        }
                
        return psData;
}

    // Smooth with a Savitzky-Golay filter with a window of width sgWindowWidthx x sgWindowWidthy x sgWindowWidthz 
    // m.nth derivative also calculated
    public double[][][][] savitzkyGolay(int sgWindowWidthx, int sgWindowWidthy, int sgWindowWidthz, int m, int n, int o){
        if(n+m+o>this.sgPolyDeg)throw new IllegalArgumentException("The sum of the derivative orders " + m + " plus " + n + " plus " + o + ", must be less than or equal to the polynomial degree, " + this.sgPolyDeg + ".");
 
        this.lastMethod = 2;
        double[][][][] ret = new double[2][this.nPointsZ][this.nPointsY][this.nPointsX];
        
         // adjust window width to an odd number of points
        this.sgWindowWidthx = this.windowLength(sgWindowWidthx);
        int wwx = (this.sgWindowWidthx - 1)/2;
        this.sgWindowWidthy = this.windowLength(sgWindowWidthy);
        int wwy = (this.sgWindowWidthy - 1)/2;
        this.sgWindowWidthz = this.windowLength(sgWindowWidthz);
        int wwz = (this.sgWindowWidthz - 1)/2;
        
        if(!this.calcSavGol)this.savitzkyGolay(sgWindowWidthx, sgWindowWidthy, sgWindowWidthz);
        ret[0] = this.vDataSavGol;
        
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
        double[][][] psData = this.padData(this.vData, wwx, wwy, wwz);

        // Apply filter       
        for(int i=wwz; i<this.nPointsZ+wwz; i++){
            for(int j=wwy; j<this.nPointsY+wwy; j++){  
                for(int k=wwx; k<this.nPointsX+wwx; k++){  
                    double sum = 0.0;
                    int counter = 0;
                    for(int k1=i-wwz; k1<=i+wwz; k1++){
                        for(int k2=j-wwy; k2<=j+wwy; k2++){   
                            for(int k3=k-wwx; k3<=k+wwx; k3++){              
                                sum += psData[k1][k2][k3]*coeff[counter++];
                            }
                        }
                    }
                    ret[1][i-wwz][j-wwy][k-wwx] = sum;
                }
            }
        }
        
        this.derivSavGol = ret[1];
        this.nthSet = true;
        return ret;
    }
        
    // Savitzky-Golay filter
    // bp = number of backward points
    // fp = number of forward points
    // For the rationale of this method see Press, W.H., Teukolsky, S.A., Vetterling, W.T. and Flannery, B.P., Numerical Recipes, 
    // The Art of Scientific Computing, Cambridge University Press, 2nd Edition, 1992, Chapter 14, 
    // Section 8, Savitzky-Golay Smoothing Filters, pp 650 - 655. 
    public double[][] savitzkyGolayFilter(int bpx, int fpx, int bpy, int fpy, int bpz, int fpz){
        
        int wx = bpx + fpx + 1;                //filter x length 
        int wy = bpy + fpy + 1;                //filter y length 
        int wz = bpz + fpz + 1;                //filter z length 
        int www = wx*wy*wz;                    // number of points in the window
        double[] coeff = new double[www];      // Savitzky-Golay coefficients 
         
        int[][] values = new int[www][3];
        int counter = 0;
        for(int i = 0; i<wx; i++){
            for(int j =0; j<wy; j++){
                for(int k =0; k<wz; k++){
                    values[counter][0] = i-bpx;
                    values[counter][1] = j-bpy;
                    values[counter++][2] = k-bpz;
                }
            }
        }
       
        double[][] x = new double[www][this.nSGcoeff];
        for(int i=0; i<www; i++){
            for(int j = 0; j<this.nSGcoeff; j++){
                x[i][j] = Math.pow(values[i][0], this.sgCoeffIndices[j][0])*Math.pow(values[i][1], this.sgCoeffIndices[j][1])*Math.pow(values[i][2], this.sgCoeffIndices[j][2]);
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
    public static double[][] savitzkyGolayFilter(int bpx, int fpx, int bpy, int fpy, int bpz, int fpz, int deg){
        ThreeDimensionalSmooth susm = new ThreeDimensionalSmooth();
        susm.setSGpolyDegree(deg);
        return susm.savitzkyGolayFilter(bpx, fpx, bpy, fpy, bpz, fpz);
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
        ThreeDimensionalSmooth susm = new ThreeDimensionalSmooth();
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
    public double[][][] getMovingAverageValues(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.vDataMovAv);
    } 
     
    // Return moving average smoothed y data as BigDecimal
    public BigDecimal[][][] getMovingAverageValuesAsBigDecimal(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return Conv.copy(this.vDataMovAvBD);
    }
    
    // Return Savitzky-Golay smoothed y data
    public double[][][] getSavitzkyGolaySmoothedValues(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return Conv.copy(this.vDataSavGol);
    }
   
    // Return Savitzky-Golay smoothed last calculated derivatives d^m+nv/dx^mdy^n
    public double[][][] getSavitzkyDerivatives(){
        if(!this.nthSet)throw new IllegalArgumentException("No Savitzky-Golay derivative smoothing method has been called");
        return Conv.copy(this.derivSavGol);
    }
    
    // EXTENT OF THE SMOOTHING
    
    // Return the extent of the moving average window smoothing
    public double extentMovingAverage(){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        this.extentMovAv = this.extent(this.vData, this.vDataMovAv);
        return this.extentMovAv;
    }
    
    // Return the extent of the Savitzky-Golay smoothing
    public double extentSavitzkyGolay(){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        this.extentSavGol = this.extent(this.vData, this.vDataSavGol);
        return this.extentSavGol;
    }
    
    // Calculate extent of smoothing
    private double extent(double[][][] vOrigl, double[][][] vSmooth){
                
        ArrayMaths am = new ArrayMaths(vOrigl);
        double min = am.getMinimum();
        double max = am.getMaximum();       
        double range = max - min;
        
        double sum = 0.0;
        for(int i=0; i<this.nPointsX; i++){
            for(int j=0; j<this.nPointsY; j++){
                for(int k=0; k<this.nPointsZ; k++){
                    sum += Math.abs(vOrigl[k][j][i] - vSmooth[k][j][i])/range;
                }
            }
        }       
        sum /= this.nPoints;
        return sum;
    }
        
    
    // INTERPOLATION
    
    // interpolate the Savitzky-Golay smoothed data
    public double interpolateSavitzkyGolay(double xi, double yi, double zi){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        return tcsSavGol.interpolate(zi, yi, xi);
    }
    
    // interpolate the moving average smoothed data
    public double interpolateMovingAverage(double xi, double yi, double zi){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        return tcsMovAv.interpolate(zi, yi, xi);
    }
    
    
    // PLOTTING
    
    // Plot x section for a given y and z value indices - Savitzky-Golay
    public void plotSavitzkyGolayX(int yIndex, int zIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        if(zIndex>=this.nPointsZ)throw new IllegalArgumentException("The index, " + zIndex + ", must be less than the number of y values, " + this.nPointsZ);
 
        int flag = 0;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        double zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, yIndex, zIndex, yValue, zValue);        
    }
    
    // Plot x section for a given y and z value  - Savitzky-Golay
    public void plotSavitzkyGolayX(double yValue, double zValue){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int flag = 0;
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        int zIndex = this.findValue(this.zData, zValue);
        zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, yIndex, zIndex, yValue, zValue);        
    }
    
    // Plot y section for a given x and z value indices - Savitzky-Golay
    public void plotSavitzkyGolayY(int xIndex, int zIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(zIndex>=this.nPointsZ)throw new IllegalArgumentException("The index, " + zIndex + ", must be less than the number of y values, " + this.nPointsZ);
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
 
        int flag = 1;
        double zValue = Fmath.truncate(this.yData[zIndex], this.trunc);
        double xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, zIndex, xValue, zValue);        
    }
    
    // Plot y section for a given x and z value  - Savitzky-Golay
    public void plotSavitzkyGolayY(double xValue, double zValue){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int flag = 1;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        int zIndex = this.findValue(this.zData, zValue);
        zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, xIndex, zIndex, xValue, zValue);        
    }
    
   // Plot z section for a given x and y value indices - Savitzky-Golay
    public void plotSavitzkyGolayZ(int xIndex, int yIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
 
        int flag = 2;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        double xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, yIndex, xValue, yValue);        
    }

    // Plot z section for a given x and y value  - Savitzky-Golay
    public void plotSavitzkyGolayZ(double xValue, double yValue){
        if(!this.calcSavGol)throw new IllegalArgumentException("No Savitzky-Golay smoothing method has been called");
        int flag = 2;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, xIndex, yIndex, xValue, yValue);        
    }
    
    // Plot x section for a given y and z value indices - Moving Average
    public void plotMovingAverageX(int yIndex, int zIndex){
        if(!this.calcSavGol)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        if(zIndex>=this.nPointsZ)throw new IllegalArgumentException("The index, " + zIndex + ", must be less than the number of z values, " + this.nPointsZ);
 
        int flag = 3;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        double zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, yIndex, zIndex, yValue, zValue);        
    }
    
    // Plot x section for a given y and z value  - moving average
    public void plotMovingAverageX(double yValue, double zValue){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        int flag = 3;
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        int zIndex = this.findValue(this.zData, zValue);
        zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, yIndex, zIndex, yValue, zValue);        
    }
    
    // Plot y section for a given x and z value indices - Moving Average
    public void plotMovingAverageY(int xIndex, int zIndex){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(zIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + zIndex + ", must be less than the number of y values, " + this.nPointsZ);
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
 
        int flag = 4;
        double zValue = Fmath.truncate(this.yData[zIndex], this.trunc);
        double xValue = Fmath.truncate(this.yData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, zIndex, xValue, zValue);        
    }
    
    // Plot y section for a given x and z value  - moving average
    public void plotMovingAverageY(double xValue, double zValue){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        int flag = 4;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        int zIndex = this.findValue(this.zData, zValue);
        zValue = Fmath.truncate(this.zData[zIndex], this.trunc);
        this.commonPlot(flag, xIndex, zIndex, xValue, zValue);        
    }
    
   // Plot z section for a given x and y value indices - Moving Average
    public void plotMovingAverageZ(int xIndex, int yIndex){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving average smoothing method has been called");
        if(yIndex>=this.nPointsY)throw new IllegalArgumentException("The index, " + yIndex + ", must be less than the number of y values, " + this.nPointsY);
        if(xIndex>=this.nPointsX)throw new IllegalArgumentException("The index, " + xIndex + ", must be less than the number of x values, " + this.nPointsX);
 
        int flag = 5;
        double yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        double xValue = Fmath.truncate(this.yData[xIndex], this.trunc);
        this.commonPlot(flag, xIndex, yIndex, xValue, yValue);        
    }

    // Plot z section for a given x and y value  - moving average
    public void plotMovingAverageZ(double xValue, double yValue){
        if(!this.calcMovAv)throw new IllegalArgumentException("No moving avrage smoothing method has been called");
        int flag = 5;
        int xIndex = this.findValue(this.xData, xValue);
        xValue = Fmath.truncate(this.xData[xIndex], this.trunc);
        int yIndex = this.findValue(this.yData, yValue);
        yValue = Fmath.truncate(this.yData[yIndex], this.trunc);
        this.commonPlot(flag, xIndex, yIndex, xValue, yValue);        
    }
    
    // Find index for a value
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
    private void commonPlot(int flag, int point1, int point2, double value1, double value2){

        String title0 = null;
        String title1 = null;
        String title1b = ",  Original data - circles,  Smoothed data - squares";
        String xleg = null;
        String yleg = null;
        int[] xPoints ={0, this.nPointsX/4, this.nPointsX/2, 3*this.nPointsX/4, this.nPointsX-1};
        int[] yPoints ={0, this.nPointsY/4, this.nPointsY/2, 3*this.nPointsY/4, this.nPointsY-1};
        int[] zPoints ={0, this.nPointsZ/4, this.nPointsZ/2, 3*this.nPointsZ/4, this.nPointsZ-1};
        double[][] data = new double[8][];
        double[] holdx1 = new double[5];
        double[] holdx2 = new double[5];
        double[] holdy1 = new double[5];
        double[] holdy2 = new double[5];
        double[] holdz1 = new double[5];
        double[] holdz2 = new double[5];
        
        switch(flag){
            
           case 0:  // Savitzky-Golay  v versus x
                    title0 = "Savitzky-Golay smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus x values for a y value of " + value1 + " a z value of "+  value2 + title1b;
                    xleg = "x values";  // CHANGE FOR SWAPPED INDICES
                    yleg = "v values";
                    data[0] = this.xData;
                    data[1] = this.vData[point2][point1];
                    data[2] = this.xData;
                    data[3] = this.vDataSavGol[point2][point1];
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.xData[xPoints[i]];
                        holdy1[i] = this.vData[point2][point1][xPoints[i]];
                        holdx2[i] = this.xData[xPoints[i]];
                        holdy2[i] = this.vDataSavGol[point2][point1][xPoints[i]];
                    }
                    break;
                
            case 1: // Savitzky-Golay  v versus y
                    title0 = "Savitzky-Golay smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus y values for a x value of " + value1 + " a z value of "+  value2 + title1b;
                    xleg = "y values";
                    yleg = "v values";
                    data[0] = this.yData;
                    data[2] = this.yData;
                    data[1] = new double[this.nPointsY];
                    data[3] = new double[this.nPointsY];
                    for(int i=0; i<nPointsZ; i++){
                        data[1][i] = this.vData[point2][i][point1];
                        data[3][i] = this.vDataSavGol[point2][i][point1];
                    }
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.yData[yPoints[i]];
                        holdy1[i] = data[1][yPoints[i]];
                        holdx2[i] = this.yData[yPoints[i]];
                        holdy2[i] = data[3][yPoints[i]];
                    }
                    break;
                
            case 2: // Savitzky-Golay  v versus z
                    title0 = "Savitzky-Golay smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus z values for a x value of " + value1 + " a y value of "+  value2 + title1b;
                    xleg = "z values";
                    yleg = "v values";
                    data[0] = this.zData;
                    data[2] = this.zData;
                    data[1] = new double[this.nPointsZ];
                    data[3] = new double[this.nPointsZ];
                    for(int i=0; i<nPointsZ; i++){
                        data[1][i] = this.vData[i][point2][point1];
                        data[3][i] = this.vDataSavGol[i][point2][point1];
                    }
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.zData[zPoints[i]];
                        holdy1[i] = data[1][zPoints[i]];
                        holdx2[i] = this.zData[zPoints[i]];
                        holdy2[i] = data[3][zPoints[i]];
                    }
                    break;
            
            case 3: // Moving Average  v versus x
                    title0 = "Moving average smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus x values for a y value of " + value1 + " a z value of "+  value2 + title1b;
                    xleg = "x values";  // CHANGE FOR SWAPPED INDICES
                    yleg = "v values";
                    data[0] = this.xData;
                    data[1] = this.vData[point2][point1];
                    data[2] = this.xData;
                    data[3] = this.vDataMovAv[point2][point1];
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.xData[xPoints[i]];
                        holdy1[i] = this.vData[point2][point1][xPoints[i]];
                        holdx2[i] = this.xData[xPoints[i]];
                        holdy2[i] = this.vDataMovAv[point2][point1][xPoints[i]];
                    }
                    break;
                
            case 4: // Moving average smoothing   v versus y
                    title0 = "Moving average smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus y values for a x value of " + value1 + " a z value of "+  value2 + title1b;
                    xleg = "y values";
                    yleg = "v values";
                    data[0] = this.yData;
                    data[2] = this.yData;
                    data[1] = new double[this.nPointsY];
                    data[3] = new double[this.nPointsY];
                    for(int i=0; i<nPointsZ; i++){
                        data[1][i] = this.vData[point2][i][point1];
                        data[3][i] = this.vDataMovAv[point2][i][point1];
                    }
                    for(int i=0; i<5;i++){
                        holdx1[i] = this.yData[yPoints[i]];
                        holdy1[i] = data[1][yPoints[i]];
                        holdx2[i] = this.yData[yPoints[i]];
                        holdy2[i] = data[3][yPoints[i]];
                    }
                    break;
                
            case 5: // Moving average smmothing  v versus z
                    title0 = "Moving average smoothing with an x by y by z window of " + this.maWindowWidthx + " x " +  + this.maWindowWidthy + " x " +  + this.maWindowWidthz + " points";
                    title1 = "Plot of v versus z values for a x value of " + value1 + " a y value of "+  value2 + title1b;
                    xleg = "z values";
                    yleg = "v values";
                    data[0] = this.yData;
                    data[2] = this.yData;
                    data[1] = new double[this.nPointsZ];
                    data[3] = new double[this.nPointsZ];
                    for(int i=0; i<nPointsZ; i++){
                        data[1][i] = this.vData[i][point2][point1];
                        data[3][i] = this.vDataMovAv[i][point2][point1];
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