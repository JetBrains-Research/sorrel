package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.PluginEnvironment
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.http.HttpWrapper
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model.StandardV2Package
import com.jetbrains.licensedetector.intellij.plugin.utils.logDebug
import com.jetbrains.licensedetector.intellij.plugin.utils.logWarn
import gson.EnumWithDeserializationFallbackAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.util.URIUtil

object ServerURLs {
    const val base = "https://package-search.services.jetbrains.com/api"
}

private val pluginEnvironment by lazy { PluginEnvironment() }

object ContentType {
    const val standard = "application/vnd.jetbrains.packagesearch.standard.v2+json"
}

class SearchClient(
    private val baseUrl: String,
    private val timeoutInSeconds: Int = 10,
    private val headers: List<Pair<String, String>> = listOf(
        Pair("JB-Plugin-Version", pluginEnvironment.pluginVersion),
        Pair("JB-IDE-Version", pluginEnvironment.ideVersion)
    )
) {
    private val httpWrapper = HttpWrapper()

    private val maxRequestResultsCount = 25
    private val maxMavenCoordinatesParts = 3

    private val gson = Gson().newBuilder()
        // https://youtrack.jetbrains.com/issue/PKGS-547
        // Ensures enum values in our model are not null if a default value is available
        // (works around cases like https://discuss.kotlinlang.org/t/json-enum-deserialization-breakes-kotlin-null-safety/11670)
        .registerTypeAdapterFactory(EnumWithDeserializationFallbackAdapterFactory())
        .registerTypeAdapter(License::class.java, PackageLicenseAdapter)
        .create()

    private val listPackagesType = object : TypeToken<List<StandardV2Package>>() {}.type

    private val packagesNameInJson = "packages"

    suspend fun packagesInfoByRange(range: List<String>): List<StandardV2Package> {
        return withContext(Dispatchers.IO) {
            val result = range.chunked(maxRequestResultsCount).map {
                async { packagesInfoByChunk(it) }
            }

            awaitAll(*result.toTypedArray())
        }.flatten()
    }

    private fun packagesInfoByChunk(chunk: List<String>): List<StandardV2Package> {
        if (chunk.isEmpty()) {
            return emptyList()
        }
        if (chunk.size > maxRequestResultsCount) {
            logWarn(LicenseDetectorBundle.message("licensedetector.search.client.error.too.many.requests.for.range"))
            return emptyList()
        }
        if (chunk.any { it.split(":").size >= maxMavenCoordinatesParts }) {
            logWarn(LicenseDetectorBundle.message("licensedetector.search.client.error.no.versions.for.range"))
            return emptyList()
        }

        val joinedRange = chunk.joinToString(",") { URIUtil.encodeQuery(it) }
        val requestUrl = "$baseUrl/package?range=$joinedRange"

        val responseJson = httpWrapper.requestJsonObject(requestUrl, ContentType.standard, timeoutInSeconds, headers)
        logDebug("Requested info about $chunk packages from Package Search")

        val responsePackageInfo: List<StandardV2Package> = gson.fromJson(
            responseJson[packagesNameInJson], listPackagesType
        ) ?: emptyList()

        logDebug("Received info about $chunk from Package Search")

        return responsePackageInfo
    }
}