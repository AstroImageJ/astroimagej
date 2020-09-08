package astroj;



public class ContourPoint implements Cloneable {
	/**X and Y coordinate of a point **/
	public float[] coords;
	
	//default constructor
	public ContourPoint()
	{
		coords = new float[2];
	}
	public ContourPoint(float x, float y)
	{
		coords = new float[2];
		coords[0]=x;
		coords[1]=y;
	}

	public ContourPoint clone()
	{
		
		ContourPoint pointret = new ContourPoint(coords[0],coords[1]);
		return pointret;
	}
	
	public float getX() {
		// TODO Auto-generated method stub
		return coords[0];
	}
	
	public float getY() {
		// TODO Auto-generated method stub
		return coords[1];
	}
	public float distance(float x, float y)
	{
		return (float) Math.sqrt(Math.pow(x-coords[0], 2)+Math.pow(y-coords[1], 2));
	}
}