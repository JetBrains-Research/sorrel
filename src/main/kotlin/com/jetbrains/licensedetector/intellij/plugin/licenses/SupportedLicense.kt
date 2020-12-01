package com.jetbrains.licensedetector.intellij.plugin.licenses

interface SupportedLicense : License {
    val priority: LicensePriority
    val fullText: String
    val compatiblePackageLicenses: Set<SupportedLicense>
}

enum class LicensePriority(val value: Int) {
    LOW(1),
    HIGH(100)
}