package com.example.exchangerateapp.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface HistoryApi {
    // Frankfurter API 歷史區間格式: /{startDate}..{endDate}?from=USD&to=JPY
//    @GET("{startDate}..{endDate}")
//    Call<HistoricalResponse> getHistoricalRates(
//            @Path("startDate") String startDate,
//            @Path("endDate") String endDate,
//            @Query("from") String fromCurrency,
//            @Query("to") String toCurrency
//    );

    // 新的 API 格式: /{date}/v1/currencies/{base}.json
    @GET("npm/@fawazahmed0/currency-api@{date}/v1/currencies/{base}.json")
    Call<JsonObject> getHistoricalRate(
            @Path("date") String date,
            @Path("base") String baseCurrency
    );
}