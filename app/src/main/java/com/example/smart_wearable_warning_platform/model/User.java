package com.example.smart_wearable_warning_platform.model;

public class User {
    private String username;
    private String password;
    private String role; // "Student" or "Admin"

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
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
}
