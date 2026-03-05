package com.example.smart_wearable_warning_platform.model;

import java.util.Date;

/**
 * 睡眠数据模型
 */
public class SleepData {
    private Date sleepTime;     // 入睡时间
    private Date wakeTime;      // 起床时间
    private long duration;      // 睡眠时长（分钟）
    private int quality;        // 睡眠质量评分（0-100）
    private String date;        // 日期（yyyy-MM-dd）
    
    public SleepData() {
    }
    
    public SleepData(String date, Date sleepTime, Date wakeTime, long duration, int quality) {
        this.date = date;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.duration = duration;
        this.quality = quality;
    }
    
    public Date getSleepTime() {
        return sleepTime;
    }
    
    public void setSleepTime(Date sleepTime) {
        this.sleepTime = sleepTime;
    }
    
    public Date getWakeTime() {
        return wakeTime;
    }
    
    public void setWakeTime(Date wakeTime) {
        this.wakeTime = wakeTime;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public int getQuality() {
        return quality;
    }
    
    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    /**
     * 获取睡眠时长（小时）
     */
    public double getDurationInHours() {
        return duration / 60.0;
    }
    
    /**
     * 获取睡眠质量描述
     */
    public String getQualityDescription() {
        if (quality >= 80) {
            return "优秀";
        } else if (quality >= 60) {
            return "良好";
        } else if (quality >= 40) {
            return "一般";
        } else {
            return "较差";
        }
    }
}