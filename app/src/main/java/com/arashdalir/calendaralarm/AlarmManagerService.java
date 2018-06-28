package com.arashdalir.calendaralarm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.AndroidException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

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
}
