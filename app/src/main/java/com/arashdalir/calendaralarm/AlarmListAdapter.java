package com.arashdalir.calendaralarm;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.constraint.Constraints;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
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

    Alarms.Alarm getItem(int position, boolean skipDeleted) {
        return alarms.getByPosition(position, skipDeleted);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_alarm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Alarms.Alarm alarm = alarms.getByPosition(position, true);

        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);

        String alarmTime;

        if (!alarm.isAllDay())
        {
            alarmTime = context.getString(R.string.activity_alarm_list_alarm_time_format, tf.format(alarm.getEventTime().getTime()));
        }
        else
        {
            alarmTime = context.getString(R.string.activity_alarm_list_alarm_all_day);
        }

        String reminderTime = context.getString(
                R.string.activity_alarm_list_alarm_reminder_time,
                context.getString(
                        R.string.activity_alarm_list_alarm_date_time_format,
                        df.format(alarm.getReminderTime().getTime()),
                        context.getString(R.string.activity_alarm_list_alarm_time_format, tf.format(alarm.getReminderTime().getTime()))
                )
        );
        String eventTime = context.getString(
                R.string.activity_alarm_list_alarm_event_time,
                context.getString(
                        R.string.activity_alarm_list_alarm_date_time_format,
                        df.format(alarm.getEventTime().getTime()),
                        alarmTime
                )
        );

        TextView title = (TextView) holder.alarmView.findViewById(R.id.alarm_title);
        Switch on = (Switch) holder.alarmView.findViewById(R.id.alarm_active);
        TextView vEventTime = (TextView) holder.alarmView.findViewById(R.id.alarm_event_time);
        TextView vReminderTime = (TextView) holder.alarmView.findViewById(R.id.alarm_reminder_time);
        final TextView calendarName = (TextView) holder.alarmView.findViewById(R.id.alarm_calendar);
        ConstraintLayout alarmItem = (ConstraintLayout) holder.alarmView.findViewById(R.id.alarm_list_cl_alarm_item);
        ConstraintLayout alarmDetails = (ConstraintLayout) holder.alarmView.findViewById(R.id.alarm_details);

        View lastItem = (View) alarmDetails;

        if (alarm.getCalendarId() != Alarms.FAKE_CALENDAR_ID)
        {
            Button viewInCalendar = new Button(context);
            viewInCalendar.setId(View.generateViewId());
            viewInCalendar.setText(R.string.activity_alarm_list_view_in_calendar);

            lastItem = (View) viewInCalendar;

            Constraints.LayoutParams lp = new Constraints.LayoutParams(Constraints.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alarmItem.addView(viewInCalendar, lp);
            ConstraintSet cs = new ConstraintSet();
            cs.clone(alarmItem);
            cs.connect(viewInCalendar.getId(), ConstraintSet.TOP, alarmDetails.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(alarmItem);

            viewInCalendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, alarm.getEventId());
                    Intent calendarEvent = new Intent(Intent.ACTION_VIEW, eventUri);
                    calendarEvent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(calendarEvent);
                }
            });
        }

        calendarName.setText(context.getString(R.string.activity_alarm_list_alarm_calendar, alarm.getCalendarName(context)));
        calendarName.setTextColor(alarm.getCalendarColor(context));
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

        if (alarm.hasState(Alarms.Alarm.STATE_ALARMING)) {
            status.add(context.getString(R.string.activity_alarm_list_alarm_alarming));
        }
        else if (alarm.hasState(Alarms.Alarm.STATE_SNOOZED)) {
            status.add(context.getString(R.string.activity_alarm_list_alarm_snoozed));
        }
        else if (alarm.hasState(Alarms.Alarm.STATE_REMINDER_TIME_PASSED)) {
            status.add(context.getString(R.string.activity_alarm_list_alarm_expired));
            on.setEnabled(false);
        }

        if (status.size() > 0) {
            TextView statusLine = new TextView(context);
            statusLine.setId(View.generateViewId());

            Constraints.LayoutParams lp = new Constraints.LayoutParams(Constraints.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alarmItem.addView(statusLine, lp);
            ConstraintSet cs = new ConstraintSet();
            cs.clone(alarmItem);
            cs.connect(statusLine.getId(), ConstraintSet.TOP, lastItem.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(alarmItem);

            statusLine.setText(TextUtils.join("\n", status));
            statusLine.setVisibility(View.VISIBLE);
        }
    }

    boolean removeItem(int position) {
        Alarms.Alarm alarm = this.getItem(position, false);
        boolean removed = false;
        if (alarm != null) {
            removed = alarms.delete(position, alarm);
        }

        return removed;
    }

    void restoreItem(Alarms.Alarm alarm) {
        alarms.find(alarm.getReminderId()).set(alarm);
    }

    @Override
    public int getItemCount() {
        return alarms.count();
    }

    Alarms getAlarms(){
        return alarms;
    }

    void storeData(Context context) {
        StorageHelper.storeAlarms(context, alarms.asJsonArray());
    }

    boolean checkTimes() {
        boolean status = alarms.checkTimes();
        storeData(context);

        return status;
    }

    Alarms.Alarm getAlarm(String reminderId) {
        return this.getAlarm(reminderId, true);
    }

    Alarms.Alarm getAlarm(String reminderId, boolean createIfNotExists) {
        return this.getAlarms().find(reminderId, createIfNotExists);
    }

    void reset(boolean clearFakes){
        alarms.clear(clearFakes);
    }
}
