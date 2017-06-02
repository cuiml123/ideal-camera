package com.mycamera2.presenter.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import com.mycamera2.CameraActivity;


public class ManagedSwitchPreference extends SwitchPreference {
    public ManagedSwitchPreference(Context context) {
        super(context);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        CameraActivity cameraApp = getCameraApp();
        if (cameraApp == null) {
            // The context and app may not be initialized upon initial inflation of the
            // preference from XML. In that case return the default value.
            return defaultReturnValue;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager != null) {
            return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, getKey());
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return default value.
            return defaultReturnValue;
        }
    }

    @Override
    public boolean persistBoolean(boolean value) {
        CameraActivity cameraApp = getCameraApp();
        if (cameraApp == null) {
            // The context may not be initialized upon initial inflation of the
            // preference from XML. In that case return false to note the value won't
            // be persisted.
            return false;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager != null) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, getKey(), value);
            return true;
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return false to note the value
            // was not persisted.
            return false;
        }
    }

    private CameraActivity getCameraApp() {
        Context context = getContext();
        if (context instanceof CameraActivity) {
            return (CameraActivity) context;
        }
        return null;
    }
}
