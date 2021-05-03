package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel

object BSD_3_Clause : SupportedLicense {
    override val name: String = "BSD 3-Clause \"New\" or \"Revised\" License"
    override val url: String = "https://opensource.org/licenses/BSD-3-Clause"
    override val htmlUrl: String = "https://opensource.org/licenses/BSD-3-Clause"
    override val spdxId: String = "BSD-3-Clause"
    override val priority: LicensePriority = LicensePriority.LOW
    override val fullText: String =
        """
                Copyright (c) <year> <owner>. All rights reserved.

                Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

                1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
                2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
                3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
                THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
            """.trimIndent()

    override val nameSpdxRegex: Regex = Regex(
        "(BSD 3-Clause \"New\" or \"Revised\" License)|(BSD-3-Clause)|(BSD.*3.*)",
        RegexOption.IGNORE_CASE
    )

    override val description: String = LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.description")
    override val permissions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.1"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.2"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.3"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.4")
    )
    override val permissionToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.1.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.2.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.3.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.permissions.4.tooltip")
    )
    override val limitations: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.limitations.1"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.limitations.2")
    )
    override val limitationsToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.limitations.1.tooltip"),
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.limitations.2.tooltip")
    )
    override val conditions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.conditions.1")
    )
    override val conditionsToolTips: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.bsd_3_clause.conditions.1.tooltip")
    )

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow][left,grow][left,grow]",
            "0[top][top][top][top][top][top][top]0"
        )

        add(RiderUI.createLicenseNameViewPanelLabel(this@BSD_3_Clause.name), "span")
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
        this,
        CDDL_1_0,
        EPL_1_0,
        GPL_2_0_only,
        GPL_2_0_with_classpath_exception,
        GPL_3_0_only,
        ISC,
        LGPL_2_1_only,
        LGPL_3_0_only,
        MIT,
        MPL_1_1,
        MPL_2_0,
        WTFPL
    )

    override val compatibleModuleLicensesBySubmoduleLicense: Set<SupportedLicense> = setOf(
        AGPL_3_0_only,
        Apache_2_0,
        BSD_2_Clause,
        this,
        CDDL_1_0,
        EPL_1_0,
        GPL_2_0_only,
        GPL_2_0_with_classpath_exception,
        GPL_3_0_only,
        ISC,
        LGPL_2_1_only,
        LGPL_3_0_only,
        MIT,
        MPL_1_1,
        MPL_2_0,
        WTFPL
    )
}
