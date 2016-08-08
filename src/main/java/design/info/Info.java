/*******************************************************************************
 * Copyright (C) 2016 Bob Rubbens
 *  
 *  
 * This file is part of e3tool.
 *  
 * e3tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * e3tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with e3tool.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package design.info;

public class Info {
//	public static long nextSUID = 0;
//	public static long getSUID() {
//		return nextSUID++;
//	}
	
	public static enum Side {
		TOP, RIGHT, BOTTOM, LEFT;
		public Side rotateRight() {
			if (this == TOP) {
				return RIGHT;
			} else if (this == RIGHT) {
				return BOTTOM;
			} else if (this == BOTTOM) {
				return LEFT;
			} else { // (this == LEFT)
				return TOP;
			}
		}
		public Side rotateLeft() {
			return this.rotateRight().rotateRight().rotateRight();
		}
	}
}
