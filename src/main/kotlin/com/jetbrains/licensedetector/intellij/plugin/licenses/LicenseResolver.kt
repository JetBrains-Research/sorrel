package com.jetbrains.licensedetector.intellij.plugin.licenses

const val COMPATIBLE_PROJECT_LICENSE_NOT_FOUND = "No licenses compatible with project dependency licenses."

val ALL_SUPPORTED_LICENSE = listOf(
        Apache_2_0,
        BSD_3_Clause,
        GPL_3_0_or_later,
        LGPL_2_1_or_later,
        MIT,
        NoLicense
).sortedByDescending { it.priority }.toTypedArray()

internal fun getLicenseOnSpdxIdOrNull(spdxId: String): SupportedLicense? {
    return ALL_SUPPORTED_LICENSE.firstOrNull { it.spdxId == spdxId }
}

internal fun getLicenseOnNameOrNull(licenseName: String): SupportedLicense? {
    return ALL_SUPPORTED_LICENSE.firstOrNull { it.name == licenseName }
}

internal fun getLicenseOnFullTextOrNull(fullText: String): SupportedLicense? {
    return ALL_SUPPORTED_LICENSE.firstOrNull { it.fullText == fullText }
}

internal fun getCompatiblePackageLicenses(projectLicenses: Set<SupportedLicense>): List<SupportedLicense> {
    return projectLicenses.map { it.compatiblePackageLicenses }.reduce { acc, set ->
        acc.intersect(set)
    }.toList().sortedByDescending { it.priority }
}