package com.mycamera2.presenter.settings;


import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AE;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AF;
import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;
import static android.hardware.camera2.CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM;
import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;
import static android.hardware.camera2.CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_EDOF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_MACRO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_SHADE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_TWILIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_AQUA;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_MONO;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_SEPIA;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_ACTION;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_BARCODE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_BEACH;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_HDR;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_NIGHT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_PARTY;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SNOW;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SPORTS;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_SUNSET;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_THEATRE;
import static android.hardware.camera2.CameraMetadata.STATISTICS_SCENE_FLICKER_50HZ;
import static android.hardware.camera2.CameraMetadata.STATISTICS_SCENE_FLICKER_60HZ;
import static android.hardware.camera2.CameraMetadata.STATISTICS_SCENE_FLICKER_NONE;
import static com.mycamera2.presenter.settings.CameraCapabilities.Antibanding.*;
import static com.mycamera2.presenter.settings.CameraCapabilities.Antibanding.OFF;
import static com.mycamera2.presenter.settings.CameraCapabilities.ColorEffect.*;
import static com.mycamera2.presenter.settings.CameraCapabilities.FocusMode.*;
import static com.mycamera2.presenter.settings.CameraCapabilities.SceneMode.*;
import static com.mycamera2.presenter.settings.CameraCapabilities.WhiteBalance.*;

public class CameraCapabilities {

    private static final String TAG = "CameraCapabilities";
    CameraCharacteristics mCharacteristics;
    protected ArrayList<int[]> mSupportedPreviewFpsRange = new ArrayList<int[]>();
    protected ArrayList<Size> mSupportedPreviewSizes = new ArrayList<Size>();
    protected TreeSet<Integer> mSupportedPreviewFormats = new TreeSet<Integer>();
    protected ArrayList<Size> mSupportedVideoSizes = new ArrayList<Size>();
    protected ArrayList<Size> mSupportedPhotoSizes = new ArrayList<Size>();
    protected TreeSet<Integer> mSupportedPhotoFormats = new TreeSet<Integer>();

    protected final EnumSet<SceneMode> mSupportedSceneModes = EnumSet.noneOf(SceneMode.class);
    protected final EnumSet<FlashMode> mSupportedFlashModes = EnumSet.noneOf(FlashMode.class);
    protected final EnumSet<FocusMode> mSupportedFocusModes = EnumSet.noneOf(FocusMode.class);
    protected final EnumSet<WhiteBalance> mSupportedWhiteBalances =
            EnumSet.noneOf(WhiteBalance.class);
    protected final EnumSet<Feature> mSupportedFeatures = EnumSet.noneOf(Feature.class);
    protected final EnumSet<Antibanding> mSupportedAntibanding= EnumSet.noneOf(Antibanding.class);
    protected final EnumSet<ColorEffect> mSupportedColorEffects = EnumSet.noneOf(ColorEffect.class);
    protected Size mPreferredPreviewSizeForVideo;
    protected int mMinExposureCompensation;
    protected int mMaxExposureCompensation;
    protected float mExposureCompensationStep;
    protected int mMaxNumOfFacesSupported;
    protected int mMaxNumOfFocusAreas;
    protected int mMaxNumOfMeteringArea;
    protected Rect mActiveArray;
    protected float mMaxZoomRatio;
    protected float mHorizontalViewAngle;
    protected float mVerticalViewAngle;
    private final Stringifier mStringifier = new Stringifier();


    public CameraCapabilities(CameraCharacteristics characteristics) {
        mCharacteristics = characteristics;
        init();
    }

    private void init() {
        StreamConfigurationMap configurationMap = mCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP);
        mSupportedPreviewSizes.addAll(Arrays.asList(configurationMap.getOutputSizes(SurfaceTexture.class)));
        for (int format : configurationMap.getOutputFormats()) {
            mSupportedPreviewFormats.add(format);
        }

        // TODO: We only support MediaRecorder video capture
        mSupportedVideoSizes.addAll(Arrays.asList(configurationMap.getOutputSizes(MediaRecorder.class)));

        // TODO: We only support JPEG image capture
        mSupportedPhotoSizes.addAll(Arrays.asList(configurationMap.getOutputSizes(ImageFormat.JPEG)));
        mSupportedPhotoFormats.addAll(mSupportedPreviewFormats);
        mActiveArray = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        buildSceneModes(mCharacteristics);
        buildFlashModes(mCharacteristics);
        buildFocusModes(mCharacteristics);
        buildWhiteBalances(mCharacteristics);

        buildAntibandingModes(mCharacteristics);

        buildColorEffects(mCharacteristics);

        // TODO: Populate mSupportedFeatures

        // TODO: Populate mPreferredPreviewSizeForVideo

        Range<Integer> ecRange = mCharacteristics.get(CONTROL_AE_COMPENSATION_RANGE);
        mMinExposureCompensation = ecRange.getLower();
        mMaxExposureCompensation = ecRange.getUpper();

        Rational ecStep = mCharacteristics.get(CONTROL_AE_COMPENSATION_STEP);
        mExposureCompensationStep = (float) ecStep.getNumerator() / ecStep.getDenominator();

        mMaxNumOfFacesSupported = mCharacteristics.get(STATISTICS_INFO_MAX_FACE_COUNT);
        mMaxNumOfMeteringArea = mCharacteristics.get(CONTROL_MAX_REGIONS_AE);

        mMaxZoomRatio = mCharacteristics.get(SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        // TODO: Populate mHorizontalViewAngle
        // TODO: Populate mVerticalViewAngle
        // TODO: Populate mZoomRatioList
        // TODO: Populate mMaxZoomIndex

        if (supports(FocusMode.AUTO)) {
            mMaxNumOfFocusAreas = mCharacteristics.get(CONTROL_MAX_REGIONS_AF);
            if (mMaxNumOfFocusAreas > 0) {
                mSupportedFeatures.add(Feature.FOCUS_AREA);
            }
        }
        if (mMaxNumOfMeteringArea > 0) {
            mSupportedFeatures.add(Feature.METERING_AREA);
        }

        if (mMaxZoomRatio > mCharacteristics.get(SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) {
            mSupportedFeatures.add(Feature.ZOOM);
        }

        // TODO: Detect other features
    }

    public void initCameraSettings(SettingsManager settingsManager, String cameraId) {
        // 初始化拍照尺寸和预览尺寸
        initSizeSettings(settingsManager, cameraId);

        // 初始化flash mode
        mSupportedFlashModes.toArray()[0].toString();
    }

    private void initSizeSettings(SettingsManager settingsManager, String cameraId) {
        Size wideScreenPicSize = new Size(0,0);
        Size ordinaryScreenPicSize = new Size(0,0);
        Size wideScreenPrevSize = new Size(0,0);
        Size ordinaryScreenPrevSize = new Size(0,0);
        for (int i = 0; i < mSupportedPhotoSizes.toArray().length; i++) {
            Size size = mSupportedPhotoSizes.get(i);
            int height = size.getHeight();
            int width = size.getWidth();
            float ratio = width / height;
            if (ratio > 1.77 && ratio < 1.78
                    && width * height > wideScreenPicSize.getHeight() * wideScreenPicSize.getWidth()) {
                wideScreenPicSize = size;
            } else if (ratio > 1.33 && ratio < 1.34
                    && width * height > ordinaryScreenPicSize.getHeight() * ordinaryScreenPicSize.getWidth()) {
                ordinaryScreenPicSize = size;
            }
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_WIDE_PIC_WIDTH,
                wideScreenPicSize.getWidth());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_WIDE_PIC_HEIGHT,
                wideScreenPicSize.getHeight());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_ORDINARY_PIC_WIDTH,
                ordinaryScreenPicSize.getWidth());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_ORDINARY_PIC_HEIGHT,
                ordinaryScreenPicSize.getHeight());
        Log.i(TAG, "initSizeSettings wideScreenPicSize = "+ wideScreenPicSize
                +", ordinaryScreenPicSize = " + ordinaryScreenPicSize);

        for (int i = 0; i < mSupportedPreviewSizes.toArray().length; i++) {
            Size size = mSupportedPreviewSizes.get(i);
            int height = size.getHeight();
            int width = size.getWidth();
            float ratio = width / height;
            if (ratio > 1.77 && ratio < 1.78
                    && width * height > wideScreenPrevSize.getHeight() * wideScreenPrevSize.getWidth()) {
                wideScreenPrevSize = size;
            } else if (ratio > 1.33 && ratio < 1.34
                    && width * height > ordinaryScreenPrevSize.getHeight() * ordinaryScreenPrevSize.getWidth()) {
                ordinaryScreenPrevSize = size;
            }
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_WIDE_PREV_WIDTH,
                wideScreenPrevSize.getWidth());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_WIDE_PREV_HEIGHT,
                wideScreenPrevSize.getHeight());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_ORDINARY_PREV_WIDTH,
                ordinaryScreenPrevSize.getWidth());
        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_ORDINARY_PREV_HEIGHT,
                ordinaryScreenPrevSize.getHeight());
        Log.i(TAG, "initSizeSettings wideScreenPrevSize = "+ wideScreenPrevSize
                +", ordinaryScreenPrevSize = " + ordinaryScreenPrevSize);

        settingsManager.set(SettingsManager.SCOPE_GLOBAL + cameraId, Keys.KEY_CURRENT_PIC_RATIO_WIDE, true);
    }


    public enum ColorEffect {
        NONE,
        MONO,
        NEGATIVE,
        SEPIA,
        COLD,
        ANTIQUE,
    }

    public enum Antibanding {
        AUTO,
        ANTIBANDING_50HZ,
        ANTIBANDING_60HZ,
        OFF,
    }

    /**
     * Focus modes.
     */
    public enum FocusMode {
        /**
         * Continuous auto focus mode intended for taking pictures.
         */
        AUTO,
        /**
         * Continuous auto focus mode intended for taking pictures.
         */
        CONTINUOUS_PICTURE,
        /**
         * Continuous auto focus mode intended for video recording.
         */
        CONTINUOUS_VIDEO,
        /**
         * Extended depth of field (EDOF).
         */
        EXTENDED_DOF,
        /**
         * Focus is fixed.
         */
        FIXED,
        /**
         * Focus is set at infinity.
         */
        // TODO: Unsupported on API 2
        INFINITY,
        /**
         * Macro (close-up) focus mode.
         */
        MACRO,
    }

    /**
     * Flash modes.
     */
    public enum FlashMode {
        /**
         * No flash.
         */
        NO_FLASH,
        /**
         * Flash will be fired automatically when required.
         */
        AUTO,
        /**
         * Flash will not be fired.
         */
        OFF,
        /**
         * Flash will always be fired during snapshot.
         */
        ON,
        /**
         * Constant emission of light during preview, auto-focus and snapshot.
         */
        TORCH,
        /**
         * Flash will be fired in red-eye reduction mode.
         */
        RED_EYE,
    }

    /**
     * Scene modes.
     */
    public enum SceneMode {
        /**
         * No supported scene mode.
         */
        NO_SCENE_MODE,
        /**
         * Scene mode is off.
         */
        AUTO,
        /**
         * Take photos of fast moving objects.
         */
        ACTION,
        /**
         * Applications are looking for a barcode.
         */
        BARCODE,
        /**
         * Take pictures on the beach.
         */
        BEACH,
        /**
         * Capture the naturally warm color of scenes lit by candles.
         */
        CANDLELIGHT,
        /**
         * For shooting firework displays.
         */
        FIREWORKS,
        /**
         * Capture a scene using high dynamic range imaging techniques.
         */
        // Note: Supported as a vendor tag on the Camera2 API for some LEGACY devices.
        HDR,
        /**
         * Take pictures on distant objects.
         */
        LANDSCAPE,
        /**
         * Take photos at night.
         */
        NIGHT,
        /**
         * Take people pictures at night.
         */
        // TODO: Unsupported on API 2
        NIGHT_PORTRAIT,
        /**
         * Take indoor low-light shot.
         */
        PARTY,
        /**
         * Take people pictures.
         */
        PORTRAIT,
        /**
         * Take pictures on the snow.
         */
        SNOW,
        /**
         * Take photos of fast moving objects.
         */
        SPORTS,
        /**
         * Avoid blurry pictures (for example, due to hand shake).
         */
        STEADYPHOTO,
        /**
         * Take sunset photos.
         */
        SUNSET,
        /**
         * Take photos in a theater.
         */
        THEATRE,
    }

    /**
     * White blances.
     */
    public enum WhiteBalance {
        AUTO,
        CLOUDY_DAYLIGHT,
        DAYLIGHT,
        FLUORESCENT,
        INCANDESCENT,
        SHADE,
        TWILIGHT,
        WARM_FLUORESCENT,
    }

    /**
     * Features.
     */
    public enum Feature {
        /**
         * Support zoom-related methods.
         */
        ZOOM,
        /**
         * Support for photo capturing during video recording.
         */
        VIDEO_SNAPSHOT,
        /**
         * Support for focus area settings.
         */
        FOCUS_AREA,
        /**
         * Support for metering area settings.
         */
        METERING_AREA,
        /**
         * Support for automatic exposure lock.
         */
        AUTO_EXPOSURE_LOCK,
        /**
         * Support for automatic white balance lock.
         */
        AUTO_WHITE_BALANCE_LOCK,
        /**
         * Support for video stabilization.
         */
        VIDEO_STABILIZATION,
    }

    /**
     * A interface stringifier to convert abstract representations to API
     * related string representation.
     */
    public static class Stringifier {
        /**
         * Converts the string to hyphen-delimited lowercase for compatibility with multiple APIs.
         *
         * @param enumCase The name of an enum constant.
         * @return The converted string.
         */
        private static String toApiCase(String enumCase) {
            return enumCase.toLowerCase(Locale.US).replaceAll("_", "-");
        }

        /**
         * Converts the string to underscore-delimited uppercase to match the enum constant names.
         *
         * @param apiCase An API-related string representation.
         * @return The converted string.
         */
        private static String toEnumCase(String apiCase) {
            return apiCase.toUpperCase(Locale.US).replaceAll("-", "_");
        }

        /**
         * Converts the focus mode to API-related string representation.
         *
         * @param focus The focus mode to convert.
         * @return The string used by the camera framework API to represent the
         *         focus mode.
         */
        public String stringify(FocusMode focus) {
            return toApiCase(focus.name());
        }

        /**
         * Converts the API-related string representation of the focus mode to the
         * abstract representation.
         *
         * @param val The string representation.
         * @return The focus mode represented by the input string, or the focus
         *         mode with the lowest ordinal if it cannot be converted.
         */
        public FocusMode focusModeFromString(String val) {
            if (val == null) {
                return FocusMode.values()[0];
            }
            try {
                return FocusMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                return FocusMode.values()[0];
            }
        }

        /**
         * Converts the flash mode to API-related string representation.
         *
         * @param flash The focus mode to convert.
         * @return The string used by the camera framework API to represent the
         *         flash mode.
         */
        public String stringify(FlashMode flash) {
            return toApiCase(flash.name());
        }

        /**
         * Converts the API-related string representation of the flash mode to the
         * abstract representation.
         *
         * @param val The string representation.
         * @return The flash mode represented by the input string, or the flash
         *         mode with the lowest ordinal if it cannot be converted.
         */
        public FlashMode flashModeFromString(String val) {
            if (val == null) {
                return FlashMode.values()[0];
            }
            try {
                return FlashMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                return FlashMode.values()[0];
            }
        }

        /**
         * Converts the scene mode to API-related string representation.
         *
         * @param scene The focus mode to convert.
         * @return The string used by the camera framework API to represent the
         *         scene mode.
         */
        public String stringify(SceneMode scene) {
            return toApiCase(scene.name());
        }

        /**
         * Converts the API-related string representation of the scene mode to the
         * abstract representation.
         *
         * @param val The string representation.
         * @return The scene mode represented by the input string, or the scene
         *         mode with the lowest ordinal if it cannot be converted.
         */
        public SceneMode sceneModeFromString(String val) {
            if (val == null) {
                return SceneMode.values()[0];
            }
            try {
                return SceneMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                return SceneMode.values()[0];
            }
        }

        /**
         * Converts the white balance to API-related string representation.
         *
         * @param wb The focus mode to convert.
         * @return The string used by the camera framework API to represent the
         * white balance.
         */
        public String stringify(WhiteBalance wb) {
            return toApiCase(wb.name());
        }

        /**
         * Converts the API-related string representation of the white balance to
         * the abstract representation.
         *
         * @param val The string representation.
         * @return The white balance represented by the input string, or the
         *         white balance with the lowest ordinal if it cannot be
         *         converted.
         */
        public WhiteBalance whiteBalanceFromString(String val) {
            if (val == null) {
                return WhiteBalance.values()[0];
            }
            try {
                return WhiteBalance.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                return WhiteBalance.values()[0];
            }
        }

        public Antibanding antibandingModeFromString(String val) {
            if (val == null) {
                return Antibanding.values()[0];
            }
            try {
                return Antibanding.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "antibandingModeFromString IllegalArgumentException !");
                return Antibanding.values()[0];
            }
        }

        public String stringify(ColorEffect colorEffect) {
            return toApiCase(colorEffect.name());
        }

        public ColorEffect colorEffectFromString(String val) {
            if (val == null) {
                return ColorEffect.values()[0];
            }
            try {
                return ColorEffect.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException ex) {
                return ColorEffect.values()[0];
            }
        }


    }

    /**
     * @return the supported picture formats. See {@link android.graphics.ImageFormat}.
     */
    public Set<Integer> getSupportedPhotoFormats() {
        return new TreeSet<Integer>(mSupportedPhotoFormats);
    }

    /**
     * Gets the supported preview formats.
     * @return The supported preview {@link android.graphics.ImageFormat}s.
     */
    public Set<Integer> getSupportedPreviewFormats() {
        return new TreeSet<Integer>(mSupportedPreviewFormats);
    }

    /**
     * Gets the supported picture sizes.
     */
    public List<Size> getSupportedPhotoSizes() {
        return new ArrayList<Size>(mSupportedPhotoSizes);
    }

    /**
     * @return The supported preview fps (frame-per-second) ranges. The returned
     * list is sorted by maximum fps then minimum fps in a descending order.
     * The values are multiplied by 1000.
     */
    public final List<int[]> getSupportedPreviewFpsRange() {
        return new ArrayList<int[]>(mSupportedPreviewFpsRange);
    }

    /**
     * @return The supported preview sizes. The list is sorted by width then
     * height in a descending order.
     */
    public final List<Size> getSupportedPreviewSizes() {
        return mSupportedPreviewSizes;
    }

    public final Size getPreferredPreviewSizeForVideo() {
        return mPreferredPreviewSizeForVideo;
    }

    /**
     * @return The supported video frame sizes that can be used by MediaRecorder.
     *         The list is sorted by width then height in a descending order.
     */
    public final List<Size> getSupportedVideoSizes() {
        return new ArrayList<Size>(mSupportedVideoSizes);
    }

    /**
     * @return The supported scene modes.
     */
    public final Set<SceneMode> getSupportedSceneModes() {
        return new HashSet<SceneMode>(mSupportedSceneModes);
    }

    /**
     * @return Whether the scene mode is supported.
     */
    public final boolean supports(SceneMode scene) {
        return (scene != null && mSupportedSceneModes.contains(scene));
    }

    /**
     * @return The supported flash modes.
     */
    public final Set<FlashMode> getSupportedFlashModes() {
        return new HashSet<FlashMode>(mSupportedFlashModes);
    }

    /**
     * @return Whether the flash mode is supported.
     */
    public final boolean supports(FlashMode flash) {
        return (flash != null && mSupportedFlashModes.contains(flash));
    }

    /**
     * @return The supported focus modes.
     */
    public final Set<FocusMode> getSupportedFocusModes() {
        return new HashSet<FocusMode>(mSupportedFocusModes);
    }

    /**
     * @return Whether the focus mode is supported.
     */
    public final boolean supports(FocusMode focus) {
        return (focus != null && mSupportedFocusModes.contains(focus));
    }

    /**
     * @return The supported white balanceas.
     */
    public final Set<WhiteBalance> getSupportedWhiteBalance() {
        return new HashSet<WhiteBalance>(mSupportedWhiteBalances);
    }

    /**
     * @return Whether the white balance is supported.
     */
    public boolean supports(WhiteBalance wb) {
        return (wb != null && mSupportedWhiteBalances.contains(wb));
    }

    public final Set<Feature> getSupportedFeature() {
        return new HashSet<Feature>(mSupportedFeatures);
    }

    public boolean supports(Feature ft) {
        return (ft != null && mSupportedFeatures.contains(ft));
    }

    public final boolean supports(Antibanding antibanding) {
        return (antibanding != null && mSupportedAntibanding.contains(antibanding));
    }

    /**
     * @return The maximal supported zoom ratio.
     */
    public float getMaxZoomRatio() {
        return mMaxZoomRatio;
    }

    /**
     * @return The min exposure compensation index. The EV is the compensation
     * index multiplied by the step value. If unsupported, both this method and
     * {@link #getMaxExposureCompensation()} return 0.
     */
    public final int getMinExposureCompensation() {
        return mMinExposureCompensation;
    }

    /**
     * @return The max exposure compensation index. The EV is the compensation
     * index multiplied by the step value. If unsupported, both this method and
     * {@link #getMinExposureCompensation()} return 0.
     */
    public final int getMaxExposureCompensation() {
        return mMaxExposureCompensation;
    }

    /**
     * @return The exposure compensation step. The EV is the compensation index
     * multiplied by the step value.
     */
    public final float getExposureCompensationStep() {
        return mExposureCompensationStep;
    }

    /**
     * @return The max number of faces supported by the face detection. 0 if
     * unsupported.
     */
    public final int getMaxNumOfFacesSupported() {
        return mMaxNumOfFacesSupported;
    }

    /**
     * @return The stringifier used by this instance.
     */
    public Stringifier getStringifier() {
        return mStringifier;
    }

    public final Set<Antibanding> getSupportedAntibanding() {
        return mSupportedAntibanding;
    }

    public final Set<ColorEffect> getSupportedColorEffects() {
        return new HashSet<ColorEffect>(mSupportedColorEffects);
    }

    public final boolean supports(ColorEffect colorEffect) {
        return (colorEffect != null && mSupportedColorEffects.contains(colorEffect));
    }

    protected final ArrayList<String> mSupportedSlowMotion = new ArrayList<String>();

    public final List<String> getSupportedSlowMotion() {
        return mSupportedSlowMotion;
    }

    public final boolean supports(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private void buildSceneModes(CameraCharacteristics p) {
        int[] scenes = p.get(CONTROL_AVAILABLE_SCENE_MODES);
        if (scenes != null) {
            for (int scene : scenes) {
                SceneMode equiv = sceneModeFromInt(scene);
                if (equiv != null) {
                    mSupportedSceneModes.add(equiv);
                }
            }
        }
    }

    private void buildFlashModes(CameraCharacteristics p) {
        mSupportedFlashModes.add(FlashMode.OFF);
        if (p.get(FLASH_INFO_AVAILABLE)) {
            mSupportedFlashModes.add(FlashMode.AUTO);
            mSupportedFlashModes.add(FlashMode.ON);
            mSupportedFlashModes.add(FlashMode.TORCH);
            for (int expose : p.get(CONTROL_AE_AVAILABLE_MODES)) {
                if (expose == CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE) {
                    mSupportedFlashModes.add(FlashMode.RED_EYE);
                }
            }
        }
    }

    private void buildFocusModes(CameraCharacteristics p) {
        int[] focuses = p.get(CONTROL_AF_AVAILABLE_MODES);
        if (focuses != null) {
            for (int focus : focuses) {
                FocusMode equiv = focusModeFromInt(focus);
                if (equiv != null) {
                    mSupportedFocusModes.add(equiv);
                }
            }
        }
    }

    private void buildWhiteBalances(CameraCharacteristics p) {
        int[] bals = p.get(CONTROL_AWB_AVAILABLE_MODES);
        if (bals != null) {
            for (int bal : bals) {
                WhiteBalance equiv = whiteBalanceFromInt(bal);
                if (equiv != null) {
                    mSupportedWhiteBalances.add(equiv);
                }
            }
        }
    }

    private void buildAntibandingModes(CameraCharacteristics p) {
        int[] antibanding = p.get(CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        if (antibanding != null) {
            for (int antiband : antibanding) {
                Antibanding equiv = antibandingFromInt(antiband);
                if (equiv != null) {
                    mSupportedAntibanding.add(equiv);
                }
            }
        }
    }

    private void buildColorEffects(CameraCharacteristics p) {
        int[] effects = p.get(CONTROL_AVAILABLE_EFFECTS);
        if (effects != null) {
            for (int effect : effects) {
                ColorEffect equiv = colorEffectFromInt(effect);
                if (equiv != null) {
                    mSupportedColorEffects.add(equiv);
                }
            }
        }
    }

    /**
     * Converts the API-related integer representation of the focus mode to the
     * abstract representation.
     *
     * @param fm The integral representation.
     * @return The mode represented by the input integer, or {@code null} if it
     *         cannot be converted.
     */
    public static FocusMode focusModeFromInt(int fm) {
        switch (fm) {
            case CONTROL_AF_MODE_AUTO:
                return FocusMode.AUTO;
            case CONTROL_AF_MODE_CONTINUOUS_PICTURE:
                return CONTINUOUS_PICTURE;
            case CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                return CONTINUOUS_VIDEO;
            case CONTROL_AF_MODE_EDOF:
                return EXTENDED_DOF;
            case CONTROL_AF_MODE_OFF:
                return FIXED;
            // TODO: We cannot support INFINITY
            case CONTROL_AF_MODE_MACRO:
                return MACRO;
        }
        Log.w(TAG, "Unable to convert from API 2 focus mode: " + fm);
        return null;
    }

    public static int focusModeToInt(FocusMode fm) {
        switch (fm) {
            case AUTO:
                return CONTROL_AF_MODE_AUTO;
            case CONTINUOUS_PICTURE:
                return CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            case CONTINUOUS_VIDEO:
                return CONTROL_AF_MODE_CONTINUOUS_VIDEO;
            case EXTENDED_DOF:
                return CONTROL_AF_MODE_EDOF;
            case FIXED:
                return CONTROL_AF_MODE_OFF;
            // TODO: We cannot support INFINITY
            case MACRO:
                return CONTROL_AF_MODE_MACRO;
        }
        Log.w(TAG, "Unable to convert from API 2 focus mode: " + fm);
        return CONTROL_AF_MODE_OFF;
    }

    /**
     * Converts the API-related integer representation of the scene mode to the
     * abstract representation.
     *
     * @param sm The integral representation.
     * @return The mode represented by the input integer, or {@code null} if it
     *         cannot be converted.
     */
    public static SceneMode sceneModeFromInt(int sm) {
        switch (sm) {
            case CONTROL_SCENE_MODE_DISABLED:
                return SceneMode.AUTO;
            case CONTROL_SCENE_MODE_ACTION:
                return ACTION;
            case CONTROL_SCENE_MODE_BARCODE:
                return BARCODE;
            case CONTROL_SCENE_MODE_BEACH:
                return BEACH;
            case CONTROL_SCENE_MODE_CANDLELIGHT:
                return CANDLELIGHT;
            case CONTROL_SCENE_MODE_FIREWORKS:
                return FIREWORKS;
            case CONTROL_SCENE_MODE_LANDSCAPE:
                return LANDSCAPE;
            case CONTROL_SCENE_MODE_NIGHT:
                return NIGHT;
            // TODO: We cannot support NIGHT_PORTRAIT
            case CONTROL_SCENE_MODE_PARTY:
                return PARTY;
            case CONTROL_SCENE_MODE_PORTRAIT:
                return PORTRAIT;
            case CONTROL_SCENE_MODE_SNOW:
                return SNOW;
            case CONTROL_SCENE_MODE_SPORTS:
                return SPORTS;
            case CONTROL_SCENE_MODE_STEADYPHOTO:
                return STEADYPHOTO;
            case CONTROL_SCENE_MODE_SUNSET:
                return SUNSET;
            case CONTROL_SCENE_MODE_THEATRE:
                return THEATRE;
            case CONTROL_SCENE_MODE_HDR:
                return HDR;
            // TODO: We cannot expose FACE_PRIORITY, or HIGH_SPEED_VIDEO
        }

        Log.w(TAG, "Unable to convert from API 2 scene mode: " + sm);
        return null;
    }

    public static int sceneModeToInt(SceneMode sm) {
        switch (sm) {
            case AUTO:
                return CONTROL_SCENE_MODE_DISABLED;
            case ACTION:
                return CONTROL_SCENE_MODE_ACTION;
            case BARCODE:
                return CONTROL_SCENE_MODE_BARCODE;
            case BEACH:
                return CONTROL_SCENE_MODE_BEACH;
            case CANDLELIGHT:
                return CONTROL_SCENE_MODE_CANDLELIGHT;
            case FIREWORKS:
                return CONTROL_SCENE_MODE_FIREWORKS;
            case LANDSCAPE:
                return CONTROL_SCENE_MODE_LANDSCAPE;
            case NIGHT:
                return CONTROL_SCENE_MODE_NIGHT;
            // TODO: We cannot support NIGHT_PORTRAIT
            case PARTY:
                return CONTROL_SCENE_MODE_PARTY;
            case PORTRAIT:
                return CONTROL_SCENE_MODE_PORTRAIT;
            case SNOW:
                return CONTROL_SCENE_MODE_SNOW;
            case SPORTS:
                return CONTROL_SCENE_MODE_SPORTS;
            case STEADYPHOTO:
                return CONTROL_SCENE_MODE_STEADYPHOTO;
            case SUNSET:
                return CONTROL_SCENE_MODE_SUNSET;
            case THEATRE:
                return CONTROL_SCENE_MODE_THEATRE;
            case HDR:
                return CONTROL_SCENE_MODE_HDR;
            // TODO: We cannot expose FACE_PRIORITY, or HIGH_SPEED_VIDEO
        }

        return CONTROL_SCENE_MODE_DISABLED;
    }
    /**
     * Converts the API-related integer representation of the white balance to
     * the abstract representation.
     *
     * @param wb The integral representation.
     * @return The balance represented by the input integer, or {@code null} if
     *         it cannot be converted.
     */
    public static WhiteBalance whiteBalanceFromInt(int wb) {
        switch (wb) {
            case CONTROL_AWB_MODE_AUTO:
                return WhiteBalance.AUTO;
            case CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
                return CLOUDY_DAYLIGHT;
            case CONTROL_AWB_MODE_DAYLIGHT:
                return DAYLIGHT;
            case CONTROL_AWB_MODE_FLUORESCENT:
                return FLUORESCENT;
            case CONTROL_AWB_MODE_INCANDESCENT:
                return INCANDESCENT;
            case CONTROL_AWB_MODE_SHADE:
                return SHADE;
            case CONTROL_AWB_MODE_TWILIGHT:
                return TWILIGHT;
            case CONTROL_AWB_MODE_WARM_FLUORESCENT:
                return WARM_FLUORESCENT;
        }
        Log.w(TAG, "Unable to convert from API 2 white balance: " + wb);
        return null;
    }

    public static int whiteBalanceToInt(WhiteBalance wb) {
        switch (wb) {
            case AUTO:
                return CONTROL_AWB_MODE_AUTO;
            case CLOUDY_DAYLIGHT:
                return CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
            case DAYLIGHT:
                return CONTROL_AWB_MODE_DAYLIGHT;
            case FLUORESCENT:
                return CONTROL_AWB_MODE_FLUORESCENT;
            case INCANDESCENT:
                return CONTROL_AWB_MODE_INCANDESCENT;
            case SHADE:
                return CONTROL_AWB_MODE_SHADE;
            case TWILIGHT:
                return CONTROL_AWB_MODE_TWILIGHT;
            case WARM_FLUORESCENT:
                return CONTROL_AWB_MODE_WARM_FLUORESCENT;
        }
        return CONTROL_AWB_MODE_AUTO;
    }

    public static Antibanding antibandingFromInt(int antiband) {
        switch (antiband) {
            case STATISTICS_SCENE_FLICKER_NONE:
                return OFF;
            case STATISTICS_SCENE_FLICKER_50HZ:
                return ANTIBANDING_50HZ;
            case STATISTICS_SCENE_FLICKER_60HZ:
                return ANTIBANDING_60HZ;
        }
        Log.w(TAG, "Unable to convert from API 2 antibanding: " + antiband);
        return null;
    }

    public static int antibandingToInt(Antibanding antiband) {
        switch (antiband) {
            case OFF:
                return STATISTICS_SCENE_FLICKER_NONE;
            case ANTIBANDING_50HZ:
                return STATISTICS_SCENE_FLICKER_50HZ;
            case ANTIBANDING_60HZ:
                return STATISTICS_SCENE_FLICKER_60HZ;
        }
        return STATISTICS_SCENE_FLICKER_NONE;
    }

    public static ColorEffect colorEffectFromInt(int ce) {
        switch (ce) {
            case CONTROL_EFFECT_MODE_OFF:
                return NONE;
            case CONTROL_EFFECT_MODE_MONO:
                return MONO;
            case CONTROL_EFFECT_MODE_NEGATIVE:
                return NEGATIVE;
            case CONTROL_EFFECT_MODE_SEPIA:
                return SEPIA;
            case CONTROL_EFFECT_MODE_AQUA:
                return COLD;
            case CONTROL_EFFECT_MODE_SOLARIZE:
                return ANTIQUE;
        }
        Log.w(TAG, "Unable to convert from API 2 color effect: " + ce);
        return null;
    }

    public static int colorEffectToInt(ColorEffect ce) {
        switch (ce) {
            case NONE:
                return CONTROL_EFFECT_MODE_OFF;
            case MONO:
                return CONTROL_EFFECT_MODE_MONO;
            case NEGATIVE:
                return CONTROL_EFFECT_MODE_NEGATIVE;
            case SEPIA:
                return CONTROL_EFFECT_MODE_SEPIA;
            case COLD:
                return CONTROL_EFFECT_MODE_AQUA;
            case ANTIQUE:
                return CONTROL_EFFECT_MODE_SOLARIZE;
        }
        return CONTROL_EFFECT_MODE_OFF;
    }
}
