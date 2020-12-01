package com.jetbrains.licensedetector.intellij.plugin.licenses

val COMPATIBLE_PROJECT_LICENSE_NOT_FOUND = "No licenses compatible with project dependency licenses."


internal fun getLicenseOnSpdxIdOrNull(spdxId: String): SupportedLicense? {
    return when (spdxId) {
        Apache_2_0.spdxId -> Apache_2_0
        BSD_3_Clause.spdxId -> BSD_3_Clause
        GPL_3_0_or_later.spdxId -> GPL_3_0_or_later
        LGPL_2_1_or_later.spdxId -> LGPL_2_1_or_later
        MIT.spdxId -> MIT
        else -> null
    }
}

internal fun getLicenseOnNameOrNull(licenseName: String): SupportedLicense? {
    return when (licenseName) {
        Apache_2_0.name -> Apache_2_0
        BSD_3_Clause.name -> BSD_3_Clause
        GPL_3_0_or_later.name -> GPL_3_0_or_later
        LGPL_2_1_or_later.name -> LGPL_2_1_or_later
        MIT.name -> MIT
        else -> null
    }
}

internal fun getCompatiblePackageLicenses(projectLicenses: Set<SupportedLicense>): List<SupportedLicense> {
    return projectLicenses.map { it.compatiblePackageLicenses }.reduce { acc, set ->
        acc.intersect(set)
    }.toList().sortedBy { it.name }
}