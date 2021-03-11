package com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.model

import com.google.gson.annotations.SerializedName

data class StandardV2StackOverflowTags(

        @SerializedName("tags")
        val tags: List<StackOverflowTag>
)

data class StackOverflowTag(

        @SerializedName("tag")
        val tag: String,

        @SerializedName("count")
        val count: Int
)
