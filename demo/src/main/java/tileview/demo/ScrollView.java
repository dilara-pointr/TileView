package tileview.demo;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/**
 * TODO: overscroll methods
 * @author Mike Dunn, 6/11/17.
 */

public class ScrollView extends FrameLayout implements
  GestureDetector.OnGestureListener,
  TouchUpGestureDetector.OnTouchUpListener {

  private static final String ADD_VIEW_ERROR_MESSAGE = "ScrollView can host only one direct child";

  private static final int ANIMATED_SCROLL_GAP = 250;

  private static final int DIRECTION_BACKWARD = -1;
  private static final int DIRECTION_FORWARD = 1;

  private boolean mIsFlinging;
  private boolean mIsDragging;

  private boolean mSmoothScrollingEnabled = true;

  private long mLastScrolledAt;

  private OverScroller mOverScroller;

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
    setFocusable(true);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    mGestureDetector = new GestureDetector(context, this);
    mTouchUpGestureDetector = new TouchUpGestureDetector(this);
    mOverScroller = new OverScroller(context);
  }

  private void assertSingleChild() {
    if (getChildCount() > 0) {
      throw new IllegalStateException(ADD_VIEW_ERROR_MESSAGE);
    }
  }

  @Override
  public void addView(View child) {
    assertSingleChild();
    super.addView(child);
  }

  @Override
  public void addView(View child, int index) {
    assertSingleChild();
    super.addView(child, index);
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    assertSingleChild();
    super.addView(child, params);
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    assertSingleChild();
    super.addView(child, index, params);
  }

  public void setOverScroller(OverScroller overScroller) {
    mOverScroller = overScroller;
  }

  public boolean isSmoothScrollingEnabled() {
    return mSmoothScrollingEnabled;
  }

  public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
    mSmoothScrollingEnabled = smoothScrollingEnabled;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (hasContent()) {
      // we need to know how big the child would be if it were unconstrained in both dimension
      measureChildren(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
      Log.d("ScrollView", "onMeasure child.measuredHeight=" + getChild().getMeasuredHeight());
    }
    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    Log.d("ScrollView", "layout height=" + (b - t) + ", " + getChild().getMeasuredHeight());
    scrollTo(getScrollX(), getScrollY());
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
   * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
   *
   * @param x the number of pixels to scroll by on the X axis
   * @param y the number of pixels to scroll by on the Y axis
   */
  public void smoothScrollBy(int x, int y) {
    long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScrolledAt;
    if (duration > ANIMATED_SCROLL_GAP) {
      mOverScroller.startScroll(getScrollX(), getScrollY(), x, y);
      awakenScrollBars();
      invalidate();
    } else {
      if (!mOverScroller.isFinished()) {
        mOverScroller.abortAnimation();
      }
      scrollBy(x, y);
    }
    mLastScrolledAt = AnimationUtils.currentAnimationTimeMillis();
  }

  /**
   * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
   *
   * @param x the position where to scroll on the X axis
   * @param y the position where to scroll on the Y axis
   */
  public void smoothScrollTo(int x, int y) {
    smoothScrollBy(x - getScrollX(), y - getScrollY());
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
   * Scrolls and centers the ScrollView to the x and y values provided using scrolling animation.
   *
   * @param x Horizontal destination point.
   * @param y Vertical destination point.
   */
  public void smoothScrollToAndCenter(int x, int y) {
    smoothScrollTo(x - getHalfWidth(), y - getHalfHeight());
  }

  @Override
  public boolean canScrollHorizontally(int direction) {
    int position = getScrollX();
    return direction > 0 ? position < getScrollLimitX() : direction < 0 && position > 0;
  }

  @Override
  public boolean canScrollVertically(int direction) {
    int position = getScrollY();
    return direction > 0 ? position < getScrollLimitY() : direction < 0 && position > 0;
  }

  public boolean canScroll(int direction) {
    return canScrollVertically(direction) || canScrollHorizontally(direction);
  }

  public boolean canScroll() {
    return canScroll(DIRECTION_FORWARD) || canScroll(DIRECTION_BACKWARD);
  }

  private void performScrollBy(int x, int y) {
    if (mSmoothScrollingEnabled) {
      smoothScrollBy(x, y);
    } else {
      scrollBy(x, y);
    }
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

  private boolean hasContent() {
    return getChildCount() > 0;
  }

  private View getChild() {
    if (hasContent()) {
      return getChildAt(0);
    }
    return null;
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
    if (hasContent()) {
      return getChild().getLeft() + getChild().getMeasuredWidth() - getWidth();
    }
    return 0;
  }

  protected int getScrollLimitY() {
    if (hasContent()) {
      return getChild().getTop() + getChild().getMeasuredHeight() - getHeight();
    }
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
    if (mOverScroller.computeScrollOffset()) {
      scrollTo(mOverScroller.getCurrX(), mOverScroller.getCurrY());
      if (mOverScroller.isFinished()) {
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
    if (mIsFlinging && !mOverScroller.isFinished()) {
      mOverScroller.forceFinished(true);
      mIsFlinging = false;
    }
    return true;
  }

  @Override
  public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
    mOverScroller.fling(
      getScrollX(), getScrollY(),
      (int) -velocityX, (int) -velocityY,
      getScrollMinX(), getScrollLimitX(),
      getScrollMinY(), getScrollLimitY());
    mIsFlinging = true;
    ViewCompat.postInvalidateOnAnimation(this);
    return true;
  }

  @Override
  public void onLongPress(MotionEvent event) {

  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    Log.d("ScrollView", "onScroll");
    if (!mIsDragging) {
      mIsDragging = true;
    }
    int scrollEndX = getScrollX() + (int) distanceX;
    int scrollEndY = getScrollY() + (int) distanceY;
    scrollTo(scrollEndX, scrollEndY);
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
  public boolean onTouchUp(MotionEvent event) {
    if (mIsDragging) {
      mIsDragging = false;
    }
    return true;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    Log.d("ScrollView", "dispatchKeyEvent");
    return super.dispatchKeyEvent(event) || executeKeyEvent(event);
  }

  /**
   * You can call this function yourself to have the scroll view perform
   * scrolling from a key event, just as if the event had been dispatched to
   * it by the view hierarchy.
   *
   * @param event The key event to execute.
   * @return Return true if the event was handled, else false.
   */
  public boolean executeKeyEvent(KeyEvent event) {
    Log.d("ScrollView", "executeKeyEvent: " + event.getKeyCode() + ", " + KeyEvent.KEYCODE_DPAD_RIGHT);
    // this reads a bit goofy to me (any key advances focus), but it's _exactly_ what android.widget.ScrollView does, so w/e...
    if (!canScroll()) {
      if (isFocused()) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
          currentFocused = null;
        }
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, View.FOCUS_DOWN);
        return nextFocused != null && nextFocused != this && nextFocused.requestFocus(View.FOCUS_DOWN);
      }
      return false;
    }
    boolean alt = event.isAltPressed();
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_DPAD_UP:
          // if we can scroll up
          if (canScrollVertically(DIRECTION_BACKWARD)) {
            // if alt is down, scroll all the way home
            if (alt) {
              performScrollBy(0, -getScrollY());
            } else {  // otherwise scroll up one "page" (height)
              performScrollBy(0, -getHeight());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          // if we can scroll down
          if (canScrollVertically(DIRECTION_FORWARD)) {
            // if alt is down, scroll all the way to the end of content
            if (alt) {
              performScrollBy(0, getChild().getMeasuredHeight() - getScrollY());
            } else {  // otherwise scroll down one "page" (height)
              performScrollBy(0, getHeight());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          // if we can scroll left
          if (canScrollHorizontally(DIRECTION_BACKWARD)) {
            // if alt is down, scroll all the way home
            if (alt) {
              performScrollBy(0, -getScrollX());
            } else {  // otherwise scroll left one "page" (width)
              performScrollBy(0, -getWidth());
            }
            return true;
          }
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          Log.d("ScrollView", "key right");
          // if we can scroll right
          if (canScrollHorizontally(DIRECTION_FORWARD)) {
            Log.d("ScrollView", "can scroll right");
            // if alt is down, scroll all the way to the end of content
            if (alt) {
              performScrollBy(getChild().getMeasuredWidth() - getScrollX(), 0);
            } else {  // otherwise scroll right one "page" (width)
              performScrollBy(getWidth(), 0);
            }
            return true;
          }
          break;
      }
    }
    return false;
  }
}
