/*-
 * #%L
 * Interactive 3D Surface Plot plugin for Fiji.
 * %%
 * Copyright (C) 2004 - 2021 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package plugins.volumetric.jrenderer3d;

import java.awt.Color;

/**
 * This class represents a text element in a 3D coordinate system.
 * It is described by the text string, its position (x, y, z), its color and its size.
 * 
 * @author Kai Uwe Barthel
 *
 */
public class Text3D {
	
	/**
	 * Creates a new Text3D object.
	 * 
	 * @param text - the text string
	 * @param x - x position of the text element
	 * @param y - y position of the text element
	 * @param z - z position of the text element
	 * @param color - color of the text element
	 * @param size - size the text element
	 */
	public Text3D(String text, double x, double y, double z, Color color, double size) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.color = color;

		this.size = size;
		
		this.number = 1;
	}
	
	public Text3D(String text, double x, double y, double z, Color color, double size, int number) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.color = color;

		this.size = size;
		
		this.number = number;
	}

	/**
	 * the text string
	 */
	public String text;
	
	/**
	 * the x position of the text element
	 */
	public double x;
	
	
	/**
	 * the y position of the text element
	 */
	public double y;
	
	
	/**
	 * the z position of the text element
	 */
	public double z;
		
	
	/**
	 * the color of the text element
	 */
	public Color color;

	/**
	 * the size of the text element
	 */
	public double size;
	
	public int number;
}
