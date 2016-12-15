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
public class BasicPathView extends View {

    private static final int DEFAULT_STROKE_COLOR = 0xFF000000;
    private static final int DEFAULT_STROKE_WIDTH = 10;
    private float mScale = 1;
    private int dotRadius = 12;
    private int dotInterval = 60;
    private boolean mShouldDraw = true;
    private PathMode mode = PathMode.Lined;
    private Matrix mMatrix = new Matrix();
    private HashSet<float[]> mDrawablePaths = new HashSet<>();
    private Paint mDefaultPaint = new Paint();

    {
        mDefaultPaint.setStyle(Paint.Style.STROKE);
        mDefaultPaint.setColor(DEFAULT_STROKE_COLOR);
        mDefaultPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        mDefaultPaint.setAntiAlias(true);
    }

    public BasicPathView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public int getDotRadius() {
        return dotRadius;
    }

    public void setDotRadius(int dotRadius) {
        this.dotRadius = dotRadius;
    }

    public int getDotInterval() {
        return dotInterval;
    }

    public void setDotInterval(int dotInterval) {
        this.dotInterval = dotInterval;
    }

    public PathMode getMode() {
        return mode;
    }

    public void setMode(PathMode mode) {
        this.mode = mode;
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
            switch (mode) {
                case Lined:
                    for (float[] path : mDrawablePaths) {
                        float[] pathToDraw = new float[path.length];
                        mMatrix.mapPoints(pathToDraw, path);
                        canvas.drawLines(pathToDraw, mDefaultPaint);
                    }
                    break;
                case Dotted:
                    for (float[] path : mDrawablePaths) {
                        float[] pathToDraw = new float[path.length];
                        mMatrix.mapPoints(pathToDraw, path);
                        float lastHypotenuse = dotInterval;
                        for (int i = 0; i < path.length; i += 4) {
                            float startX = pathToDraw[i];
                            float endX = pathToDraw[i + 2];

                            float startY = pathToDraw[i + 1];
                            float endY = pathToDraw[i + 3];

                            float leftoverLength = dotInterval - lastHypotenuse;
                            float hypotenuse = (float) Math.hypot(startX - endX, startY - endY);
                            if (hypotenuse < leftoverLength) {
                                lastHypotenuse += hypotenuse;
                                continue;
                            }

                            float angle = (float) Math.atan2(endY - startY, endX - startX);
                            float cosAngle = (float) Math.cos(angle);
                            float sinAngle = (float) Math.sin(angle);

                            float incrementationX = cosAngle * leftoverLength;
                            float incrementationY = sinAngle * leftoverLength;
                            startX += incrementationX;
                            startY += incrementationY;
                            canvas.drawCircle(startX, startY, dotRadius, mDefaultPaint);

                            incrementationX = cosAngle * dotInterval;
                            incrementationY = sinAngle * dotInterval;

                            while ((lastHypotenuse = (float) Math.hypot(startX - endX, startY - endY)) > dotInterval) {
                                startX += incrementationX;
                                startY += incrementationY;
                                canvas.drawCircle(startX, startY, dotRadius, mDefaultPaint);
                            }
                        }
                    }
                    break;
            }
        }
        super.onDraw(canvas);
    }

    public enum PathMode {Lined, Dotted}
}
