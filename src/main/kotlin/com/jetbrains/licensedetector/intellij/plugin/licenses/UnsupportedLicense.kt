package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.google.gson.annotations.SerializedName
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle

data class UnsupportedLicense(
        @SerializedName("name")
        private val pureName: String?,
        @SerializedName("url")
        override val url: String?,
        @SerializedName("html_url")
        override val htmlUrl: String?,
        @SerializedName("spdx_id")
        override val spdxId: String?
) : License {
        override val name: String
            get() = pureName + LicenseDetectorBundle.message("licensedetector.license.unsupported.postfix")
}