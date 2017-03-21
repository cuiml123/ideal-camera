package com.mycamera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.mycamera2.presenter.CapturePresenter;
import com.mycamera2.presenter.ICameraPresenter;
import com.mycamera2.presenter.settings.CameraCapabilities;
import com.mycamera2.presenter.settings.SettingsManager;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity {
    private TextureView mContentView;
    private static String TAG = "CameraActivity";

    private ICameraPresenter mCameraPresenter;

    private CameraManager mCameraManager;
    private CameraManager.AvailabilityCallback mAvailabilityCallback;
    private boolean mBackCameraAvailable;
    private boolean mFrontCameraAvailable;
    private CameraDevice mCamera;
    private SurfaceTexture mSurface;

    private SettingsManager mSettingsManager;
    private DrawerLayout drawerLayout;
    private MaterialMenuDrawable materialMenu;
    private boolean isDrawerOpened;

    private int mCurrentAeState = CaptureResult.CONTROL_AE_STATE_INACTIVE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mCameraPresenter = new CapturePresenter();
        mCameraPresenter.onActivityCreate(this);

        //全屏
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        materialMenu = new MaterialMenuDrawable(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);

        toolbar.setNavigationIcon(materialMenu);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Handle your drawable state here
                if(isDrawerOpened) {
                    materialMenu.setIconState(MaterialMenuDrawable.IconState.ARROW);
                    drawerLayout.closeDrawers();
                } else {
                    materialMenu.setIconState(MaterialMenuDrawable.IconState.BURGER);
                    drawerLayout.openDrawer((ListView) findViewById(R.id.left_drawer));
                }
            }
        });

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) drawerLayout.getLayoutParams();
        param.setMargins(0, (int) getResources().getDimension(R.dimen.tool_bar_height), 0, 0);
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                materialMenu.setTransformationOffset(
                        MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                        isDrawerOpened ? 2 - slideOffset : slideOffset
                );
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                isDrawerOpened = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                isDrawerOpened = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if(newState == DrawerLayout.STATE_IDLE) {
                    if(isDrawerOpened) {
                        materialMenu.setIconState(MaterialMenuDrawable.IconState.ARROW);
                    } else {
                        materialMenu.setIconState(MaterialMenuDrawable.IconState.BURGER);
                    }
                }
            }
        });
        mContentView = (TextureView) findViewById(R.id.fullscreen_content);
        mContentView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                Log.i(TAG, "onSurfaceTextureAvailable");
                mSurface = surfaceTexture;
                mCameraPresenter.setPreviewTexture(mSurface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PackageManager.PERMISSION_GRANTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mSettingsManager = new SettingsManager(this);
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (int i = 0; i < cameraIds.length; i++) {
                CameraCharacteristics ca = mCameraManager.getCameraCharacteristics(cameraIds[i]);
                CameraCapabilities capabilities = new CameraCapabilities(ca);
                capabilities.initCameraSettings(mSettingsManager, cameraIds[i]);
                Log.i(TAG, "CameraCharacteristics = " + ca);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
            @Override
            public void onCameraAvailable(String cameraId) {
                if (Integer.parseInt(cameraId) == 0) {
                    mBackCameraAvailable = true;
                }
                if (Integer.parseInt(cameraId) == 1) {
                    mFrontCameraAvailable = true;
                }
            }

            public void onCameraUnavailable(String cameraId) {
                if (Integer.parseInt(cameraId) == 0) {
                    mBackCameraAvailable = false;
                }
                if (Integer.parseInt(cameraId) == 1) {
                    mFrontCameraAvailable = false;
                }
            }
        };
        mCameraManager.registerAvailabilityCallback(mAvailabilityCallback, new Handler(getMainLooper()));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraPresenter.openCamera("0", this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraPresenter.closeCamera();
    }

    public void resizeTexture() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mContentView.getLayoutParams();
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        int height = metric.heightPixels;   // 屏幕高度（像素）
        params.width = width;
        params.height = width* 4 / 3;
        int bottomHeight = (int) getResources().getDimension(R.dimen.bottom_bar_height);
        params.setMargins(0, height - bottomHeight - params.height, 0, bottomHeight);
        mContentView.setLayoutParams(params);
    }
}
