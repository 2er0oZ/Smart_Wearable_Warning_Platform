package com.example.smart_wearable_warning_platform.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smart_wearable_warning_platform.model.HeartRateEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 心率数据访问对象
 */
public class HeartRateEntryDAO {
    
    private DatabaseHelper dbHelper;
    
    public HeartRateEntryDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }
    
    /**
     * 插入单条心率数据
     * @param entry 心率数据对象
     * @return 插入的行ID，-1表示失败
     */
    public long insertHeartRateEntry(HeartRateEntry entry, String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, entry.getTimestamp());
        values.put(DatabaseHelper.COLUMN_BPM, entry.getBpm());
        values.put(DatabaseHelper.COLUMN_STEP_FREQUENCY, entry.getStepFrequency());
        values.put(DatabaseHelper.COLUMN_USERNAME_FK, username);
        
        long result = db.insert(DatabaseHelper.TABLE_HEART_RATE, null, values);
        db.close();
        return result;
    }
    
    /**
     * 批量插入心率数据
     * @param entries 心率数据列表
     * @param username 用户名
     * @return 成功插入的记录数
     */
    public int insertHeartRateEntries(List<HeartRateEntry> entries, String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int successCount = 0;
        
        db.beginTransaction();
        try {
            for (HeartRateEntry entry : entries) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_TIMESTAMP, entry.getTimestamp());
                values.put(DatabaseHelper.COLUMN_BPM, entry.getBpm());
                values.put(DatabaseHelper.COLUMN_STEP_FREQUENCY, entry.getStepFrequency());
                values.put(DatabaseHelper.COLUMN_USERNAME_FK, username);
                
                long result = db.insert(DatabaseHelper.TABLE_HEART_RATE, null, values);
                if (result != -1) {
                    successCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        
        return successCount;
    }
    
    /**
     * 根据用户名查询所有心率数据
     * @param username 用户名
     * @return 心率数据列表
     */
    public List<HeartRateEntry> getHeartRateData(String username) {
        List<HeartRateEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_HEART_RATE,
                new String[]{DatabaseHelper.COLUMN_TIMESTAMP, DatabaseHelper.COLUMN_BPM, DatabaseHelper.COLUMN_STEP_FREQUENCY},
                DatabaseHelper.COLUMN_USERNAME_FK + " = ?",
                new String[]{username},
                null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HeartRateEntry entry = new HeartRateEntry(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BPM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQUENCY))
                );
                entries.add(entry);
            }
            cursor.close();
        }
        
        db.close();
        return entries;
    }
    
    /**
     * 根据时间范围查询心率数据
     * @param username 用户名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 心率数据列表
     */
    public List<HeartRateEntry> getHeartRateDataByTimeRange(String username, String startTime, String endTime) {
        List<HeartRateEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_HEART_RATE,
                new String[]{DatabaseHelper.COLUMN_TIMESTAMP, DatabaseHelper.COLUMN_BPM, DatabaseHelper.COLUMN_STEP_FREQUENCY},
                DatabaseHelper.COLUMN_USERNAME_FK + " = ? AND " + DatabaseHelper.COLUMN_TIMESTAMP + " BETWEEN ? AND ?",
                new String[]{username, startTime, endTime},
                null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HeartRateEntry entry = new HeartRateEntry(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BPM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQUENCY))
                );
                entries.add(entry);
            }
            cursor.close();
        }
        
        db.close();
        return entries;
    }
    
    /**
     * 获取用户的心率数据数量
     * @param username 用户名
     * @return 数据数量
     */
    public int getHeartRateCount(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_HEART_RATE +
                " WHERE " + DatabaseHelper.COLUMN_USERNAME_FK + " = ?",
                new String[]{username}
        );
        
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 删除用户的所有心率数据
     * @param username 用户名
     * @return 删除的行数
     */
    public int deleteHeartRateData(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_HEART_RATE,
                DatabaseHelper.COLUMN_USERNAME_FK + " = ?",
                new String[]{username}
        );
        db.close();
        return result;
    }
    
    /**
     * 根据时间戳删除心率数据
     * @param username 用户名
     * @param timestamp 时间戳
     * @return 删除的行数
     */
    public int deleteHeartRateEntry(String username, String timestamp) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_HEART_RATE,
                DatabaseHelper.COLUMN_USERNAME_FK + " = ? AND " + DatabaseHelper.COLUMN_TIMESTAMP + " = ?",
                new String[]{username, timestamp}
        );
        db.close();
        return result;
    }
    
    /**
     * 获取最新的心率数据
     * @param username 用户名
     * @param limit 限制数量
     * @return 心率数据列表
     */
    public List<HeartRateEntry> getLatestHeartRateData(String username, int limit) {
        List<HeartRateEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_HEART_RATE,
                new String[]{DatabaseHelper.COLUMN_TIMESTAMP, DatabaseHelper.COLUMN_BPM, DatabaseHelper.COLUMN_STEP_FREQUENCY},
                DatabaseHelper.COLUMN_USERNAME_FK + " = ?",
                new String[]{username},
                null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " DESC",
                String.valueOf(limit)
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HeartRateEntry entry = new HeartRateEntry(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BPM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQUENCY))
                );
                entries.add(entry);
            }
            cursor.close();
        }
        
        db.close();
        return entries;
    }
}