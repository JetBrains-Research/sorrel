package com.jetbrains.licensedetector.intellij.plugin

import arrow.core.Either
import com.jetbrains.packagesearch.intellij.plugin.api.SearchClient
import com.jetbrains.packagesearch.intellij.plugin.api.ServerURLs
import com.jetbrains.packagesearch.intellij.plugin.api.model.StandardV2PackagesWithRepos

object DependencyLicenseFinder {

    private val searchClient: SearchClient = SearchClient(ServerURLs.base)

    // Format of dependency: "groupId:artifactId"
    fun getLicensesForDependencies(listDependencies: List<String>): Map<String, List<String>> {
        val licensesDependencies: MutableMap<String, List<String>> = mutableMapOf()

        listDependencies.chunked(searchClient.maxRequestResultsCount) { chunk ->
            val result = searchClient.packagesByRange(chunk)
            if (result is Either.Right<StandardV2PackagesWithRepos>) {
                result.b.packages?.forEach { standardPackage ->
                    if (standardPackage.licenses != null) {
                        val packageLicenses = mutableListOf<String>()
                        if (standardPackage.licenses.mainLicense?.name != null) {
                            packageLicenses.add(standardPackage.licenses.mainLicense.name)
                        }
                        if (standardPackage.licenses.otherLicenses != null) {
                            packageLicenses.addAll(
                                    standardPackage.licenses.otherLicenses.mapNotNull {
                                        it.name
                                    }
                            )
                        }
                        licensesDependencies[standardPackage.toSimpleIdentifier()] = packageLicenses
                    }
                }
            }
        }

        return licensesDependencies
    }
}