package com.jetbrains.licensedetector.intellij.plugin.licenses

import com.google.gson.annotations.SerializedName

data class UnsupportedLicense(
        @SerializedName("name")
        override val name: String?,
        @SerializedName("url")
        override val url: String?,
        @SerializedName("html_url")
        override val htmlUrl: String?,
        @SerializedName("spdx_id")
        override val spdxId: String?
) : License