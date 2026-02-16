package com.example.smart_wearable_warning_platform.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String PREFS_NAME = "HealthAppPrefs";
    private static final String KEY_USERS = "users_list";
    private static final String KEY_ALERTS = "alerts_list";
    private static final String KEY_MIN_HR = "min_hr_threshold"; // 默认50
    private static final String KEY_MAX_HR = "max_hr_threshold"; // 默认100
    private static final String KEY_CURRENT_USER = "current_user";


    private SharedPreferences prefs;
    private Gson gson;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // --- 用户管理 ---

    public void registerUser(User user) {
        List<User> users = getAllUsers();
        for (User u : users) {
            if (u.getUsername().equals(user.getUsername())) {
                return; // 用户已存在
            }
        }
        users.add(user);
        saveUsers(users);
    }

    public User loginUser(String username, String password) {
        List<User> users = getAllUsers();
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                // 保存当前登录用户
                prefs.edit().putString(KEY_CURRENT_USER, gson.toJson(u)).apply();
                return u;
            }
        }
        return null;
    }

    public User getCurrentUser() {
        String userJson = prefs.getString(KEY_CURRENT_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public void logout() {
        prefs.edit().remove(KEY_CURRENT_USER).commit();
    }


    private List<User> getAllUsers() {
        String json = prefs.getString(KEY_USERS, null);
        Type type = new TypeToken<ArrayList<User>>(){}.getType();
        if (json == null) return new ArrayList<>();
        return gson.fromJson(json, type);
    }

    private void saveUsers(List<User> users) {
        String json = gson.toJson(users);
        prefs.edit().putString(KEY_USERS, json).apply();
    }

    // --- 预警与阈值管理 ---

    public int getMinThreshold() {
        return prefs.getInt(KEY_MIN_HR, 50);
    }

    public void setMinThreshold(int value) {
        prefs.edit().putInt(KEY_MIN_HR, value).apply();
    }

    public int getMaxThreshold() {
        return prefs.getInt(KEY_MAX_HR, 100);
    }

    public void setMaxThreshold(int value) {
        prefs.edit().putInt(KEY_MAX_HR, value).apply();
    }

    public void addAlert(HealthAlert alert) {
        List<HealthAlert> alerts = getAllAlerts();
        alerts.add(alert);
        saveAlerts(alerts);
    }

    public List<HealthAlert> getAllAlerts() {
        String json = prefs.getString(KEY_ALERTS, null);
        Type type = new TypeToken<ArrayList<HealthAlert>>(){}.getType();
        if (json == null) return new ArrayList<>();
        return gson.fromJson(json, type);
    }

    private void saveAlerts(List<HealthAlert> alerts) {
        String json = gson.toJson(alerts);
        prefs.edit().putString(KEY_ALERTS, json).apply();
    }

    // --- CSV 数据处理 (核心逻辑) ---

    public List<HeartRateEntry> parseCSV(InputStream inputStream) throws IOException {
        List<HeartRateEntry> data = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        boolean isFirstLine = true;
        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // 跳过标题行: 时间戳,日期时间,心率值
            }

            String[] parts = line.split(",");
            if (parts.length >= 3) {
                try {
                    String timestamp = parts[1]; // 取日期时间作为X轴
                    int bpm = Integer.parseInt(parts[2].trim());

                    HeartRateEntry entry = new HeartRateEntry(timestamp, bpm);
                    data.add(entry);

                    // 实时预警检查逻辑
                    checkThresholdAndAlert(bpm, timestamp);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        reader.close();
        return data;
    }

    private void checkThresholdAndAlert(int bpm, String timestamp) {
        User currentUser = getCurrentUser();
        if (currentUser != null && "Student".equals(currentUser.getRole())) {
            int min = getMinThreshold();
            int max = getMaxThreshold();

            if (bpm < min) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "心率过低预警: " + bpm + " bpm (阈值: " + min + ")",
                        currentUser.getUsername());
                addAlert(alert);
            } else if (bpm > max) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "心率过高预警: " + bpm + " bpm (阈值: " + max + ")",
                        currentUser.getUsername());
                addAlert(alert);
            }
        }
    }
    // 保存心率数据 (关联到具体的用户名)
    public void saveHeartRateData(String username, List<HeartRateEntry> data) {
        String key = "hr_data_" + username;
        String json = gson.toJson(data);
        prefs.edit().putString(key, json).apply();
    }

    // 读取心率数据
    public List<HeartRateEntry> getHeartRateData(String username) {
        String key = "hr_data_" + username;
        String json = prefs.getString(key, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<HeartRateEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * 检查用户名是否已经存在
     */
    public boolean isUserExists(String username) {
        List<User> users = getAllUsers();
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return true; // 用户名已存在
            }
        }
        return false; // 用户名不存在
    }
    /**
     * 检查新导入的数据，如果超出阈值，则生成预警记录
     */
    public void checkAndGenerateAlerts(String username, List<HeartRateEntry> newEntries) {
        // 读取时使用 KEY_ALERTS (常量 "alerts_list")
        List<HealthAlert> allAlerts = getAllAlerts();
        int minThreshold = getMinThreshold();
        int maxThreshold = getMaxThreshold();

        boolean hasNewAlerts = false;

        for (HeartRateEntry entry : newEntries) {
            int bpm = entry.getBpm();
            String timestamp = entry.getTimestamp();

            if (bpm < minThreshold || bpm > maxThreshold) {
                boolean isDuplicate = false;
                for (HealthAlert existing : allAlerts) {
                    if (existing.getStudentName().equals(username) && existing.getTimestamp().equals(timestamp)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    HealthAlert alert = new HealthAlert();
                    alert.setStudentName(username);
                    alert.setTimestamp(timestamp);
                    alert.setBpm(bpm);

                    String type = (bpm < minThreshold) ? "心率过低" : "心率过高";
                    alert.setMessage(type);

                    allAlerts.add(alert);
                    hasNewAlerts = true;
                }
            }
        }

        if (hasNewAlerts) {
            // --- 修复点 2 ---
            // 错误写法: prefs.edit().putString("all_alerts", json).apply();
            // 正确写法: 调用 saveAlerts 方法 (内部使用的是 KEY_ALERTS)，或者手动使用 KEY_ALERTS

            // 建议直接复用已有的方法，避免再次拼写错误
            saveAlerts(allAlerts);
        }
    }




}
