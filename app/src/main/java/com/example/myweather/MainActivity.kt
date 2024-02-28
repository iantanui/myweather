package com.example.myweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myweather.ui.theme.MyweatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyweatherTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp() {
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var apiResponse by remember { mutableStateOf(ApiResponse("", "")) }

    var isSearchClicked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Enter City name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            modifier = Modifier
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSearchClicked = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Weather")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display error message if any
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (apiResponse.temp.isNotEmpty() && apiResponse.humidity.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Temperature: ${apiResponse.temp}",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Humidity: ${apiResponse.humidity}",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    LaunchedEffect(isSearchClicked) {
        if (isSearchClicked && searchText.isNotBlank()) {
            val response = getWeatherFromApi(searchText)
            if (response.temp.isNotEmpty() && response.humidity.isNotEmpty()) {
                apiResponse = response
                errorMessage = ""
            } else {
                // Display error
                errorMessage = "Weather details not found"
            }
            isSearchClicked = false
        }
    }
}

data class ApiResponse(
    val temp: String,
    val humidity: String,
)

object ApiConfig {
    const val BASE_URL = "https://open-weather13.p.rapidapi.com/city/London"
    const val API_KEY = ""
    const val HOST = "open-weather13.p.rapidapi.com"
}

suspend fun getWeatherFromApi(cityName: String): ApiResponse {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL)
            .get()
            .addHeader("X-RapidAPI-Key", ApiConfig.API_KEY)
            .addHeader("X-RapidAPI-Host", ApiConfig.HOST)
            .build()

        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(responseBody)

                val mainObject = jsonObject.optJSONObject("main")
                val temp = mainObject?.optDouble("temp", Double.NaN)?.toString() ?: ""
                val humidity = mainObject?.optInt("humidity", -1)?.toString() ?: ""

                ApiResponse(temp, humidity)
            } else {
                ApiResponse("Request failed with code: ${response.code}", "")
            }
        } catch (e: IOException) {
            ApiResponse("Network error occurred: ${e.message}", "")
        } catch (e: JSONException) {
            ApiResponse("Error parsing JSON: ${e.message}", "")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyweatherTheme {
        WeatherApp()
    }
}