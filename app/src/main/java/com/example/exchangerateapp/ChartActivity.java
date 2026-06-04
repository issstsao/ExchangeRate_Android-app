package com.example.exchangerateapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.exchangerateapp.api.HistoryApi;
import com.example.exchangerateapp.model.HistoricalResponse;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.lineChart);
        tvChartTitle = findViewById(R.id.tvChartTitle); // 建議在 XML 中給標題 TextView 一個 ID

        // 接收 MainActivity 傳來的貨幣
        fromCurrency = getIntent().getStringExtra("FROM_CURRENCY");
        toCurrency = getIntent().getStringExtra("TO_CURRENCY");

        if (fromCurrency == null) fromCurrency = "USD";
        if (toCurrency == null) toCurrency = "EUR";

        tvChartTitle.setText(String.format("過去 7 天走勢: %s → %s", fromCurrency, toCurrency));

        // 判斷是否包含不支援的 TWD
        if ("TWD".equals(fromCurrency) || "TWD".equals(toCurrency)) {
            Toast.makeText(this, "API 不支援 TWD 歷史資料，切換為模擬數據", Toast.LENGTH_SHORT).show();
            loadMockDataForTWD();
        } else {
            fetchRealHistoricalData(); // 呼叫真實 API
        }
    }

    private void fetchRealHistoricalData() {
        // 計算日期區間 (今天與 7 天前)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        String endDate = sdf.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = sdf.format(calendar.getTime());

        // 建立專屬的 Retrofit 實體 (因為 Base URL 不同)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.frankfurter.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HistoryApi historyApi = retrofit.create(HistoryApi.class);

        // 發送請求
        historyApi.getHistoricalRates(startDate, endDate, fromCurrency, toCurrency)
                .enqueue(new Callback<HistoricalResponse>() {
                    @Override
                    public void onResponse(Call<HistoricalResponse> call, Response<HistoricalResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Map<String, Double>> ratesMap = response.body().getRates();
                            if (ratesMap != null && !ratesMap.isEmpty()) {
                                processAndDisplayData(ratesMap);
                            } else {
                                Toast.makeText(ChartActivity.this, "該區間無匯率資料", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 若選到 TWD 會觸發此處，因為 API 不支援
                            Toast.makeText(ChartActivity.this, "API 請求失敗，可能不支援此貨幣組合", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<HistoricalResponse> call, Throwable t) {
                        Log.e(TAG, "獲取歷史資料失敗", t);
                        Toast.makeText(ChartActivity.this, "網路連線錯誤", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processAndDisplayData(Map<String, Map<String, Double>> ratesMap) {
        // 提取日期並排序 (確保 X 軸時間順序正確)
        List<String> sortedDates = new ArrayList<>(ratesMap.keySet());
        Collections.sort(sortedDates);

        List<Entry> chartEntries = new ArrayList<>();
        List<String> xAxisLabels = new ArrayList<>();

        int index = 0;
        for (String date : sortedDates) {
            Double rate = ratesMap.get(date).get(toCurrency);
            if (rate != null) {
                chartEntries.add(new Entry(index, rate.floatValue()));
                // 取 MM-dd 格式作為 X 軸標籤
                xAxisLabels.add(date.substring(5));
                index++;
            }
        }

        // 設定資料集外觀
        LineDataSet dataSet = new LineDataSet(chartEntries, fromCurrency + " to " + toCurrency);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);

        // 設定 X 軸格式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        // 將索引 (0,1,2...) 替換為實際的日期字串
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate(); // 刷新圖表
    }

    private void loadMockDataForTWD() {
        // a: 建立圖表的資料點集合
        List<Entry> a = new ArrayList<>();
        a.add(new Entry(0, 31.20f));
        a.add(new Entry(1, 31.55f));
        a.add(new Entry(2, 31.42f));
        a.add(new Entry(3, 31.80f));
        a.add(new Entry(4, 32.10f));
        a.add(new Entry(5, 32.05f));
        a.add(new Entry(6, 32.30f));

        // b: 建立 X 軸的假日期標籤
        List<String> b = new ArrayList<>();
        b.add("05-30"); b.add("05-31"); b.add("06-01");
        b.add("06-02"); b.add("06-03"); b.add("06-04"); b.add("06-05");

        // c: 將資料點包裝成 DataSet
        LineDataSet c = new LineDataSet(a, fromCurrency + " to " + toCurrency + " (模擬)");
        c.setColor(Color.BLUE);
        c.setCircleColor(Color.BLUE);
        c.setLineWidth(2f);
        c.setValueTextSize(10f);
        c.setValueTextColor(Color.BLACK);

        // d: 設定 X 軸樣式
        XAxis d = lineChart.getXAxis();
        d.setPosition(XAxis.XAxisPosition.BOTTOM);
        d.setGranularity(1f);
        d.setValueFormatter(new IndexAxisValueFormatter(b));

        // e: 將 DataSet 裝入 LineData
        LineData e = new LineData(c);

        lineChart.setData(e);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
}