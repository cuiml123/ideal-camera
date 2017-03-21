package com.mycamera2.model;


import android.graphics.SurfaceTexture;

import com.mycamera2.CameraActivity;

public interface ICameraModel {

    void onShutter();

    void openCamera(String id, CameraActivity activity);

    void closeCamera();

    void startPreview();

    void setPreviewTexture(SurfaceTexture surface);
}
