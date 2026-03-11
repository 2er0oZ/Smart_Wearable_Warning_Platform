package com.example.smart_wearable_warning_platform.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smart_wearable_warning_platform.model.StudentThreshold;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生阈值数据访问对象
 */
public class StudentThresholdDAO {
    
    private DatabaseHelper dbHelper;
    
    public StudentThresholdDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }
    
    /**
     * 插入学生阈值
     * @param username 用户名
     * @param threshold 阈值对象
     * @return 插入的行ID，-1表示失败
     */
    public long insertThreshold(String username, StudentThreshold threshold) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_THRESHOLD_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_MIN_HR, threshold.getMinHr());
        values.put(DatabaseHelper.COLUMN_MAX_HR, threshold.getMaxHr());
        values.put(DatabaseHelper.COLUMN_MIN_STEP, threshold.getMinStep());
        values.put(DatabaseHelper.COLUMN_MAX_STEP, threshold.getMaxStep());
        values.put(DatabaseHelper.COLUMN_SLEEP_START_TIME, threshold.getSleepStartTime());
        values.put(DatabaseHelper.COLUMN_SLEEP_END_TIME, threshold.getSleepEndTime());
        values.put(DatabaseHelper.COLUMN_ENABLE_SLEEP_ALERT, threshold.isEnableSleepTimeAlert() ? 1 : 0);
        
        long result = db.insert(DatabaseHelper.TABLE_THRESHOLDS, null, values);
        db.close();
        return result;
    }
    
    /**
     * 更新学生阈值
     * @param username 用户名
     * @param threshold 阈值对象
     * @return 更新的行数
     */
    public int updateThreshold(String username, StudentThreshold threshold) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_MIN_HR, threshold.getMinHr());
        values.put(DatabaseHelper.COLUMN_MAX_HR, threshold.getMaxHr());
        values.put(DatabaseHelper.COLUMN_MIN_STEP, threshold.getMinStep());
        values.put(DatabaseHelper.COLUMN_MAX_STEP, threshold.getMaxStep());
        values.put(DatabaseHelper.COLUMN_SLEEP_START_TIME, threshold.getSleepStartTime());
        values.put(DatabaseHelper.COLUMN_SLEEP_END_TIME, threshold.getSleepEndTime());
        values.put(DatabaseHelper.COLUMN_ENABLE_SLEEP_ALERT, threshold.isEnableSleepTimeAlert() ? 1 : 0);
        
        int result = db.update(
                DatabaseHelper.TABLE_THRESHOLDS,
                values,
                DatabaseHelper.COLUMN_THRESHOLD_USERNAME + " = ?",
                new String[]{username}
        );
        
        db.close();
        return result;
    }
    
    /**
     * 插入或更新学生阈值
     * @param username 用户名
     * @param threshold 阈值对象
     * @return 是否成功
     */
    public boolean insertOrUpdateThreshold(String username, StudentThreshold threshold) {
        StudentThreshold existing = getThreshold(username);
        if (existing != null) {
            return updateThreshold(username, threshold) > 0;
        } else {
            return insertThreshold(username, threshold) != -1;
        }
    }
    
    /**
     * 根据用户名查询阈值
     * @param username 用户名
     * @return 阈值对象，不存在返回null
     */
    public StudentThreshold getThreshold(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StudentThreshold threshold = null;
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_THRESHOLDS,
                null,
                DatabaseHelper.COLUMN_THRESHOLD_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            threshold = new StudentThreshold();
            threshold.setMinHr(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIN_HR)));
            threshold.setMaxHr(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MAX_HR)));
            threshold.setMinStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIN_STEP)));
            threshold.setMaxStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MAX_STEP)));
            threshold.setSleepStartTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SLEEP_START_TIME)));
            threshold.setSleepEndTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SLEEP_END_TIME)));
            threshold.setEnableSleepTimeAlert(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ENABLE_SLEEP_ALERT)) == 1);
            cursor.close();
        }
        
        db.close();
        return threshold;
    }
    
    /**
     * 查询所有学生阈值
     * @return 阈值列表
     */
    public List<StudentThreshold> getAllThresholds() {
        List<StudentThreshold> thresholds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_THRESHOLDS,
                null,
                null, null, null, null, null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                StudentThreshold threshold = new StudentThreshold();
                threshold.setMinHr(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIN_HR)));
                threshold.setMaxHr(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MAX_HR)));
                threshold.setMinStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIN_STEP)));
                threshold.setMaxStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MAX_STEP)));
                threshold.setSleepStartTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SLEEP_START_TIME)));
                threshold.setSleepEndTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SLEEP_END_TIME)));
                threshold.setEnableSleepTimeAlert(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ENABLE_SLEEP_ALERT)) == 1);
                thresholds.add(threshold);
            }
            cursor.close();
        }
        
        db.close();
        return thresholds;
    }
    
    /**
     * 删除学生阈值
     * @param username 用户名
     * @return 删除的行数
     */
    public int deleteThreshold(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_THRESHOLDS,
                DatabaseHelper.COLUMN_THRESHOLD_USERNAME + " = ?",
                new String[]{username}
        );
        db.close();
        return result;
    }
    
    /**
     * 删除所有阈值
     * @return 删除的行数
     */
    public int deleteAllThresholds() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(DatabaseHelper.TABLE_THRESHOLDS, null, null);
        db.close();
        return result;
    }
    
    /**
     * 设置心率阈值
     * @param username 用户名
     * @param minHr 最小心率
     * @param maxHr 最大心率
     * @return 是否成功
     */
    public boolean setHeartRateThreshold(String username, int minHr, int maxHr) {
        StudentThreshold threshold = getThreshold(username);
        if (threshold == null) {
            threshold = new StudentThreshold(minHr, maxHr);
            return insertThreshold(username, threshold) != -1;
        } else {
            threshold.setMinHr(minHr);
            threshold.setMaxHr(maxHr);
            return updateThreshold(username, threshold) > 0;
        }
    }
    
    /**
     * 设置步频阈值
     * @param username 用户名
     * @param minStep 最小步频
     * @param maxStep 最大步频
     * @return 是否成功
     */
    public boolean setStepFrequencyThreshold(String username, int minStep, int maxStep) {
        StudentThreshold threshold = getThreshold(username);
        if (threshold == null) {
            threshold = new StudentThreshold();
            threshold.setMinStep(minStep);
            threshold.setMaxStep(maxStep);
            return insertThreshold(username, threshold) != -1;
        } else {
            threshold.setMinStep(minStep);
            threshold.setMaxStep(maxStep);
            return updateThreshold(username, threshold) > 0;
        }
    }
    
    /**
     * 设置睡眠时间区间
     * @param username 用户名
     * @param sleepStartTime 睡眠开始时间
     * @param sleepEndTime 睡眠结束时间
     * @param enableSleepAlert 是否启用睡眠预警
     * @return 是否成功
     */
    public boolean setSleepTimeRange(String username, String sleepStartTime, String sleepEndTime, boolean enableSleepAlert) {
        StudentThreshold threshold = getThreshold(username);
        if (threshold == null) {
            threshold = new StudentThreshold();
            threshold.setSleepStartTime(sleepStartTime);
            threshold.setSleepEndTime(sleepEndTime);
            threshold.setEnableSleepTimeAlert(enableSleepAlert);
            return insertThreshold(username, threshold) != -1;
        } else {
            threshold.setSleepStartTime(sleepStartTime);
            threshold.setSleepEndTime(sleepEndTime);
            threshold.setEnableSleepTimeAlert(enableSleepAlert);
            return updateThreshold(username, threshold) > 0;
        }
    }
    
    /**
     * 获取阈值数量
     * @return 阈值总数
     */
    public int getThresholdCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_THRESHOLDS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
}