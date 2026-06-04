package com.example.exchangerateapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class HistoricalResponse {
    @SerializedName("rates")
    private Map<String, Map<String, Double>> rates;

    public Map<String, Map<String, Double>> getRates() {
        return rates;
    }
}