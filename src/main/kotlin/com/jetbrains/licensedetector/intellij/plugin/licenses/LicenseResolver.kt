package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.rd.util.remove

const val COMPATIBLE_PROJECT_LICENSE_NOT_FOUND = "No licenses compatible with project dependency licenses."

val ALL_SUPPORTED_LICENSE = listOf(
    AGPL_3_0_only,
    Apache_2_0,
    BSD_2_Clause,
    BSD_3_Clause,
    CDDL_1_0,
    EPL_1_0,
    GPL_2_0_only,
    GPL_3_0_only,
    ISC,
    LGPL_2_1_only,
    LGPL_3_0_only,
    MIT,
    MPL_1_1,
    MPL_2_0,
    WTFPL,
    NoLicense
).sortedByDescending { it.priority }.toTypedArray()

internal fun getCompatibleLicenseWithPackageLicenses(projectLicenses: Set<SupportedLicense>): Set<SupportedLicense> {
    if (projectLicenses.isEmpty()) {
        return ALL_SUPPORTED_LICENSE.remove(NoLicense).toSet()
    }

    return projectLicenses.map { it.compatibleModuleLicenses }.reduce { acc, set ->
        acc.intersect(set)
    }
}