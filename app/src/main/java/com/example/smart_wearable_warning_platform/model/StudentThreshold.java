package com.example.smart_wearable_warning_platform.model;

public class StudentThreshold {
    private int minHr;
    private int maxHr;
    private int minStep;
    private int maxStep;

    public StudentThreshold() {
        // default constructor required for Gson
        // gson 会填充现有字段，若旧 JSON 中没有步频字段则保持默认 0，此时后续代码
        // 使用 getMinStep()/getMaxStep() 时，若值为0会被认为默认范围 0-5
        this.minStep = 0;
        this.maxStep = 5;
    }

    // constructor with only心率阈值，步频使用默认 0-5
    public StudentThreshold(int minHr, int maxHr) {
        this.minHr = minHr;
        this.maxHr = maxHr;
        this.minStep = 0;
        this.maxStep = 5;
    }

    public StudentThreshold(int minHr, int maxHr, int minStep, int maxStep) {
        this.minHr = minHr;
        this.maxHr = maxHr;
        this.minStep = minStep;
        this.maxStep = maxStep;
    }

    public int getMinHr() { return minHr; }
    public void setMinHr(int minHr) { this.minHr = minHr; }

    public int getMaxHr() { return maxHr; }
    public void setMaxHr(int maxHr) { this.maxHr = maxHr; }

    public int getMinStep() { return minStep; }
    public void setMinStep(int minStep) { this.minStep = minStep; }

    public int getMaxStep() { return maxStep; }
    public void setMaxStep(int maxStep) { this.maxStep = maxStep; }
}