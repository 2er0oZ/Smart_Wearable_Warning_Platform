package com.example.smart_wearable_warning_platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HealthAlert;
import com.example.smart_wearable_warning_platform.adapter.AlertAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    // --- 阈值设置控件 ---
    private EditText etMinThreshold, etMaxThreshold;
    private Button btnSaveThreshold;

    // --- 退出登录控件 ---
    private Button btnLogout; // 新增

    // --- 预警列表控件 ---
    private RecyclerView recyclerViewAlerts;
    private AlertAdapter alertAdapter;

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        dataManager = new DataManager(this);

        // 1. 初始化所有控件
        btnLogout = findViewById(R.id.btn_logout); // 初始化退出按钮

        etMinThreshold = findViewById(R.id.et_min_threshold);
        etMaxThreshold = findViewById(R.id.et_max_threshold);
        btnSaveThreshold = findViewById(R.id.btn_save_threshold);

        recyclerViewAlerts = findViewById(R.id.recycler_view_alerts);
        recyclerViewAlerts.setLayoutManager(new LinearLayoutManager(this));

        // 2. 加载阈值设置
        loadThresholdSettings();

        // 3. 绑定保存按钮事件
        btnSaveThreshold.setOnClickListener(v -> saveThresholdSettings());

        // 4. 绑定退出登录按钮事件 (新增)
        btnLogout.setOnClickListener(v -> performLogout());

        // 5. 加载预警数据
        loadAlerts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlerts();
    }

    /**
     * 退出登录逻辑
     */
    private void performLogout() {
        // 1. 清除登录状态 (使用 commit 确保立即生效)
        dataManager.logout();

        // 2. 提示用户
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();

        // 3. 跳转逻辑
        try {
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            // 必须设置这两个 Flag，清空任务栈，防止用户按返回键回到管理员界面
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "跳转失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private void loadThresholdSettings() {
        int min = dataManager.getMinThreshold();
        int max = dataManager.getMaxThreshold();
        if (min == 0 && max == 0) {
            etMinThreshold.setHint("最低心率 (如 60)");
            etMaxThreshold.setHint("最高心率 (如 100)");
        } else {
            etMinThreshold.setText(String.valueOf(min));
            etMaxThreshold.setText(String.valueOf(max));
        }
    }

    private void saveThresholdSettings() {
        String minStr = etMinThreshold.getText().toString().trim();
        String maxStr = etMaxThreshold.getText().toString().trim();

        if (minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "请输入完整的上限和下限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int min = Integer.parseInt(minStr);
            int max = Integer.parseInt(maxStr);

            if (min >= max) {
                Toast.makeText(this, "错误：最低心率必须小于最高心率", Toast.LENGTH_SHORT).show();
                return;
            }

            dataManager.setMinThreshold(min);
            dataManager.setMaxThreshold(max);
            Toast.makeText(this, "阈值已更新为: " + min + " - " + max, Toast.LENGTH_SHORT).show();
            // 保存后根据新阈值重建预警列表，确保管理员能立即看到基于新阈值的预警变化
            dataManager.rebuildAlertsFromSavedData();
            loadAlerts();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAlerts() {
        List<HealthAlert> alerts = dataManager.getAllAlerts();
        if (alerts == null) {
            alerts = new ArrayList<>();
        }
        if (alerts.isEmpty()) {
            // 仅作提示，不打扰
        }
        alertAdapter = new AlertAdapter(alerts);
        recyclerViewAlerts.setAdapter(alertAdapter);
    }
}
