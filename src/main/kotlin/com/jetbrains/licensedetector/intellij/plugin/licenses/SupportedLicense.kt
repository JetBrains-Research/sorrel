package com.jetbrains.licensedetector.intellij.plugin.licenses

interface SupportedLicense : License {
    override val name: String
    override val spdxId: String
    override val url: String
    override val htmlUrl: String
    val priority: LicensePriority
    val fullText: String
    val compatiblePackageLicenses: Set<SupportedLicense>
}

enum class LicensePriority(val value: Int) {
    NO_LICENSE(0),
    LOW(1),
    HIGH(100)
}