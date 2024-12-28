package com.example.currencyconverter.api;

import java.util.Map;
import com.google.gson.annotations.SerializedName;

public class ExchangeRateResponse {
    @SerializedName("conversion_rates")
    private Map<String, Double> conversion_rates;

    public Map<String, Double> getRates() {
        return conversion_rates;
    }
}
