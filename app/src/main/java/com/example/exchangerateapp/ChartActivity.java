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

    private static final String a = "ChartActivity";
    private LineChart b;
    private TextView c;
    private String d, e;

    private HistoryApi f;
    private Map<String, Double> g;
    private int h = 0;
    private static final int i = 7;

    @Override
    protected void onCreate(Bundle j) {
        super.onCreate(j);
        setContentView(R.layout.activity_chart);

        b = findViewById(R.id.lineChart);
        c = findViewById(R.id.tvChartTitle);

        d = getIntent().getStringExtra("FROM_CURRENCY");
        e = getIntent().getStringExtra("TO_CURRENCY");

        if (d == null) d = "USD";
        if (e == null) e = "TWD";

        c.setText(String.format("過去 7 天走勢: %s → %s", d, e));

        Retrofit k = new Retrofit.Builder()
                .baseUrl("https://cdn.jsdelivr.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        f = k.create(HistoryApi.class);

        fetchAllHistoricalData();
    }

    private void fetchAllHistoricalData() {
        g = new HashMap<>();
        h = 0;

        SimpleDateFormat l = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar m = Calendar.getInstance();

        for (int n = 0; n < i; n++) {
            String o = l.format(m.getTime());
            fetchSingleDayRate(o);
            m.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void fetchSingleDayRate(String p) {
        String q = d.toLowerCase();
        String r = e.toLowerCase();

        f.getHistoricalRate(p, q).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> s, Response<JsonObject> t) {
                if (t.isSuccessful() && t.body() != null) {
                    try {
                        JsonObject u = t.body();
                        JsonObject v = u.getAsJsonObject(q);

                        if (v != null && v.has(r)) {
                            double w = v.get(r).getAsDouble();
                            g.put(p, w);
                        }
                    } catch (Exception x) {
                        Log.e(a, "解析 JSON 錯誤: " + p, x);
                    }
                }
                checkCompletion();
            }

            @Override
            public void onFailure(Call<JsonObject> y, Throwable z) {
                Log.e(a, "API 請求失敗: " + p, z);
                checkCompletion();
            }
        });
    }

    private void checkCompletion() {
        h++;
        if (h == i) {
            if (g.isEmpty()) {
                Toast.makeText(this, "無法取得此貨幣組合的歷史資料", Toast.LENGTH_SHORT).show();
            } else {
                processAndDisplayData();
            }
        }
    }

    private void processAndDisplayData() {
        List<String> aa = new ArrayList<>(g.keySet());
        Collections.sort(aa);

        List<Entry> ab = new ArrayList<>();
        List<String> ac = new ArrayList<>();

        int ad = 0;
        for (String ae : aa) {
            Double af = g.get(ae);
            if (af != null) {
                ab.add(new Entry(ad, af.floatValue()));
                ac.add(ae.substring(5));
                ad++;
            }
        }

        LineDataSet ag = new LineDataSet(ab, d + " to " + e);

        // --- FinTech 明亮系圖表外觀客製化 ---
        ag.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 啟用流暢的貝茲平滑曲線
        ag.setColor(Color.parseColor("#00B074"));  // 優雅金融綠線條
        ag.setCircleColor(Color.parseColor("#00B074"));
        ag.setCircleRadius(4f);
        ag.setDrawCircleHole(true);                // 開啟空心圓點設計，增加精緻感
        ag.setCircleHoleColor(Color.WHITE);
        ag.setLineWidth(3f);

        // 設定線條下方區域的淺色輕盈漸層填滿
        ag.setDrawFilled(true);
        ag.setFillColor(Color.parseColor("#00B074"));
        ag.setFillAlpha(25);

        // 數據點文字與字型大小設定
        ag.setValueTextColor(Color.parseColor("#718096"));
        ag.setValueTextSize(10f);

        // X 軸淺色樣式客製化
        XAxis ah = b.getXAxis();
        ah.setPosition(XAxis.XAxisPosition.BOTTOM);
        ah.setGranularity(1f);
        ah.setValueFormatter(new IndexAxisValueFormatter(ac));
        ah.setTextColor(Color.parseColor("#718096"));
        ah.setGridColor(Color.parseColor("#E2E8F0")); // 輕量化網格線
        ah.setDrawAxisLine(false);                   // 隱藏粗軸線，展現極簡感

        // 左側 Y 軸淺色樣式客製化
        b.getAxisLeft().setTextColor(Color.parseColor("#718096"));
        b.getAxisLeft().setGridColor(Color.parseColor("#E2E8F0"));
        b.getAxisLeft().setDrawAxisLine(false);

        // 隱藏右側 Y 軸、圖例與內建描述，貼近國際主流金融科技介面
        b.getAxisRight().setEnabled(false);
        b.getLegend().setEnabled(false);
        b.getDescription().setEnabled(false);

        // 優化縮放與手勢操作
        b.setScaleEnabled(false);
        b.setTouchEnabled(true);
        b.setBackgroundColor(Color.parseColor("#F8F9FC")); // 設定圖表內部淺色背景

        LineData ai = new LineData(ag);
        b.setData(ai);
        b.animateX(900); // 橫向動態渲染動畫
        b.invalidate();
    }
}