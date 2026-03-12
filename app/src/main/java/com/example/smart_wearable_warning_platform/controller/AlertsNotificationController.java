package com.example.smart_wearable_warning_platform.controller;

import android.content.Context;

import com.example.smart_wearable_warning_platform.adapter.AlertAdapter;
import com.example.smart_wearable_warning_platform.adapter.GroupedAlertAdapter;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HealthAlert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警通知控制器
 */
public class AlertsNotificationController {
    
    public interface View {
        void showAlerts(List<GroupedAlertAdapter.Group> groups);
        void showLoadMoreButton(boolean show);
        void updateAdapter(GroupedAlertAdapter adapter);
        void notifyDataSetChanged();
    }
    
    private View view;
    private DataManager dataManager;
    private List<HealthAlert> allAlerts = new ArrayList<>();
    // 分组数据
    private List<GroupedAlertAdapter.Group> allGroups = new ArrayList<>();
    private List<GroupedAlertAdapter.Group> shownGroups = new ArrayList<>();
    private int pageSize = 10; // 每页显示的分组数量
    
    public AlertsNotificationController(View view, Context context) {
        this.view = view;
        this.dataManager = new DataManager(context);
    }
    
    /**
     * 加载所有预警信息
     */
    public void loadAllAlerts() {
        allAlerts = dataManager.getAllAlerts();
        // 聚合为按学生分组
        Map<String, GroupedAlertAdapter.Group> map = new LinkedHashMap<>();
        for (HealthAlert a : allAlerts) {
            String name = a.getStudentName();
            String studentId = a.getStudentId();
            if (name == null) name = "未知";
            if (studentId == null) studentId = "";
            
            // 使用姓名作为分组键
            String key = name + "_" + studentId;
            GroupedAlertAdapter.Group g = map.get(key);
            if (g == null) {
                g = new GroupedAlertAdapter.Group();
                g.studentName = name;
                g.studentId = studentId;
                map.put(key, g);
            }
            g.alerts.add(a);
        }
        allGroups.clear();
        allGroups.addAll(map.values());

        // 初始化显示第一页分组
        shownGroups.clear();
        int end = Math.min(pageSize, allGroups.size());
        for (int i = 0; i < end; i++) shownGroups.add(allGroups.get(i));
        
        GroupedAlertAdapter adapter = new GroupedAlertAdapter(shownGroups);
        view.updateAdapter(adapter);
        view.showLoadMoreButton(allGroups.size() > shownGroups.size());
        
        // 通知View更新UI，显示或隐藏无预警信息提示
        view.showAlerts(allGroups);
    }
    
    /**
     * 加载更多预警信息
     */
    public void loadMore() {
        int current = shownGroups.size();
        int end = Math.min(current + pageSize, allGroups.size());
        for (int i = current; i < end; i++) shownGroups.add(allGroups.get(i));
        view.notifyDataSetChanged();
        view.showLoadMoreButton(allGroups.size() > shownGroups.size());
    }
}