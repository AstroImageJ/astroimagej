/*
*   CLASS:      Smooth
*               REPLACED BY CurveSmooth on 5 March 2012
*
*   USAGE:      Class for smoothing a curve and obtaining the maxima and minima of a curve
*               Smoothing methods: moving average window or Savitzky-Golay filter
*      
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:       February 2012
*   AMENDED:    26-27 February 2012, 3-4 March 2012 
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

import java.math.BigDecimal;
import java.math.BigInteger;

import flanagan.math.ArrayMaths;

public class Smooth extends CurveSmooth{
    
        
    // CONSTRUCTORS
    
    // Constructor - data as 1D double, y = f(x)
    public Smooth(double[] x, double[] y){ 
        super(x,y);
    }
    
    // Constructor - data as 1D double - no x data, y = f(x)
    public Smooth(double[] y){ 
        super(y);
    }
    
    // Constructor - data as 1D float, y = f(x)
    public Smooth(float[] x, float[] y){ 
        super(x,y);
    }
    
    // Constructor - data as 1D float - no x data, y = f(x)
    public Smooth(float[] y){ 
        super(y);
    }
    
    
    // Constructor - data as long, y = f(x)
    public Smooth(long[] x, long[] y){ 
        super(x,y);
    }
     
    // Constructor - data as long - no x data, y = f(x)
    public Smooth(long[] y){ 
        super(y);
    }
    
    // Constructor - data as int, y = f(x)
    public Smooth(int[] x, int[] y){ 
        super(x,y);
    }
    
    // Constructor - data as int - no x data, y = f(x)
    public Smooth(int[] y){ 
        super(y);
    }
    
    // Constructor - data as 1D BigDecimal, y = f(x)
    public Smooth(BigDecimal[] x, BigDecimal[] y){ 
        super(x,y);
    } 
    
    // Constructor - data as 1D BigDecimal - no x data, y = f(x)
    public Smooth(BigDecimal[] y){ 
        super(y);
    }
    
    // Constructor - data as 1D BigInteger, y = f(x)
    public Smooth(BigInteger[] x, BigInteger[] y){ 
        super(x,y);
    }
    
    // Constructor - data as 1D BigInteger - no x data, y = f(x)
    public Smooth(BigInteger[] y){ 
        super(y);
    }
   
    // Constructor - data as ArrayMaths, y = f(x)
    public Smooth(ArrayMaths x, ArrayMaths y){ 
        super(x,y);
    }
       
    // Constructor - data as ArrayMaths - no x data, y = f(x)
    public Smooth(ArrayMaths y){ 
        super(y);
    }
    
}
