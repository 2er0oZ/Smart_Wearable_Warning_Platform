package com.example.smart_wearable_warning_platform.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smart_wearable_warning_platform.model.HealthAlert;

import java.util.ArrayList;
import java.util.List;

/**
 * 健康预警数据访问对象
 */
public class HealthAlertDAO {
    
    private DatabaseHelper dbHelper;
    
    public HealthAlertDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }
    
    /**
     * 插入预警记录
     * @param alert 预警对象
     * @return 插入的行ID，-1表示失败
     */
    public long insertAlert(HealthAlert alert) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ALERT_TIMESTAMP, alert.getTimestamp());
        values.put(DatabaseHelper.COLUMN_MESSAGE, alert.getMessage());
        values.put(DatabaseHelper.COLUMN_STUDENT_NAME, alert.getStudentName());
        values.put(DatabaseHelper.COLUMN_STUDENT_ID, alert.getStudentId());
        values.put(DatabaseHelper.COLUMN_ALERT_BPM, alert.getBpm());
        values.put(DatabaseHelper.COLUMN_STEP_FREQ, alert.getStepFreq());
        values.put(DatabaseHelper.COLUMN_IS_STEP, alert.isStep() ?1 : 0);
        values.put(DatabaseHelper.COLUMN_IS_EXPANDED, alert.isExpanded() ?1 : 0);
        
        long result = db.insert(DatabaseHelper.TABLE_ALERTS, null, values);
        db.close();
        return result;
    }
    
    /**
     * 批量插入预警记录
     * @param alerts 预警列表
     * @return 成功插入的记录数
     */
    public int insertAlerts(List<HealthAlert> alerts) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int successCount = 0;
        
        db.beginTransaction();
        try {
            for (HealthAlert alert : alerts) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_ALERT_TIMESTAMP, alert.getTimestamp());
                values.put(DatabaseHelper.COLUMN_MESSAGE, alert.getMessage());
                values.put(DatabaseHelper.COLUMN_STUDENT_NAME, alert.getStudentName());
                values.put(DatabaseHelper.COLUMN_STUDENT_ID, alert.getStudentId());
                values.put(DatabaseHelper.COLUMN_ALERT_BPM, alert.getBpm());
                values.put(DatabaseHelper.COLUMN_STEP_FREQ, alert.getStepFreq());
                values.put(DatabaseHelper.COLUMN_IS_STEP, alert.isStep() ?1 : 0);
                values.put(DatabaseHelper.COLUMN_IS_EXPANDED, alert.isExpanded() ?1 : 0);
                
                long result = db.insert(DatabaseHelper.TABLE_ALERTS, null, values);
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
     * 查询所有预警记录
     * @return 预警列表
     */
    public List<HealthAlert> getAllAlerts() {
        List<HealthAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ALERTS,
                null,
                null, null, null, null,
                DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " DESC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HealthAlert alert = new HealthAlert();
                alert.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_TIMESTAMP)));
                alert.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE)));
                alert.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_NAME)));
                alert.setStudentId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_ID)));
                alert.setBpm(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_BPM)));
                alert.setStepFreq(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQ)));
                alert.setStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_STEP)) ==1);
                alert.setExpanded(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_EXPANDED)) ==1);
                alerts.add(alert);
            }
            cursor.close();
        }
        
        db.close();
        return alerts;
    }
    
    /**
     * 根据学生名查询预警记录
     * @param studentName 学生名
     * @return 预警列表
     */
    public List<HealthAlert> getAlertsByStudent(String studentName) {
        List<HealthAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ALERTS,
                null,
                DatabaseHelper.COLUMN_STUDENT_NAME + " = ?",
                new String[]{studentName},
                null, null,
                DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " DESC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HealthAlert alert = new HealthAlert();
                alert.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_TIMESTAMP)));
                alert.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE)));
                alert.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_NAME)));
                alert.setStudentId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_ID)));
                alert.setBpm(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_BPM)));
                alert.setStepFreq(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQ)));
                alert.setStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_STEP)) ==1);
                alert.setExpanded(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_EXPANDED)) ==1);
                alerts.add(alert);
            }
            cursor.close();
        }
        
        db.close();
        return alerts;
    }
    
    /**
     * 根据时间范围查询预警记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 预警列表
     */
    public List<HealthAlert> getAlertsByTimeRange(String startTime, String endTime) {
        List<HealthAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ALERTS,
                null,
                DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " BETWEEN ? AND ?",
                new String[]{startTime, endTime},
                null, null,
                DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " DESC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HealthAlert alert = new HealthAlert();
                alert.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_TIMESTAMP)));
                alert.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE)));
                alert.setStudentName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_NAME)));
                alert.setStudentId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_ID)));
                alert.setBpm(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ALERT_BPM)));
                alert.setStepFreq(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STEP_FREQ)));
                alert.setStep(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_STEP)) ==1);
                alert.setExpanded(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_EXPANDED)) ==1);
                alerts.add(alert);
            }
            cursor.close();
        }
        
        db.close();
        return alerts;
    }
    
    /**
     * 删除所有预警记录
     * @return 删除的行数
     */
    public int deleteAllAlerts() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(DatabaseHelper.TABLE_ALERTS, null, null);
        db.close();
        return result;
    }
    
    /**
     * 根据学生名删除预警记录
     * @param studentName 学生名
     * @return 删除的行数
     */
    public int deleteAlertsByStudent(String studentName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_ALERTS,
                DatabaseHelper.COLUMN_STUDENT_NAME + " = ?",
                new String[]{studentName}
        );
        db.close();
        return result;
    }
    
    /**
     * 根据时间戳和学生名删除预警记录
     * @param studentName 学生名
     * @param timestamp 时间戳
     * @return 删除的行数
     */
    public int deleteAlert(String studentName, String timestamp) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_ALERTS,
                DatabaseHelper.COLUMN_STUDENT_NAME + " = ? AND " + DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " = ?",
                new String[]{studentName, timestamp}
        );
        db.close();
        return result;
    }
    
    /**
     * 更新预警记录的展开状态
     * @param studentName 学生名
     * @param timestamp 时间戳
     * @param expanded 展开状态
     * @return 更新的行数
     */
    public int updateAlertExpanded(String studentName, String timestamp, boolean expanded) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IS_EXPANDED, expanded ?1 : 0);
        
        int result = db.update(
                DatabaseHelper.TABLE_ALERTS,
                values,
                DatabaseHelper.COLUMN_STUDENT_NAME + " = ? AND " + DatabaseHelper.COLUMN_ALERT_TIMESTAMP + " = ?",
                new String[]{studentName, timestamp}
        );
        
        db.close();
        return result;
    }
    
    /**
     * 获取预警记录数量
     * @return 预警总数
     */
    public int getAlertCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ALERTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 获取学生的预警记录数量
     * @param studentName 学生名
     * @return 预警数量
     */
    public int getAlertCountByStudent(String studentName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ALERTS +
                " WHERE " + DatabaseHelper.COLUMN_STUDENT_NAME + " = ?",
                new String[]{studentName}
        );
        
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
}