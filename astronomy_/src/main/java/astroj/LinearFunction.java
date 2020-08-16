// LinearFunction.java

package astroj;

/**
 * Interface to LinearLeastSquaresFunction.
 */
public interface LinearFunction
	{
	/**
	 * Returns linear model function value of the form:
	 * 	y = c[0]*f_0(x[0],x[1],..) + c[1]*f_1(x[0],x[1],...) ... + c[n-1]*f_(n-1)(x[0],x[1],...)
	 */
	public double model (int indx, double x, double[] c);

	/**
	 * Returns a matrix containing the model function
	 *	m[0][0] = wgt*f_0(x,..); m[0][1] = wgt*f_1(x,...); ... m[0][n-1] = *wgt*f_(n-1)(x,...);	m[0][n] = wgt*y*f_...
	 */
	public double[][] modelFunctions (int indx, double x, double y, double wgt);

	/**
	 * Returns whether the function is actually of the form y^2 = ....
	 */
	public boolean isReallyQuadratic();

	/**
	 * Returns the size of the model function array (e.g. =2 for a line).
	 */
	public int numberOfParameters();

	/**
	 * Returns a short but informative description of the model.
	 */
	public String modelName();
	}
