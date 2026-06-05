package com.example.exchangerateapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.exchangerateapp.api.*;
import com.example.exchangerateapp.model.*;
import com.example.exchangerateapp.utils.*;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ExchangeRateApp";

    private Spinner spinnerFrom, spinnerTo;
    private EditText etAmount;
    private Button btnConvert, btnSwap, btnChart;
    private TextView tvResult, tvRate, tvLastUpdate;
    private ImageButton btnFavorite;

    private ExchangeRateApi api;
    private PreferenceHelper prefs;
    private FirebaseHelper firebaseHelper;

    // 趣味卡片元件
    private LinearLayout llFunFacts;
    private TextView tvFunFact1, tvFunFact2, tvFunFact3;

    // 彩帶特效元件
    private nl.dionsegijn.konfetti.KonfettiView viewKonfetti;

    // 儲存趨勢判斷後的彩蛋訊息
    private String easterEggMessage = "請先進行換算，讓我為你分析最新趨勢 👀";

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
        setupFavoriteButton();
        api = RetrofitClient.getInstance().create(ExchangeRateApi.class);

        loadLastCurrencies();

        btnConvert.setOnClickListener(v -> convertCurrency());
        btnSwap.setOnClickListener(v -> swapCurrencies());

        btnChart.setOnClickListener(v -> {
            String fromCurrency = spinnerFrom.getSelectedItem().toString().replace("⭐ ", "");
            String toCurrency = spinnerTo.getSelectedItem().toString().replace("⭐ ", "");

            Intent intent = new Intent(MainActivity.this, ChartActivity.class);
            intent.putExtra("FROM_CURRENCY", fromCurrency);
            intent.putExtra("TO_CURRENCY", toCurrency);
            startActivity(intent);
        });

        // 升級版：長按不清空資料，直接跳出根據趨勢判斷的嘲諷彩蛋
        tvResult.setOnLongClickListener(v -> {
            Toast.makeText(MainActivity.this, easterEggMessage, Toast.LENGTH_LONG).show();
            return true;
        });

        Log.d(TAG, "MainActivity 初始化完成");
    }

    private void initViews() {
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etAmount = findViewById(R.id.etAmount);
        btnConvert = findViewById(R.id.btnConvert);
        btnSwap = findViewById(R.id.btnSwap);
        btnChart = findViewById(R.id.btnChart);
        tvResult = findViewById(R.id.tvResult);
        tvRate = findViewById(R.id.tvRate);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        btnFavorite = findViewById(R.id.btnFavorite);

        llFunFacts = findViewById(R.id.llFunFacts);
        tvFunFact1 = findViewById(R.id.tvFunFact1);
        tvFunFact2 = findViewById(R.id.tvFunFact2);
        tvFunFact3 = findViewById(R.id.tvFunFact3);

        viewKonfetti = findViewById(R.id.viewKonfetti);
    }

    private void setupSpinners() {
        SharedPreferences sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> favorites = sharedPrefs.getStringSet("favorites", new HashSet<>());
        List<String> currencyList = new ArrayList<>();

        for (String fav : favorites) {
            currencyList.add("⭐ " + fav);
        }

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

    private void setupFavoriteButton() {
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                String selectedCurrency = spinnerFrom.getSelectedItem().toString().replace("⭐ ", "");
                SharedPreferences sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                Set<String> favorites = new HashSet<>(sharedPrefs.getStringSet("favorites", new HashSet<>()));
                SharedPreferences.Editor editor = sharedPrefs.edit();

                if (favorites.contains(selectedCurrency)) {
                    favorites.remove(selectedCurrency);
                    Toast.makeText(this, "已移除最愛", Toast.LENGTH_SHORT).show();
                } else {
                    favorites.add(selectedCurrency);
                    Toast.makeText(this, "已加入最愛", Toast.LENGTH_SHORT).show();
                }

                editor.putStringSet("favorites", favorites);
                editor.apply();

                setupSpinners();
                loadLastCurrencies();
            });
        }
    }

    private void loadLastCurrencies() {
        String lastFrom = prefs.getLastFrom();
        String lastTo = prefs.getLastTo();
        setSpinnerSelection(spinnerFrom, lastFrom, 0);
        setSpinnerSelection(spinnerTo, lastTo, 1);
    }

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
        String from = spinnerFrom.getSelectedItem().toString().replace("⭐ ", "");
        String to = spinnerTo.getSelectedItem().toString().replace("⭐ ", "");
        String amountStr = etAmount.getText().toString().trim();

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

        // 彩蛋：百萬富翁「黃金彩帶雨」特效觸發
        if (amount >= 1000000 && viewKonfetti != null) {
            Toast.makeText(this, "🎉 財富自由啦！！！", Toast.LENGTH_LONG).show();

            // 觸發純程式碼生成的彩帶特效
            viewKonfetti.build()
                    .addColors(
                            android.graphics.Color.YELLOW,
                            android.graphics.Color.parseColor("#FFD700"), // 金色
                            android.graphics.Color.parseColor("#FFA500")  // 橘金色
                    )
                    .setDirection(0.0, 359.0) // 360 度四面八方噴射
                    .setSpeed(1f, 5f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(2000L) // 彩帶在螢幕上存活時間
                    .addShapes(
                            nl.dionsegijn.konfetti.models.Shape.Square.INSTANCE,
                            nl.dionsegijn.konfetti.models.Shape.Circle.INSTANCE
                    )
                    .addSizes(new nl.dionsegijn.konfetti.models.Size(12, 5f))
                    .setPosition(-50f, viewKonfetti.getWidth() + 50f, -50f, -50f) // 從上方落下
                    .streamFor(300, 5000L); // 產生 300 個彩帶，持續下雨 5 秒鐘
        }

        // 每次重新換算時，先將彩蛋訊息重置
        easterEggMessage = "趨勢分析中，請稍後再試 🔍";

        api.getRates(from).enqueue(new Callback<ExchangeResponse>() {
            @Override
            public void onResponse(Call<ExchangeResponse> call, Response<ExchangeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getConversionRates();
                    Double rate = rates != null ? rates.get(to) : null;

                    if (rate != null) {
                        double result = amount * rate;

                        Double twdRate = rates.get("TWD");
                        if (twdRate != null && llFunFacts != null) {
                            double amountInTwd = amount * (twdRate / rates.get(from));
                            int priceBigMac = 75, priceBoba = 50, priceUSJ = 2000;
                            tvFunFact1.setText(String.format(Locale.getDefault(), "🍔 ≈ %.1f 個大麥克", amountInTwd / priceBigMac));
                            tvFunFact2.setText(String.format(Locale.getDefault(), "🧋 ≈ %.1f 杯五十嵐一號", amountInTwd / priceBoba));
                            tvFunFact3.setText(String.format(Locale.getDefault(), "🎢 ≈ %.1f 張環球影城門票", amountInTwd / priceUSJ));
                            llFunFacts.setVisibility(View.VISIBLE);
                        }

                        tvResult.setText(String.format(Locale.getDefault(), "%.2f %s = %.2f %s", amount, from, result, to));
                        tvRate.setText(String.format(Locale.getDefault(), "1 %s = %.4f %s", from, rate, to));
                        tvLastUpdate.setText("更新時間: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.TAIWAN).format(new Date()));

                        ConversionRecord record = new ConversionRecord(from, to, amount, result, rate);
                        firebaseHelper.saveConversionRecord(record);
                        prefs.saveLastCurrencies(from, to);

                        // 觸發背景分析趨勢 (取得昨日匯率做比較)
                        analyzeTrendForEasterEgg(from, to, rate);

                        Toast.makeText(MainActivity.this, "✅ 換算成功！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "無法取得匯率", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "API 請求失敗", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "網路錯誤", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 專門用來分析趨勢以決定彩蛋文案的非同步方法
    private void analyzeTrendForEasterEgg(String from, String to, double currentRate) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://cdn.jsdelivr.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HistoryApi historyApi = retrofit.create(HistoryApi.class);

        // 取得昨天的日期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

        historyApi.getHistoricalRate(yesterday, from.toLowerCase()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject ratesObj = response.body().getAsJsonObject(from.toLowerCase());
                        if (ratesObj != null && ratesObj.has(to.toLowerCase())) {
                            double yesterdayRate = ratesObj.get(to.toLowerCase()).getAsDouble();

                            // 判斷趨勢並賦予情緒化文案
                            if (currentRate < yesterdayRate) {
                                easterEggMessage = "📉 " + to + " 正在下跌！大特價中，現在不買你要等到何時？";
                            } else if (currentRate > yesterdayRate) {
                                easterEggMessage = "📈 " + to + " 正在上漲！現在換可能有點虧，忍住你的雙手！";
                            } else {
                                easterEggMessage = "⚖️ " + to + " 匯率平如止水，跟你的錢包一樣毫無波動 😶";
                            }
                        }
                    } catch (Exception e) {
                        easterEggMessage = "🤔 趨勢分析中發生了一點小錯誤";
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                easterEggMessage = "📶 網路不給力，目前無法分析趨勢";
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
}