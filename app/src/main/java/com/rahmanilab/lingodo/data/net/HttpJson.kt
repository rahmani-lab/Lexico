package com.rahmanilab.lingodo.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny JSON-over-HTTP helper built on [HttpURLConnection] — no third-party networking dependency.
 * All calls run on [Dispatchers.IO]. On a non-2xx response it throws [HttpException] carrying the
 * status code and (truncated) body so callers can surface a useful message.
 */
object HttpJson {

    class HttpException(val code: Int, val bodySnippet: String) :
        IOException("HTTP $code: ${bodySnippet.take(300)}")

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15_000
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        connection.readResult()
    }

    suspend fun postJson(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 45_000
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        connection.readResult()
    }

    private fun HttpURLConnection.readResult(): String = try {
        val code = responseCode
        val stream = if (code in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code in 200..299) text else throw HttpException(code, text)
    } finally {
        disconnect()
    }
}
