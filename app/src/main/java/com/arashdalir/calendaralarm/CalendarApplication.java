package com.arashdalir.calendaralarm;

import android.app.Application;

public class CalendarApplication extends Application {
    @Override
    public void onCreate() {
        AlarmManagerService.enqueueWork(this);
        super.onCreate();
    }
}
