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
class SurfacePlotData {
	
	double x;
	double y;
	double z;    
	double zf;   // luminance (filtered)
	double lum;  // luminance
	
	int color;
	
	boolean isVisible;
	
	// Normals
	double dx;
	double dy;
	double dz;

	double dx2;
	double dy2;
	double dz2;

}

