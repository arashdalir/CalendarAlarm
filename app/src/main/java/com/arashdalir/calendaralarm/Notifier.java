package com.arashdalir.calendaralarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class Notifier {
    static final int NOTIFY_PERMISSIONS_MISSING = 1;
    static final int NOTIFY_GENERAL = 2;
    static final int NOTIFY_SNOOZE = 3;

    private static NotificationManager notificationManager;

    private static final String NOTIFICATION_CHANNEL = "com.arashdalir.calendaralarm.NOTIFICATION_CHANNEL";


    static NotificationCompat.Builder getBuilder(Context context) {
        return new NotificationCompat.Builder(context, Notifier.NOTIFICATION_CHANNEL);
    }

    static boolean notify(Context context, NotificationCompat.Builder builder, int notificationId, int priority) {
        builder.setPriority(priority)
                .setSmallIcon(R.drawable.ic_info_black_24dp);

        try {
            prepareNotificationChannel(context).cancel(notificationId);
            prepareNotificationChannel(context).notify(notificationId, builder.build());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static NotificationManager prepareNotificationChannel(Context context) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = context.getString(R.string.app_name);
                String description = context.getString(R.string.app_name) + ' ' + context.getString(R.string.notification_channel_name);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }

        return notificationManager;
    }

    static void showToast(Context context, String message, int length) {
        Toast toast = Toast.makeText(context, message, length);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
        toast.show();
    }

    static void showSnackBar(View rv, String message, int length) {
        showSnackBar(rv, message, length, null);
    }

    static void showSnackBar(View rv, String message, int length, SnackBarAction[] actions) {
        Snackbar snackbar = Snackbar.make(rv, message, length);

        if (actions != null && actions.length > 0) {
            for (SnackBarAction action : actions) {
                snackbar.setAction(action.message, action.onClickListener);
                snackbar.setActionTextColor(action.actionTextColor);
            }
        }
        snackbar.show();
    }

    static void cancelNotification(Context context, int notificationId) {
        try {
            prepareNotificationChannel(context).cancel(notificationId);
        } catch (Exception e) {
        }
    }

    static void showSnooze(Context context, Alarms.Alarm alarm) {
        Intent snooze = new Intent(context, AlarmManagerService.class);
        snooze.setAction(ServiceHelper.ACTION_REMINDER_SNOOZE);
        snooze.putExtra(ServiceHelper.EXTRA_SNOOZE_ALARM, alarm.toJSON().toString());
        PendingIntent pSnooze = PendingIntent.getService(context, (int) System.currentTimeMillis(), snooze, 0);

        Intent cancel = new Intent(context, AlarmManagerService.class);
        cancel.setAction(ServiceHelper.ACTION_REMINDER_CANCEL);
        cancel.putExtra(ServiceHelper.EXTRA_SNOOZE_ALARM, alarm.toJSON().toString());
        PendingIntent pCancel = PendingIntent.getService(context, (int) System.currentTimeMillis(), cancel, 0);

        NotificationCompat.Builder builder = Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_alarming, alarm.getTitle()))
                .setContentText(alarm.getCalendarName(context))
                .setColor(alarm.getCalendarColor(context))
                .setOngoing(true)
                .addAction(R.drawable.ic_info_black_24dp, context.getString(R.string.activity_snooze_snooze), pSnooze)
                .addAction(R.drawable.ic_info_black_24dp, context.getString(R.string.activity_snooze_cancel), pCancel);

        Notifier.notify(context, builder, Notifier.NOTIFY_SNOOZE, NotificationCompat.PRIORITY_MAX);
    }

    static class SnackBarAction {
        String message;
        View.OnClickListener onClickListener;
        int actionTextColor;
    }
}

