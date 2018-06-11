package com.arashdalir.calendaralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class CalendarChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        AlarmManagerService.enqueueWork(context);
        Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_looking_for_events))
                .setContentText(context.getString(R.string.notification_message_looking_for_events_description, context.getString(R.string.app_name)));

        Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_DEFAULT);
    }
}
