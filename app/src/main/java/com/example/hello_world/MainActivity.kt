package com.example.hello_world

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var isUpdatingText = false
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rateEdit = findViewById<EditText>(R.id.rateEdit)
        val somEdit = findViewById<EditText>(R.id.somEdit)
        val currencySelect = findViewById<Spinner>(R.id.currencySelect)
        val amountEdit = findViewById<EditText>(R.id.amountEdit)

        val currencies = arrayOf("USD", "EUR", "KGS", "JPY", "GBP", "CNY", "RUB")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySelect.adapter = adapter

        val currencyRates = mutableMapOf<String, Double>()

        fun updateSomEdit() {
            val amount = amountEdit.text.toString().toDoubleOrNull() ?: 0.0
            val rate = rateEdit.text.toString().toDoubleOrNull() ?: 1.0
            val result = roundToTwoDecimals(amount * rate)

            somEdit.setText(result.toString())
        }

        fun updateAmountEdit() {
            val som = somEdit.text.toString().toDoubleOrNull() ?: 0.0
            val rate = rateEdit.text.toString().toDoubleOrNull() ?: 1.0
            val result = roundToTwoDecimals(som / rate)

            amountEdit.setText(result.toString())
        }

        val somTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingText) return
                isUpdatingText = true
                try {
                    updateAmountEdit()
                } finally {
                    isUpdatingText = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        val amountTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingText) return
                isUpdatingText = true
                try {
                    updateSomEdit()
                } finally {
                    isUpdatingText = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        amountEdit.addTextChangedListener(amountTextWatcher)
        somEdit.addTextChangedListener(somTextWatcher)
        rateEdit.addTextChangedListener(amountTextWatcher)

        fun fetchRates() {
            val url = "https://api.exchangerate-api.com/v4/latest/KGS"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to fetch rates", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.let { responseBody ->
                        val json = JSONObject(responseBody.string())
                        val rates = json.getJSONObject("rates")

                        // Store rates in the currencyRates map
                        currencyRates["USD"] = roundToTwoDecimals(1 / rates.getDouble("USD"))
                        currencyRates["EUR"] = roundToTwoDecimals(1 / rates.getDouble("EUR"))
                        currencyRates["JPY"] = roundToTwoDecimals(1 / rates.getDouble("JPY"))
                        currencyRates["GBP"] = roundToTwoDecimals(1 / rates.getDouble("GBP"))
                        currencyRates["CNY"] = roundToTwoDecimals(1 / rates.getDouble("CNY"))
                        currencyRates["RUB"] = roundToTwoDecimals(1 / rates.getDouble("RUB"))

                        runOnUiThread {
                            val selectedCurrency = currencySelect.selectedItem.toString()
                            val rate = currencyRates[selectedCurrency] ?: 1.0
                            rateEdit.setText(rate.toString())
                        }
                    }
                }
            })
        }

        fetchRates()

        currencySelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                val rate = currencyRates[selectedCurrency] ?: 1.0
                rateEdit.setText(rate.toString())

                updateSomEdit()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    fun roundToTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}
