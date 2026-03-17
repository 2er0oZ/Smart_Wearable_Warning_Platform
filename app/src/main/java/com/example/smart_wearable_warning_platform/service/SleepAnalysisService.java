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

public class SleepAnalysisService {
    
    public List<SleepData> analyzeSleepData(List<HeartRateEntry> heartRateData, StudentThreshold threshold) {
        android.util.Log.d("SleepAnalysisService", "开始分析睡眠数据，数据量: " + (heartRateData != null ? heartRateData.size() : 0));
        
        List<SleepData> sleepDataList = new ArrayList<>();
        
        if (heartRateData == null || heartRateData.isEmpty()) {
            android.util.Log.d("SleepAnalysisService", "心率数据为空，返回空列表");
            return sleepDataList;
        }
        
        String sleepStartTime = threshold.getSleepStartTime();
        String sleepEndTime = threshold.getSleepEndTime();
        
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        List<HeartRateEntry> sortedData = new ArrayList<>(heartRateData);
        sortedData.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        android.util.Log.d("SleepAnalysisService", "数据排序完成");
        
        // 预先解析所有时间戳，避免重复解析
        List<Date> parsedDates = new ArrayList<>();
        for (HeartRateEntry entry : sortedData) {
            try {
                parsedDates.add(dateTimeFormat.parse(entry.getTimestamp()));
            } catch (ParseException e) {
                parsedDates.add(null);
            }
        }
        android.util.Log.d("SleepAnalysisService", "时间戳解析完成");
        
        // 解析睡眠时间区间
        int sleepStartHour = Integer.parseInt(sleepStartTime.split(":")[0]);
        int sleepEndHour = Integer.parseInt(sleepEndTime.split(":")[0]);
        
        // 单次遍历，识别睡眠周期
        int i = 0;
        while (i < sortedData.size()) {
            Date currentDate = parsedDates.get(i);
            if (currentDate == null) {
                i++;
                continue;
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            // 检查是否在睡眠开始时间附近
            boolean isInSleepStartRange = false;
            if (sleepStartHour > sleepEndHour) {
                isInSleepStartRange = (hour >= sleepStartHour || hour < sleepEndHour);
            } else {
                isInSleepStartRange = (hour >= sleepStartHour);
            }
            
            // 如果在睡眠开始时间附近，且步频较低，开始寻找睡眠周期
            if (isInSleepStartRange && sortedData.get(i).getStepFrequency() < 5) {
                // 找到入睡时间（心率最低点）
                int sleepIndex = i;
                int minBpm = sortedData.get(i).getBpm();
                
                // 在接下来2小时内寻找心率最低点
                int searchEnd = Math.min(i + 120, sortedData.size());
                for (int j = i; j < searchEnd; j++) {
                    if (parsedDates.get(j) != null && sortedData.get(j).getBpm() < minBpm) {
                        minBpm = sortedData.get(j).getBpm();
                        sleepIndex = j;
                    }
                }
                
                Date sleepTime = parsedDates.get(sleepIndex);
                
                                // 从入睡时间开始，寻找起床时间
                int wakeIndex = -1;
                int searchWakeEnd = Math.min(sleepIndex + 720, sortedData.size()); // 最多12小时
                
                // 检测步频变化：从平稳的0变为有起伏
                int consecutiveStepIncrease = 0;
                int consecutiveHeartRateIncrease = 0;
                
                for (int j = sleepIndex + 1; j < searchWakeEnd; j++) {
                    if (parsedDates.get(j) == null) continue;
                    
                    Calendar wakeCal = Calendar.getInstance();
                    wakeCal.setTime(parsedDates.get(j));
                    int wakeHour = wakeCal.get(Calendar.HOUR_OF_DAY);
                    
                    // 检查是否在起床时间附近（睡眠结束时间后4小时内）
                    boolean isInWakeRange = (wakeHour >= sleepEndHour) || 
                                          (wakeHour == sleepEndHour && wakeCal.get(Calendar.MINUTE) >= 0);
                    
                    if (!isInWakeRange) {
                        // 不在起床时间范围内，重置计数器
                        consecutiveStepIncrease = 0;
                        consecutiveHeartRateIncrease = 0;
                        continue;
                    }
                    
                    // 检测步频变化：从0变为有起伏（步频 > 0）
                    boolean stepFrequencyChanged = sortedData.get(j).getStepFrequency() > 0;
                    if (stepFrequencyChanged) {
                        consecutiveStepIncrease++;
                    } else {
                        consecutiveStepIncrease = 0;
                    }
                    
                    // 检测心率变化：心率上升（增加超过5%）
                    boolean heartRateIncreased = false;
                    if (j > 0) {
                        heartRateIncreased = sortedData.get(j).getBpm() > sortedData.get(j - 1).getBpm() * 1.05;
                    }
                    if (heartRateIncreased) {
                        consecutiveHeartRateIncrease++;
                    } else {
                        consecutiveHeartRateIncrease = 0;
                    }
                    
                    // 如果连续3个数据点都出现步频变化和心率上升，认为是起床时间
                    if (consecutiveStepIncrease >= 3 && consecutiveHeartRateIncrease >= 3) {
                        wakeIndex = j - 2; // 返回第一个变化的数据点
                        break;
                    }
                    
                    // 备用方案：如果步频 > 1 且心率上升超过5%，认为是起床时间
                    if (sortedData.get(j).getStepFrequency() > 1) {
                        if (j > 0 && sortedData.get(j).getBpm() > sortedData.get(j - 1).getBpm() * 1.05) {
                            wakeIndex = j;
                            break;
                        }
                    }
                }
                
                if (wakeIndex != -1) {
                    Date wakeTime = parsedDates.get(wakeIndex);
                    
                    // 计算睡眠时长
                    long durationMs = wakeTime.getTime() - sleepTime.getTime();
                    if (durationMs < 0) {
                        durationMs += 24 * 60 * 60 * 1000;
                    }
                    long duration = durationMs / (1000 * 60);
                    
                    // 检查睡眠时长是否合理（3-12小时）
                    if (duration >= 180 && duration <= 720) {
                        // 收集睡眠期间的数据
                        List<HeartRateEntry> sleepPeriodData = new ArrayList<>();
                        for (int j = sleepIndex; j <= wakeIndex && j < sortedData.size(); j++) {
                            sleepPeriodData.add(sortedData.get(j));
                        }
                        
                        int quality = calculateSleepQuality(sleepPeriodData);
                        String date = dateFormat.format(sleepTime);
                        
                        SleepData sleepData = new SleepData(date, sleepTime, wakeTime, duration, quality);
                        sleepDataList.add(sleepData);
                        android.util.Log.d("SleepAnalysisService", "找到睡眠周期: " + date + ", 时长: " + duration + "分钟");
                        
                        // 跳到起床时间之后
                        i = wakeIndex + 1;
                        continue;
                    }
                }
            }
            
            i++;
        }
        
        android.util.Log.d("SleepAnalysisService", "分析完成，找到: " + sleepDataList.size() + " 个睡眠周期");
        return sleepDataList;
    }
    
    private int calculateSleepQuality(List<HeartRateEntry> sleepPeriodData) {
        if (sleepPeriodData.isEmpty()) {
            return 50;
        }
        
        double totalHeartRate = 0;
        for (HeartRateEntry entry : sleepPeriodData) {
            totalHeartRate += entry.getBpm();
        }
        double avgHeartRate = totalHeartRate / sleepPeriodData.size();
        
        double variance = 0;
        for (HeartRateEntry entry : sleepPeriodData) {
            variance += Math.pow(entry.getBpm() - avgHeartRate, 2);
        }
        variance /= sleepPeriodData.size();
        double stdDev = Math.sqrt(variance);
        
        int quality = 100;
        
        if (avgHeartRate < 50) {
            quality -= 0;
        } else if (avgHeartRate < 60) {
            quality -= 10;
        } else if (avgHeartRate < 70) {
            quality -= 20;
        } else {
            quality -= 30;
        }
        
        if (stdDev < 5) {
            quality -= 0;
        } else if (stdDev < 10) {
            quality -= 10;
        } else if (stdDev < 15) {
            quality -= 20;
        } else {
            quality -= 30;
        }
        
        quality = Math.max(0, Math.min(100, quality));
        
        return quality;
    }
    
    public int calculateOverallSleepScore(List<SleepData> sleepDataList) {
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return 0;
        }
        
        double totalQuality = 0;
        for (SleepData sleepData : sleepDataList) {
            totalQuality += sleepData.getQuality();
        }
        double avgQuality = totalQuality / sleepDataList.size();
        
        int regularityScore = calculateRegularityScore(sleepDataList);
        
        int overallScore = (int) (avgQuality * 0.7 + regularityScore * 0.3);
        
        return overallScore;
    }
    
    public int calculateRegularityScore(List<SleepData> sleepDataList) {
        if (sleepDataList == null || sleepDataList.size() < 2) {
            return 50;
        }
        
        List<Integer> sleepMinutes = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (SleepData sleepData : sleepDataList) {
            String sleepTimeStr = timeFormat.format(sleepData.getSleepTime());
            String[] parts = sleepTimeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            sleepMinutes.add(hours * 60 + minutes);
        }
        
        double totalMinutes = 0;
        for (int minutes : sleepMinutes) {
            totalMinutes += minutes;
        }
        double avgMinutes = totalMinutes / sleepMinutes.size();
        
        double variance = 0;
        for (int minutes : sleepMinutes) {
            variance += Math.pow(minutes - avgMinutes, 2);
        }
        variance /= sleepMinutes.size();
        double stdDev = Math.sqrt(variance);
        
        int regularityScore = 100;
        if (stdDev < 30) {
            regularityScore = 100;
        } else if (stdDev < 60) {
            regularityScore = 80;
        } else if (stdDev < 90) {
            regularityScore = 60;
        } else if (stdDev < 120) {
            regularityScore = 40;
        } else {
            regularityScore = 20;
        }
        
        return regularityScore;
    }
    
    public List<SleepAdvice> generateSleepAdvice(List<SleepData> sleepDataList, int overallScore) {
        List<SleepAdvice> adviceList = new ArrayList<>();
        
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return adviceList;
        }
        
        if (overallScore < 40) {
            adviceList.add(new SleepAdvice(1, "睡眠质量较差", "您的睡眠质量评分较低，建议您调整作息时间，保持规律睡眠。", "改善建议", 5));
        } else if (overallScore < 70) {
            adviceList.add(new SleepAdvice(1, "睡眠质量一般", "您的睡眠质量有提升空间，建议您保持规律作息，避免睡前使用电子设备。", "改善建议", 3));
        } else {
            adviceList.add(new SleepAdvice(1, "睡眠质量良好", "您的睡眠质量很好，请继续保持良好的作息习惯。", "保持建议", 2));
        }
        
        double avgDuration = 0;
        for (SleepData sleepData : sleepDataList) {
            avgDuration += sleepData.getDurationInHours();
        }
        avgDuration /= sleepDataList.size();
        
        if (avgDuration < 6) {
            adviceList.add(new SleepAdvice(2, "睡眠时间不足", "您的平均睡眠时间不足6小时，建议您保证每晚7-8小时的睡眠时间。", "健康建议", 4));
        } else if (avgDuration > 9) {
            adviceList.add(new SleepAdvice(2, "睡眠时间过长", "您的平均睡眠时间超过9小时，过长的睡眠时间可能影响日间精力。", "健康建议", 3));
        }
        
        int regularityScore = calculateRegularityScore(sleepDataList);
        if (regularityScore < 60) {
            adviceList.add(new SleepAdvice(3, "睡眠规律性较差", "您的入睡时间不太规律，建议您固定每天的入睡时间，培养良好的生物钟。", "改善建议", 4));
        }
        
        return adviceList;
    }
}