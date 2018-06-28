package com.arashdalir.calendaralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class ServiceHelper {
    public static final String ACTION_SET_WAKEUP_TIMERS = "com.arashdalir.calendaralarm.action.SET_WAKEUP_TIMERS";
    public static final String ACTION_CHECK_REMINDER_ALARMS = "com.arashdalir.calendaralarm.action.MODIFY_REMINDER_ALARMS";
    public static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";

    public static final String EXTRA_SNOOZE_ALARM = "alarm";
    private static Observable obAdapter = null;

    private Context context;

    static Observable observeAdapter() {
        if (obAdapter == null) {
            obAdapter = new Observable();
        }

        return obAdapter;
    }

    ServiceHelper(Context context){
        this.context = context;
    }

    public void readEventAlarms() {
        Cursor cursor = CalendarHelper.readEvents(context);
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);

        Integer[] allowedMethods = new Integer[]{
                CalendarContract.Reminders.METHOD_ALARM,
                CalendarContract.Reminders.METHOD_ALERT
        };

        List<Integer> calendars = new ArrayList<>(Arrays.asList(StorageHelper.getCalendars(context)));

        Boolean vibrate = StorageHelper.getVibrate(context);
        String ringtone = StorageHelper.getRingtone(context);

        if (cursor.moveToFirst()) {
            do {
                int calendarId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Reminders.CALENDAR_ID));

                if (!calendars.contains(calendarId)) {
                    continue;
                }

                int eventId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                Long beginTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));

                Cursor rc = CalendarHelper.readReminders(context, eventId);

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

                            if (!adapter.alarmExists(reminderId)) {

                                adapter.getAlarms().getAlarm(reminderId)
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

        adapter.getAlarms().sort();

        Alarms.AlarmsStati status = adapter.getAlarms().filterCalendars(calendars);

        if (status.getTotal() > 0) {
            if (status.getAdded() > 0) {
                Intent mainIntent = new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                PendingIntent pIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);

                String notificationMessage = context.getString(R.string.notification_message_alarms_modified_description, status.getTotal(), status.getAdded(), status.getDeleted());
                Notifier.getBuilder(context).setContentTitle(context.getString(R.string.notification_message_alarms_modified))
                        .setContentIntent(pIntent)
                        .setContentText(notificationMessage)
                        .setAutoCancel(true)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(notificationMessage)
                        );

                Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
            } else {
                Notifier.getBuilder(context).setContentTitle(context.getString(R.string.notification_message_alarms_not_modified))
                        .setContentText(context.getString(R.string.notification_message_alarms_not_modified_description));

                Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
            }
        }

        StorageHelper.saveLastExecutionTime(context, Calendar.getInstance().getTime());
        observeAdapter().notifyObservers(adapter);
    }

    public void onHandleWork(Intent intent) {
        if (PermissionsHelper.checkPermissions(context)) {
            if (StorageHelper.getCalendars(context).length > 0) {
                final String action = intent.getAction();
                if (ServiceHelper.ACTION_CHECK_REMINDER_ALARMS.equals(action)) {
                    handleAlarmReminders();
                } else if (ServiceHelper.ACTION_SET_WAKEUP_TIMERS.equals(action)) {
                    handleSetupTimers();
                } else if (ServiceHelper.ACTION_START_SERVICE.equals(action)) {
                    Intent appIntent = new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                    PendingIntent pi = PendingIntent.getActivity(context, 0, appIntent, 0);

                    Notifier.getBuilder(context)
                            .setContentTitle(context.getString(R.string.notification_message_service_started))
                            .setContentIntent(pi)
                            .setContentText(
                                    context.getString(
                                            R.string.notification_message_service_started_description,
                                            context.getString(R.string.app_name)
                                    )
                            );

                    Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_HIGH);

                    Log.i(context.getClass().toString(), "Alarm Manager Service Started.");

                    handleReadReminders();
                }
            } else {
                Intent calendarSelectionIntent = new Intent(context, CalendarSelectionActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                PendingIntent pIntent = PendingIntent.getActivity(context, 0, calendarSelectionIntent, 0);

                Notifier.getBuilder(context)
                        .setContentTitle(context.getString(R.string.notification_message_no_calendars_selected))
                        .setContentText(context.getString(R.string.notification_message_no_calendars_selected_description))
                        .setContentIntent(pIntent)
                        .setAutoCancel(true);

                Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
            }
        } else {
            Intent permissionsIntent = new Intent(context, PermissionCheckActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, permissionsIntent, 0);

            Notifier.getBuilder(context)
                    .setContentTitle(context.getString(R.string.notification_message_permission_missing_title))
                    .setContentText(context.getString(R.string.notification_message_permissions_missing_description))
                    .setContentIntent(pIntent)
                    .setAutoCancel(true);

            Notifier.notify(context, Notifier.NOTIFY_PERMISSIONS_MISSING, NotificationCompat.PRIORITY_MAX);
        }
    }

    private void handleSetupTimers() {

    }

    private void handleAlarmReminders() {
        AlarmManager am = (AlarmManager) context.getSystemService(AlarmManagerService.ALARM_SERVICE);
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);

        if (adapter.getItemCount() > 0) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                boolean createTimer = false;
                Alarms.Alarm alarm = adapter.getItem(i);

                createTimer = alarm.checkTimes();

                PendingIntent pi = getSnoozeIntent(alarm, false);

                try {
                    if (createTimer) {
                        if (pi == null) {
                            pi = getSnoozeIntent(alarm, true);
                            am.setExact(AlarmManager.RTC_WAKEUP, alarm.getReminderTime().getTimeInMillis(), pi);
                        }
                    } else if (pi != null) {
                        am.cancel(pi);
                    }
                } catch (Exception e) {

                }
            }
            adapter.storeData(context.getApplicationContext());
        }
    }

    private PendingIntent getSnoozeIntent(Alarms.Alarm alarm, boolean create) {
        String actionName = context.getString(R.string.service_action_string, SnoozeActivity.ACTION_SNOOZE, alarm.getReminderId());
        Intent snooze = new Intent(context, SnoozeActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        snooze.setAction(actionName);
        snooze.putExtra(ServiceHelper.EXTRA_SNOOZE_ALARM, alarm.toJSON().toString());

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, snooze, PendingIntent.FLAG_NO_CREATE);

        if (pi == null && create) {
            pi = PendingIntent.getBroadcast(context, 0, snooze, 0);
        }

        return pi;
    }
    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */

    private void handleReadReminders() {
        readEventAlarms();
        handleAlarmReminders();
    }
}
