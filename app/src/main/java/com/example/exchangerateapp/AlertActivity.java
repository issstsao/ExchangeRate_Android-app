package com.example.exchangerateapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.exchangerateapp.utils.PreferenceHelper;
import com.example.exchangerateapp.worker.RateCheckWorker;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class AlertActivity extends AppCompatActivity {

    private Spinner spinnerAlertFrom, spinnerAlertTo;
    private EditText etTargetRate;
    private Switch switchAlert;
    private TextView tvCurrentAlert;
    private Button btnStartAlert, btnStopAlert;

    private PreferenceHelper prefs;
    private String[] currencies = {"USD","TWD","JPY","EUR","GBP","CNY","HKD","KRW","SGD","AUD","CAD","CHF","MYR","THB","VND","PHP"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        prefs = new PreferenceHelper(this);

        initViews();
        setupSpinners();
        loadCurrentSettings();

        btnStartAlert.setOnClickListener(v -> startAlert());
        btnStopAlert.setOnClickListener(v -> stopAlert());
    }

    private void initViews() {
        spinnerAlertFrom = findViewById(R.id.spinnerAlertFrom);
        spinnerAlertTo = findViewById(R.id.spinnerAlertTo);
        etTargetRate = findViewById(R.id.etTargetRate);
        switchAlert = findViewById(R.id.switchAlert);
        tvCurrentAlert = findViewById(R.id.tvCurrentAlert);
        btnStartAlert = findViewById(R.id.btnStartAlert);
        btnStopAlert = findViewById(R.id.btnStopAlert);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlertFrom.setAdapter(adapter);
        spinnerAlertTo.setAdapter(adapter);
    }

    private void loadCurrentSettings() {
        spinnerAlertFrom.setSelection(java.util.Arrays.asList(currencies).indexOf(prefs.getAlertFrom()));
        spinnerAlertTo.setSelection(java.util.Arrays.asList(currencies).indexOf(prefs.getAlertTo()));
        etTargetRate.setText(String.valueOf(prefs.getAlertTargetRate()));
        switchAlert.setChecked(prefs.isAlertEnabled());
        updateAlertStatusText();
    }

    private void startAlert() {
        String from = spinnerAlertFrom.getSelectedItem().toString();
        String to = spinnerAlertTo.getSelectedItem().toString();
        String rateStr = etTargetRate.getText().toString();

        if (rateStr.isEmpty()) {
            Toast.makeText(this, "請輸入目標匯率", Toast.LENGTH_SHORT).show();
            return;
        }

        float targetRate = Float.parseFloat(rateStr);

        prefs.saveAlertSettings(from, to, targetRate, true);
        switchAlert.setChecked(true);

        // 啟動 WorkManager
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                RateCheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("rate_alert_work")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "rate_alert_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest);

        updateAlertStatusText();
        Toast.makeText(this, "✅ 匯率提醒已啟動", Toast.LENGTH_LONG).show();
    }

    private void stopAlert() {
        prefs.disableAlert();
        switchAlert.setChecked(false);
        WorkManager.getInstance(this).cancelUniqueWork("rate_alert_work");
        updateAlertStatusText();
        Toast.makeText(this, "⛔ 匯率提醒已停用", Toast.LENGTH_SHORT).show();
    }

    private void updateAlertStatusText() {
        if (prefs.isAlertEnabled()) {
            tvCurrentAlert.setText("📌 監控中：1 " + prefs.getAlertFrom() + " ≥ " +
                    prefs.getAlertTargetRate() + " " + prefs.getAlertTo());
        } else {
            tvCurrentAlert.setText("目前無啟用監控");
        }
    }
}