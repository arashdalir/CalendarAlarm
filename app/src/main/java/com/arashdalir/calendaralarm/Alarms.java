package com.arashdalir.calendaralarm;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Alarms {
    private static ArrayList<Alarm> alarms;

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

    public static class Alarm {
        public static final int STATE_NEW = 0;
        public static final int STATE_STORED = 1;
        public static final int STATE_INACTIVE = 1 << 1;
        public static final int STATE_REMINDER_TIME_PASSED = 1 << 2;
        public static final int STATE_SNOOZED = 1 << 3;
        public static final int STATE_DELETED = 1 << 4;
        public static final int STATE_ALARMING = 1 << 8;

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

        private Vibrator vibrator = null;
        private MediaPlayer player = null;

        Alarm(String id) {
            this.reminderId = id;
            reset();
        }

        Alarm(JSONObject alm) {
            try {
                this.reminderId = alm.getString(ALARM_ID);
                ;
                this.set(alm);
            } catch (Exception e) {

            }
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
                Log.e(this.getClass().toString(), e.getMessage());
            }

            return set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, state, version);
        }

        Alarm set(int calendarId, String title, Calendar reminderTime, Calendar eventTime, String ringtone, boolean vibrate) {
            return set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, STATE_NEW, ALARMS_STORAGE_VERSION);
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

        boolean eventTimePassed() {
            Calendar now = Calendar.getInstance();
            return (eventTime.getTimeInMillis() < now.getTimeInMillis() && !hasState(Alarm.STATE_SNOOZED));
        }

        boolean reminderTimePassed() {

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
            if (state == Alarm.STATE_NEW) {
                this.state = Alarm.STATE_NEW;
            } else {
                this.state |= state;
            }
        }

        public void resetState(int state) {
            if (state == Alarm.STATE_NEW) {
                this.state = Alarm.STATE_STORED;
            } else if (hasState(state)) {
                this.state ^= state;
            }
        }

        public boolean hasState(int state) {
            boolean status = false;
            if (state == Alarm.STATE_NEW && this.getState() == Alarm.STATE_NEW) {
                status = true;
            } else if ((this.getState() & state) != Alarm.STATE_NEW) {
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
            } catch (Exception e) {

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

        public boolean checkTimes() {
            boolean createTimer = false;
            if (!hasState(Alarms.Alarm.STATE_INACTIVE)) {
                if (eventTimePassed()) {
                    markForDeletion();
                } else if (reminderTimePassed()) {
                    setState(Alarms.Alarm.STATE_REMINDER_TIME_PASSED);
                } else if (!isSnoozing()) {
                    if (!eventTimePassed()) {
                        createTimer = true;
                    }
                } else {
                    markForDeletion();
                }
            }

            return createTimer;
        }

        public boolean isDeleted() {
            return isMarkedForDeletion() || hasState(Alarm.STATE_DELETED);
        }

        public void vibrate(Context context) {
            if (isVibrate()) {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

                if (vibrator.hasVibrator()) {
                    long[] pattern = new long[]{0, 500, 0, 0, 500};
                    vibrator.vibrate(pattern, 0);
                }
            }
        }

        public void cancelVibrate() {

            if (vibrator != null) {
                vibrator.cancel();
            }
        }

        public void playRingtone(Context context) {
            String ringtone = StorageHelper.getRingtone(context, true);
            Uri ringtoneUri = Uri.parse(ringtone);

            if (!Uri.EMPTY.equals(ringtoneUri)) {
                player = MediaPlayer.create(context, ringtoneUri);
                player.setLooping(true);
                player.start();
            }
        }

        public void stopRingtone() {
            if (this.player != null) {
                if (this.player.isPlaying()) {
                    this.player.stop();
                }
            }
        }

        public int getCalendarColor(Context context) {
            CalendarHelper.CalendarInfo calendar = CalendarHelper.getCalendarInfo(context, getCalendarId());

            return calendar.getColor();
        }

        public String getCalendarName(Context context) {
            CalendarHelper.CalendarInfo calendar = CalendarHelper.getCalendarInfo(context, getCalendarId());

            return calendar.getDisplayName();
        }

        public void stopAlarm() {
            resetState(STATE_ALARMING);
            cancelVibrate();
            stopRingtone();
        }

        public void startAlarm(Context context) {
            setState(STATE_ALARMING);
            vibrate(context);
            playRingtone(context);
        }

        public static String getIdFromJSON(JSONObject obj) {
            String id = null;
            try {
                id = obj.getString(ALARM_ID);
            } catch (Exception e) {

            }

            return id;
        }
    }

    public class AlarmsStati {
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

    Alarms() {
        alarms = new ArrayList<>();
    }

    public AlarmsStati filterCalendars(List<Integer> calendars) {
        AlarmsStati status = new AlarmsStati();

        boolean expired = false, passed = false;
        for (int i = 0; i < count(); i++) {
            Alarm alarm = this.getByPosition(i);

            if (alarm == null) {
                continue;
            }

            expired = alarm.reminderTimePassed();
            passed = alarm.eventTimePassed();

            if (!calendars.contains(alarm.getCalendarId()) || passed) {
                alarm.markForDeletion();
                status.setDeleted();
            } else if (alarm.hasState(Alarm.STATE_NEW)) {
                status.setAdded();
                alarm.resetState(Alarm.STATE_NEW);
            } else if (expired) {
                alarm.setState(Alarm.STATE_REMINDER_TIME_PASSED);
                status.setExpired();
            }
        }

        return status;
    }

    public boolean checkTimes() {
        boolean status = false;
        if (!alarms.isEmpty()) {
            for (Alarm alarm : alarms) {
                alarm.checkTimes();

                if (alarm.isDeleted()) {
                    status = true;
                }
            }
        }
        return status;
    }

    public Alarm find(String reminderId) {
        return find(reminderId, true);
    }

    public Alarm find(String reminderId, boolean createIfNotExists) {

        Alarm alarm = null;
        int position = getPosition(reminderId);

        if (position != -1) {
            alarm = getByPosition(position);
        } else {
            if (createIfNotExists)
            {
                alarm = new Alarm(reminderId);
                alarms.add(alarm);
            }
        }
        return alarm;
    }

    public Alarm getByPosition(int position) {
        return getByPosition(position, false);
    }

    public Alarm getByPosition(int position, boolean skipDeleted) {
        Alarm alarm = null;
        if (!alarms.isEmpty()) {
            for (int i = 0; i < alarms.size(); i++) {
                if (!skipDeleted || alarms.get(i).isDeleted()) {
                    continue;
                }
                if (position-- == 0)
                {
                    alarm = alarms.get(i);
                    break;
                }
            }
        }

        return alarm;
    }

    private int getPosition(String reminderId) {
        for (int i = 0; i < alarms.size(); i++) {
            Alarm alarm  = alarms.get(i);
            if (!alarm.isDeleted()) {
                if (alarm.reminderId.equals(reminderId)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public boolean alarmExists(String reminderId) {
        if (getPosition(reminderId) != -1) {
            return true;
        } else {
            return false;
        }
    }

    boolean delete(int position, Alarm deleted) {
        Alarm alarm = this.getByPosition(position);
        if (alarm.reminderId.equals(deleted.reminderId)) {
            alarm.markForDeletion();
            return true;
        } else {
            return false;
        }
    }

    boolean set(JSONArray alarms) {
        try {

            boolean converted = false;

            if (alarms != null) {
                for (int i = 0; i < alarms.length(); i++) {
                    JSONObject alm = (JSONObject) alarms.get(i);
                    String alarmId = alm.getString(ALARM_ID);

                    Alarm alarm = this.find(alarmId);
                    alarm.set(alm);
                    if (alarm.hasState(Alarm.STATE_NEW)) {
                        alarm.setState(Alarm.STATE_STORED);
                    }
                    converted = alarm.convertVersion();
                }
            }

            sort();

            return converted;
        } catch (Exception e) {
            return false;
        }
    }

    JSONArray asJsonArray() {
        JSONArray almJSON = new JSONArray();

        if (alarms.size() > 0) {
            for (int i = 0; i < alarms.size(); i++) {
                Alarm alarm = alarms.get(i);
                if (!alarm.isMarkedForDeletion()) {
                    try {
                        JSONObject alm = alarm.toJSON();
                        almJSON.put(alm);
                    } catch (Exception e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }
            }
        }

        return almJSON;
    }

    void sort() {
        Collections.sort(
                alarms,
                new Comparator<Alarm>() {
                    @Override
                    public int compare(Alarm o1, Alarm o2) {
                        return o1.getReminderTime().compareTo(o2.getReminderTime());
                    }
                }
        );
    }

    void clear() {
        alarms.clear();
    }

    public int count() {
        int count = 0;

        for (Alarm alarm : alarms) {

            alarm.checkTimes();

            if (!alarm.isDeleted()) {
                count++;
            }
        }

        return count;
    }
}
