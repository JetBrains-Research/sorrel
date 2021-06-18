package com.jetbrains.sorrel.plugin.licenses

import javax.swing.JComponent

interface SupportedLicense : License {
    override val name: String
    override val spdxId: String
    override val url: String
    override val htmlUrl: String
    val priority: LicensePriority
    val fullText: String

    // For detection of license by name or by spdx
    val nameSpdxRegex: Regex

    //Compatible module licenses if the library has a current license
    val compatibleModuleLicensesByLibraryLicense: Set<SupportedLicense>

    //Compatible module licenses if the submodule has a current license
    val compatibleModuleLicensesBySubmoduleLicense: Set<SupportedLicense>

    //For description panel
    val description: String
    val permissions: List<String>
    val permissionToolTips: List<String>
    val limitations: List<String>
    val limitationsToolTips: List<String>
    val conditions: List<String>
    val conditionsToolTips: List<String>

    fun descriptionPanel(): JComponent
}

enum class LicensePriority(val value: Int) {
    NO_LICENSE(0),
    VERY_STRONG_COPYLEFT(1),
    STRONG_COPYLEFT(2),
    WEAK_COPYLEFT(3),
    PERMISSIVE(50),
    RECOMMENDED(100)
}