package com.arashdalir.calendaralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(this.getClass().toString(), "Service started at boot!");

            AlarmManagerService.enqueueWork(context, intent);
        }
    }
}
