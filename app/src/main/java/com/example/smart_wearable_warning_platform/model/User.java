package com.example.smart_wearable_warning_platform.model;

public class User {
    private String username; // 学号或管理员账号
    private String password;
    private String role; // "Student" or "Admin"
    private String name; // 学生姓名（管理员可为null）

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = null;
    }

    public User(String username, String password, String role, String name) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getRole() {
        return role;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}