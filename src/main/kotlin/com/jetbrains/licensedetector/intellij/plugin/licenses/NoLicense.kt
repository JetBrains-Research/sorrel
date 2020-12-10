package com.jetbrains.licensedetector.intellij.plugin.licenses

object NoLicense : SupportedLicense {
    override val name: String = "No License"
    override val spdxId: String = ""
    override val url: String = ""
    override val htmlUrl: String = ""
    override val priority: LicensePriority = LicensePriority.NO_LICENSE
    override val fullText: String = COMPATIBLE_PROJECT_LICENSE_NOT_FOUND
    override val compatibleDependencyLicenses: Set<SupportedLicense> = setOf()
}