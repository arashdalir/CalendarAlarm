package com.arashdalir.calendaralarm;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import androidx.core.app.NotificationCompat;

public class CalendarReminderObserver extends ContentObserver{
    private Handler handler;
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public CalendarReminderObserver(Handler handler) {
        super(handler);
        this.handler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        /*
        AlarmManagerService.enqueueWork(context);
        Notifier.getBuilder(context)
                .setContentTitle(context.getString(R.string.notification_message_looking_for_events))
                .setContentText(context.getString(R.string.notification_message_looking_for_events_description, context.getString(R.string.app_name)));

        Notifier.notify(context, Notifier.NOTIFY_GENERAL, NotificationCompat.PRIORITY_DEFAULT);
        */

    }
}
