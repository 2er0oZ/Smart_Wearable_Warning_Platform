package com.example.smart_wearable_warning_platform.model;

public class HealthAlert {
    private String timestamp;
    private String message;
    private String studentName;
    private int bpm;

    // --- 新增：控制是否展开的字段 ---
    private boolean isExpanded = false;
    // ---

    public HealthAlert() {}

    public HealthAlert(String timestamp, String message, String studentName) {
        this.timestamp = timestamp;
        this.message = message;
        this.studentName = studentName;
        // bpm 在创建时没有传入，默认为0，需要在建立后设置。
    }

    // ... Getters 和 Setters (保持之前的) ...

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }

    // --- 新增：展开状态的控制 ---
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}
