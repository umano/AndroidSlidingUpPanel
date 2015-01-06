package com.sothree.slidinguppanel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ThomasR on 06.01.2015.
 */
public class FloatingActionButtonLayout extends ViewGroup {
    SlidingUpPanelLayout mSlidingUpPanelLayout;
    View mFloatingActionButton;
    boolean mFirstLayout = true;

    public FloatingActionButtonLayout(Context context) {
        this(context, null);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mSlidingUpPanelLayout = (SlidingUpPanelLayout) getChildAt(0);
        mSlidingUpPanelLayout.measure(widthMeasureSpec, heightMeasureSpec);
        mFloatingActionButton = getChildAt(1);
        measureChildWithMargins(mFloatingActionButton, widthMeasureSpec, MeasureSpec.getSize(widthMeasureSpec), heightMeasureSpec, MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childcount = getChildCount();

        if (childcount != 2) {
            throw new IllegalStateException("FloatingActionButtonLayout must have exactly 2 children");
        }

        if (mFirstLayout) {
            mSlidingUpPanelLayout.layout(l, t, r, b);
            MarginLayoutParams lp = (MarginLayoutParams) mFloatingActionButton.getLayoutParams();
            SlidingUpPanelLayout.PanelState state = mSlidingUpPanelLayout.getPanelState();
            // First get Left and Right (independent of slide state)
            int fabRight = r -lp.rightMargin;
            int fabLeft = fabRight - mFloatingActionButton.getMeasuredWidth();
            // Then calculate Top and Bottom values
            int initialfabBottom = b - lp.bottomMargin;
            int initialfabTop = initialfabBottom - mFloatingActionButton.getMeasuredHeight();
            int collapsedfabBottom = b - mSlidingUpPanelLayout.getPanelHeight() + mFloatingActionButton.getMeasuredHeight() / 2;
            int collapsedfabTop = collapsedfabBottom - mFloatingActionButton.getMeasuredHeight();
            int expandedfabBottom = t + mSlidingUpPanelLayout.getPanelHeight() + mFloatingActionButton.getMeasuredHeight() / 2;
            int expandedfabTop = expandedfabBottom - mFloatingActionButton.getMeasuredHeight();
            int fabBottom = 0;
            int fabTop = 0;
            switch (state) {
                case HIDDEN:
                    fabBottom = initialfabBottom;
                    fabTop = initialfabTop;
                    break;
                case COLLAPSED:
                    fabBottom = collapsedfabBottom;
                    fabTop = collapsedfabTop;
                    break;
                case EXPANDED:
                    fabBottom = expandedfabBottom;
                    fabTop = expandedfabTop;
                    break;
            }
            mFloatingActionButton.layout(fabLeft, fabTop, fabRight, fabBottom);
            mSlidingUpPanelLayout.attachFloatingActionButton(mFloatingActionButton, initialfabTop, collapsedfabTop, expandedfabTop);
        }

        mFirstLayout = false;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

}