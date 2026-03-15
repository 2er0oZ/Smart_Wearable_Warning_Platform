package com.example.smart_wearable_warning_platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etStudentId, etName, etPassword, etConfirmPassword;
    private Button btnRegister, btnBackToLogin;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 对应下面的布局文件

        dataManager = new DataManager(this);

        etStudentId = findViewById(R.id.et_reg_student_id);
        etName = findViewById(R.id.et_reg_name);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        btnRegister.setOnClickListener(v -> handleRegister());

        btnBackToLogin.setOnClickListener(v -> {
            finish(); // 返回登录页
        });
    }

    private void handleRegister() {
        String studentId = etStudentId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String role = "Student";

        // 1. 基础校验
        if (studentId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "学号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 学生需要输入姓名
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 密码确认校验
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. 检查学号是否已存在
        if (dataManager.isUserExists(studentId)) {
            Toast.makeText(this, "该学号已注册，请更换", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. 注册用户
        User newUser = new User(studentId, password, role, name);
        dataManager.registerUser(newUser);

        Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show();

        // 6. 跳转回登录页
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 销毁当前注册页
    }
}