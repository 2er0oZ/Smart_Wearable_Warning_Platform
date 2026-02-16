package com.example.smart_wearable_warning_platform;

import android.content.Context;
import android.widget.TextView;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

public class CustomMarkerView extends MarkerView {

    private TextView tvTime, tvBpm;
    private List<HeartRateEntry> dataList; // 传入原始数据，用于查找时间

    public CustomMarkerView(Context context, int layoutResource, List<HeartRateEntry> dataList) {
        super(context, layoutResource);
        this.dataList = dataList;
        tvTime = findViewById(R.id.tv_marker_time);
        tvBpm = findViewById(R.id.tv_marker_bpm);
    }

    // 每次点击点的时候，都会调用这个方法
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // e.getX() 是我们在 renderChart 里设置的索引 (0, 1, 2...)
        int index = (int) e.getX();
        float bpm = e.getY();

        // 从数据列表中取出对应的时间字符串
        String timeStr = "--:--";
        if (index >= 0 && index < dataList.size()) {
            String fullTime = dataList.get(index).getTimestamp();
            // 截取 HH:mm
            if (fullTime.length() > 16) {
                timeStr = fullTime.substring(11, 16);
            }
        }

        tvTime.setText("时间: " + timeStr);
        tvBpm.setText("心率: " + (int) bpm);

        super.refreshContent(e, highlight);
    }

    // 定义气泡显示的位置（在点的上方）
    @Override
    public MPPointF getOffset() {
        // -(width / 2) 让气泡水平居中，-getHeight() 让气泡显示在点上方
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
