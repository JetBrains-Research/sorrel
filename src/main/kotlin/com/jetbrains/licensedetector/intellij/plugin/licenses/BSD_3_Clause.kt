package com.jetbrains.licensedetector.intellij.plugin.licenses

object BSD_3_Clause : SupportedLicense {
    override val name: String = "BSD 3-Clause \"New\" or \"Revised\" License"
    override val url: String = "https://opensource.org/licenses/BSD-3-Clause"
    override val htmlUrl: String = "https://opensource.org/licenses/BSD-3-Clause"
    override val spdxId: String = "BSD-3-Clause"
    override val compatiblePackageLicenses: Set<SupportedLicense> = setOf(
            Apache_2_0,
            this,
            GPL_3_0_or_later,
            LGPL_2_1_or_later,
            MIT
    )
}
