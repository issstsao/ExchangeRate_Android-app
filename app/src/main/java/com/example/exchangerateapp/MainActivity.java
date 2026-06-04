package com.example.exchangerateapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.exchangerateapp.api.*;
import com.example.exchangerateapp.model.*;
import com.example.exchangerateapp.utils.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "ExchangeRateApp";

    private Spinner spinnerFrom, spinnerTo;
    private EditText etAmount;
    private Button btnConvert, btnSwap;
    private TextView tvResult, tvRate, tvLastUpdate;
    private ImageButton btnFavorite; // 新增最愛按鈕變數

    private SensorManager sensorManager;
    private long lastShakeTime = 0;
    private static final float SHAKE_THRESHOLD = 12.0f;

    private ExchangeRateApi api;
    private PreferenceHelper prefs;
    private FirebaseHelper firebaseHelper;

    private String[] currencies = {"USD","TWD","JPY","EUR","GBP","CNY","HKD","KRW","SGD","AUD","CAD","CHF","MYR","THB","VND","PHP"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferenceHelper(this);
        firebaseHelper = new FirebaseHelper(prefs.getUserId());
        NotificationHelper.createNotificationChannel(this);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        initViews();
        setupSpinners();
        setupFavoriteButton(); // 呼叫新增的最愛按鈕綁定方法
        api = RetrofitClient.getInstance().create(ExchangeRateApi.class);

        loadLastCurrencies();

        btnConvert.setOnClickListener(v -> convertCurrency());
        btnSwap.setOnClickListener(v -> swapCurrencies());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Log.d(TAG, "MainActivity 初始化完成");
    }

    private void initViews() {
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etAmount = findViewById(R.id.etAmount);
        btnConvert = findViewById(R.id.btnConvert);
        btnSwap = findViewById(R.id.btnSwap);
        tvResult = findViewById(R.id.tvResult);
        tvRate = findViewById(R.id.tvRate);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        btnFavorite = findViewById(R.id.btnFavorite); // 初始化按鈕
    }

    // 整合 SharedPreferences 的新版 Spinner 設定
    private void setupSpinners() {
        SharedPreferences sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favorites = sharedPrefs.getStringSet("favorites", new HashSet<>());
        List<String> currencyList = new ArrayList<>();

        // 先加入最愛
        for (String fav : favorites) {
            currencyList.add("⭐ " + fav);
        }

        // 再加入非最愛
        for (String currency : currencies) {
            if (!favorites.contains(currency)) {
                currencyList.add(currency);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
    }

    // 新增最愛按鈕的點擊邏輯
    private void setupFavoriteButton() {
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                String selectedCurrency = spinnerFrom.getSelectedItem().toString().replace("⭐ ", "");
                SharedPreferences sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                Set<String> favorites = new HashSet<>(sharedPrefs.getStringSet("favorites", new HashSet<>()));
                SharedPreferences.Editor editor = sharedPrefs.edit();

                if (favorites.contains(selectedCurrency)) {
                    favorites.remove(selectedCurrency); // 若已存在則取消最愛
                    Toast.makeText(this, "已移除最愛", Toast.LENGTH_SHORT).show();
                } else {
                    favorites.add(selectedCurrency);    // 若不存在則加入最愛
                    Toast.makeText(this, "已加入最愛", Toast.LENGTH_SHORT).show();
                }

                editor.putStringSet("favorites", favorites);
                editor.apply();

                // 重新載入 Spinner 以更新順序，並套用原來的選擇
                setupSpinners();
                loadLastCurrencies();
            });
        }
    }

    // 更新以動態尋找 Index 的方法，避免因為加入最愛而錯位
    private void loadLastCurrencies() {
        String lastFrom = prefs.getLastFrom();
        String lastTo = prefs.getLastTo();
        setSpinnerSelection(spinnerFrom, lastFrom, 0);
        setSpinnerSelection(spinnerTo, lastTo, 1);
    }

    // 輔助方法：根據貨幣字串找尋 Adapter 內的正確 Index
    private void setSpinnerSelection(Spinner targetSpinner, String targetCurrency, int defaultIndex) {
        if (targetCurrency == null) {
            targetSpinner.setSelection(defaultIndex);
            return;
        }

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) targetSpinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                String item = adapter.getItem(i);
                if (item != null && item.replace("⭐ ", "").equals(targetCurrency)) {
                    targetSpinner.setSelection(i);
                    return;
                }
            }
        }
        targetSpinner.setSelection(defaultIndex);
    }

    private void convertCurrency() {
        // 重要：必須去掉 "⭐ " 以防 API 吃不到正確幣值
        String from = spinnerFrom.getSelectedItem().toString().replace("⭐ ", "");
        String to = spinnerTo.getSelectedItem().toString().replace("⭐ ", "");
        String amountStr = etAmount.getText().toString().trim();

        Log.d(TAG, "換算請求: " + from + " → " + to + " 金額=" + amountStr);

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "請輸入金額", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            Toast.makeText(this, "金額格式錯誤", Toast.LENGTH_SHORT).show();
            return;
        }

        api.getRates(from).enqueue(new Callback<ExchangeResponse>() {
            @Override
            public void onResponse(Call<ExchangeResponse> call, Response<ExchangeResponse> response) {
                Log.d(TAG, "API 回應碼: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getConversionRates();
                    Double rate = rates != null ? rates.get(to) : null;

                    if (rate != null) {
                        double result = amount * rate;
                        tvResult.setText(String.format("%.2f %s = %.2f %s", amount, from, result, to));
                        tvRate.setText(String.format("1 %s = %.4f %s", from, rate, to));
                        tvLastUpdate.setText("更新時間: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.TAIWAN).format(new Date()));

                        ConversionRecord record = new ConversionRecord(from, to, amount, result, rate);
                        firebaseHelper.saveConversionRecord(record);
                        prefs.saveLastCurrencies(from, to);

                        Toast.makeText(MainActivity.this, "✅ 換算成功！", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "換算成功: " + result);
                    } else {
                        Toast.makeText(MainActivity.this, "無法取得匯率", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "API 請求失敗 (" + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeResponse> call, Throwable t) {
                Log.e(TAG, "API 失敗", t);
                Toast.makeText(MainActivity.this, "網路錯誤: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void swapCurrencies() {
        int pos1 = spinnerFrom.getSelectedItemPosition();
        int pos2 = spinnerTo.getSelectedItemPosition();
        spinnerFrom.setSelection(pos2);
        spinnerTo.setSelection(pos1);
        convertCurrency();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_alert) startActivity(new Intent(this, AlertActivity.class));
        else if (id == R.id.menu_webview) startActivity(new Intent(this, WebViewActivity.class));
        else if (id == R.id.menu_history) startActivity(new Intent(this, HistoryActivity.class));
        return super.onOptionsItemSelected(item);
    }

    // Shake detection
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastShakeTime < 1500) return;

            float x = event.values[0], y = event.values[1], z = event.values[2];
            float acceleration = (float) Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                lastShakeTime = currentTime;
                etAmount.setText("");
                tvResult.setText("");
                tvRate.setText("");
                Toast.makeText(this, "🔄 已搖晃清除所有資料", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}