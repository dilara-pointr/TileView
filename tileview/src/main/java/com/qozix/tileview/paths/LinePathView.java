package com.qozix.tileview.paths;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;

import java.util.HashSet;

/**
 * Created by deliganli on 14.09.2016.
 */
public class LinePathView extends View {

    private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
    private static final int DEFAULT_STROKE_WIDTH = 10;

    private float mScale = 1;

    private boolean mShouldDraw = true;

    private Matrix mMatrix = new Matrix();

    private HashSet<float[]> mDrawablePaths = new HashSet<>();

    private Paint mDefaultPaint = new Paint();

    {
        mDefaultPaint.setStyle(Paint.Style.STROKE);
        mDefaultPaint.setColor(DEFAULT_STROKE_COLOR);
        mDefaultPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        mDefaultPaint.setAntiAlias(true);
    }

    public LinePathView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        mScale = scale;
        mMatrix.setScale(mScale, mScale);  // TODO: test this
        invalidate();
    }

    public void setPaint(Paint paint) {
        mDefaultPaint = paint;
    }

    public Paint getDefaultPaint() {
        return mDefaultPaint;
    }

    public void addPath(float[] path) {
        mDrawablePaths.add(path);
        invalidate();
    }

    public void removePath(float[] path) {
        if (mDrawablePaths.size() == 0) return;

        mDrawablePaths.remove(path);
        invalidate();
    }

    public void setPath(float[] path) {
        mDrawablePaths.clear();
        mDrawablePaths.add(path);
        invalidate();
    }

    public void clear() {
        if (mDrawablePaths.size() == 0) return;

        mDrawablePaths.clear();
        invalidate();
    }

    public void setShouldDraw(boolean shouldDraw) {
        mShouldDraw = shouldDraw;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mShouldDraw) {
            for (float[] path : mDrawablePaths) {
                float[] pathToDraw = new float[path.length];
                mMatrix.mapPoints(pathToDraw, path);
                canvas.drawLines(pathToDraw, mDefaultPaint);
            }
        }
        super.onDraw(canvas);
    }
}
