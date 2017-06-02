package com.mycamera2.presenter;

import com.mycamera2.model.CameraModelImpl;

public class CapturePresenter extends CameraPresenterImpl {

    public CapturePresenter() {
        mCameraModel = CameraModelImpl.getInstance();
    }

}
