package com.mycamera2.view;


import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.mycamera2.R;
import com.mycamera2.presenter.settings.Keys;
import com.mycamera2.presenter.settings.SettingsManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class ButtonManager implements SettingsManager.OnSettingChangedListener {

    private static final String TAG = "ButtonManager";

    public static int BUTTON_SHUTTER = 0;
    public static int BUTTON_SWITCH_MODE = 1;
    public static int BUTTON_SWITCH_CAMERA = 2;
    public static int BUTTON_FLASH_MODE = 3;
    public static int BUTTON_HDR = 4;
    public static int BUTTON_SETTINGS = 5;

    private CircleImageView mShutterButton;
    private CircleImageView mSwitchModeButton;
    private ImageButton mFlashModeButton;
    private ImageButton mHdrButton;
    private ImageButton mSettingsButton;

    private Activity mContext;
    private ButtonListener mButtonListener;

    public ButtonManager(Activity context) {
        mContext = context;
        init();
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        final Drawable capture = mContext.getDrawable(R.mipmap.icon_capture);
        final Drawable video = mContext.getDrawable(R.mipmap.icon_video_recoding);
        if (Keys.KEY_IS_CURRENT_CAPTURE_MODULE.equals(key)) {
            if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_IS_CURRENT_CAPTURE_MODULE)) {
                mShutterButton.setImageDrawable(capture);
                mSwitchModeButton.setImageDrawable(video);
            } else {
                mShutterButton.setImageDrawable(video);
                mSwitchModeButton.setImageDrawable(capture);
            }
        }
    }

    public interface ButtonListener {
        void onShutterButtonClick();
        void onSwitchModeClick();
    }

    public void setButtonListener(ButtonListener listener) {
        mButtonListener = listener;
    }

    private void init() {
        Log.i(TAG, "init");
        mShutterButton = (CircleImageView) mContext.findViewById(R.id.shutter_button);
        mSwitchModeButton = (CircleImageView) mContext.findViewById(R.id.switch_mode);
        mFlashModeButton = (ImageButton) mContext.findViewById(R.id.flash_mode);
        mHdrButton = (ImageButton) mContext.findViewById(R.id.hdr);
        mSettingsButton = (ImageButton) mContext.findViewById(R.id.camera_param);

        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "mShutterButton onClick mButtonListener = " + mButtonListener);
                if (mButtonListener != null) {
                    mButtonListener.onShutterButtonClick();
                }
            }
        });

        mSwitchModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mButtonListener != null) {
                    mButtonListener.onSwitchModeClick();
                }
            }
        });
    }
}
