package Astronomy;// Coordinate_Convertor.java

import ij.plugin.*;
import astroj.*;

/**
 * This plugin converts sky coordinates in a variety of formats.
 * @author K.A. Collins, University of Louisville
 * @date 2011-12-27
 * @version 1.0
 * 
 */
public class Coordinate_Converter implements PlugIn
	{

    public void run(String arg)   //***** public AstroCC()
        {
        AstroConverter astroc = new AstroConverter(true);
        }
}
