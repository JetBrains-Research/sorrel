package com.jetbrains.licensedetector.intellij.plugin.licenses

object MIT : SupportedLicense {
    override val name: String = "MIT License"
    override val url: String = "https://opensource.org/licenses/MIT"
    override val htmlUrl: String = "https://opensource.org/licenses/MIT"
    override val spdxId: String = "MIT"
    override val compatiblePackageLicenses: Set<SupportedLicense> = setOf(
            Apache_2_0,
            BSD_3_Clause,
            GPL_3_0_or_later,
            LGPL_2_1_or_later,
            this
    )
}