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
class Misc {
		
		static String fm(int len, int val) {
			String s = "" + val;
			
			while (s.length() < len) {
				s = " " + s;
			}
			return s;
		}
		static String fm(int len, double val) {
			String s = "" + val;
			
			while (s.length() < len) {
				s = s + " ";
			}
			return s;
		}
		
		static boolean inside(int[] p, int[] p1, int[] p2, int[] p3) {
			int x  = p[0];
			int y  = p[1];
			int x1 = p1[0];
			int y1 = p1[1];
			int x2 = p2[0];
			int y2 = p2[1];
			int x3 = p3[0];
			int y3 = p3[1];
			
			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
			
			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}
		
		static boolean inside(int x, int y, int x1, int y1, int x2, int y2, int x3, int y3) {
			
			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
			
			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}
		
	}
