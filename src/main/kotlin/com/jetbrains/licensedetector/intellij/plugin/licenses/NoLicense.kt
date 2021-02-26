package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel

object NoLicense : SupportedLicense {
    override val name: String = "No License"
    override val spdxId: String = ""
    override val url: String = ""
    override val htmlUrl: String = ""
    override val priority: LicensePriority = LicensePriority.NO_LICENSE
    override val fullText: String = COMPATIBLE_PROJECT_LICENSE_NOT_FOUND

    override val nameSpdxRegex: Regex = Regex("", RegexOption.IGNORE_CASE)

    override val description: String = ""
    override val permissions: List<String> = listOf()
    override val limitations: List<String> = listOf()
    override val conditions: List<String> = listOf()

    override val descriptionPanel: JComponent = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow]",
            "0[top]0"
        )

        add(RiderUI.createLicenseNameViewPanelLabel(this@NoLicense.name), "span")
    }

    override val compatibleModuleLicenses: Set<SupportedLicense> = setOf()
    override val compatibleDependencyLicenses: Set<SupportedLicense> = setOf()
}