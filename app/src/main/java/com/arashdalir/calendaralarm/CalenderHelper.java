package com.arashdalir.calendaralarm;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;

import java.util.Calendar;
import java.util.List;

/**
 * Created by ada on 18-Oct-17.
 */

public class CalenderHelper {
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

    public static final String[] REMINDERS_PROJECTION = new String[]{
            CalendarContract.Reminders.EVENT_ID,
            CalendarContract.Reminders.METHOD,
            CalendarContract.Reminders.MINUTES
    };

    public static Cursor readCalendars(Context context) {
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(builder.build(), CALENDAR_PROJECTION, null, null, null);
    }

    public static Cursor readEvents(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();

        Calendar now = Calendar.getInstance();
        long beginms = now.getTimeInMillis();
        now.add(Calendar.DATE, 1);
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

    public static Cursor readReminders(Context context, int eventId) {
        ContentResolver cr = context.getContentResolver();
        Uri.Builder builder = CalendarContract.Reminders.CONTENT_URI.buildUpon();

        ContentUris.appendId(builder, eventId);

        return cr.query(
                builder.build(),
                REMINDERS_PROJECTION,
                null,
                null,
                null
        );
    }

    public static Cursor readCurrentAlarms(Context context) {
        Uri uri = Uri.parse("content://com.android.alarmclock/alarm");
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);


        return c;
    }
}
