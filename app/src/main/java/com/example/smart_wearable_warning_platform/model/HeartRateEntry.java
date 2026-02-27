package com.example.smart_wearable_warning_platform.model;

public class HeartRateEntry {
    private String timestamp;
    private int bpm;
    private int stepFrequency; // 新增字段，用于存储步频数据

    // 原有构造函数保持兼容性（老数据不会包含步频）
    public HeartRateEntry(String timestamp, int bpm) {
        this(timestamp, bpm, 0);
    }

    // 新增构造函数，接收步频
    public HeartRateEntry(String timestamp, int bpm, int stepFrequency) {
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.stepFrequency = stepFrequency;
    }

    public String getTimestamp() { return timestamp; }
    public int getBpm() { return bpm; }
    public int getStepFrequency() { return stepFrequency; }
}
