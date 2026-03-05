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
        
        // 根据优先级设置不同的颜色
        if (advice.getPriority() >= 4) {
            holder.ivPriority.setColorFilter(Color.parseColor("#FF6B6B"));
        } else if (advice.getPriority() >= 3) {
            holder.ivPriority.setColorFilter(Color.parseColor("#FFA500"));
        } else {
            holder.ivPriority.setColorFilter(Color.parseColor("#4ECDC4"));
        }
    }
    
    @Override
    public int getItemCount() {
        return adviceList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvCategory;
        ImageView ivPriority;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_advice_title);
            tvContent = itemView.findViewById(R.id.tv_advice_content);
            tvCategory = itemView.findViewById(R.id.tv_advice_category);
            ivPriority = itemView.findViewById(R.id.iv_advice_priority);
        }
    }
}