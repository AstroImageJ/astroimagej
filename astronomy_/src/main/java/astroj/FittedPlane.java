// FittedPlane.java

package astroj;

import Jama.Jama.*;
import ij.*;

/**
 * Accepts data which can be used to define a plane in (x,y,z) space and fits the coefficients coefs[], where
 *
 *		z = coefs[0]+coefs[1]*x+coefs[2]*y
 */
public class FittedPlane
	{
	protected double[] xData,yData,zData;
	public double coefs[];
	public int npts;

	/**
	 * Instantiates a FittedPlane using the expected number of data points.
	 */
	public FittedPlane (int n)
		{
		coefs = null;
		xData = new double[n];
		yData = new double[n];
		zData = new double[n];
		npts = 0;
		}

	/**
	 * Instantiates a FittedPlane using input arrays of the (x,y,z) points.
	 */
	public FittedPlane (double[] x, double[] y, double[] z)
		{
		coefs = null;
		xData = null;
		yData = null;
		zData = null;
		if (x == null || y == null || z == null || x.length != y.length || x.length != z.length) return;
		xData = (double[]) x.clone();
		yData = (double[]) y.clone();
		zData = (double[]) z.clone();
		npts = x.length;
		}
			
	/**
	 * Adds a data point to the internal arrays which can be used later for fitting the plane coefficients.
	 */
	public void addPoint (double x, double y, double z)
		{
		if (npts < xData.length)
			{
			xData[npts] = x;
			yData[npts] = y;
			zData[npts] = z;
			npts++;
			}
		}

	/**
	 * Same as addPoint (double,double,double) but for a handy integer x and y format.
	 */
	public void addPoint (int x, int y, double z)
		{
		if (npts < xData.length)
			{
			xData[npts] = (double)x;
			yData[npts] = (double)y;
			zData[npts] = z;
			npts++;
			}
		}
    

	/**
	 * Fits the plane coefficients using least-squares.
	 */
	public boolean fitPlane ()
		{
		if (npts < 4) return false;

		double x,y,z;
		double sum=0.0;
		double sumx = 0.0;
		double sumy = 0.0;
		double sumxx = 0.0;
		double sumyy = 0.0;
		double sumxy = 0.0;
		double sumz = 0.0;
		double sumxz = 0.0;
		double sumyz = 0.0;
        
		for (int i=0; i < npts; i++)
			{
			x = xData[i];
			y = yData[i];
			z = zData[i];
			sum += 1.0;
			sumx += x;
			sumy += y;
			sumz += z;
			sumxx += x*x;
			sumyy += y*y;
			sumxy += x*y;
			sumxz += x*z;
			sumyz += y*z;
			}
        
        

		// PERFORM FIT

		Matrix m,b,c;
		double[][] mvals = {
						{sum,    sumx,   sumy},
						{sumx,  sumxx, sumxy},
						{sumy,  sumxy,  sumyy}
						};
		m = new Matrix(mvals);
		double[][] bvals =	{
						{sumz},
						{sumxz},
						{sumyz}
						};
		b = new Matrix(bvals);
		try	{
			LUDecomposition lu = new LUDecomposition(m);			
            c = lu.solve(b);
			}
		catch (RuntimeException e)
			{
			System.err.println("FittedPlane exception: "+e.getMessage());
			return false;
			}

		coefs =  c.getRowPackedCopy();
		return true;
		}

	/**
	 * Returns the value of the plane at a give (x,y) coordinate.
	 */
	public double valueAt (double x, double y)
		{
		if (coefs == null)
			return Double.NaN;
		else
			return coefs[0]+coefs[1]*x+coefs[2]*y;
		}
	public double valueAt (int x, int y)
		{
		if (coefs == null)
			return Double.NaN;
		else
			return coefs[0]+coefs[1]*x+coefs[2]*y;
		}
	}
