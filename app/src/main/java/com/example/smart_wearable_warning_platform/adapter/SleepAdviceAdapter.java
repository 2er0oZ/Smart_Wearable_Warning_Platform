package com.example.smart_wearable_warning_platform.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.model.SleepAdvice;

import java.util.List;

/**
 * 睡眠建议适配器
 */
public class SleepAdviceAdapter extends RecyclerView.Adapter<SleepAdviceAdapter.ViewHolder> {
    private List<SleepAdvice> adviceList;
    
    public SleepAdviceAdapter(List<SleepAdvice> adviceList) {
        this.adviceList = adviceList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sleep_advice, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SleepAdvice advice = adviceList.get(position);
        holder.tvTitle.setText(advice.getTitle());
        holder.tvContent.setText(advice.getContent());
        holder.tvCategory.setText(advice.getCategory());
        
        // 根据优先级设置不同的颜色和背景
        int priorityColor;
        int backgroundColor;
        String priorityText;
        
        if (advice.getPriority() >= 5) {
            // 最高优先级 - 深红色
            priorityColor = Color.parseColor("#D32F2F");
            backgroundColor = Color.parseColor("#FFEBEE");
            priorityText = "紧急";
        } else if (advice.getPriority() >= 4) {
            // 高优先级 - 红色
            priorityColor = Color.parseColor("#F44336");
            backgroundColor = Color.parseColor("#FFEBEE");
            priorityText = "重要";
        } else if (advice.getPriority() >= 3) {
            // 中等优先级 - 橙色
            priorityColor = Color.parseColor("#FF9800");
            backgroundColor = Color.parseColor("#FFF3E0");
            priorityText = "建议";
        } else if (advice.getPriority() >= 2) {
            // 低优先级 - 蓝色
            priorityColor = Color.parseColor("#2196F3");
            backgroundColor = Color.parseColor("#E3F2FD");
            priorityText = "提示";
        } else {
            // 最低优先级 - 绿色
            priorityColor = Color.parseColor("#4CAF50");
            backgroundColor = Color.parseColor("#E8F5E8");
            priorityText = "参考";
        }
        
        // 设置优先级图标颜色
        holder.ivPriority.setColorFilter(priorityColor);
        
        // 设置背景颜色
        holder.itemView.setBackgroundColor(backgroundColor);
        
        // 设置优先级文本（如果有的话）
        if (holder.tvPriority != null) {
            holder.tvPriority.setText(priorityText);
            holder.tvPriority.setTextColor(priorityColor);
        }
    }
    
    @Override
    public int getItemCount() {
        return adviceList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvCategory, tvPriority;
        ImageView ivPriority;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_advice_title);
            tvContent = itemView.findViewById(R.id.tv_advice_content);
            tvCategory = itemView.findViewById(R.id.tv_advice_category);
            ivPriority = itemView.findViewById(R.id.iv_advice_priority);
            // 尝试获取优先级文本视图，如果布局中没有则为null
            tvPriority = itemView.findViewById(R.id.tv_advice_priority_text);
        }
    }
}