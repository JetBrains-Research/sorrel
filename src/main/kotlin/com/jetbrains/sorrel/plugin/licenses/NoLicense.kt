package com.jetbrains.sorrel.plugin.licenses

import com.jetbrains.sorrel.plugin.SorrelUtilUI
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
    override val permissionToolTips: List<String> = listOf()
    override val limitations: List<String> = listOf()
    override val limitationsToolTips: List<String> = listOf()
    override val conditions: List<String> = listOf()
    override val conditionsToolTips: List<String> = listOf()

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = SorrelUtilUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow]",
            "0[top]0"
        )

        add(SorrelUtilUI.createLicenseNameViewPanelLabel(this@NoLicense.name), "span")
    }

    override val compatibleModuleLicensesByLibraryLicense: Set<SupportedLicense> = setOf()
    override val compatibleModuleLicensesBySubmoduleLicense: Set<SupportedLicense> = setOf()
}