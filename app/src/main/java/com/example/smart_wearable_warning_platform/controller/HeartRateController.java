package com.example.smart_wearable_warning_platform.controller;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;
import com.example.smart_wearable_warning_platform.model.User;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 心率控制器
 */
public class HeartRateController {
    
    public interface View {
        void showData(List<HeartRateEntry> data);
        void showFilteredData(List<HeartRateEntry> data);
        void showNoDataMessage();
        void showErrorMessage(String message);
        void updateStatistics(List<HeartRateEntry> data);
        void updateChart(List<HeartRateEntry> data);
        void showTimeSelection(String startDate, String startTime, String endDate, String endTime);
        String getStartDate();
        String getStartTime();
        String getEndDate();
        String getEndTime();
    }
    
    private View view;
    private DataManager dataManager;
    private List<HeartRateEntry> allData;
    private List<HeartRateEntry> filteredData;
    private boolean isQueryActive = false;
    
    public HeartRateController(View view, Context context) {
        this.view = view;
        this.dataManager = new DataManager(context);
    }
    
    /**
     * 加载心率数据
     */
    public void loadData() {
        User currentUser = dataManager.getCurrentUser();
        if (currentUser != null) {
            allData = dataManager.getHeartRateData(currentUser.getUsername());
            if (!allData.isEmpty()) {
                if (isQueryActive && filteredData != null && !filteredData.isEmpty()) {
                    view.showFilteredData(filteredData);
                } else {
                    view.showData(allData);
                }
            } else {
                view.showNoDataMessage();
            }
        }
    }
    
    /**
     * 根据时间区间查询数据
     */
    public void queryDataByTimeRange() {
        if (allData == null || allData.isEmpty()) {
            view.showErrorMessage("没有可查询的数据");
            return;
        }
        
        String startDateStr = view.getStartDate();
        String startTimeStr = view.getStartTime();
        String endDateStr = view.getEndDate();
        String endTimeStr = view.getEndTime();
        
        // 验证输入
        if (startDateStr.isEmpty() || startTimeStr.isEmpty() || endDateStr.isEmpty() || endTimeStr.isEmpty()) {
            view.showErrorMessage("请选择完整的时间区间");
            return;
        }
        
        try {
            // 构建开始和结束时间
            String startDateTime = startDateStr + " " + startTimeStr + ":00";
            String endDateTime = endDateStr + " " + endTimeStr + ":00";
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date startDate = sdf.parse(startDateTime);
            Date endDate = sdf.parse(endDateTime);
            
            if (startDate == null || endDate == null) {
                view.showErrorMessage("时间格式不正确");
                return;
            }
            
            if (startDate.after(endDate)) {
                view.showErrorMessage("开始时间不能晚于结束时间");
                return;
            }
            
            // 过滤数据
            filteredData = new ArrayList<>();
            for (HeartRateEntry entry : allData) {
                try {
                    Date entryDate = sdf.parse(entry.getTimestamp());
                    if (entryDate != null && !entryDate.before(startDate) && !entryDate.after(endDate)) {
                        filteredData.add(entry);
                    }
                } catch (ParseException e) {
                    // 跳过格式不正确的条目
                }
            }
            
            if (filteredData.isEmpty()) {
                view.showErrorMessage("所选时间区间内没有数据");
                isQueryActive = false;
            } else {
                view.showFilteredData(filteredData);
                isQueryActive = true;
            }
            
        } catch (ParseException e) {
            view.showErrorMessage("时间格式不正确");
        }
    }
    
    /**
     * 重置时间过滤器，显示所有数据
     */
    public void resetTimeFilter() {
        isQueryActive = false;
        filteredData = null;
        if (allData != null && !allData.isEmpty()) {
            view.showData(allData);
        }
    }
    
    /**
     * 处理CSV文件
     */
    public void processCsvFile(Uri uri, Context context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            List<HeartRateEntry> data = dataManager.parseCSV(inputStream);
            inputStream.close();

            if (data.isEmpty()) {
                view.showErrorMessage("CSV文件为空或格式错误");
                return;
            }

            User currentUser = dataManager.getCurrentUser();
            if (currentUser != null) {
                // 保存并合并已有数据
                dataManager.saveHeartRateData(currentUser.getUsername(), data);
                // 使用原始新条目生成预警
                dataManager.checkAndGenerateAlerts(currentUser.getUsername(), data);
                
                // 将合并后的全量数据加载到图表
                allData = dataManager.getHeartRateData(currentUser.getUsername());
                
                // 重置查询状态，因为新数据可能使之前的查询失效
                isQueryActive = false;
                filteredData = null;
                
                // 清空时间选择控件
                view.showTimeSelection("", "", "", "");
                
                // 显示更新后的数据
                view.showData(allData);
            }

        } catch (Exception e) {
            view.showErrorMessage("文件读取失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存查询状态
     */
    public void saveQueryState(Context context) {
        // 使用SharedPreferences保存查询状态
        android.content.SharedPreferences prefs = context.getSharedPreferences("HeartRateFragment", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        // 保存查询状态
        editor.putBoolean("isQueryActive", isQueryActive);
        
        // 保存时间选择
        editor.putString("startDate", view.getStartDate());
        editor.putString("startTime", view.getStartTime());
        editor.putString("endDate", view.getEndDate());
        editor.putString("endTime", view.getEndTime());
        
        editor.apply();
    }
    
    /**
     * 恢复查询状态
     */
    public void restoreQueryState(Context context) {
        // 从SharedPreferences恢复查询状态
        android.content.SharedPreferences prefs = context.getSharedPreferences("HeartRateFragment", Context.MODE_PRIVATE);
        
        // 恢复查询状态
        isQueryActive = prefs.getBoolean("isQueryActive", false);
        
        // 恢复时间选择
        String startDate = prefs.getString("startDate", "");
        String startTime = prefs.getString("startTime", "");
        String endDate = prefs.getString("endDate", "");
        String endTime = prefs.getString("endTime", "");
        
        view.showTimeSelection(startDate, startTime, endDate, endTime);
        
        // 如果有查询状态，重新执行查询
        if (isQueryActive && !startDate.isEmpty() && !startTime.isEmpty() && !endDate.isEmpty() && !endTime.isEmpty()) {
            queryDataByTimeRange();
        }
    }
}