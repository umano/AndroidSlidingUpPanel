package com.sothree.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.sothree.slidinguppanel.library.R;

import java.util.ArrayList;
import java.util.List;

public class SlidingUpPanelLayout extends ViewGroup {

    private static final String TAG = SlidingUpPanelLayout.class.getSimpleName();

    /**
     * Default peeking out panel height
     */
    private static final int DEFAULT_PANEL_SIZE = 68; // dp;

    /**
     * Default anchor point height
     */
    private static final float DEFAULT_ANCHOR_POINT = 1.0f; // In relative %

    /**
     * Default gravity
     */
    private static final int DEFAULT_GRAVITY = Gravity.BOTTOM;

    /**
     * Default initial state for the component
     */
    private static PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;


    /**
     * Default height of the shadow above the peeking out panel
     */
    private static final int DEFAULT_SHADOW_SIZE = 4; // dp;

    /**
     * If no fade color is given by default it will fade to 80% gray.
     */
    private static final int DEFAULT_FADE_COLOR = 0x99000000;

    /**
     * Default Minimum velocity that will be detected as a fling
     */
    private static final int DEFAULT_MIN_FLING_VELOCITY = 400; // dips per second
    /**
     * Default is set to false because that is how it was written
     */
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    /**
     * Default is set to true for clip panel for performance reasons
     */
    private static final boolean DEFAULT_CLIP_PANEL_FLAG = true;
    /**
     * Default attributes for layout
     */
    private static final int[] DEFAULT_ATTRS = new int[]{
            android.R.attr.gravity
    };
    /**
     * Tag for the sliding state stored inside the bundle
     */
    public static final String SLIDING_STATE = "sliding_state";

    /**
     * Minimum velocity that will be detected as a fling
     */
    private int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;

    /**
     * The fade color used for the panel covered by the slider. 0 = no fading.
     */
    private int mCoveredFadeColor = DEFAULT_FADE_COLOR;

    /**
     * Default parallax length of the main view
     */
    private static final int DEFAULT_PARALLAX_OFFSET = 0;

    /**
     * The paint used to dim the main layout when sliding
     */
    private final Paint mCoveredFadePaint = new Paint();

    /**
     * Drawable used to draw the shadow between panes.
     */
    private final Drawable mShadowDrawable;

    /**
     * The size of the overhang in pixels.
     */
    private int mPaneSize = -1;

    /**
     * The size of the shadow in pixels.
     */
    private int mShadowHeight = -1;

    /**
     * Parallax offset
     */
    private int mParallaxOffset = -1;

    private boolean mIsSlidingVertically = true;

    /**
     * Panel overlays the windows instead of putting it underneath it.
     */
    private boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;

    /**
     * The main view is clipped to the main top border
     */
    private boolean mClipPanel = DEFAULT_CLIP_PANEL_FLAG;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private View mDragView;

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private int mDragViewResId = -1;

    /**
     * If provided, the panel will transfer the scroll from this view to itself when needed.
     */
    private View mScrollableView;
    private int mScrollableViewResId;
    private ScrollableViewHelper mScrollableViewHelper = new ScrollableViewHelper();

    /**
     * The child view that can slide, if any.
     */
    private View mSlideableView;

    /**
     * The main view
     */
    private View mMainView;

    /**
     * Current state of the slideable view.
     */
    public enum PanelState {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        HIDDEN,
        DRAGGING
    }

    private PanelState mSlideState = DEFAULT_SLIDE_STATE;

    /**
     *
     */
    private int mGravity = DEFAULT_GRAVITY;

    /**
     * If the current slide state is DRAGGING, this will store the last non dragging state
     */
    private PanelState mLastNotDraggingSlideState = DEFAULT_SLIDE_STATE;

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private float mSlideOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    private int mSlideRange;

    /**
     * An anchor point where the panel can stop during sliding
     */
    private float mAnchorPoint = 1.f;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    private boolean mIsUnableToDrag;

    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    private boolean mIsTouchEnabled;

    private float mPrevMotionLocation;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private boolean mIsScrollableViewHandlingTouch = false;

    private List<PanelSlideListener> mPanelSlideListeners = new ArrayList<>();
    private View.OnClickListener mFadeOnClickListener;

    private final ViewDragHelper mDragHelper;

    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a sliding pane's position changes.
         *
         * @param panel       The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        public void onPanelSlide(View panel, float slideOffset);

        /**
         * Called when a sliding panel state changes
         *
         * @param panel The child view that was slid to an collapsed position
         */
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState);
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
        public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
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

        if (isInEditMode()) {
            mShadowDrawable = null;
            mDragHelper = null;
            return;
        }

        Interpolator scrollerInterpolator = null;
        if (attrs != null) {
            TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

            if (defAttrs != null) {
                int gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);
                setGravity(gravity);
            }

            defAttrs.recycle();

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout);

            if (ta != null) {
                mPaneSize = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoPanelHeight, -1);
                mShadowHeight = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoShadowHeight, -1);
                mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_umanoParallaxOffset, -1);

                mMinFlingVelocity = ta.getInt(R.styleable.SlidingUpPanelLayout_umanoFlingVelocity, DEFAULT_MIN_FLING_VELOCITY);
                mCoveredFadeColor = ta.getColor(R.styleable.SlidingUpPanelLayout_umanoFadeColor, DEFAULT_FADE_COLOR);

                mDragViewResId = ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoDragView, -1);
                mScrollableViewResId = ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoScrollableView, -1);

                mOverlayContent = ta.getBoolean(R.styleable.SlidingUpPanelLayout_umanoOverlay, DEFAULT_OVERLAY_FLAG);
                mClipPanel = ta.getBoolean(R.styleable.SlidingUpPanelLayout_umanoClipPanel, DEFAULT_CLIP_PANEL_FLAG);

                mAnchorPoint = ta.getFloat(R.styleable.SlidingUpPanelLayout_umanoAnchorPoint, DEFAULT_ANCHOR_POINT);

                mSlideState = PanelState.values()[ta.getInt(R.styleable.SlidingUpPanelLayout_umanoInitialState, DEFAULT_SLIDE_STATE.ordinal())];

                int interpolatorResId = ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoScrollInterpolator, -1);
                if (interpolatorResId != -1) {
                    scrollerInterpolator = AnimationUtils.loadInterpolator(context, interpolatorResId);
                }
            }

            ta.recycle();
        }

        final float density = context.getResources().getDisplayMetrics().density;
        if (mPaneSize == -1) {
            mPaneSize = (int) (DEFAULT_PANEL_SIZE * density + 0.5f);
        }
        if (mShadowHeight == -1) {
            mShadowHeight = (int) (DEFAULT_SHADOW_SIZE * density + 0.5f);
        }
        if (mParallaxOffset == -1) {
            mParallaxOffset = (int) (DEFAULT_PARALLAX_OFFSET * density);
        }
        // If the shadow height is zero, don't show the shadow
        if (mShadowHeight > 0) {
            switch (mGravity) {
                case Gravity.BOTTOM:
                    mShadowDrawable = getResources().getDrawable(R.drawable.above_shadow);
                    break;
                case Gravity.TOP:
                    mShadowDrawable = getResources().getDrawable(R.drawable.below_shadow);
                    break;
                case Gravity.RIGHT:
                    mShadowDrawable = getResources().getDrawable(R.drawable.left_shadow);
                    break;
                case Gravity.LEFT:
                    mShadowDrawable = getResources().getDrawable(R.drawable.right_shadow);
                    break;
                default:
                    mShadowDrawable = null;
            }
        } else {
            mShadowDrawable = null;
        }

        setWillNotDraw(false);

        mDragHelper = ViewDragHelper.create(this, 0.5f, scrollerInterpolator, new DragHelperCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity * density);

        mIsTouchEnabled = true;
    }

    /**
     * Set the Drag View after the view is inflated
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            setDragView(findViewById(mDragViewResId));
        }
        if (mScrollableViewResId != -1) {
            setScrollableView(findViewById(mScrollableViewResId));
        }
    }

    public void setGravity(int gravity) {
        if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM
                && gravity != Gravity.LEFT && gravity != Gravity.RIGHT) {
            throw new IllegalArgumentException("gravity must be set to either top, bottom, left or right");
        }

        mGravity = gravity;
        switch (gravity) {
            case Gravity.BOTTOM: case Gravity.TOP:
                mIsSlidingVertically = true;
                break;
            case Gravity.LEFT: case Gravity.RIGHT:
                mIsSlidingVertically = false;
                break;
        }
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the expanded state.
     *
     * @param color An ARGB-packed color value
     */
    public void setCoveredFadeColor(int color) {
        mCoveredFadeColor = color;
        requestLayout();
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     */
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * Set sliding enabled flag
     *
     * @param enabled flag value
     */
    public void setTouchEnabled(boolean enabled) {
        mIsTouchEnabled = enabled;
    }

    public boolean isTouchEnabled() {
        return mIsTouchEnabled && mSlideableView != null && mSlideState != PanelState.HIDDEN;
    }

    /**
     * Set the collapsed panel height in pixels
     *
     * @param val A height in pixels
     */
    public void setPanelHeight(int val) {
        if (getPanelHeight() == val) {
            return;
        }

        mPaneSize = val;
        if (!mFirstLayout) {
            requestLayout();
        }

        if (getPanelState() == PanelState.COLLAPSED) {
            smoothToBottom();
            invalidate();
            return;
        }
    }

    protected void smoothToBottom() {
        smoothSlideTo(0, 0);
    }

    /**
     * @return The current shadow height
     */
    public int getShadowHeight() {
        return mShadowHeight;
    }

    /**
     * Set the shadow height
     *
     * @param val A height in pixels
     */
    public void setShadowHeight(int val) {
        mShadowHeight = val;
        if (!mFirstLayout) {
            invalidate();
        }
    }

    /**
     * @return The current collapsed panel height
     */
    public int getPanelHeight() {
        return mPaneSize;
    }

    /**
     * @return The current parallax offset
     */
    public int getCurrentParallaxOffset() {
        // Clamp slide offset at zero for parallax computation;
        int offset = (int) (mParallaxOffset * Math.max(mSlideOffset, 0));

        return mGravity == Gravity.BOTTOM || mGravity == Gravity.RIGHT ? -offset : offset;
    }

    /**
     * Set parallax offset for the panel
     *
     * @param val A height in pixels
     */
    public void setParallaxOffset(int val) {
        mParallaxOffset = val;
        if (!mFirstLayout) {
            requestLayout();
        }
    }

    /**
     * @return The current minimin fling velocity
     */
    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    /**
     * Sets the minimum fling velocity for the panel
     *
     * @param val the new value
     */
    public void setMinFlingVelocity(int val) {
        mMinFlingVelocity = val;
    }

    /**
     * Adds a panel slide listener
     *
     * @param listener
     */
    public void addPanelSlideListener(PanelSlideListener listener) {
        synchronized (mPanelSlideListeners) {
            mPanelSlideListeners.add(listener);
        }
    }

    /**
     * Removes a panel slide listener
     *
     * @param listener
     */
    public void removePanelSlideListener(PanelSlideListener listener) {
        synchronized (mPanelSlideListeners) {
            mPanelSlideListeners.remove(listener);
        }
    }

    /**
     * Provides an on click for the portion of the main view that is dimmed. The listener is not
     * triggered if the panel is in a collapsed or a hidden position. If the on click listener is
     * not provided, the clicks on the dimmed area are passed through to the main layout.
     * @param listener
     */
    public void setFadeOnClickListener(View.OnClickListener listener) {
        mFadeOnClickListener = listener;
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    public void setDragView(View dragView) {
        if (mDragView != null) {
            mDragView.setOnClickListener(null);
        }
        mDragView = dragView;
        if (mDragView != null) {
            mDragView.setClickable(true);
            mDragView.setFocusable(false);
            mDragView.setFocusableInTouchMode(false);
            mDragView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isEnabled() || !isTouchEnabled()) return;
                    if (mSlideState != PanelState.EXPANDED && mSlideState != PanelState.ANCHORED) {
                        if (mAnchorPoint < 1.0f) {
                            setPanelState(PanelState.ANCHORED);
                        } else {
                            setPanelState(PanelState.EXPANDED);
                        }
                    } else {
                        setPanelState(PanelState.COLLAPSED);
                    }
                }
            });
            ;
        }
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragViewResId The resource ID of the new drag view
     */
    public void setDragView(int dragViewResId) {
        mDragViewResId = dragViewResId;
        setDragView(findViewById(dragViewResId));
    }

    /**
     * Set the scrollable child of the sliding layout. If set, scrolling will be transfered between
     * the panel and the view when necessary
     *
     * @param scrollableView The scrollable view
     */
    public void setScrollableView(View scrollableView) {
        mScrollableView = scrollableView;
    }

    /**
     * Sets the current scrollable view helper. See ScrollableViewHelper description for details.
     * @param helper
     */
    public void setScrollableViewHelper(ScrollableViewHelper helper) {
        mScrollableViewHelper = helper;
    }

    /**
     * Set an anchor point where the panel can stop during sliding
     *
     * @param anchorPoint A value between 0 and 1, determining the position of the anchor point
     *                    starting from the top of the layout.
     */
    public void setAnchorPoint(float anchorPoint) {
        if (anchorPoint > 0 && anchorPoint <= 1) {
            mAnchorPoint = anchorPoint;
            mFirstLayout = true;
            requestLayout();
        }
    }

    /**
     * Gets the currently set anchor point
     *
     * @return the currently set anchor point
     */
    public float getAnchorPoint() {
        return mAnchorPoint;
    }

    /**
     * Sets whether or not the panel overlays the content
     *
     * @param overlayed
     */
    public void setOverlayed(boolean overlayed) {
        mOverlayContent = overlayed;
    }

    /**
     * Check if the panel is set as an overlay.
     */
    public boolean isOverlayed() {
        return mOverlayContent;
    }

    /**
     * Sets whether or not the main content is clipped to the top of the panel
     *
     * @param clip
     */
    public void setClipPanel(boolean clip) {
        mClipPanel = clip;
    }

    /**
     * Check whether or not the main content is clipped to the top of the panel
     */
    public boolean isClipPanel() {
        return mClipPanel;
    }


    void dispatchOnPanelSlide(View panel) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener l : mPanelSlideListeners) {
                l.onPanelSlide(panel, mSlideOffset);
            }
        }
    }


    void dispatchOnPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
        synchronized (mPanelSlideListeners) {
            for (PanelSlideListener l : mPanelSlideListeners) {
                l.onPanelStateChanged(panel, previousState, newState);
            }
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
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
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

        if (widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        } else if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }

        final int childCount = getChildCount();

        if (childCount != 2) {
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
        }

        mMainView = getChildAt(0);
        mSlideableView = getChildAt(1);
        if (mDragView == null) {
            setDragView(mSlideableView);
        }

        // If the sliding panel is not visible, then put the whole view in the hidden state
        if (mSlideableView.getVisibility() != VISIBLE) {
            mSlideState = PanelState.HIDDEN;
        }

        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();

        // First pass. Measure based on child LayoutParams width/height.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // We always measure the sliding panel in order to know it's height (needed for show panel)
            if (child.getVisibility() == GONE && i == 0) {
                continue;
            }

            int height = layoutHeight;
            int width = layoutWidth;
            if (child == mMainView) {
                if (!mOverlayContent && mSlideState != PanelState.HIDDEN) {
                    if(mIsSlidingVertically){
                        height -= mPaneSize;
                    } else {
                        width -= mPaneSize;
                    }
                }

                if (mIsSlidingVertically){
                    width -= lp.leftMargin + lp.rightMargin;
                } else {
                    height -= lp.topMargin + lp.bottomMargin;
                }
            } else if (child == mSlideableView) {
                // The slideable view should be aware of its top margin.
                // See https://github.com/umano/AndroidSlidingUpPanel/issues/412.
                if(mIsSlidingVertically){
                    height -= lp.topMargin;
                } else {
                    width -= lp.leftMargin;
                }
            }

            int childWidthSpec;
            int childHeightSpec;

            if(mIsSlidingVertically){
                if (lp.width == LayoutParams.WRAP_CONTENT) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
                } else if (lp.width == LayoutParams.MATCH_PARENT) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                }

                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
                } else {
                    // Modify the height based on the weight.
                    if (lp.weight > 0 && lp.weight < 1) {
                        height = (int) (height * lp.weight);
                    } else if (lp.height != LayoutParams.MATCH_PARENT) {
                        height = lp.height;
                    }
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                }
            } else { // Horizontally
                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
                } else if (lp.height == LayoutParams.MATCH_PARENT) {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                }

                if (lp.width == LayoutParams.WRAP_CONTENT) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
                } else {
                    // Modify the width based on the weight.
                    if (lp.weight > 0 && lp.weight < 1) {
                        width = (int) (width * lp.weight);
                    } else if (lp.width != LayoutParams.MATCH_PARENT) {
                        width = lp.width;
                    }
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                }
            }


            child.measure(childWidthSpec, childHeightSpec);

            if (child == mSlideableView) {
                if(mIsSlidingVertically){
                    mSlideRange = mSlideableView.getMeasuredHeight() - mPaneSize;
                } else {
                    mSlideRange = mSlideableView.getMeasuredWidth() - mPaneSize;
                }
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();

        if (mFirstLayout) {
            switch (mSlideState) {
                case EXPANDED:
                    mSlideOffset = 1.0f;
                    break;
                case ANCHORED:
                    mSlideOffset = mAnchorPoint;
                    break;
                case HIDDEN:
                    if(mIsSlidingVertically){
                        int newTop = computePanelTopPosition(0.0f) + (mGravity == Gravity.BOTTOM ? +mPaneSize : -mPaneSize);
                        mSlideOffset = computeSlideOffset(newTop);
                    } else {
                        int newLeft = computePanelLeftPosition(0.0f) + (mGravity == Gravity.RIGHT ? +mPaneSize : -mPaneSize);
                        mSlideOffset = computeSlideOffset(newLeft);
                    }

                    break;
                default:
                    mSlideOffset = 0.f;
                    break;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            // Always layout the sliding view on the first layout
            if (child.getVisibility() == GONE && (i == 0 || mFirstLayout)) {
                continue;
            }

            if (mIsSlidingVertically) {

                int childTop = paddingTop;
                if (child == mSlideableView) {
                    childTop = computePanelTopPosition(mSlideOffset);
                }

                if (mGravity == Gravity.TOP) {
                    if (child == mMainView && !mOverlayContent) {
                        childTop = computePanelTopPosition(mSlideOffset) + mSlideableView.getMeasuredHeight();
                    }
                }

                final int childHeight = child.getMeasuredHeight();
                final int childBottom = childTop + childHeight;
                final int childLeft = paddingLeft + lp.leftMargin;
                final int childRight = childLeft + child.getMeasuredWidth();

                child.layout(childLeft, childTop, childRight, childBottom);
            } else {
                int childLeft = paddingLeft;

                if (child == mSlideableView) {
                    childLeft = computePanelLeftPosition(mSlideOffset);
                }

                if (mGravity == Gravity.LEFT) {
                    if (child == mMainView && !mOverlayContent) {
                        childLeft = computePanelLeftPosition(mSlideOffset) + mSlideableView.getMeasuredWidth();
                    }
                }

                final int childWidth = child.getMeasuredWidth();
                final int childRight = childLeft + childWidth;
                final int childTop = paddingTop + lp.topMargin;
                final int childBottom = childTop + child.getMeasuredHeight();

                child.layout(childLeft, childTop, childRight, childBottom);
            }

        }

        if (mFirstLayout) {
            updateObscuredViewVisibility();
        }
        applyParallaxForCurrentSlideOffset();

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (h != oldh) {
            mFirstLayout = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If the scrollable view is handling touch, never intercept
        if (mIsScrollableViewHandlingTouch || !isTouchEnabled()) {
            mDragHelper.abort();
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int dragSlop = mDragHelper.getTouchSlop();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (!isViewUnder(mDragView, (int) x, (int) y)) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (ady > dragSlop && adx > ady) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If the dragView is still dragging when we get here, we need to call processTouchEvent
                // so that the view is settled
                // Added to make scrollable views work (tokudu)
                if (mDragHelper.isDragging()) {
                    mDragHelper.processTouchEvent(ev);
                    return true;
                }
                // Check if this was a click on the faded part of the screen, and fire off the listener if there is one.
                if (ady <= dragSlop
                        && adx <= dragSlop
                        && mSlideOffset > 0 && !isViewUnder(mSlideableView, (int) mInitialMotionX, (int) mInitialMotionY) && mFadeOnClickListener != null) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                    mFadeOnClickListener.onClick(this);
                    return true;
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isTouchEnabled()) {
            return super.onTouchEvent(ev);
        }
        try {
            mDragHelper.processTouchEvent(ev);
            return true;
        } catch (Exception ex) {
            // Ignore the pointer out of range exception
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (!isEnabled() || !isTouchEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.abort();
            return super.dispatchTouchEvent(ev);
        }

        final float location;
        if (mIsSlidingVertically) {
            location = ev.getY();
        } else {
            location = ev.getX();
        }

        if (action == MotionEvent.ACTION_DOWN) {
            mIsScrollableViewHandlingTouch = false;
            mPrevMotionLocation = location;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float deltaLocation = location - mPrevMotionLocation;
            mPrevMotionLocation = location;

            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(mScrollableView, (int) mInitialMotionX, (int) mInitialMotionY)) {
                return super.dispatchTouchEvent(ev);
            }

            // Which direction (up or down) is the drag moving?
            int factor = 0;
            if(mGravity == Gravity.BOTTOM || mGravity == Gravity.RIGHT){
                factor = 1;
            } else {
                factor = -1;
            }
            if (deltaLocation * factor > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                // TODO: Does the ScrollableViewHandler need to know about horizontal sliding?
                if (mScrollableViewHelper.getScrollableViewScrollPosition(mScrollableView, mGravity == Gravity.BOTTOM) > 0) {
                    mIsScrollableViewHandlingTouch = true;
                    return super.dispatchTouchEvent(ev);
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (mIsScrollableViewHandlingTouch) {
                    // Send an 'UP' event to the child.
                    MotionEvent up = MotionEvent.obtain(ev);
                    up.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(up);
                    up.recycle();

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = false;
                return this.onTouchEvent(ev);
            } else if (deltaLocation * factor < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (mSlideOffset < 1.0f) {
                    mIsScrollableViewHandlingTouch = false;
                    return this.onTouchEvent(ev);
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!mIsScrollableViewHandlingTouch && mDragHelper.isDragging()) {
                    mDragHelper.cancel();
                    ev.setAction(MotionEvent.ACTION_DOWN);
                }

                mIsScrollableViewHandlingTouch = true;
                return super.dispatchTouchEvent(ev);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (mIsScrollableViewHandlingTouch) {
                mDragHelper.setDragState(ViewDragHelper.STATE_IDLE);
            }
        }

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev);
    }

    private boolean isViewUnder(View view, int x, int y) {
        if (view == null) return false;
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private int computePanelTopPosition(float slideOffset) {
        int slidingViewHeight = mSlideableView != null ? mSlideableView.getMeasuredHeight() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mGravity == Gravity.BOTTOM
                ? getMeasuredHeight() - getPaddingBottom() - mPaneSize - slidePixelOffset
                : getPaddingTop() - slidingViewHeight + mPaneSize + slidePixelOffset;
    }

    /*
     * Computes the left position of the panel based on the slide offset.
     */
    private int computePanelLeftPosition(float slideOffset) {
        int slidingViewWidth = mSlideableView != null ? mSlideableView.getMeasuredWidth() : 0;
        int slidePixelOffset = (int) (slideOffset * mSlideRange);
        // Compute the top of the panel if its collapsed
        return mGravity == Gravity.RIGHT
                ? getMeasuredWidth() - getPaddingRight() - mPaneSize - slidePixelOffset
                : getPaddingLeft() - slidingViewWidth + mPaneSize + slidePixelOffset;
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private float computeSlideOffset(int topOrLeftPosition) {
        // Compute the panel top position if the panel is collapsed (offset 0)
        final int topOrLeftBoundCollapsed;
        if(mIsSlidingVertically){
            topOrLeftBoundCollapsed = computePanelTopPosition(0);
        } else {
            topOrLeftBoundCollapsed = computePanelLeftPosition(0);
        }

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return (mGravity == Gravity.BOTTOM || mGravity == Gravity.RIGHT
                ? (float) (topOrLeftBoundCollapsed - topOrLeftPosition) / mSlideRange
                : (float) (topOrLeftPosition - topOrLeftBoundCollapsed) / mSlideRange);
    }

    /**
     * Returns the current state of the panel as an enum.
     *
     * @return the current panel state
     */
    public PanelState getPanelState() {
        return mSlideState;
    }

    /**
     * Change panel state to the given state with
     *
     * @param state - new panel state
     */
    public void setPanelState(PanelState state) {

        // Abort any running animation, to allow state change
        if(mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING){
            Log.d(TAG, "View is settling. Aborting animation.");
            mDragHelper.abort();
        }

        if (state == null || state == PanelState.DRAGGING) {
            throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
        }
        if (!isEnabled()
                || (!mFirstLayout && mSlideableView == null)
                || state == mSlideState
                || mSlideState == PanelState.DRAGGING) return;

        if (mFirstLayout) {
            setPanelStateInternal(state);
        } else {
            if (mSlideState == PanelState.HIDDEN) {
                mSlideableView.setVisibility(View.VISIBLE);
                requestLayout();
            }
            switch (state) {
                case ANCHORED:
                    smoothSlideTo(mAnchorPoint, 0);
                    break;
                case COLLAPSED:
                    smoothSlideTo(0, 0);
                    break;
                case EXPANDED:
                    smoothSlideTo(1.0f, 0);
                    break;
                case HIDDEN:
                    if (mIsSlidingVertically) {
                        int newTop = computePanelTopPosition(0.0f) + (mGravity == Gravity.BOTTOM ? +mPaneSize : -mPaneSize);
                        smoothSlideTo(computeSlideOffset(newTop), 0);
                    } else {
                        int newTop = computePanelLeftPosition(0.0f) + (mGravity == Gravity.RIGHT ? +mPaneSize : -mPaneSize);
                        smoothSlideTo(computeSlideOffset(newTop), 0);
                    }

                    break;
            }
        }
    }

    private void setPanelStateInternal(PanelState state) {
        if (mSlideState == state) return;
        PanelState oldState = mSlideState;
        mSlideState = state;
        dispatchOnPanelStateChanged(this, oldState, state);
    }

    /**
     * Update the parallax based on the current slide offset.
     */
    @SuppressLint("NewApi")
    private void applyParallaxForCurrentSlideOffset() {
        if (mParallaxOffset > 0) {
            int mainViewOffset = getCurrentParallaxOffset();
            if (mIsSlidingVertically){
                ViewCompat.setTranslationY(mMainView, mainViewOffset);
            } else {
                ViewCompat.setTranslationX(mMainView, mainViewOffset);
            }
        }
    }

    private void onPanelDragged(int newTopOrLeft) {
        if (mSlideState != PanelState.DRAGGING) {
            mLastNotDraggingSlideState = mSlideState;
        }
        setPanelStateInternal(PanelState.DRAGGING);
        // Recompute the slide offset based on the new top position
        mSlideOffset = computeSlideOffset(newTopOrLeft);
        applyParallaxForCurrentSlideOffset();
        // Dispatch the slide event
        dispatchOnPanelSlide(mSlideableView);
        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPaneSize;
        int defaultWidth = getWidth() - getPaddingRight() - getPaddingLeft() - mPaneSize;

        if (mSlideOffset <= 0 && !mOverlayContent) {
            // expand the main view
            if(mIsSlidingVertically){
                lp.height = mGravity == Gravity.BOTTOM ? (newTopOrLeft - getPaddingBottom()) : (getHeight() - getPaddingBottom() - mSlideableView.getMeasuredHeight() - newTopOrLeft);
                if (lp.height == defaultHeight) {
                    lp.height = LayoutParams.MATCH_PARENT;
                }
            } else {
                lp.width = mGravity == Gravity.RIGHT ? (newTopOrLeft - getPaddingRight()) : (getWidth() - getPaddingRight() - mSlideableView.getMeasuredWidth() - newTopOrLeft);
                if (lp.width == defaultWidth) {
                    lp.width = LayoutParams.MATCH_PARENT;
                }
            }

            mMainView.requestLayout();
        } else if (mIsSlidingVertically && (lp.height != LayoutParams.MATCH_PARENT && !mOverlayContent)) {
            lp.height = LayoutParams.MATCH_PARENT;
            mMainView.requestLayout();
        } else if (!mIsSlidingVertically && (lp.width != LayoutParams.MATCH_PARENT && !mOverlayContent)) {
            lp.width = LayoutParams.MATCH_PARENT;
            mMainView.requestLayout();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result;
        final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

        if (mSlideableView != null && mSlideableView != child) { // if main view
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            canvas.getClipBounds(mTmpRect);
            if (!mOverlayContent) {
                switch (mGravity){
                    case Gravity.BOTTOM:
                        mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
                        break;
                    case Gravity.TOP:
                        mTmpRect.top = Math.max(mTmpRect.top, mSlideableView.getBottom());
                        break;
                    case Gravity.RIGHT:
                        mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
                        break;
                    case Gravity.LEFT:
                        mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
                }
            }
            if (mClipPanel) {
                canvas.clipRect(mTmpRect);
            }

            result = super.drawChild(canvas, child, drawingTime);

            if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
                final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
                final int imag = (int) (baseAlpha * mSlideOffset);
                final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);
                mCoveredFadePaint.setColor(color);
                canvas.drawRect(mTmpRect, mCoveredFadePaint);
            }
        } else {
            result = super.drawChild(canvas, child, drawingTime);
        }

        canvas.restoreToCount(save);

        return result;
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity    initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!isEnabled() || mSlideableView == null) {
            // Nothing to do.
            return false;
        }

        boolean success;
        if(mIsSlidingVertically){
            int panelTop = computePanelTopPosition(slideOffset);
            success = mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), panelTop);
        } else {
            int panelLeft = computePanelLeftPosition(slideOffset);
            success = mDragHelper.smoothSlideViewTo(mSlideableView, panelLeft, mSlideableView.getTop());
        }

        if (success) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper != null && mDragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        // draw the shadow
        if (mShadowDrawable != null && mSlideableView != null) {
            final int top, bottom, left, right;
            if(mIsSlidingVertically){
                if (mGravity == Gravity.BOTTOM) {
                    top = mSlideableView.getTop() - mShadowHeight;
                    bottom = mSlideableView.getTop();
                } else {
                    top = mSlideableView.getBottom();
                    bottom = mSlideableView.getBottom() + mShadowHeight;
                }

                right = mSlideableView.getRight();
                left = mSlideableView.getLeft();
            } else {
                if (mGravity == Gravity.RIGHT) {
                    left = mSlideableView.getLeft() - mShadowHeight;
                    right = mSlideableView.getLeft();
                } else {
                    left = mSlideableView.getRight();
                    right = mSlideableView.getRight() + mShadowHeight;
                }

                top = mSlideableView.getTop();
                bottom = mSlideableView.getBottom();
            }

            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(c);
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
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
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putSerializable(SLIDING_STATE, mSlideState != PanelState.DRAGGING ? mSlideState : mLastNotDraggingSlideState);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mSlideState = (PanelState) bundle.getSerializable(SLIDING_STATE);
            mSlideState = mSlideState == null ? DEFAULT_SLIDE_STATE : mSlideState;
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mIsUnableToDrag) {
                return false;
            }

            return child == mSlideableView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                if(mIsSlidingVertically){
                    mSlideOffset = computeSlideOffset(mSlideableView.getTop());
                } else {
                    mSlideOffset = computeSlideOffset(mSlideableView.getLeft());
                }
                applyParallaxForCurrentSlideOffset();

                if (mSlideOffset == 1) {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.EXPANDED);
                } else if (mSlideOffset == 0) {
                    setPanelStateInternal(PanelState.COLLAPSED);
                } else if (mSlideOffset < 0) {
                    setPanelStateInternal(PanelState.HIDDEN);
                    mSlideableView.setVisibility(View.INVISIBLE);
                } else {
                    updateObscuredViewVisibility();
                    setPanelStateInternal(PanelState.ANCHORED);
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if(mIsSlidingVertically){
                onPanelDragged(top);
            } else {
                onPanelDragged(left);
            }
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int target = 0;

            // direction is always positive if we are sliding in the expanded direction
            float direction;
            if (mIsSlidingVertically){
                direction = mGravity == Gravity.BOTTOM ? -yvel : yvel;
            } else {
                direction = mGravity == Gravity.RIGHT ? -xvel : xvel;
            }

            if (direction > 0 && mSlideOffset <= mAnchorPoint) {
                // swipe up -> expand and stop at anchor point
                if (mIsSlidingVertically) target = computePanelTopPosition(mAnchorPoint);
                else target = computePanelLeftPosition(mAnchorPoint);
            } else if (direction > 0 && mSlideOffset > mAnchorPoint) {
                // swipe up past anchor -> expand
                if (mIsSlidingVertically) target = computePanelTopPosition(1.0f);
                else target = computePanelLeftPosition(1.0f);
            } else if (direction < 0 && mSlideOffset >= mAnchorPoint) {
                // swipe down -> collapse and stop at anchor point
                if (mIsSlidingVertically) target = computePanelTopPosition(mAnchorPoint);
                else target = computePanelLeftPosition(mAnchorPoint);
            } else if (direction < 0 && mSlideOffset < mAnchorPoint) {
                // swipe down past anchor -> collapse
                if(mIsSlidingVertically) target = computePanelTopPosition(0.0f);
                else target = computePanelLeftPosition(0.0f);
            } else if (mSlideOffset >= (1.f + mAnchorPoint) / 2) {
                // zero velocity, and far enough from anchor point => expand to the top
                if(mIsSlidingVertically) target = computePanelTopPosition(1.0f);
                else target = computePanelLeftPosition(1.0f);
            } else if (mSlideOffset >= mAnchorPoint / 2) {
                // zero velocity, and close enough to anchor point => go to anchor
                if(mIsSlidingVertically) target = computePanelTopPosition(mAnchorPoint);
                else target = computePanelLeftPosition(mAnchorPoint);
            } else {
                // settle at the bottom
                if (mIsSlidingVertically) target = computePanelTopPosition(0.0f);
                else target = computePanelLeftPosition(0.0f);
            }

            if(mIsSlidingVertically){
                mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
            } else {
                mDragHelper.settleCapturedViewAt(target, releasedChild.getTop());
            }
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            if(mIsSlidingVertically){ // Vertical
                return mSlideRange;
            } else { // Horizontal
                return super.getViewVerticalDragRange(child);
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            if(!mIsSlidingVertically){ // Horizontal
                return mSlideRange;
            } else { // Vertical
                return super.getViewHorizontalDragRange(child);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // The ViewDragHelper does not know, which scrolling direction is allowed and which not.
            // Therefore only clamp view position, if vertical scrolling is enabled, here.
            if(mIsSlidingVertically){
                final int collapsedTop = computePanelTopPosition(0.f);
                final int expandedTop = computePanelTopPosition(1.0f);
                if (mGravity == Gravity.BOTTOM) {
                    return Math.min(Math.max(top, expandedTop), collapsedTop);
                } else {
                    return Math.min(Math.max(top, collapsedTop), expandedTop);
                }
            } else { // Horizontally
                return super.clampViewPositionVertical(child, top, dy);
            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            // The ViewDragHelper does not know, which scrolling direction is allowed and which not.
            // Therefore only clamp view position, if horizontal scrolling is enabled, here.
            if(!mIsSlidingVertically) {// Horizontally
                final int collapsedLeft = computePanelLeftPosition(0.f);
                final int expandedLeft = computePanelLeftPosition(1.0f);
                if (mGravity == Gravity.RIGHT) {
                    return Math.min(Math.max(left, expandedLeft), collapsedLeft);
                } else {
                    return Math.min(Math.max(left, collapsedLeft), expandedLeft);
                }
            } else { // Vertically
                return super.clampViewPositionHorizontal(child, left, dx);
            }
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        public float weight = 0;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
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

            final TypedArray ta = c.obtainStyledAttributes(attrs, ATTRS);
            if (ta != null) {
                this.weight = ta.getFloat(0, 0);
            }

            ta.recycle();
        }
    }
}
