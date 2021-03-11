package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model

import com.google.gson.annotations.SerializedName

data class StandardV2Author(

    // Requires whitespace cleanup
        @SerializedName("name")
        val name: String?,

        @SerializedName("org")
        val org: String?,

        @SerializedName("org_url")
        val orgUrl: String?
)
