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

    private EditText etUsername, etPassword, etConfirmPassword;
    private RadioButton rbStudent, rbAdmin;
    private Button btnRegister, btnBackToLogin;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 对应下面的布局文件

        dataManager = new DataManager(this);

        etUsername = findViewById(R.id.et_reg_username);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        rbStudent = findViewById(R.id.rb_reg_student);
        rbAdmin = findViewById(R.id.rb_reg_admin);
        btnRegister = findViewById(R.id.btn_register);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        btnRegister.setOnClickListener(v -> handleRegister());

        btnBackToLogin.setOnClickListener(v -> {
            finish(); // 返回登录页
        });
    }

    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String role = rbStudent.isChecked() ? "Student" : "Admin";

        // 1. 基础校验
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 密码确认校验
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 检查用户名是否已存在 (关键步骤)
        if (dataManager.isUserExists(username)) {
            Toast.makeText(this, "用户名已存在，请更换", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. 注册用户
        User newUser = new User(username, password, role);
        dataManager.registerUser(newUser);

        Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show();

        // 5. 跳转回登录页
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 销毁当前注册页
    }
}
