package com.example.exchangerateapp.api;

import com.example.exchangerateapp.model.ExchangeResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    Call<ExchangeResponse> getRates(@Path("base") String baseCurrency);
}