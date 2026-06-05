package com.example.exchangerateapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.exchangerateapp.api.HistoryApi;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChartActivity extends AppCompatActivity {

    private static final String TAG = "ChartActivity";
    private LineChart lineChart;
    private TextView tvChartTitle;
    private String fromCurrency, toCurrency;

    private HistoryApi historyApi;
    private Map<String, Double> fetchedRates;
    private int completedRequests = 0;
    private static final int TOTAL_DAYS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.lineChart);
        tvChartTitle = findViewById(R.id.tvChartTitle);

        fromCurrency = getIntent().getStringExtra("FROM_CURRENCY");
        toCurrency = getIntent().getStringExtra("TO_CURRENCY");

        if (fromCurrency == null) fromCurrency = "USD";
        if (toCurrency == null) toCurrency = "TWD";

        tvChartTitle.setText(String.format("過去 7 天走勢: %s → %s", fromCurrency, toCurrency));

        // 初始化 Retrofit (使用開源全貨幣 API)
        // 將原本的 Retrofit 初始化區塊替換為以下這段：
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://cdn.jsdelivr.net/") // 確保這裡是以斜線 / 結尾
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        historyApi = retrofit.create(HistoryApi.class);

        fetchAllHistoricalData();
    }

    private void fetchAllHistoricalData() {
        fetchedRates = new HashMap<>();
        completedRequests = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();

        // 往前推算 7 天的日期，並發出 7 次請求
        for (int i = 0; i < TOTAL_DAYS; i++) {
            String dateStr = sdf.format(calendar.getTime());
            fetchSingleDayRate(dateStr);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void fetchSingleDayRate(String dateStr) {
        // 此 API 的貨幣代碼必須為小寫
        String baseCurrency = fromCurrency.toLowerCase();
        String targetCurrency = toCurrency.toLowerCase();

        historyApi.getHistoricalRate(dateStr, baseCurrency).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject ratesObject = body.getAsJsonObject(baseCurrency);

                        if (ratesObject != null && ratesObject.has(targetCurrency)) {
                            double rate = ratesObject.get(targetCurrency).getAsDouble();
                            fetchedRates.put(dateStr, rate); // 將成功取得的匯率存入 Map
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析 JSON 錯誤: " + dateStr, e);
                    }
                }
                checkCompletion();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "API 請求失敗: " + dateStr, t);
                checkCompletion();
            }
        });
    }

    // 檢查是否 7 次請求都已回應
    private void checkCompletion() {
        completedRequests++;
        if (completedRequests == TOTAL_DAYS) {
            if (fetchedRates.isEmpty()) {
                Toast.makeText(this, "無法取得此貨幣組合的歷史資料", Toast.LENGTH_SHORT).show();
            } else {
                processAndDisplayData();
            }
        }
    }

    private void processAndDisplayData() {
        // 將日期排序 (確保 X 軸時間從舊到新)
        List<String> sortedDates = new ArrayList<>(fetchedRates.keySet());
        Collections.sort(sortedDates);

        List<Entry> chartEntries = new ArrayList<>();
        List<String> xAxisLabels = new ArrayList<>();

        int index = 0;
        for (String date : sortedDates) {
            Double rate = fetchedRates.get(date);
            if (rate != null) {
                chartEntries.add(new Entry(index, rate.floatValue()));
                xAxisLabels.add(date.substring(5)); // 取 MM-dd 格式
                index++;
            }
        }

        LineDataSet dataSet = new LineDataSet(chartEntries, fromCurrency + " to " + toCurrency);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
}