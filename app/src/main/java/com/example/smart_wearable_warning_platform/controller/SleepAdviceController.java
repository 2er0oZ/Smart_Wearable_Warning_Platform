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
        void updateSleepTrendChart(List<SleepData> data);
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
        
        // 使用服务层分析睡眠数据
        List<SleepData> sleepDataList = sleepAnalysisService.analyzeSleepData(heartRateData, threshold);
        if (sleepDataList.isEmpty()) {
            view.showNoDataMessage();
            return;
        }
        
        // 使用服务层计算睡眠质量评分
        int overallScore = sleepAnalysisService.calculateOverallSleepScore(sleepDataList);
        
        // 更新UI
        view.updateSleepScoreUI(overallScore);
        view.updateSleepStatistics(sleepDataList);
        view.updateLastSleepInfo(sleepDataList.get(sleepDataList.size() - 1));
        view.updateSleepTrendChart(sleepDataList);
        view.updateSleepAdvice(sleepAnalysisService.generateSleepAdvice(sleepDataList, overallScore));
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
    
    /**
     * 准备睡眠趋势图表数据
     */
    public List<SleepData> prepareChartData(List<SleepData> sleepDataList) {
        // 直接返回所有睡眠数据，按日期排序
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按日期排序
        List<SleepData> sortedData = new ArrayList<>(sleepDataList);
        sortedData.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        
        return sortedData;
    }
}