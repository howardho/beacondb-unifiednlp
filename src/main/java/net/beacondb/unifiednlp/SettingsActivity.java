/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package net.beacondb.unifiednlp;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "IchnaeaPreferences";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Set initial summary and update on change for the endpoint EditTextPreference
        Preference p = findPreference("endpoint");
        if (p instanceof EditTextPreference) {
            final EditTextPreference endpointPref = (EditTextPreference) p;
            CharSequence current = endpointPref.getText();
            if (current == null) {
                current = endpointPref.getSharedPreferences().getString("endpoint", "https://api.beacondb.net/v1/geolocate");
            }
            endpointPref.setSummary(current);
            endpointPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    return true; // persist the new value
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Preferences paused, reloading backend settings");
        BackendService.reloadInstanceSettings();
    }
}
