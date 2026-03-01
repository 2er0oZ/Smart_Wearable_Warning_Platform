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
        public List<HealthAlert> alerts = new ArrayList<>();
        public boolean expanded = false;
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
        holder.tvStudentName.setText(g.studentName);
        holder.tvCount.setText(g.alerts.size() + "条");

        // 控制详情视图
        if (g.expanded) {
            holder.layoutDetails.setVisibility(View.VISIBLE);
            // 先清空已有，然后填充
            holder.layoutDetails.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (HealthAlert a : g.alerts) {
                View detail = inflater.inflate(R.layout.alert_detail_item, holder.layoutDetails, false);
                TextView tvMsg = detail.findViewById(R.id.tv_detail_message);
                TextView tvTime = detail.findViewById(R.id.tv_detail_time);
                // 构建显示心率和步频
                String extra;
                if (a.isStep()) {
                    extra = "步频: " + a.getStepFreq();
                } else {
                    extra = "心率: " + a.getBpm() + " bpm";
                }
                tvMsg.setText(a.getMessage() + " (" + extra + ")");
                tvTime.setText(a.getTimestamp());
                holder.layoutDetails.addView(detail);
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
