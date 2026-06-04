package com.example.exchangerateapp;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // a: 綁定 LineChart 元件
        LineChart a = findViewById(R.id.lineChart);

        // b: 建立圖表的資料點集合 (X 軸: 天數, Y 軸: 匯率模擬數據)
        List<Entry> b = new ArrayList<>();
        b.add(new Entry(1f, 31.20f));
        b.add(new Entry(2f, 31.55f));
        b.add(new Entry(3f, 31.42f));
        b.add(new Entry(4f, 31.80f));
        b.add(new Entry(5f, 32.10f));
        b.add(new Entry(6f, 32.05f));
        b.add(new Entry(7f, 32.30f));

        // c: 將資料點包裝成 DataSet，並設定視覺樣式
        LineDataSet c = new LineDataSet(b, "USD to TWD");
        c.setColor(Color.BLUE);
        c.setCircleColor(Color.BLUE);
        c.setLineWidth(2f);
        c.setValueTextSize(10f);
        c.setValueTextColor(Color.BLACK);

        // d: 設定 X 軸樣式 (顯示在下方)
        XAxis d = a.getXAxis();
        d.setPosition(XAxis.XAxisPosition.BOTTOM);
        d.setGranularity(1f); // X 軸間距為 1

        // e: 將 DataSet 裝入 LineData
        LineData e = new LineData(c);

        // 寫入資料並刷新圖表
        a.setData(e);
        a.getDescription().setEnabled(false); // 隱藏右下角預設描述
        a.animateX(1000); // 加入 X 軸動畫效果
        a.invalidate();
    }
}