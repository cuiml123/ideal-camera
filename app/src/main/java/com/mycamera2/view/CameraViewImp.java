package com.mycamera2.view;


import android.graphics.SurfaceTexture;

import com.mycamera2.presenter.ICameraPresenter;

public abstract class CameraViewImp implements ICameraView {

    private static final String TAG = "CameraViewImp";
    protected ICameraPresenter mCameraPresenter;


    @Override
    public void onShutterClick() {

    }

    public void setPreviewTexture(SurfaceTexture mSurface) {
        mCameraPresenter.setPreviewTexture(mSurface);

    }

}
