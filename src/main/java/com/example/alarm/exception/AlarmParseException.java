package com.example.alarm.exception;

public class AlarmParseException extends RuntimeException {
    private final String alarmText;

    public AlarmParseException(String alarmText, String message) {
        super(message);
        this.alarmText = alarmText;
    }

    public String getAlarmText() { return alarmText; }
}
