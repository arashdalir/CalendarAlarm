package com.arashdalir.calendaralarm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */

public class AlarmManagerService extends JobIntentService {
    private ServiceHelper serviceHelper;

    public AlarmManagerService() {
        super();
        serviceHelper = new ServiceHelper(this);
    }

    public static void enqueueWork(Context context) {
        enqueueWork(context, ServiceHelper.ACTION_START_SERVICE);
        Log.i(AlarmManagerService.class.toString(), "Service started called to action.");
    }

    public static void enqueueWork(Context context, String action) {
        Intent serviceIntent = new Intent(context, AlarmManagerService.class).setAction(action);
        enqueueWork(context, serviceIntent);
    }

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, AlarmManagerService.class, 1, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        serviceHelper.onHandleWork(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        serviceHelper.onHandleWork(intent);

        return START_STICKY;
    }
}
