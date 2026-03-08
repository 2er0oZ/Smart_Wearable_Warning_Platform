package com.example.smart_wearable_warning_platform.controller;

import android.content.Context;
import android.widget.Toast;

import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.SleepAdvice;
import com.example.smart_wearable_warning_platform.model.SleepData;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;
import com.example.smart_wearable_warning_platform.model.User;
import com.example.smart_wearable_warning_platform.service.SleepAnalysisService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 睡眠建议控制器
 */
public class SleepAdviceController {
    
    public interface View {
        void showData(List<SleepData> data);
        void showNoDataMessage();
        void showErrorMessage(String message);
        void updateSleepScoreUI(int score);
        void updateSleepStatistics(List<SleepData> data);
        void updateLastSleepInfo(SleepData lastSleepData);
        void updateSleepAdvice(List<SleepAdvice> adviceList);
    }
    
    private View view;
    private DataManager dataManager;
    private SleepAnalysisService sleepAnalysisService;
    
    public SleepAdviceController(View view, Context context) {
        this.view = view;
        this.dataManager = new DataManager(context);
        this.sleepAnalysisService = new SleepAnalysisService();
    }
    
    /**
     * 加载睡眠数据
     */
    public void loadSleepData() {
        User currentUser = dataManager.getCurrentUser();
        if (currentUser == null) {
            view.showErrorMessage("用户信息获取失败");
            return;
        }
        
        // 获取心率数据
        List<HeartRateEntry> heartRateData = dataManager.getHeartRateData(currentUser.getUsername());
        if (heartRateData == null || heartRateData.isEmpty()) {
            view.showNoDataMessage();
            return;
        }
        
        // 获取学生阈值设置
        StudentThreshold threshold = dataManager.getStudentThreshold(currentUser.getUsername());
        if (threshold == null) {
            threshold = new StudentThreshold(); // 使用默认值
        }
        
        // 分析睡眠数据
        List<SleepData> sleepDataList = sleepAnalysisService.analyzeSleepData(heartRateData, threshold);
        if (sleepDataList.isEmpty()) {
            view.showNoDataMessage();
            return;
        }
        
        // 计算睡眠质量评分
        int overallScore = calculateOverallSleepScore(sleepDataList);
        
        // 更新UI
        view.updateSleepScoreUI(overallScore);
        view.updateSleepStatistics(sleepDataList);
        view.updateLastSleepInfo(sleepDataList.get(sleepDataList.size() - 1));
        
        // 生成睡眠建议
        List<SleepAdvice> adviceList = sleepAnalysisService.generateSleepAdvice(sleepDataList, overallScore);
        view.updateSleepAdvice(adviceList);
    }
    
    /**
     * 创建最近7天的日期列表
     */
    private List<String> getLast7Days() {
        List<String> last7Days = new ArrayList<>();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // 从今天开始，往前推7天
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            last7Days.add(dateFormat.format(calendar.getTime()));
        }
        
        return last7Days;
    }
    
    // 睡眠趋势图表数据准备功能已移除
    
    /**
     * 计算规律性评分
     */
    public int calculateRegularityScore(List<SleepData> sleepDataList) {
        if (sleepDataList.size() < 2) return 50; // 数据不足，默认中等规律性
        
        // 计算入睡时间的标准差
        List<Integer> sleepMinutes = new ArrayList<>();
        for (SleepData sleepData : sleepDataList) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sleepData.getSleepTime());
            int minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            sleepMinutes.add(minutes);
        }
        
        // 计算平均值
        double sum = 0;
        for (int minutes : sleepMinutes) {
            sum += minutes;
        }
        double avg = sum / sleepMinutes.size();
        
        // 计算标准差
        double variance = 0;
        for (int minutes : sleepMinutes) {
            variance += Math.pow(minutes - avg, 2);
        }
        variance /= sleepMinutes.size();
        double stdDev = Math.sqrt(variance);
        
        // 标准差越小，规律性越高
        int regularityScore = 100;
        if (stdDev > 60) { // 超过1小时的标准差
            regularityScore -= 40;
        } else if (stdDev > 30) { // 超过30分钟的标准差
            regularityScore -= 20;
        }
        
        return Math.max(0, regularityScore);
    }
    
    /**
     * 计算整体睡眠评分
     */
    private int calculateOverallSleepScore(List<SleepData> sleepDataList) {
        if (sleepDataList.isEmpty()) return 0;
        
        int totalScore = 0;
        for (SleepData sleepData : sleepDataList) {
            totalScore += sleepData.getQuality();
        }
        
        return totalScore / sleepDataList.size();
    }
}