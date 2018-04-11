package com.arashdalir.calendaralarm;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.support.v4.app.NotificationCompat;

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

    private static final String APP_PREFIX = "AutoAlarm - ";

    // TODO: Rename parameters
    //private static final String PARAM_CONTEXT = "com.arashdalir.calendaralarm.extra.context";


    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = (Context) this;

        if (AlarmCalenderHelper.checkPermissions(context)) {
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
            //TODO: create proper notification for user to ensure permissions are granted
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(textTitle)
                    .setContentText(textContent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
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
