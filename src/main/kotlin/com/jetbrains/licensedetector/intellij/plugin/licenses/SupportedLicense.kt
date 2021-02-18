package com.jetbrains.licensedetector.intellij.plugin.licenses

import javax.swing.JComponent

interface SupportedLicense : License {
    override val name: String
    override val spdxId: String
    override val url: String
    override val htmlUrl: String
    val priority: LicensePriority
    val fullText: String

    //Compatible module licenses if the dependency has a current license
    val compatibleModuleLicenses: Set<SupportedLicense>

    //Compatible dependencies licenses if the module has a current license
    val compatibleDependencyLicenses: Set<SupportedLicense>

    //For description panel
    val description: String
    val permissions: List<String>
    val limitations: List<String>
    val conditions: List<String>

    val descriptionPanel: JComponent
}

enum class LicensePriority(val value: Int) {
    NO_LICENSE(0),
    LOW(1),
    HIGH(100)
}