package com.sothree.slidinguppanel;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Helper class for determining the scroll capability for scrollable views.
 */
public interface ScrollableViewHelper {
    /**
     * If this method returns zero false or it means at the scrollable view is in a position such
     * as the panel should handle scrolling. If the method returns anything above zero,
     * then the panel will let the scrollable view handle the scrolling
     *
     * @param scrollableView the scrollable view
     * @param direction negative to check scrolling up, positive to check scrolling down.
     * @return the scroll position
     */
    public boolean canScrollVertically(View scrollableView, int direction);

    static final ScrollableViewHelper DEFAULT = new ScrollableViewHelper() {
        @Override
        public boolean canScrollVertically(View scrollableView, int direction) {
            return ViewCompat.canScrollVertically(scrollableView, direction);
        }
    };
}
