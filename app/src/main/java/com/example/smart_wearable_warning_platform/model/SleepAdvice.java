package com.example.smart_wearable_warning_platform.model;

/**
 * 睡眠建议模型
 */
public class SleepAdvice {
    private int id;                 // 建议ID
    private String title;           // 建议标题
    private String content;         // 建议内容
    private String category;         // 建议分类（睡眠时间、睡眠质量、生活习惯等）
    private int priority;           // 优先级（1-5，5为最高）
    
    public SleepAdvice() {
    }
    
    public SleepAdvice(int id, String title, String content, String category, int priority) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.priority = priority;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
}