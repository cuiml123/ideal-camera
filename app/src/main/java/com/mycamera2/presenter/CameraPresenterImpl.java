package com.mycamera2.presenter;

import android.graphics.SurfaceTexture;

import com.mycamera2.CameraActivity;
import com.mycamera2.model.ICameraModel;
import com.mycamera2.view.ICameraView;


public  abstract class CameraPresenterImpl implements ICameraPresenter {

    protected ICameraModel mCameraModel;
    protected ICameraView mCameraView;

    @Override
    public void handleClickEvent() {

    }

    @Override
    public void openCamera(String id, CameraActivity activity) {
        mCameraModel.openCamera(id, activity);
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surface) {
        mCameraModel.setPreviewTexture(surface);
    }

    @Override
    public void onActivityCreate(CameraActivity activity) {
        openCamera("0", activity);
    }

    @Override
    public void closeCamera() {
        mCameraModel.closeCamera();
    }
}
