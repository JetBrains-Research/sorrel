package com.jetbrains.licensedetector.intellij.plugin.licenses

object GPL_3_0_or_later : SupportedLicense {
    override val name: String = "GNU General Public License v3.0 or later"
    override val url: String = "https://www.gnu.org/licenses/gpl-3.0-standalone.html"
    override val htmlUrl: String = "https://www.gnu.org/licenses/gpl-3.0-standalone.html"
    override val spdxId: String = "GPL-3.0-or-later"
    override val compatiblePackageLicenses: Set<SupportedLicense> = setOf(
            this
    )
}