package com.mycamera2.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

public class PreviewOverLay extends View {

    private static final String TAG = "CAM_PreviewOverLay";

    private int mSlop;
    private GestureDetector mSwipeGesture;

    private FrameLayout mLeftDrawer;
    private FrameLayout mRightDrawer;
    private FrameLayout mSwipeHandler;
    private boolean mSwipeDetected = false;

    public PreviewOverLay(Context context) {
        super(context);
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mSwipeGesture = new GestureDetector(getContext(), new SwipeGesture());
    }

    public PreviewOverLay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mSwipeGesture = new GestureDetector(getContext(), new SwipeGesture());
    }

    public PreviewOverLay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLeftDrawer(FrameLayout drawer) {
        mLeftDrawer = drawer;
    }

    public void setRightDrawer(FrameLayout drawer) {
        Log.i(TAG, "setRightDrawer drawer = " + drawer);
        mRightDrawer = drawer;
        final GestureDetector rightDetector =
                new GestureDetector(getContext(), new DrawerSwipeGesture(mRightDrawer, false));
        mRightDrawer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                rightDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mSwipeHandler = null;
            mSwipeDetected = false;
            return true;
        }

        if (mSwipeHandler != null && mSwipeHandler == mRightDrawer) {
            mSwipeHandler.setX( - mRightDrawer.getWidth() + event.getX());
        } else if (mSwipeHandler != null && mSwipeHandler == mLeftDrawer) {
            mSwipeHandler.setX(mLeftDrawer.getWidth() + event.getX());
        } else if (!mSwipeDetected) {
            mSwipeGesture.onTouchEvent(event);
        }

        return true;
    }

    private class SwipeGesture extends GestureDetector.SimpleOnGestureListener {//单手指操作
        private MotionEvent mDown;
        @Override //双击
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int deltaX = (int) (e2.getX() - mDown.getX());
            int deltaY = (int) (e2.getY() - mDown.getY());
            if (e2.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                    if (deltaX >= Math.abs(deltaY)) {
                        // Swipe right.
                        setSwipeHandler(mRightDrawer);
                    } else if (deltaX <= -Math.abs(deltaY)) {
                        // Swipe left.
                        setSwipeHandler(mLeftDrawer);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            mDown = MotionEvent.obtain(ev);
            return false;
        }
    }

    private void setSwipeHandler(FrameLayout drawer) {
        Log.i(TAG, "setSwipeHandler drawer = " + drawer);
        mSwipeDetected = true;
        mSwipeHandler = drawer;
        if (mSwipeHandler != null) {
            mSwipeHandler.setVisibility(VISIBLE);
        }
    }


    private class DrawerSwipeGesture extends GestureDetector.SimpleOnGestureListener {

        private MotionEvent mDown;
        private boolean mIsLeftDrawer;
        private FrameLayout mDrawer;

        public DrawerSwipeGesture(FrameLayout drawer, boolean isLeft) {
            mDrawer = drawer;
            mIsLeftDrawer = isLeft;
        }
        @Override //双击
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int deltaX = (int) (e2.getX() - mDown.getX());
            int deltaY = (int) (e2.getY() - mDown.getY());
            if (e2.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                    if (deltaX >= Math.abs(deltaY)) {
                        // Swipe right.
                    } else if (deltaX <= -Math.abs(deltaY)) {
                        // Swipe left.
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            mDown = MotionEvent.obtain(ev);
            return false;
        }
    }
}
