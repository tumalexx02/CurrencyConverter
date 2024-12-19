package com.example.currencyconverter.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ExchangeRateAPI {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    Call<ExchangeRateResponse> getLatestRates(
            @Path("apiKey") String apiKey,
            @Path("baseCurrency") String baseCurrency
    );

    @GET("v6/{apiKey}/history/{baseCurrency}/{year}/{month}/{day}")
    Call<ExchangeRateResponse> getRatesByDate(
            @Path("apiKey") String apiKey,
            @Path("baseCurrency") String baseCurrency,
            @Path("year") int year,
            @Path("month") int month,
            @Path("day") int day
    );
}
