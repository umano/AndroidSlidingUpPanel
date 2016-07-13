package com.sothree.slidinguppanel;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by bubu on 7/13/16.
 */
public final class AnimatorProxy extends Animation {
    /** Whether or not the current running platform needs to be proxied. */
    public static final boolean NEEDS_PROXY = Integer.valueOf(Build.VERSION.SDK) < Build.VERSION_CODES.HONEYCOMB;

    private static final WeakHashMap<View, AnimatorProxy> PROXIES =
            new WeakHashMap<View, AnimatorProxy>();

    /**
     * Create a proxy to allow for modifying post-3.0 view properties on all
     * pre-3.0 platforms. <strong>DO NOT</strong> wrap your views if you are
     * using {@code ObjectAnimator} as it will handle that itself.
     *
     * @param view View to wrap.
     * @return Proxy to post-3.0 properties.
     */
    public static AnimatorProxy wrap(View view) {
        AnimatorProxy proxy = PROXIES.get(view);
        // This checks if the proxy already exists and whether it still is the animation of the given view
        if (proxy == null || proxy != view.getAnimation()) {
            proxy = new AnimatorProxy(view);
            PROXIES.put(view, proxy);
        }
        return proxy;
    }

    private final WeakReference<View> mView;
    private final Camera mCamera = new Camera();
    private boolean mHasPivot;

    private float mAlpha = 1;
    private float mPivotX;
    private float mPivotY;
    private float mRotationX;
    private float mRotationY;
    private float mRotationZ;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mTranslationX;
    private float mTranslationY;

    private final RectF mBefore = new RectF();
    private final RectF mAfter = new RectF();
    private final Matrix mTempMatrix = new Matrix();

    private AnimatorProxy(View view) {
        setDuration(0); //perform transformation immediately
        setFillAfter(true); //persist transformation beyond duration
        view.setAnimation(this);
        mView = new WeakReference<View>(view);
    }

    public float getAlpha() {
        return mAlpha;
    }
    public void setAlpha(float alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            View view = mView.get();
            if (view != null) {
                view.invalidate();
            }
        }
    }
    public float getPivotX() {
        return mPivotX;
    }
    public void setPivotX(float pivotX) {
        if (!mHasPivot || mPivotX != pivotX) {
            prepareForUpdate();
            mHasPivot = true;
            mPivotX = pivotX;
            invalidateAfterUpdate();
        }
    }
    public float getPivotY() {
        return mPivotY;
    }
    public void setPivotY(float pivotY) {
        if (!mHasPivot || mPivotY != pivotY) {
            prepareForUpdate();
            mHasPivot = true;
            mPivotY = pivotY;
            invalidateAfterUpdate();
        }
    }
    public float getRotation() {
        return mRotationZ;
    }
    public void setRotation(float rotation) {
        if (mRotationZ != rotation) {
            prepareForUpdate();
            mRotationZ = rotation;
            invalidateAfterUpdate();
        }
    }
    public float getRotationX() {
        return mRotationX;
    }
    public void setRotationX(float rotationX) {
        if (mRotationX != rotationX) {
            prepareForUpdate();
            mRotationX = rotationX;
            invalidateAfterUpdate();
        }
    }
    public float getRotationY() {
        return mRotationY;
    }

    public void setRotationY(float rotationY) {
        if (mRotationY != rotationY) {
            prepareForUpdate();
            mRotationY = rotationY;
            invalidateAfterUpdate();
        }
    }
    public float getScaleX() {
        return mScaleX;
    }
    public void setScaleX(float scaleX) {
        if (mScaleX != scaleX) {
            prepareForUpdate();
            mScaleX = scaleX;
            invalidateAfterUpdate();
        }
    }
    public float getScaleY() {
        return mScaleY;
    }
    public void setScaleY(float scaleY) {
        if (mScaleY != scaleY) {
            prepareForUpdate();
            mScaleY = scaleY;
            invalidateAfterUpdate();
        }
    }
    public int getScrollX() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getScrollX();
    }
    public void setScrollX(int value) {
        View view = mView.get();
        if (view != null) {
            view.scrollTo(value, view.getScrollY());
        }
    }
    public int getScrollY() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getScrollY();
    }
    public void setScrollY(int value) {
        View view = mView.get();
        if (view != null) {
            view.scrollTo(view.getScrollX(), value);
        }
    }

    public float getTranslationX() {
        return mTranslationX;
    }
    public void setTranslationX(float translationX) {
        if (mTranslationX != translationX) {
            prepareForUpdate();
            mTranslationX = translationX;
            invalidateAfterUpdate();
        }
    }
    public float getTranslationY() {
        return mTranslationY;
    }
    public void setTranslationY(float translationY) {
        if (mTranslationY != translationY) {
            prepareForUpdate();
            mTranslationY = translationY;
            invalidateAfterUpdate();
        }
    }
    public float getX() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getLeft() + mTranslationX;
    }
    public void setX(float x) {
        View view = mView.get();
        if (view != null) {
            setTranslationX(x - view.getLeft());
        }
    }
    public float getY() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getTop() + mTranslationY;
    }
    public void setY(float y) {
        View view = mView.get();
        if (view != null) {
            setTranslationY(y - view.getTop());
        }
    }

    private void prepareForUpdate() {
        View view = mView.get();
        if (view != null) {
            computeRect(mBefore, view);
        }
    }
    private void invalidateAfterUpdate() {
        View view = mView.get();
        if (view == null || view.getParent() == null) {
            return;
        }

        final RectF after = mAfter;
        computeRect(after, view);
        after.union(mBefore);

        ((View)view.getParent()).invalidate(
                (int) Math.floor(after.left),
                (int) Math.floor(after.top),
                (int) Math.ceil(after.right),
                (int) Math.ceil(after.bottom));
    }

    private void computeRect(final RectF r, View view) {
        // compute current rectangle according to matrix transformation
        final float w = view.getWidth();
        final float h = view.getHeight();

        // use a rectangle at 0,0 to make sure we don't run into issues with scaling
        r.set(0, 0, w, h);

        final Matrix m = mTempMatrix;
        m.reset();
        transformMatrix(m, view);
        mTempMatrix.mapRect(r);

        r.offset(view.getLeft(), view.getTop());

        // Straighten coords if rotations flipped them
        if (r.right < r.left) {
            final float f = r.right;
            r.right = r.left;
            r.left = f;
        }
        if (r.bottom < r.top) {
            final float f = r.top;
            r.top = r.bottom;
            r.bottom = f;
        }
    }

    private void transformMatrix(Matrix m, View view) {
        final float w = view.getWidth();
        final float h = view.getHeight();
        final boolean hasPivot = mHasPivot;
        final float pX = hasPivot ? mPivotX : w / 2f;
        final float pY = hasPivot ? mPivotY : h / 2f;

        final float rX = mRotationX;
        final float rY = mRotationY;
        final float rZ = mRotationZ;
        if ((rX != 0) || (rY != 0) || (rZ != 0)) {
            final Camera camera = mCamera;
            camera.save();
            camera.rotateX(rX);
            camera.rotateY(rY);
            camera.rotateZ(-rZ);
            camera.getMatrix(m);
            camera.restore();
            m.preTranslate(-pX, -pY);
            m.postTranslate(pX, pY);
        }

        final float sX = mScaleX;
        final float sY = mScaleY;
        if ((sX != 1.0f) || (sY != 1.0f)) {
            m.postScale(sX, sY);
            final float sPX = -(pX / w) * ((sX * w) - w);
            final float sPY = -(pY / h) * ((sY * h) - h);
            m.postTranslate(sPX, sPY);
        }

        m.postTranslate(mTranslationX, mTranslationY);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        View view = mView.get();
        if (view != null) {
            t.setAlpha(mAlpha);
            transformMatrix(t.getMatrix(), view);
        }
    }
}
