package com.jetbrains.packagesearch.intellij.plugin.api

import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetbrains.licensedetector.intellij.plugin.PluginEnvironment
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.packagesearch.intellij.plugin.api.http.requestJsonObject
import com.jetbrains.packagesearch.intellij.plugin.api.model.StandardV2Package
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
        }.fold(mutableListOf()) { acc, item ->
            if (item is Either.Right) {
                acc.addAll(item.b)
                acc
            } else {
                //TODO: Add to log
                acc
            }
        }
    }

    private fun packagesInfoByChunk(chunk: List<String>): Either<String, List<StandardV2Package>> {
        if (chunk.isEmpty()) {
            return Either.right(emptyList())
        }
        if (chunk.size > maxRequestResultsCount) {
            return Either.left(PackageSearchBundle.message("packagesearch.search.client.error.too.many.requests.for.range"))
        }
        if (chunk.any { it.split(":").size >= maxMavenCoordinatesParts }) {
            return Either.left(PackageSearchBundle.message("packagesearch.search.client.error.no.versions.for.range"))
        }

        val joinedRange = chunk.joinToString(",") { URIUtil.encodeQuery(it) }
        val requestUrl = "$baseUrl/package?range=$joinedRange"

        return requestJsonObject(requestUrl, ContentType.standard, timeoutInSeconds, headers)
                .fold(
                        { Either.left(it) },
                        {
                            Either.right(
                                    gson.fromJson(
                                            it[packagesNameInJson],
                                            listPackagesType) ?: emptyList())
                        })
    }
}