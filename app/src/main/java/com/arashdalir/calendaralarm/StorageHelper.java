package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class StorageHelper {
    public static final String STORAGE_ID = "alarm_calendar_preferences";
    public static final String STORAGE_CALENDARS = "calendar_list";
    public static final String STORAGE_ALARM_RINGTONE = "alarm_ringtone";
    public static final String STORAGE_ALARM_VIBRATE = "alarm_vibrate";
    public static final String STORAGE_EXECUTION_TIME = "last_exec_time";
    public static final String STORAGE_ALARM_LIST = "alarm_list";

    public static Integer[] getCalendars(Context context) {
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);
        String list = settingsStorage.getString(StorageHelper.STORAGE_CALENDARS, "");

        List<Integer> currentCalendars = new ArrayList<>();
        try {
            JSONArray js = new JSONArray(list);
            for (int i = 0; i < js.length(); i++) {
                currentCalendars.add((Integer) js.get(i));
            }
        } catch (Exception e) {

        }

        Integer[] output = new Integer[currentCalendars.size()];

        return currentCalendars.toArray(output);
    }

    public static void setCalendars(Context context, List<Integer> calendars) {
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = settingsStorage.edit();
        e.putString(StorageHelper.STORAGE_CALENDARS, Arrays.toString(calendars.toArray()));
        e.apply();
    }

    public static String getRingtone(Context context) {
        return getRingtone(context, false);
    }

    public static String getRingtone(Context context, boolean returnDefault) {
        Uri defaultAlarmRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        String defaultRingtone = defaultAlarmRingtone.toString();
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);

        String ringtone = settingsStorage.getString(StorageHelper.STORAGE_ALARM_RINGTONE, defaultRingtone);

        if (!returnDefault && ringtone.equals(defaultAlarmRingtone.toString()))
        {
            ringtone = "";
        }
        return ringtone;
    }

    public static void setRingtone(Context context, String alarmRingtone) {
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = settingsStorage.edit();
        e.putString(StorageHelper.STORAGE_ALARM_RINGTONE, alarmRingtone);
        e.apply();
    }

    public static Boolean getVibrate(Context context) {
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);
        return settingsStorage.getBoolean(StorageHelper.STORAGE_ALARM_VIBRATE, false);
    }

    public static void setVibrate(Context context, boolean vibrate) {
        SharedPreferences settingsStorage = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = settingsStorage.edit();
        e.putBoolean(StorageHelper.STORAGE_ALARM_VIBRATE, vibrate);
        e.apply();
    }

    public static JSONArray getStoredAlarms(Context context) throws JSONException {
        SharedPreferences settingsStorage = context.getSharedPreferences(StorageHelper.STORAGE_ID, MODE_PRIVATE);
        String alarms = settingsStorage.getString(StorageHelper.STORAGE_ALARM_LIST, "");

        if (!alarms.isEmpty()) {
            return new JSONArray(alarms);
        } else {
            return null;
        }

    }

    public static void storeAlarms(Context context, JSONArray alarms) {
        SharedPreferences settingsStorage = context.getSharedPreferences(StorageHelper.STORAGE_ID, MODE_PRIVATE);
        SharedPreferences.Editor e = settingsStorage.edit();
        e.putString(StorageHelper.STORAGE_ALARM_LIST, alarms.toString());
        e.apply();
    }
}
