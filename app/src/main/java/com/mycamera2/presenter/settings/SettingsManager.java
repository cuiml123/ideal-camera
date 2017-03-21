package com.mycamera2.presenter.settings;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String TAG = "CAM_SettingsManager";

    private final Context mContext;
    private final String mPackageName;
    private final SharedPreferences mDefaultPreferences;
    private SharedPreferences mCustomPreferences;
    private final DefaultsStore mDefaultsStore = new DefaultsStore();

    /**
     * A List of OnSettingChangedListener's, maintained to compare to new
     * listeners and prevent duplicate registering.
     */
    private final List<OnSettingChangedListener> mListeners =
            new ArrayList<OnSettingChangedListener>();

    /**
     * A List of OnSharedPreferenceChangeListener's, maintained to hold pointers
     * to actually registered listeners, so they can be unregistered.
     */
    private final List<SharedPreferences.OnSharedPreferenceChangeListener> mSharedPreferenceListeners =
            new ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>();

    public SettingsManager(Context context) {
        mContext = context;
        mPackageName = mContext.getPackageName();

        mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Get the SettingsManager's default preferences.  This is useful
     * to third party modules as they are defining their upgrade paths,
     * since most third party modules will use either SCOPE_GLOBAL or a
     * custom scope.
     */
    public SharedPreferences getDefaultPreferences() {
        return mDefaultPreferences;
    }

    /**
     * Open a SharedPreferences file by custom scope.
     * Also registers any known SharedPreferenceListeners on this
     * SharedPreferences instance.
     */
    protected SharedPreferences openPreferences(String scope) {
        SharedPreferences preferences = mContext.getSharedPreferences(
                mPackageName + scope, Context.MODE_PRIVATE);

        for (SharedPreferences.OnSharedPreferenceChangeListener listener : mSharedPreferenceListeners) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }

        return preferences;
    }

    /**
     * Close a SharedPreferences file by custom scope.
     * The file isn't explicitly closed (the SharedPreferences API makes
     * this unnecessary), so the real work is to unregister any known
     * SharedPreferenceListeners from this SharedPreferences instance.
     *
     * It's important to do this as camera and modules change, because
     * we don't want old SharedPreferences listeners executing on
     * cameras/modules they are not compatible with.
     */
    protected void closePreferences(SharedPreferences preferences) {
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : mSharedPreferenceListeners) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    /**
     * Interface with Camera Device Settings and Modules.
     */
    public interface OnSettingChangedListener {
        /**
         * Called every time a SharedPreference has been changed.
         */
        public void onSettingChanged(SettingsManager settingsManager, String key);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener(
            final OnSettingChangedListener listener) {
        return new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                Log.i(TAG, "onSharedPreferenceChanged key = " + key);
                listener.onSettingChanged(SettingsManager.this, key);
            }
        };
    }

    /**
     * Add an OnSettingChangedListener to the SettingsManager, which will
     * execute onSettingsChanged when any SharedPreference has been updated.
     */
    public void addListener(final OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnSettingChangedListener cannot be null.");
        }
        if (mListeners.contains(listener)) {
            return;
        }

        mListeners.add(listener);
        SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener =
                getSharedPreferenceListener(listener);
        mSharedPreferenceListeners.add(sharedPreferenceListener);
        mDefaultPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);

        if (mCustomPreferences != null) {
            mCustomPreferences.registerOnSharedPreferenceChangeListener(
                    sharedPreferenceListener);
        }
        Log.v(TAG, "listeners: " + mListeners);
    }

    /**
     * Remove a specific SettingsListener. This should be done in onPause if a
     * listener has been set.
     */
    public void removeListener(OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException();
        }
        if (!mListeners.contains(listener)) {
            return;
        }

        int index = mListeners.indexOf(listener);
        mListeners.remove(listener);

        SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener =
                mSharedPreferenceListeners.get(index);
        mSharedPreferenceListeners.remove(index);
        mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(
                sharedPreferenceListener);

        if (mCustomPreferences != null) {
            mCustomPreferences.unregisterOnSharedPreferenceChangeListener(
                    sharedPreferenceListener);
        }
    }

    /**
     * Remove all OnSharedPreferenceChangedListener's. This should be done in
     * onDestroy.
     */
    public void removeAllListeners() {
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : mSharedPreferenceListeners) {
            mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(listener);

            if (mCustomPreferences != null) {
                mCustomPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            }
        }
        mSharedPreferenceListeners.clear();
        mListeners.clear();
    }

    /** This scope stores and retrieves settings from
     default preferences. */
    public static final String SCOPE_GLOBAL = "default_scope";


    /**
     * Returns the SharedPreferences file matching the scope
     * argument.
     *
     * Camera and module preferences files are cached,
     * until the camera id or module id changes, then the listeners
     * are unregistered and a new file is opened.
     */
    private SharedPreferences getPreferencesFromScope(String scope) {
        if (scope.equals(SCOPE_GLOBAL)) {
            return mDefaultPreferences;
        }

        if (mCustomPreferences != null) {
            closePreferences(mCustomPreferences);
        }
        mCustomPreferences = openPreferences(scope);
        return mCustomPreferences;
    }

    /**
     * Set default and valid values for a setting, for a String default and
     * a set of String possible values that are already defined.
     * This is not required.
     */
    public void setDefaults(String key, String defaultValue, String[] possibleValues) {
        mDefaultsStore.storeDefaults(key, defaultValue, possibleValues);
    }

    /**
     * Set default and valid values for a setting, for an Integer default and
     * a set of Integer possible values that are already defined.
     * This is not required.
     */
    public void setDefaults(String key, int defaultValue, int[] possibleValues) {
        String defaultValueString = Integer.toString(defaultValue);
        String[] possibleValuesString = new String[possibleValues.length];
        for (int i = 0; i < possibleValues.length; i++) {
            possibleValuesString[i] = Integer.toString(possibleValues[i]);
        }
        mDefaultsStore.storeDefaults(key, defaultValueString, possibleValuesString);
    }

    /**
     * Set default and valid values for a setting, for a boolean default.
     * The set of boolean possible values is always { false, true }.
     * This is not required.
     */
    public void setDefaults(String key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String[] possibleValues = { "0", "1" };
        mDefaultsStore.storeDefaults(key, defaultValueString, possibleValues);
    }

    /**
     * Retrieve a default from the DefaultsStore as a String.
     */
    public String getStringDefault(String key) {
        return mDefaultsStore.getDefaultValue(key);
    }

    /**
     * Retrieve a default from the DefaultsStore as an Integer.
     */
    public Integer getIntegerDefault(String key) {
        String defaultValueString = mDefaultsStore.getDefaultValue(key);
        return defaultValueString == null ? 0 : Integer.parseInt(defaultValueString);
    }

    /**
     * Retrieve a default from the DefaultsStore as a boolean.
     */
    public boolean getBooleanDefault(String key) {
        String defaultValueString = mDefaultsStore.getDefaultValue(key);
        return defaultValueString == null ? false :
                (Integer.parseInt(defaultValueString) != 0);
    }

    /**
     * Retrieve a setting's value as a String, manually specifiying
     * a default value.
     */
    public String getString(String scope, String key, String defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        try {
            return preferences.getString(key, defaultValue);
        } catch (ClassCastException e) {
            Log.w(TAG, "existing preference with invalid type, removing and returning default", e);
            preferences.edit().remove(key).apply();
            return defaultValue;
        }
    }

    /**
     * Retrieve a setting's value as a String, using the default value
     * stored in the DefaultsStore.
     */
    public String getString(String scope, String key) {
        return getString(scope, key, getStringDefault(key));
    }

    /**
     * Retrieve a setting's value as an Integer, manually specifying
     * a default value.
     */
    public Integer getInteger(String scope, String key, Integer defaultValue) {
        String defaultValueString = Integer.toString(defaultValue);
        String value = getString(scope, key, defaultValueString);
        return convertToInt(value);
    }

    /**
     * Retrieve a setting's value as an Integer, converting the default value
     * stored in the DefaultsStore.
     */
    public Integer getInteger(String scope, String key) {
        return getInteger(scope, key, getIntegerDefault(key));
    }

    /**
     * Retrieve a setting's value as a boolean, manually specifiying
     * a default value.
     */
    public boolean getBoolean(String scope, String key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String value = getString(scope, key, defaultValueString);
        return convertToBoolean(value);
    }

    /**
     * Retrieve a setting's value as a boolean, converting the default value
     * stored in the DefaultsStore.
     */
    public boolean getBoolean(String scope, String key) {
        return getBoolean(scope, key, getBooleanDefault(key));
    }

    /**
     * Retrieve a setting's value as a {@link Size}. Returns <code>null</code>
     * if value could not be parsed as a size.
     */
    /*public Size getSize(String scope, String key) {
        String strValue = getString(scope, key);
        if (strValue == null) {
            return null;
        }

        String[] widthHeight = strValue.split("x");
        if (widthHeight.length != 2) {
            return null;
        }

        try {
            int width = Integer.parseInt(widthHeight[0]);
            int height = Integer.parseInt(widthHeight[1]);
            return new Size(width, height);
        } catch (NumberFormatException ex) {
            return null;
        }
    }*/

    /**
     * If possible values are stored for this key, return the
     * index into that list of the currently set value.
     *
     * For example, if a set of possible values is [2,3,5],
     * and the current value set of this key is 3, this method
     * returns 1.
     *
     * If possible values are not stored for this key, throw
     * an IllegalArgumentException.
     */
    public int getIndexOfCurrentValue(String scope, String key) {
        String[] possibleValues = mDefaultsStore.getPossibleValues(key);
        if (possibleValues == null || possibleValues.length == 0) {
            throw new IllegalArgumentException(
                    "No possible values for scope=" + scope + " key=" + key);
        }

        String value = getString(scope, key);
        for (int i = 0; i < possibleValues.length; i++) {
            if (value.equals(possibleValues[i])) {
                return i;
            }
        }
        throw new IllegalStateException("Current value for scope=" + scope + " key="
                + key + " not in list of possible values");
    }

    /**
     * Store a setting's value using a String value.  No conversion
     * occurs before this value is stored in SharedPreferences.
     */
    public void set(String scope, String key, String value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putString(key, value).apply();
    }

    /**
     * Store a setting's value using an Integer value.  Type conversion
     * to String occurs before this value is stored in SharedPreferences.
     */
    public void set(String scope, String key, int value) {
        set(scope, key, convert(value));
    }

    /**
     * Store a setting's value using a boolean value.  Type conversion
     * to an Integer and then to a String occurs before this value is
     * stored in SharedPreferences.
     */
    public void set(String scope, String key, boolean value) {
        set(scope, key, convert(value));
    }

    /**
     * Set a setting to the default value stored in the DefaultsStore.
     */
    public void setToDefault(String scope, String key) {
        set(scope, key, getStringDefault(key));
    }

    /**
     * If a set of possible values is defined, set the current value
     * of a setting to the possible value found at the given index.
     *
     * For example, if the possible values for a key are [2,3,5],
     * and the index given to this method is 2, then this method would
     * store the value 5 in SharedPreferences for the key.
     *
     * If the index is out of the bounds of the range of possible values,
     * or there are no possible values for this key, then this
     * method throws an exception.
     */
    public void setValueByIndex(String scope, String key, int index) {
        String[] possibleValues = mDefaultsStore.getPossibleValues(key);
        if (possibleValues.length == 0) {
            throw new IllegalArgumentException(
                    "No possible values for scope=" + scope + " key=" + key);
        }

        if (index >= 0 && index < possibleValues.length) {
            set(scope, key, possibleValues[index]);
        } else {
            throw new IndexOutOfBoundsException("For possible values of scope=" + scope
                    + " key=" + key);
        }
    }

    /**
     * Check that a setting has some value stored.
     */
    public boolean isSet(String scope, String key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.contains(key);
    }

    /**
     * Check whether a settings's value is currently set to the
     * default value.
     */
    public boolean isDefault(String scope, String key) {
        String defaultValue = getStringDefault(key);
        String value = getString(scope, key);
        return value == null ? false : value.equals(defaultValue);
    }

    /**
     * Remove a setting.
     */
    public void remove(String scope, String key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().remove(key).apply();
    }

    /**
     * Package private conversion method to turn ints into preferred
     * String storage format.
     *
     * @param value int to be stored in Settings
     * @return String which represents the int
     */
    static String convert(int value) {
        return Integer.toString(value);
    }

    /**
     * Package private conversion method to turn String storage format into
     * ints.
     *
     * @param value String to be converted to int
     * @return int value of stored String
     */
    static int convertToInt(String value) {
        return Integer.parseInt(value);
    }

    /**
     * Package private conversion method to turn String storage format into
     * booleans.
     *
     * @param value String to be converted to boolean
     * @return boolean value of stored String
     */
    static boolean convertToBoolean(String value) {
        return Integer.parseInt(value) != 0;
    }


    /**
     * Package private conversion method to turn booleans into preferred
     * String storage format.
     *
     * @param value boolean to be stored in Settings
     * @return String which represents the boolean
     */
    static String convert(boolean value) {
        return value ? "1" : "0";
    }
}
