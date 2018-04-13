package com.arashdalir.calendaralarm;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

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
    public static final String ACTION_MANAGE_ALARMS = "com.arashdalir.calendaralarm.action.MANAGE_ALARMS";
    public static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";

    private static final String APP_PREFIX = "AutoAlarm - ";

    // TODO: Rename parameters
    //private static final String PARAM_CONTEXT = "com.arashdalir.calendaralarm.extra.context";


    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    public static void startService(Context context)
    {
        Intent serviceIntent = new Intent(context, AlarmManagerService.class);
        serviceIntent.setAction(AlarmManagerService.ACTION_START_SERVICE);
        context.startService(serviceIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (PermissionsHelper.checkPermissions(this)) {
            if (intent != null) {
                final String action = intent.getAction();
                if (ACTION_MANAGE_ALARMS.equals(action)) {
                    handleActionCreateAlarms();
                } else if (ACTION_START_SERVICE.equals(action)) {
                    handleActionStart();

                    Intent settingsActivity = new Intent(this.getBaseContext(), SettingsActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, settingsActivity, 0);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), Notifier.NOTIFICATION_CHANNEL)
                            .setContentTitle(getString(R.string.notification_message_service_started))
                            .setContentText(getString(R.string.notification_message_service_started_description, getString(R.string.app_name)))
                            .setContentIntent(pIntent)
                            .setAutoCancel(true);

                    Notifier.notify(this, mBuilder, Notifier.NOTIFY_SERVICE_STARTED, NotificationCompat.PRIORITY_HIGH);

                }
            }
        } else {
            Intent permissionsIntent = new Intent(this.getBaseContext(), PermissionCheckActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, permissionsIntent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), Notifier.NOTIFICATION_CHANNEL)
                    .setContentTitle(getString(R.string.notification_message_permission_missing_title))
                    .setContentText(getString(R.string.notification_message_permissions_missing_description))
                    .setContentIntent(pIntent)
                    .setAutoCancel(true);

            Notifier.notify(this, mBuilder, Notifier.NOTIFY_PERMISSIONS_MISSING, NotificationCompat.PRIORITY_HIGH);
        }
    }

    private void handleActionStart() {
        handleActionCreateAlarms();
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void handleActionCreateAlarms() {
        // TODO: Handle action Foo
        // Run query
        Cursor cursor = CalenderHelper.readEvents(this);
        Integer[] allowedMethods = new Integer[]{
                CalendarContract.Reminders.METHOD_ALARM,
                CalendarContract.Reminders.METHOD_ALERT
        };

        if (cursor.moveToFirst()) {
            Calendar now = Calendar.getInstance();

            do {
                int eventCalendar = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID));
                int eventId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                Long beginTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));

                Cursor rc = CalenderHelper.readReminders(this, eventId);

                if (rc.moveToFirst()) {
                    do {
                        int method = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.METHOD));

                        if (Arrays.asList(allowedMethods).contains(method)) {
                            int minutes = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.MINUTES));

                            Calendar eventTime = Calendar.getInstance();
                            eventTime.setTimeInMillis(beginTime);
                            eventTime.add(Calendar.MINUTE, -1 * minutes);

                            if (now.getTimeInMillis() < eventTime.getTimeInMillis()) {
                                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                                i.putExtra(AlarmClock.EXTRA_HOUR, eventTime.get(Calendar.HOUR_OF_DAY));
                                i.putExtra(AlarmClock.EXTRA_MINUTES, eventTime.get(Calendar.MINUTE));
                                i.putExtra(AlarmClock.EXTRA_MESSAGE, APP_PREFIX + eventTitle);
                                i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                                startActivity(i);
                            }
                        }
                    }while (rc.moveToNext());

                    rc.close();
                }

            }
            while (cursor.moveToNext()) ;

            cursor.close();
        }
    }
}
