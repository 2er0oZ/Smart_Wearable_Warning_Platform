package com.example.smart_wearable_warning_platform;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private RadioButton rbStudent, rbAdmin;
    private Button btnLogin;
    private TextView tvGoToRegister; // 新增：跳转注册
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dataManager = new DataManager(this);

        // 检查是否已登录
        if (dataManager.getCurrentUser() != null) {
            navigateToHome(dataManager.getCurrentUser());
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        rbStudent = findViewById(R.id.rb_student);
        rbAdmin = findViewById(R.id.rb_admin);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register); // 绑定ID

        btnLogin.setOnClickListener(v -> handleLogin());

        // 新增：点击跳转到注册页
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String role = rbStudent.isChecked() ? "Student" : "Admin";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 仅执行登录逻辑
        User user = dataManager.loginUser(username, password);

        if (user != null) {
            // 登录成功
            if (!user.getRole().equals(role)) {
                Toast.makeText(this, "该账户角色不匹配，请选择: " + user.getRole(), Toast.LENGTH_SHORT).show();
            } else {
                navigateToHome(user);
            }
        } else {
            // 登录失败：提示用户名或密码错误（不再自动注册）
            Toast.makeText(this, "用户名不存在或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToHome(User user) {
        Intent intent;
        if ("Student".equals(user.getRole())) {
            intent = new Intent(this, StudentActivity.class);
        } else {
            intent = new Intent(this, AdminActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
