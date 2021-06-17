package com.jetbrains.sorrel.plugin.licenses

import com.jetbrains.sorrel.plugin.SorrelUtilUI
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

object WTFPL : SupportedLicense {
    override val name: String = "Do What The F*ck You Want To Public License"
    override val url: String = "http://www.wtfpl.net/about/"
    override val htmlUrl: String = "http://sam.zoy.org/wtfpl/COPYING"
    override val spdxId: String = "WTFPL"
    override val priority: LicensePriority = LicensePriority.LOW
    override val fullText: String =
        """
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
            Version 2, December 2004

            Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>

            Everyone is permitted to copy and distribute verbatim or modified copies of this license document, and changing it is allowed as long as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
            TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

            0. You just DO WHAT THE FUCK YOU WANT TO.
        """.trimIndent()

    override val nameSpdxRegex: Regex = Regex(
        "(Do What The F\\*ck You Want To Public License)|(.*WTFPL)",
        RegexOption.IGNORE_CASE
    )

    override val description: String = com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.description")
    override val permissions: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.1"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.2"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.3"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.4"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.5")
    )
    override val permissionToolTips: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.1.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.2.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.3.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.4.tooltip"),
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.permissions.5.tooltip")
    )
    override val limitations: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.limitations.1"),
    )
    override val limitationsToolTips: List<String> = listOf()
    override val conditions: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.conditions.1")
    )
    override val conditionsToolTips: List<String> = listOf(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.wtfpl.conditions.1.tooltip")
    )

    override fun descriptionPanel(): JComponent = JPanel().apply {
        background = SorrelUtilUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow][left,grow][left,grow]",
            "0[top][top][top][top][top][top][top][top]0"
        )

        add(SorrelUtilUI.createLicenseNameViewPanelLabel(this@WTFPL.name), "span")
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
        add(SorrelUtilUI.createLicensePermissionLabel(permissions[4], permissionToolTips[4]), "cell 0 7,growx")

        add(JLabel(limitations[0]), "cell 1 3,growx")

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
        ISC,
        LGPL_2_1_only,
        LGPL_3_0_only,
        MIT,
        MPL_1_1,
        MPL_2_0,
        this
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
        MIT,
        MPL_1_1,
        MPL_2_0,
        this
    )
}