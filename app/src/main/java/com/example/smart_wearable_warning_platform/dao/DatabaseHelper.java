package com.example.smart_wearable_warning_platform.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类 - 管理所有数据表的创建和版本控制
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    // 数据库信息
    private static final String DATABASE_NAME = "health_monitor.db";
    private static final int DATABASE_VERSION = 3;
    
    // 用户表
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_NAME = "name";
    
    // 心率数据表
    public static final String TABLE_HEART_RATE = "heart_rate_entries";
    public static final String COLUMN_HR_ID = "_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_BPM = "bpm";
    public static final String COLUMN_STEP_FREQUENCY = "step_frequency";
    public static final String COLUMN_USERNAME_FK = "username";
    
    // 健康预警表
    public static final String TABLE_ALERTS = "health_alerts";
    public static final String COLUMN_ALERT_ID = "_id";
    public static final String COLUMN_ALERT_TIMESTAMP = "timestamp";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_STUDENT_NAME = "student_name";
    public static final String COLUMN_STUDENT_ID = "student_id";
    public static final String COLUMN_ALERT_BPM = "bpm";
    public static final String COLUMN_STEP_FREQ = "step_freq";
    public static final String COLUMN_IS_STEP = "is_step";
    public static final String COLUMN_IS_EXPANDED = "is_expanded";
    
    // 学生阈值表
    public static final String TABLE_THRESHOLDS = "student_thresholds";
    public static final String COLUMN_THRESHOLD_ID = "_id";
    public static final String COLUMN_THRESHOLD_USERNAME = "username";
    public static final String COLUMN_MIN_HR = "min_hr";
    public static final String COLUMN_MAX_HR = "max_hr";
    public static final String COLUMN_MIN_STEP = "min_step";
    public static final String COLUMN_MAX_STEP = "max_step";
    public static final String COLUMN_SLEEP_START_TIME = "sleep_start_time";
    public static final String COLUMN_SLEEP_END_TIME = "sleep_end_time";
    public static final String COLUMN_ENABLE_SLEEP_ALERT = "enable_sleep_alert";
    
    // 睡眠数据表
    public static final String TABLE_SLEEP_DATA = "sleep_data";
    public static final String COLUMN_SLEEP_ID = "_id";
    public static final String COLUMN_SLEEP_DATE = "date";
    public static final String COLUMN_SLEEP_TIME = "sleep_time";
    public static final String COLUMN_WAKE_TIME = "wake_time";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_QUALITY = "quality";
    public static final String COLUMN_SLEEP_USERNAME = "username";
    
    // 创建用户表的SQL语句
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "(" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
            COLUMN_PASSWORD + " TEXT NOT NULL, " +
            COLUMN_ROLE + " TEXT NOT NULL, " +
            COLUMN_NAME + " TEXT" + ");";
    
    // 创建心率数据表的SQL语句
    private static final String CREATE_TABLE_HEART_RATE = "CREATE TABLE " + TABLE_HEART_RATE + "(" +
            COLUMN_HR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
            COLUMN_BPM + " INTEGER NOT NULL, " +
            COLUMN_STEP_FREQUENCY + " INTEGER DEFAULT 0, " +
            COLUMN_USERNAME_FK + " TEXT NOT NULL, " +
            "FOREIGN KEY(" + COLUMN_USERNAME_FK + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USERNAME + ")" + ");";
    
    // 创建健康预警表的SQL语句
    private static final String CREATE_TABLE_ALERTS = "CREATE TABLE " + TABLE_ALERTS + "(" +
            COLUMN_ALERT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ALERT_TIMESTAMP + " TEXT NOT NULL, " +
            COLUMN_MESSAGE + " TEXT NOT NULL, " +
            COLUMN_STUDENT_NAME + " TEXT NOT NULL, " +
            COLUMN_STUDENT_ID + " TEXT NOT NULL, " +
            COLUMN_ALERT_BPM + " INTEGER DEFAULT 0, " +
            COLUMN_STEP_FREQ + " INTEGER DEFAULT 0, " +
            COLUMN_IS_STEP + " INTEGER DEFAULT 0, " +
            COLUMN_IS_EXPANDED + " INTEGER DEFAULT 0" + ");";
    
    // 创建学生阈值表的SQL语句
    private static final String CREATE_TABLE_THRESHOLDS = "CREATE TABLE " + TABLE_THRESHOLDS + "(" +
            COLUMN_THRESHOLD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_THRESHOLD_USERNAME + " TEXT UNIQUE NOT NULL, " +
            COLUMN_MIN_HR + " INTEGER DEFAULT 50, " +
            COLUMN_MAX_HR + " INTEGER DEFAULT 100, " +
            COLUMN_MIN_STEP + " INTEGER DEFAULT 0, " +
            COLUMN_MAX_STEP + " INTEGER DEFAULT 5, " +
            COLUMN_SLEEP_START_TIME + " TEXT DEFAULT '21:00', " +
            COLUMN_SLEEP_END_TIME + " TEXT DEFAULT '07:00', " +
            COLUMN_ENABLE_SLEEP_ALERT + " INTEGER DEFAULT 1" + ");";
    
    // 创建睡眠数据表的SQL语句
    private static final String CREATE_TABLE_SLEEP_DATA = "CREATE TABLE " + TABLE_SLEEP_DATA + "(" +
            COLUMN_SLEEP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SLEEP_DATE + " TEXT NOT NULL, " +
            COLUMN_SLEEP_TIME + " TEXT NOT NULL, " +
            COLUMN_WAKE_TIME + " TEXT NOT NULL, " +
            COLUMN_DURATION + " INTEGER NOT NULL, " +
            COLUMN_QUALITY + " INTEGER NOT NULL, " +
            COLUMN_SLEEP_USERNAME + " TEXT NOT NULL, " +
            "FOREIGN KEY(" + COLUMN_SLEEP_USERNAME + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE" + ");";
    
    // 创建索引的SQL语句
    private static final String CREATE_INDEX_HR_TIMESTAMP = "CREATE INDEX idx_hr_timestamp ON " + TABLE_HEART_RATE + "(" + COLUMN_TIMESTAMP + ")";
    private static final String CREATE_INDEX_HR_USERNAME = "CREATE INDEX idx_hr_username ON " + TABLE_HEART_RATE + "(" + COLUMN_USERNAME_FK + ")";
    private static final String CREATE_INDEX_ALERT_TIMESTAMP = "CREATE INDEX idx_alert_timestamp ON " + TABLE_ALERTS + "(" + COLUMN_ALERT_TIMESTAMP + ")";
    private static final String CREATE_INDEX_ALERT_STUDENT = "CREATE INDEX idx_alert_student ON " + TABLE_ALERTS + "(" + COLUMN_STUDENT_NAME + ")";
    private static final String CREATE_INDEX_SLEEP_USERNAME_DATE = "CREATE INDEX idx_sleep_username_date ON " + TABLE_SLEEP_DATA + "(" + COLUMN_SLEEP_USERNAME + ", " + COLUMN_SLEEP_DATE + ")";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建所有表
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_HEART_RATE);
        db.execSQL(CREATE_TABLE_ALERTS);
        db.execSQL(CREATE_TABLE_THRESHOLDS);
        db.execSQL(CREATE_TABLE_SLEEP_DATA);
        
        // 创建索引以提高查询性能
        db.execSQL(CREATE_INDEX_HR_TIMESTAMP);
        db.execSQL(CREATE_INDEX_HR_USERNAME);
        db.execSQL(CREATE_INDEX_ALERT_TIMESTAMP);
        db.execSQL(CREATE_INDEX_ALERT_STUDENT);
        db.execSQL(CREATE_INDEX_SLEEP_USERNAME_DATE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 从版本1升级到版本2：添加name字段
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_NAME + " TEXT");
        }
        if (oldVersion < 3) {
            // 从版本2升级到版本3：添加student_id字段
            db.execSQL("ALTER TABLE " + TABLE_ALERTS + " ADD COLUMN " + COLUMN_STUDENT_ID + " TEXT");
        }
        // 如果需要更多版本升级，可以在这里添加更多if语句
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // 启用外键约束
        db.execSQL("PRAGMA foreign_keys = ON;");
    }
}