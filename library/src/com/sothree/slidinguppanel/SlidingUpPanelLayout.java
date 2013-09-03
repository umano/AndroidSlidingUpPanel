package com.sothree.slidinguppanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import java.util.Arrays;

public class SlidingUpPanelLayout extends ViewGroup {
    private static final String TAG = "SlidingPaneLayout";

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_HEIGHT = 68; // dp;

    /**
     * Default height of the shadow above the peeking out panel
     */
    private static final int DEFAULT_SHADOW_HEIGHT = 4; // dp;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    /**
     * The paint used to dim the main layout when sliding
     */
    private final Paint mCoveredFadePaint = new Paint();

    /**
     * Drawable used to draw the shadow between panes.
     */
    private Drawable mShadowDrawable;

    /**
     * The size of the overhang in pixels.
     */
    private int mPanelHeight;

    /**
     * The size of the shadow in pixels.
     */
    private final int mShadowHeight;

    /**
     * True if a panel can slide with the current measurements
     */
    private boolean mCanSlide;

    /**
     * Is the panel currently hidden.
     */
    private boolean mPanelHidden;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private  View mDragView;

    /**
     * The child view that can slide, if any.
     */
    private View mSlideableView;

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = expanded, 1 = hidden.
     *
     * Note this is different to the slide offset that is passed to
     * {@link PanelSlideListener#onPanelSlide(android.view.View, float)}
     */
    private float mSlideOffsetInternal;

    /**
     * The offset the panel when it is in its collapsed (but not hidden)
     * position.
     */
    private float mCollapsedOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    private int mSlideRange;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private float[] mLastMotionX;
    private float[] mLastMotionY;

    private PanelSlideListener mPanelSlideListener;

    private final ViewDragHelper mDragHelper;

    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mPreservedExpandedState;
    private boolean mFirstLayout = true;
    private Runnable mPostLayoutRunnable;

    private final Rect mTmpRect = new Rect();

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a sliding pane's position changes.
         * @param panel The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        public void onPanelSlide(View panel, float slideOffset);
        /**
         * Called when a sliding pane becomes slid completely collapsed. The pane may or may not
         * be interactive at this point depending on if it's shown or hidden
         * @param panel The child view that was slid to an collapsed position, revealing other panes
         */
        public void onPanelCollapsed(View panel);

        /**
         * Called when a sliding pane becomes slid completely expanded. The pane is now guaranteed
         * to be interactive. It may now obscure other views in the layout.
         * @param panel The child view that was slid to a expanded position
         */
        public void onPanelExpanded(View panel);
    }

    /**
     * No-op stubs for {@link PanelSlideListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
        }
        @Override
        public void onPanelCollapsed(View panel) {
        }
        @Override
        public void onPanelExpanded(View panel) {
        }
    }

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final float density = context.getResources().getDisplayMetrics().density;
        mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
        mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);

        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * density);

        mCanSlide = true;

        setCoveredFadeColor(DEFAULT_FADE_COLOR);
    }

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the expanded state.
     *
     * @param color An ARGB-packed color value
     */
    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        invalidate();
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     */
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * Set the collapsed panel height in pixels
     *
     * @param val A height in pixels
     */
    public void setPanelHeight(int val) {
        mPanelHeight = val;
    }

    /**
     * @return The current collapsed panel height
     */
    public int getPanelHeight() {
        return mPanelHeight;
    }

    public void setPanelSlideListener(PanelSlideListener listener) {
        mPanelSlideListener = listener;
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    public void setDragView(View dragView) {
        mDragView = dragView;
    }

    /**
     * Set the shadow for the sliding panel
     *
     */
    public void setShadowDrawable(Drawable drawable) {
        mShadowDrawable = drawable;
    }

    void dispatchOnPanelSlide(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelSlide(panel, getExternalSlideOffset());
        }
    }

    void dispatchOnPanelExpanded(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelExpanded(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelCollapsed(View panel) {
        if (mPanelSlideListener != null) {
            mPanelSlideListener.onPanelCollapsed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewVisibility() {
        if (getChildCount() == 0) {
            return;
        }
        final int leftBound = getPaddingLeft();
        final int rightBound = getWidth() - getPaddingRight();
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
            left = mSlideableView.getLeft();
            right = mSlideableView.getRight();
            top = mSlideableView.getTop();
            bottom = mSlideableView.getBottom();
        } else {
            left = right = top = bottom = 0;
        }
        View child = getChildAt(0);
        final int clampedChildLeft = Math.max(leftBound, child.getLeft());
        final int clampedChildTop = Math.max(topBound, child.getTop());
        final int clampedChildRight = Math.min(rightBound, child.getRight());
        final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
        final int vis;
        if (clampedChildLeft >= left && clampedChildTop >= top &&
                clampedChildRight <= right && clampedChildBottom <= bottom) {
            vis = INVISIBLE;
        } else {
            vis = VISIBLE;
        }
        child.setVisibility(vis);
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
        int panelHeight = mPanelHeight;

        final int childCount = getChildCount();

        if (childCount > 2) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.");
        } else if (mPanelHidden) {
            panelHeight = 0;
        }

        // We'll find the current one below.
        mSlideableView = null;
        mCanSlide = false;

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int height = layoutHeight;
            if (child.getVisibility() == GONE) {
                lp.dimWhenOffset = false;
                continue;
            }

            if (i == 1) {
                lp.slideable = true;
                lp.dimWhenOffset = true;
                mSlideableView = child;
                mCanSlide = true;
            } else {
                height -= panelHeight;
            }

            int childWidthSpec;
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);

            if (i == 1) {
                int measuredHeight = child.getMeasuredHeight();
                mSlideRange = measuredHeight - mPanelHeight;
                mCollapsedOffset = 1.f - (float) mPanelHeight / measuredHeight;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();
        int yStart = paddingTop;
        int nextYStart = yStart;

        if (mFirstLayout) {
            mSlideOffsetInternal = mPanelHidden ? 1.f : mCanSlide && mPreservedExpandedState ? 0.f : mCollapsedOffset;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childHeight = child.getMeasuredHeight();

            if (lp.slideable) {
                yStart += (int) (childHeight * mSlideOffsetInternal);
            } else {
                yStart = nextYStart;
            }

            final int childTop = yStart;
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft;
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft, childTop, childRight, childBottom);

            nextYStart += child.getHeight();
        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }

        mFirstLayout = false;

        if (mPostLayoutRunnable != null) {
            post(mPostLayoutRunnable);
            mPostLayoutRunnable = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    private boolean isDragViewHit(int x, int y) {
        View v = mDragView != null ? mDragView : mSlideableView;
        if (v == null) return false;
        int[] viewLocation = new int[2];
        v.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + v.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + v.getHeight();
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (!isInTouchMode() && !mCanSlide) {
            mPreservedExpandedState = child == mSlideableView;
        }
    }

    private void ensureMotionHistorySize(int pointerId) {
        if (mLastMotionX == null || mLastMotionX.length <= pointerId) {
            float[] lmx = new float[pointerId + 1];
            float[] lmy = new float[pointerId + 1];

            if (mLastMotionX != null) {
                System.arraycopy(mLastMotionX, 0, lmx, 0, mLastMotionX.length);
                System.arraycopy(mLastMotionY, 0, lmy, 0, mLastMotionY.length);
            }

            mLastMotionX = lmx;
            mLastMotionY = lmy;
        }
    }

    private void saveInitialMotion(int pointerId, float x, float y) {
        ensureMotionHistorySize(pointerId);
        mLastMotionX[pointerId] = x;
        mLastMotionY[pointerId] = y;
    }

    private void saveMotion(MotionEvent ev) {
        final int pointerCount = MotionEventCompat.getPointerCount(ev);
        for (int i = 0; i < pointerCount; i++) {
            final int pointerId = MotionEventCompat.getPointerId(ev, i);
            final float x = MotionEventCompat.getX(ev, i);
            final float y = MotionEventCompat.getY(ev, i);
            mLastMotionX[pointerId] = x;
            mLastMotionY[pointerId] = y;
        }
    }

    private void clearMotionHistory(int pointerId) {
        mLastMotionX[pointerId] = 0;
        mLastMotionY[pointerId] = 0;
    }

    private void clearMotionHistory() {
        Arrays.fill(mLastMotionX, 0);
        Arrays.fill(mLastMotionY, 0);
    }

    /**
     * Save the current positions of all the pointers.
     *
     * Since there is no way to get current pointer positions from ViewDragHelper,
     * we save them ourselves here.
     */
    private void processTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        final int actionIndex = MotionEventCompat.getActionIndex(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerId = MotionEventCompat.getPointerId(ev, actionIndex);
                saveInitialMotion(pointerId, MotionEventCompat.getX(ev, actionIndex),
                        MotionEventCompat.getY(ev, actionIndex));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                saveMotion(ev);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerId = MotionEventCompat.getPointerId(ev, actionIndex);
                clearMotionHistory(pointerId);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                clearMotionHistory();
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mCanSlide) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        processTouchEvent(ev);
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mCanSlide) {
            return super.onTouchEvent(ev);
        }

        processTouchEvent(ev);
        mDragHelper.processTouchEvent(ev);

        final int action = ev.getAction();
        boolean wantTouchEvents = true;

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                final float dx = x - mInitialMotionX;
                final float dy = y - mInitialMotionY;
                final int slop = mDragHelper.getTouchSlop();
                if (dx * dx + dy * dy < slop * slop &&
                        isDragViewHit((int) x, (int) y)) {
                    View v = mDragView != null ? mDragView : mSlideableView;
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    if (!isExpanded()) {
                        expandPane(mSlideableView, 0);
                    } else {
                        collapsePane();
                    }
                    break;
                }
                break;
            }
        }

        return wantTouchEvents;
    }

    private boolean expandPane(View pane, int initialVelocity) {
        if (!mPanelHidden && (mFirstLayout || smoothSlideTo(0.f, initialVelocity))) {
            mPreservedExpandedState = true;
            return true;
        }
        return false;
    }

    private boolean collapsePane(View pane, int initialVelocity) {
        if (!mPanelHidden && (mFirstLayout || smoothSlideTo(mCollapsedOffset, initialVelocity))) {
            mPreservedExpandedState = false;
            return true;
        }
        return false;
    }

    /**
     * Collapse the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now collapsed/in the process of collapsing
     */
    public boolean collapsePane() {
        return collapsePane(mSlideableView, 0);
    }

    /**
     * Expand the sliding pane if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now expanded/in the process of expanding
     */
    public boolean expandPane() {
        if (!isPaneVisible()) {
            showPane(true);
            return true;
        } else {
            return expandPane(mSlideableView, 0);
        }
    }

    /**
     * Check if the layout is completely expanded.
     *
     * @return true if sliding panels are completely expanded
     */
    public boolean isExpanded() {
        return mFirstLayout && mPreservedExpandedState
                || !mFirstLayout && mCanSlide && mSlideOffsetInternal == 0;
    }

    /**
     * Check if the content in this layout cannot fully fit side by side and therefore
     * the content pane can be slid back and forth.
     *
     * @return true if content in this layout can be expanded
     */
    public boolean isSlideable() {
        return mCanSlide;
    }

    public boolean isPaneVisible() {
        if (getChildCount() < 2) {
            return false;
        }
        View slidingPane = getChildAt(1);
        return !mPanelHidden && slidingPane.getVisibility() == View.VISIBLE;
    }

    public void showPane() {
        showPane(false);
    }

    private void showPane(final boolean expanded) {
        if (!mPanelHidden) {
            return;
        }

        mPanelHidden = false;
        if (!mFirstLayout) {
            runAfterLayout(new Runnable() {
                @Override
                public void run() {
                    smoothSlideTo(expanded ? 0.f : mCollapsedOffset, 0);
                }
            });
        }
    }

    public void hidePane() {
        if (mPanelHidden) {
            return;
        }

        mPanelHidden = true;
        if (!mFirstLayout && mSlideableView != null) {
            runAfterLayout(new Runnable() {
                @Override
                public void run() {
                    smoothSlideTo(1.f, 0);
                }
            });
        }
    }

    private void runAfterLayout(Runnable runnable) {
        mPostLayoutRunnable = runnable;
        requestLayout();
    }

    private void onPanelDragged(int newTop) {
        final int topBound = getPaddingTop();
        mSlideOffsetInternal = (float) (newTop - topBound) / mSlideableView.getHeight();
        dispatchOnPanelSlide(mSlideableView);
    }

    private float getExternalSlideOffset() {
        // We could use mSlideOffsetInternal / mCollapsedOffset but
        // rounding errors tend to show up, so compute it based on the pixels instead.
        return Math.min((float) (mSlideableView.getTop() - getPaddingTop()) / mSlideRange, 1.f);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        boolean result;
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

        boolean drawScrim = false;

        if (mCanSlide && !mPanelHidden && !lp.slideable && mSlideableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(mTmpRect);
            mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
            canvas.clipRect(mTmpRect);
            if (mSlideOffsetInternal < 1) {
                drawScrim = true;
            }
        }

        result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);

        if (drawScrim) {
            final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * (1 - getExternalSlideOffset()));
            final int color = imag << 24 | (mCoveredFadeColor & 0xfffffff);
            mCoveredFadePaint.setColor(color);
            canvas.drawRect(mTmpRect, mCoveredFadePaint);
        }

        return result;
    }


    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!mCanSlide) {
            // Nothing to do.
            return false;
        }

        final int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mSlideableView.getHeight());

        if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), y)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            if (!mCanSlide) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        if (mSlideableView == null) {
            // No need to draw a shadow if we don't have one.
            return;
        }

        final int right = mSlideableView.getRight();
        final int top = mSlideableView.getTop() - mShadowHeight;
        final int bottom = mSlideableView.getTop();
        final int left = mSlideableView.getLeft();

        if (mShadowDrawable != null) {
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
                        y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
                        canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }


    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.isExpanded = isSlideable() ? isExpanded() : mPreservedExpandedState;
        ss.isHidden = mPanelHidden;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.isExpanded) {
            expandPane();
        } else {
            collapsePane();
        }
        mPreservedExpandedState = ss.isExpanded;
        mPanelHidden = ss.isHidden;
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return ((LayoutParams) child.getLayoutParams()).slideable &&
                    mDragView == null || isDragViewHit((int) mLastMotionX[pointerId], (int) mLastMotionY[pointerId]);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                if (mSlideOffsetInternal == 0) {
                    updateObscuredViewVisibility();
                    dispatchOnPanelExpanded(mSlideableView);
                    mPreservedExpandedState = true;
                } else {
                    dispatchOnPanelCollapsed(mSlideableView);
                    mPreservedExpandedState = false;
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            // Make all child views visible in preparation for sliding things around
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(top);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            if (yvel > 0 || (yvel == 0 && mSlideOffsetInternal > 0.5f)) {
                top += mSlideRange;
            }
            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = topBound + mSlideRange;

            final int newLeft = Math.min(Math.max(top, topBound), bottomBound);

            return newLeft;
        }

    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[] {
            android.R.attr.layout_weight
        };

        /**
         * True if this pane is the slideable pane in the layout.
         */
        boolean slideable;

        /**
         * True if this view should be drawn dimmed
         * when it's been offset from its default position.
         */
        boolean dimWhenOffset;

        Paint dimPaint;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            a.recycle();
        }

    }

    static class SavedState extends BaseSavedState {
        boolean isExpanded;
        boolean isHidden;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isExpanded = in.readInt() != 0;
            isHidden = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isExpanded ? 1 : 0);
            out.writeInt(isHidden ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

