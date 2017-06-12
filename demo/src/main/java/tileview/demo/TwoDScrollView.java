package tileview.demo;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Scroller;

import java.util.List;

/**
 * @author Mike Dunn, 6/11/17.
 */

public class TwoDScrollView extends FrameLayout {

  private static final String ADD_VIEW_ERROR_MESSAGE = "ScrollView can host only one direct child";

  private static final int ANIMATED_SCROLL_GAP = 250;
  private static final float MAX_SCROLL_FACTOR = 0.5f;

  private long mLastScroll;
  private final Rect mTempRect = new Rect();
  private Scroller mScroller;
  private boolean mScrollViewMovedFocus;
  private float mLastMotionY;
  private boolean mIsLayoutDirty = true;
  private View mChildToScrollTo = null;
  private boolean mIsBeingDragged = false;
  private VelocityTracker mVelocityTracker;
  private boolean mFillViewport;
  private boolean mSmoothScrollingEnabled = true;
  private int mTouchSlop;
  private int mMinimumVelocity;
  private int mMaximumVelocity;

  public TwoDScrollView(Context context) {
    this(context, null);
  }

  public TwoDScrollView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TwoDScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mScroller = new Scroller(getContext());
    setFocusable(true);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    setWillNotDraw(false);
    final ViewConfiguration configuration = ViewConfiguration.get(getContext());
    mTouchSlop = configuration.getScaledTouchSlop();
    mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
  }

  /**
   * @return The maximum amount this scroll view will scroll in response to
   * an arrow event.
   */
  public int getMaxScrollYAmount() {
    return (int) (MAX_SCROLL_FACTOR * (getBottom() - getTop()));
  }

  /**
   * @return
   */
  public int getMaxScrollXAmount() {
    return (int) (MAX_SCROLL_FACTOR * (getRight() - getLeft()));
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

  private boolean canScrollVertically() {
    View child = getChildAt(0);
    if (child != null) {
      return getHeight() < child.getHeight() + getPaddingTop() + getPaddingBottom();
    }
    return false;
  }

  private boolean canScrollHorizontally() {
    View child = getChildAt(0);
    if (child != null) {
      return getWidth() < child.getWidth() + getPaddingBottom() + getPaddingRight();
    }
    return false;
  }

  private boolean canScroll() {
    return canScrollVertically() || canScrollHorizontally();
  }

  /**
   * Indicates whether this ScrollView's content is stretched to fill the viewport.
   *
   * @return True if the content fills the viewport, false otherwise.
   */
  public boolean isFillViewport() {
    return mFillViewport;
  }

  /**
   * Indicates this ScrollView whether it should stretch its content height to fill
   * the viewport or not.
   *
   * @param fillViewport True to stretch the content's height to the viewport's
   *                     boundaries, false otherwise.
   */
  public void setFillViewport(boolean fillViewport) {
    if (fillViewport != mFillViewport) {
      mFillViewport = fillViewport;
      requestLayout();
    }
  }

  /**
   * @return Whether arrow scrolling will animate its transition.
   */
  public boolean isSmoothScrollingEnabled() {
    return mSmoothScrollingEnabled;
  }

  /**
   * Set whether arrow scrolling will animate its transition.
   *
   * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
   */
  public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
    mSmoothScrollingEnabled = smoothScrollingEnabled;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
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
    mTempRect.setEmpty();

    if (!canScroll()) {
      if (isFocused()) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
          currentFocused = null;
        }
        View nextFocused = FocusFinder.getInstance().findNextFocus(this,          currentFocused, View.FOCUS_DOWN);
        return nextFocused != null          && nextFocused != this          && nextFocused.requestFocus(View.FOCUS_DOWN);
      }
      return false;
    }

    boolean handled = false;
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_DPAD_UP:
          if (!event.isAltPressed()) {
            handled = arrowScroll(View.FOCUS_UP);
          } else {
            handled = fullScroll(View.FOCUS_UP);
          }
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          if (!event.isAltPressed()) {
            handled = arrowScroll(View.FOCUS_DOWN);
          } else {
            handled = fullScroll(View.FOCUS_DOWN);
          }
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          if (!event.isAltPressed()) {
            handled = arrowScroll(View.FOCUS_LEFT);
          } else {
            handled = fullScroll(View.FOCUS_LEFT);
          }
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          if (!event.isAltPressed()) {
            handled = arrowScroll(View.FOCUS_RIGHT);
          } else {
            handled = fullScroll(View.FOCUS_RIGHT);
          }
          break;
        case KeyEvent.KEYCODE_SPACE:
          pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
          break;
      }
    }

    return handled;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = ev.getAction();
    if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
      return true;
    }

    if (!canScroll()) {
      mIsBeingDragged = false;
      return false;
    }

    final float y = ev.getY();

    switch (action) {
      case MotionEvent.ACTION_MOVE:
        final int yDiff = (int) Math.abs(y - mLastMotionY);
        if (yDiff > mTouchSlop) {
          mIsBeingDragged = true;
        }
        break;

      case MotionEvent.ACTION_DOWN:
        mLastMotionY = y;
        mIsBeingDragged = !mScroller.isFinished();
        break;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        mIsBeingDragged = false;
        break;
    }
    return mIsBeingDragged;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {

    if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
      // Don't handle edge touches immediately -- they may actually belong to one of our
      // descendants.
      return false;
    }

    if (!canScroll()) {
      return false;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

    final int action = ev.getAction();
    final float y = ev.getY();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
                /*
                * If being flinged and user touches, stop the fling. isFinished
                * will be false if being flinged.
                */
        if (!mScroller.isFinished()) {
          mScroller.abortAnimation();
        }

        // Remember where the motion event started
        mLastMotionY = y;
        break;
      case MotionEvent.ACTION_MOVE:
        // Scroll to follow the motion event
        final int deltaY = (int) (mLastMotionY - y);
        mLastMotionY = y;

        if (deltaY < 0) {
          if (getScrollY() > 0) {
            scrollBy(0, deltaY);
          }
        } else if (deltaY > 0) {
          final int bottomEdge = getHeight() - getPaddingBottom();
          final int availableToScroll = getChildAt(0).getBottom() - getScrollY() - bottomEdge;
          if (availableToScroll > 0) {
            scrollBy(0, Math.min(availableToScroll, deltaY));
          }
        }
        break;
      case MotionEvent.ACTION_UP:
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        int initialVelocity = (int) velocityTracker.getYVelocity();

        if ((Math.abs(initialVelocity) > mMinimumVelocity) && getChildCount() > 0) {
          fling(-initialVelocity);
        }

        if (mVelocityTracker != null) {
          mVelocityTracker.recycle();
          mVelocityTracker = null;
        }
    }
    return true;
  }

  /**
   * <p>
   * Finds the next focusable component that fits in this View's bounds
   * (excluding fading edges) pretending that this View's top is located at
   * the parameter top.
   * </p>
   *
   * @param topFocus           look for a candidate is the one at the top of the bounds
   *                           if topFocus is true, or at the bottom of the bounds if topFocus is
   *                           false
   * @param top                the top offset of the bounds in which a focusable must be
   *                           found (the fading edge is assumed to start at this position)
   * @param preferredFocusable the View that has highest priority and will be
   *                           returned if it is within my bounds (null is valid)
   * @return the next focusable component in the bounds or null if none can be
   * found
   */
  private View findFocusableViewInMyBounds(final boolean topFocus,
                                           final int top, View preferredFocusable) {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
    final int fadingEdgeLength = getVerticalFadingEdgeLength() / 2;
    final int topWithoutFadingEdge = top + fadingEdgeLength;
    final int bottomWithoutFadingEdge = top + getHeight() - fadingEdgeLength;

    if ((preferredFocusable != null)
      && (preferredFocusable.getTop() < bottomWithoutFadingEdge)
      && (preferredFocusable.getBottom() > topWithoutFadingEdge)) {
      return preferredFocusable;
    }

    return findFocusableViewInBounds(topFocus, topWithoutFadingEdge,
      bottomWithoutFadingEdge);
  }

  /**
   * <p>
   * Finds the next focusable component that fits in the specified bounds.
   * </p>
   *
   * @param topFocus look for a candidate is the one at the top of the bounds
   *                 if topFocus is true, or at the bottom of the bounds if topFocus is
   *                 false
   * @param top      the top offset of the bounds in which a focusable must be
   *                 found
   * @param bottom   the bottom offset of the bounds in which a focusable must
   *                 be found
   * @return the next focusable component in the bounds or null if none can
   * be found
   */
  private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

    List<View> focusables = getFocusables(View.FOCUS_FORWARD);
    View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
    boolean foundFullyContainedFocusable = false;

    int count = focusables.size();
    for (int i = 0; i < count; i++) {
      View view = focusables.get(i);
      int viewTop = view.getTop();
      int viewBottom = view.getBottom();

      if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

        final boolean viewIsFullyContained = (top < viewTop) &&
          (viewBottom < bottom);

        if (focusCandidate == null) {
                    /* No candidate, take this one */
          focusCandidate = view;
          foundFullyContainedFocusable = viewIsFullyContained;
        } else {
          final boolean viewIsCloserToBoundary =
            (topFocus && viewTop < focusCandidate.getTop()) ||
              (!topFocus && viewBottom > focusCandidate
                .getBottom());

          if (foundFullyContainedFocusable) {
            if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
              focusCandidate = view;
            }
          } else {
            if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
              focusCandidate = view;
              foundFullyContainedFocusable = true;
            } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
              focusCandidate = view;
            }
          }
        }
      }
    }

    return focusCandidate;
  }

  private boolean pageScrollHorizontal(int direction) {
    boolean right = direction == View.FOCUS_RIGHT;
    int width = getWidth();

    if (right) {
      mTempRect.left = getScrollX() + width;
      int count = getChildCount();
      if (count > 0) {
        View view = getChildAt(0);
        if (mTempRect.left + width > view.getRight()) {
          mTempRect.left = view.getRight() - width;
        }
      }
    } else {
      mTempRect.left = getScrollX() - width;
      if (mTempRect.left < 0) {
        mTempRect.left = 0;
      }
    }
    mTempRect.right = mTempRect.left + width;

    return scrollAndFocusHorizontal(direction, mTempRect.left, mTempRect.right);
  }

  private boolean pageScrollVertical(int direction) {
    boolean down = direction == View.FOCUS_DOWN;
    int height = getHeight();

    if (down) {
      mTempRect.top = getScrollY() + height;
      int count = getChildCount();
      if (count > 0) {
        View view = getChildAt(count - 1);
        if (mTempRect.top + height > view.getBottom()) {
          mTempRect.top = view.getBottom() - height;
        }
      }
    } else {
      mTempRect.top = getScrollY() - height;
      if (mTempRect.top < 0) {
        mTempRect.top = 0;
      }
    }
    mTempRect.bottom = mTempRect.top + height;

    return scrollAndFocusVertical(direction, mTempRect.top, mTempRect.bottom);
  }

  /**
   * <p>Handles scrolling in response to a "page up/down" shortcut press. This
   * method will scroll the view by one page up or down and give the focus
   * to the topmost/bottommost component in the new visible area. If no
   * component is a good candidate for focus, this scrollview reclaims the
   * focus.</p>
   *
   * @param direction the scroll direction: {@link View#FOCUS_UP}
   *                  to go one page up or
   *                  {@link View#FOCUS_DOWN} to go one page down
   * @return true if the key event is consumed by this method, false otherwise
   */
  public boolean pageScroll(int direction) {
    if (canScrollVertically()) {
      return pageScrollVertical(direction);
    }
    if (canScrollHorizontally()) {
      return pageScrollHorizontal(direction);
    }
    return false;
  }

  private boolean fullScrollHorizontal(int direction) {
    boolean right = direction == View.FOCUS_RIGHT;
    int width = getWidth();

    mTempRect.left = 0;
    mTempRect.right = width;

    if (right) {
      int count = getChildCount();
      if (count > 0) {
        View view = getChildAt(0);
        mTempRect.right = view.getRight();
        mTempRect.left = mTempRect.right - width;
      }
    }

    return scrollAndFocusHorizontal(direction, mTempRect.left, mTempRect.right);
  }

  private boolean fullScrollVertical(int direction) {
    boolean down = direction == View.FOCUS_DOWN;
    int height = getHeight();

    mTempRect.top = 0;
    mTempRect.bottom = height;

    if (down) {
      int count = getChildCount();
      if (count > 0) {
        View view = getChildAt(count - 1);
        mTempRect.bottom = view.getBottom();
        mTempRect.top = mTempRect.bottom - height;
      }
    }

    return scrollAndFocusVertical(direction, mTempRect.top, mTempRect.bottom);
  }

  /**
   * <p>Handles scrolling in response to a "home/end" shortcut press. This
   * method will scroll the view to the top or bottom and give the focus
   * to the topmost/bottommost component in the new visible area. If no
   * component is a good candidate for focus, this scrollview reclaims the
   * focus.</p>
   *
   * @param direction the scroll direction: {@link View#FOCUS_UP}
   *                  to go the top of the view or
   *                  {@link View#FOCUS_DOWN} to go the bottom
   * @return true if the key event is consumed by this method, false otherwise
   */
  public boolean fullScroll(int direction) {
    if (canScrollVertically()) {
      return fullScrollVertical(direction);
    }
    if (canScrollHorizontally()) {
      return fullScrollHorizontal(direction);
    }
    return false;
  }

  private boolean scrollAndFocusHorizontal(int direction, int left, int right) {
    boolean handled = true;

    int width = getWidth();
    int containerLeft = getScrollX();
    int containerRight = containerLeft + width;
    boolean goLeft = direction == View.FOCUS_LEFT;

    View newFocused = findFocusableViewInBounds(goLeft, left, right);
    if (newFocused == null) {
      newFocused = this;
    }

    if (left >= containerLeft && right <= containerRight) {
      handled = false;
    } else {
      int delta = goLeft ? (left - containerLeft) : (right - containerRight);
      doScrollX(delta);
    }

    if (newFocused != findFocus() && newFocused.requestFocus(direction)) {
      mScrollViewMovedFocus = true;
      mScrollViewMovedFocus = false;
    }

    return handled;
  }

  private boolean scrollAndFocusVertical(int direction, int top, int bottom) {
    boolean handled = true;

    int height = getHeight();
    int containerTop = getScrollY();
    int containerBottom = containerTop + height;
    boolean up = direction == View.FOCUS_UP;

    View newFocused = findFocusableViewInBounds(up, top, bottom);
    if (newFocused == null) {
      newFocused = this;
    }

    if (top >= containerTop && bottom <= containerBottom) {
      handled = false;
    } else {
      int delta = up ? (top - containerTop) : (bottom - containerBottom);
      doScrollY(delta);
    }

    if (newFocused != findFocus() && newFocused.requestFocus(direction)) {
      mScrollViewMovedFocus = true;
      mScrollViewMovedFocus = false;
    }

    return handled;
  }

  /**
   * Handle scrolling in response to an up or down arrow click.
   *
   * @param direction The direction corresponding to the arrow key that was
   *                  pressed
   * @return True if we consumed the event, false otherwise
   */
  public boolean arrowScroll(int direction) {

    View currentFocused = findFocus();
    if (currentFocused == this) {
      currentFocused = null;
    }

    View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

    final int maxYJump = getMaxScrollYAmount();
    final int maxXJump = getMaxScrollXAmount();

    if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxYJump, getHeight())) {
      nextFocused.getDrawingRect(mTempRect);
      offsetDescendantRectToMyCoords(nextFocused, mTempRect);
      int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
      doScrollY(scrollDelta);
      nextFocused.requestFocus(direction);
    } else {
      // no new focus
      int scrollDelta = maxYJump;

      if (direction == View.FOCUS_UP && getScrollY() < scrollDelta) {
        scrollDelta = getScrollY();
      } else if (direction == View.FOCUS_DOWN) {
        if (getChildCount() > 0) {
          int daBottom = getChildAt(0).getBottom();

          int screenBottom = getScrollY() + getHeight();

          if (daBottom - screenBottom < maxYJump) {
            scrollDelta = daBottom - screenBottom;
          }
        }
      }
      if (scrollDelta == 0) {
        return false;
      }
      doScrollY(direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta);
    }

    if (currentFocused != null && currentFocused.isFocused()
      && isOffScreen(currentFocused)) {
      // previously focused item still has focus and is off screen, give
      // it up (take it back to ourselves)
      // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
      // sure to
      // get it)
      final int descendantFocusability = getDescendantFocusability();  // save
      setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
      requestFocus();
      setDescendantFocusability(descendantFocusability);  // restore
    }
    return true;
  }

  /**
   * @return whether the descendant of this scroll view is scrolled off
   * screen.
   */
  private boolean isOffScreen(View descendant) {
    return !isWithinDeltaOfScreen(descendant, 0, getHeight());
  }

  /**
   * @return whether the descendant of this scroll view is within delta
   * pixels of being on the screen.
   */
  private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
    descendant.getDrawingRect(mTempRect);
    offsetDescendantRectToMyCoords(descendant, mTempRect);

    return (mTempRect.bottom + delta) >= getScrollY()
      && (mTempRect.top - delta) <= (getScrollY() + height);
  }

  /**
   * Smooth scroll by a Y delta
   *
   * @param delta the number of pixels to scroll by on the Y axis
   */
  private void doScrollY(int delta) {
    doScroll(0, delta);
  }

  private void doScrollX(int delta) {
    doScroll(delta, 0);
  }

  private void doScroll(int deltaX, int deltaY) {
    if (mSmoothScrollingEnabled) {
      smoothScrollBy(deltaX, deltaY);
    } else {
      scrollBy(deltaX, deltaY);
    }
  }

  /**
   * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
   *
   * @param dx the number of pixels to scroll by on the X axis
   * @param dy the number of pixels to scroll by on the Y axis
   */
  public final void smoothScrollBy(int dx, int dy) {
    long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
    if (duration > ANIMATED_SCROLL_GAP) {
      mScroller.startScroll(getScrollX(), getScrollY(), dx, dy);
      awakenScrollBars(mScroller.getDuration());
      invalidate();
    } else {
      if (!mScroller.isFinished()) {
        mScroller.abortAnimation();
      }
      scrollBy(dx, dy);
    }
    mLastScroll = AnimationUtils.currentAnimationTimeMillis();
  }

  /**
   * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
   *
   * @param x the position where to scroll on the X axis
   * @param y the position where to scroll on the Y axis
   */
  public final void smoothScrollTo(int x, int y) {
    smoothScrollBy(x - getScrollX(), y - getScrollY());
  }

  /**
   * <p>The scroll range of a scroll view is the overall height of all of its
   * children.</p>
   */
  @Override
  protected int computeVerticalScrollRange() {
    return getChildCount() == 0 ? getHeight() : (getChildAt(0)).getBottom();
  }


  @Override
  protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
    ViewGroup.LayoutParams lp = child.getLayoutParams();
    int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
    int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  @Override
  protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  @Override
  public void computeScroll() {
    if (mScroller.computeScrollOffset()) {
      int x = mScroller.getCurrX();
      int y = mScroller.getCurrY();
      scrollTo(x, y);
    }
  }

  /**
   * Scrolls the view to the given child.
   *
   * @param child the View to scroll to
   */
  private void scrollToChild(View child) {
    child.getDrawingRect(mTempRect);
    offsetDescendantRectToMyCoords(child, mTempRect);
    int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
    if (scrollDelta != 0) {
      scrollBy(0, scrollDelta);
    }
  }

  /**
   * If rect is off screen, scroll just enough to get it (or at least the
   * first screen size chunk of it) on screen.
   *
   * @param rect      The rectangle.
   * @param immediate True to scroll immediately without animation
   * @return true if scrolling was performed
   */
  private boolean scrollToChildRect(Rect rect, boolean immediate) {
    final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
    final boolean scroll = delta != 0;
    if (scroll) {
      if (immediate) {
        scrollBy(0, delta);
      } else {
        smoothScrollBy(0, delta);
      }
    }
    return scroll;
  }

  /**
   * Compute the amount to scroll in the Y direction in order to get
   * a rectangle completely on the screen (or, if taller than the screen,
   * at least the first screen size chunk of it).
   *
   * @param rect The rect.
   * @return The scroll delta.
   */
  protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
    if (getChildCount() == 0) return 0;

    int height = getHeight();
    int screenTop = getScrollY();
    int screenBottom = screenTop + height;

    int fadingEdge = getVerticalFadingEdgeLength();

    // leave room for top fading edge as long as rect isn't at very top
    if (rect.top > 0) {
      screenTop += fadingEdge;
    }

    // leave room for bottom fading edge as long as rect isn't at very bottom
    if (rect.bottom < getChildAt(0).getHeight()) {
      screenBottom -= fadingEdge;
    }

    int scrollYDelta = 0;

    if (rect.bottom > screenBottom && rect.top > screenTop) {
      // need to move down to get it in view: move down just enough so
      // that the entire rectangle is in view (or at least the first
      // screen size chunk).

      if (rect.height() > height) {
        // just enough to get screen size chunk on
        scrollYDelta += (rect.top - screenTop);
      } else {
        // get entire rect at bottom of screen
        scrollYDelta += (rect.bottom - screenBottom);
      }

      // make sure we aren't scrolling beyond the end of our content
      int bottom = getChildAt(0).getBottom();
      int distanceToBottom = bottom - screenBottom;
      scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

    } else if (rect.top < screenTop && rect.bottom < screenBottom) {
      // need to move up to get it in view: move up just enough so that
      // entire rectangle is in view (or at least the first screen
      // size chunk of it).

      if (rect.height() > height) {
        // screen size chunk
        scrollYDelta -= (screenBottom - rect.bottom);
      } else {
        // entire rect at top
        scrollYDelta -= (screenTop - rect.top);
      }

      // make sure we aren't scrolling any further than the top our content
      scrollYDelta = Math.max(scrollYDelta, -getScrollY());
    }
    return scrollYDelta;
  }

  @Override
  public void requestChildFocus(View child, View focused) {
    if (!mScrollViewMovedFocus) {
      if (!mIsLayoutDirty) {
        scrollToChild(focused);
      } else {
        // The child may not be laid out yet, we can't compute the scroll yet
        mChildToScrollTo = focused;
      }
    }
    super.requestChildFocus(child, focused);
  }


  /**
   * When looking for focus in children of a scroll view, need to be a little
   * more careful not to give focus to something that is scrolled off screen.
   *
   * This is more expensive than the default {@link ViewGroup}
   * implementation, otherwise this behavior might have been made the default.
   */
  @Override
  protected boolean onRequestFocusInDescendants(int direction,                                                Rect previouslyFocusedRect) {

    // convert from forward / backward notation to up / down / left / right
    // (ugh).
    if (direction == View.FOCUS_FORWARD) {
      direction = View.FOCUS_DOWN;
    } else if (direction == View.FOCUS_BACKWARD) {
      direction = View.FOCUS_UP;
    }

    final View nextFocus = previouslyFocusedRect == null ?
      FocusFinder.getInstance().findNextFocus(this, null, direction) :
      FocusFinder.getInstance().findNextFocusFromRect(this,
        previouslyFocusedRect, direction);

    if (nextFocus == null) {
      return false;
    }

    if (isOffScreen(nextFocus)) {
      return false;
    }

    return nextFocus.requestFocus(direction, previouslyFocusedRect);
  }

  @Override
  public boolean requestChildRectangleOnScreen(View child, Rect rectangle,                                               boolean immediate) {
    // offset into coordinate space of this scroll view
    rectangle.offset(child.getLeft() - child.getScrollX(),
      child.getTop() - child.getScrollY());

    return scrollToChildRect(rectangle, immediate);
  }

  @Override
  public void requestLayout() {
    mIsLayoutDirty = true;
    super.requestLayout();
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    mIsLayoutDirty = false;
    // Give a child focus if it needs it
    if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
      scrollToChild(mChildToScrollTo);
    }
    mChildToScrollTo = null;

    // Calling this with the present values causes it to re-clam them
    scrollTo(getScrollX(), getScrollY());
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    View currentFocused = findFocus();
    if (null == currentFocused || this == currentFocused) {
      return;
    }
    // If the currently-focused view was visible on the screen when the
    // screen was at the old height, then scroll the screen to make that
    // view visible with the new screen height.
    if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
      currentFocused.getDrawingRect(mTempRect);
      offsetDescendantRectToMyCoords(currentFocused, mTempRect);
      int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
      doScrollY(scrollDelta);
    }
  }

  /**
   * Return true if child is an descendant of parent, (or equal to the parent).
   */
  private boolean isViewDescendantOf(View child, View parent) {
    if (child == parent) {
      return true;
    }

    final ViewParent theParent = child.getParent();
    return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
  }

  /**
   * Fling the scroll view
   *
   * @param velocityY The initial velocity in the Y direction. Positive
   *                  numbers mean that the finger/curor is moving down the screen,
   *                  which means we want to scroll towards the top.
   */
  public void fling(int velocityY) {
    if (getChildCount() > 0) {
      int height = getHeight() - getPaddingBottom() - getPaddingTop();
      int bottom = getChildAt(0).getHeight();

      mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, bottom - height);

      final boolean movingDown = velocityY > 0;

      View newFocused =
        findFocusableViewInMyBounds(movingDown, mScroller.getFinalY(), findFocus());
      if (newFocused == null) {
        newFocused = this;
      }

      if (newFocused != findFocus()
        && newFocused.requestFocus(movingDown ? View.FOCUS_DOWN : View.FOCUS_UP)) {
        mScrollViewMovedFocus = true;
        mScrollViewMovedFocus = false;
      }

      awakenScrollBars(mScroller.getDuration());
      invalidate();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This version also clamps the scrolling to the bounds of our child.
   */
  public void scrollTo(int x, int y) {
    // we rely on the fact the View.scrollBy calls scrollTo.
    if (getChildCount() > 0) {
      View child = getChildAt(0);
      x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
      y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
      if (x != getScrollX() || y != getScrollY()) {
        super.scrollTo(x, y);
      }
    }
  }

  private int clamp(int n, int my, int child) {
    if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- getScrollX() --|
             */
      return 0;
    }
    if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- getScrollX() --|
             */
      return child - my;
    }
    return n;
  }
}
