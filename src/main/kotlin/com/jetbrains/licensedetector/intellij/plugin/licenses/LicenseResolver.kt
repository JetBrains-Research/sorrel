package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.rd.util.remove

const val COMPATIBLE_PROJECT_LICENSE_NOT_FOUND = "No licenses compatible with project dependency licenses."

val ALL_SUPPORTED_LICENSE = listOf(
    Apache_2_0,
    BSD_3_Clause,
    GPL_3_0_or_later,
    LGPL_2_1_or_later,
    MIT,
    NoLicense
).sortedByDescending { it.priority }.toTypedArray()

internal fun getLicenseOnFullTextOrNull(fullText: String): SupportedLicense? {
    return ALL_SUPPORTED_LICENSE.firstOrNull { it.fullText == fullText }
}

internal fun getCompatiblePackageLicenses(projectLicenses: Set<SupportedLicense>): Set<SupportedLicense> {
    if (projectLicenses.isEmpty()) {
        return ALL_SUPPORTED_LICENSE.remove(NoLicense).toSet()
    }

    return projectLicenses.map { it.compatibleModuleLicenses }.reduce { acc, set ->
        acc.intersect(set)
    }
}