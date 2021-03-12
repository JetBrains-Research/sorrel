package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel

object ISC : SupportedLicense {
    override val name: String = "ISC License"
    override val url: String = "https://opensource.org/licenses/ISC"
    override val htmlUrl: String = "https://opensource.org/licenses/ISC"
    override val spdxId: String = "ISC"
    override val priority: LicensePriority = LicensePriority.LOW
    override val fullText: String =
        """
                ISC License

                Copyright (c) 2004-2010 by Internet Systems Consortium, Inc. ("ISC")
                Copyright (c) 1995-2003 by Internet Software Consortium

                Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.

                THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
        """.trimIndent()

    override val nameSpdxRegex: Regex = Regex(
        "(ISC License)|(ISC)",
        RegexOption.IGNORE_CASE
    )

    override val description: String = LicenseDetectorBundle.message("licensedetector.ui.isc.description")
    override val permissions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.isc.permissions.1"),
        LicenseDetectorBundle.message("licensedetector.ui.isc.permissions.2"),
        LicenseDetectorBundle.message("licensedetector.ui.isc.permissions.3"),
        LicenseDetectorBundle.message("licensedetector.ui.isc.permissions.4")
    )
    override val limitations: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.isc.limitations.1"),
        LicenseDetectorBundle.message("licensedetector.ui.isc.limitations.2")
    )
    override val conditions: List<String> = listOf(
        LicenseDetectorBundle.message("licensedetector.ui.isc.conditions.1")
    )

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow][left,grow][left,grow]",
            "0[top][top][top][top][top][top][top]0"
        )

        add(RiderUI.createLicenseNameViewPanelLabel(this@ISC.name), "span")
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

        add(RiderUI.createLicensePermissionLabel(permissions[0]), "cell 0 3,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[1]), "cell 0 4,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[2]), "cell 0 5,growx")
        add(RiderUI.createLicensePermissionLabel(permissions[3]), "cell 0 6,growx")

        add(RiderUI.createLicenseLimitationsLabel(limitations[0]), "cell 1 3,growx")
        add(RiderUI.createLicenseLimitationsLabel(limitations[1]), "cell 1 4,growx")

        add(RiderUI.createLicenseConditionsLabel(conditions[0]), "cell 2 3,growx")
    }

    override val compatibleModuleLicenses: Set<SupportedLicense> = setOf(
        Apache_2_0,
        this,
        BSD_2_Clause,
        BSD_3_Clause,
        GPL_2_0_only,
        GPL_3_0_only,
        LGPL_2_1_only,
        LGPL_3_0_only,
        MIT
    )

    override val compatibleDependencyLicenses: Set<SupportedLicense> = setOf(
        Apache_2_0,
        MIT,
        this,
        BSD_2_Clause,
        BSD_3_Clause
    )
}
