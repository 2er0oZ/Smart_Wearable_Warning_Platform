package com.example.smart_wearable_warning_platform.model;

public class HeartRateEntry {
    private String timestamp;
    private int bpm;

    public HeartRateEntry(String timestamp, int bpm) {
        this.timestamp = timestamp;
        this.bpm = bpm;
    }

    public String getTimestamp() { return timestamp; }
    public int getBpm() { return bpm; }
}
