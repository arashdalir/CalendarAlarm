package com.arashdalir.calendaralarm;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;

import java.text.DateFormat;
import java.util.ArrayList;

public class AlarmListAdapter
        extends RecyclerView.Adapter<com.arashdalir.calendaralarm.AlarmListAdapter.ViewHolder> {
    private static Alarms alarms = null;
    private Context context;

    public boolean checkTimes() {
        boolean status = alarms.checkTimes();
        StorageHelper.storeAlarms(context, alarms.asJsonArray());

        return status;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View alarmView;

        ViewHolder(View v) {
            super(v);
            alarmView = v;
        }
    }

    void setContext(Context ctx)
    {
        context = ctx;
    }

    AlarmListAdapter(JSONArray alms) {
        super();
        alarms = new Alarms();
        alarms.set(alms);
    }

    public boolean alarmExists(String reminderId) {
        return alarms.alarmExists(reminderId);
    }

    public Alarms.Alarm getItem(int position) {
        if (position < this.getItemCount()) {
            return alarms.getAlarm(position);
        } else {
            return null;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_alarm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Alarms.Alarm alarm = alarms.getAlarm(position);

        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
        String reminderTime = context.getString(R.string.activity_alarm_list_alarm_reminder_time, context.getString(R.string.activity_alarm_list_alarm_time_format, df.format(alarm.getReminderTime().getTime()), tf.format(alarm.getReminderTime().getTime())));
        String eventTime = context.getString(R.string.activity_alarm_list_alarm_event_time, context.getString(R.string.activity_alarm_list_alarm_time_format, df.format(alarm.getEventTime().getTime()), tf.format(alarm.getEventTime().getTime())));

        int cId = alarm.getCalendarId();

        CalendarHelper.CalendarInfo calendar = CalendarHelper.getCalendarInfo(context, cId);

        TextView title = holder.alarmView.findViewById(R.id.alarm_title);
        Switch on = holder.alarmView.findViewById(R.id.alarm_active);
        TextView vEventTime = holder.alarmView.findViewById(R.id.alarm_event_time);
        TextView vReminderTime = holder.alarmView.findViewById(R.id.alarm_reminder_time);
        TextView calendarName = holder.alarmView.findViewById(R.id.alarm_calendar);
        TextView statusLine = holder.alarmView.findViewById(R.id.alarm_status_line);

        calendarName.setText(context.getString(R.string.activity_alarm_list_alarm_calendar, calendar.getDisplayName()));
        calendarName.setTextColor(calendar.getColor());
        title.setText(alarm.getTitle());
        vEventTime.setText(eventTime);
        vReminderTime.setText(reminderTime);
        on.setShowText(false);

        if (!alarm.hasState(Alarms.Alarm.STATE_INACTIVE)) {
            on.setChecked(true);
        } else {
            on.setChecked(false);
        }

        on.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!buttonView.isEnabled()) {
                    return;
                }

                if (isChecked) {
                    alarm.resetState(Alarms.Alarm.STATE_INACTIVE);
                } else {
                    alarm.setState(Alarms.Alarm.STATE_INACTIVE);
                }

                buttonView.setChecked(isChecked);
            }
        });


        ArrayList<String> status = new ArrayList<>();

        if (alarm.hasState(Alarms.Alarm.STATE_REMINDER_TIME_PASSED)) {
            status.add(context.getString(R.string.activity_alarm_list_alarm_expired));
            on.setEnabled(false);
        }

        if (alarm.hasState(Alarms.Alarm.STATE_SNOOZED)) {
            status.add(context.getString(R.string.activity_alarm_list_alarm_snoozed));
        }

        if (status.size() > 0) {
            statusLine.setText(TextUtils.join("\n", status));
            statusLine.setVisibility(View.VISIBLE);
        }
    }

    void removeItem(int position) {
        Alarms.Alarm alarm = this.getItem(position);

        if (alarm != null) {
            if (alarms.delete(position, alarm)) {
                notifyDataSetChanged();
            }
        }
    }

    void restoreItem(Alarms.Alarm alarm) {
        alarms.getAlarm(alarm.getReminderId()).set(alarm);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return alarms.count();
    }

    Alarms getAlarms(){
        return alarms;
    }

    public void storeData(Context context) {
        StorageHelper.storeAlarms(context, alarms.asJsonArray());
    }
}
