package com.example.smart_wearable_warning_platform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_wearable_warning_platform.LoginActivity;
import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.User;

public class ProfileFragment extends Fragment {

    private DataManager dataManager;
    private TextView tvUsername, tvRole;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        tvUsername = root.findViewById(R.id.tv_username);
        tvRole = root.findViewById(R.id.tv_role);
        btnLogout = root.findViewById(R.id.btn_logout_profile);

        dataManager = new DataManager(requireContext());
        User current = dataManager.getCurrentUser();
        if (current != null) {
            tvUsername.setText(current.getUsername());
            tvRole.setText("身份: " + current.getRole());
        }

        btnLogout.setOnClickListener(v -> {
            dataManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return root;
    }
}