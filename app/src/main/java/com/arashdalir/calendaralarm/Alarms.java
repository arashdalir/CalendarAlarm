package com.arashdalir.calendaralarm;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

class Alarms {
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
    private static final String ALARM_EVENT_ID = "id";

    static final int FAKE_CALENDAR_ID = -1;


    private static final String ALARMS_STORAGE_VERSION = "v1.1";

    static class Alarm {
        static final int STATE_UNCHANGED = -1;
        static final int STATE_NEW = 0;
        static final int STATE_STORED = 1;
        static final int STATE_INACTIVE = 1 << 1;
        static final int STATE_REMINDER_TIME_PASSED = 1 << 2;
        static final int STATE_SNOOZED = 1 << 3;
        static final int STATE_DELETED = 1 << 4;
        static final int STATE_ALARMING = 1 << 8;

        private String reminderId;
        private String ringtone = null;
        private boolean vibrate = false;
        private Calendar reminderTime = null;
        private Calendar eventTime = null;
        private String title = null;
        private int calendarId = 0;
        private String version = null;
        private int state = STATE_NEW;
        private boolean markDeleted = false;
        private int eventId = 0;

        private Vibrator vibrator = null;
        private MediaPlayer player = null;
        private AudioManager audioManager;

        Alarm(String id) {
            this.reminderId = id;
        }

        int getCalendarId() {
            return calendarId;
        }

        String getReminderId() {
            return reminderId;
        }

        String getRingtone() {
            return ringtone;
        }

        boolean isVibrate() {
            return vibrate;
        }

        Calendar getReminderTime() {
            return reminderTime;
        }

        Calendar getEventTime() {
            return eventTime;
        }

        String getTitle() {
            return title;
        }

        void setRingtone(String ringtone) {
            this.ringtone = ringtone;
        }

        void setVibrate(boolean vibrate) {
            this.vibrate = vibrate;
        }

        void setReminderTime(Calendar reminderTime) {
            this.reminderTime = reminderTime;
        }

        void setEventTime(Calendar eventTime) {
            this.eventTime = eventTime;
        }

        void setTitle(String title) {
            this.title = title;
        }

        void setCalendarId(int calendarId) {
            this.calendarId = calendarId;
        }

        Alarm set(JSONObject alm) {
            Calendar reminderTime = null,
                    eventTime = null;
            String title = null,
                    ringtone = null;
            boolean vibrate = false;
            int calendarId = 0,
                    eventId = 0,
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
                eventId = alm.getInt(ALARM_EVENT_ID);
                state = alm.getInt(ALARM_STATE);
            } catch (Exception e) {
                Log.e(this.getClass().toString(), e.getMessage());
            }

            return set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, state, eventId, version);
        }

        void set(int calendarId, String title, Calendar reminderTime, Calendar eventTime, String ringtone, boolean vibrate, int eventId) {
            set(calendarId, title, reminderTime, eventTime, ringtone, vibrate, STATE_UNCHANGED, eventId, ALARMS_STORAGE_VERSION);
        }

        Alarm set(int calendarId, String title, Calendar reminderTime, Calendar eventTime, String ringtone, boolean vibrate, int state, int eventId, String version) {
            setReminderTime(reminderTime);
            setEventTime(eventTime);
            setTitle(title);
            setRingtone(ringtone);
            setVibrate(vibrate);
            setCalendarId(calendarId);
            setVersion(version);
            setState(state);
            setEventId(eventId);

            return this;
        }

        boolean eventTimePassed() {
            Calendar now = Calendar.getInstance();
            return (eventTime.before(now) && (!hasState(Alarm.STATE_SNOOZED) || reminderTimePassed()));
        }

        boolean reminderTimePassed() {
            Calendar now = Calendar.getInstance();
            return reminderTime.before(now);
        }

        void set(Alarm alarm) {
            this.set(alarm.getCalendarId(), alarm.getTitle(), alarm.getReminderTime(), alarm.getEventTime(), alarm.getRingtone(), alarm.isVibrate(), alarm.getEventId());
        }

        String getVersion() {
            return version;
        }

        void setVersion(String version) {
            this.version = version;
        }

        int getState() {
            return state;
        }

        void setState(int state) {
            if (state != STATE_UNCHANGED) {
                if (state == Alarm.STATE_NEW) {
                    this.state = Alarm.STATE_NEW;
                } else {
                    this.state |= state;
                }
            }
        }

        void resetState(int state) {
            if (state == Alarm.STATE_NEW) {
                this.state = Alarm.STATE_STORED;
            } else if (hasState(state)) {
                this.state ^= state;
            }
        }

        boolean hasState(int state) {
            boolean status = false;
            if (state == Alarm.STATE_NEW && this.getState() == Alarm.STATE_NEW) {
                status = true;
            } else if ((this.getState() & state) != Alarm.STATE_NEW) {
                status = true;
            }

            return status;
        }

        JSONObject toJSON() {
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
                        .put(ALARM_EVENT_ID, this.eventId)
                        .put(ALARM_VERSION, this.version)
                        .put(ALARM_STATE, this.state);
            } catch (Exception e) {
                Log.d(e.getClass().toString(), "Converting alarm toJSON failed");
            }

            return alm;
        }

        boolean isMarkedForDeletion() {
            return markDeleted;
        }

        void markForDeletion() {
            this.markDeleted = true;
        }

        boolean checkTimes() {
            boolean createTimer = false;
            if (!hasState(Alarms.Alarm.STATE_INACTIVE)) {
                if (eventTimePassed()) {
                    markForDeletion();
                } else if (reminderTimePassed()) {
                    setState(Alarms.Alarm.STATE_REMINDER_TIME_PASSED);
                } else {
                    createTimer = true;
                }
            }

            return createTimer;
        }

        boolean isDeleted() {
            return isMarkedForDeletion() || hasState(Alarm.STATE_DELETED);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        void vibrate(Context context) {
            if (vibrator == null) {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (isVibrate()) {
                if (vibrator.hasVibrator()) {
                    long[] pattern = new long[]{0, 500, 0, 0, 500};
                    vibrator.vibrate(pattern, 0,
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    );
                }
            }
        }

        void cancelVibrate() {

            if (vibrator != null) {
                vibrator.cancel();
            }
        }

        void playRingtone(Context context) {
            String ringtoneName = StorageHelper.getRingtone(context, true);
            Uri ringtoneUri = Uri.parse(ringtoneName);

            try {
                if (!Uri.EMPTY.equals(ringtoneUri)) {
                    if (audioManager == null) {
                        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    }

                    player = MediaPlayer.create(context, ringtoneUri);
                    player.stop();

                    int sMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    int sVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    //audioManager.setStreamVolume(AudioManager.STREAM_ALARM, sVolume, sVolume);

                    player.setLooping(true);
                    player.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setLegacyStreamType(AudioManager.STREAM_ALARM)
                                    //.setUsage(AudioAttributes.USAGE_ALARM)
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setFlags(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    );

                    player.prepare();
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AUDIOFOCUS_GAIN_TRANSIENT);
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, sMax, 0);
                    player.start();
                }
            } catch (Exception e) {

            }
        }

        void stopRingtone() {
            if (player != null) {
                if (player.isPlaying()) {
                    player.stop();
                    player.release();
                    player = null;
                }
            }

            if (audioManager != null) {
                audioManager.abandonAudioFocus(null);
            }
        }

        int getCalendarColor(Context context) {
            CalendarHelper.CalendarInfo calendar = CalendarHelper.getCalendarInfo(context, getCalendarId());

            return calendar.getColor();
        }

        String getCalendarName(Context context) {
            CalendarHelper.CalendarInfo calendar = CalendarHelper.getCalendarInfo(context, getCalendarId());

            return calendar.getDisplayName();
        }

        void stopAlarm() {
            resetState(STATE_ALARMING);
            cancelVibrate();
            stopRingtone();
        }

        void startAlarm(Context context) {
            setState(STATE_ALARMING);
            vibrate(context);
            playRingtone(context);
        }

        static String getIdFromJSON(JSONObject obj) {
            String id = null;
            try {
                id = obj.getString(ALARM_ID);
            } catch (Exception e) {

            }

            return id;
        }

        int getEventId() {
            return eventId;
        }

        void setEventId(int eventId) {
            this.eventId = eventId;
        }
    }

    class AlarmsStati {
        private int added = 0;
        private int expired = 0;
        private int deleted = 0;

        int getAdded() {
            return added;
        }

        void setAdded() {
            this.added++;
        }

        int getExpired() {
            return expired;
        }

        void setExpired() {
            this.expired++;
        }

        int getDeleted() {
            return deleted;
        }

        void setDeleted() {
            this.deleted++;
        }

        int getTotal() {
            return getAdded() + getDeleted();
        }
    }

    Alarms() {
        alarms = new ArrayList<>();
    }

    AlarmsStati filterCalendars(List<Integer> calendars) {
        AlarmsStati status = new AlarmsStati();

        boolean expired = false, passed = false;
        for (int i = 0; i < count(); i++) {
            Alarm alarm = this.getByPosition(i);

            if (alarm == null) {
                continue;
            }

            expired = alarm.reminderTimePassed();
            passed = alarm.eventTimePassed();

            int calendarId = alarm.getCalendarId();

            if (!(calendarId == Alarms.FAKE_CALENDAR_ID || calendars.contains(calendarId)) || passed) {
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

    boolean checkTimes() {
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

    Alarm find(String reminderId) {
        return find(reminderId, true);
    }

    Alarm find(String reminderId, boolean createIfNotExists) {

        Alarm alarm = null;
        int position = getPosition(reminderId);

        if (position != -1) {
            alarm = getByPosition(position);
        } else {
            if (createIfNotExists) {
                alarm = new Alarm(reminderId);
                alarms.add(alarm);
            }
        }
        return alarm;
    }

    private Alarm getByPosition(int position) {
        return getByPosition(position, false);
    }

    Alarm getByPosition(int position, boolean skipDeleted) {
        Alarm alarm = null;
        if (!alarms.isEmpty()) {
            for (int i = 0; i < alarms.size(); i++) {
                if (skipDeleted && alarms.get(i).isDeleted()) {
                    continue;
                }
                if (position-- == 0) {
                    alarm = alarms.get(i);
                    break;
                }
            }
        }

        return alarm;
    }

    private int getPosition(String reminderId) {
        for (int i = 0; i < alarms.size(); i++) {
            Alarm alarm = alarms.get(i);
            if (!alarm.isDeleted()) {
                if (alarm.reminderId.equals(reminderId)) {
                    return i;
                }
            }
        }

        return -1;
    }

    boolean delete(int position, Alarm deleted) {
        Alarm alarm = this.getByPosition(position);
        if (alarm.reminderId.equals(deleted.reminderId) && !alarm.hasState(Alarm.STATE_ALARMING)) {
            alarm.markForDeletion();
            return true;
        } else {
            return false;
        }
    }

    void set(JSONArray alarms) {
        try {
            if (alarms != null) {
                for (int i = 0; i < alarms.length(); i++) {
                    JSONObject alm = (JSONObject) alarms.get(i);
                    String alarmId = alm.getString(ALARM_ID);

                    Alarm alarm = this.find(alarmId);
                    alarm.set(alm);
                    if (alarm.hasState(Alarm.STATE_NEW)) {
                        alarm.setState(Alarm.STATE_STORED);
                    }
                }
            }

            sort();
        } catch (Exception e) {
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

    int count() {
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
