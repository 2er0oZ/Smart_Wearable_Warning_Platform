package com.example.smart_wearable_warning_platform;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_wearable_warning_platform.fragment.AlertsNotificationFragment;
import com.example.smart_wearable_warning_platform.fragment.ProfileFragment;
import com.example.smart_wearable_warning_platform.fragment.StudentManagementFragment;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;
import com.example.smart_wearable_warning_platform.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

public class AdminActivity extends AppCompatActivity {
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        
        dataManager = new DataManager(this);

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_students:
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.admin_nav_host_fragment, new StudentManagementFragment())
                            .commit();
                    return true;
                case R.id.nav_profile:
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.admin_nav_host_fragment, new ProfileFragment())
                            .commit();
                    return true;
                case R.id.nav_alerts:
                default:
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.admin_nav_host_fragment, new AlertsNotificationFragment())
                            .commit();
                    return true;
            }
        });

        // 默认显示预警通知
        bottomNav.setSelectedItemId(R.id.nav_alerts);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 添加统一设置睡眠时间的菜单项
        MenuItem sleepTimeItem = menu.add(Menu.NONE, Menu.NONE, 1, "统一睡眠时间设置");
        sleepTimeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理统一睡眠时间设置菜单项点击
        if (item.getTitle().equals("统一睡眠时间设置")) {
            showUnifiedSleepTimeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 显示统一睡眠时间设置对话框
     */
    private void showUnifiedSleepTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("统一设置学生睡眠时间预警区间");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        EditText etSleepStart = new EditText(this);
        etSleepStart.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etSleepStart.setHint("睡眠开始时间 (如: 21:00)");
        etSleepStart.setText("21:00");
        
        EditText etSleepEnd = new EditText(this);
        etSleepEnd.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etSleepEnd.setHint("睡眠结束时间 (如: 07:00)");
        etSleepEnd.setText("07:00");
        
        layout.addView(etSleepStart);
        layout.addView(etSleepEnd);
        
        builder.setView(layout);
        
        builder.setPositiveButton("应用到所有学生", (dialog, which) -> {
            String sleepStart = etSleepStart.getText().toString().trim();
            String sleepEnd = etSleepEnd.getText().toString().trim();
            
            // 验证时间格式
            if (!isValidTimeFormat(sleepStart) || !isValidTimeFormat(sleepEnd)) {
                Toast.makeText(this, "请输入有效的时间格式 (HH:MM)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 应用到所有学生
            applySleepTimeToAllStudents(sleepStart, sleepEnd);
            
            Toast.makeText(this, "睡眠时间预警区间已应用到所有学生", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        
        builder.show();
    }
    
    /**
     * 应用睡眠时间设置到所有学生
     */
    private void applySleepTimeToAllStudents(String sleepStart, String sleepEnd) {
        // 获取所有学生用户
        for (User user : dataManager.getAllUsers()) {
            if ("Student".equals(user.getRole())) {
                // 获取当前学生的阈值
                StudentThreshold threshold = dataManager.getThresholdForUser(user.getUsername());
                // 更新睡眠时间
                threshold.setSleepStartTime(sleepStart);
                threshold.setSleepEndTime(sleepEnd);
                threshold.setEnableSleepTimeAlert(true);
                // 保存更新后的阈值
                dataManager.setStudentThresholdWithSleepTime(
                    user.getUsername(), 
                    threshold.getMinHr(), 
                    threshold.getMaxHr(), 
                    threshold.getMinStep(), 
                    threshold.getMaxStep(), 
                    sleepStart, 
                    sleepEnd
                );
            }
        }
        
        // 重新构建预警数据以应用新的睡眠时间设置
        dataManager.rebuildAlertsFromSavedData();
    }
    
    /**
     * 验证时间格式是否为 HH:MM
     */
    private boolean isValidTimeFormat(String time) {
        if (time == null || !time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            return false;
        }
        return true;
    }
}