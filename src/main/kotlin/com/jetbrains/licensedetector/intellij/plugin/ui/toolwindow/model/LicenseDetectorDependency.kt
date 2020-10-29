package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.extractScmUrl
import com.jetbrains.packagesearch.intellij.plugin.api.model.StandardV2Package

//TODO: Maybe add installed version (what if many versions installed?)
data class LicenseDetectorDependency(
        val groupId: String,
        val artifactId: String,
        var isInstalled: Boolean = true,
        var remoteInfo: StandardV2Package? = null
) {

    val identifier = "$groupId:$artifactId".toLowerCase()

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

}