package com.arashdalir.calendaralarm;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.util.EventLog;

import java.util.Calendar;

/**
 * Created by ada on 18-Oct-17.
 */

public class AlarmCalenderHelper {
    // Calendars Projection
    public static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.CALENDAR_COLOR
    };

    // Instances Projection
    public static final String[] INSTANCES_PROJECTION = new String[]{
            Instances._ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.DESCRIPTION
    };

    public static String[] permissions = {
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "com.android.alarm.permission.SET_ALARM",
            "com.android.alarm.permission.GET_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED"
    };

    public static Cursor createCalendarsCursor(Context context) {
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(builder.build(), CALENDAR_PROJECTION, null, null, null);
    }

    public static Cursor readEvents(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();

        Calendar now = Calendar.getInstance();
        long beginms = now.getTimeInMillis();
        now.add(Calendar.DATE, 2);
        long endms = now.getTimeInMillis();

        ContentUris.appendId(builder, beginms);
        ContentUris.appendId(builder, endms);

        return contentResolver.query(
                builder.build(),
                INSTANCES_PROJECTION,
                null,
                null,
                null
        );
    }

    public static Cursor readCurrentAlarms(Context context)
    {
        Uri uri = Uri.parse("content://com.android.alarmclock/alarm");
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);


        return c;
    }


    public static boolean checkPermissions(Context context) {
        for (int i = 0; i < permissions.length; i++) {
            if (context.checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
