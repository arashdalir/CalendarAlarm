package com.arashdalir.calendaralarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, AlarmManagerService.class);
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.BOOT_COMPLETED"))
        {
            context.startService(startIntent);
        }
    }
}
