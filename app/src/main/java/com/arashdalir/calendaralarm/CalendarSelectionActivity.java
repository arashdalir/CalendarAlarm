package com.arashdalir.calendaralarm;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.provider.CalendarContract;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class CalendarSelectionActivity extends AppCompatActivity {
    protected final List<CheckBox> checkBoxes = new ArrayList<>();
    protected final HashMap<String, Integer> calendars = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();
        setTitle(R.string.title_activity_calendar_selection);
        setContentView(R.layout.activity_calendar_selection);

        LinearLayout layout = findViewById(R.id.CalendarList);
        List<Integer> currentCalendars = new ArrayList<>();

        for (Integer i: StorageHelper.getCalendars(this))
        {
            currentCalendars.add(i);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, (int) getResources().getDimension(R.dimen.spacing), 0, 0);

        if (PermissionsHelper.checkPermissions(context)) {
            Cursor cursor = CalendarHelper.readCalendars(context);

            if (cursor.moveToFirst()){
                do {
                    int count = cursor.getCount();

                    for (int i = 0; i < count; i++) {
                        cursor.moveToPosition(i);
                        String entry = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
                        int bgColor = cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR));
                        int calendarId = cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID));
                        calendars.put(entry, calendarId);

                        CheckBox cb = new CheckBox(context);

                        if (currentCalendars.contains(calendarId))
                        {
                            cb.setChecked(true);
                        }
                        cb.setText(entry);
                        cb.setLayoutParams(params);
                        layout.addView(cb);
                        cb.setTextColor(bgColor);
                        setCheckBoxColor(cb, bgColor, bgColor);
                        checkBoxes.add(cb);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            Intent intent = new Intent(context, PermissionCheckActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    /**
     * @see "https://stackoverflow.com/questions/28800451/change-checkbox-coloraccent-in-runtime-programmatically/44250259#44250259"
     * @param checkBox
     * @param checkedColor
     * @param uncheckedColor
     */
    public void setCheckBoxColor(CheckBox checkBox, int checkedColor, int uncheckedColor) {
        int states[][] = {{android.R.attr.state_checked}, {}};
        int colors[] = {checkedColor, uncheckedColor};
        CompoundButtonCompat.setButtonTintList(checkBox, new
                ColorStateList(states, colors));
    }

    public void saveCalendars(View view) {
        List<Integer> selectedCalendars = new ArrayList<>();

        if (checkBoxes.size() > 0)
        {
            for(CheckBox cb: checkBoxes)
            {
                if(cb.isChecked())
                {
                    int calendarId = calendars.get((String) cb.getText());
                    if (calendarId > 0)
                    {
                        selectedCalendars.add(calendarId);
                    }
                }
            }
        }

        Context context = view.getContext();

        StorageHelper.setCalendars(this, selectedCalendars);

        //Notifier.showToast(context, context.getString(R.string.notification_toast_calendar_selection_saved), Toast.LENGTH_LONG);

        AlarmManagerService.enqueueWork(context);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return OptionsItemSelectionHelper.handleOptionSelection(this, item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsItemSelectionHelper.createMenuItems(this, menu);
    }
}
