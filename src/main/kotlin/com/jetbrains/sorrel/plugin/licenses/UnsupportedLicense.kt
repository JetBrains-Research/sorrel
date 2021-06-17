package com.jetbrains.sorrel.plugin.licenses

import com.google.gson.annotations.SerializedName

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
            get() = pureName + com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.license.unsupported.postfix")
}