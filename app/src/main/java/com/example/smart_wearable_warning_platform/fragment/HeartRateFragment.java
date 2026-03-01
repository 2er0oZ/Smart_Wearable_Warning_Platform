package com.example.smart_wearable_warning_platform.fragment;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeartRateFragment extends Fragment {

    private DataManager dataManager;
    private Button btnUpload;
    private LineChart lineChart;
    private TextView tvAvgHr, tvMaxHr, tvMinHr;
    private TextView tvAvgSteps, tvMaxSteps, tvMinSteps; // 步频统计

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
        tvAvgSteps = root.findViewById(R.id.tv_avg_steps);
        tvMaxSteps = root.findViewById(R.id.tv_max_steps);
        tvMinSteps = root.findViewById(R.id.tv_min_steps);

        dataManager = new DataManager(requireContext());
        btnUpload = root.findViewById(R.id.btn_upload_csv);
        lineChart = root.findViewById(R.id.line_chart);

        btnUpload.setOnClickListener(v -> csvPickerLauncher.launch("*/*"));

        loadSavedData();
        return root;
    }

    private void loadSavedData() {
        User currentUser = dataManager.getCurrentUser();
        if (currentUser != null) {
            List<HeartRateEntry> savedData = dataManager.getHeartRateData(currentUser.getUsername());
            if (!savedData.isEmpty()) {
                renderChart(savedData);
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
                dataManager.saveHeartRateData(currentUser.getUsername(), data);
                dataManager.checkAndGenerateAlerts(currentUser.getUsername(), data);
                Toast.makeText(requireContext(), "数据已保存，预警检测完成", Toast.LENGTH_SHORT).show();
            }

            renderChart(data);

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
        hrSet.setColor(Color.RED);
        hrSet.setCircleColor(Color.RED);
        hrSet.setLineWidth(2.5f);
        hrSet.setCircleRadius(3f);
        hrSet.setDrawValues(false);
        hrSet.setDrawCircles(true);
        hrSet.setMode(LineDataSet.Mode.LINEAR);

        LineDataSet stepSet = new LineDataSet(stepEntries, "步频");
        stepSet.setColor(Color.BLUE);
        stepSet.setCircleColor(Color.BLUE);
        stepSet.setLineWidth(2.0f);
        stepSet.setCircleRadius(2.5f);
        stepSet.setDrawValues(false);
        stepSet.setDrawCircles(true);
        stepSet.setMode(LineDataSet.Mode.LINEAR);
        stepSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

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
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);
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
        rightAxis.setTextColor(Color.BLUE);

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
        leftAxis.setTextColor(Color.DKGRAY);
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

        CustomMarkerView mv = new CustomMarkerView(requireContext(), R.layout.layout_marker, data);
        mv.setChartView(lineChart);
        lineChart.setMarker(mv);

        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
        lineChart.moveViewToX(0);
    }

    private void updateStatistics(List<HeartRateEntry> data) {
        if (data == null || data.isEmpty()) {
            tvAvgHr.setText("--");
            tvMaxHr.setText("--");
            tvMinHr.setText("--");
            tvAvgSteps.setText("--");
            tvMaxSteps.setText("--");
            tvMinSteps.setText("--");
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;

        int minStep = Integer.MAX_VALUE;
        int maxStep = Integer.MIN_VALUE;
        long sumStep = 0;

        for (HeartRateEntry entry : data) {
            int bpm = entry.getBpm();
            sum += bpm;
            if (bpm < min) min = bpm;
            if (bpm > max) max = bpm;

            int sf = entry.getStepFrequency();
            if (sf < 0) sf = 0; // 不能小于零
            sumStep += sf;
            if (sf < minStep) minStep = sf;
            if (sf > maxStep) maxStep = sf;
        }

        double avg = (double) sum / data.size();
        double avgStep = (double) sumStep / data.size();
        tvAvgHr.setText(String.format("%.1f", avg));
        tvMaxHr.setText(String.valueOf(max));
        tvMinHr.setText(String.valueOf(min));
        tvAvgSteps.setText(String.format("%.1f", avgStep));
        tvMaxSteps.setText(String.valueOf(maxStep));
        tvMinSteps.setText(String.valueOf(minStep));
    }
}