package com.sothree.slidinguppanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.sothree.slidinguppanel.library.R;

/**
 * Created by ThomasR on 06.01.2015.
 */
public class FloatingActionButtonLayout extends ViewGroup {
    SlidingUpPanelLayout mSlidingUpPanelLayout;
    View mFloatingActionButton;
    private boolean mFloatingActionButtonAttached = true;
    boolean mFirstLayout = true;

    public enum FabMode {
        LEAVE_BEHIND,
        CIRCULAR_REVEAL,
        FADE
    }

    private static FabMode DEFAULT_FAB_MODE = FabMode.LEAVE_BEHIND;
    private FabMode mFabMode = DEFAULT_FAB_MODE;

    public FloatingActionButtonLayout(Context context) {
        this(context, null);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null){
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButtonLayout);
            if (ta != null){
                mFabMode = FabMode.values()[ta.getInt(R.styleable.FloatingActionButtonLayout_umanoFabMode, DEFAULT_FAB_MODE.ordinal())];
            }
            ta.recycle();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childcount = getChildCount();

        // layout built for Sliding Up Panel Layout and Floating Action Button
        if (childcount != 2) {
            throw new IllegalStateException("FloatingActionButtonLayout must have exactly 2 children");
        }

        // retrieve and measure the views
        mSlidingUpPanelLayout = (SlidingUpPanelLayout) getChildAt(0);
        mSlidingUpPanelLayout.measure(widthMeasureSpec, heightMeasureSpec);
        mFloatingActionButton = getChildAt(1);
        measureChildWithMargins(mFloatingActionButton, widthMeasureSpec, 0, heightMeasureSpec, 0);

        // measure layout itself
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mSlidingUpPanelLayout.layout(l, t, r, b);

        if (mFirstLayout) {
            int expandedYSpace = getMeasuredHeight() - mSlidingUpPanelLayout.getChildAt(1).getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) mFloatingActionButton.getLayoutParams();
            SlidingUpPanelLayout.PanelState state = mSlidingUpPanelLayout.getPanelState();
            // First get Left and Right (independent of slide state)
            int fabRight = r - lp.rightMargin;
            int fabLeft = fabRight - mFloatingActionButton.getMeasuredWidth();
            // Then calculate Top and Bottom values
            int initialfabBottom = b - lp.bottomMargin;
            int initialfabTop = initialfabBottom - mFloatingActionButton.getMeasuredHeight();
            int collapsedfabBottom = b - mSlidingUpPanelLayout.getPanelHeight() + mFloatingActionButton.getMeasuredHeight() / 2;
            int collapsedfabTop = collapsedfabBottom - mFloatingActionButton.getMeasuredHeight();
            int expandedfabBottom = t + expandedYSpace + mSlidingUpPanelLayout.getPanelHeight() + mFloatingActionButton.getMeasuredHeight() / 2;
            int expandedfabTop = expandedfabBottom - mFloatingActionButton.getMeasuredHeight();
            int fabBottom = 0;
            int fabTop = 0;
            switch (state) {
                case HIDDEN:
                    fabBottom = initialfabBottom;
                    fabTop = initialfabTop;
                    break;
                case ANCHORED:
                    float anchor = mSlidingUpPanelLayout.getAnchorPoint();
                    if(anchor != 1.0f) {
                        fabBottom = t + Math.round((getMeasuredHeight() - mSlidingUpPanelLayout.getPanelHeight()) * (1f - anchor) + mFloatingActionButton.getMeasuredHeight() / 2);
                        fabTop = fabBottom - mFloatingActionButton.getMeasuredHeight();
                        break;
                    }
                case EXPANDED:
                    fabBottom = expandedfabBottom;
                    fabTop = expandedfabTop;
                    if (mFabMode != FabMode.LEAVE_BEHIND) {
                        mFloatingActionButton.setVisibility(View.INVISIBLE);
                    }
                    break;
                default:
                    fabBottom = collapsedfabBottom;
                    fabTop = collapsedfabTop;
                    break;
            }
            mFloatingActionButton.layout(fabLeft, fabTop, fabRight, fabBottom);
            mSlidingUpPanelLayout.setFloatingActionButtonVisibility(mFloatingActionButton.getVisibility());
            mSlidingUpPanelLayout.attachFloatingActionButton(mFloatingActionButton, initialfabTop, collapsedfabTop, expandedfabTop, expandedYSpace, mFabMode);
        }

        mFirstLayout = false;
    }

    /**
     * method to set the attachment state of the Floating Action Button
     *
     * @param attached boolean indicating desired behavior (true for attaching, false for detaching)
     */
    public void setFloatingActionButtonAttached(boolean attached) {
        if (mSlidingUpPanelLayout != null) {
            mFloatingActionButtonAttached = attached;
            mSlidingUpPanelLayout.setFloatingActionButtonAttached(attached);
        }
    }

    public boolean isFloatingActionButtonAttached() {
        return mFloatingActionButtonAttached;
    }

    /**
     * method to set the visibility of the Floating Action Button
     * (call this instead of the standard setVisibility() so the code can handle visibility while sliding)
     *
     * @param visibility integer with the desired visibility (must be one of either View.VISIBLE, View.INVISIBLE or View.GONE)
     */
    public void setFloatingActionButtonVisibility(int visibility) {
        if (mSlidingUpPanelLayout != null) {
            mSlidingUpPanelLayout.setFloatingActionButtonVisibility(visibility);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FloatingActionButtonLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }
    }
}