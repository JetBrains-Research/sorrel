package com.jetbrains.sorrel.plugin.packagesearch.api.model

import com.google.gson.annotations.SerializedName
import com.jetbrains.sorrel.plugin.licenses.License

data class StandardV2Licenses(

        @SerializedName("main_license")
        val mainLicense: License?,

        @SerializedName("other_licenses")
        val otherLicenses: Set<License>?
)
