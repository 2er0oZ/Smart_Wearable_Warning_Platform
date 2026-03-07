package com.example.smart_wearable_warning_platform.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.adapter.SleepAdviceAdapter;
import com.example.smart_wearable_warning_platform.controller.SleepAdviceController;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.SleepAdvice;
import com.example.smart_wearable_warning_platform.model.SleepData;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 睡眠建议Fragment
 */
public class SleepAdviceFragment extends Fragment implements SleepAdviceController.View {
    
    private SleepAdviceController controller;
    private TextView tvSleepScore, tvSleepQuality, tvAvgDuration, tvRegularityScore;
    private TextView tvLastSleepTime, tvLastWakeTime, tvLastDuration, tvLastQuality;
    private ProgressBar progressSleepQuality;
    private LineChart chartSleepTrend;
    private RecyclerView rvSleepAdvice;
    private SleepAdviceAdapter adviceAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sleep_advice, container, false);
        
        // 初始化视图
        tvSleepScore = root.findViewById(R.id.tv_sleep_score);
        tvSleepQuality = root.findViewById(R.id.tv_sleep_quality);
        tvAvgDuration = root.findViewById(R.id.tv_avg_duration);
        tvRegularityScore = root.findViewById(R.id.tv_regularity_score);
        tvLastSleepTime = root.findViewById(R.id.tv_last_sleep_time);
        tvLastWakeTime = root.findViewById(R.id.tv_last_wake_time);
        tvLastDuration = root.findViewById(R.id.tv_last_duration);
        tvLastQuality = root.findViewById(R.id.tv_last_quality);
        progressSleepQuality = root.findViewById(R.id.progress_sleep_quality);
        chartSleepTrend = root.findViewById(R.id.chart_sleep_trend);
        rvSleepAdvice = root.findViewById(R.id.rv_sleep_advice);
        
        // 初始化控制器
        controller = new SleepAdviceController(this, requireContext());
        
        // 设置RecyclerView
        rvSleepAdvice.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 加载睡眠数据
        controller.loadSleepData();
        
        return root;
    }
    

    
    // 实现View接口方法
    @Override
    public void showData(List<SleepData> data) {
        // 这个方法在当前实现中不需要，因为数据通过其他方法更新
    }
    
    @Override
    public void showNoDataMessage() {
        tvSleepScore.setText("--");
        tvSleepQuality.setText("无数据");
        tvAvgDuration.setText("-- 小时");
        tvRegularityScore.setText("--");
        tvLastSleepTime.setText("--:--");
        tvLastWakeTime.setText("--:--");
        tvLastDuration.setText("-- 小时");
        tvLastQuality.setText("--");
        progressSleepQuality.setProgress(0);
        
        // 显示空建议列表
        List<SleepAdvice> emptyAdvice = new ArrayList<>();
        emptyAdvice.add(new SleepAdvice(1, "暂无数据", "请先导入心率数据以获取睡眠建议", "提示", 1));
        adviceAdapter = new SleepAdviceAdapter(emptyAdvice);
        rvSleepAdvice.setAdapter(adviceAdapter);
    }
    
    @Override
    public void showErrorMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void updateSleepScoreUI(int score) {
        tvSleepScore.setText(String.valueOf(score));
        progressSleepQuality.setProgress(score);
        
        String qualityText;
        if (score >= 80) {
            qualityText = "优秀";
        } else if (score >= 60) {
            qualityText = "良好";
        } else if (score >= 40) {
            qualityText = "一般";
        } else {
            qualityText = "较差";
        }
        
        tvSleepQuality.setText(qualityText);
    }
    
    @Override
    public void updateSleepStatistics(List<SleepData> data) {
        if (data.isEmpty()) return;
        
        // 计算平均睡眠时长
        double totalDuration = 0;
        for (SleepData sleepData : data) {
            totalDuration += sleepData.getDurationInHours();
        }
        double avgDuration = totalDuration / data.size();
        tvAvgDuration.setText(String.format("%.1f 小时", avgDuration));
        
        // 计算规律性评分
        int regularityScore = calculateRegularityScore(data);
        tvRegularityScore.setText(String.valueOf(regularityScore));
    }
    
    @Override
    public void updateLastSleepInfo(SleepData lastSleepData) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        tvLastSleepTime.setText(timeFormat.format(lastSleepData.getSleepTime()));
        tvLastWakeTime.setText(timeFormat.format(lastSleepData.getWakeTime()));
        tvLastDuration.setText(String.format("%.1f 小时", lastSleepData.getDurationInHours()));
        tvLastQuality.setText(lastSleepData.getQualityDescription());
    }
    
    @Override
    public void updateSleepTrendChart(List<SleepData> data) {
        // 只显示最近7天的数据
        int daysToShow = Math.min(7, data.size());
        List<SleepData> recentData = data.subList(data.size() - daysToShow, data.size());
        
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < recentData.size(); i++) {
            entries.add(new Entry(i, recentData.get(i).getQuality()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "睡眠质量");
        dataSet.setColor(Color.parseColor("#4ECDC4"));
        dataSet.setCircleColor(Color.parseColor("#4ECDC4"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#334ECDC4"));
        
        LineData lineData = new LineData(dataSet);
        chartSleepTrend.setData(lineData);
        
        // 配置X轴
        XAxis xAxis = chartSleepTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(daysToShow);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < recentData.size()) {
                    String date = recentData.get(index).getDate();
                    return date.substring(5); // 只显示月-日
                }
                return "";
            }
        });
        
        // 配置Y轴
        YAxis leftAxis = chartSleepTrend.getAxisLeft();
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);
        leftAxis.setGranularity(20f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setTextColor(Color.parseColor("#666666"));
        
        // 隐藏右侧Y轴
        chartSleepTrend.getAxisRight().setEnabled(false);
        
        // 配置图表
        chartSleepTrend.getDescription().setEnabled(false);
        chartSleepTrend.getLegend().setEnabled(false);
        chartSleepTrend.setTouchEnabled(true);
        chartSleepTrend.setPinchZoom(true);
        chartSleepTrend.setDoubleTapToZoomEnabled(true);
        chartSleepTrend.setScaleEnabled(true);
        chartSleepTrend.setDragEnabled(true);
        chartSleepTrend.setBackgroundColor(Color.parseColor("#FAFAFA"));
        chartSleepTrend.setDrawGridBackground(false);
        
        // 刷新图表
        chartSleepTrend.notifyDataSetChanged();
        chartSleepTrend.invalidate();
    }
    
    @Override
    public void updateSleepAdvice(List<SleepAdvice> adviceList) {
        adviceAdapter = new SleepAdviceAdapter(adviceList);
        rvSleepAdvice.setAdapter(adviceAdapter);
    }
    
    /**
     * 分析睡眠数据
     */
    private List<SleepData> analyzeSleepData(List<HeartRateEntry> heartRateData, StudentThreshold threshold) {
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
                int quality = calculateSleepQuality(dayData, sleepTime, wakeTime);
                
                // 创建睡眠数据
                SleepData sleepData = new SleepData(date, sleepTime, wakeTime, duration, quality);
                sleepDataList.add(sleepData);
            }
        }
        
        return sleepDataList;
    }
    
    /**
     * 按日期分组数据
     */
    private Map<String, List<HeartRateEntry>> groupDataByDate(List<HeartRateEntry> heartRateData) {
        Map<String, List<HeartRateEntry>> dataByDate = new java.util.HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (HeartRateEntry entry : heartRateData) {
            String date = entry.getTimestamp().substring(0, 10); // 提取日期部分
            
            if (!dataByDate.containsKey(date)) {
                dataByDate.put(date, new ArrayList<>());
            }
            
            dataByDate.get(date).add(entry);
        }
        
        return dataByDate;
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
     * 计算睡眠质量评分
     */
    private int calculateSleepQuality(List<HeartRateEntry> dayData, Date sleepTime, Date wakeTime) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            // 收集睡眠期间的心率数据
            List<Integer> sleepBpmList = new ArrayList<>();
            
            for (HeartRateEntry entry : dayData) {
                Date entryTime = dateTimeFormat.parse(entry.getTimestamp());
                if (entryTime != null && !entryTime.before(sleepTime) && !entryTime.after(wakeTime)) {
                    sleepBpmList.add(entry.getBpm());
                }
            }
            
            if (sleepBpmList.isEmpty()) return 50; // 默认中等质量
            
            // 计算心率的变异性
            int minBpm = Integer.MAX_VALUE;
            int maxBpm = Integer.MIN_VALUE;
            long sumBpm = 0;
            
            for (int bpm : sleepBpmList) {
                sumBpm += bpm;
                if (bpm < minBpm) minBpm = bpm;
                if (bpm > maxBpm) maxBpm = bpm;
            }
            
            double avgBpm = (double) sumBpm / sleepBpmList.size();
            double variability = (maxBpm - minBpm) / avgBpm;
            
            // 计算睡眠质量评分
            int quality = 100;
            
            // 心率变异性越低，睡眠质量越高
            if (variability > 0.3) {
                quality -= 30;
            } else if (variability > 0.2) {
                quality -= 15;
            }
            
            // 平均心率越低，睡眠质量越高
            if (avgBpm > 70) {
                quality -= 20;
            } else if (avgBpm > 60) {
                quality -= 10;
            }
            
            // 确保评分在0-100之间
            quality = Math.max(0, Math.min(100, quality));
            
            return quality;
        } catch (Exception e) {
            e.printStackTrace();
            return 50; // 默认中等质量
        }
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
    

    

    
    /**
     * 计算规律性评分
     */
    private int calculateRegularityScore(List<SleepData> sleepDataList) {
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
     * 生成睡眠建议
     */
    private List<SleepAdvice> generateSleepAdvice(List<SleepData> sleepDataList, int overallScore) {
        List<SleepAdvice> adviceList = new ArrayList<>();
        
        // 根据整体评分生成建议
        if (overallScore < 60) {
            adviceList.add(new SleepAdvice(1, "改善睡眠质量", "您的睡眠质量评分较低，建议您保持规律的作息时间，避免在睡前使用电子设备。", "睡眠质量", 5));
        }
        
        // 计算平均睡眠时长
        double totalDuration = 0;
        for (SleepData sleepData : sleepDataList) {
            totalDuration += sleepData.getDurationInHours();
        }
        double avgDuration = totalDuration / sleepDataList.size();
        
        // 根据睡眠时长生成建议
        if (avgDuration < 7) {
            adviceList.add(new SleepAdvice(2, "增加睡眠时长", "您的平均睡眠时长不足7小时，建议您每天保证7-9小时的睡眠时间。", "睡眠时长", 4));
        } else if (avgDuration > 9) {
            adviceList.add(new SleepAdvice(2, "调整睡眠时长", "您的平均睡眠时长超过9小时，过长的睡眠时间可能影响日间精神状态。", "睡眠时长", 3));
        }
        
        // 计算规律性评分
        int regularityScore = calculateRegularityScore(sleepDataList);
        if (regularityScore < 70) {
            adviceList.add(new SleepAdvice(3, "保持规律作息", "您的睡眠时间不太规律，建议您每天在相同的时间入睡和起床，即使是周末。", "作息规律", 4));
        }
        
        // 通用睡眠建议
        adviceList.add(new SleepAdvice(4, "睡前放松", "睡前进行放松活动，如阅读、冥想或听轻音乐，有助于改善睡眠质量。", "生活习惯", 3));
        adviceList.add(new SleepAdvice(5, "优化睡眠环境", "保持卧室安静、黑暗和凉爽，使用舒适的床垫和枕头，有助于提高睡眠质量。", "睡眠环境", 2));
        
        return adviceList;
    }
}