package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.packagesearch.intellij.plugin.api.model.StandardV2Package

data class LicenseDetectorDependency(
        val groupId: String,
        val artifactId: String,
        var isInstalled: Boolean = true,
        var remoteInfo: StandardV2Package? = null
) {

    val identifier = "$groupId:$artifactId".toLowerCase()

    //TODO: Is it really necessary??
/*
    fun getAllLinks(): MutableMap<InfoLink, String> {
        val links = mutableMapOf<InfoLink, String>()
        remoteInfo?.url?.let {
            if (it.isNotEmpty()) links[PROJECT_SITE] = it
        }
        extractScmUrl(remoteInfo?.scm)?.let { scmUrl ->
            if (scmUrl.url.startsWith("http", ignoreCase = true)) {
                // Only display HTTP(s) links
                links[scmUrl.type.linkKey] = scmUrl.url
            }
        }
        remoteInfo?.gitHub?.communityProfile?.let {
            if (!it.documentationUrl.isNullOrEmpty()) links[DOCUMENTATION] = it.documentationUrl
            it.files?.readme?.let { gitHubFile ->
                val url = gitHubFile.htmlUrl ?: gitHubFile.url
                if (!url.isNullOrEmpty()) links[README] = url
            }
            it.files?.codeOfConduct?.let { gitHubFile ->
                val url = gitHubFile.htmlUrl ?: gitHubFile.url
                if (!url.isNullOrEmpty()) links[CODE_OF_CONDUCT] = url
            }
        }
        return links
    }

 */
}