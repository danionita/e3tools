package design.main;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxRectangle;

public class Utils {
	public static boolean overlap(mxRectangle a, mxRectangle b) {
		if (a.getX() > b.getX()) {
			mxRectangle dummy = a;
			a = b;
			b = dummy;
		}
		
		if (a.getX() + a.getWidth() > b.getX()) {
			// Horizontal overlap
			if (a.getY() > b.getY()) {
				mxRectangle dummy = a;
				a = b;
				b = dummy;
			}
			
			if (a.getY() + a.getHeight() > b.getY()) {
				// And vertical overlap as well
				return true;
			}
		} else {
			return false;
		}
		
		return false;
	}
	
	/**
	 * Returns true if inner is within outer
	 * @param inner
	 * @param outer
	 * @return
	 */
	public static boolean within(mxRectangle inner, mxRectangle outer) {
		if (inner.getX() < outer.getX())
			return false;
		
		if (inner.getX() + inner.getWidth() > outer.getX() + outer.getWidth())
			return false;
		
		if (inner.getY() < outer.getY())
			return false;
		
		if (inner.getY() + inner.getHeight() > outer.getY() + outer.getHeight())
			return false;
		
		return true;
	}
	
	public static mxRectangle rect(mxGeometry gm) {
		return new mxRectangle(gm.getX(), gm.getY(), gm.getWidth(), gm.getHeight());
	}
}
