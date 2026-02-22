package com.example.smart_wearable_warning_platform;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_wearable_warning_platform.fragment.HeartRateFragment;
import com.example.smart_wearable_warning_platform.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_profile:
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment, new ProfileFragment())
                            .commit();
                    return true;
                case R.id.nav_chart:
                default:
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment, new HeartRateFragment())
                            .commit();
                    return true;
            }
        });

        // 默认显示心率页
        bottomNav.setSelectedItemId(R.id.nav_chart);
    }
}
