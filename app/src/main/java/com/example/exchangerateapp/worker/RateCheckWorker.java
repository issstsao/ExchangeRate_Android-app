package com.example.exchangerateapp.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.exchangerateapp.api.HistoryApi;
import com.example.exchangerateapp.api.RetrofitClient;
import com.example.exchangerateapp.api.ExchangeRateApi;
import com.example.exchangerateapp.model.ExchangeResponse;
import com.example.exchangerateapp.utils.NotificationHelper;
import com.example.exchangerateapp.utils.PreferenceHelper;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
            // 1. 取得今日最新匯率
            ExchangeRateApi api = RetrofitClient.getInstance().create(ExchangeRateApi.class);
            Response<ExchangeResponse> response = api.getRates(from).execute();

            if (response.isSuccessful() && response.body() != null) {
                Double rate = response.body().getConversionRates().get(to);

                // 若匯率達到使用者設定的目標值
                if (rate != null && rate >= target) {

                    String notificationBody = "🎯 " + to + " 已經達到你的目標匯率！ (目前: " + String.format(Locale.getDefault(), "%.4f", rate) + ")";

                    // 2. 為了迷因推播，同步去抓取昨日的歷史匯率來做趨勢判斷
                    try {
                        Retrofit historyRetrofit = new Retrofit.Builder()
                                .baseUrl("https://cdn.jsdelivr.net/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
                        HistoryApi historyApi = historyRetrofit.create(HistoryApi.class);

                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, -1);
                        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

                        Response<JsonObject> historyResponse = historyApi.getHistoricalRate(yesterday, from.toLowerCase()).execute();

                        if (historyResponse.isSuccessful() && historyResponse.body() != null) {
                            JsonObject ratesObj = historyResponse.body().getAsJsonObject(from.toLowerCase());
                            if (ratesObj != null && ratesObj.has(to.toLowerCase())) {
                                double yesterdayRate = ratesObj.get(to.toLowerCase()).getAsDouble();

                                // 全貨幣動態判斷趨勢
                                if (rate < yesterdayRate) {
                                    notificationBody = "📉 " + to + " 跌到目標價啦！大特價中，現在不買你要等到何時？ (目前: " + String.format(Locale.getDefault(), "%.4f", rate) + ")";
                                } else if (rate > yesterdayRate) {
                                    notificationBody = "📈 " + to + " 漲到目標價啦！該出手了，忍住不換會後悔！ (目前: " + String.format(Locale.getDefault(), "%.4f", rate) + ")";
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 若歷史 API 抓取失敗，就忽略並使用預設的通知文案
                        e.printStackTrace();
                    }

                    // 觸發推播並關閉警報
                    NotificationHelper.showRateAlert(getApplicationContext(), notificationBody);
                    prefs.disableAlert();
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}