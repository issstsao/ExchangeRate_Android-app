package com.example.exchangerateapp.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.exchangerateapp.api.RetrofitClient;
import com.example.exchangerateapp.api.ExchangeRateApi;
import com.example.exchangerateapp.model.ExchangeResponse;
import com.example.exchangerateapp.utils.NotificationHelper;
import com.example.exchangerateapp.utils.PreferenceHelper;
import retrofit2.Response;

public class RateCheckWorker extends Worker {
    public RateCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferenceHelper prefs = new PreferenceHelper(getApplicationContext());
        if (!prefs.isAlertEnabled()) return Result.success();

        String from = prefs.getAlertFrom();
        String to = prefs.getAlertTo();
        float target = prefs.getAlertTargetRate();

        try {
            ExchangeRateApi api = RetrofitClient.getInstance().create(ExchangeRateApi.class);
            Response<ExchangeResponse> response = api.getRates(from).execute();

            if (response.isSuccessful() && response.body() != null) {
                Double rate = response.body().getConversionRates().get(to);
                if (rate != null && rate >= target) {
                    String msg = "1 " + from + " = " + String.format("%.4f", rate) + " " + to +
                            " 已達到目標 " + target;
                    NotificationHelper.showRateAlert(getApplicationContext(), msg);
                    prefs.disableAlert();
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}