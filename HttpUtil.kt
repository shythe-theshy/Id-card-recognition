package com.example.tryagian

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    @Throws(IOException::class)
    fun sendPostRequest(url: String, jsonData: String, headers: Map<String, String>): String {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val requestUrl = URL(url)
            connection = requestUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.outputStream.use { os: OutputStream ->
                val input = jsonData.toByteArray()
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                response.toString()
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var errorLine: String?
                while (errorReader.readLine().also { errorLine = it } != null) {
                    errorResponse.append(errorLine)
                }
                errorReader.close()
                throw IOException("HTTP error code: $responseCode, Response: $errorResponse")
            }
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val url = URL("https://www.baidu.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: IOException) {
            false
        }
    }
}