package com.example.exchangerateapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ExchangeResponse {
    @SerializedName("conversion_rates")
    private Map<String, Double> conversionRates;

    @SerializedName("rates")
    private Map<String, Double> rates;

    public Map<String, Double> getConversionRates() {
        return conversionRates != null ? conversionRates : rates;
    }
}