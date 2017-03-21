package com.mycamera2.view;


import android.graphics.SurfaceTexture;

public interface ICameraView {

    void onShutterClick();

    void setPreviewTexture(SurfaceTexture mSurface);
}
