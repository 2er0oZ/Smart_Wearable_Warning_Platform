package com.example.smart_wearable_warning_platform.service;

import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.HealthAlert;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 心率服务
 */
public class HeartRateService {
    
    /**
     * 检查心率数据并生成预警
     * @param username 学生用户名
     * @param heartRateData 心率数据列表
     * @param threshold 学生阈值设置
     * @return 预警列表
     */
    public List<HealthAlert> checkAndGenerateAlerts(String username, List<HeartRateEntry> heartRateData, StudentThreshold threshold) {
        List<HealthAlert> alerts = new ArrayList<>();
        
        if (heartRateData == null || heartRateData.isEmpty()) {
            return alerts;
        }
        
        // 获取阈值设置
        int minHeartRate = threshold.getMinHr();
        int maxHeartRate = threshold.getMaxHr();
        int minStepFreq = threshold.getMinStep();
        int maxStepFreq = threshold.getMaxStep();
        
        // 获取睡眠时间区间
        String sleepStartTime = threshold.getSleepStartTime();
        String sleepEndTime = threshold.getSleepEndTime();
        
        // 解析睡眠时间
        int sleepHour = Integer.parseInt(sleepStartTime.split(":")[0]);
        int wakeHour = Integer.parseInt(sleepEndTime.split(":")[0]);
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH", Locale.getDefault());
        
        for (HeartRateEntry entry : heartRateData) {
            try {
                // 解析时间戳
                Date entryDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(entry.getTimestamp());
                if (entryDate == null) {
                    continue;
                }
                
                // 获取小时
                int hour = Integer.parseInt(timeFormat.format(entryDate));
                
                // 判断是否在睡眠时间区间内
                boolean isInSleepTime = false;
                if (sleepHour > wakeHour) {
                    // 跨天睡眠，如21:00-7:00
                    isInSleepTime = (hour >= sleepHour || hour < wakeHour);
                } else {
                    // 不跨天睡眠，如23:00-6:00
                    isInSleepTime = (hour >= sleepHour && hour < wakeHour);
                }
                
                // 在睡眠时间区间内，检查心率和步频是否同时超出阈值
                if (isInSleepTime) {
                    boolean isHeartRateAbnormal = entry.getBpm() < minHeartRate || entry.getBpm() > maxHeartRate;
                    boolean isStepFreqAbnormal = entry.getStepFrequency() < minStepFreq || entry.getStepFrequency() > maxStepFreq;
                    
                    // 只有当心率和步频同时异常时才生成预警
                    if (isHeartRateAbnormal && isStepFreqAbnormal) {
                        // 生成综合预警信息
                        String heartRateType = entry.getBpm() < minHeartRate ? "心率过低" : "心率过高";
                        String alertMessage = String.format("睡眠时间异常：%s(心率%dbpm)，(步频%d)", 
                            heartRateType, entry.getBpm(), entry.getStepFrequency());
                        
                        HealthAlert alert = new HealthAlert();
                        alert.setStudentName(username);
                        alert.setMessage(alertMessage);
                        alert.setTimestamp(entry.getTimestamp());
                        alert.setBpm(entry.getBpm());
                        alert.setStepFreq(entry.getStepFrequency());
                        alert.setStep(true); // 标记为睡眠时间预警
                        
                        alerts.add(alert);
                    }
                }
                
            } catch (ParseException e) {
                // 跳过格式不正确的数据
            }
        }
        
        return alerts;
    }
    
    /**
     * 计算心率统计信息
     * @param heartRateData 心率数据列表
     * @return 统计信息数组 [平均值, 最大值, 最小值]
     */
    public double[] calculateHeartRateStatistics(List<HeartRateEntry> heartRateData) {
        if (heartRateData == null || heartRateData.isEmpty()) {
            return new double[]{0, 0, 0};
        }
        
        double sum = 0;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        
        for (HeartRateEntry entry : heartRateData) {
            int heartRate = entry.getBpm();
            sum += heartRate;
            if (heartRate > max) max = heartRate;
            if (heartRate < min) min = heartRate;
        }
        
        return new double[]{sum / heartRateData.size(), max, min};
    }
    
    /**
     * 计算步频统计信息
     * @param heartRateData 心率数据列表（包含步频数据）
     * @return 统计信息数组 [最大值, 最小值]
     */
    public int[] calculateStepFreqStatistics(List<HeartRateEntry> heartRateData) {
        if (heartRateData == null || heartRateData.isEmpty()) {
            return new int[]{0, 0};
        }
        
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        
        for (HeartRateEntry entry : heartRateData) {
            int stepFreq = entry.getStepFrequency();
            if (stepFreq > max) max = stepFreq;
            if (stepFreq < min) min = stepFreq;
        }
        
        return new int[]{max, min};
    }
}