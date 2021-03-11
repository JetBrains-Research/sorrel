package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model.StandardV2Package
import com.jetbrains.licensedetector.intellij.plugin.utils.extractScmUrl

//TODO: Maybe add installed version (what if many versions installed?)
data class PackageDependency(
    val groupId: String,
    val artifactId: String,
    val installationInformation: MutableList<InstallationInformation> = mutableListOf(),
    var remoteInfo: StandardV2Package? = null,
    var licensesFromJarMetaInfo: Set<License> = setOf()
) {

    val identifier = "$groupId:$artifactId".toLowerCase()

    val isInstalled: Boolean
        get() = installationInformation.isNotEmpty()

    fun getAllLinks(): MutableMap<InfoLink, String> {
        val links = mutableMapOf<InfoLink, String>()
        remoteInfo?.url?.let {
            if (it.isNotEmpty()) links[InfoLink.PROJECT_SITE] = it
        }
        extractScmUrl(remoteInfo?.scm)?.let { scmUrl ->
            if (scmUrl.url.startsWith("http", ignoreCase = true)) {
                // Only display HTTP(s) links
                links[scmUrl.type.linkKey] = scmUrl.url
            }
        }
        remoteInfo?.gitHub?.communityProfile?.let {
            if (!it.documentationUrl.isNullOrEmpty()) links[InfoLink.DOCUMENTATION] = it.documentationUrl
            it.files?.readme?.let { gitHubFile ->
                val url = gitHubFile.htmlUrl ?: gitHubFile.url
                if (!url.isNullOrEmpty()) links[InfoLink.README] = url
            }
            it.files?.codeOfConduct?.let { gitHubFile ->
                val url = gitHubFile.htmlUrl ?: gitHubFile.url
                if (!url.isNullOrEmpty()) links[InfoLink.CODE_OF_CONDUCT] = url
            }
        }
        return links
    }

    fun getMainLicense(): License? {
        return when {
            licensesFromJarMetaInfo.size == 1 -> {
                licensesFromJarMetaInfo.first()
            }
            remoteInfo?.licenses?.mainLicense != null -> {
                remoteInfo?.licenses?.mainLicense!!
            }
            else -> {
                null
            }
        }
    }

    fun getOtherLicenses(): Set<License> {
        val mainLicense = getMainLicense()
        val result = mutableSetOf<License>()

        result.addAll(licensesFromJarMetaInfo)
        val remoteMainLicense = remoteInfo?.licenses?.mainLicense
        if (remoteMainLicense != null) {
            result.add(remoteMainLicense)
        }
        val remoteOtherLicenses = remoteInfo?.licenses?.otherLicenses
        if (remoteOtherLicenses != null) {
            result.addAll(remoteOtherLicenses)
        }
        result.remove(mainLicense)
        return result
    }

}