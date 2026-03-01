package com.example.smart_wearable_warning_platform.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.model.HealthAlert;

import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private List<HealthAlert> alertList;

    public AlertAdapter(List<HealthAlert> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthAlert alert = alertList.get(position);

        // 绑定头部数据
        holder.tvStudentName.setText(alert.getStudentName());
        holder.tvMessage.setText(alert.getMessage());

        // 绑定详情数据：始终显示心率与步频
        holder.tvHr.setText("心率: " + alert.getBpm() + " bpm");
        holder.tvStep.setText("步频: " + alert.getStepFreq());
        holder.tvTimestamp.setText("时间: " + alert.getTimestamp());

        // 处理折叠/展开状态
        boolean isExpanded = alert.isExpanded();
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // 点击整个卡片进行切换
        holder.itemView.setOnClickListener(v -> {
            // 切换状态
            alert.setExpanded(!isExpanded);
            // 通知当前item状态改变（只刷新这一个item，优化性能）
            notifyItemChanged(position);

            // 也可以直接操作 View 视图实现（可选），不需要 notifyItemChanged，这样没有动画刷新
            // layoutDetails.setVisibility(!isExpanded ? View.VISIBLE : View.GONE);
            // alert.setExpanded(!isExpanded);
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvMessage, tvHr, tvStep, tvTimestamp;
        LinearLayout layoutDetails;

        public ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvHr = itemView.findViewById(R.id.tv_hr);
            tvStep = itemView.findViewById(R.id.tv_step);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            layoutDetails = itemView.findViewById(R.id.layout_details);
        }
    }
}
