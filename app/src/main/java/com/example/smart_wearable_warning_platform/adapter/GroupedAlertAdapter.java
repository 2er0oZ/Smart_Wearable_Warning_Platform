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

import java.util.ArrayList;
import java.util.List;

public class GroupedAlertAdapter extends RecyclerView.Adapter<GroupedAlertAdapter.ViewHolder> {

    public static class Group {
        public String studentName;
        public String studentId; // 学生学号
        public List<HealthAlert> alerts = new ArrayList<>();
        public boolean expanded = false;
        public int currentCount = 10; // 当前显示的预警数量，初始值为10
        public int pageSize = 10; // 每次加载更多时显示的预警数量
    }

    private List<Group> groups;

    public GroupedAlertAdapter(List<Group> groups) {
        this.groups = groups;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group g = groups.get(position);
        String displayName = g.studentName + " " + g.studentId;
        holder.tvStudentName.setText(displayName);
        holder.tvCount.setText(g.alerts.size() + "条");

        // 控制详情视图
        if (g.expanded) {
            holder.layoutDetails.setVisibility(View.VISIBLE);
            // 先清空已有，然后填充
            holder.layoutDetails.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            
            // 计算当前显示的结束索引
            int endIndex = Math.min(g.currentCount, g.alerts.size());
            
            // 显示从0到currentCount的所有预警信息
            for (int i = 0; i < endIndex; i++) {
                HealthAlert a = g.alerts.get(i);
                View detail = inflater.inflate(R.layout.alert_detail_item, holder.layoutDetails, false);
                TextView tvMsg = detail.findViewById(R.id.tv_detail_message);
                TextView tvTime = detail.findViewById(R.id.tv_detail_time);
                // message 已经包含阈值和步频信息，无需额外拼接
                tvMsg.setText(a.getMessage());
                tvTime.setText(a.getTimestamp());
                holder.layoutDetails.addView(detail);
            }
            
            // 如果还有更多数据，显示加载更多按钮
            if (g.currentCount < g.alerts.size()) {
                View loadMoreView = inflater.inflate(R.layout.alert_detail_item, holder.layoutDetails, false);
                TextView tvMsg = loadMoreView.findViewById(R.id.tv_detail_message);
                TextView tvTime = loadMoreView.findViewById(R.id.tv_detail_time);
                tvMsg.setText("加载更多...");
                tvTime.setText((g.alerts.size() - g.currentCount) + "条剩余");
                tvMsg.setTextColor(0xFF2196F3); // 蓝色
                loadMoreView.setOnClickListener(v -> {
                    g.currentCount += g.pageSize;
                    notifyItemChanged(position);
                });
                holder.layoutDetails.addView(loadMoreView);
            }
        } else {
            holder.layoutDetails.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            g.expanded = !g.expanded;
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvCount;
        LinearLayout layoutDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvCount = itemView.findViewById(R.id.tv_count);
            layoutDetails = itemView.findViewById(R.id.layout_group_details);
        }
    }
}