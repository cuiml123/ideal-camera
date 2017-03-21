package com.mycamera2.presenter.settings;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import java.util.HashMap;
import java.util.Map;

public class CameraSettings {

    public static String TAG = "CameraSettings";

    private CameraDevice mCamera;
    private Surface mPreviewSurface;
    private Surface mCaptureSurface;

    private final Map<CaptureRequest.Key<?>, Object> mDictionary;

    public CameraSettings() {
        mDictionary = new HashMap<>();
    }

    public void init(CameraDevice camera, Surface preview, Surface capture) {
        mCamera = camera;
        mPreviewSurface = preview;
        mCaptureSurface = capture;
    }

    public void setFlashMode(int flashMode) {
        mDictionary.put(CaptureRequest.FLASH_MODE, flashMode);
    }

    public void setFocusMode(int focusMode) {
        mDictionary.put(CaptureRequest.CONTROL_AF_MODE, focusMode);
    }

    public void setColorEffect(int colorEffect) {
        mDictionary.put(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
    }

    public void setWhiteBalances(int whiteBalances) {
        mDictionary.put(CaptureRequest.CONTROL_AWB_MODE, whiteBalances);
    }

    public void setSceneModes(int sceneModes) {
        mDictionary.put(CaptureRequest.CONTROL_SCENE_MODE, sceneModes);
    }

    public void setZoomValue(float zoomValue) {

    }

    public void setExposureTime(int exposureTime) {

    }

    public void setFocusPosition(int focusPosition) {

    }

    public void setFpsRange() {

    }

    /**
     * Create a {@link CaptureRequest} specialized for the specified
     * {@link CameraDevice} and targeting the given {@link Surface}s.
     *
     * @param camera The camera from which to capture.
     * @param template A {@link CaptureRequest} template defined in
     *                 {@link CameraDevice}.
     * @param targets The location(s) to draw the resulting image onto.
     * @return The request, ready to be passed to the camera framework.
     *
     * @throws CameraAccessException Upon an underlying framework API failure.
     * @throws NullPointerException If any argument is {@code null}.
     */
    public CaptureRequest createRequest(CameraDevice camera, int template, Surface... targets)
            throws CameraAccessException {
        if (camera == null) {
            throw new NullPointerException("Tried to create request using null CameraDevice");
        }

        CaptureRequest.Builder reqBuilder = camera.createCaptureRequest(template);
        for (CaptureRequest.Key<?> key : mDictionary.keySet()) {
            setRequestFieldIfNonNull(reqBuilder, key);
        }
        for (Surface target : targets) {
            if (target == null) {
                throw new NullPointerException("Tried to add null Surface as request target");
            }
            reqBuilder.addTarget(target);
        }
        return reqBuilder.build();
    }

    private <T> void setRequestFieldIfNonNull(CaptureRequest.Builder requestBuilder, CaptureRequest.Key<T> key) {
        T value = get(key);
        if (value != null) {
            requestBuilder.set(key, value);
        }
    }


    /**
     * Interrogate the current specialization of a setting.
     *
     * @param key Which setting to check.
     * @return The current selection for that setting, or {@code null} if the
     *         setting is unset or forced to the template-defined default.
     *
     * @throws NullPointerException If {@code key} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(CaptureRequest.Key<T> key) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        }
        return (T) mDictionary.get(key);
    }
}
