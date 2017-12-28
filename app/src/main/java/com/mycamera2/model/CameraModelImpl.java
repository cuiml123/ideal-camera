package com.mycamera2.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.mycamera2.CameraActivity;
import com.mycamera2.R;
import com.mycamera2.presenter.settings.CameraCapabilities;
import com.mycamera2.presenter.settings.CameraSettings;
import com.mycamera2.presenter.settings.Keys;
import com.mycamera2.presenter.settings.SettingsManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class CameraModelImpl implements ICameraModel {

    private static final String TAG = "CameraModelImpl";
    public static final String FORMAT_JPEG = ".jpg";
    public static final String FORMAT_MP4 = ".mp4";

    protected CameraDevice mCamera; //相机实例
    protected CameraCaptureSession mSession; //会话，向底层发送命令通过这个接口
    protected String mCameraId;
    protected CameraCharacteristics mCameraCharacteristics;
    protected CameraActivity mActivity;
    protected CameraManager mCameraManager; //android系统的camera2 api相机入口
    protected CameraSettings mCameraSettings;
    protected CameraCapabilities mCameraCapabilities;
    protected Rect mActiveArray;
    protected boolean mLegacyDevice;
    protected Handler mOptionHandler;
    protected HandlerThread mHandleThread;

    protected Size mPreviewSize;
    private Size mPhotoSize;

    private Surface mPreviewSurface; //传递给底层的是Surface，上层的控件是SurfaceTexture，这个要区分开
    private SurfaceTexture mSurface; //通过布局中的TextureView获取，用来构建preview使用的surface
    protected ImageReader mCaptureReader; //获取照片使用的ImageReader

    protected static final int MSG_OPEN_CAMERA = 1;
    protected static final int MSG_CLOSE_CAMERA = 2;
    protected static final int MSG_START_PREVIEW = 3;
    protected static final int MSG_STOP_PREVIEW = 4;
    protected static final int MSG_APPLY_SETTINGS = 5;
    protected static final int MSG_TAKE_PICTURE = 6;
    // Available when taking picture between AE trigger receipt and autoexposure convergence

    private int mCameraState = STATE_UNOPENED;
    private static final int STATE_CLOSED = -1;
    private static final int STATE_UNOPENED = 0;
    private static final int STATE_OPENED = 1;
    private static final int STATE_PREVIEWING = 2;
    private static final int STATE_FOCUSING = 3;
    private static final int STATE_IDEL = 4;

    private boolean mRecoderRecoding = false;
    private MediaRecorder mMediaRecorder;
    private String mVideoFileName;
    private long mVideoStartTime;
    private Size mVideoSize;

    private static class Holder {
        private static CameraModelImpl INSTANCE = new CameraModelImpl();
    }

    private CameraModelImpl() {}

    public static CameraModelImpl getInstance() {
        return Holder.INSTANCE;
    }

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
                        Log.i(TAG, "onOpened HardWard level = " + mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));
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
        SettingsManager settingsManager = mActivity.getSettingsManager();
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_IS_CURRENT_CAPTURE_MODULE)) {
            takePicture();
        } else {
            if (mRecoderRecoding) {
                stopRecoding();
            } else {
                startRecoding();
            }
        }
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
        if (mRecoderRecoding) {
            stopRecoding();
        }
        closePreviewSession();
        if (mCamera != null) {
            mCamera.close();
        }
        if (mCaptureReader != null) {
            mCaptureReader.close();
            mCaptureReader = null;
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

        /*List<Size> supportedPhotoSizes = mCameraCapabilities.getSupportedPhotoSizes();
        List<Size> supportedPreviewSizes = mCameraCapabilities.getSupportedPreviewSizes();*/
        SettingsManager settingsManager = mActivity.getSettingsManager();
        boolean widePreview = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                Keys.KEY_CURRENT_PIC_RATIO_WIDE);
        int preWidth = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                Keys.KEY_ORDINARY_PREV_WIDTH);
        int preHeight = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                Keys.KEY_ORDINARY_PREV_HEIGHT);

        int picWidth = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                Keys.KEY_ORDINARY_PIC_WIDTH);
        int picHeight = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                Keys.KEY_ORDINARY_PIC_HEIGHT);

        if (widePreview) {
            preWidth = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                    Keys.KEY_WIDE_PREV_WIDTH);
            preHeight = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                    Keys.KEY_WIDE_PREV_HEIGHT);

            picWidth = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                    Keys.KEY_WIDE_PIC_WIDTH);
            picHeight = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(),
                    Keys.KEY_WIDE_PIC_HEIGHT);
        }
        mPreviewSize = new Size(preWidth, preHeight);
        mPhotoSize = new Size(picWidth, picHeight);
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
                    Log.e(TAG, new StringBuilder().append("Capture attempt failed with reason ").append(failure.getReason()).toString());
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

    protected void takePicture() {
        try {
            final CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    Log.i(TAG, "takePicture onCaptureStarted");
                    startPreview();
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {

                }
            };
            mCaptureReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.i(TAG, "takePicture onImageAvailable");
                    try (Image image = reader.acquireNextImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        final byte[] pixels = new byte[buffer.remaining()];
                        buffer.get(pixels);
                        mOptionHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onPictureTaken(pixels);
                            }
                        });
                    }
                }
            }, mOptionHandler);
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 95);
            builder.addTarget(mCaptureReader.getSurface());
            /*builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);*/
            builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            //停止预览会话
            mSession.stopRepeating();
            //启动拍照会话
            mSession.capture(builder.build(), captureCallback, null);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Unable to run autoexposure and perform capture", ex);
        }
    }

    protected void onPictureTaken(byte[] pixels) {
        Log.i(TAG, "onPictureTaken pixels.length = " + pixels.length);
        try {
            InputStream inputStream = new ByteArrayInputStream(pixels);
            ExifInterface exif = new ExifInterface(inputStream);
            inputStream.close();
            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss", Locale.CHINESE);
            String name = "IMG_" + format.format(new Date());
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            Log.i(TAG, "onPictureTaken path = " + path + ", name = " + name);
            //保存图片文件
            OutputStream outputStream = new FileOutputStream(path + "/" + name + FORMAT_JPEG);
            outputStream.write(pixels);

            outputStream.flush();
            outputStream.close();

            //插入更新数据库
            ContentResolver resolver = mActivity.getContentResolver();

            File file = new File(path + "/" + name + FORMAT_JPEG);
            long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());

            ContentValues values = new ContentValues(11);
            values.put(MediaStore.Images.ImageColumns.TITLE, name);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, name + FORMAT_JPEG);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, 90);
            values.put(MediaStore.Images.ImageColumns.DATA, path);
            values.put(MediaStore.Images.ImageColumns.SIZE, pixels.length);
            values.put(MediaStore.MediaColumns.WIDTH, exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0));
            values.put(MediaStore.MediaColumns.HEIGHT, exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0));
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaStoreUpdateIntent.setData(Uri.fromFile(file));
            mActivity.sendBroadcast(mediaStoreUpdateIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startRecoding() {
        Log.i(TAG, "startRecoding");
        try {
            mMediaRecorder = new MediaRecorder();
            setupMediaRecorder();
            SurfaceTexture surfaceTexture =
                    ((TextureView) mActivity.findViewById(R.id.fullscreen_content)).getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            final CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(previewSurface); //预览的surface
            builder.addTarget(recordSurface); //录像的surface，MediaRecorder获取图像的图像源

            mCamera.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mCaptureReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mSession = session;
                            try {
                                //不断的请求图像，可以用来启动预览和启动录像
                                mSession.setRepeatingRequest(
                                        builder.build(), mCameraResultStateCallback, mOptionHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startRecord");
                        }
                    }, mOptionHandler);

            mMediaRecorder.start();
            mVideoStartTime = System.currentTimeMillis();
            mRecoderRecoding = true;
        } catch (Exception e) {
            Log.i(TAG, "startRecoding failed");
            e.printStackTrace();
            mRecoderRecoding = false;
        }
    }

    private void stopRecoding() {
        Log.i(TAG, "stopRecoding");
        long dateTaken = System.currentTimeMillis();
        startPreview();
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mRecoderRecoding = false;

        ContentResolver resolver = mActivity.getContentResolver();

        File file = new File(mVideoFileName);
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());

        ContentValues values = new ContentValues(11);
        values.put(MediaStore.Video.Media.TITLE, file.getName());
        values.put(MediaStore.Video.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, dateTaken / 1000);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Video.Media.WIDTH, mVideoSize.getWidth());
        values.put(MediaStore.Video.Media.HEIGHT, mVideoSize.getHeight());
        values.put(MediaStore.Video.Media.RESOLUTION, mVideoSize.getWidth() + "x" + mVideoSize.getHeight());
        values.put(MediaStore.Video.Media.DURATION, System.currentTimeMillis() - mVideoStartTime);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, dateModifiedSeconds);
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
        mActivity.sendBroadcast(mediaStoreUpdateIntent);
        mMediaRecorder = null;
        mVideoFileName = null;
    }

    //创建MediaRecorder
    private void setupMediaRecorder() throws IOException {
        Log.i(TAG, "setupMediaRecorder");
        SettingsManager settingsManager = mActivity.getSettingsManager();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINESE);
        String name = "VIDEO_" + format.format(new Date());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mVideoFileName = path + "/" + name + FORMAT_MP4;
        mMediaRecorder.setOutputFile(mVideoFileName); //输出文件
        Log.i(TAG, "setupMediaRecorder mVideoFileName = " + mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000); //视频编码比特率
        mMediaRecorder.setVideoFrameRate(30); //录像帧录
        int width = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(), Keys.KEY_ORDINARY_VIDEO_WIDTH);
        int height = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(), Keys.KEY_ORDINARY_VIDEO_HEIGHT);
        if (mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CURRENT_PIC_RATIO_WIDE)) {
            width = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(), Keys.KEY_WIDE_VIDEO_WIDTH);
            height = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL + mCamera.getId(), Keys.KEY_WIDE_VIDEO_HEIGHT);
        }
        mVideoSize = new Size(width, height);
        mMediaRecorder.setVideoSize(width, height);
        Log.i(TAG, "setupMediaRecorder setVideoSize width = " + width + ", height = " + height);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.prepare();
    }

    private void setupTimelapse() throws IOException {
        /*mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setCaptureRate(2);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();*/
    }

}
