package com.mycamera2.model;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.mycamera2.CameraActivity;
import com.mycamera2.presenter.settings.CameraCapabilities;
import com.mycamera2.presenter.settings.CameraSettings;

import java.util.Arrays;
import java.util.List;


public class CameraModelImpl implements ICameraModel {

    private static final String TAG = "CameraModelImpl";
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    protected String mCameraId;
    protected CameraCharacteristics mCameraCharacteristics;
    protected CameraActivity mActivity;
    protected CameraManager mCameraManager;
    protected CameraSettings mCameraSettings;
    protected CameraCapabilities mCameraCapabilities;
    protected Rect mActiveArray;
    protected boolean mLegacyDevice;
    protected Handler mOptionHandler;
    protected HandlerThread mHandleThread;

    private Size mPreviewSize;
    private Size mPhotoSize;

    private Surface mPreviewSurface;
    private SurfaceTexture mSurface;
    private ImageReader mCaptureReader;

    protected static final int MSG_OPEN_CAMERA = 1;
    protected static final int MSG_CLOSE_CAMERA = 2;
    protected static final int MSG_START_PREVIEW = 3;
    protected static final int MSG_STOP_PREVIEW = 4;
    protected static final int MSG_APPLY_SETTINGS = 5;
    // Available when taking picture between AE trigger receipt and autoexposure convergence

    private int mCameraState = STATE_UNOPENED;
    private static final int STATE_CLOSED = -1;
    private static final int STATE_UNOPENED = 0;
    private static final int STATE_OPENED = 1;
    private static final int STATE_PREVIEWING = 2;
    private static final int STATE_FOCUSING = 3;
    private static final int STATE_IDEL = 4;

    private CaptureAvailableListener mOneshotCaptureCallback;

    private CameraDevice.StateCallback mCameraDeviceStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Log.i(TAG, "onOpened CameraDevice camera=" + camera);
                    mCamera = camera;
                    mCameraState = STATE_OPENED;
                    try {
                        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(camera.getId());
                        Log.i(TAG, "onOpened mCameraCharacteristics = " + mCameraCharacteristics);
                        mCameraCapabilities = new CameraCapabilities(mCameraCharacteristics);
                        Log.i(TAG, "onOpened mCameraCapabilities = " + mCameraCapabilities);
                        mCameraSettings = new CameraSettings();
                        mActiveArray = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                        Log.i(TAG, "onOpened mActiveArray = " + mActiveArray);
                        mLegacyDevice = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
                        Log.i(TAG, "onOpened mLegacyDevice = " + mLegacyDevice);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    setPreviewTexture(mSurface);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Log.w(TAG, "Camera device '" + camera + "' was disconnected");
                    mCameraState = STATE_CLOSED;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "Camera device encountered error code '" + error + '\'');
                }
            };

    @Override
    public void onShutter() {

    }

    @Override
    public void openCamera(String id, CameraActivity activity) {
        Log.i(TAG, "openCamera mCameraState = " + mCameraState);
        if (mHandleThread == null) {
            mHandleThread = new HandlerThread("handleThread");
            mHandleThread.start();
        }
        if (mOptionHandler == null) {
            initHandler();
        }
        mActivity = activity;
        mCameraId = id;
        if (mCameraState >= STATE_OPENED ) return;
        mOptionHandler.sendEmptyMessage(MSG_OPEN_CAMERA);

    }

    private void initHandler() {
        mOptionHandler = new Handler(mHandleThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, "handleMessage msg.what = " + msg.what);
                switch (msg.what) {
                    case MSG_OPEN_CAMERA:
                        if (mCameraManager == null) {
                            mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
                        }
                        try {
                            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(mActivity,
                                        new String[]{Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PackageManager.PERMISSION_GRANTED);
                                return;
                            }
                            Log.i(TAG, "MSG_OPEN_CAMERA mCameraId = " + mCameraId);
                            mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mOptionHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MSG_CLOSE_CAMERA:
                        if (mCamera != null) {
                            mCamera.close();
                            mCameraState = STATE_CLOSED;
                        }
                        break;
                    case MSG_START_PREVIEW:
                        startPreview();
                        break;
                    case MSG_STOP_PREVIEW:
                        closePreviewSession();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void closeCamera() {
        Log.i(TAG, "closeCamera start");
        closePreviewSession();
        if (mCamera != null) {
            mCamera.close();
        }
        mCameraState = STATE_CLOSED;
        Log.i(TAG, "closeCamera end");
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surface) {
        Log.i(TAG, "setPreviewTexture");
        mSurface = surface;
        if (mSurface == null) {
            Log.i(TAG, "surface is not ready");
            return;
        }
        if (mCameraState < STATE_OPENED) {
            Log.i(TAG, "camera is not ready");
            return;
        }

        if (mSession != null) {
            closePreviewSession();
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.resizeTexture();
            }
        });

        List<Size> supportedPhotoSizes = mCameraCapabilities.getSupportedPhotoSizes();
        List<Size> supportedPreviewSizes = mCameraCapabilities.getSupportedPreviewSizes();
        mPreviewSize = supportedPreviewSizes.get(0);
        mPhotoSize = supportedPhotoSizes.get(0);
        Log.i(TAG, "mPhotoSize = " + mPhotoSize);
        Log.i(TAG, "mPreviewSize = " + mPreviewSize);

        mSurface.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        if (mPreviewSurface != null) {
            mPreviewSurface.release();
        }
        mPreviewSurface = new Surface(mSurface);

        if (mCaptureReader != null) {
            mCaptureReader.close();
        }
        mCaptureReader = ImageReader.newInstance(
                mPhotoSize.getWidth(),
                mPhotoSize.getHeight(),
                ImageFormat.JPEG, 1);

        //mCameraSettings.init(mCamera, mPreviewSurface, mCaptureReader.getSurface());
        try {
            mCamera.createCaptureSession(
                    Arrays.asList(mPreviewSurface, mCaptureReader.getSurface()),
                    mCameraPreviewStateCallback,
                    mOptionHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Failed to create camera capture session", ex);
        }
    }

    @Override
    public void startPreview() {
        if (mSession == null) return;
        startPreviewSession();
    }


    private void closePreviewSession() {
        Log.i(TAG, "closePreviewSession");
        try {
            if (mSession != null) {
                mSession.abortCaptures();
                mSession = null;
            }
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Failed to close existing camera capture session", ex);
        }
    }


    // This callback monitors our camera session (i.e. our transition into and out of preview).
    private CameraCaptureSession.StateCallback mCameraPreviewStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mSession = session;
                    startPreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // TODO: Invoke a callback
                    Log.e(TAG, "Failed to configure the camera for capture");
                }

                @Override
                public void onActive(CameraCaptureSession session) {

                }
            };

    // This callback monitors requested captures and notifies any relevant callbacks.
    private CameraCaptureSession.CaptureCallback mCameraResultStateCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult result) {
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    //Log.i(TAG, "mCameraResultStateCallback onCaptureCompleted");
                    //mCameraState = STATE_PREVIEWING;
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                            CaptureFailure failure) {
                    Log.e(TAG, "Capture attempt failed with reason " + failure.getReason());
                }};

    private void startPreviewSession() {
        Log.i(TAG, "startPreviewSession");
        try {
            //CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //builder.addTarget(mPreviewSurface);
            mSession.setRepeatingRequest(
                    mCameraSettings.createRequest(mCamera, CameraDevice.TEMPLATE_PREVIEW, mPreviewSurface),
                    //builder.build(),
                    /*listener*/mCameraResultStateCallback,
                    /*handler*/mOptionHandler);
        } catch(CameraAccessException ex) {
            Log.w(TAG, "Unable to start preview", ex);
        }
    }

    public Rect faceForConvertCoordinate (Rect rect){
        if (mActiveArray == null) {
            return null;
        }

        int sensorWidth = mActiveArray.width();
        int sendorHeight = mActiveArray.height();
        int left = rect.left * 2000/sensorWidth - 1000;
        int top = rect.top * 2000/sendorHeight - 1000;
        int right = rect.right * 2000/sensorWidth - 1000;
        int bottom = rect.bottom * 2000/sendorHeight -1000;
        return new Rect(left,top,right,bottom);
    }

    private static abstract class CaptureAvailableListener
            extends CameraCaptureSession.CaptureCallback
            implements ImageReader.OnImageAvailableListener {}
}
