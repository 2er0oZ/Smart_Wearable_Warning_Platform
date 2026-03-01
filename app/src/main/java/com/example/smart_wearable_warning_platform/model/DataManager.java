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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static final String PREFS_NAME = "HealthAppPrefs";
    private static final String KEY_USERS = "users_list";
    private static final String KEY_ALERTS = "alerts_list";
    private static final String KEY_MIN_HR = "min_hr_threshold"; // 默认50
    private static final String KEY_MAX_HR = "max_hr_threshold"; // 默认100
    private static final String KEY_STUDENT_THRESHOLDS = "student_thresholds"; // 存储每个学生的阈值映射
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


    public List<User> getAllUsers() {
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
        List<HealthAlert> list = gson.fromJson(json, type);
        // 回填：如果之前的记录没有 bpm 字段逻辑，尝试从 message 中解析
        for (HealthAlert alert : list) {
            if (alert.getBpm() == 0) {
                String msg = alert.getMessage();
                if (msg != null) {
                    // 提取第一个数字
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(msg);
                    if (m.find()) {
                        try {
                            int parsed = Integer.parseInt(m.group(1));
                            alert.setBpm(parsed);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return list;
    }

    private void saveAlerts(List<HealthAlert> alerts) {
        String json = gson.toJson(alerts);
        prefs.edit().putString(KEY_ALERTS, json).apply();
    }

    // --- 每个学生的阈值管理 (以 username 为 key 存储) ---
    private Map<String, StudentThreshold> getStudentThresholdsMap() {
        String json = prefs.getString(KEY_STUDENT_THRESHOLDS, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<HashMap<String, StudentThreshold>>(){}.getType();
        Map<String, StudentThreshold> map = gson.fromJson(json, type);
        if (map == null) return new HashMap<>();
        return map;
    }

    private void saveStudentThresholdsMap(Map<String, StudentThreshold> map) {
        String json = gson.toJson(map);
        prefs.edit().putString(KEY_STUDENT_THRESHOLDS, json).apply();
    }

    // 心率阈值 + 步频阈值四个参数
    public void setStudentThreshold(String username, int minHr, int maxHr, int minStep, int maxStep) {
        Map<String, StudentThreshold> map = getStudentThresholdsMap();
        StudentThreshold th = new StudentThreshold(minHr, maxHr, minStep, maxStep);
        map.put(username, th);
        saveStudentThresholdsMap(map);
    }

    // 兼容老调用，只设置心率阈值，步频使用默认 0-5
    public void setStudentThreshold(String username, int minHr, int maxHr) {
        setStudentThreshold(username, minHr, maxHr, 0, 5);
    }

    /**
     * 返回指定用户的阈值数组：{min, max}. 如果该用户没有单独设置，返回全局阈值。
     */
    public StudentThreshold getThresholdForUser(String username) {
        Map<String, StudentThreshold> map = getStudentThresholdsMap();
        if (map.containsKey(username)) {
            return map.get(username);
        }
        // 全局阈值（心率） + 默认步频
        return new StudentThreshold(getMinThreshold(), getMaxThreshold());
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
                continue; // 跳过标题行: 时间戳,日期时间,心率值,步频（可选）
            }

            String[] parts = line.split(",");
            if (parts.length >= 3) {
                try {
                    String timestamp = parts[1]; // 取日期时间作为X轴
                    int bpm = Integer.parseInt(parts[2].trim());
                    int stepFreq = 0;
                    if (parts.length >= 4) {
                        try {
                            stepFreq = Integer.parseInt(parts[3].trim());
                        } catch (NumberFormatException ignored) {
                            // 如果步频不是数字，保持为0
                        }
                    }
                    if (stepFreq < 0) {
                        stepFreq = 0; // 步频不允许为负
                    }

                    HeartRateEntry entry = new HeartRateEntry(timestamp, bpm, stepFreq);
                    data.add(entry);

                    // 实时预警检查逻辑
                    checkThresholdAndAlert(bpm, stepFreq, timestamp);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        reader.close();
        return data;
    }

    private void checkThresholdAndAlert(int bpm, int stepFreq, String timestamp) {
        User currentUser = getCurrentUser();
        if (currentUser != null && "Student".equals(currentUser.getRole())) {
            StudentThreshold th = getThresholdForUser(currentUser.getUsername());
            int minHr = th.getMinHr();
            int maxHr = th.getMaxHr();
            int minStep = th.getMinStep();
            int maxStep = th.getMaxStep();

            if (bpm < minHr) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "心率过低预警: " + bpm + " bpm (阈值: " + minHr + ")",
                        currentUser.getUsername());
                alert.setBpm(bpm);
                alert.setStepFreq(stepFreq);
                addAlert(alert);
            } else if (bpm > maxHr) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "心率过高预警: " + bpm + " bpm (阈值: " + maxHr + ")",
                        currentUser.getUsername());
                alert.setBpm(bpm);
                alert.setStepFreq(stepFreq);
                addAlert(alert);
            }

            // 步频检查
            if (stepFreq < minStep) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "步频过低预警: " + stepFreq + " (阈值: " + minStep + ")",
                        currentUser.getUsername());
                alert.setStepFreq(stepFreq);
                alert.setStep(true);
                addAlert(alert);
            } else if (stepFreq > maxStep) {
                HealthAlert alert = new HealthAlert(timestamp,
                        "步频过高预警: " + stepFreq + " (阈值: " + maxStep + ")",
                        currentUser.getUsername());
                alert.setStepFreq(stepFreq);
                alert.setStep(true);
                addAlert(alert);
            }
        }
    }
    // 保存心率数据 (关联到具体的用户名)
    // 将新数据合并到已有数据中：同一时间戳使用新条目，最终按时间排序
    public void saveHeartRateData(String username, List<HeartRateEntry> data) {
        String key = "hr_data_" + username;
        List<HeartRateEntry> existing = getHeartRateData(username);
        // 使用 LinkedHashMap 保持插入顺序（时间顺序会在最后排序）
        java.util.Map<String, HeartRateEntry> map = new java.util.LinkedHashMap<>();
        for (HeartRateEntry e : existing) {
            map.put(e.getTimestamp(), e);
        }
        for (HeartRateEntry e : data) {
            // 新数据覆盖旧数据
            map.put(e.getTimestamp(), e);
        }
        List<HeartRateEntry> merged = new java.util.ArrayList<>(map.values());
        // 按时间戳升序
        java.util.Collections.sort(merged, (a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        String json = gson.toJson(merged);
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
        // 对于指定的 username，优先使用该学生自定义阈值
        StudentThreshold thForUser = getThresholdForUser(username);
        int minThreshold = thForUser.getMinHr();
        int maxThreshold = thForUser.getMaxHr();
        int minStep = thForUser.getMinStep();
        int maxStep = thForUser.getMaxStep();

        boolean hasNewAlerts = false;

        for (HeartRateEntry entry : newEntries) {
            int bpm = entry.getBpm();
            int stepFreq = entry.getStepFrequency();
            String timestamp = entry.getTimestamp();

            // 先删除同一学生、同一时间戳的旧预警（如果有），以便下面重新生成
            for (int i = allAlerts.size() - 1; i >= 0; --i) {
                HealthAlert existing = allAlerts.get(i);
                if (existing.getStudentName().equals(username) && existing.getTimestamp().equals(timestamp)) {
                    allAlerts.remove(i);
                }
            }

            if (bpm < minThreshold || bpm > maxThreshold) {
                HealthAlert alert = new HealthAlert();
                alert.setStudentName(username);
                alert.setTimestamp(timestamp);
                alert.setBpm(bpm);
                alert.setStepFreq(stepFreq);
                String type = (bpm < minThreshold) ? "心率过低" : "心率过高";
                alert.setMessage(type);
                allAlerts.add(alert);
                hasNewAlerts = true;
            }
            // 步频阈值
            if (stepFreq < minStep || stepFreq > maxStep) {
                HealthAlert alert = new HealthAlert();
                alert.setStudentName(username);
                alert.setTimestamp(timestamp);
                // 保留心率供参考
                alert.setBpm(bpm);
                alert.setStepFreq(stepFreq);
                alert.setStep(true);
                String type = (stepFreq < minStep) ? "步频过低" : "步频过高";
                alert.setMessage(type);
                allAlerts.add(alert);
                hasNewAlerts = true;
            }
        }

        if (hasNewAlerts) {
            saveAlerts(allAlerts);
        }
    }

    /**
     * 根据已保存的所有学生心率数据，使用当前阈值重建全部预警列表。
     * 该方法会覆盖原有的预警数据，适用于管理员更新阈值后重新计算所有历史预警。
     */
    public void rebuildAlertsFromSavedData() {
        List<HealthAlert> newAlerts = new ArrayList<>();
        List<User> users = getAllUsers();
        for (User u : users) {
            String username = u.getUsername();
            StudentThreshold th = getThresholdForUser(username);
            int minThresholdForUser = th.getMinHr();
            int maxThresholdForUser = th.getMaxHr();
            int minStepForUser = th.getMinStep();
            int maxStepForUser = th.getMaxStep();
            List<HeartRateEntry> data = getHeartRateData(username);
            for (HeartRateEntry entry : data) {
                int bpm = entry.getBpm();
                int stepFreq = entry.getStepFrequency();
                String timestamp = entry.getTimestamp();
                if (bpm < minThresholdForUser || bpm > maxThresholdForUser) {
                    HealthAlert alert = new HealthAlert();
                    alert.setStudentName(username);
                    alert.setTimestamp(timestamp);
                    alert.setBpm(bpm);
                    alert.setStepFreq(stepFreq);
                    String type = (bpm < minThresholdForUser) ? "心率过低" : "心率过高";
                    alert.setMessage(type + ": " + bpm + " bpm (阈值: " + (bpm < minThresholdForUser ? minThresholdForUser : maxThresholdForUser) + ")");
                    newAlerts.add(alert);
                }
                if (stepFreq < minStepForUser || stepFreq > maxStepForUser) {
                    HealthAlert alert = new HealthAlert();
                    alert.setStudentName(username);
                    alert.setTimestamp(timestamp);
                    alert.setStepFreq(stepFreq);
                    alert.setStep(true);
                    String type = (stepFreq < minStepForUser) ? "步频过低" : "步频过高";
                    alert.setMessage(type + ": " + stepFreq + " (阈值: " + (stepFreq < minStepForUser ? minStepForUser : maxStepForUser) + ")");
                    newAlerts.add(alert);
                }
            }
        }

        // 存储新的预警列表（覆盖原有）
        saveAlerts(newAlerts);
    }




}
