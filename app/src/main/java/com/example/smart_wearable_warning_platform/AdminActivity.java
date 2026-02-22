package com.example.smart_wearable_warning_platform;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_wearable_warning_platform.fragment.AlertsNotificationFragment;
import com.example.smart_wearable_warning_platform.fragment.ProfileFragment;
import com.example.smart_wearable_warning_platform.fragment.StudentManagementFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
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
}
