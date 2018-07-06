package com.sothree.slidinguppanel.canvassaveproxy;

import android.graphics.Canvas;

public interface CanvasSaveProxy {
    int save();

    boolean isFor(final Canvas canvas);
}
