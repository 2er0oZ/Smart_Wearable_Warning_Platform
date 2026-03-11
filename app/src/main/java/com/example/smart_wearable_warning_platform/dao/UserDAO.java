package com.example.smart_wearable_warning_platform.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.smart_wearable_warning_platform.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问对象
 */
public class UserDAO {
    
    private DatabaseHelper dbHelper;
    
    public UserDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }
    
    /**
     * 插入用户
     * @param user 用户对象
     * @return 插入的行ID，-1表示失败
     */
    public long insertUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, user.getUsername());
        values.put(DatabaseHelper.COLUMN_PASSWORD, user.getPassword());
        values.put(DatabaseHelper.COLUMN_ROLE, user.getRole());
        
        long result = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        db.close();
        return result;
    }
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象，不存在返回null
     */
    public User getUserByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME, DatabaseHelper.COLUMN_PASSWORD, DatabaseHelper.COLUMN_ROLE},
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            user = new User(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROLE))
            );
            cursor.close();
        }
        
        db.close();
        return user;
    }
    
    /**
     * 根据用户名和密码查询用户（用于登录）
     * @param username 用户名
     * @param password 密码
     * @return 用户对象，不存在返回null
     */
    public User getUserByUsernameAndPassword(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        User user = null;
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME, DatabaseHelper.COLUMN_PASSWORD, DatabaseHelper.COLUMN_ROLE},
                DatabaseHelper.COLUMN_USERNAME + " = ? AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            user = new User(
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROLE))
            );
            cursor.close();
        }
        
        db.close();
        return user;
    }
    
    /**
     * 查询所有用户
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME, DatabaseHelper.COLUMN_PASSWORD, DatabaseHelper.COLUMN_ROLE},
                null, null, null, null, null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                User user = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROLE))
                );
                userList.add(user);
            }
            cursor.close();
        }
        
        db.close();
        return userList;
    }
    
    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新的行数
     */
    public int updateUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PASSWORD, user.getPassword());
        values.put(DatabaseHelper.COLUMN_ROLE, user.getRole());
        
        int result = db.update(
                DatabaseHelper.TABLE_USERS,
                values,
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{user.getUsername()}
        );
        
        db.close();
        return result;
    }
    
    /**
     * 删除用户
     * @param username 用户名
     * @return 删除的行数
     */
    public int deleteUser(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(
                DatabaseHelper.TABLE_USERS,
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username}
        );
        db.close();
        return result;
    }
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return true表示存在，false表示不存在
     */
    public boolean isUserExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME},
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );
        
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        
        db.close();
        return exists;
    }
    
    /**
     * 获取用户数量
     * @return 用户总数
     */
    public int getUserCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
}