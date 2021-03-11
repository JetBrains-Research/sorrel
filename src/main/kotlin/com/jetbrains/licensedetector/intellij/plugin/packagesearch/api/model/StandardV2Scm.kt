package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model

import com.google.gson.annotations.SerializedName

data class StandardV2Scm(

        @SerializedName("url")
        val url: String?
)
