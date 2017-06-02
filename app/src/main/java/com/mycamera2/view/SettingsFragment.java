package com.mycamera2.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.mycamera2.R;


public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "CAM_SettingsFragment";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isVisible()) {
            setSummary(sharedPreferences, findPreference(key));
        }
    }

    protected void setSummary(SharedPreferences sharedPreferences, Preference preference) {
        if (!(preference instanceof ListPreference)) {
            return;
        }

        ListPreference listPreference = (ListPreference) preference;
        listPreference.setSummary(listPreference.getEntry());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.camera_settings);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

}
