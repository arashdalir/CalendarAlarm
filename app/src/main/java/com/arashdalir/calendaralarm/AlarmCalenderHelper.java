package com.arashdalir.calendaralarm;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.support.v4.app.NotificationManagerCompat;
import android.util.EventLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    public static Cursor readCurrentAlarms(Context context)
    {
        Uri uri = Uri.parse("content://com.android.alarmclock/alarm");
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);


        return c;
    }

    public static List<String> getPermissions(Context context){
        List<String> permissions = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();

        try{
            PackageInfo pi = pm.getPackageInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);

            if (pi != null && pi.requestedPermissions.length > 0)
            {
                for(int i = 0; i < pi.requestedPermissions.length; i++)
                {
                    permissions.add(pi.requestedPermissions[i]);
                }
            }
        }
        catch(Exception e)
        {

        }

        return permissions;
    }

    public static boolean checkPermissions(Context context) {
       List<String> permissions = getPermissions(context);
        for (int i = 0; i < permissions.size(); i++) {
            if (context.checkSelfPermission(permissions.get(i)) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /*
    public static void createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            //CharSequence name = context.getString(R.string.channel_name);
            //String description = getString(R.string.channel_description);

            CharSequence name = "CalendarAlarmNotificationChannel";
            String description = "This is CalendarAlarm's notification channel.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CalendarAlarmChannel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.createNotificationChannel(channel);
        }
    }
    */
}
