package com.example.smart_wearable_warning_platform.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.adapter.AlertAdapter;
import com.example.smart_wearable_warning_platform.adapter.GroupedAlertAdapter;
import com.example.smart_wearable_warning_platform.controller.AlertsNotificationController;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HealthAlert;

import java.util.ArrayList;
import java.util.List;

public class AlertsNotificationFragment extends Fragment implements AlertsNotificationController.View {

    private AlertsNotificationController controller;
    private RecyclerView recyclerAlerts;
    private Button btnLoadMore;
    private LinearLayout layoutNoAlerts;

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
        layoutNoAlerts = root.findViewById(R.id.layout_no_alerts);

        controller = new AlertsNotificationController(this, requireContext());
        recyclerAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));

        controller.loadAllAlerts();

        btnLoadMore.setOnClickListener(v -> controller.loadMore());

        return root;
    }

    // 实现View接口方法
    @Override
    public void showAlerts(List<GroupedAlertAdapter.Group> groups) {
        // 根据是否有预警信息显示不同的UI
        if (groups.isEmpty()) {
            recyclerAlerts.setVisibility(View.GONE);
            btnLoadMore.setVisibility(View.GONE);
            // 显示"暂无预警信息"提示
            if (layoutNoAlerts != null) {
                layoutNoAlerts.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerAlerts.setVisibility(View.VISIBLE);
            // 隐藏"暂无预警信息"提示
            if (layoutNoAlerts != null) {
                layoutNoAlerts.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void showLoadMoreButton(boolean show) {
        btnLoadMore.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    @Override
    public void updateAdapter(GroupedAlertAdapter adapter) {
        recyclerAlerts.setAdapter(adapter);
    }
    
    @Override
    public void notifyDataSetChanged() {
        if (recyclerAlerts.getAdapter() != null) {
            recyclerAlerts.getAdapter().notifyDataSetChanged();
        }
    }


}