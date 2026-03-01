package com.example.smart_wearable_warning_platform.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.User;
import com.example.smart_wearable_warning_platform.model.StudentThreshold;

import java.util.ArrayList;
import java.util.List;

public class StudentManagementFragment extends Fragment {

    private RecyclerView recyclerStudents;
    private EditText etSearch;
    private android.widget.Button btnSearch;
    private DataManager dataManager;
    private List<User> allUsers; // all users (已筛选学生名单)
    private List<User> users; // currently shown (根据搜索过滤后的名单)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_student_management, container, false);
        recyclerStudents = root.findViewById(R.id.recycler_students);
        etSearch = root.findViewById(R.id.et_search_students);
        btnSearch = root.findViewById(R.id.btn_search_students);
        dataManager = new DataManager(requireContext());

        // 读取所有用户，筛选出学生
        List<User> all = dataManager.getAllUsers();
        allUsers = new ArrayList<>();
        for (User u : all) {
            if ("Student".equals(u.getRole())) {
                allUsers.add(u);
            }
        }

        // 初始显示全部学生
        users = new ArrayList<>(allUsers);

        recyclerStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerStudents.setAdapter(new StudentAdapter());

        // 搜索框实时过滤
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 点击搜索按钮也触发一次过滤（并可用于提交搜索）
        btnSearch.setOnClickListener(v -> filterUsers(etSearch.getText().toString()));
        return root;
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(20, 30, 20, 30);
            tv.setTextSize(16);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = users.get(position);
            holder.tv.setText(u.getUsername());
            holder.tv.setOnClickListener(v -> showThresholdDialog(u));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            public VH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }

    private void showThresholdDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("设置 " + user.getUsername() + " 的阈值");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etMinHr = new EditText(requireContext());
        etMinHr.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMinHr.setHint("最低心率");
        EditText etMaxHr = new EditText(requireContext());
        etMaxHr.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMaxHr.setHint("最高心率");

        EditText etMinStep = new EditText(requireContext());
        etMinStep.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMinStep.setHint("最低步频");
        EditText etMaxStep = new EditText(requireContext());
        etMaxStep.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMaxStep.setHint("最高步频");

        // 获取当前阈值对象
        StudentThreshold th = dataManager.getThresholdForUser(user.getUsername());
        etMinHr.setText(String.valueOf(th.getMinHr()));
        etMaxHr.setText(String.valueOf(th.getMaxHr()));
        etMinStep.setText(String.valueOf(th.getMinStep()));
        etMaxStep.setText(String.valueOf(th.getMaxStep()));

        layout.addView(etMinHr);
        layout.addView(etMaxHr);
        layout.addView(etMinStep);
        layout.addView(etMaxStep);

        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            try {
                int minHr = Integer.parseInt(etMinHr.getText().toString().trim());
                int maxHr = Integer.parseInt(etMaxHr.getText().toString().trim());
                int minStep = Integer.parseInt(etMinStep.getText().toString().trim());
                int maxStep = Integer.parseInt(etMaxStep.getText().toString().trim());
                if (minHr >= maxHr) {
                    Toast.makeText(requireContext(), "最低心率必须小于最高心率", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (minStep >= maxStep) {
                    Toast.makeText(requireContext(), "最低步频必须小于最高步频", Toast.LENGTH_SHORT).show();
                    return;
                }
                dataManager.setStudentThreshold(user.getUsername(), minHr, maxHr, minStep, maxStep);
                // 保存后建议重建预警以便立即生效
                dataManager.rebuildAlertsFromSavedData();
                Toast.makeText(requireContext(), "阈值已保存", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void filterUsers(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        users.clear();
        if (q.isEmpty()) {
            users.addAll(allUsers);
        } else {
            for (User u : allUsers) {
                if (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) {
                    users.add(u);
                }
            }
        }
        // 通知 RecyclerView 更新
        requireActivity().runOnUiThread(() -> {
            if (recyclerStudents.getAdapter() != null) {
                recyclerStudents.getAdapter().notifyDataSetChanged();
            }
        });
    }
}
