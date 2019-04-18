package com.arashdalir.calendaralarm;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.TimeZone;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.content.Intent.makeMainActivity;
import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

class ServiceHelper {
    static final String ACTION_CHECK_REMINDER_ALARMS = "com.arashdalir.calendaralarm.action.MODIFY_REMINDER_ALARMS";
    static final String ACTION_START_SERVICE = "com.arashdalir.calendaralarm.action.START_SERVICE";
    static final String ACTION_REMINDER_SNOOZE = "com.arashdalir.calendaralarm.action.REMINDER_SNOOZE";
    static final String ACTION_REMINDER_CANCEL = "com.arashdalir.calendaralarm.action.REMINDER_CANCEL";
    static final String ACTION_DO_JOB = "com.arashdalir.calendaralarm.action.DO_JOB";

    static final String EXTRA_SNOOZE_ALARM = "alarm";
    private static Observable obAdapter = null;

    static private Vibrator vibrator = null;
    static private MediaPlayer player = null;
    static private AudioManager audioManager;
    static private int reminderQueue = 0;

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
                } else if (ServiceHelper.ACTION_DO_JOB.equals(action)) {
                    handleDoJob(action);
                }
            } else {
                handlerNoCalendar();
            }
        } else {
            handleNoPermission();
        }
    }

    private void handleDoJob(String action) {
        Alarms.AlarmsStati stati = readNotifications();
        handleCreateNotificationAlarms();

        boolean modified = false;

        createRedoTimer();

        if (stati.getTotal() > 0) {
            NotificationCompat.Builder builder = Notifier.getBuilder(context);
            Intent mainIntent = new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pIntent = PendingIntent.getActivity(context, 5, mainIntent, 0);

            if (stati.getAdded() > 0) {

                modified = true;

                String notificationMessage = context.getString(R.string.notification_message_alarms_modified_description, stati.getTotal(), stati.getAdded(), stati.getDeleted());

                builder.setContentTitle(context.getString(R.string.notification_message_alarms_modified))
                        .setContentText(notificationMessage)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(notificationMessage)
                        );

            } else {
                if (action.equals(ServiceHelper.ACTION_START_SERVICE)) {
                    builder.setContentTitle(context.getString(R.string.notification_message_alarms_not_modified))
                            .setContentText(context.getString(R.string.notification_message_alarms_not_modified_description));

                }
            }
            builder.setAutoCancel(true)
                    .setContentIntent(pIntent);

            if (modified) {
                Notifier.notify(context, builder, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_MAX);
            }
        }
    }

    private void createRedoTimer() {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(AlarmManagerService.ALARM_SERVICE);

            if (am != null) {
                Intent redoAction = new Intent(context, Receiver.class);
                redoAction.setAction(ServiceHelper.ACTION_DO_JOB);

                Calendar timer = Calendar.getInstance();
                //timer.add(Calendar.HOUR, 1);
                timer.add(Calendar.MINUTE, 1);
                PendingIntent pi = PendingIntent.getBroadcast(context, 1, redoAction, PendingIntent.FLAG_UPDATE_CURRENT);
                am.setExact(AlarmManager.RTC_WAKEUP, timer.getTimeInMillis(), pi);

            }
        } catch (Exception e) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleSnooze(Intent intent, String action) {
        CalendarApplication app = (CalendarApplication) context.getApplicationContext();
        final AlarmListAdapter adapter = app.getAdapter(context);
        Alarms.Alarm alarm = getAlarmByIntent(context, intent);

        if (alarm == null) {
            return;
        }

        alarm.stopAlarm();
        Notifier.cancelNotification(context, alarm.getEventId());
        ServiceHelper.stopNotificationSound();

        int state = 0;
        switch (action) {
            case ServiceHelper.ACTION_REMINDER_SNOOZE:
                state = Alarms.Alarm.STATE_SNOOZED;
                Calendar reminderTime = alarm.getReminderTime();
                reminderTime.add(Calendar.MINUTE, 1);
                alarm.setReminderTime(reminderTime);
                break;

            case ServiceHelper.ACTION_REMINDER_CANCEL:
                state = Alarms.Alarm.STATE_REMINDER_TIME_PASSED;
                alarm.resetState(Alarms.Alarm.STATE_SNOOZED);
                break;

        }

        if (state != 0) {
            alarm.setState(state);
            adapter.storeData(context);
            createNotificationAlarm(alarm, context);
        }

        ServiceHelper.observeAdapter().hasChanged();
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

        int alarmCount = adapter.getItemCount();
        if (alarmCount > 0) {
            for (int i = 0; i < alarmCount; i++) {
                Alarms.Alarm alarm = adapter.getItem(i, true);

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
        try {
            PendingIntent pi = getSnoozeIntent(alarm);

            if (alarm.checkTimes()) {
                if (am != null) {
                    am.setExact(AlarmManager.RTC_WAKEUP, alarm.getReminderTime().getTimeInMillis(), pi);
                }
            }
            else
            {
                if (pi != null)
                {
                    pi.cancel();
                }
            }
        } catch (Exception e) {

        }
    }

    private PendingIntent getSnoozeIntent(Alarms.Alarm alarm) {
        String actionName = context.getString(R.string.service_action_string, SnoozeActivity.ACTION_SNOOZE, alarm.getReminderId());
        Intent snooze = new Intent(context, Receiver.class);
        snooze.setAction(actionName);
        snooze.putExtra(ServiceHelper.EXTRA_SNOOZE_ALARM, alarm.toJSON().toString());

        return PendingIntent.getBroadcast(context, 0, snooze, PendingIntent.FLAG_UPDATE_CURRENT);
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

        handleDoJob(ServiceHelper.ACTION_START_SERVICE);
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
        alarm.startAlarm();
        ServiceHelper.startNotificationSound(context, alarm.isVibrate());

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


    private Alarms.AlarmsStati readNotifications() {
        Calendar now = Calendar.getInstance();

        int rawOffset = now.getTimeZone().getRawOffset();

        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.add(Calendar.MILLISECOND, -1 * rawOffset);
        long beginms = now.getTimeInMillis();
        now.add(Calendar.DATE, 8);
        long endms = now.getTimeInMillis();

        Cursor cursor = CalendarHelper.readEvents(context, beginms, endms);
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);

        //adapter.reset(false);

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
                String timeZone = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.EVENT_TIMEZONE));

                beginTime = beginTime + (TimeZone.getTimeZone(timeZone).getRawOffset() - rawOffset);

                if (!calendars.contains(calendarId) || beginms > beginTime) {
                    continue;
                }

                int eventId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID));
                String eventTitle = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));

                boolean isAllDay = false;

                Calendar eventTime = Calendar.getInstance();
                eventTime.setTimeInMillis(beginTime);

                int allDay = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY));
                if (allDay == 1)
                {
                    isAllDay = true;
                }
                //DateFormat df = android.text.format.DateFormat.getDateFormat(context);
                //DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);

                Cursor rc = CalendarHelper.readReminders(context, eventId);

                if (rc.moveToFirst()) {
                    do {
                        int method = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.METHOD));

                        if (Arrays.asList(allowedMethods).contains(method)) {
                            int minutes = rc.getInt(rc.getColumnIndex(CalendarContract.Reminders.MINUTES));
                            String reminderId = Integer.toString(rc.getInt(rc.getColumnIndex(CalendarContract.Reminders._ID)));

                            Calendar reminderTime = (Calendar) eventTime.clone();
                            reminderTime.add(Calendar.MINUTE, -1 * minutes);

                            adapter.getAlarm(reminderId)
                                    .set(
                                            calendarId,
                                            eventTitle,
                                            reminderTime,
                                            eventTime,
                                            ringtone,
                                            vibrate,
                                            eventId,
                                            isAllDay
                                    );

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

        observeAdapter().notifyObservers(adapter);

        return status;
    }

    private static void startNotificationSound(Context context, boolean shouldVibrate){
        if (reminderQueue == 0)
        {
            ServiceHelper.vibrate(context, shouldVibrate);
            ServiceHelper.playRingtone(context);
        }

        reminderQueue++;
    }

    private static void stopNotificationSound(){
        reminderQueue = Math.max(reminderQueue-1, 0);

        if (reminderQueue == 0)
        {
            ServiceHelper.cancelVibrate();
            ServiceHelper.stopRingtone();
        }


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void vibrate(Context context, boolean shouldVibrate) {
        if (vibrator == null) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (shouldVibrate) {
            if (vibrator.hasVibrator()) {
                long[] pattern = new long[]{0, 500, 0, 0, 500};
                vibrator.vibrate(pattern, 0,
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                );
            }
        }
    }

    private static void cancelVibrate() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private static void playRingtone(Context context) {
        String ringtoneName = StorageHelper.getRingtone(context, true);
        Uri ringtoneUri = Uri.parse(ringtoneName);

        try {
            if (!Uri.EMPTY.equals(ringtoneUri)) {
                if (audioManager == null) {
                    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                }

                player = MediaPlayer.create(context, ringtoneUri);
                player.stop();

                int sMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                int sVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                //audioManager.setStreamVolume(AudioManager.STREAM_ALARM, sVolume, sVolume);

                player.setLooping(true);
                player.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                                //.setUsage(AudioAttributes.USAGE_ALARM)
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setFlags(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                );

                player.prepare();
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AUDIOFOCUS_GAIN_TRANSIENT);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, sMax, 0);
                player.start();
            }
        } catch (Exception e) {

        }
    }

    private static void stopRingtone() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
                player.release();
                player = null;
            }
        }

        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }
}
