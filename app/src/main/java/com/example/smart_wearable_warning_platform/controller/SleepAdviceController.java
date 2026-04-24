package com.example.smart_wearable_warning_platform.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 数据缓存（按用户隔离）
    private List<SleepData> cachedSleepDataList;
    private int cachedOverallScore;
    private SleepData cachedLastSleepData;
    private List<SleepAdvice> cachedAdviceList;
    private boolean isDataLoaded = false;
    private String cachedUsername = null; // 记录缓存对应的用户名
    
    // 单例实例
    private static SleepAdviceController instance;
    
    private SleepAdviceController(Context context) {
        this.dataManager = new DataManager(context);
        this.sleepAnalysisService = new SleepAnalysisService();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 获取单例实例
     */
    public static SleepAdviceController getInstance(Context context) {
        if (instance == null) {
            instance = new SleepAdviceController(context);
        }
        return instance;
    }
    
    /**
     * 清除缓存（切换用户时调用）
     */
    private void clearCache() {
        isDataLoaded = false;
        cachedSleepDataList = null;
        cachedOverallScore = 0;
        cachedLastSleepData = null;
        cachedAdviceList = null;
        cachedUsername = null;
    }
    
    /**
     * 设置视图
     */
    public void setView(View view) {
        this.view = view;
    }
    
    /**
     * 加载睡眠数据（异步执行，带缓存）
     */
    public void loadSleepData() {
        android.util.Log.d("SleepAdviceController", "开始加载睡眠数据");
        
        User currentUser = dataManager.getCurrentUser();
        if (currentUser == null) {
            android.util.Log.e("SleepAdviceController", "用户信息获取失败");
            if (view != null) {
                mainHandler.post(() -> view.showErrorMessage("用户信息获取失败"));
            }
            return;
        }
        
        // 检查缓存是否属于当前用户
        if (isDataLoaded && cachedSleepDataList != null && cachedUsername != null 
                && cachedUsername.equals(currentUser.getUsername())) {
            android.util.Log.d("SleepAdviceController", "使用缓存数据，跳过加载");
            mainHandler.post(() -> {
                view.updateSleepScoreUI(cachedOverallScore);
                view.updateSleepStatistics(cachedSleepDataList);
                view.updateLastSleepInfo(cachedLastSleepData);
                view.updateSleepAdvice(cachedAdviceList);
            });
            return;
        }
        
        // 用户不匹配，清除旧缓存
        if (cachedUsername != null && !cachedUsername.equals(currentUser.getUsername())) {
            android.util.Log.d("SleepAdviceController", "用户切换，清除旧缓存: " + cachedUsername + " -> " + currentUser.getUsername());
            clearCache();
        }
        
        executorService.execute(() -> {
            try {
                android.util.Log.d("SleepAdviceController", "异步任务开始执行");
                
                android.util.Log.d("SleepAdviceController", "当前用户: " + currentUser.getUsername());
                
                // 获取心率数据
                List<HeartRateEntry> heartRateData = dataManager.getHeartRateData(currentUser.getUsername());
                android.util.Log.d("SleepAdviceController", "心率数据数量: " + (heartRateData != null ? heartRateData.size() : 0));
                
                if (heartRateData == null || heartRateData.isEmpty()) {
                    android.util.Log.w("SleepAdviceController", "心率数据为空");
                    mainHandler.post(() -> view.showNoDataMessage());
                    return;
                }
                
                // 获取学生阈值设置
                StudentThreshold threshold = dataManager.getStudentThreshold(currentUser.getUsername());
                if (threshold == null) {
                    android.util.Log.d("SleepAdviceController", "使用默认阈值");
                    threshold = new StudentThreshold(); // 使用默认值
                }
                
                android.util.Log.d("SleepAdviceController", "睡眠时间区间: " + threshold.getSleepStartTime() + " - " + threshold.getSleepEndTime());
                
                // 分析睡眠数据
                List<SleepData> sleepDataList = sleepAnalysisService.analyzeSleepData(heartRateData, threshold);
                android.util.Log.d("SleepAdviceController", "分析得到的睡眠数据数量: " + sleepDataList.size());
                
                if (sleepDataList.isEmpty()) {
                    android.util.Log.w("SleepAdviceController", "睡眠数据列表为空");
                    mainHandler.post(() -> view.showNoDataMessage());
                    return;
                }
                
                // 计算睡眠质量评分
                int overallScore = calculateOverallSleepScore(sleepDataList);
                android.util.Log.d("SleepAdviceController", "整体睡眠评分: " + overallScore);
                
                // 找到最近一个完整的睡眠数据
                SleepData lastCompleteSleepData = findLastCompleteSleepData(sleepDataList);
                android.util.Log.d("SleepAdviceController", "最近完整睡眠数据: " + (lastCompleteSleepData != null ? lastCompleteSleepData.getDate() : "null"));
                
                // 生成睡眠建议
                List<SleepAdvice> adviceList = sleepAnalysisService.generateSleepAdvice(sleepDataList, overallScore);
                android.util.Log.d("SleepAdviceController", "生成的建议数量: " + adviceList.size());
                
                // 缓存数据（关联当前用户）
                cachedSleepDataList = sleepDataList;
                cachedOverallScore = overallScore;
                cachedLastSleepData = lastCompleteSleepData;
                cachedAdviceList = adviceList;
                cachedUsername = currentUser.getUsername();
                isDataLoaded = true;
                android.util.Log.d("SleepAdviceController", "数据已缓存，用户: " + cachedUsername);

                
                // 在主线程更新UI
                mainHandler.post(() -> {
                    android.util.Log.d("SleepAdviceController", "开始更新UI");
                    view.updateSleepScoreUI(overallScore);
                    view.updateSleepStatistics(sleepDataList);
                    view.updateLastSleepInfo(lastCompleteSleepData);
                    view.updateSleepAdvice(adviceList);
                    android.util.Log.d("SleepAdviceController", "UI更新完成");
                });
            } catch (Exception e) {
                android.util.Log.e("SleepAdviceController", "数据加载失败", e);
                mainHandler.post(() -> view.showErrorMessage("数据加载失败: " + e.getMessage()));
            }
        });
    }
    
    /**
     * 刷新睡眠数据（强制重新加载）
     */
    public void refreshSleepData() {
        android.util.Log.d("SleepAdviceController", "刷新睡眠数据");
        
        // 清除缓存
        clearCache();
        
        // 重新加载数据
        loadSleepData();
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
     * 找到最近一个完整的睡眠数据
     * 如果最后一天的数据不完整（只有入睡时间没有起床时间），返回前一天的完整数据
     */
    private SleepData findLastCompleteSleepData(List<SleepData> sleepDataList) {
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return null;
        }
        
        // 从后往前遍历，找到第一个完整的睡眠数据
        for (int i = sleepDataList.size() - 1; i >= 0; i--) {
            SleepData sleepData = sleepDataList.get(i);
            
            // 检查睡眠数据是否完整
            if (isSleepDataComplete(sleepData)) {
                return sleepData;
            }
        }
        
        // 如果没有完整的数据，返回最后一个（即使不完整）
        return sleepDataList.get(sleepDataList.size() - 1);
    }
    
    /**
     * 检查睡眠数据是否完整
     * 完整的睡眠数据应该有合理的入睡时间和起床时间
     */
    private boolean isSleepDataComplete(SleepData sleepData) {
        if (sleepData == null) {
            return false;
        }
        
        // 检查入睡时间和起床时间是否存在
        if (sleepData.getSleepTime() == null || sleepData.getWakeTime() == null) {
            return false;
        }
        
        // 检查睡眠时长是否合理（3-12小时）
        long durationMinutes = sleepData.getDuration();
        if (durationMinutes < 180 || durationMinutes > 720) {
            return false;
        }
        
        return true;
    }
    
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