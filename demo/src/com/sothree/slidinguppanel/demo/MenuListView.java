package com.sothree.slidinguppanel.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 * Created by xiangzhc on 07/07/15.
 */
public class MenuListView extends ListView implements SlidingUpPanelLayout.TouchInterceptNegotiator  {
    private boolean bottomReached;
    private boolean scrollDisabled;

    public MenuListView(Context context) {
        this(context, null);
    }

    public MenuListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MenuListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate();
    }

    public void disableScroll(boolean flag) {
        scrollDisabled = flag;
    }

    private void onCreate() {
        this.setOnScrollListener(new ScrollToEndDetector() {
            @Override
            public void onReachTop() {
                bottomReached = false;
            }

            @Override
            public void onReachBottom() {
                bottomReached = true;
            }

            @Override
            public void onReachNone() {
                bottomReached = false;
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        Log.v(getClass().getSimpleName(), "bottomReached =" + bottomReached + ", onTouchEvent(ev = " + ev + " )");

        if (scrollDisabled) {
            Log.v(getClass().getSimpleName(), "scrollDisabled =" + scrollDisabled);
            return false;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean allowed(float mInitialMotionX, float mInitialMotionY, int dragSlop, MotionEvent ev) {
        if (scrollDisabled)
            return true;

        if (bottomReached && mInitialMotionY - ev.getY() > dragSlop)
            return true;
        else
            return false;
    }

}
