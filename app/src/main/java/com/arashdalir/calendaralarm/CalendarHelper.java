package com.arashdalir.calendaralarm;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.util.SparseArray;

import java.util.Calendar;

/**
 * Created by ada on 18-Oct-17.
 */

class CalendarHelper {
    // Calendars Projection
    private static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.CALENDAR_COLOR
    };

    // Instances Projection
    private static final String[] INSTANCES_PROJECTION = new String[]{
            Instances._ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.DESCRIPTION
    };

    private static final String[] REMINDERS_PROJECTION = new String[]{
            CalendarContract.Reminders.EVENT_ID,
            CalendarContract.Reminders._ID,
            CalendarContract.Reminders.METHOD,
            CalendarContract.Reminders.MINUTES
    };

    private static SparseArray<CalendarInfo> calendars = new SparseArray<>();

    static class CalendarInfo{
        private int id;
        private String displayName;
        private int color;

        CalendarInfo(int id, String displayName, int color)
        {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
        }

        public int getId(){
            return id;
        }

        String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    static Cursor readCalendars(Context context) {
        return readCalendars(context, 0);
    }

    static Cursor readCalendars(Context context, int calendarId) {
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        ContentResolver contentResolver = context.getContentResolver();

        if (calendarId != 0){
            ContentUris.appendId(builder, calendarId);
        }

        return contentResolver.query(builder.build(), CALENDAR_PROJECTION, null, null, null);
    }

    static Cursor readEvents(Context context, long beginms, long endms) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();

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

    static Cursor readReminders(Context context, int eventId) {
        ContentResolver cr = context.getContentResolver();
        Uri.Builder builder = CalendarContract.Reminders.CONTENT_URI.buildUpon();

        String[] args = {""};
        args[0] = Integer.toString(eventId);

        return cr.query(
                builder.build(),
                REMINDERS_PROJECTION,
                CalendarContract.Reminders.EVENT_ID + " = ?",
                args,
                null
        );
    }

    static CalendarInfo getCalendarInfo(Context context, int calendarId)
    {
        if (calendarId == -1)
        {
            return new CalendarHelper.CalendarInfo(calendarId, "Fake Calendar", Color.RED);
        }
        else{
            if (calendars.indexOfKey(calendarId) < 0)
            {
                Cursor cr = readCalendars(context, calendarId);

                String calendarName;
                int color;

                if (cr.moveToFirst())
                {
                    do{
                        calendarName = cr.getString(cr.getColumnIndex(Calendars.CALENDAR_DISPLAY_NAME));
                        color = cr.getInt(cr.getColumnIndex(Calendars.CALENDAR_COLOR));

                        CalendarInfo info = new CalendarInfo(calendarId, calendarName, color);

                        calendars.put(calendarId, info);
                    }while (cr.moveToNext());
                }

                cr.close();
            }
            return calendars.get(calendarId, null);
        }
    }
}
