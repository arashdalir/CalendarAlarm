package com.arashdalir.calendaralarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class OptionsItemSelectionHelper {
    public static boolean handleOptionSelection(Fragment fragment, MenuItem item) {
        return handleOptionSelection(fragment.getActivity(), item);
    }

    public static boolean handleOptionSelection(Context context, MenuItem item) {
        boolean status = true;
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                context.startActivity(new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;

            //case R.id.menu_reload:
                //ServiceHelper.readEventAlarms(context);
            //   break;

            case R.id.menu_settings:
                context.startActivity(new Intent(context, SettingsActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;

            case R.id.menu_list:
                context.startActivity(new Intent(context, AlarmListActivity.class).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT));
                break;

            case R.id.menu_create_fake_reminder:
                createFakeReminder(context);
                break;

            case R.id.menu_reset_list:
                resetStoredEvents(context);
                break;
            default:
                status = false;
                break;
        }


        return status;
    }
    public static boolean createMenuItems(Activity activity, Menu menu) {
        MenuInflater inflater = activity.getMenuInflater();
        return createMenuItems(inflater, menu);
    }

    public static boolean createMenuItems(MenuInflater inflater, Menu menu){
        inflater.inflate(R.menu.actions, menu);
        return true;

    }

    private static void createFakeReminder(final Context context) {
        AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);
        Alarms alarms = adapter.getAlarms();

        Calendar now = Calendar.getInstance();
        Alarms.Alarm alarm = alarms.getAlarm(String.format("fake-%d", now.getTimeInMillis()));
        Calendar reminder = (Calendar) now.clone();
        reminder.add(Calendar.MINUTE, 5);
        Calendar event = (Calendar) now.clone();
        event.add(Calendar.MINUTE, 10);

        alarm.set(-1, "Fake Alarm", reminder, event, StorageHelper.getRingtone(context), StorageHelper.getVibrate(context));
        alarms.sort();
        adapter.notifyItemInserted(alarms.getAlarmPosition(alarm));
    }


    private static void resetStoredEvents(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.menu_reset_title)
                .setMessage(R.string.menu_reset_title_description)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Notifier.showToast(context, context.getString(R.string.menu_reset_initiated), Toast.LENGTH_LONG);
                                AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);
                                adapter.getAlarms().clear();
                                adapter.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(android.R.string.no, null).show();
    }
}
