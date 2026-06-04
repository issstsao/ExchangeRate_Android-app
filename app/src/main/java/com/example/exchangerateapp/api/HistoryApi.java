package com.example.exchangerateapp.api;

import com.example.exchangerateapp.model.HistoricalResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HistoryApi {
    // Frankfurter API 歷史區間格式: /{startDate}..{endDate}?from=USD&to=JPY
    @GET("{startDate}..{endDate}")
    Call<HistoricalResponse> getHistoricalRates(
            @Path("startDate") String startDate,
            @Path("endDate") String endDate,
            @Query("from") String fromCurrency,
            @Query("to") String toCurrency
    );
}