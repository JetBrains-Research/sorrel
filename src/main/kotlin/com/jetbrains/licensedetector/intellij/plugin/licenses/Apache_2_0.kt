package com.jetbrains.licensedetector.intellij.plugin.licenses

object Apache_2_0 : SupportedLicense {
    override val name: String = "Apache License 2.0"
    override val url: String = "http://www.apache.org/licenses/LICENSE-2.0"
    override val htmlUrl: String = "http://www.apache.org/licenses/LICENSE-2.0"
    override val spdxId: String = "Apache-2.0"
    override val compatiblePackageLicenses: Set<SupportedLicense> = setOf(
            this,
            BSD_3_Clause,
            GPL_3_0_or_later,
            LGPL_2_1_or_later,
            MIT
    )
}