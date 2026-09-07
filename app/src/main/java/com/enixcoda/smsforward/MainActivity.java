package com.enixcoda.smsforward;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class MainActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(buildRequiredPermissions(), 0);
        requestIgnoreBatteryOptimizations();

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }

        ForwardingService.sync(this);
    }

    private String[] buildRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        return new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS
        };
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String askedKey = "key_asked_ignore_battery_optimizations";
        if (prefs.getBoolean(askedKey, false))
            return;
        PowerManager powerManager = getSystemService(PowerManager.class);
        if (powerManager == null || powerManager.isIgnoringBatteryOptimizations(getPackageName()))
            return;
        prefs.edit().putBoolean(askedKey, true).apply();
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.root_preferences);
            bindAllSummaries(getPreferenceScreen());
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .registerOnSharedPreferenceChangeListener(this);
            bindAllSummaries(getPreferenceScreen());
        }

        @Override
        public void onPause() {
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (getActivity() == null || key == null)
                return;
            Preference preference = findPreference(key);
            if (preference != null)
                bindSummary(preference);
            if (getString(R.string.key_enable_sms).equals(key)
                    || getString(R.string.key_enable_telegram).equals(key)
                    || getString(R.string.key_enable_web).equals(key)) {
                ForwardingService.sync(getActivity());
            }
        }

        private void bindAllSummaries(PreferenceGroup group) {
            if (group == null)
                return;
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference preference = group.getPreference(i);
                if (preference instanceof PreferenceCategory || preference instanceof PreferenceScreen) {
                    bindAllSummaries((PreferenceGroup) preference);
                } else {
                    bindSummary(preference);
                }
            }
        }

        private void bindSummary(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                CharSequence entry = listPreference.getEntry();
                listPreference.setSummary(entry != null ? entry : "");
                return;
            }
            if (!(preference instanceof EditTextPreference))
                return;

            EditTextPreference editPreference = (EditTextPreference) preference;
            String value = editPreference.getText();
            if (value != null)
                value = value.trim();
            if (value == null || value.isEmpty()) {
                editPreference.setSummary(emptySummaryFor(preference.getKey()));
            } else {
                editPreference.setSummary(value);
            }
        }

        private String emptySummaryFor(String key) {
            if (getString(R.string.key_target_sms).equals(key))
                return getString(R.string.target_summary_sms);
            if (getString(R.string.key_target_web).equals(key))
                return getString(R.string.target_summary_web);
            if (getString(R.string.key_sender_filter_numbers).equals(key))
                return getString(R.string.sender_filter_numbers_summary);
            if (getString(R.string.key_target_telegram).equals(key))
                return getString(R.string.target_summary_telegram);
            if (getString(R.string.key_telegram_apikey).equals(key))
                return getString(R.string.summary_telegram_apikey);
            return "";
        }
    }
}
