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

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i).getBpm()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "心率");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(data.size(), 8), true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < data.size()) {
                    String fullTime = data.get(index).getTimestamp();
                    if (fullTime.length() > 16) return fullTime.substring(11, 16);
                    return fullTime;
                }
                return "";
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        float minVal = dataSet.getYMin();
        float maxVal = dataSet.getYMax();
        if (minVal == maxVal) {
            minVal -= 10;
            maxVal += 10;
        }
        leftAxis.setAxisMinimum(minVal - 10);
        leftAxis.setAxisMaximum(maxVal + 10);
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setGranularity(1f);
        lineChart.getAxisRight().setEnabled(false);

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setVisibleXRangeMaximum(30f);

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
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long sum = 0;

        for (HeartRateEntry entry : data) {
            int bpm = entry.getBpm();
            sum += bpm;
            if (bpm < min) min = bpm;
            if (bpm > max) max = bpm;
        }

        double avg = (double) sum / data.size();
        tvAvgHr.setText(String.format("%.1f", avg));
        tvMaxHr.setText(String.valueOf(max));
        tvMinHr.setText(String.valueOf(min));
    }
}