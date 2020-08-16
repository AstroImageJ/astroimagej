/*
*   Class Integration
*       interface IntegralFunction also required
*
*   Contains the methods for Gaussian-Legendre quadrature, the
*   backward and forward rectangular rules and the trapezium rule
*
*   The function to be integrated is supplied by means of
*       an interface, IntegralFunction
*
*   WRITTEN BY: Dr Michael Thomas Flanagan

*   DATE:       February 2002
*   UPDATE:     22 June 2003, 16 July 2006, 25 April 2007, 2 May 2007, 4 July 2008, 22 September 2008, 6 November 2012
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web page:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Integration.html
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*
*   Copyright (c) 2002 - 2012 Michael Thomas Flanagan
*
* PERMISSION TO COPY:
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

package flanagan.integration;

import java.util.*;
import flanagan.math.Fmath;

// Numerical integration class
public class Integration{

        private IntegralFunction integralFunc = null;   // Function to be integrated
        private boolean setFunction = false;            // = true when IntegralFunction set
        private double lowerLimit = Double.NaN;         // Lower integration limit
        private double upperLimit = Double.NaN;         // Upper integration limit
        private boolean setLimits = false;              // = true when limits set

        private int glPoints = 0;                       // Number of points in the Gauss-Legendre integration
        private boolean setGLpoints = false;            // = true when glPoints set
        private int nIntervals = 0;                     // Number of intervals in the rectangular rule integrations
        private boolean setIntervals = false;           // = true when nIntervals set

        private double integralSum = 0.0D;              // Sum returned by the numerical integration method
        private double integralMean = 0.0;              // mean returned by Gaussian quadrature
        private double integralSD = 0.0;                // sd returned by Gaussian quadrature
        private boolean integrationDone = false;        // = true when integration performed
        private boolean integrationStatsDone = false;   // = true when integration with mean and sd calculation performed
        
    	// ArrayLists to hold Gauss-Legendre Coefficients saving repeated calculation
    	private ArrayList<Integer> gaussQuadIndex = new ArrayList<Integer>();               // Gauss-Legendre indices
    	private ArrayList<double[]> gaussQuadDistArrayList = new ArrayList<double[]>();     // Gauss-Legendre distances
        private double[] gaussQuadDist = null;                                              // Working array of Gauss-Legendre distances
    	private ArrayList<double[]> gaussQuadWeightArrayList = new ArrayList<double[]>();   // Gauss-Legendre weights
        private	double[] gaussQuadWeight = null;                                            // Working array of Gauss-Legendre 
        private double glPrec = 3e-11;                                                      // Relative precision in the calculation of Gauss-Legendre weights
 
    	// Iterative trapezium rule
    	private double requiredAccuracy = 0.0D;     // required accuracy at which iterative trapezium is terminated
    	private double trapeziumAccuracy = 0.0D;    // actual accuracy at which iterative trapezium is terminated as instance variable
    	private static double trapAccuracy = 0.0D;  // actual accuracy at which iterative trapezium is terminated as class variable
    	private int maxIntervals = 0;               // maximum number of intervals allowed in iterative trapezium
    	private int trapeziumIntervals = 1;         // number of intervals in trapezium at which accuracy was satisfied as instance variable
    	private static int trapIntervals = 1;       // number of intervals in trapezium at which accuracy was satisfied as class variable

    	// CONSTRUCTORS

        // Default constructor
        public Integration(){
        }

        // Constructor taking function to be integrated
        public Integration(IntegralFunction intFunc){
            this.integralFunc = intFunc;
            this.setFunction = true;
        }

        // Constructor taking function to be integrated and the limits
        public Integration(IntegralFunction intFunc, double lowerLimit, double upperLimit){
            this.integralFunc = intFunc;
            this.setFunction = true;
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;
            this.setLimits = true;
        }

    	// SET METHODS

        // Set function to be integrated
        public void setIntegrationFunction(IntegralFunction intFunc){
            this.integralFunc = intFunc;
            this.setFunction = true;
        }

        // Set limits
        public void setLimits(double lowerLimit, double upperLimit){
             this.lowerLimit = lowerLimit;
             this.upperLimit = upperLimit;
             this.setLimits = true;
        }

        // Set lower limit
        public void setLowerLimit(double lowerLimit){
             this.lowerLimit = lowerLimit;
             if(!Fmath.isNaN(this.upperLimit))this.setLimits=true;
        }

        // Set lower limit
        public void setlowerLimit(double lowerLimit){
             this.lowerLimit = lowerLimit;
             if(!Fmath.isNaN(this.upperLimit))this.setLimits=true;
        }

        // Set upper limit
        public void setUpperLimit(double upperLimit){
             this.upperLimit = upperLimit;
             if(!Fmath.isNaN(this.lowerLimit))this.setLimits=true;
        }

        // Set upper limit
        public void setupperLimit(double upperLimit){
             this.upperLimit = upperLimit;
             if(!Fmath.isNaN(this.lowerLimit))this.setLimits=true;
        }

        // Set number of points in the Gaussian Legendre integration
        public void setGLpoints(int nPoints){
             this.glPoints = nPoints;
             this.setGLpoints = true;
        }

        // Set number of intervals in trapezoidal, forward or backward rectangular integration
        public void setNintervals(int nIntervals){
             this.nIntervals = nIntervals;
             this.setIntervals = true;
        }

    	// GET METHODS

        // Get the sum returned by the numerical integration
        public double getIntegralSum(){
            if(!this.integrationDone)throw new IllegalArgumentException("No integration has been performed");
            return this.integralSum;
        }
        
        // Get the continious function mean returned by the numerical integration
        // Only appies to Gaussian quadrature
        public double getIntegralMean(){
            if(!this.integrationStatsDone)throw new IllegalArgumentException("No relevant integration has been performed");
            return this.integralMean;
        }
        
        // Get the continious function sd returned by the numerical integration
        // Only appies to Gaussian quadrature
        public double getIntegralStandardDeviation(){
            if(!this.integrationStatsDone)throw new IllegalArgumentException("No relevant integration has been performed");
            return this.integralSD;
        }

        // Return current Gaussian-Legendre quadrature distances
        public double[] getGauassQuadDistances(){
            return this.gaussQuadDist;
        }
        
        // Return current Gaussian-Legendre quadrature weights
        public double[] getGauassQuadWeights(){
            return this.gaussQuadWeight;
        }
        
    	// GAUSSIAN-LEGENDRE QUADRATURE

    	// Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    	// All parametes preset
    	public double gaussQuad(){
    	    if(!this.setGLpoints)throw new IllegalArgumentException("Number of points not set");
    	    if(!this.setLimits)throw new IllegalArgumentException("One limit or both limits not set");
    	    if(!this.setFunction)throw new IllegalArgumentException("No integral function has been set");

        	this.gaussQuadDist = new double[this.glPoints];
        	this.gaussQuadWeight = new double[this.glPoints];
        	boolean test = true;
        	int kn=-1;

        	// Get Gauss-Legendre coefficients, i.e. the weights and scaled distances
        	// Check if coefficients have been already calculated on an earlier call
        	if(!this.gaussQuadIndex.isEmpty()){
            		for(int k=0; k<this.gaussQuadIndex.size(); k++){
                		Integer ki = this.gaussQuadIndex.get(k);
                		if(ki.intValue()==this.glPoints){
                    			test=false;
                    			kn = k;
                		}
            		}
        	}

        	if(test){
            		// Calculate and store coefficients
            		this.gaussQuadCoeff(this.glPoints);
            		this.gaussQuadIndex.add(new Integer(this.glPoints));
            		this.gaussQuadDistArrayList.add(this.gaussQuadDist);
            		this.gaussQuadWeightArrayList.add(this.gaussQuadWeight);
        	}
        	else{
        		// Recover coefficients
            		this.gaussQuadDist = this.gaussQuadDistArrayList.get(kn);
            		this.gaussQuadWeight = this.gaussQuadWeightArrayList.get(kn);
        	}

        	// Perform summation
                double sum=0.0D;
        	double xplus = 0.5D*(this.upperLimit + this.lowerLimit);
        	double xminus = 0.5D*(this.upperLimit - this.lowerLimit);
        	double dx = 0.0D;
        	for(int i=0; i<glPoints; i++){
            		dx = xminus*this.gaussQuadDist[i];
            		sum += this.gaussQuadWeight[i]*this.integralFunc.function(xplus+dx);
        	}
        	this.integralSum = sum*xminus;      // rescale
        	this.integrationDone = true;        // integration performed
        	return this.integralSum;            // return value
    	}

    	// Numerical integration using n point Gaussian-Legendre quadrature (instance method)
        // All parametes except the number of points in the Gauss-Legendre integration preset
    	public double gaussQuad(int glPoints){
    	    this.glPoints = glPoints;
    	    this.setGLpoints = true;
            return this.gaussQuad();
        }

    	// Numerical integration using n point Gaussian-Legendre quadrature (static method)
        // All parametes provided
    	public static double gaussQuad(IntegralFunction intFunc, double lowerLimit, double upperLimit, int glPoints){
    	    Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
    	    return intgrtn.gaussQuad(glPoints);
    	}
        
        // Sum, mean and SD of a continuous distribution
        // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    	// All parametes preset
    	public ArrayList<Double> gaussQuadPlusMeanAndSD(){
    	    if(!this.setGLpoints)throw new IllegalArgumentException("Number of points not set");
    	    if(!this.setLimits)throw new IllegalArgumentException("One limit or both limits not set");
    	    if(!this.setFunction)throw new IllegalArgumentException("No integral function has been set");

        	this.gaussQuadDist = new double[this.glPoints];
        	this.gaussQuadWeight = new double[this.glPoints];
        	double xplus = 0.5D*(this.upperLimit + this.lowerLimit);
        	double xminus = 0.5D*(this.upperLimit - this.lowerLimit);
        	boolean test = true;
        	int kn = -1;

        	// Get Gauss-Legendre coefficients, i.e. the weights and scaled distances
        	// Check if coefficients have been already calculated on an earlier call
        	if(!this.gaussQuadIndex.isEmpty()){
            		for(int k=0; k<this.gaussQuadIndex.size(); k++){
                		Integer ki = this.gaussQuadIndex.get(k);
                		if(ki.intValue()==this.glPoints){
                    			test=false;
                    			kn = k;
                		}
            		}
        	}

        	if(test){
            		// Calculate and store coefficients
            		this.gaussQuadCoeff(this.glPoints);
            		this.gaussQuadIndex.add(new Integer(this.glPoints));
            		this.gaussQuadDistArrayList.add(this.gaussQuadDist);
            		this.gaussQuadWeightArrayList.add(this.gaussQuadWeight);
        	}
        	else{
        		// Recover coefficients
            		this.gaussQuadDist = this.gaussQuadDistArrayList.get(kn);
            		this.gaussQuadWeight = this.gaussQuadWeightArrayList.get(kn);
        	}

        	// Perform summation and mean calculation
                double sum = 0.0D;
                double sum0 = 0.0D;
                double posn = 0.0;
                double mean = 0.0D;
                double dx = 0.0D;
        	for(int i=0; i<glPoints; i++){
            		dx = xminus*gaussQuadDist[i];
                        posn = xplus+dx;
                        sum0 = this.gaussQuadWeight[i]*this.integralFunc.function(posn);
                        sum += sum0;
                        mean += sum0*posn;
        	}
                this.integralSum = sum*xminus;
        	this.integralMean = mean/sum;
                
                // Perform sd calculation
                double sd = 0.0;
        	for(int i=0; i<glPoints; i++){
                    dx = xminus*this.gaussQuadDist[i];
                    posn = xplus+dx;
                    sum0 = this.gaussQuadWeight[i]*this.integralFunc.function(posn);
                    sd += Fmath.square(posn - this.integralMean)*sum0;
                }
                this.integralSD = Math.sqrt(sd/sum);
                
                ArrayList<Double> ret = new ArrayList<Double>();
                ret.add(new Double(this.integralSum));
                ret.add(new Double(this.integralMean));
                ret.add(new Double(this.integralSD));
                
                this.integrationStatsDone = true;
                this.integrationDone = true;
                
        	return ret;                  
    	}
        
        // Sum, mean and sd of a continuous distribution
        // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
        // All parametes except the number of points in the Gauss-Legendre integration preset
    	public ArrayList<Double> gaussQuadPlusMeanAndSD(int glPoints){
    	    this.glPoints = glPoints;
    	    this.setGLpoints = true;
            return this.gaussQuadPlusMeanAndSD();
        }

    	// Sum, mean and sd of a continuous distribution
        // Numerical integration using n point Gaussian-Legendre quadrature (static method)
        // All parametes provided
    	public static ArrayList<Double> gaussQuadPlusMeanAndSD(IntegralFunction intFunc, double lowerLimit, double upperLimit, int glPoints){
    	    Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
    	    return intgrtn.gaussQuadPlusMeanAndSD(glPoints);
    	}


    	// Calculates the distance (gaussQuadDist) and weight coefficients (gaussQuadCoeff)
    	// for an n point Gauss-Legendre Quadrature.
    	// The Gauss-Legendre distances, gaussQuadDist, are scaled to -1 to 1
    	// See Numerical Recipes for details of this procedure
    	public void gaussQuadCoeff(int n){

	    	double approxRoot0=0.0D;
                double approxRoot1=0.0D;
		
                double legendrePoly1=0.0D;
                double legendrePoly2=0.0D;
                double legendrePoly3=0.0D;
                double derivLegendrePoly1=0.0D;
                
	    	double	x1 = -1.0D;		// lower limit
	    	double	x2 = 1.0D;		// upper limit

	    	// Calculate roots
	    	// Roots are symmetrical - only half calculated
	    	int nh = (n+1)/2;
	    	double xplus = 0.5D*(x2+x1);
	    	double xminus = 0.5D*(x2-x1);

	    	// Loop for  each root
	    	for(int i=1; i<=nh; i++){
			// Approximation of ith root
		    	approxRoot0 = Math.cos(Math.PI*(i-0.25D)/(n+0.5D));

		    	// Refinement on above using Newton's method
		    	do{
			    	legendrePoly1 = 1.0D;
			    	legendrePoly2 = 0.0D;

			    	// Legendre polynomial (legendrePoly1, evaluated at approxRoot0, legendrePoly2 is polynomial one order lower) recurrence relationsip
			    	for(int j=1; j<=n; j++){
				    	legendrePoly3 = legendrePoly2;
				    	legendrePoly2 = legendrePoly1;
				    	legendrePoly1= ((2.0D*j - 1.0D)*approxRoot0*legendrePoly2 - (j - 1.0D)*legendrePoly3)/j;
			    	}
			    	derivLegendrePoly1 = n*(approxRoot0*legendrePoly1 - legendrePoly2)/(approxRoot0*approxRoot0 - 1.0D);    // Derivative of legendrePoly1
			    	approxRoot1 = approxRoot0;
			    	approxRoot0 = approxRoot1 - legendrePoly1/derivLegendrePoly1;       // Newton's method
		    	} while(Math.abs(approxRoot0 - approxRoot1) > this.glPrec);

		    	this.gaussQuadDist[i-1] = xplus - xminus*approxRoot0;                       // Scale root to desired interval
		    	this.gaussQuadDist[n-i] = xplus + xminus*approxRoot0;                       // Symmetric counterpart
		    	this.gaussQuadWeight[i-1] = 2.0*xminus/((1.0 - approxRoot0*approxRoot0)*derivLegendrePoly1*derivLegendrePoly1);     // Compute weight
		    	this.gaussQuadWeight[n-i] = this.gaussQuadWeight[i-1];                      // Symmetric counterpart
	    	}
    	}
        
        // Returns the distance (gaussQuadDist) and weight coefficients (gaussQuadCoeff)
    	// for an n point Gauss-Legendre Quadrature.
    	// The Gauss-Legendre distances, gaussQuadDist, are scaled to -1 to 1
    	// See Numerical Recipes for details
        //  Static method
    	public static void gaussQuadCoeff(double[] gaussQuadDist, double[] gaussQuadWeight, int n){
            Integration intg = new Integration();
            intg.gaussQuadCoeff(n);
            gaussQuadDist = intg.getGauassQuadDistances();
            gaussQuadWeight = intg.getGauassQuadWeights();
        }
        

    	// TRAPEZIUM METHODS

    	// Numerical integration using the trapeziodal rule (instance method)
    	// all parameters preset
    	public double trapezium(){
    	    if(!this.setIntervals)throw new IllegalArgumentException("Number of intervals not set");
    	    if(!this.setLimits)throw new IllegalArgumentException("One limit or both limits not set");
    	    if(!this.setFunction)throw new IllegalArgumentException("No integral function has been set");

        	double 	y1 = 0.0D;
        	double 	interval = (this.upperLimit - this.lowerLimit)/this.nIntervals;
        	double	x0 = this.lowerLimit;
        	double 	x1 = this.lowerLimit + interval;
        	double	y0 = this.integralFunc.function(x0);
        	this.integralSum= 0.0D;

		    for(int i=0; i<nIntervals; i++){
		            // adjust last interval for rounding errors
		            if(x1>this.upperLimit){
		                x1 = this.upperLimit;
		                interval -= (x1 - this.upperLimit);
		            }

		            // perform summation
            		y1 = this.integralFunc.function(x1);
            		this.integralSum += 0.5D*(y0+y1)*interval;
            		x0 = x1;
            		y0 = y1;
            		x1 += interval;
        	}
        	this.integrationDone = true;
        	return this.integralSum;
    	}

    	// Numerical integration using the trapeziodal rule (instance method)
    	// all parameters except the number of intervals preset
    	public double trapezium(int nIntervals){
    	    this.nIntervals = nIntervals;
    	    this.setIntervals = true;
            return this.trapezium();
        }

    	// Numerical integration using the trapeziodal rule (static method)
    	// all parameters to be provided
    	public static double trapezium(IntegralFunction intFunc, double lowerLimit, double upperLimit, int nIntervals){
  	    Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
    	    return intgrtn.trapezium(nIntervals);
    	}

    	// Numerical integration using an iteration on the number of intervals in the trapeziodal rule
    	// until two successive results differ by less than a predetermined accuracy times the penultimate result
    	public double trapezium(double accuracy, int maxIntervals){
    	    this.requiredAccuracy = accuracy;
    	    this.maxIntervals = maxIntervals;
        	this.trapeziumIntervals = 1;

        	double  summ = Integration.trapezium(this.integralFunc, this.lowerLimit, this.upperLimit, 1);
        	double oldSumm = summ;
        	int i = 2;
        	for(i=2; i<=this.maxIntervals; i++){
            		summ = Integration.trapezium(this.integralFunc, this.lowerLimit, this.upperLimit, i);
            		this.trapeziumAccuracy = Math.abs((summ - oldSumm)/oldSumm);
            		if(this.trapeziumAccuracy<=this.requiredAccuracy)break;
            		oldSumm = summ;
        	}

		    if(i > this.maxIntervals){
            		System.out.println("accuracy criterion was not met in this.trapezium - current sum was returned as result.");
            		this.trapeziumIntervals = this.maxIntervals;
        	}
        	else{
            		this.trapeziumIntervals = i;
        	}
        	trapIntervals = this.trapeziumIntervals;
        	trapAccuracy = this.trapeziumAccuracy;
        	return summ;
    	}

    	// Numerical integration using an iteration on the number of intervals in the trapeziodal rule (static method)
    	// until two successive results differ by less than a predtermined accuracy times the penultimate result
    	// All parameters to be provided
    	public static double trapezium(IntegralFunction intFunc, double lowerLimit, double upperLimit, double accuracy, int maxIntervals){
  	    Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
    	    return intgrtn.trapezium(accuracy, maxIntervals);
        }

    	// Get the number of intervals at which accuracy was last met in trapezium if using the instance trapezium call
    	public int getTrapeziumIntervals(){
        	return this.trapeziumIntervals;
    	}

    	// Get the number of intervals at which accuracy was last met in trapezium if using static trapezium call
    	public static int getTrapIntervals(){
        	return trapIntervals;
    	}

    	// Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the instance method
    	public double getTrapeziumAccuracy(){
        	return this.trapeziumAccuracy;
    	}

    	// Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the static method
    	public static double getTrapAccuracy(){
        	return trapAccuracy;
    	}

    	// BACKWARD RECTANGULAR METHODS

    	// Numerical integration using the backward rectangular rule (instance method)
    	// All parameters preset
    	public double backward(){
    	    if(!this.setIntervals)throw new IllegalArgumentException("Number of intervals not set");
    	    if(!this.setLimits)throw new IllegalArgumentException("One limit or both limits not set");
    	    if(!this.setFunction)throw new IllegalArgumentException("No integral function has been set");

        	double interval = (this.upperLimit - this.lowerLimit)/this.nIntervals;
        	double x = this.lowerLimit + interval;
        	double y = this.integralFunc.function(x);
        	this.integralSum = 0.0D;

        	for(int i=0; i<this.nIntervals; i++){
        	        // adjust last interval for rounding errors
		            if(x>this.upperLimit){
		                x = this.upperLimit;
		                interval -= (x - this.upperLimit);
		            }

		            // perform summation
            		y = this.integralFunc.function(x);
            		this.integralSum += y*interval;
            		x += interval;
        	}

        	this.integrationDone = true;
        	return this.integralSum;
    	}

    	// Numerical integration using the backward rectangular rule (instance method)
    	// all parameters except number of intervals preset
    	public double backward(int nIntervals){
    	    this.nIntervals = nIntervals;
    	    this.setIntervals = true;
            return this.backward();
        }

    	// Numerical integration using the backward rectangular rule (static method)
    	// all parameters must be provided
    	public static double backward(IntegralFunction intFunc, double lowerLimit, double upperLimit, int nIntervals){
   	    Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
    	    return intgrtn.backward(nIntervals);
         }

    	// FORWARD RECTANGULAR METHODS

    	// Numerical integration using the forward rectangular rule
    	// all parameters preset
    	public double forward(){

        	double interval = (this.upperLimit - this.lowerLimit)/this.nIntervals;
        	double x = this.lowerLimit;
        	double y = this.integralFunc.function(x);
        	this.integralSum = 0.0D;

        	for(int i=0; i<this.nIntervals; i++){
        	    // adjust last interval for rounding errors
		            if(x>this.upperLimit){
		                x = this.upperLimit;
		                interval -= (x - this.upperLimit);
		            }

		            // perform summation
            		y = this.integralFunc.function(x);
            		this.integralSum += y*interval;
            		x += interval;
        	}
        	this.integrationDone = true;
        	return this.integralSum;
    	}

    	// Numerical integration using the forward rectangular rule
    	// all parameters except number of intervals preset
    	public double forward(int nIntervals){
    	    this.nIntervals = nIntervals;
    	    this.setIntervals = true;
            return this.forward();
        }

    	// Numerical integration using the forward rectangular rule (static method)
    	// all parameters provided
    	public static double forward(IntegralFunction integralFunc, double lowerLimit, double upperLimit, int nIntervals){
   	    Integration intgrtn = new Integration(integralFunc, lowerLimit, upperLimit);
    	    return intgrtn.forward(nIntervals);
         }

         public static double foreward(IntegralFunction integralFunc, double lowerLimit, double upperLimit, int nIntervals){
   	    Integration intgrtn = new Integration(integralFunc, lowerLimit, upperLimit);
    	    return intgrtn.forward(nIntervals);
         }


}
