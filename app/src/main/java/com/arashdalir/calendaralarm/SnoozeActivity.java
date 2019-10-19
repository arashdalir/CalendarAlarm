package com.arashdalir.calendaralarm;

import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class SnoozeActivity extends AppCompatActivity {

    public static final String ACTION_SNOOZE = "snooze";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);
        TextView title = findViewById(R.id.alarm_title);
        ConstraintLayout layout = findViewById(R.id.snooze_layout);

        AlarmListAdapter adapter = ((CalendarApplication) getApplication()).getAdapter(getApplicationContext());
        String action = getIntent().getAction();

        String reminderId = action.replace("snooze-", "");

        Alarms.Alarm alarm = adapter.getAlarms().find(reminderId);

        if (alarm != null)
        {
            CalendarHelper.CalendarInfo calendarInfo = CalendarHelper.getCalendarInfo(getApplicationContext(), alarm.getCalendarId());
            layout.setBackgroundColor(calendarInfo.getColor());

            title.setText(alarm.getTitle());
        }
    }

}
