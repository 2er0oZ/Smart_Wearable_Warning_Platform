package com.example.smart_wearable_warning_platform.service;

import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.SleepAdvice;
import com.example.smart_wearable_warning_platform.model.SleepData;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 睡眠分析服务
 */
public class SleepAnalysisService {
    
    /**
     * 分析心率数据，生成睡眠数据
     * @param heartRateData 心率数据列表
     * @param threshold 学生阈值设置
     * @return 睡眠数据列表
     */
    public List<SleepData> analyzeSleepData(List<HeartRateEntry> heartRateData, StudentThreshold threshold) {
        List<SleepData> sleepDataList = new ArrayList<>();
        
        // 获取睡眠时间区间
        String sleepStartTime = threshold.getSleepStartTime();
        String sleepEndTime = threshold.getSleepEndTime();
        
        // 按日期分组数据
        Map<String, List<HeartRateEntry>> dataByDate = groupDataByDate(heartRateData);
        
        // 分析每天的睡眠情况
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        for (Map.Entry<String, List<HeartRateEntry>> entry : dataByDate.entrySet()) {
            String date = entry.getKey();
            List<HeartRateEntry> dayData = entry.getValue();
            
            if (dayData.isEmpty()) continue;
            
            // 找出睡眠时间区间内的心率最低点作为入睡时间
            Date sleepTime = findSleepTime(dayData, sleepStartTime, sleepEndTime, dateTimeFormat);
            
            // 找出第二天早晨的心率上升点作为起床时间
            Date wakeTime = findWakeTime(dayData, sleepStartTime, sleepEndTime, dateTimeFormat);
            
            if (sleepTime != null && wakeTime != null) {
                // 计算睡眠时长（分钟）
                long duration = (wakeTime.getTime() - sleepTime.getTime()) / (1000 * 60);
                
                // 计算睡眠质量评分
                int quality = calculateSleepQuality(dayData);
                
                // 创建睡眠数据
                SleepData sleepData = new SleepData(date, sleepTime, wakeTime, duration, quality);
                sleepDataList.add(sleepData);
            }
        }
        
        return sleepDataList;
    }
    
    /**
     * 按日期分组心率数据
     */
    private Map<String, List<HeartRateEntry>> groupDataByDate(List<HeartRateEntry> heartRateData) {
        Map<String, List<HeartRateEntry>> dataByDate = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (HeartRateEntry entry : heartRateData) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(entry.getTimestamp());
                if (date != null) {
                    String dateStr = dateFormat.format(date);
                    
                    if (!dataByDate.containsKey(dateStr)) {
                        dataByDate.put(dateStr, new ArrayList<>());
                    }
                    
                    dataByDate.get(dateStr).add(entry);
                }
            } catch (ParseException e) {
                // 跳过格式不正确的数据
            }
        }
        
        return dataByDate;
    }
    
    /**
     * 分析一天的睡眠数据
     */
    private SleepData analyzeDaySleepData(String date, List<HeartRateEntry> dayData, StudentThreshold threshold) {
        if (dayData == null || dayData.isEmpty()) {
            return null;
        }
        
        // 获取睡眠时间区间
        String sleepStartTime = threshold.getSleepStartTime();
        String sleepEndTime = threshold.getSleepEndTime();
        
        // 解析睡眠时间
        int sleepHour = Integer.parseInt(sleepStartTime.split(":")[0]);
        int wakeHour = Integer.parseInt(sleepEndTime.split(":")[0]);
        
        // 找出睡眠时间区间内的心率数据
        List<HeartRateEntry> sleepPeriodData = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH", Locale.getDefault());
        
        for (HeartRateEntry entry : dayData) {
            try {
                Date entryDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(entry.getTimestamp());
                if (entryDate != null) {
                    int hour = Integer.parseInt(timeFormat.format(entryDate));
                    
                    // 判断是否在睡眠时间区间内
                    if (sleepHour > wakeHour) {
                        // 跨天睡眠，如21:00-7:00
                        if (hour >= sleepHour || hour < wakeHour) {
                            sleepPeriodData.add(entry);
                        }
                    } else {
                        // 不跨天睡眠，如23:00-6:00
                        if (hour >= sleepHour && hour < wakeHour) {
                            sleepPeriodData.add(entry);
                        }
                    }
                }
            } catch (ParseException e) {
                // 跳过格式不正确的数据
            }
        }
        
        if (sleepPeriodData.isEmpty()) {
            return null;
        }
        
        // 计算睡眠数据
        SleepData sleepData = new SleepData();
        
        // 计算入睡时间和起床时间
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date sleepTime = findSleepTime(dayData, sleepStartTime, sleepEndTime, dateTimeFormat);
        Date wakeTime = findWakeTime(dayData, sleepStartTime, sleepEndTime, dateTimeFormat);
        
        // 根据睡眠时间的实际日期来确定睡眠记录的归属日期
        if (sleepTime != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String sleepDate = dateFormat.format(sleepTime);
            sleepData.setDate(sleepDate);
        } else {
            // 如果无法确定睡眠时间，使用数据中的第一个时间点
            try {
                String firstTimestamp = sleepPeriodData.get(0).getTimestamp();
                Date firstDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(firstTimestamp);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String firstDateStr = dateFormat.format(firstDate);
                sleepData.setDate(firstDateStr);
            } catch (ParseException e) {
                // 如果解析失败，使用原日期
                sleepData.setDate(date);
            }
        }
        
        if (sleepTime != null && wakeTime != null) {
            sleepData.setSleepTime(sleepTime);
            sleepData.setWakeTime(wakeTime);
            
            // 计算睡眠时长（分钟）
            long durationMs = wakeTime.getTime() - sleepTime.getTime();
            if (durationMs < 0) {
                durationMs += 24 * 60 * 60 * 1000; // 跨天情况
            }
            long durationMinutes = durationMs / (1000 * 60);
            sleepData.setDuration(durationMinutes);
            
            // 检查逻辑是否合理：睡眠时长应该在合理范围内（3-12小时）
            if (durationMinutes < 180 || durationMinutes > 720) {
                // 如果睡眠时长不合理，可能是计算错误，使用简单方法
                // 使用第一个数据点作为入睡时间，最后一个数据点作为起床时间
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    sleepTime = dateFormat.parse(sleepPeriodData.get(0).getTimestamp());
                    wakeTime = dateFormat.parse(sleepPeriodData.get(sleepPeriodData.size() - 1).getTimestamp());
                    
                    // 重新计算时长
                    durationMs = wakeTime.getTime() - sleepTime.getTime();
                    if (durationMs < 0) {
                        durationMs += 24 * 60 * 60 * 1000;
                    }
                    durationMinutes = durationMs / (1000 * 60);
                    
                    // 确保时长在合理范围内
                    if (durationMinutes > 720) {
                        durationMinutes = 480; // 默认8小时
                    }
                    
                    sleepData.setSleepTime(sleepTime);
                    sleepData.setWakeTime(wakeTime);
                    sleepData.setDuration(durationMinutes);
                } catch (ParseException e) {
                    // 解析失败，使用默认值
                    sleepData.setDuration(480); // 默认8小时
                }
            }
        }
        
        // 计算睡眠质量
        int quality = calculateSleepQuality(sleepPeriodData);
        sleepData.setQuality(quality);
        
        return sleepData;
    }
    
    /**
     * 找出入睡时间
     */
    private Date findSleepTime(List<HeartRateEntry> dayData, String sleepStartTime, String sleepEndTime, SimpleDateFormat dateTimeFormat) {
        try {
            // 解析睡眠时间区间
            String[] sleepStartParts = sleepStartTime.split(":");
            int sleepStartHour = Integer.parseInt(sleepStartParts[0]);
            int sleepStartMinute = Integer.parseInt(sleepStartParts[1]);
            
            String[] sleepEndParts = sleepEndTime.split(":");
            int sleepEndHour = Integer.parseInt(sleepEndParts[0]);
            int sleepEndMinute = Integer.parseInt(sleepEndParts[1]);
            
            // 找出睡眠时间区间内的心率最低点
            HeartRateEntry minEntry = null;
            int minBpm = Integer.MAX_VALUE;
            
            for (HeartRateEntry entry : dayData) {
                Date entryTime = dateTimeFormat.parse(entry.getTimestamp());
                if (entryTime == null) continue;
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(entryTime);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                
                // 检查是否在睡眠时间区间内
                boolean isInSleepRange = false;
                
                if (sleepStartHour > sleepEndHour) {
                    // 跨夜情况，如21:00-7:00
                    isInSleepRange = (hour > sleepStartHour || (hour == sleepStartHour && minute >= sleepStartMinute)) ||
                            (hour < sleepEndHour || (hour == sleepEndHour && minute <= sleepEndMinute));
                } else {
                    // 同一天内，如23:00-6:00
                    isInSleepRange = (hour > sleepStartHour || (hour == sleepStartHour && minute >= sleepStartMinute)) &&
                            (hour < sleepEndHour || (hour == sleepEndHour && minute <= sleepEndMinute));
                }
                
                if (isInSleepRange && entry.getBpm() < minBpm) {
                    minBpm = entry.getBpm();
                    minEntry = entry;
                }
            }
            
            return minEntry != null ? dateTimeFormat.parse(minEntry.getTimestamp()) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 找出起床时间
     */
    private Date findWakeTime(List<HeartRateEntry> dayData, String sleepStartTime, String sleepEndTime, SimpleDateFormat dateTimeFormat) {
        try {
            // 解析睡眠时间区间
            String[] sleepEndParts = sleepEndTime.split(":");
            int sleepEndHour = Integer.parseInt(sleepEndParts[0]);
            int sleepEndMinute = Integer.parseInt(sleepEndParts[1]);
            
            // 找出早晨心率显著上升的时间点
            HeartRateEntry wakeEntry = null;
            
            for (int i = 1; i < dayData.size(); i++) {
                HeartRateEntry prevEntry = dayData.get(i - 1);
                HeartRateEntry currEntry = dayData.get(i);
                
                Date currTime = dateTimeFormat.parse(currEntry.getTimestamp());
                if (currTime == null) continue;
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(currTime);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                
                // 检查是否在起床时间附近（睡眠结束时间后2小时内）
                boolean isInWakeRange = false;
                
                if (hour > sleepEndHour || (hour == sleepEndHour && minute >= sleepEndMinute)) {
                    // 睡眠结束时间后2小时内
                    int hoursAfterSleepEnd = (hour - sleepEndHour);
                    if (hoursAfterSleepEnd <= 2) {
                        isInWakeRange = true;
                    }
                }
                
                // 如果心率显著上升（增加超过10%），认为是起床时间
                if (isInWakeRange && currEntry.getBpm() > prevEntry.getBpm() * 1.1) {
                    wakeEntry = currEntry;
                    break;
                }
            }
            
            return wakeEntry != null ? dateTimeFormat.parse(wakeEntry.getTimestamp()) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 计算睡眠质量
     */
    private int calculateSleepQuality(List<HeartRateEntry> sleepPeriodData) {
        if (sleepPeriodData.isEmpty()) {
            return 50; // 默认中等质量
        }
        
        // 计算平均心率
        double totalHeartRate = 0;
        for (HeartRateEntry entry : sleepPeriodData) {
            totalHeartRate += entry.getBpm();
        }
        double avgHeartRate = totalHeartRate / sleepPeriodData.size();
        
        // 计算心率变异性（简化版）
        double variance = 0;
        for (HeartRateEntry entry : sleepPeriodData) {
            variance += Math.pow(entry.getBpm() - avgHeartRate, 2);
        }
        variance /= sleepPeriodData.size();
        double stdDev = Math.sqrt(variance);
        
        // 基于心率和心率变异性计算睡眠质量
        int quality = 100;
        
        // 平均心率越低，睡眠质量越好
        if (avgHeartRate < 50) {
            quality -= 0;
        } else if (avgHeartRate < 60) {
            quality -= 10;
        } else if (avgHeartRate < 70) {
            quality -= 20;
        } else {
            quality -= 30;
        }
        
        // 心率变异性越小，睡眠质量越好
        if (stdDev < 5) {
            quality -= 0;
        } else if (stdDev < 10) {
            quality -= 10;
        } else if (stdDev < 15) {
            quality -= 20;
        } else {
            quality -= 30;
        }
        
        // 确保质量在0-100范围内
        quality = Math.max(0, Math.min(100, quality));
        
        return quality;
    }
    
    /**
     * 计算整体睡眠评分
     */
    public int calculateOverallSleepScore(List<SleepData> sleepDataList) {
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return 0;
        }
        
        // 计算平均睡眠质量
        double totalQuality = 0;
        for (SleepData sleepData : sleepDataList) {
            totalQuality += sleepData.getQuality();
        }
        double avgQuality = totalQuality / sleepDataList.size();
        
        // 计算规律性评分
        int regularityScore = calculateRegularityScore(sleepDataList);
        
        // 综合评分：睡眠质量占70%，规律性占30%
        int overallScore = (int) (avgQuality * 0.7 + regularityScore * 0.3);
        
        return overallScore;
    }
    
    /**
     * 计算睡眠规律性评分
     */
    public int calculateRegularityScore(List<SleepData> sleepDataList) {
        if (sleepDataList == null || sleepDataList.size() < 2) {
            return 50; // 数据不足，返回中等评分
        }
        
        // 计算入睡时间的标准差
        List<Integer> sleepMinutes = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (SleepData sleepData : sleepDataList) {
            String sleepTimeStr = timeFormat.format(sleepData.getSleepTime());
            String[] parts = sleepTimeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            sleepMinutes.add(hours * 60 + minutes);
        }
        
        // 计算平均值
        double totalMinutes = 0;
        for (int minutes : sleepMinutes) {
            totalMinutes += minutes;
        }
        double avgMinutes = totalMinutes / sleepMinutes.size();
        
        // 计算方差和标准差
        double variance = 0;
        for (int minutes : sleepMinutes) {
            variance += Math.pow(minutes - avgMinutes, 2);
        }
        variance /= sleepMinutes.size();
        double stdDev = Math.sqrt(variance);
        
        // 标准差越小，规律性越好
        int regularityScore = 100;
        if (stdDev < 30) { // 30分钟以内
            regularityScore = 100;
        } else if (stdDev < 60) { // 1小时以内
            regularityScore = 80;
        } else if (stdDev < 90) { // 1.5小时以内
            regularityScore = 60;
        } else if (stdDev < 120) { // 2小时以内
            regularityScore = 40;
        } else {
            regularityScore = 20;
        }
        
        return regularityScore;
    }
    
    /**
     * 生成睡眠建议
     */
    public List<SleepAdvice> generateSleepAdvice(List<SleepData> sleepDataList, int overallScore) {
        List<SleepAdvice> adviceList = new ArrayList<>();
        
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return adviceList;
        }
        
        // 基于整体评分生成建议
        if (overallScore < 40) {
            adviceList.add(new SleepAdvice(1, "睡眠质量较差", "您的睡眠质量评分较低，建议您调整作息时间，保持规律睡眠。", "改善建议", 1));
        } else if (overallScore < 70) {
            adviceList.add(new SleepAdvice(1, "睡眠质量一般", "您的睡眠质量有提升空间，建议您保持规律作息，避免睡前使用电子设备。", "改善建议", 2));
        } else {
            adviceList.add(new SleepAdvice(1, "睡眠质量良好", "您的睡眠质量很好，请继续保持良好的作息习惯。", "保持建议", 3));
        }
        
        // 基于睡眠时长生成建议
        double avgDuration = 0;
        for (SleepData sleepData : sleepDataList) {
            avgDuration += sleepData.getDurationInHours();
        }
        avgDuration /= sleepDataList.size();
        
        if (avgDuration < 6) {
            adviceList.add(new SleepAdvice(2, "睡眠时间不足", "您的平均睡眠时间不足6小时，建议您保证每晚7-8小时的睡眠时间。", "健康建议", 1));
        } else if (avgDuration > 9) {
            adviceList.add(new SleepAdvice(2, "睡眠时间过长", "您的平均睡眠时间超过9小时，过长的睡眠时间可能影响日间精力。", "健康建议", 2));
        }
        
        // 基于规律性生成建议
        int regularityScore = calculateRegularityScore(sleepDataList);
        if (regularityScore < 60) {
            adviceList.add(new SleepAdvice(3, "作息不规律", "您的作息时间不太规律，建议您每天在同一时间入睡和起床，即使在周末。", "规律建议", 1));
        }
        
        return adviceList;
    }
}