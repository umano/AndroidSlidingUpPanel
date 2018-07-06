package com.sothree.slidinguppanel.canvassaveproxy;

import android.graphics.Canvas;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Deprecated
class LegacyCanvasSaveProxy implements CanvasSaveProxy {
    private static final String TAG = CanvasSaveProxy.class.getSimpleName();
    private static final String METHOD_NAME = "save";
    private static final String FIELD_NAME = "CLIP_SAVE_FLAG";

    private final Canvas mCanvas;
    private final Method mSaveMethod;
    private final int mClipSaveFlag;

    LegacyCanvasSaveProxy(final Canvas canvas) {
        Log.d(TAG, "New LegacyCanvasSaveProxy");

        mCanvas = canvas;
        mSaveMethod = findSaveMethod();
        mClipSaveFlag = getClipSaveFlagValue();
    }

    @Override
    public int save() {
        return invokeSave();
    }

    @Override
    public boolean isFor(final Canvas canvas) {
        return canvas == mCanvas;
    }

    private int getClipSaveFlagValue() {
        final Field constantField;
        try {
            constantField = Canvas.class.getDeclaredField(FIELD_NAME);
            return (int) constantField.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to get value of " + FIELD_NAME + " - NoSuchFieldException", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to get value of " + FIELD_NAME + " - IllegalAccessException", e);
        }
    }

    private Method findSaveMethod() {
        try {
            return Canvas.class.getMethod(METHOD_NAME, int.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Canvas does not contain a method with signature save(int)");
        }
    }

    private int invokeSave() {
        try {
            return (int) mSaveMethod.invoke(mCanvas, mClipSaveFlag);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to execute save(int) - IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to execute save(int) - InvocationTargetException", e);
        }
    }
}
