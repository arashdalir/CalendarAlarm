package com.arashdalir.calendaralarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class CalendarSelectionActivity extends AppCompatActivity {
    LinearLayout layout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this.getApplicationContext();

        layout = findViewById(R.id.CalendarList);

        if (AlarmCalenderHelper.checkPermissions(context)) {

            Cursor cursor = AlarmCalenderHelper.readCalendars(context);

            if (cursor != null) {
                cursor.moveToFirst();
                do {

                    int count = cursor.getCount();
                    String entry = null;
                    String entries[] = new String[count];
                    String entryValues[] = new String[count];
                    int bgColor = 0,
                            fgColor = Color.parseColor("#ffffff");

                    for (int i = 0; i < count; i++) {
                        cursor.moveToPosition(i);
                        entry = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
                        bgColor = cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR));

                        entries[i] = entry;

                        CheckBox cb = new CheckBox(context);
                        cb.setText(entry);
                        cb.setBackgroundColor(bgColor);
                        cb.setTextColor(fgColor);

                        layout.addView(cb);

                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
            setContentView(R.layout.activity_calendar_selection);
        }
        else
        {
            Intent intent = new Intent(context, PermissionCheckActivity.class);
            startActivity(intent);
        }
    }

}
