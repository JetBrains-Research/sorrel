package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle

class HttpWrapper {
    private val logger = Logger.getInstance(this.javaClass)
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
                    logger.info(LicenseDetectorBundle.message("licensedetector.search.client.response.body.is.empty"))
                    ""
                }
                else -> responseText
            }
        } catch (e: Exception) {
            logger.warn(e)
            return ""
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

    private fun String.asJSONObject(): JsonObject = JsonParser().parse(this).asJsonObject
}