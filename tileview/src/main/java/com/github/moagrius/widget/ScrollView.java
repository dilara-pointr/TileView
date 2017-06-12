package com.github.moagrius.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.github.moagrius.geom.FloatMathHelper;
import com.github.moagrius.view.TouchUpGestureDetector;

import java.lang.ref.WeakReference;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollView extends FrameLayout implements
  GestureDetector.OnGestureListener,
  GestureDetector.OnDoubleTapListener,
  TouchUpGestureDetector.OnTouchUpListener {

  private static final int DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400;

  private boolean mIsFlinging;
  private boolean mIsDragging;
  private boolean mIsSliding;

  private int mAnimationDuration = DEFAULT_ZOOM_PAN_ANIMATION_DURATION;

  private Scroller mScroller;
  private ZoomPanAnimator mZoomPanAnimator;

  private GestureDetector mGestureDetector;
  private TouchUpGestureDetector mTouchUpGestureDetector;

  /**
   * Constructor to use when creating a ScrollView from code.
   *
   * @param context The Context the ScrollView is running in, through which it can access the current theme, resources, etc.
   */
  public ScrollView(Context context) {
    this(context, null);
  }

  public ScrollView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setWillNotDraw(false);
    setClipChildren(false);
    mGestureDetector = new GestureDetector(context, this);
    mTouchUpGestureDetector = new TouchUpGestureDetector(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // TODO:
    /*
    // the container's children should be the size provided by setSize
    // don't use measureChildren because that grabs the child's LayoutParams
    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mScaledWidth, MeasureSpec.EXACTLY);
    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mScaledHeight, MeasureSpec.EXACTLY);
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    // but the layout itself should report normal (on screen) dimensions
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    width = resolveSize(width, widthMeasureSpec);
    height = resolveSize(height, heightMeasureSpec);
    setMeasuredDimension(width, height);
    */
  }

  /*
  ZoomPanChildren will always be laid out with the scaled dimenions - what is visible during
  scroll operations.  Thus, a RelativeLayout added as a child that had views within it using
  rules like ALIGN_PARENT_RIGHT would function as expected; similarly, an ImageView would be
  stretched between the visible edges.
  If children further operate on scale values, that should be accounted for
  in the child's logic (see ScalingLayout).
   */
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    // TODO:
    /*
    final int width = getWidth();
    final int height = getHeight();

    mOffsetX = mScaledWidth >= width ? 0 : width / 2 - mScaledWidth / 2;
    mOffsetY = mScaledHeight >= height ? 0 : height / 2 - mScaledHeight / 2;

    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        child.layout(mOffsetX, mOffsetY, mScaledWidth + mOffsetX, mScaledHeight + mOffsetY);
      }
    }
    calculateMinimumScaleToFit();
    constrainScrollToLimits();
    */
  }

  /**
   * Returns whether the ScrollView is currently being flung.
   *
   * @return true if the ScrollView is currently flinging, false otherwise.
   */
  public boolean isFlinging() {
    return mIsFlinging;
  }

  /**
   * Returns whether the ScrollView is currently being dragged.
   *
   * @return true if the ScrollView is currently dragging, false otherwise.
   */
  public boolean isDragging() {
    return mIsDragging;
  }

  /**
   * Returns whether the ScrollView is currently operating a scroll tween.
   *
   * @return True if the ScrollView is currently scrolling, false otherwise.
   */
  public boolean isSliding() {
    return mIsSliding;
  }

  /**
   * Returns the Scroller instance used to manage dragging and flinging.
   *
   * @return The Scroller instance use to manage dragging and flinging.
   */
  public Scroller getScroller() {
    // Instantiate default scroller if none is available
    if (mScroller == null) {
      mScroller = new Scroller(getContext());
    }
    return mScroller;
  }

  /**
   * Returns the duration zoom and pan animations will use.
   *
   * @return The duration zoom and pan animations will use.
   */
  public int getAnimationDuration() {
    return mAnimationDuration;
  }

  /**
   * Set the duration zoom and pan animation will use.
   *
   * @param animationDuration The duration animations will use.
   */
  public void setAnimationDuration(int animationDuration) {
    mAnimationDuration = animationDuration;
    if (mZoomPanAnimator != null) {
      mZoomPanAnimator.setDuration(mAnimationDuration);
    }
  }

  /**
   * Scrolls and centers the ScrollView to the x and y values provided.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void scrollToAndCenter(int x, int y) {
    scrollTo(x - getHalfWidth(), y - getHalfHeight());
  }

  /**
   * Scrolls the ScrollView to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideTo(int x, int y) {
    getAnimator().animatePan(x, y);
  }

  /**
   * Scrolls and centers the ScrollView to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void slideToAndCenter(int x, int y) {
    slideTo(x - getHalfWidth(), y - getHalfHeight());
  }

  private void constrainScrollToLimits() {
    int x = getScrollX();
    int y = getScrollY();
    int constrainedX = getConstrainedScrollX(x);
    int constrainedY = getConstrainedScrollY(y);
    if (x != constrainedX || y != constrainedY) {
      scrollTo(constrainedX, constrainedY);
    }
  }

  protected ZoomPanAnimator getAnimator() {
    if (mZoomPanAnimator == null) {
      mZoomPanAnimator = new ZoomPanAnimator(this);
      mZoomPanAnimator.setDuration(mAnimationDuration);
    }
    return mZoomPanAnimator;
  }

  private int getOffsetScrollXFromScale(int offsetX, float destinationScale, float currentScale) {
    int scrollX = getScrollX() + offsetX;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollX * deltaScale) - offsetX;
  }

  private int getOffsetScrollYFromScale(int offsetY, float destinationScale, float currentScale) {
    int scrollY = getScrollY() + offsetY;
    float deltaScale = destinationScale / currentScale;
    return (int) (scrollY * deltaScale) - offsetY;
  }

  @Override
  public boolean canScrollHorizontally(int direction) {
    int position = getScrollX();
    return direction > 0 ? position < getScrollLimitX() : direction < 0 && position > 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean gestureIntercept = mGestureDetector.onTouchEvent(event);
    boolean touchIntercept = mTouchUpGestureDetector.onTouchEvent(event);
    return gestureIntercept || touchIntercept || super.onTouchEvent(event);
  }

  @Override
  public void scrollTo(int x, int y) {
    x = getConstrainedScrollX(x);
    y = getConstrainedScrollY(y);
    super.scrollTo(x, y);
  }

  protected int getHalfWidth() {
    return FloatMathHelper.scale(getWidth(), 0.5f);
  }

  protected int getHalfHeight() {
    return FloatMathHelper.scale(getHeight(), 0.5f);
  }

  /**
   * This strategy is used to avoid that a custom return value of {@link #getScrollMinX} (which
   * default to 0) become the return value of this method which shifts the whole TileView.
   */
  protected int getConstrainedScrollX(int x) {
    return Math.max(getScrollMinX(), Math.min(x, getScrollLimitX()));
  }

  /**
   * See {@link #getConstrainedScrollX(int)}
   */
  protected int getConstrainedScrollY(int y) {
    return Math.max(getScrollMinY(), Math.min(y, getScrollLimitY()));
  }

  protected int getScrollLimitX() {
    // TODO:
    // return mScaledWidth - getWidth();
    return 0;
  }

  protected int getScrollLimitY() {
    // TODO:
    // return mScaledHeight - getHeight();
    return 0;
  }

  protected int getScrollMinX() {
    return 0;
  }

  protected int getScrollMinY() {
    return 0;
  }

  @Override
  public void computeScroll() {
    if (getScroller().computeScrollOffset()) {
      int startX = getScrollX();
      int startY = getScrollY();
      int endX = getConstrainedScrollX(getScroller().getCurrX());
      int endY = getConstrainedScrollY(getScroller().getCurrY());
      if (startX != endX || startY != endY) {
        scrollTo(endX, endY);
      }
      if (getScroller().isFinished()) {
        if (mIsFlinging) {
          mIsFlinging = false;
        }
      } else {
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }
  }

  @Override
  public boolean onDown(MotionEvent event) {
    if (mIsFlinging && !getScroller().isFinished()) {
      getScroller().forceFinished(true);
      mIsFlinging = false;
    }
    return true;
  }

  @Override
  public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
    getScroller().fling(getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY,
      getScrollMinX(), getScrollLimitX(), getScrollMinY(), getScrollLimitY());

    mIsFlinging = true;
    ViewCompat.postInvalidateOnAnimation(this);
    return true;
  }

  @Override
  public void onLongPress(MotionEvent event) {

  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    int scrollEndX = getScrollX() + (int) distanceX;
    int scrollEndY = getScrollY() + (int) distanceY;
    scrollTo(scrollEndX, scrollEndY);
    if (!mIsDragging) {
      mIsDragging = true;
    }
    return true;
  }

  @Override
  public void onShowPress(MotionEvent event) {

  }

  @Override
  public boolean onSingleTapUp(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onDoubleTap(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onTouchUp(MotionEvent event) {
    if (mIsDragging) {
      mIsDragging = false;
    }
    return true;
  }


  private static class ZoomPanAnimator extends ValueAnimator implements
    ValueAnimator.AnimatorUpdateListener,
    ValueAnimator.AnimatorListener {

    private WeakReference<ScrollView> mScrollViewWeakReference;
    private ScrollView.ZoomPanAnimator.ZoomPanState mStartState = new ScrollView.ZoomPanAnimator.ZoomPanState();
    private ScrollView.ZoomPanAnimator.ZoomPanState mEndState = new ScrollView.ZoomPanAnimator.ZoomPanState();
    private boolean mHasPendingPanUpdates;

    public ZoomPanAnimator(ScrollView ScrollView) {
      super();
      addUpdateListener(this);
      addListener(this);
      setFloatValues(0f, 1f);
      setInterpolator(new ScrollView.ZoomPanAnimator.FastEaseInInterpolator());
      mScrollViewWeakReference = new WeakReference<>(ScrollView);
    }

    private boolean setupPanAnimation(int x, int y) {
      ScrollView ScrollView = mScrollViewWeakReference.get();
      if (ScrollView != null) {
        mStartState.x = ScrollView.getScrollX();
        mStartState.y = ScrollView.getScrollY();
        mEndState.x = x;
        mEndState.y = y;
        return mStartState.x != mEndState.x || mStartState.y != mEndState.y;
      }
      return false;
    }

    public void animatePan(int x, int y) {
      ScrollView ScrollView = mScrollViewWeakReference.get();
      if (ScrollView != null) {
        mHasPendingPanUpdates = setupPanAnimation(x, y);
        if (mHasPendingPanUpdates) {
          start();
        }
      }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
      ScrollView scrollView = mScrollViewWeakReference.get();
      if (scrollView != null && mHasPendingPanUpdates) {
        float progress = (float) animation.getAnimatedValue();
          int x = (int) (mStartState.x + (mEndState.x - mStartState.x) * progress);
          int y = (int) (mStartState.y + (mEndState.y - mStartState.y) * progress);
          scrollView.scrollTo(x, y);
      }
    }

    @Override
    public void onAnimationStart(Animator animator) {
      ScrollView scrollView = mScrollViewWeakReference.get();
      if (scrollView != null && mHasPendingPanUpdates) {
        scrollView.mIsSliding = true;
      }
    }

    @Override
    public void onAnimationEnd(Animator animator) {
      ScrollView scrollView = mScrollViewWeakReference.get();
      if (scrollView != null && mHasPendingPanUpdates) {
        mHasPendingPanUpdates = false;
        scrollView.mIsSliding = false;
      }
    }

    @Override
    public void onAnimationCancel(Animator animator) {
      onAnimationEnd(animator);
    }

    @Override
    public void onAnimationRepeat(Animator animator) {

    }

    private static class ZoomPanState {
      public int x;
      public int y;
    }

    private static class FastEaseInInterpolator implements Interpolator {
      @Override
      public float getInterpolation(float input) {
        return (float) (1 - Math.pow(1 - input, 8));
      }
    }
  }

}
