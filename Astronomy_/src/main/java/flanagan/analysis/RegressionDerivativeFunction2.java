/*
*   Interface RegressionDerivativeFunction2
*
*   The first and second derivatives of the regression function with respect to a
*   pair of parameters, for multiple y array option, needed by the statistical 
*   analysis methods called by the non-linear regression methods in the class 
*   Regression is supplied by means of this interface, RegressionDerivativeFunction
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:	    January 2012
*   MODIFIED:
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web page:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/Regression.html
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*
*   Copyright (c) 2008
*
*   PERMISSION TO COPY:
*
*   Redistributions of this source code, or parts of, must retain the above
*   copyright notice, this list of conditions and the following disclaimer.
*
*   Redistribution in binary form of all or parts of this class, must reproduce
*   the above copyright, this list of conditions and the following disclaimer in
*   the documentation and/or other materials provided with the distribution.
*
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all
*   copies and associated documentation or publications.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/


package flanagan.analysis;

// Interface for Regression class
// First and second derivative function for non-linear regression methods
// p = parameter values
// x = independent variable values
// i = index of parameter p[i] in df/dp[i] and d2f/dp[i]dp[j]
// j = index of parameter p[j] in df/dp[j] and d2f/dp[i]dp[j]
// k = index of the independent variable
// returns df/dp[i], df/dp[j] and d2f/dp[i]dp[j] as an array of double, f = the function of x and p

public interface RegressionDerivativeFunction2{
    double[] function(double[]p, double[] x,  int i, int j, int k);
}
