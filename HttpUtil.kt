package com.example.tryagian

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpUtil {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun sendPostRequest(url: String, jsonData: String, headers: Map<String, String>): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonData.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP error: ${response.code}, ${response.message}")
        }
        return response.body?.string() ?: throw IOException("Empty response body")
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("https://www.baidu.com")
                .head()
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: IOException) {
            false
        }
    }
}
