package com.arashdalir.calendaralarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class Notifier {
    public static final int NOTIFY_PERMISSIONS_MISSING = 1;
    public static final int NOTIFY_SERVICE_STARTED = 2;

    public static final String NOTIFICATION_CHANNEL = "com.arashdalir.calendaralarm.NOTIFICATION_CHANNEL";

    public static final int LEVEL_ERROR = NotificationCompat.PRIORITY_MAX;
    public static final int LEVEL_INFO = NotificationCompat.PRIORITY_DEFAULT;
    public static final int LEVEL_WARNING = NotificationCompat.PRIORITY_HIGH;

    public static boolean notify(Context context, NotificationCompat.Builder mBuilder, int notificationId, int priority) {

        switch (priority) {
            case LEVEL_ERROR:
                mBuilder.setPriority(LEVEL_ERROR)
                        .setSmallIcon(R.drawable.ic_info_black_24dp);
                break;

            case LEVEL_WARNING:
                mBuilder.setPriority(LEVEL_WARNING)
                        .setSmallIcon(R.drawable.ic_info_black_24dp);

                break;

            case LEVEL_INFO:
            default:
                mBuilder.setPriority(LEVEL_INFO)
                        .setSmallIcon(R.drawable.ic_info_black_24dp);
                break;
        }

        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                //CharSequence name = context.getString(R.string.channel_name);
                //String description = getString(R.string.channel_description);

                CharSequence name = context.getString(R.string.app_name);
                String description = context.getString(R.string.app_name) + ' ' + context.getString(R.string.notification_channel_name);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                notificationManager.createNotificationChannel(channel);
            }

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId, mBuilder.build());

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
