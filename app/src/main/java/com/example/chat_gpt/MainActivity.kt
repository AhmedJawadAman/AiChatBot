package com.example.chat_gpt

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageView
    private lateinit var outputText: TextView
    private val client = OkHttpClient()

    private val apiKey = "AIzaSyAsWnRDcwMbTtivqHaxaRXXA3PoJs7h6Yw"
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        outputText = findViewById(R.id.outputText)

        sendButton.setOnClickListener {
            val userMessage = inputMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                sendRequestToGemini(userMessage)
                inputMessage.setText("")
                hideKeyboard(inputMessage)
            }
        }
    }
    private fun sendRequestToGemini(prompt: String) {
        val jsonObject = JSONObject().apply {
            put("contents", JSONArray().apply {  // Correcting the contents structure
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val jsonMediaType = "application/json".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    outputText.text = "Error: ${e.message}"
                    Log.e("API_ERROR", "Request failed", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("API_RESPONSE", "Response: $responseBody")
                if (response.isSuccessful && responseBody != null) {
                    runOnUiThread {
                        outputText.text = parseResponse(responseBody)

                    }
                } else {
                    runOnUiThread {
                        outputText.text = "Error: ${response.message}"
                        Log.e("API_ERROR", "Response unsuccessful: ${response.message}")
                    }
                }
            }
        })
    }


    private fun parseResponse(responseBody: String): String {
        return try {
            val jsonResponse = JSONObject(responseBody)
            val candidatesArray = jsonResponse.optJSONArray("candidates")

            if (candidatesArray != null && candidatesArray.length() > 0) {
                val firstCandidate = candidatesArray.getJSONObject(0)
                val contentObject = firstCandidate.getJSONObject("content")
                val partsArray = contentObject.optJSONArray("parts")

                if (partsArray != null && partsArray.length() > 0) {
                    partsArray.getJSONObject(0).getString("text")
                } else {
                    "No parts found in the response."
                }
            } else {
                "No candidates found in the response."
            }
        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "Failed to parse response", e)
            "Failed to parse response."
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
