/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package net.beacondb.unifiednlp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "IchnaeaPreferences";
    private static final int REQUEST_READ_PHONE_STATE = 1001;
    private static final int REQUEST_LOCATION_PERMISSIONS = 1002;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Runtime check/request for READ_PHONE_STATE (API 23+). Wrapped in try/catch to avoid crashes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Try to get location permissions
            try {
                boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean needBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
                boolean background = true;
                if (needBackground) {
                    try {
                        background = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    } catch (Exception e) {
                        Log.w(TAG, "Background permission check failed", e);
                        background = false;
                    }
                }

                if (!fine || !coarse || (needBackground && !background)) {
                    try {
                        // Show a simple rationale dialog before requesting permissions
                        showLocationRationaleAndRequest(needBackground);
                    } catch (Exception e) {
                        Log.w(TAG, "requestPermissions for location failed", e);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "location permission check failed", e);
            }

            // Try to get READ_PHONE_STATE permission for cell correlation
            try {
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    try {
                        requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
                    } catch (Exception e) {
                        Log.w(TAG, "requestPermissions failed", e);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "permission check failed", e);
            }
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            if (requestCode == REQUEST_READ_PHONE_STATE) {
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "READ_PHONE_STATE granted");
                } else {
                    Log.w(TAG, "READ_PHONE_STATE not granted");
                }
            } else if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
                boolean grantedAll = true;
                if (grantResults != null && grantResults.length > 0) {
                    for (int r : grantResults) {
                        if (r != PackageManager.PERMISSION_GRANTED) {
                            grantedAll = false;
                            break;
                        }
                    }
                } else {
                    grantedAll = false;
                }
                if (grantedAll) {
                    Log.d(TAG, "Location permissions granted");
                } else {
                    Log.w(TAG, "Location permissions not fully granted");
                    // Show dialog offering retry
                    showPermissionDeniedRetryDialog();
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } catch (Exception e) {
            Log.w(TAG, "onRequestPermissionsResult handling failed", e);
        }
    }

    private void showLocationRationaleAndRequest(final boolean needBackground) {
        Log.d(TAG, "in showLocationRationaleAndRequest, needBackground=" + (needBackground?"yes":"no"));
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(getString(R.string.permission_location_title));
            String msg = getString(R.string.permission_location_message);
            if (needBackground) {
                msg = msg + "\n\n" + getString(R.string.permission_location_background_explanation);
            }
            b.setMessage(msg);
            b.setPositiveButton(getString(R.string.permission_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        if (needBackground) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_LOCATION_PERMISSIONS);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSIONS);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "requestPermissions for location failed", e);
                    }
                }
            });
            b.setNegativeButton(getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.w(TAG, "User canceled location permission request");
                }
            });
            b.setCancelable(true);
            b.show();
        } catch (Exception e) {
            Log.w(TAG, "showLocationRationaleAndRequest failed", e);
        }
    }

    private void showPermissionDeniedRetryDialog() {
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(getString(R.string.permission_denied_title));
            b.setMessage(getString(R.string.permission_denied_message));
            b.setPositiveButton(getString(R.string.permission_retry), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // Retry: re-run the location permission rationale flow
                        boolean needBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
                        showLocationRationaleAndRequest(needBackground);
                    } catch (Exception e) {
                        Log.w(TAG, "Retry request failed", e);
                    }
                }
            });
            b.setNegativeButton(getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.w(TAG, "User chose not to retry permissions");
                }
            });
            b.setCancelable(true);
            b.show();
        } catch (Exception e) {
            Log.w(TAG, "showPermissionDeniedRetryDialog failed", e);
        }
    }
}
