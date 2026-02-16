package com.example.smart_wearable_warning_platform;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_wearable_warning_platform.model.DataManager;
import com.example.smart_wearable_warning_platform.model.HeartRateEntry;
import com.example.smart_wearable_warning_platform.model.User;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentActivity extends AppCompatActivity {

    private DataManager dataManager;
    private Button btnUpload, btnLogout;
    private LineChart lineChart;
    private TextView tvAvgHr, tvMaxHr, tvMinHr;

    // 输入格式：2025-12-14 18:20:47
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    // 输出格式：18:21 (只精确到分钟)
    private final SimpleDateFormat axisDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final ActivityResultLauncher<String> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processCsvFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        tvAvgHr = findViewById(R.id.tv_avg_hr);
        tvMaxHr = findViewById(R.id.tv_max_hr);
        tvMinHr = findViewById(R.id.tv_min_hr);

        dataManager = new DataManager(this);
        btnUpload = findViewById(R.id.btn_upload_csv);
        btnLogout = findViewById(R.id.btn_logout);
        lineChart = findViewById(R.id.line_chart);

        btnUpload.setOnClickListener(v -> {
            csvPickerLauncher.launch("*/*");
        });

        btnLogout.setOnClickListener(v -> {
            dataManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        loadSavedData();
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
            InputStream inputStream = getContentResolver().openInputStream(uri);
            List<HeartRateEntry> data = dataManager.parseCSV(inputStream);
            inputStream.close();

            if (data.isEmpty()) {
                Toast.makeText(this, "CSV文件为空或格式错误", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取当前登录用户
            User currentUser = dataManager.getCurrentUser();
            if (currentUser != null) {
                // 1. 保存心率数据（用于学生端显示）
                dataManager.saveHeartRateData(currentUser.getUsername(), data);

                // --- 核心新增：自动检查并生成预警 ---
                // 当数据导入时，立即调用检查逻辑，匹配管理员设定的阈值
                dataManager.checkAndGenerateAlerts(currentUser.getUsername(), data);

                Toast.makeText(this, "数据已保存，预警检测完成", Toast.LENGTH_SHORT).show();
            }

            renderChart(data);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "文件读取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void renderChart(List<HeartRateEntry> data) {
        // 1. 在绘制图表之前，先计算并显示统计数据
        updateStatistics(data);

        // 2. 准备图表数据
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

        // 3. X 轴设置
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

        // 4. Y 轴设置
        YAxis leftAxis = lineChart.getAxisLeft();
        float minVal = dataSet.getYMin();
        float maxVal = dataSet.getYMax();

        // 计算Y轴范围 (保留之前的逻辑)
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

        // 5. 其他设置
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setVisibleXRangeMaximum(30f);

        // 刷新
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
        lineChart.moveViewToX(0);

        // 6. 设置 Marker (保留之前的逻辑)
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.layout_marker, data);
        mv.setChartView(lineChart);
        lineChart.setMarker(mv);
    }

    // --- 新增方法：计算统计数据 ---
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

        tvAvgHr.setText(String.format("%.1f", avg)); // 保留一位小数
        tvMaxHr.setText(String.valueOf(max));
        tvMinHr.setText(String.valueOf(min));
    }


}
