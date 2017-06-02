package com.mycamera2.view;


import com.mycamera2.presenter.VideoPresenter;

public class VideoView extends CameraViewImp {

    private static final String TAG = "VideoView";

    public VideoView() {
        mCameraPresenter = new VideoPresenter();
    }

    @Override
    public void onShutterClick() {
        super.onShutterClick();
    }
}
