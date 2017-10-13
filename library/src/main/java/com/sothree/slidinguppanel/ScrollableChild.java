package com.sothree.slidinguppanel;

/**
 * Created by Shad on 21/12/2016.
 */

public interface ScrollableChild {
	boolean canScrollVertically(int x, int y, boolean up);
	
	boolean canBePinched();
}

