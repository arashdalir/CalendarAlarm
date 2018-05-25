package com.arashdalir.calendaralarm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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
    public static final String ACTION_FETCH_REMINDERS = "com.arashdalir.calendaralarm.action.FETCH_REMINDERS";
    public static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";
    public static final String ACTION_MODIFY_REMINDER_ALARMS = "com.arashdalir.calendaralarm.action.MODIFY_REMINDER_ALARMS";
    public static final String ACTION_ADMINISTER_ALARMS = "com.arashdalir.calendaralarm.action.ADMINISTER_ALARMS";

    public static final String EXTRA_SNOOZE_ALARM = "alarm";

    private Alarms alarms = new Alarms(this);


    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    public static void startService(Context context) {
        startService(context, AlarmManagerService.ACTION_START_SERVICE);
    }

    public static void startService(Context context, String action) {
        Intent serviceIntent = new Intent(context, AlarmManagerService.class);
        serviceIntent.setAction(action);
        context.startService(serviceIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (PermissionsHelper.checkPermissions(this)) {
            if (intent != null) {
                final String action = intent.getAction();
                if (ACTION_FETCH_REMINDERS.equals(action)) {
                    handleActionModifyAlarms();
                } else if (ACTION_START_SERVICE.equals(action)) {
                    Notifier.getBuilder(this)
                            .setContentTitle(getString(R.string.notification_message_service_started))
                            .setContentText(getString(R.string.notification_message_service_started_description, getString(R.string.app_name)));

                    Notifier.notify(this, Notifier.NOTIFY_GENERAL, Notifier.LEVEL_INFO);

                    handleActionStart();
                } else if (ACTION_MODIFY_REMINDER_ALARMS.equals(action)) {
                    handleReminderAlarms();
                } else if (ACTION_ADMINISTER_ALARMS.equals(action)) {
                    handleAdministerAlarms();
                }
            }
        } else {
            Intent permissionsIntent = new Intent(this.getBaseContext(), PermissionCheckActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, permissionsIntent, 0);

            Notifier.getBuilder(this)
                    .setContentTitle(getString(R.string.notification_message_permission_missing_title))
                    .setContentText(getString(R.string.notification_message_permissions_missing_description))
                    .setContentIntent(pIntent)
                    .setAutoCancel(true);

            Notifier.notify(this, Notifier.NOTIFY_PERMISSIONS_MISSING, Notifier.LEVEL_ERROR);
        }
    }

    private void handleAdministerAlarms(){

    }

    private void handleReminderAlarms() {
        alarms.getStoredAlarms();
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (alarms.length() > 0) {
            for (int i = 0; i < alarms.length(); i++) {
                boolean createTimer = false;
                Alarms.Alarm alarm = alarms.getAlarm(i);


                if (!alarm.hasState(Alarms.Alarm.STATE_INACTIVE)) {

                    if (alarm.reminderTimePassed())
                    {
                        alarm.setState(Alarms.Alarm.STATE_REMINDER_TIME_PASSED);
                    }
                    else if(!alarm.isSnoozing()) {
                        if (!alarm.eventTimePassed()) {
                            createTimer = true;
                        }
                    }
                    else
                    {
                        alarm.markForDeletion();
                    }
                }

                PendingIntent pi = getSnoozeIntent(alarm, false);

                try{
                    if (createTimer)
                    {
                        if(pi == null)
                        {
                            pi = getSnoozeIntent(alarm, true);
                            am.setExact(AlarmManager.RTC_WAKEUP, alarm.getReminderTime().getTimeInMillis(), pi);
                        }
                    }
                    else if (pi != null)
                    {
                        am.cancel(pi);
                    }
                }
                catch (Exception e)
                {

                }
            }
            alarms.storeAlarms();
        }
    }

    private PendingIntent getSnoozeIntent(Alarms.Alarm alarm, boolean create){
        String actionName = getString(R.string.service_action_string, SnoozeActivity.ACTION_SNOOZE, alarm.getReminderId());
        Intent snooze = new Intent(this, SnoozeActivity.class);
        snooze.setAction(actionName);
        snooze.putExtra(EXTRA_SNOOZE_ALARM, alarm.toJSON().toString());

        PendingIntent pi = PendingIntent.getBroadcast(this, 0, snooze, PendingIntent.FLAG_NO_CREATE);

        if (pi == null && create)
        {
            pi = PendingIntent.getBroadcast(this, 0, snooze, 0);
        }

        return pi;
    }

    private void handleActionStart() {
        handleActionModifyAlarms();

    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void handleActionModifyAlarms() {
        Cursor cursor = CalenderHelper.readEvents(this);
        Integer[] allowedMethods = new Integer[]{
                CalendarContract.Reminders.METHOD_ALARM,
                CalendarContract.Reminders.METHOD_ALERT
        };

        alarms.getStoredAlarms();

        List<Integer> calendars = new ArrayList<>(Arrays.asList(StorageHelper.getCalendars(this)));

        Boolean vibrate = StorageHelper.getVibrate(this);
        String ringtone = StorageHelper.getRingtone(this);

        if (cursor.moveToFirst()) {
            do {
                int calendarId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Reminders.CALENDAR_ID));

                int eventId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                Long beginTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));

                Cursor rc = CalenderHelper.readReminders(this, eventId);

                if (rc.moveToFirst()) {
                    do {
                        int method = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.METHOD));

                        if (Arrays.asList(allowedMethods).contains(method)) {
                            int minutes = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.MINUTES));
                            String reminderId = Integer.toString(rc.getInt(rc.getColumnIndex(CalendarContract.Reminders._ID)));

                            Calendar eventTime = Calendar.getInstance();
                            eventTime.setTimeInMillis(beginTime);
                            Calendar reminderTime = (Calendar) eventTime.clone();
                            reminderTime.add(Calendar.MINUTE, -1 * minutes);

                            if (!alarms.alarmExists(reminderId)) {

                                alarms.getAlarm(reminderId)
                                        .set(
                                                calendarId,
                                                eventTitle,
                                                reminderTime,
                                                eventTime,
                                                ringtone,
                                                vibrate
                                        );
                            }
                        }
                    } while (rc.moveToNext());
                }
                rc.close();
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        Alarms.AlarmsStati status = alarms.filterCalendars(calendars);
        alarms.storeAlarms();

        if (status.getTotal() > 0) {
            if (status.getAdded() > 0) {
                Intent mainIntent = new Intent(this, AlarmListActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

                Notifier.getBuilder(this).setContentTitle(getString(R.string.notification_message_alarms_modified))
                        .setContentIntent(pIntent)
                        .setContentText(getString(R.string.notification_message_alarms_modified_description, status.getTotal(), status.getAdded(), status.getDeleted()));

                Notifier.notify(this, Notifier.NOTIFY_GENERAL, Notifier.LEVEL_WARNING);
            } else {
                Notifier.getBuilder(this).setContentTitle(getString(R.string.notification_message_alarms_not_modified))
                        .setContentText(getString(R.string.notification_message_alarms_not_modified_description));

                Notifier.notify(this, Notifier.NOTIFY_GENERAL, Notifier.LEVEL_WARNING);
            }
        }

        StorageHelper.saveLastExecutionTime(this, Calendar.getInstance().getTime());
        handleReminderAlarms();
    }
}
