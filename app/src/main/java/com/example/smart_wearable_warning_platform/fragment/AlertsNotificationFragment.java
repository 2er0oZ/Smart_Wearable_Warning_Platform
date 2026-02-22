package com.example.smart_wearable_warning_platform.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.adapter.AlertAdapter;
import com.example.smart_wearable_warning_platform.adapter.GroupedAlertAdapter;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HealthAlert;

import java.util.ArrayList;
import java.util.List;

public class AlertsNotificationFragment extends Fragment {

    private RecyclerView recyclerAlerts;
    private Button btnLoadMore;
    private DataManager dataManager;
    private AlertAdapter adapter;

    private List<HealthAlert> allAlerts = new ArrayList<>();
    // 分组数据
    private List<GroupedAlertAdapter.Group> allGroups = new ArrayList<>();
    private List<GroupedAlertAdapter.Group> shownGroups = new ArrayList<>();
    private GroupedAlertAdapter groupedAdapter;
    private int pageSize = 10; // 每页显示的分组数量

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_alerts, container, false);
        recyclerAlerts = root.findViewById(R.id.recycler_alerts);
        btnLoadMore = root.findViewById(R.id.btn_load_more);

        dataManager = new DataManager(requireContext());
        recyclerAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadAllAlerts();

        btnLoadMore.setOnClickListener(v -> loadMore());

        return root;
    }

    private void loadAllAlerts() {
        allAlerts = dataManager.getAllAlerts();
        // 聚合为按学生分组
        java.util.LinkedHashMap<String, GroupedAlertAdapter.Group> map = new java.util.LinkedHashMap<>();
        for (HealthAlert a : allAlerts) {
            String name = a.getStudentName();
            if (name == null) name = "未知";
            GroupedAlertAdapter.Group g = map.get(name);
            if (g == null) {
                g = new GroupedAlertAdapter.Group();
                g.studentName = name;
                map.put(name, g);
            }
            g.alerts.add(a);
        }
        allGroups.clear();
        allGroups.addAll(map.values());

        // 初始化显示第一页分组
        shownGroups.clear();
        int end = Math.min(pageSize, allGroups.size());
        for (int i = 0; i < end; i++) shownGroups.add(allGroups.get(i));

        groupedAdapter = new GroupedAlertAdapter(shownGroups);
        recyclerAlerts.setAdapter(groupedAdapter);
        btnLoadMore.setVisibility(allGroups.size() > shownGroups.size() ? View.VISIBLE : View.GONE);
    }

    private void loadMore() {
        int current = shownGroups.size();
        int end = Math.min(current + pageSize, allGroups.size());
        for (int i = current; i < end; i++) shownGroups.add(allGroups.get(i));
        groupedAdapter.notifyDataSetChanged();
        btnLoadMore.setVisibility(allGroups.size() > shownGroups.size() ? View.VISIBLE : View.GONE);
    }
}
