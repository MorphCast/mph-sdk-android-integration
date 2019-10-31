package com.morphcast.poc_demo.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 8/7/2016.
 */
public class SquareCameraPreview extends SurfaceView {

    public static final String TAG = SquareCameraPreview.class.getSimpleName();
    private static final int INVALID_POINTER_ID = -1;
    private static final int ZOOM_OUT = 0;
    private static final int ZOOM_IN = 1;
    private static final int ZOOM_DELTA = 1;
    private static final int FOCUS_SQR_SIZE = 100;
    private static final int FOCUS_MAX_BOUND = 1000;
    private static final int FOCUS_MIN_BOUND = -FOCUS_MAX_BOUND;
    private static final double ASPECT_RATIO = 3.0 / 4.0;
    public Camera mCamera;
    private float mLastTouchX;
    private float mLastTouchY;
    // For scaling
    private int mMaxZoom;
    private boolean mIsZoomSupported;
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mScaleFactor = 1;
    private ScaleGestureDetector mScaleDetector;

    // For focus
    private boolean mIsFocus;
    private boolean mIsFocusReady;
    private Camera.Area mFocusArea;
    private ArrayList<Camera.Area> mFocusAreas;

    private boolean drawingViewSet = false;
    Rect touchRect;

    public SquareCameraPreview(Context context) {
        super(context);
        init(context);
    }

    public SquareCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SquareCameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mFocusArea = new Camera.Area(new Rect(), 1000);
        mFocusAreas = new ArrayList<>();
        mFocusAreas.add(mFocusArea);
    }

    /**
     * Measure the view and its content to determine the measured width and the
     * measured height
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        final boolean isPortrait =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (isPortrait) {
            if (width > height * ASPECT_RATIO) {
                width = (int) (height * ASPECT_RATIO + 0.5);
            } else {
                height = (int) (width / ASPECT_RATIO + 0.5);
            }
        } else {
            if (height > width * ASPECT_RATIO) {
                height = (int) (width * ASPECT_RATIO + 0.5);
            } else {
                width = (int) (height / ASPECT_RATIO + 0.5);
            }
        }

        setMeasuredDimension(width, height);
    }

    public int getViewWidth() {
        return getWidth();
    }

    public int getViewHeight() {
        return getHeight();
    }

    public void setCamera(Camera camera) {
        mCamera = camera;

        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            mIsZoomSupported = params.isZoomSupported();
            if (mIsZoomSupported) {
                mMaxZoom = params.getMaxZoom();
            }
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mIsFocus = true;

                mLastTouchX = event.getX();
                mLastTouchY = event.getY();

                mActivePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mIsFocus && mIsFocusReady) {
                    if (mCamera == null){
                        Toast.makeText(getContext(), "Can't get Camera correctly. Please try it again.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        try {
                            handleFocus(mCamera.getParameters());
                        }catch (RuntimeException ex){
                            ex.printStackTrace();
                        }
                    }
                }
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                try {
                    mCamera.cancelAutoFocus();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
                mIsFocus = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
        }

        return true;
    }

    private void handleZoom(Camera.Parameters params) {
        int zoom = params.getZoom();
        if (mScaleFactor == ZOOM_IN) {
            if (zoom < mMaxZoom) zoom += ZOOM_DELTA;
        } else if (mScaleFactor == ZOOM_OUT) {
            if (zoom > 0) zoom -= ZOOM_DELTA;
        }
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    private void handleFocus(Camera.Parameters params) {
        final float x = mLastTouchX;
        final float y = mLastTouchY;


        touchRect = new Rect(
                (int)(x - 70),
                (int)(y - 70),
                (int)(x + 70),
                (int)(y + 70));


        final Rect targetFocusRect = new Rect(
                touchRect.left * 2000/this.getWidth() - 1000,
                touchRect.top * 2000/this.getHeight() - 1000,
                touchRect.right * 2000/this.getWidth() - 1000,
                touchRect.bottom * 2000/this.getHeight() - 1000);



    //   if (!setFocusBound(x, y)) return;

        List<String> supportedFocusModes = params.getSupportedFocusModes();

        if (supportedFocusModes != null
                && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {

            doTouchFocus(targetFocusRect);

        }

//            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//            params.setFocusAreas(mFocusAreas);
//            mCamera.setParameters(params);



//            mCamera.autoFocus(new Camera.AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean success, Camera camera) {
//
//                    if(success){
//                        drawingView.setHaveTouch(true, touchRect, true);
//                        drawingView.invalidate();
//                        // Callback when the auto focus completes
//                    }
//                }
//            });
        }

//    }



    public void setIsFocusReady(final boolean isFocusReady) {
        mIsFocusReady = isFocusReady;
    }

    private boolean setFocusBound(float x, float y) {
        int left = (int) (x - FOCUS_SQR_SIZE / 2);
        int right = (int) (x + FOCUS_SQR_SIZE / 2);
        int top = (int) (y - FOCUS_SQR_SIZE / 2);
        int bottom = (int) (y + FOCUS_SQR_SIZE / 2);

        if (FOCUS_MIN_BOUND > left || left > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > right || right > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > top || top > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > bottom || bottom > FOCUS_MAX_BOUND) return false;

        mFocusArea.rect.set(left, top, right, bottom);

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            try {
                mScaleFactor = (int) detector.getScaleFactor();
                handleZoom(mCamera.getParameters());
            }catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            return true;
        }
    }


    public void doTouchFocus(final Rect tfocusRect) {

        try {

            final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);

            Camera.Parameters para = mCamera.getParameters();
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            mCamera.setParameters(para);

            mCamera.autoFocus(myAutoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Unable to autofocus");
        }
    }

    /**
     * AutoFocus callback
     */
    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean success, Camera camera) {


        }
    };


}
