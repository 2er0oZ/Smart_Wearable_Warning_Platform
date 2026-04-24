package com.example.smart_wearable_warning_platform.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smart_wearable_warning_platform.LoginActivity;
import com.example.smart_wearable_warning_platform.R;
import com.example.smart_wearable_warning_platform.CustomMarkerView;
import com.example.smart_wearable_warning_platform.controller.SleepAdviceController;
import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.User;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class HeartRateFragment extends Fragment {

    private DataManager dataManager;
    private Button btnUpload;
    private LineChart lineChart;
    private TextView tvAvgHr, tvMaxHr, tvMinHr;
    private TextView tvMaxSteps, tvMinSteps; // 步频统计（只保留最高和最低）
    
    // 时间查询相关控件
    private TextView tvStartDate, tvStartTime, tvEndDate, tvEndTime;
    private Button btnQuery, btnReset;
    private List<HeartRateEntry> allData; // 存储所有数据，用于查询过滤
    private List<HeartRateEntry> filteredData; // 存储过滤后的数据
    
    // 日期和时间选择器相关变量
    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    // 查询状态保存
    private boolean isQueryActive = false; // 是否有查询结果

    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private final ActivityResultLauncher<String> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processCsvFile(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_heart_rate, container, false);

        tvAvgHr = root.findViewById(R.id.tv_avg_hr);
        tvMaxHr = root.findViewById(R.id.tv_max_hr);
        tvMinHr = root.findViewById(R.id.tv_min_hr);
        tvMaxSteps = root.findViewById(R.id.tv_max_steps);
        tvMinSteps = root.findViewById(R.id.tv_min_steps);
        
        // 初始化时间查询控件
        tvStartDate = root.findViewById(R.id.tv_start_date);
        tvStartTime = root.findViewById(R.id.tv_start_time);
        tvEndDate = root.findViewById(R.id.tv_end_date);
        tvEndTime = root.findViewById(R.id.tv_end_time);
        btnQuery = root.findViewById(R.id.btn_query);
        btnReset = root.findViewById(R.id.btn_reset);

        dataManager = new DataManager(requireContext());
        btnUpload = root.findViewById(R.id.btn_upload_csv);
        lineChart = root.findViewById(R.id.line_chart);

        btnUpload.setOnClickListener(v -> csvPickerLauncher.launch("*/*"));
        
        // 设置时间查询按钮点击事件
        btnQuery.setOnClickListener(v -> queryDataByTimeRange());
        btnReset.setOnClickListener(v -> resetTimeFilter());
        
        // 设置日期和时间选择器的点击事件
        tvStartDate.setOnClickListener(v -> showStartDatePicker());
        tvStartTime.setOnClickListener(v -> showStartTimePicker());
        tvEndDate.setOnClickListener(v -> showEndDatePicker());
        tvEndTime.setOnClickListener(v -> showEndTimePicker());

        loadSavedData();
        
        // 恢复查询状态
        restoreQueryState();
        
        return root;
    }

    private void loadSavedData() {
        User currentUser = dataManager.getCurrentUser();
        if (currentUser != null) {
            allData = dataManager.getHeartRateData(currentUser.getUsername());
            if (!allData.isEmpty()) {
                // 如果有查询结果，显示查询结果；否则显示全部数据
                if (isQueryActive && filteredData != null && !filteredData.isEmpty()) {
                    renderChart(filteredData);
                } else {
                    renderChart(allData);
                }
            }
        }
    }

    private void processCsvFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            List<HeartRateEntry> data = dataManager.parseCSV(inputStream);
            inputStream.close();

            if (data.isEmpty()) {
                Toast.makeText(requireContext(), "CSV文件为空或格式错误", Toast.LENGTH_SHORT).show();
                return;
            }

            User currentUser = dataManager.getCurrentUser();
            if (currentUser != null) {
                // 保存并合并已有数据
                dataManager.saveHeartRateData(currentUser.getUsername(), data);
                // 使用原始新条目生成预警
                dataManager.checkAndGenerateAlerts(currentUser.getUsername(), data);
                // 刷新睡眠建议数据
                SleepAdviceController.getInstance(requireContext()).refreshSleepData();
                Toast.makeText(requireContext(), "数据已保存，预警检测完成", Toast.LENGTH_SHORT).show();
                // 将合并后的全量数据加载到图表
                allData = dataManager.getHeartRateData(currentUser.getUsername());
                // 重置查询状态，因为新数据可能使之前的查询失效
                isQueryActive = false;
                filteredData = null;
                // 清空时间选择控件
                tvStartDate.setText("");
                tvStartTime.setText("");
                tvEndDate.setText("");
                tvEndTime.setText("");
            }

        // 渲染图表
        if (isQueryActive && filteredData != null && !filteredData.isEmpty()) {
            renderChart(filteredData);
        } else {
            renderChart(allData);
        }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "文件读取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderChart(List<HeartRateEntry> data) {
        updateStatistics(data);

        List<Entry> hrEntries = new ArrayList<>();
        List<Entry> stepEntries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            hrEntries.add(new Entry(i, data.get(i).getBpm()));
            stepEntries.add(new Entry(i, data.get(i).getStepFrequency()));
        }

        LineDataSet hrSet = new LineDataSet(hrEntries, "心率");
        hrSet.setColor(Color.parseColor("#FF6B6B"));
        hrSet.setCircleColor(Color.parseColor("#FF6B6B"));
        hrSet.setLineWidth(2.5f);
        hrSet.setCircleRadius(3f);
        hrSet.setDrawValues(false);
        hrSet.setDrawCircles(true);
        hrSet.setMode(LineDataSet.Mode.LINEAR);
        hrSet.setDrawFilled(true);
        hrSet.setFillColor(Color.parseColor("#33FF6B6B"));
        hrSet.setFillAlpha(30);
        
        LineDataSet stepSet = new LineDataSet(stepEntries, "步频");
        stepSet.setColor(Color.parseColor("#4ECDC4"));
        stepSet.setCircleColor(Color.parseColor("#4ECDC4"));
        stepSet.setLineWidth(2.0f);
        stepSet.setCircleRadius(2.5f);
        stepSet.setDrawValues(false);
        stepSet.setDrawCircles(true);
        stepSet.setMode(LineDataSet.Mode.LINEAR);
        stepSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        stepSet.setDrawFilled(true);
        stepSet.setFillColor(Color.parseColor("#334ECDC4"));
        stepSet.setFillAlpha(30);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(hrSet);
        dataSets.add(stepSet);

        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);

        // X 轴显示日期+时间，避免显示负号问题
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        // 限制最多6个标签并自动调整，过多则旋转防止拥挤
        xAxis.setLabelCount(Math.min(data.size(), 6), true);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < data.size()) {
                    String fullTime = data.get(index).getTimestamp();
                    if (fullTime.length() >= 16) {
                        return fullTime.substring(0, 16);
                    }
                    return fullTime;
                }
                return "";
            }
        });

        // 右侧 Y 轴用于步频
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(true);
        float minStep = stepSet.getYMin();
        float maxStep = stepSet.getYMax();
        float margin = 10f; // 让图形上下各留一些空间
        // 轴最小值设置为 minStep - margin，即可在底部留白，可能为负数
        rightAxis.setAxisMinimum(minStep - margin);
        if (maxStep <= 0) {
            maxStep = 10; // 如果所有数据都是0，则给一个默认范围
        }
        rightAxis.setAxisMaximum(maxStep + margin);
        // 隐藏负数标签，使底部空间不显示数字
        rightAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 0) return "";
                return String.valueOf((int) value);
            }
        });
        rightAxis.setDrawGridLines(false);
        rightAxis.setTextColor(Color.parseColor("#4ECDC4"));

        // 左侧 Y 轴心率设置保留
        YAxis leftAxis = lineChart.getAxisLeft();
        float minVal = hrSet.getYMin();
        float maxVal = hrSet.getYMax();
        if (minVal == maxVal) {
            minVal -= 10;
            maxVal += 10;
        }
        leftAxis.setAxisMinimum(minVal - 10);
        leftAxis.setAxisMaximum(maxVal + 10);
        // 给心率轴加一些边缘空隙
        leftAxis.setSpaceTop(15f);
        leftAxis.setSpaceBottom(15f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setTextColor(Color.parseColor("#FF6B6B"));
        leftAxis.setGranularity(1f);

        lineChart.getAxisRight().setEnabled(true);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setVisibleXRangeMaximum(30f);
        // 避免底部被按钮遮挡
        lineChart.setExtraBottomOffset(40f);
        // 设置图表背景
        lineChart.setBackgroundColor(Color.parseColor("#FAFAFA"));
        lineChart.setDrawGridBackground(false);

        CustomMarkerView mv = new CustomMarkerView(requireContext(), R.layout.layout_marker, data);
        mv.setChartView(lineChart);
        lineChart.setMarker(mv);

        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
        lineChart.moveViewToX(0);
        // 添加动画效果
        lineChart.animateX(1000);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 在Fragment恢复时，重新加载数据并保持查询状态
        loadSavedData();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 在Fragment暂停时，保存当前查询状态
        saveQueryState();
    }
    
    /**
     * 保存当前查询状态
     */
    private void saveQueryState() {
        // 使用SharedPreferences保存查询状态
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("HeartRateFragment", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        // 保存查询状态
        editor.putBoolean("isQueryActive", isQueryActive);
        
        // 保存时间选择
        editor.putString("startDate", tvStartDate.getText().toString());
        editor.putString("startTime", tvStartTime.getText().toString());
        editor.putString("endDate", tvEndDate.getText().toString());
        editor.putString("endTime", tvEndTime.getText().toString());
        
        editor.apply();
    }
    
    /**
     * 恢复查询状态
     */
    private void restoreQueryState() {
        // 从SharedPreferences恢复查询状态
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("HeartRateFragment", Context.MODE_PRIVATE);
        
        // 恢复查询状态
        isQueryActive = prefs.getBoolean("isQueryActive", false);
        
        // 恢复时间选择
        String startDate = prefs.getString("startDate", "");
        String startTime = prefs.getString("startTime", "");
        String endDate = prefs.getString("endDate", "");
        String endTime = prefs.getString("endTime", "");
        
        tvStartDate.setText(startDate);
        tvStartTime.setText(startTime);
        tvEndDate.setText(endDate);
        tvEndTime.setText(endTime);
        
        // 如果有查询状态，重新执行查询
        if (isQueryActive && !startDate.isEmpty() && !startTime.isEmpty() && !endDate.isEmpty() && !endTime.isEmpty()) {
            queryDataByTimeRange();
        }
    }
    
    private void updateStatistics(List<HeartRateEntry> data) {
        if (data == null || data.isEmpty()) {
            tvAvgHr.setText("--");
            tvMaxHr.setText("--");
            tvMinHr.setText("--");
            tvMaxSteps.setText("--");
            tvMinSteps.setText("--");
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;

        int minStep = Integer.MAX_VALUE;
        int maxStep = Integer.MIN_VALUE;

        for (HeartRateEntry entry : data) {
            int bpm = entry.getBpm();
            sum += bpm;
            if (bpm < min) min = bpm;
            if (bpm > max) max = bpm;

            int sf = entry.getStepFrequency();
            if (sf < 0) sf = 0; // 不能小于零
            if (sf < minStep) minStep = sf;
            if (sf > maxStep) maxStep = sf;
        }

        double avg = (double) sum / data.size();
        tvAvgHr.setText(String.format("%.1f", avg));
        tvMaxHr.setText(String.valueOf(max));
        tvMinHr.setText(String.valueOf(min));
        tvMaxSteps.setText(String.valueOf(maxStep));
        tvMinSteps.setText(String.valueOf(minStep));
    }
    
    /**
     * 根据时间区间查询数据
     */
    private void queryDataByTimeRange() {
        if (allData == null || allData.isEmpty()) {
            Toast.makeText(requireContext(), "没有可查询的数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String startDateStr = tvStartDate.getText().toString().trim();
        String startTimeStr = tvStartTime.getText().toString().trim();
        String endDateStr = tvEndDate.getText().toString().trim();
        String endTimeStr = tvEndTime.getText().toString().trim();
        
        // 验证输入
        if (startDateStr.isEmpty() || startTimeStr.isEmpty() || endDateStr.isEmpty() || endTimeStr.isEmpty()) {
            Toast.makeText(requireContext(), "请选择完整的时间区间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // 构建开始和结束时间
            String startDateTime = startDateStr + " " + startTimeStr + ":00";
            String endDateTime = endDateStr + " " + endTimeStr + ":00";
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date startDate = sdf.parse(startDateTime);
            Date endDate = sdf.parse(endDateTime);
            
            if (startDate == null || endDate == null) {
                Toast.makeText(requireContext(), "时间格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (startDate.after(endDate)) {
                Toast.makeText(requireContext(), "开始时间不能晚于结束时间", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 过滤数据
            filteredData = new ArrayList<>();
            for (HeartRateEntry entry : allData) {
                try {
                    Date entryDate = sdf.parse(entry.getTimestamp());
                    if (entryDate != null && !entryDate.before(startDate) && !entryDate.after(endDate)) {
                        filteredData.add(entry);
                    }
                } catch (ParseException e) {
                    // 跳过格式不正确的条目
                }
            }
            
            if (filteredData.isEmpty()) {
                Toast.makeText(requireContext(), "所选时间区间内没有数据", Toast.LENGTH_SHORT).show();
                isQueryActive = false; // 查询无结果，标记为非活动状态
            } else {
                Toast.makeText(requireContext(), "查询到 " + filteredData.size() + " 条数据", Toast.LENGTH_SHORT).show();
                isQueryActive = true; // 查询有结果，标记为活动状态
            }
            
            // 更新图表
            renderChart(filteredData);
            
        } catch (ParseException e) {
            Toast.makeText(requireContext(), "时间格式不正确", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 重置时间过滤器，显示所有数据
     */
    private void resetTimeFilter() {
        tvStartDate.setText("");
        tvStartTime.setText("");
        tvEndDate.setText("");
        tvEndTime.setText("");
        
        if (allData != null && !allData.isEmpty()) {
            renderChart(allData);
            Toast.makeText(requireContext(), "已重置，显示所有数据", Toast.LENGTH_SHORT).show();
        }
        
        // 重置查询状态
        isQueryActive = false;
        filteredData = null;
    }
    
    /**
     * 显示开始日期选择器
     */
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    startCalendar.set(Calendar.YEAR, year);
                    startCalendar.set(Calendar.MONTH, month);
                    startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    tvStartDate.setText(dateFormat.format(startCalendar.getTime()));
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
    
    /**
     * 显示开始时间选择器
     */
    private void showStartTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startCalendar.set(Calendar.MINUTE, minute);
                    tvStartTime.setText(timeFormat.format(startCalendar.getTime()));
                },
                startCalendar.get(Calendar.HOUR_OF_DAY),
                startCalendar.get(Calendar.MINUTE),
                true); // 24小时制
        timePickerDialog.show();
    }
    
    /**
     * 显示结束日期选择器
     */
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    endCalendar.set(Calendar.YEAR, year);
                    endCalendar.set(Calendar.MONTH, month);
                    endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    tvEndDate.setText(dateFormat.format(endCalendar.getTime()));
                },
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
    
    /**
     * 显示结束时间选择器
     */
    private void showEndTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    endCalendar.set(Calendar.MINUTE, minute);
                    tvEndTime.setText(timeFormat.format(endCalendar.getTime()));
                },
                endCalendar.get(Calendar.HOUR_OF_DAY),
                endCalendar.get(Calendar.MINUTE),
                true); // 24小时制
        timePickerDialog.show();
    }
}