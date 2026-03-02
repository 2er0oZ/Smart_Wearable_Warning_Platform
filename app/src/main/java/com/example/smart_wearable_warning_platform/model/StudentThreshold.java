package com.example.smart_wearable_warning_platform.model;

public class StudentThreshold {
    private int minHr;
    private int maxHr;
    private int minStep;
    private int maxStep;
    // 时间预警区间，用于睡眠时间管理
    private String sleepStartTime = "21:00"; // 默认睡眠开始时间
    private String sleepEndTime = "07:00"; // 默认睡眠结束时间
    private boolean enableSleepTimeAlert = true; // 是否启用睡眠时间预警
    public StudentThreshold() {
        // default constructor required for Gson
        // gson 会填充现有字段，若旧 JSON 中没有步频字段则保持默认 0，此时后续代码
        // 使用 getMinStep()/getMaxStep() 时，若值为0会被认为默认范围 0-5
        this.minStep = 0;
        this.maxStep = 5;
        // 默认睡眠时间预警区间
        this.sleepStartTime = "21:00";
        this.sleepEndTime = "07:00";
        this.enableSleepTimeAlert = true;
    }

    // constructor with only心率阈值，步频使用默认 0-5
    public StudentThreshold(int minHr, int maxHr) {
        this.minHr = minHr;
        this.maxHr = maxHr;
        this.minStep = 0;
        this.maxStep = 5;
        // 默认睡眠时间预警区间
        this.sleepStartTime = "21:00";
        this.sleepEndTime = "07:00";
        this.enableSleepTimeAlert = true;
    }

    public StudentThreshold(int minHr, int maxHr, int minStep, int maxStep) {
        this.minHr = minHr;
        this.maxHr = maxHr;
        this.minStep = minStep;
        this.maxStep = maxStep;
        // 默认睡眠时间预警区间
        this.sleepStartTime = "21:00";
        this.sleepEndTime = "07:00";
        this.enableSleepTimeAlert = true;
    }

    public int getMinHr() { return minHr; }
    public void setMinHr(int minHr) { this.minHr = minHr; }

    public int getMaxHr() { return maxHr; }
    public void setMaxHr(int maxHr) { this.maxHr = maxHr; }

    public int getMinStep() { return minStep; }
    public void setMinStep(int minStep) { this.minStep = minStep; }

    public int getMaxStep() { return maxStep; }
    public void setMaxStep(int maxStep) { this.maxStep = maxStep; }
    
    // 睡眠时间预警区间相关方法
    public String getSleepStartTime() { return sleepStartTime; }
    public void setSleepStartTime(String sleepStartTime) { this.sleepStartTime = sleepStartTime; }
    
    public String getSleepEndTime() { return sleepEndTime; }
    public void setSleepEndTime(String sleepEndTime) { this.sleepEndTime = sleepEndTime; }
    
    public boolean isEnableSleepTimeAlert() { return enableSleepTimeAlert; }
    public void setEnableSleepTimeAlert(boolean enableSleepTimeAlert) { this.enableSleepTimeAlert = enableSleepTimeAlert; }
}