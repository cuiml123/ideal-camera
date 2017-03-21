package com.mycamera2.view;


import com.mycamera2.presenter.CapturePresenter;

public class CaptureView extends CameraViewImp {

    private static final String TAG = "CaptureView";
    public CaptureView() {
        mCameraPresenter = new CapturePresenter();
    }

}
