package com.example.smart_wearable_warning_platform.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smart_wearable_warning_platform.service.HeartRateService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
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
    private HeartRateService heartRateService;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        heartRateService = new HeartRateService();
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

    // 心率阈值 + 步频阈值 + 睡眠时间区间
    public void setStudentThresholdWithSleepTime(String username, int minHr, int maxHr, int minStep, int maxStep, String sleepStart, String sleepEnd) {
        Map<String, StudentThreshold> map = getStudentThresholdsMap();
        StudentThreshold th = new StudentThreshold(minHr, maxHr, minStep, maxStep);
        th.setSleepStartTime(sleepStart);
        th.setSleepEndTime(sleepEnd);
        th.setEnableSleepTimeAlert(true);
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
    
    /**
     * 获取学生预警阈值设置
     */
    public StudentThreshold getStudentThreshold(String username) {
        return getThresholdForUser(username);
    }
    
    /**
     * 获取睡眠时间区间内的心率数据
     */
    public List<HeartRateEntry> getSleepTimeHeartRateData(String username) {
        List<HeartRateEntry> allData = getHeartRateData(username);
        List<HeartRateEntry> sleepData = new ArrayList<>();
        
        if (allData == null || allData.isEmpty()) {
            return sleepData;
        }
        
        StudentThreshold threshold = getThresholdForUser(username);
        
        // 获取睡眠时间区间
        String sleepStartTime = threshold.getSleepStartTime();
        String sleepEndTime = threshold.getSleepEndTime();
        
        if (sleepStartTime == null || sleepEndTime == null) {
            return sleepData; // 未设置睡眠时间
        }
        
        try {
            // 解析睡眠时间区间
            String[] sleepStartParts = sleepStartTime.split(":");
            int sleepStartHour = Integer.parseInt(sleepStartParts[0]);
            int sleepStartMinute = Integer.parseInt(sleepStartParts[1]);
            
            String[] sleepEndParts = sleepEndTime.split(":");
            int sleepEndHour = Integer.parseInt(sleepEndParts[0]);
            int sleepEndMinute = Integer.parseInt(sleepEndParts[1]);
            
            for (HeartRateEntry entry : allData) {
                String timestamp = entry.getTimestamp();
                String[] dateTimeParts = timestamp.split(" ");
                if (dateTimeParts.length < 2) continue;
                
                String[] timeParts = dateTimeParts[1].split(":");
                if (timeParts.length < 2) continue;
                
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                
                // 检查是否在睡眠时间区间内
                boolean isInSleepRange = isInTimeRange(hour, minute, sleepStartHour, sleepStartMinute, sleepEndHour, sleepEndMinute);
                
                if (isInSleepRange) {
                    sleepData.add(entry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return sleepData;
    }

    // --- CSV 数据处理 (核心逻辑) ---

    public List<HeartRateEntry> parseCSV(InputStream inputStream) throws IOException {
        List<HeartRateEntry> rawData = new ArrayList<>();
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
                    rawData.add(entry);

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        reader.close();
        
        // 按分钟聚合数据，取平均值
        List<HeartRateEntry> averagedData = aggregateDataByMinute(rawData);
        
        // 对聚合后的数据进行预警检查
        for (HeartRateEntry entry : averagedData) {
            checkThresholdAndAlert(entry.getBpm(), entry.getStepFrequency(), entry.getTimestamp());
        }
        
        return averagedData;
    }
    
    /**
     * 按分钟聚合数据，取平均值
     * @param rawData 原始数据列表
     * @return 按分钟聚合后的数据列表
     */
    private List<HeartRateEntry> aggregateDataByMinute(List<HeartRateEntry> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用Map按分钟分组，键为分钟级时间戳（格式：yyyy-MM-dd HH:mm）
        Map<String, List<HeartRateEntry>> minuteGroups = new HashMap<>();
        
        for (HeartRateEntry entry : rawData) {
            String timestamp = entry.getTimestamp();
            // 提取分钟级时间戳（去掉秒）
            String minuteTimestamp = timestamp.substring(0, Math.min(16, timestamp.length()));
            
            // 如果该分钟还没有分组，则创建
            if (!minuteGroups.containsKey(minuteTimestamp)) {
                minuteGroups.put(minuteTimestamp, new ArrayList<>());
            }
            
            // 添加到对应分钟的分组
            minuteGroups.get(minuteTimestamp).add(entry);
        }
        
        // 计算每分钟的平均值
        List<HeartRateEntry> averagedData = new ArrayList<>();
        for (Map.Entry<String, List<HeartRateEntry>> entry : minuteGroups.entrySet()) {
            String minuteTimestamp = entry.getKey();
            List<HeartRateEntry> entries = entry.getValue();
            
            if (entries.isEmpty()) {
                continue;
            }
            
            // 计算心率和步频的平均值
            double totalHr = 0;
            double totalStep = 0;
            
            for (HeartRateEntry e : entries) {
                totalHr += e.getBpm();
                totalStep += e.getStepFrequency();
            }
            
            int avgHr = (int) Math.round(totalHr / entries.size());
            int avgStep = (int) Math.round(totalStep / entries.size());
            
            // 创建新的聚合数据条目，使用分钟级时间戳
            HeartRateEntry averagedEntry = new HeartRateEntry(minuteTimestamp + ":00", avgHr, avgStep);
            averagedData.add(averagedEntry);
        }
        
        // 按时间戳排序
        Collections.sort(averagedData, (a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        
        return averagedData;
    }

    private void checkThresholdAndAlert(int bpm, int stepFreq, String timestamp) {
        User currentUser = getCurrentUser();
        if (currentUser != null && "Student".equals(currentUser.getRole())) {
            StudentThreshold th = getThresholdForUser(currentUser.getUsername());
            int minHr = th.getMinHr();
            int maxHr = th.getMaxHr();
            int minStep = th.getMinStep();
            int maxStep = th.getMaxStep();

            // 检查是否在睡眠时间预警区间内
            boolean isInSleepTime = isInSleepTimeRange(timestamp, th);
            
            // 仅当心率 AND 步频同时不在各自区间时触发预警
            boolean hrBad = bpm < minHr || bpm > maxHr;
            boolean stepBad = stepFreq < minStep || stepFreq > maxStep;
            
            // 修改预警条件：在睡眠时间预警区间内，同时检测心率与步频都不符合阈值时才发送预警通知
            if (isInSleepTime && hrBad && stepBad) {
                // 构建带阈值和步频的消息
                String msg;
                if (bpm < minHr) {
                    msg = "睡眠时间心率过低：" + bpm + " bpm（阈值：" + minHr + "）（步频：" + stepFreq + ")";
                } else {
                    msg = "睡眠时间心率过高：" + bpm + " bpm（阈值：" + maxHr + "）（步频：" + stepFreq + ")";
                }
                HealthAlert alert = new HealthAlert(timestamp, msg, currentUser.getUsername());
                alert.setBpm(bpm);
                alert.setStepFreq(stepFreq);
                alert.setStep(true); // 标记为睡眠时间预警
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
        // 先按分钟聚合数据
        List<HeartRateEntry> averagedEntries = aggregateDataByMinute(newEntries);
        
        // 读取时使用 KEY_ALERTS (常量 "alerts_list")
        List<HealthAlert> allAlerts = getAllAlerts();
        
        // 获取学生阈值设置
        StudentThreshold threshold = getThresholdForUser(username);
        if (threshold == null) {
            threshold = new StudentThreshold(); // 使用默认值
        }
        
        // 使用服务层检查并生成预警
        List<HealthAlert> newAlerts = heartRateService.checkAndGenerateAlerts(username, averagedEntries, threshold);
        
        // 删除同一学生、同一时间戳的旧预警（如果有），以便下面重新生成
        for (HealthAlert newAlert : newAlerts) {
            for (int i = allAlerts.size() - 1; i >= 0; --i) {
                HealthAlert existing = allAlerts.get(i);
                if (existing.getStudentName().equals(username) && existing.getTimestamp().equals(newAlert.getTimestamp())) {
                    allAlerts.remove(i);
                }
            }
            allAlerts.add(newAlert);
        }
        
        // 保存预警
        if (!newAlerts.isEmpty()) {
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
            StudentThreshold threshold = getThresholdForUser(username);
            List<HeartRateEntry> data = getHeartRateData(username);
            
            // 使用服务层检查并生成预警
            List<HealthAlert> userAlerts = heartRateService.checkAndGenerateAlerts(username, data, threshold);
            newAlerts.addAll(userAlerts);

        }

        // 存储新的预警列表（覆盖原有）
        saveAlerts(newAlerts);
    }
    
    /**
     * 检查给定时间戳是否在睡眠时间预警区间内
     * @param timestamp 数据时间戳
     * @param threshold 学生阈值设置
     * @return 是否在睡眠时间预警区间内
     */
    private boolean isInSleepTimeRange(String timestamp, StudentThreshold threshold) {
        if (!threshold.isEnableSleepTimeAlert()) {
            return false; // 未启用睡眠时间预警
        }
        
        try {
            // 解析时间戳中的时间部分
            String[] dateTimeParts = timestamp.split(" ");
            if (dateTimeParts.length < 2) {
                return false; // 时间戳格式不正确
            }
            
            String timeStr = dateTimeParts[1]; // 获取时间部分
            String[] timeParts = timeStr.split(":");
            if (timeParts.length < 2) {
                return false; // 时间格式不正确
            }
            
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            // 解析睡眠开始和结束时间
            String[] sleepStartParts = threshold.getSleepStartTime().split(":");
            String[] sleepEndParts = threshold.getSleepEndTime().split(":");
            
            int sleepStartHour = Integer.parseInt(sleepStartParts[0]);
            int sleepStartMinute = Integer.parseInt(sleepStartParts[1]);
            int sleepEndHour = Integer.parseInt(sleepEndParts[0]);
            int sleepEndMinute = Integer.parseInt(sleepEndParts[1]);
            
            // 检查当前时间是否在睡眠时间区间内
            return isInTimeRange(hour, minute, sleepStartHour, sleepStartMinute, sleepEndHour, sleepEndMinute);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查给定时间是否在指定的时间范围内
     * @param hour 当前小时
     * @param minute 当前分钟
     * @param startHour 开始小时
     * @param startMinute 开始分钟
     * @param endHour 结束小时
     * @param endMinute 结束分钟
     * @return 是否在时间范围内
     */
    private boolean isInTimeRange(int hour, int minute, int startHour, int startMinute, int endHour, int endMinute) {
        // 将时间转换为分钟数，便于比较
        int currentMinutes = hour * 60 + minute;
        int startMinutes = startHour * 60 + startMinute;
        int endMinutes = endHour * 60 + endMinute;
        
        // 处理跨日情况（如21:00到次日7:00）
        if (startMinutes > endMinutes) {
            // 时间范围跨日，例如21:00到次日7:00
            // 当前时间要么大于等于开始时间，要么小于等于结束时间
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
        } else {
            // 普通时间范围，不跨日
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
        }
    }
}