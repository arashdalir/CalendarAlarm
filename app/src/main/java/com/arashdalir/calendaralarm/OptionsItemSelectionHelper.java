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

import java.util.Calendar;

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
        final AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);
        Alarms alarms = adapter.getAlarms();

        Calendar now = Calendar.getInstance();
        Calendar reminder = (Calendar) now.clone();
        reminder.set(Calendar.MILLISECOND, 0);
        reminder.set(Calendar.SECOND, 0);
        Alarms.Alarm alarm = alarms.find(String.format("fake-%d", reminder.getTimeInMillis()));
        reminder.add(Calendar.MINUTE, 2);
        Calendar event = (Calendar) reminder.clone();
        event.add(Calendar.MINUTE, 3);

        alarm.set(-1, "Fake Alarm", reminder, event, StorageHelper.getRingtone(context), StorageHelper.getVibrate(context));
        alarms.sort();

        Activity a = (Activity)context;
        a.runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }


    private static void resetStoredEvents(final Context context) {
        final Activity a = (Activity) context;
        new AlertDialog.Builder(context)
                .setTitle(R.string.menu_reset_title)
                .setMessage(R.string.menu_reset_title_description)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Notifier.showToast(context, context.getString(R.string.menu_reset_initiated), Toast.LENGTH_SHORT);
                                final AlarmListAdapter adapter = ((CalendarApplication) context.getApplicationContext()).getAdapter(context);
                                adapter.getAlarms().clear();
                                Notifier.showToast(context, context.getString(R.string.menu_reset_finished), Toast.LENGTH_LONG);

                                a.runOnUiThread(new Runnable() {
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });

                                Intent i = new Intent();
                                i.setAction(ServiceHelper.ACTION_READ_REMINDERS);

                                AlarmManagerService.enqueueWork(context, i);
                            }
                        })
                .setNegativeButton(android.R.string.no, null).show();
    }
}
