package com.example.exchangerateapp.model;

import com.google.firebase.Timestamp;

public class ConversionRecord {
    private String fromCurrency;
    private String toCurrency;
    private double inputAmount;
    private double outputAmount;
    private double rate;
    private Timestamp timestamp;

    public ConversionRecord() {} // Firebase 需要無參數建構子

    public ConversionRecord(String from, String to, double input, double output, double rate) {
        this.fromCurrency = from;
        this.toCurrency = to;
        this.inputAmount = input;
        this.outputAmount = output;
        this.rate = rate;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public double getInputAmount() { return inputAmount; }
    public void setInputAmount(double inputAmount) { this.inputAmount = inputAmount; }

    public double getOutputAmount() { return outputAmount; }
    public void setOutputAmount(double outputAmount) { this.outputAmount = outputAmount; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}