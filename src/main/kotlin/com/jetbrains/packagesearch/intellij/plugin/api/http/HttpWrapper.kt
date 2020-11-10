package com.jetbrains.packagesearch.intellij.plugin.api.http

import arrow.core.Either
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle

private const val pluginUserAgent: String = "License Detection Plugin"

fun requestString(url: String, acceptContentType: String, timeoutInSeconds: Int = 10, headers: List<Pair<String, String>>): Either<String, String> =
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

            when {
                responseText.isEmpty() -> Either.left(PackageSearchBundle.message("packagesearch.search.client.response.body.is.empty"))
                else -> Either.right(responseText)
            }
        } catch (e: Exception) {
            Either.left(e.logAndReturnMessage())
        }

fun requestJsonObject(
        url: String,
        acceptContentType: String,
        timeoutInSeconds: Int = 10,
        headers: List<Pair<String, String>>
): Either<String, JsonObject> {
    val response = requestString(url, acceptContentType, timeoutInSeconds, headers)

    return response.fold(
            { Either.left(it) },
            { Either.right(it.asJSONObject()) })
}

private fun String.asJSONObject(): JsonObject = JsonParser().parse(this).asJsonObject

private fun Throwable.logAndReturnMessage(): String {
    @Suppress("TooGenericExceptionCaught") // Guarding against random runtime failures
    try {
        Logger.getInstance(this.javaClass).error(this)
    } catch (t: Throwable) {
        // IntelliJ logger rethrows logged exception
    }

    return this.message!!
}
