package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CalendarContract;
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
        setContentView(R.layout.activity_calendar_selection);
        layout = findViewById(R.id.CalendarList);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, (int) getResources().getDimension(R.dimen.spacing), 0, 0);

        if (PermissionsHelper.checkPermissions(context)) {

            Cursor cursor = CalenderHelper.readCalendars(context);

            if (cursor != null) {
                cursor.moveToFirst();
                do {

                    int count = cursor.getCount();
                    String entry = null;
                    int bgColor = 0;

                    for (int i = 0; i < count; i++) {
                        cursor.moveToPosition(i);
                        entry = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
                        bgColor = cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR));
                        CheckBox cb = new CheckBox(context);
                        cb.setText(entry);
                        cb.setTextColor(bgColor);
                        cb.setLayoutParams(params);

                        layout.addView(cb);

                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        else
        {
            Intent intent = new Intent(context, PermissionCheckActivity.class);
            startActivity(intent);
        }
    }

}
