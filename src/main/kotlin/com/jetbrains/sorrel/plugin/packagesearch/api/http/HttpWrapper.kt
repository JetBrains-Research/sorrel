package com.jetbrains.sorrel.plugin.packagesearch.api.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.intellij.util.io.HttpRequests
import com.jetbrains.sorrel.plugin.utils.logInfo
import com.jetbrains.sorrel.plugin.utils.logWarn

class HttpWrapper {
    private val pluginUserAgent: String = "License Detector Plugin"

    private fun requestString(
        url: String,
        acceptContentType: String,
        timeoutInSeconds: Int = 10,
        headers: List<Pair<String, String>>
    ): String {
        try {
            val responseText = HttpRequests.request(url)
                .userAgent(pluginUserAgent)
                .accept(acceptContentType)
                .connectTimeout(timeoutInSeconds * 1000)
                .readTimeout(timeoutInSeconds * 1000)
                .tuner { connection ->
                    headers.forEach {
                        connection.setRequestProperty(it.first, it.second)
                    }
                }
                .readString()

            return when {
                responseText.isEmpty() -> {
                    logInfo(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.search.client.response.body.is.empty"))
                    "{}"
                }
                else -> responseText
            }
        } catch (e: Exception) {
            logWarn(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.search.client.response.exception", url), e)
            return "{}"
        }
    }

    fun requestJsonObject(
        url: String,
        acceptContentType: String,
        timeoutInSeconds: Int = 10,
        headers: List<Pair<String, String>>
    ): JsonObject {
        return requestString(url, acceptContentType, timeoutInSeconds, headers).asJSONObject()
    }

    private fun String.asJSONObject(): JsonObject = parseString(this).asJsonObject
}
