package com.jetbrains.sorrel.plugin.licenses

import com.jetbrains.sorrel.plugin.SorrelUtilUI
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

    override val description: String = com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.description")
    override val permissions: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.1"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.2"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.3"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.4")
    )
    override val permissionToolTips: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.1.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.2.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.3.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.permissions.4.tooltip")
    )
    override val limitations: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.limitations.1"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.limitations.2")
    )
    override val limitationsToolTips: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.limitations.1.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.limitations.2.tooltip")
    )
    override val conditions: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.conditions.1")
    )
    override val conditionsToolTips: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.isc.conditions.1.tooltip")
    )

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = SorrelUtilUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow][left,grow][left,grow]",
            "0[top][top][top][top][top][top][top]0"
        )

        add(SorrelUtilUI.createLicenseNameViewPanelLabel(this@ISC.name), "span")
        add(SorrelUtilUI.createLabel("<html>${description}</html>"), "span")

        add(
            SorrelUtilUI.createHeaderLabel(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.licenseView.permissions")),
            "cell 0 2,growx"
        )
        add(
            SorrelUtilUI.createHeaderLabel(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.licenseView.limitations")),
            "cell 1 2,growx"
        )
        add(
            SorrelUtilUI.createHeaderLabel(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.licenseView.conditions")),
            "cell 2 2, growx"
        )

        add(SorrelUtilUI.createLicensePermissionLabel(permissions[0], permissionToolTips[0]), "cell 0 3,growx")
        add(SorrelUtilUI.createLicensePermissionLabel(permissions[1], permissionToolTips[1]), "cell 0 4,growx")
        add(SorrelUtilUI.createLicensePermissionLabel(permissions[2], permissionToolTips[2]), "cell 0 5,growx")
        add(SorrelUtilUI.createLicensePermissionLabel(permissions[3], permissionToolTips[3]), "cell 0 6,growx")

        add(SorrelUtilUI.createLicenseLimitationsLabel(limitations[0], limitationsToolTips[0]), "cell 1 3,growx")
        add(SorrelUtilUI.createLicenseLimitationsLabel(limitations[1], limitationsToolTips[1]), "cell 1 4,growx")

        add(SorrelUtilUI.createLicenseConditionsLabel(conditions[0], conditionsToolTips[0]), "cell 2 3,growx")
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
        this,
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
        BSD_3_Clause,
        CDDL_1_0,
        EPL_1_0,
        GPL_2_0_only,
        GPL_2_0_with_classpath_exception,
        GPL_3_0_only,
        this,
        LGPL_2_1_only,
        LGPL_3_0_only,
        MIT,
        MPL_1_1,
        MPL_2_0,
        WTFPL
    )
}
