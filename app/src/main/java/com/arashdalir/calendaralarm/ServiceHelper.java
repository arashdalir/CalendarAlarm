package com.arashdalir.calendaralarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.CalendarContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

class ServiceHelper {
    static final String ACTION_CHECK_REMINDER_ALARMS = "com.arashdalir.calendaralarm.action.MODIFY_REMINDER_ALARMS";
    static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";
    static final String ACTION_REMINDER_SNOOZE = "com.arashdalir.calendaralarm.action.REMINDER_SNOOZE";
    static final String ACTION_REMINDER_CANCEL = "com.arashdalir.calendaralarm.action.REMINDER_CANCEL";
    static final String ACTION_CREATE_HOURLY_CHECK = "com.arashdalir.calendaralarm.action.CREATE_HOURLY_CHECK";

    static final String EXTRA_SNOOZE_ALARM = "alarm";
    private static Observable obAdapter = null;

    private Context context;

    static Observable observeAdapter() {
        if (obAdapter == null) {
            obAdapter = new Observable();
        }

        return obAdapter;
    }

    ServiceHelper(Context context) {
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    void onHandleWork(Intent intent) {
        if (intent == null) {
            return;
        }

        if (PermissionsHelper.checkPermissions(context)) {
            if (StorageHelper.getCalendars(context).length > 0) {
                final String action = intent.getAction();
                if (ServiceHelper.ACTION_CHECK_REMINDER_ALARMS.equals(action)) {
                    handleCreateNotificationAlarms();
                } else if (ServiceHelper.ACTION_START_SERVICE.equals(action)) {
                    handleStartService();
                } else if (
                        ServiceHelper.ACTION_REMINDER_SNOOZE.equals(action) ||
                                ServiceHelper.ACTION_REMINDER_CANCEL.equals(action)
                        ) {
                    handleSnooze(intent, action);
                } else if (ServiceHelper.ACTION_CREATE_HOURLY_CHECK.equals(action)) {
                    handleHourlyCheck();
                }
            } else {
                handlerNoCalendar();
            }
        } else {
            handleNoPermission();
        }
    }

    private void handleHourlyCheck() {
        AlarmManager am = (AlarmManager) context.getSystemService(AlarmManagerService.ALARM_SERVICE);

        Intent hourlyCheck = new Intent(context, AlarmManagerService.class);
        hourlyCheck.setAction(ServiceHelper.ACTION_START_SERVICE);

        PendingIntent pi = PendingIntent.getBroadcast(context, 1, hourlyCheck, PendingIntent.FLAG_NO_CREATE);

        try {
            if (pi == null) {
                if (am != null) {
                    Calendar timer = Calendar.getInstance();
                    //timer.add(Calendar.HOUR, 1);
                    timer.add(Calendar.MINUTE, 1);
                    pi = PendingIntent.getBroadcast(context, 1, hourlyCheck, PendingIntent.FLAG_NO_CREATE);
                    am.setExact(AlarmManager.RTC_WAKEUP, timer.getTimeInMillis(), pi);
                }
            }
        } catch (Exception e) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleSnooze(Intent intent, String action) {
        CalendarApplication app = (CalendarApplication) context.getApplicationContext();
        AlarmListAdapter adapter = app.getAdapter(context);
        Alarms.Alarm alarm = getAlarmByIntent(context, intent);

        if (alarm == null) {
            return;
        }

        alarm.stopAlarm();

        int state = 0;
        switch (action) {
            case ServiceHelper.ACTION_REMINDER_SNOOZE:
                state = Alarms.Alarm.STATE_SNOOZED;
                alarm.getReminderTime().add(Calendar.SECOND, 30);
                createNotificationAlarm(alarm, context);
                break;

            case ServiceHelper.ACTION_REMINDER_CANCEL:
                state = Alarms.Alarm.STATE_REMINDER_TIME_PASSED;
                break;

        }

        if (state != 0) {
            alarm.setState(state);
            adapter.storeData(context);
            Notifier.cancelNotification(context, Notifier.NOTIFY_SNOOZE);
        }

        handleCreateNotificationAlarms();
    }

    private void handleNoPermission() {
        Intent permissionsIntent = new Intent(context, PermissionCheckActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(context, 2, permissionsIntent, 0);

        NotificationCompat.Builder builder = Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_permission_missing_title))
                .setContentText(context.getString(R.string.notification_message_permissions_missing_description))
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        Notifier.notify(context, builder, Notifier.NOTIFY_PERMISSIONS_MISSING, NotificationCompat.PRIORITY_MAX);
    }

    private void handlerNoCalendar() {
        Intent calendarSelectionIntent = new Intent(context, CalendarSelectionActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pIntent = PendingIntent.getActivity(context, 3, calendarSelectionIntent, 0);

        NotificationCompat.Builder builder = Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_no_calendars_selected))
                .setContentText(context.getString(R.string.notification_message_no_calendars_selected_description))
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        Notifier.notify(context, builder, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleCreateNotificationAlarms() {
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);

        if (adapter.getItemCount() > 0) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                Alarms.Alarm alarm = adapter.getItem(i);

                if (alarm == null) {
                    continue;
                }

                createNotificationAlarm(alarm, context);
            }
            adapter.storeData(context.getApplicationContext());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createNotificationAlarm(Alarms.Alarm alarm, Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(AlarmManagerService.ALARM_SERVICE);
        boolean createTimer = alarm.checkTimes();

        PendingIntent pi = getSnoozeIntent(alarm, false);

        try {
            if (createTimer) {
                if (pi == null) {
                    pi = getSnoozeIntent(alarm, true);
                    if (am != null) {
                        am.setExact(AlarmManager.RTC_WAKEUP, alarm.getReminderTime().getTimeInMillis(), pi);
                    }
                }
            } else if (pi != null) {
                if (am != null) {
                    am.cancel(pi);
                }
            }
        } catch (Exception e) {

        }
    }

    private PendingIntent getSnoozeIntent(Alarms.Alarm alarm, boolean create) {
        String actionName = context.getString(R.string.service_action_string, SnoozeActivity.ACTION_SNOOZE, alarm.getReminderId());
        Intent snooze = new Intent(context, Receiver.class);
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleStartService() {
        Intent appIntent = new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(context, 4, appIntent, 0);

        NotificationCompat.Builder builder = Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_service_started))
                .setContentIntent(pi)
                .setContentText(
                        context.getString(
                                R.string.notification_message_service_started_description,
                                context.getString(R.string.app_name)
                        )
                );

        Notifier.notify(context, builder, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_HIGH);

        Log.i(context.getClass().toString(), "Alarm Manager Service Started.");

        readNotifications();
        handleCreateNotificationAlarms();
        handleHourlyCheck();
    }

    static void doAlarm(Context context, Intent intent) {
        Alarms.Alarm alarm = getAlarmByIntent(context, intent);

        if (alarm == null) {
            return;
        }

        if (alarm.hasState(Alarms.Alarm.STATE_INACTIVE) || alarm.hasState(Alarms.Alarm.STATE_DELETED)) {
            return;
        }

        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);
        alarm.startAlarm(context);

        Notifier.showSnooze(context, alarm);
        adapter.storeData(context);
    }

    private static Alarms.Alarm getAlarmByIntent(Context context, Intent intent) {
        Alarms.Alarm alarm = null;
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);

        try {
            JSONObject alm = new JSONObject(intent.getStringExtra(ServiceHelper.EXTRA_SNOOZE_ALARM));

            String reminderId = Alarms.Alarm.getIdFromJSON(alm);
            alarm = adapter.getAlarm(reminderId, false);

            if (alarm != null) {
                alarm = adapter.getAlarm(reminderId).set(alm);
            }

        } catch (Exception e) {

        }

        return alarm;
    }


    private void readNotifications() {
        Calendar now = Calendar.getInstance();
        long beginms = now.getTimeInMillis();
        now.add(Calendar.DATE, 7);
        long endms = now.getTimeInMillis();

        Cursor cursor = CalendarHelper.readEvents(context, beginms, endms);
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
                Long beginTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));

                if (!calendars.contains(calendarId) || beginms > beginTime) {
                    continue;
                }

                int eventId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));

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

                                adapter.getAlarm(reminderId)
                                        .set(
                                                calendarId,
                                                eventTitle,
                                                reminderTime,
                                                eventTime,
                                                ringtone,
                                                vibrate,
                                                eventId
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
            NotificationCompat.Builder builder = Notifier.getBuilder(context);
            Intent mainIntent = new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pIntent = PendingIntent.getActivity(context, 5, mainIntent, 0);

            if (status.getAdded() > 0) {

                String notificationMessage = context.getString(R.string.notification_message_alarms_modified_description, status.getTotal(), status.getAdded(), status.getDeleted());

                builder.setContentTitle(context.getString(R.string.notification_message_alarms_modified))
                        .setContentText(notificationMessage)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(notificationMessage)
                        );

            } else {
                builder.setContentTitle(context.getString(R.string.notification_message_alarms_not_modified))
                        .setContentText(context.getString(R.string.notification_message_alarms_not_modified_description));
            }
            builder.setAutoCancel(true)
                    .setContentIntent(pIntent);
            Notifier.notify(context, builder, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
        }

        StorageHelper.saveLastExecutionTime(context, Calendar.getInstance().getTime());
        observeAdapter().notifyObservers(adapter);
    }
}
