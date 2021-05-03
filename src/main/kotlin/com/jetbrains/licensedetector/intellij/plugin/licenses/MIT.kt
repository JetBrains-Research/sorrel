package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel

object MIT : SupportedLicense {
    override val name: String = "MIT License"
    override val url: String = "https://opensource.org/licenses/MIT"
    override val htmlUrl: String = "https://opensource.org/licenses/MIT"
    override val spdxId: String = "MIT"
    override val priority: LicensePriority = LicensePriority.HIGH
    override val fullText: String =
        """
                MIT License

                Copyright (c) <year> <copyright holders>

                Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

                The above copyright notice and this permission notice (including the next paragraph) shall be included in all copies or substantial portions of the Software.

                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            """.trimIndent()

    override val nameSpdxRegex: Regex = Regex(
        "(.*MIT( License)?)|(MIT)",
        RegexOption.IGNORE_CASE
    )

    override val description: String = LicenseDetectorBundle.message("licensedetector.ui.mit.description")
    override val permissions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.1"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.2"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.3"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.4")
    )
    override val permissionToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.1.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.2.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.3.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.permissions.4.tooltip")
    )
    override val limitations: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.limitations.1"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.limitations.2")
    )
    override val limitationsToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.limitations.1.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.mit.limitations.2.tooltip")
    )
    override val conditions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.conditions.1")
    )
    override val conditionsToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.mit.conditions.1.tooltip")
    )

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow][left,grow][left,grow]",
            "0[top][top][top][top][top][top][top]0"
        )

        add(RiderUI.createLicenseNameViewPanelLabel(this@MIT.name), "span")
        add(RiderUI.createLabel("<html>${description}</html>"), "span")

        add(
            RiderUI.createHeaderLabel(LicenseDetectorBundle.message("licensedetector.ui.licenseView.permissions")),
            "cell 0 2,growx"
        )
        add(
            RiderUI.createHeaderLabel(LicenseDetectorBundle.message("licensedetector.ui.licenseView.limitations")),
            "cell 1 2,growx"
        )
        add(
            RiderUI.createHeaderLabel(LicenseDetectorBundle.message("licensedetector.ui.licenseView.conditions")),
            "cell 2 2, growx"
        )

        add(RiderUI.createLicensePermissionLabel(permissions[0], permissionToolTips[0]), "cell 0 3,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[1], permissionToolTips[1]), "cell 0 4,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[2], permissionToolTips[2]), "cell 0 5,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[3], permissionToolTips[3]), "cell 0 6,growx")

        add(RiderUI.createLicenseLimitationsLabel(limitations[0], limitationsToolTips[0]), "cell 1 3,growx")
        add(RiderUI.createLicenseLimitationsLabel(limitations[1], limitationsToolTips[1]), "cell 1 4,growx")

        add(RiderUI.createLicenseConditionsLabel(conditions[0], conditionsToolTips[0]), "cell 2 3,growx")
    }

    override val compatibleModuleLicensesByLibraryLicense: Set<SupportedLicense> = setOf(
        AGPL_3_0_only,
        Apache_2_0,
        BSD_2_Clause,
        BSD_3_Clause,
        CDDL_1_0,
        EPL_1_0,
        GPL_2_0_only,
        GPL_2_0_with_classpath_exception,
        GPL_3_0_only,
        ISC,
        LGPL_2_1_only,
        LGPL_3_0_only,
        this,
        MPL_1_1,
        MPL_2_0,
        WTFPL
    )
    override val compatibleModuleLicensesBySubmoduleLicense: Set<SupportedLicense> = setOf(
        AGPL_3_0_only,
        Apache_2_0,
        BSD_2_Clause,
        BSD_3_Clause,
        CDDL_1_0,
        EPL_1_0,
        GPL_2_0_only,
        GPL_2_0_with_classpath_exception,
        GPL_3_0_only,
        ISC,
        LGPL_2_1_only,
        LGPL_3_0_only,
        this,
        MPL_1_1,
        MPL_2_0,
        WTFPL
    )
}