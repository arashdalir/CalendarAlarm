package com.arashdalir.calendaralarm;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.net.Inet4Address;
import java.util.Calendar;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AlarmManagerService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_CREATE_ALARMS = "com.arashdalir.calendaralarm.action.CREATE_ALARMS";
    public static final String ACTION_DELETE_ALARMS = "com.arashdalir.calendaralarm.action.DELETE_ALARMS";
    public static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";
    public static final String NOTIFICATION_CHANNEL = "com.arashdalir.calendaralarm.NOTIFICATION_CHANNEL";

    private static final String APP_PREFIX = "AutoAlarm - ";

    // TODO: Rename parameters
    //private static final String PARAM_CONTEXT = "com.arashdalir.calendaralarm.extra.context";


    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (AlarmCalenderHelper.checkPermissions(this)) {
            if (intent != null) {
                final String action = intent.getAction();
                if (ACTION_CREATE_ALARMS.equals(action)) {
                    handleActionCreateAlarms();
                } else if (ACTION_DELETE_ALARMS.equals(action)) {
                    handleActionDeleteAlarms();
                } else if (ACTION_START_SERVICE.equals(action)) {
                    handleActionStart();
                }
            }
        }
        else
        {
            Intent permissionsIntent = new Intent(this.getBaseContext(), PermissionCheckActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, permissionsIntent, 0);
            //TODO: create proper notification for user to ensure permissions are granted
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getApplicationContext(), NOTIFICATION_CHANNEL)
                    .setSmallIcon(R.drawable.ic_info_black_24dp)
                    .setContentTitle("Permissions Missing")
                    .setContentText("Please go to CalendarAlarm's Config page to resolve permissions issues.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                //CharSequence name = context.getString(R.string.channel_name);
                //String description = getString(R.string.channel_description);

                CharSequence name = "Calendar Alarm";
                String description = "This is CalendarAlarm's notification channel.";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                notificationManager.createNotificationChannel(channel);
            }

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(1, mBuilder.build());
        }
    }

    private void handleActionStart() {
        handleActionCreateAlarms();
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionCreateAlarms() {
        // TODO: Handle action Foo
        // Run query
        Cursor cursor = AlarmCalenderHelper.readEvents((Context) this);
        if(cursor.moveToFirst())
        {
            Calendar now = Calendar.getInstance();

            do {
                int eventCalendar = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                Long beginTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
                Calendar eventTime = Calendar.getInstance();
                eventTime.setTimeInMillis(beginTime);

                if (now.getTimeInMillis() < eventTime.getTimeInMillis())
                {
                    Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                    i.putExtra(AlarmClock.EXTRA_HOUR, eventTime.get(Calendar.HOUR_OF_DAY));
                    i.putExtra(AlarmClock.EXTRA_MINUTES, eventTime.get(Calendar.MINUTE));
                    i.putExtra(AlarmClock.EXTRA_MESSAGE, APP_PREFIX + eventTitle);
                    i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                    startActivity(i);
                }
            }while(cursor.moveToNext());
        }

        cursor.close();
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDeleteAlarms() {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
