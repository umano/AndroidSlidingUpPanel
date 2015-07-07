package com.sothree.slidinguppanel.demo;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

/**
 * Created by Administrator on 2015/7/2.
 */
abstract public class ScrollToEndDetector implements AbsListView.OnScrollListener {
    private boolean topReached = true;/* Initial state of a listView*/
    private boolean bottomReached = false;

    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        _onScrollStateChanged(listView, scrollState);

        if (topReached)
            onReachTop();
        else if (bottomReached)
            onReachBottom();
        else
            onReachNone();

    }

    private void _onScrollStateChanged(AbsListView listView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            if (listView.getFirstVisiblePosition() == 0) {
                final View firstVisibleView = listView.getChildAt(0);
                if (firstVisibleView != null && isAllShown(listView, firstVisibleView)){
                    topReached = true;
                    bottomReached = false;
                    return;
                }
            } else if (listView.getLastVisiblePosition() == listView.getCount() - 1) {
                final View lastVisibleView = listView.getChildAt(listView.getChildCount() - 1);
                if (lastVisibleView != null && isAllShown(listView, lastVisibleView)) {
                    topReached = false;
                    bottomReached = true;
                    return;
                }
            }


            topReached = bottomReached = false;
            return;
        }
    }

    private boolean isAllShown(AbsListView listView, View itemView) {
        final Rect r = new Rect(0, 0, itemView.getWidth(), itemView.getHeight());
        final double height = itemView.getHeight () * 1.0;

        listView.getChildVisibleRect(itemView, r, null);
        Log.d("Visible1 ", "  " + height + "  " + r.height());
        if ( height == r.height())
            return true;

        return false;
    }

    @Override
    public void onScroll(AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {

    }

    abstract public void onReachTop();
    abstract public void onReachBottom();
    abstract public void onReachNone();
}
