package com.arashdalir.calendaralarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class Notifier {
    public static final int NOTIFY_PERMISSIONS_MISSING = 1;
    public static final int NOTIFY_GENERAL = 2;
    public static final int NOTIFY_SNOOZE = 3;

    private static final String NOTIFICATION_CHANNEL = "com.arashdalir.calendaralarm.NOTIFICATION_CHANNEL";

    private static NotificationCompat.Builder mBuilder = null;

    public static NotificationCompat.Builder getBuilder(Context context) {
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(context, Notifier.NOTIFICATION_CHANNEL);
        }
        return mBuilder;
    }

    public static boolean notify(Context context, int notificationId, int priority) {
        mBuilder.setPriority(priority)
                .setSmallIcon(R.drawable.ic_info_black_24dp);

        try {
            NotificationManager notificationManager = prepareNotificationChannel(context);
            notificationManager.cancel(notificationId);
            Notification notification  = mBuilder.build();

            notificationManager.notify(notificationId, notification);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static NotificationManager prepareNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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

        return notificationManager;
    }

    public static void showToast(Context context, String message, int length) {
        Toast toast = Toast.makeText(context, message, length);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
        toast.show();
    }

    public static void showSnackBar(View rv, String message, int length) {
        showSnackBar(rv, message, length, null);
    }

    public static void showSnackBar(View rv, String message, int length, snackBarAction[] actions) {
        Snackbar snackbar = Snackbar.make(rv, message, length);

        if (actions != null && actions.length > 0) {
            for (snackBarAction action : actions) {
                snackbar.setAction(action.message, action.onClickListener);
                snackbar.setActionTextColor(action.actionTextColor);
            }
        }
        snackbar.show();
    }

    public static void cancelNotification(Context context, int notificationId) {
        try {
            NotificationManager notificationManager = prepareNotificationChannel(context);
            notificationManager.cancel(notificationId);
        } catch (Exception e) {
        }
    }

    public static class snackBarAction {
        public String message;
        public View.OnClickListener onClickListener;
        public int actionTextColor;
    }
}

