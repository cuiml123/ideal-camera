package com.mycamera2.presenter;

import com.mycamera2.model.CameraModelImpl;

public class VideoPresenter extends CameraPresenterImpl {

    public VideoPresenter() {
        mCameraModel = CameraModelImpl.getInstance();
    }
}
