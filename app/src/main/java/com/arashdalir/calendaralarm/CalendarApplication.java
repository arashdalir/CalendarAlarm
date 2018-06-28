package com.arashdalir.calendaralarm;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.v7.widget.RecyclerView;

import org.json.JSONArray;

public class CalendarApplication extends Application {

    public AlarmListAdapter adapter;

    @Override
    public void onCreate() {
        AlarmManagerService.enqueueWork(this);
        super.onCreate();
        JSONArray alarms = null;
        try {
            alarms = StorageHelper.getStoredAlarms(this);
        } catch (Exception e) {

        }

        adapter = new AlarmListAdapter(alarms);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                StorageHelper.storeAlarms(getApplicationContext(), adapter.getAlarms().asJsonArray());
                super.onChanged();
            }
        });

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                AlarmManagerService.enqueueWork(getApplicationContext(), ServiceHelper.ACTION_CHECK_REMINDER_ALARMS);
            }
        });

        /*
        getContentResolver().
                registerContentObserver(
                        CalendarContract.Reminders.CONTENT_URI,
                        true,
                        new CalendarReminderObserver(new Handler())
                );
        */
    }

    AlarmListAdapter getAdapter(Context context) {
        adapter.setContext(context);
        return adapter;
    }
}
