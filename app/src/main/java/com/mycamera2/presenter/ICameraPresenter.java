package com.mycamera2.presenter;


import android.graphics.SurfaceTexture;

import com.mycamera2.CameraActivity;

public interface ICameraPresenter {

    void onShutterButtonClick();

    void openCamera(String id, CameraActivity activity);

    void setPreviewTexture(SurfaceTexture surface);

    void onActivityCreate(CameraActivity activity);

    void closeCamera();
}
