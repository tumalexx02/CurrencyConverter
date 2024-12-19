package com.example.currencyconverter;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyconverter.api.ExchangeRateAPI;
import com.example.currencyconverter.api.ExchangeRateResponse;
import com.example.currencyconverter.api.RetrofitInstance;

import java.util.Calendar;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText inputAmount;
    private Spinner spinnerFromCurrency, spinnerToCurrency;
    private Button swapButton, datePickerButton;
    private TextView convertedAmount, selectedDateText;
    private String[] currencies = {"USD", "RUB", "EUR", "CNY"};
    private String[] currencySymbols = {"$", "₽", "€", "¥"};
    private static final String API_KEY = "584f27e6f08aa7e33d907518";
    private String selectedDate = null;

    Calendar calendar = Calendar.getInstance();
    int currentYear = calendar.get(Calendar.YEAR);
    int currentMonth = calendar.get(Calendar.MONTH);
    int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
    private Double currentRate = fetchFirstExchangeRate("USD", "RUB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupCurrencySpinners();
        setupButtonListeners();
    }

    private void initializeViews() {
        inputAmount = findViewById(R.id.inputAmount);
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency);
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency);
        swapButton = findViewById(R.id.swapButton);
        convertedAmount = findViewById(R.id.convertedAmount);
        datePickerButton = findViewById(R.id.selectedDate);
    }

    private void setupCurrencySpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFromCurrency.setAdapter(adapter);
        spinnerToCurrency.setAdapter(adapter);

        spinnerFromCurrency.setSelection(0);
        spinnerToCurrency.setSelection(1);

        // Recalculate when currency is changed
        spinnerFromCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                recalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        spinnerToCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                recalculate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // Recalculate when input is changed
        inputAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String input = inputAmount.getText().toString();
                if (isValidInput(input)) {
                    try {
                        double amount = evaluateExpression(input);

                        if (amount != 0) {
                            double converted = amount * currentRate;
                            String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
                            String toCurrency = spinnerToCurrency.getSelectedItem().toString();
                            String fromCurrencySymbol = getCurrencySymbol(fromCurrency);
                            String toCurrencySymbol = getCurrencySymbol(toCurrency);
                            convertedAmount.setText(String.format("%.2f%s = %.2f%s", amount, fromCurrencySymbol, converted, toCurrencySymbol));
                        } else {
                            convertedAmount.setText("Введите корректное выражение!");
                        }
                    } catch (Exception e) {
                        convertedAmount.setText("Ошибка в выражении!");
                    }
                } else {
                    convertedAmount.setText("Введите корректное выражение!");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {}
        });
    }

    private boolean isValidInput(String input) {
        // Разрешаем цифры, знаки +, -, *, / и пробелы
        return Pattern.matches("[0-9+\\-*/.\\s]*", input);
    }

    private double evaluateExpression(String input) {
        // Убираем пробелы из выражения
        input = input.replaceAll("\\s+", "");

        String[] tokens = input.split("(?=[+\\-*/])|(?<=[+\\-*/])");

        double result = 0;
        double currentNumber = 0;
        char lastOperator = '+';

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (token.matches("[0-9.]+")) {
                currentNumber = Double.parseDouble(token);

                // Выполняем операцию в зависимости от последнего оператора
                switch (lastOperator) {
                    case '+':
                        result += currentNumber;
                        break;
                    case '-':
                        result -= currentNumber;
                        break;
                    case '*':
                        result *= currentNumber;
                        break;
                    case '/':
                        if (currentNumber != 0) {
                            result /= currentNumber;
                        } else {
                            return Double.NaN;
                        }
                        break;
                    default:
                        break;
                }
            } else if (token.matches("[+\\-*/]")) {
                lastOperator = token.charAt(0);
            }
        }

        return result;
    }

    private void setupButtonListeners() {
        swapButton.setOnClickListener(view -> swapCurrencies());

        datePickerButton.setOnClickListener(view -> showDatePicker());
    }

    private void recalculate() {
        try {
            String input = inputAmount.getText().toString();
            double amount = evaluateExpression(input);

            String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
            String toCurrency = spinnerToCurrency.getSelectedItem().toString();

            if (amount != 0) {
                fetchExchangeRate(amount, fromCurrency, toCurrency, null);
            }
        } catch (NumberFormatException e) {
            convertedAmount.setText("Введите корректную сумму!");
        }
    }

    private Double fetchFirstExchangeRate(String fromCurrency, String toCurrency) {
        ExchangeRateAPI api = RetrofitInstance.getRetrofitInstance().create(ExchangeRateAPI.class);
        Call<ExchangeRateResponse> call;

        call = api.getLatestRates(API_KEY, fromCurrency);
        final Double[] firstRate = new Double[1];

        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double rate = response.body().getRates().get(toCurrency);
                    currentRate = rate;
                    firstRate[0] = rate;
                } else {
                    convertedAmount.setText("Ошибка API");
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                convertedAmount.setText("Ошибка API");
            }
        });

        return firstRate[0];
    }

    private void fetchExchangeRate(double amount, String fromCurrency, String toCurrency, String date) {
        ExchangeRateAPI api = RetrofitInstance.getRetrofitInstance().create(ExchangeRateAPI.class);
        Call<ExchangeRateResponse> call;

        String apiPath;
        if (selectedDate == null) {
            call = api.getLatestRates(API_KEY, fromCurrency);
            apiPath = String.format("v6/%s/latest/%s", API_KEY, fromCurrency);
        } else {
            String[] dateParts = selectedDate.split("-");
            call = api.getRatesByDate(
                    API_KEY,
                    fromCurrency,
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]),
                    Integer.parseInt(dateParts[2])
            );
            apiPath = String.format("v6/%s/history/%s/%s/%s/%s",
                    API_KEY, fromCurrency, dateParts[0], dateParts[1], dateParts[2]);
        }

        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double rate = response.body().getRates().get(toCurrency);
                    currentRate = rate;
                    if (rate != null) {
                        double converted = amount * rate;
                        String fromCurrencySymbol = getCurrencySymbol(fromCurrency);
                        String toCurrencySymbol = getCurrencySymbol(toCurrency);
                        convertedAmount.setText(String.format("%.2f%s = %.2f%s", amount, fromCurrencySymbol, converted, toCurrencySymbol));
                    } else {
                        convertedAmount.setText("Курс не найден!");
                    }
                } else {
                    convertedAmount.setText("Ошибка API");
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                convertedAmount.setText("Ошибка API");
            }
        });
    }

    private String getCurrencySymbol(String currency) {
        switch (currency) {
            case "USD": return "$";
            case "RUB": return "₽";
            case "EUR": return "€";
            case "CNY": return "¥";
            default: return "";
        }
    }

    private void swapCurrencies() {
        int fromIndex = spinnerFromCurrency.getSelectedItemPosition();
        int toIndex = spinnerToCurrency.getSelectedItemPosition();
        spinnerFromCurrency.setSelection(toIndex);
        spinnerToCurrency.setSelection(fromIndex);

        // Update the result after swapping
        if (!inputAmount.getText().toString().isEmpty()) {
            String input = inputAmount.getText().toString();
            double amount = evaluateExpression(input);
            String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
            String toCurrency = spinnerToCurrency.getSelectedItem().toString();
            if (amount != 0) {
                fetchExchangeRate(amount, fromCurrency, toCurrency, null);
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        @SuppressLint("DefaultLocale") DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    if (selectedYear == currentYear &&
                            selectedMonth == currentMonth &&
                            selectedDay == currentDay) {
                        selectedDate = null;
                        datePickerButton.setText("Сегодня");
                    } else {
                        selectedDate = String.format("%d-%02d-%02d",
                                selectedYear, selectedMonth + 1, selectedDay);
                        datePickerButton.setText(String.format("%02d.%02d.%d",
                                selectedDay, selectedMonth + 1, selectedYear));
                    }
                    recalculate();
                }, currentYear, currentMonth, currentDay);

        datePickerDialog.show();
    }
}