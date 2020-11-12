package com.jetbrains.licensedetector.intellij.plugin.licenses

object LGPL_2_1_or_later : SupportedLicense {
    override val name: String = "GNU Lesser General Public License v2.1 or later"
    override val url: String = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html"
    override val htmlUrl: String = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html"
    override val spdxId: String = "LGPL-2.1-or-later"
    override val compatiblePackageLicenses: Set<SupportedLicense> = setOf(
            GPL_3_0_or_later,
            this
    )
}