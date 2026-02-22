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
    private List<HealthAlert> shownAlerts = new ArrayList<>();
    private int pageSize = 10;

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
        shownAlerts.clear();
        int end = Math.min(pageSize, allAlerts.size());
        for (int i = 0; i < end; i++) shownAlerts.add(allAlerts.get(i));
        adapter = new AlertAdapter(shownAlerts);
        recyclerAlerts.setAdapter(adapter);
        btnLoadMore.setVisibility(allAlerts.size() > shownAlerts.size() ? View.VISIBLE : View.GONE);
    }

    private void loadMore() {
        int current = shownAlerts.size();
        int end = Math.min(current + pageSize, allAlerts.size());
        for (int i = current; i < end; i++) shownAlerts.add(allAlerts.get(i));
        adapter.notifyDataSetChanged();
        btnLoadMore.setVisibility(allAlerts.size() > shownAlerts.size() ? View.VISIBLE : View.GONE);
    }
}
