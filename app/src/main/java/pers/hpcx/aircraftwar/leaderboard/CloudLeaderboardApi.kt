package pers.hpcx.aircraftwar.leaderboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CloudLeaderboardApi(private val context: Context) {
    
    fun uploadRecord(record: LeaderboardRecord) = UploadLeaderboardRecordResponse.fromJson(
        JSONObject(
            doRequest(
                path = "/api/leaderboard/upload",
                method = "POST",
                body = record.toJson().toString(),
            )
        )
    )
    
    fun fetchWorldRecords() = JSONArray(
        doRequest(
            path = "/api/leaderboard/fetch",
            method = "GET",
        )
    ).let { array ->
        buildList {
            for (index in 0 until array.length()) {
                add(LeaderboardRecord.fromJson(array.getJSONObject(index)))
            }
        }
    }
    
    private fun doRequest(path: String, method: String, body: String? = null): String {
        val baseUrl = CloudLeaderboardConfig.getBaseUrl(context)
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                throw IllegalStateException(message.orEmpty().ifBlank { "请求失败，HTTP $code" })
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}
