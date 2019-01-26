package com.arashdalir.calendaralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.Inet4Address;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(this.getClass().toString(), "Service started at boot!");

            AlarmManagerService.enqueueWork(context, intent);
        } else {
            if (intent.getAction().contains(SnoozeActivity.ACTION_SNOOZE)) {
                Log.i(this.getClass().toString(), "Timer for an alarm reached!");
                ServiceHelper.doAlarm(context.getApplicationContext(), intent);
            }
            if (intent.getAction().contains(ServiceHelper.ACTION_DO_JOB)) {
                Log.i(this.getClass().toString(), "Checking things again, starting to work!");

                AlarmManagerService.enqueueWork(context, intent);
            }
            if (intent.getAction().equals(Intent.ACTION_PROVIDER_CHANGED)) {
                Log.i(this.getClass().toString(), "Calendar changes received");

                AlarmManagerService.enqueueWork(context, intent);
                NotificationCompat.Builder builder = Notifier.getBuilder(context)
                        .setContentTitle(context.getString(R.string.notification_message_looking_for_events))
                        .setContentText(context.getString(R.string.notification_message_looking_for_events_description, context.getString(R.string.app_name)));

                Notifier.notify(context, builder, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_DEFAULT);
            }
        }
    }
}
