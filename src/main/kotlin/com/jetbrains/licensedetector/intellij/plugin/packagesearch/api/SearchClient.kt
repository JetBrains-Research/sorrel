package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.PluginEnvironment
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.http.HttpWrapper
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model.StandardV2Package
import gson.EnumWithDeserializationFallbackAdapterFactory
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
    private val logger = Logger.getInstance(this.javaClass)

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

    fun packagesInfoByRange(range: List<String>): List<StandardV2Package> {
        return range.chunked(maxRequestResultsCount).map {
            packagesInfoByChunk(it)
        }.flatten()
    }

    private fun packagesInfoByChunk(chunk: List<String>): List<StandardV2Package> {
        if (chunk.isEmpty()) {
            return emptyList()
        }
        if (chunk.size > maxRequestResultsCount) {
            logger.warn(LicenseDetectorBundle.message("licensedetector.search.client.error.too.many.requests.for.range"))
            return emptyList()
        }
        if (chunk.any { it.split(":").size >= maxMavenCoordinatesParts }) {
            logger.warn(LicenseDetectorBundle.message("licensedetector.search.client.error.no.versions.for.range"))
            return emptyList()
        }

        val joinedRange = chunk.joinToString(",") { URIUtil.encodeQuery(it) }
        val requestUrl = "$baseUrl/package?range=$joinedRange"

        val responseJson = httpWrapper.requestJsonObject(requestUrl, ContentType.standard, timeoutInSeconds, headers)

        return gson.fromJson(responseJson[packagesNameInJson], listPackagesType) ?: emptyList()
    }
}