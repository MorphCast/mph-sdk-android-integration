package com.morphcast.poc_demo.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.morphcast.poc_demo.utils.GlobalObject;
import com.morphcast.poc_demo.R;

import java.io.IOException;
import java.util.List;

import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by Administrator on 8/7/2016.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {

    public static final String TAG = CameraFragment.class.getSimpleName();
    public static final String CAMERA_ID_KEY = "camera_id";
    public static final String CAMERA_FLASH_KEY = "flash_mode";
    public static final String CAMERA_CAPTURE_KEY = "capture_mode";
    public static final String IMAGE_INFO = "image_info";
    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

    private boolean m_bSurfaceDestroyed;
    private int mCameraID;
    private String mFlashMode;
    private static String mCaptureMode;
    private Camera mCamera = null;
    private SquareCameraPreview mPreviewView;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsSafeToTakePhoto = false;
    private ImageParameters mImageParameters;
    private CameraOrientationListener mOrientationListener;

    private RelativeLayout rootView;
    private TextView m_tvAge;
    private TextView m_tvGender;
    private boolean m_bSurfaceRemoved;
    private boolean m_bShouldRemoveSurface;

    public static Fragment newInstance() {
        return new CameraFragment();
    }


    private byte[] mGrayData;
    private boolean m_bEngineFlag;

    private Context context;

    public CameraFragment() {
        
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mOrientationListener = new CameraOrientationListener(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore your state here because a double rotation with this fragment
        // in the backstack will cause improper state restoration
        // onCreate() -> onSavedInstanceState() instead of going through onCreateView()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        mCameraID = getFrontCameraID();
        mFlashMode = CameraSettingPreferences.getCameraFlashMode(context);
        mImageParameters = new ImageParameters();
        mCaptureMode = CameraSettingPreferences.getCaptureMode(context);
        m_bSurfaceDestroyed = true;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (RelativeLayout) inflater.inflate(R.layout.squarecamera_fragment_camera, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOrientationListener.enable();

        m_bSurfaceRemoved = false;
        mPreviewView =  view.findViewById(R.id.camera_preview_view);
        m_tvAge = view.findViewById(R.id.tv_age);
        m_tvGender = view.findViewById(R.id.tv_gender);
        GlobalObject.g_tvAge = m_tvAge;
        GlobalObject.g_tvGender = m_tvGender;

        mPreviewView.getHolder().addCallback(CameraFragment.this);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt(CAMERA_ID_KEY, mCameraID);
        outState.putString(CAMERA_FLASH_KEY, mFlashMode);
        outState.putParcelable(IMAGE_INFO, mImageParameters);
        outState.putString(CAMERA_CAPTURE_KEY, mCaptureMode);
        super.onSaveInstanceState(outState);
    }

    private void getCamera(int cameraID) {
        try {
            mCamera = Camera.open(cameraID);
            mPreviewView.setCamera(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Restart the camera preview
     */
    private void restartPreview() {
        if (mCamera != null) {
            stopCameraPreview();
            mCamera.release();
            mCamera = null;
        }

        getCamera(mCameraID);
        if(!m_bSurfaceDestroyed)
            startCameraPreview();
    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        if (mCamera != null){

            determineDisplayOrientation();
            setupCamera();
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();

                setSafeToTakePhoto(true);
                setCameraFocusReady(true);
            }
            catch (RuntimeException ex) {
                ex.printStackTrace();
                m_bShouldRemoveSurface = false;
                Toast.makeText(context, "Can't start camera preview", Toast.LENGTH_LONG).show();
                ((CameraActivity)context).finish();
            }
            catch (IOException e) {
                Log.d(TAG, "Can't start camera preview due to IOException " + e);
                e.printStackTrace();
                Toast.makeText(context, "Can't start camera preview", Toast.LENGTH_LONG).show();
                m_bShouldRemoveSurface = false;
                ((CameraActivity)context).finish();
            }
        } else {
            Toast.makeText(context, "Can't start camera preview", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stop the camera preview
     */
    private void stopCameraPreview() {
        setSafeToTakePhoto(false);
        setCameraFocusReady(false);

        // Nulls out callbacks, stops face detection
        mCamera.stopPreview();
        mPreviewView.setCamera(null);
    }

    private void setSafeToTakePhoto(final boolean isSafeToTakePhoto) {
        mIsSafeToTakePhoto = isSafeToTakePhoto;
    }

    private void setCameraFocusReady(final boolean isFocusReady) {
        if (this.mPreviewView != null) {
            mPreviewView.setIsFocusReady(isFocusReady);
        }
    }

    private void determineDisplayOrientation() {

        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraID, cameraInfo);

            // Clockwise rotation needed to align the window display to the natural position
            int rotation = ((CameraActivity)context).getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;

            switch (rotation) {
                case Surface.ROTATION_0: {
                    degrees = 0;
                    break;
                }
                case Surface.ROTATION_90: {
                    degrees = 90;
                    break;
                }
                case Surface.ROTATION_180: {
                    degrees = 180;
                    break;
                }
                case Surface.ROTATION_270: {
                    degrees = 270;
                    break;
                }
            }

            int displayOrientation;

            // CameraInfo.Orientation is the angle relative to the natural position of the device
            // in clockwise rotation (angle that is rotated clockwise from the natural position)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // Orientation is angle of rotation when facing the camera for
                // the camera image to match the natural orientation of the device
                displayOrientation = (cameraInfo.orientation + degrees) % 360;
                displayOrientation = (360 - displayOrientation) % 360;
            } else {
                displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
            }

            mImageParameters.mDisplayOrientation = displayOrientation;
            mImageParameters.mLayoutOrientation = degrees;

            mCamera.setDisplayOrientation(mImageParameters.mDisplayOrientation);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Setup the camera parameters
     */
    private void setupCamera() {
        // Never keep a global parameters
      //  mCamera.cancelAutoFocus();
        try {
            Camera.Parameters parameters = mCamera.getParameters();

            Log.d("cameraPara", parameters.toString());
            Camera.Size bestPreviewSize = determineBestPreviewSize(parameters);
            Camera.Size bestPictureSize = determineBestPictureSize(parameters);


            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
            parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

            // Set continuous picture focus, if it's supported
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

//            final View changeCameraFlashModeBtn = getView().findViewById(R.id.change_flash);
            List<String> flashModes = parameters.getSupportedFlashModes();
            if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            } else {
            }

            // Lock in the changes
            mCamera.setParameters(parameters);
        }catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    private Camera.Size determineBestPreviewSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPreviewSizes(), PREVIEW_SIZE_MAX_WIDTH);
    }

    private Camera.Size determineBestPictureSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPictureSizes(), PICTURE_SIZE_MAX_WIDTH);
    }

    private Camera.Size determineBestSize(List<Camera.Size> sizes, int widthThreshold) {
        Camera.Size bestSize = null;
        Camera.Size size;
        int numOfSizes = sizes.size();
        for (int i = 0; i < numOfSizes; i++) {
            size = sizes.get(i);
            boolean isDesireRatio = (size.width / 4) == (size.height / 3);
            boolean isBetterSize = (bestSize == null) || size.width > bestSize.width;

            if (isDesireRatio && isBetterSize) {
                bestSize = size;
            }
        }

        if (bestSize == null) {
            Log.d(TAG, "cannot find the best camera size");
            return sizes.get(sizes.size()-1 );
        }

        return bestSize;
    }

    private int getFrontCameraID() {
        PackageManager pm = ((CameraActivity)context).getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        return getBackCameraID();
    }

    private int getBackCameraID() {
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * Take a picture
     */
    private void takePicture() {

//        if (mCamera == null){
//            Toast.makeText(context, "Camera is not opened, please wait and try again.", Toast.LENGTH_LONG).show();
//            return;
//        }
//        boolean bAutoFocus = false;
//        try {
//            bAutoFocus =  mCamera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO);
//        }catch (RuntimeException ex){
//            ex.printStackTrace();
//        }
//        final Camera.ShutterCallback shutterCallback = null;
//        final Camera.PictureCallback raw = null;
//
//        if(!bAutoFocus) {
//            if (mIsSafeToTakePhoto) {
//                setSafeToTakePhoto(false);
//                mCamera.takePicture(shutterCallback, raw, postView);
//            }
//        } else {
//            try {
//                mPreviewView.mCamera.autoFocus(new AutoFocusCallback() {
//                    @Override
//                    public void onAutoFocus(boolean success, Camera camera) {
//
//                        if (mIsSafeToTakePhoto) {
//                            setSafeToTakePhoto(false);
//                            try {
//                                mCamera.takePicture(shutterCallback, raw, postView);
//                            } catch (RuntimeException ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//                    }
//                });
//            }catch (RuntimeException ex){
//                ex.printStackTrace();
//            }
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCamera == null) {
            addPreview();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void releaseCamera() {
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraSettingPreferences.saveCameraFlashMode(context, mFlashMode);

        releaseCamera();
        mOrientationListener.disable();

        if(mPreviewView != null && m_bShouldRemoveSurface) {
            rootView.removeView(mPreviewView);
            m_bSurfaceRemoved = true;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        m_bShouldRemoveSurface = true;
        getCamera(mCameraID);
        startCameraPreview();
        mCamera.setPreviewCallback(previewCallback);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // The surface is destroyed with the visibility of the SurfaceView is set to View.Invisible
        m_bSurfaceDestroyed = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case 1:
                Uri imageUri = data.getData();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
    /**
     * A picture has been taken
     * @param data
     * @param camera
     */
    Camera.PictureCallback postView  =  new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, final Camera camera) {


//            Bitmap bmp = ImageUtil.decodeSampledBitmapFromByte(context, data);
//            ImageUtil.saveImage(bmp);
//            bmp.recycle();
//            bmp = null;

            mCamera.startPreview();
            mCamera.setPreviewCallback(previewCallback);
            setSafeToTakePhoto(true);
        }

    };

    /**
     * When orientation changes, onOrientationChanged(int) of the listener will be called
     */
    private static class CameraOrientationListener extends OrientationEventListener {

        private int mCurrentNormalizedOrientation;
        private int mRememberedNormalOrientation;

        public CameraOrientationListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation != ORIENTATION_UNKNOWN) {
                mCurrentNormalizedOrientation = normalize(orientation);
            }
        }

        /**
         * @param degrees Amount of clockwise rotation from the device's natural position
         * @return Normalized degrees to just 0, 90, 180, 270
         */
        private int normalize(int degrees) {
            if (degrees > 315 || degrees <= 45) {
                return 0;
            }

            if (degrees > 45 && degrees <= 135) {
                return 90;
            }

            if (degrees > 135 && degrees <= 225) {
                return 180;
            }

            if (degrees > 225 && degrees <= 315) {
                return 270;
            }

            throw new RuntimeException("The physics as we know them are no more. Watch out for anomalies.");
        }

        public void rememberOrientation() {
            mRememberedNormalOrientation = mCurrentNormalizedOrientation;
        }

        public int getRememberedNormalOrientation() {
            rememberOrientation();
            return mRememberedNormalOrientation;
        }
    }

    private Camera.PreviewCallback  previewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data,final Camera cam)
        {


            if (m_bEngineFlag)
                return;

            new Thread(new Runnable() {
                @Override
                public void run() {

                    m_bEngineFlag = true;

                    Camera.Size previewSize = cam.getParameters().getPreviewSize();
                    int height = previewSize.width;
                    int width = previewSize.height;

                    if(data != null && (data.length == (int)(width * height * 1.5f))) {

                        if (mGrayData == null) {
                            mGrayData = new byte[previewSize.width * previewSize.height];
                        }

                        mCamera.addCallbackBuffer(data);

                    }
                    if (GlobalObject.g_flag) {
                        GlobalObject.g_rotation = getPhotoRotation();
                        GlobalObject.g_data = data;
                        GlobalObject.g_cam = cam;
                    }
                    GlobalObject.g_flag = false;
                    m_bEngineFlag = false;
                }
            }).start();


        }
    };

    private int getPhotoRotation() {
        int rotation;
        int orientation = mOrientationListener.getRememberedNormalOrientation();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {
            rotation = (info.orientation + orientation) % 360;
        }

        return rotation;
    }

    public void addPreview() {
        restartPreview();

        if(m_bSurfaceRemoved) {

            rootView.addView(mPreviewView);
        }
    }

}
