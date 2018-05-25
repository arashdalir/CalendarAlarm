package com.arashdalir.calendaralarm;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Alarms {
    private ArrayList<Alarm> alarms;
    private Context context;

    private static final String ALARM_EVENT_TIME = "eventTime";
    private static final String ALARM_VIBRATE = "vibrate";
    private static final String ALARM_RINGTONE = "ringtone";
    private static final String ALARM_REMINDER_TIME = "reminderTime";
    private static final String ALARM_TITLE = "title";
    private static final String ALARM_ID = "alarm_id";
    private static final String ALARM_CALENDAR_ID = "calendarId";
    private static final String ALARM_VERSION = "app_version";
    private static final String ALARM_STATE = "state";

    private static final String ALARMS_STORAGE_VERSION = "v1";

    public ArrayList<Alarm> getAlarms() {
        return alarms;
    }

    public class Alarm {
        public static final int STATE_NEW = 0;
        public static final int STATE_STORED = 1;
        public static final int STATE_INACTIVE = 1 << 1;
        public static final int STATE_REMINDER_TIME_PASSED = 1 << 2;
        public static final int STATE_SNOOZED = 1 << 3;

        private String reminderId = null;
        private String ringtone = null;
        private boolean vibrate = false;
        private Calendar reminderTime = null;
        private Calendar eventTime = null;
        private String title = null;
        private int calendarId = 0;
        private String version = null;
        private int state = STATE_NEW;
        private boolean markDeleted = false;

        Alarm() {
            reminderId = null;
            reset();
        }

        Alarm(String id) {
            this.reminderId = id;
            reset();
        }

        public int getCalendarId() {
            return calendarId;
        }

        public String getReminderId() {
            return reminderId;
        }

        public String getRingtone() {
            return ringtone;
        }

        public boolean isVibrate() {
            return vibrate;
        }

        public Calendar getReminderTime() {
            return reminderTime;
        }

        public Calendar getEventTime() {
            return eventTime;
        }

        public String getTitle() {
            return title;
        }

        public void setRingtone(String ringtone) {
            this.ringtone = ringtone;
        }

        public void setVibrate(boolean vibrate) {
            this.vibrate = vibrate;
        }

        public void setReminderTime(Calendar reminderTime) {
            this.reminderTime = reminderTime;
        }

        public void setEventTime(Calendar eventTime) {
            this.eventTime = eventTime;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setCalendarId(int calendarId) {
            this.calendarId = calendarId;
        }

        Alarm set(JSONObject alm) {
            Calendar reminderTime = null,
                    eventTime = null;
            String title = null,
                    ringtone = null;
            boolean vibrate = false;
            int calendarId = 0,
                state = STATE_STORED;
            String version = ALARMS_STORAGE_VERSION;

            try {
                calendarId = alm.getInt(ALARM_CALENDAR_ID);
                reminderTime = Calendar.getInstance();
                reminderTime.setTimeInMillis(alm.getLong(ALARM_REMINDER_TIME));
                eventTime = (Calendar) reminderTime.clone();
                eventTime.setTimeInMillis(alm.getLong(ALARM_EVENT_TIME));
                title = alm.getString(ALARM_TITLE);
                ringtone = alm.getString(ALARM_RINGTONE);
                vibrate = alm.getBoolean(ALARM_VIBRATE);
                version = alm.getString(ALARM_VERSION);
                state = alm.getInt(ALARM_STATE);
            } catch (Exception e) {
                Log.e(Alarms.class.toString(), e.getMessage());
            }

            return set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, state, version);
        }

        Alarm set(int calendarId, String title, Calendar reminderTime, Calendar eventTime, String ringtone, boolean vibrate) {
            return set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, STATE_NEW, ALARM_VERSION);
        }

        Alarm set(int calendarId, String title, Calendar reminderTime, Calendar eventTime, String ringtone, boolean vibrate, int state, String version) {
            setReminderTime(reminderTime);
            setEventTime(eventTime);
            setTitle(title);
            setRingtone(ringtone);
            setVibrate(vibrate);
            setCalendarId(calendarId);
            setVersion(version);
            setState(state);

            return this;
        }

        boolean eventTimePassed()
        {
            Calendar now = Calendar.getInstance();
            return (eventTime.getTimeInMillis() < now.getTimeInMillis() && !hasState(Alarm.STATE_SNOOZED));
        }

        boolean reminderTimePassed(){

            Calendar now = Calendar.getInstance();
            return reminderTime.getTimeInMillis() < now.getTimeInMillis();
        }

        Alarm reset() {
            set(0, null, null, null, null, false);

            return this;
        }

        public Alarm set(Alarm alarm) {
            this.set(alarm.getCalendarId(), alarm.getTitle(), alarm.getReminderTime(), alarm.getEventTime(), alarm.getRingtone(), alarm.isVibrate());

            return this;
        }

        public boolean convertVersion() {
            boolean converted = false;
            if (!this.getVersion().equals(ALARMS_STORAGE_VERSION)) {

                converted = true;
            }

            return converted;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            if (state == Alarm.STATE_NEW)
            {
                this.state = Alarm.STATE_NEW;
            }
            else
            {
                this.state &= state;
            }
        }

        public void resetState(int state)
        {
            if (state == Alarm.STATE_NEW)
            {
                this.state = Alarm.STATE_STORED;
            }
            else if (hasState(state))
            {
                this.state ^= state;
            }
        }

        public boolean hasState(int state)
        {
            boolean status = false;
            if (state == Alarm.STATE_NEW && this.getState() == Alarm.STATE_NEW)
            {
                status = true;
            }
            else if ((this.getState() & state) != Alarm.STATE_NEW)
            {
                status = true;
            }

            return status;
        }

        public boolean isSnoozing() {
            return hasState(Alarm.STATE_SNOOZED);
        }

        public JSONObject toJSON() {
            JSONObject alm = new JSONObject();
            try {
                alm
                        .put(ALARM_ID, this.reminderId)
                        .put(ALARM_TITLE, this.title)
                        .put(ALARM_EVENT_TIME, this.eventTime.getTimeInMillis())
                        .put(ALARM_REMINDER_TIME, this.reminderTime.getTimeInMillis())
                        .put(ALARM_RINGTONE, this.ringtone)
                        .put(ALARM_VIBRATE, this.vibrate)
                        .put(ALARM_CALENDAR_ID, this.calendarId)
                        .put(ALARM_VERSION, this.version)
                        .put(ALARM_STATE, this.state);
            }
            catch(Exception e)
            {

            }

            return alm;
        }

        public boolean isMarkedForDeletion() {
            return markDeleted;
        }

        public void markForDeletion() {
            markForDeletion(true);
        }

        public void markForDeletion(boolean markDeleted) {
            this.markDeleted = markDeleted;
        }
    }

    public class AlarmsStati{
        private int added = 0;
        private int expired = 0;
        private int deleted = 0;

        public int getAdded() {
            return added;
        }

        public void setAdded() {
            this.added++;
        }

        public int getExpired() {
            return expired;
        }

        public void setExpired() {
            this.expired++;
        }

        public int getDeleted() {
            return deleted;
        }

        public void setDeleted() {
            this.deleted++;
        }

        public int getTotal() {
            return getAdded() + getDeleted();
        }
    }

    Alarms(Context context) {
        this.context = context;
        alarms = new ArrayList<>();
    }

    Context getContext() {
        return context;
    }

    public AlarmsStati filterCalendars(List<Integer> calendars) {
        AlarmsStati status = new AlarmsStati();

        boolean expired = false, passed = false;
        int length = this.length();
        for (int i = 0; i < length; i++) {
            Alarm alarm = this.getAlarm(i);
            expired =  alarm.reminderTimePassed();
            passed = alarm.eventTimePassed();

            if (!calendars.contains(alarm.getCalendarId()) || passed) {
                alarm.markForDeletion();
                status.setDeleted();
            }
            else if(alarm.hasState(Alarm.STATE_NEW))
            {
                status.setAdded();
                alarm.resetState(Alarm.STATE_NEW);
            }
            else if (expired)
            {
                alarm.setState(Alarm.STATE_REMINDER_TIME_PASSED);
                status.setExpired();
            }
        }

        return status;
    }

    public Alarm getAlarm(String reminderId) {
        Alarm alarm;
        int position = getAlarmPosition(reminderId);

        if (position != -1)
        {
            alarm = getAlarm(position);
        }
        else {
            alarm = new Alarm(reminderId);
            alarms.add(alarm);
        }
        return alarm;
    }

    public int length() {
        return alarms.size();
    }

    public Alarm getAlarm(int position) {
        if (!alarms.isEmpty()) {
            return alarms.get(position);
        } else {
            return null;
        }
    }

    public int getAlarmPosition(Alarm alarm)
    {
        return getAlarmPosition(alarm.getReminderId());
    }

    private int getAlarmPosition(String reminderId) {
        Alarm alarm = null;

        for (int i = 0; i < alarms.size(); i++) {
            alarm = alarms.get(i);
            if (alarm.reminderId.equals(reminderId)) {
                return i;
            }
        }

        return -1;
    }

    public boolean alarmExists(String reminderId) {
        if (getAlarmPosition(reminderId) != -1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    boolean delete(int position, Alarm deleted) {
        Alarm alarm = this.getAlarm(position);
        if (alarm.reminderId.equals(deleted.reminderId)) {
            this.alarms.remove(position);
            this.storeAlarms();
            return true;
        } else {
            return false;
        }
    }

    boolean getStoredAlarms() {
        try {
            JSONArray alarms = StorageHelper.getStoredAlarms(context);

            boolean converted = false;

            if (alarms != null) {
                for (int i = 0; i < alarms.length(); i++) {
                    JSONObject alm = (JSONObject) alarms.get(i);
                    String alarmId = alm.getString(ALARM_ID);
                    Alarm alarm = new Alarm(alarmId).set(alm);
                    this.alarms.add(alarm);
                    converted = alarm.convertVersion();
                }
            }

            if (converted) {
                this.storeAlarms();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void storeAlarms() {
        JSONArray almJSON = new JSONArray();

        if (alarms.size() > 0) {
            for (int i = 0; i < alarms.size(); i++) {
                Alarm alarm = alarms.get(i);
                if (!alarm.isMarkedForDeletion())
                {
                    try {
                        JSONObject alm = alarm.toJSON();
                        almJSON.put(alm);
                    } catch (Exception e) {
                        Log.e(Alarms.class.toString(), e.getMessage());
                    }
                }
            }
        }
        StorageHelper.storeAlarms(context, almJSON);
    }

    //boolean modifyAlarm(String alarmId, )
}
