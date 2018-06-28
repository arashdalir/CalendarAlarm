package com.arashdalir.calendaralarm;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Context context = preference.getContext();
            String key = preference.getKey();
            String summary = getPreferenceSummary(context, key, value);
            preference.setSummary(summary);

            Notifier.showToast(context, context.getResources().getString(R.string.notification_toast_settings_changes_saved), Toast.LENGTH_SHORT);
            AlarmManagerService.enqueueWork(context);
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.

        if (preference != null) {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's  current value.

            if (preference instanceof RingtonePreference) {
                // Trigger the listener immediately with the preference's
                // current value.
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(
                                preference.getContext()).getString(preference.getKey(), ""));
            } else if (preference instanceof SwitchPreference) {
                // Trigger the listener immediately with the preference's
                // current value.
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(
                                preference.getContext()).getBoolean(preference.getKey(), true));
            }
        }
    }

    protected static String getPreferenceSummary(Context context, String key, Object value) {
        String summary = "";

        switch (key) {
            case StorageHelper.STORAGE_ALARM_RINGTONE:
                String val = (String) value;
                if (val.isEmpty()) {
                    summary = context.getString(R.string.pref_ringtone_silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse((String) val));

                    summary = ringtone.getTitle(context);
                }
                break;

            case StorageHelper.STORAGE_ALARM_VIBRATE:
                if ((Boolean) value) {
                    summary = context.getString(R.string.pref_vibrate_on);
                } else {
                    summary = context.getString(R.string.pref_vibrate_off);
                }
                break;
            default:
                break;
        }

        return summary;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String alarmRingtone = StorageHelper.getRingtone(this);

        if (alarmRingtone != null && !alarmRingtone.isEmpty()) {
            StorageHelper.setRingtone(this, alarmRingtone);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(StorageHelper.STORAGE_ALARM_RINGTONE));
            bindPreferenceSummaryToValue(findPreference(StorageHelper.STORAGE_ALARM_VIBRATE));
        }

        /*
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            return OptionsItemSelectionHelper.handleOptionSelection(this, item) || super.onOptionsItemSelected(item);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            OptionsItemSelectionHelper.createMenuItems(inflater, menu);
        }
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return OptionsItemSelectionHelper.handleOptionSelection(this, item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsItemSelectionHelper.createMenuItems(this, menu);
    }
}
