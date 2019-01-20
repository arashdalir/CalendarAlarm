package com.arashdalir.calendaralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(this.getClass().toString(), "Service started at boot!");

            AlarmManagerService.enqueueWork(context, intent);
        }
        else
        {
            if (intent.getAction().contains(SnoozeActivity.ACTION_SNOOZE))
            {
                Log.i(this.getClass().toString(), "Timer for an alarm reached!");
                ServiceHelper.doAlarm(context.getApplicationContext(), intent);
            }
        }
    }
}
