package com.jetbrains.sorrel.plugin.packagesearch.api.model

import com.google.gson.annotations.SerializedName

data class StandardV2GitHub(

    // Requires whitespace cleanup
        @SerializedName("description")
        val description: String?,

        @SerializedName("is_fork")
        val isFork: Boolean?,

        @SerializedName("stars")
        val stars: Int,

        @SerializedName("watchers")
        val watchers: Int,

        @SerializedName("forks")
        val forks: Int,

        @SerializedName("subscribers")
        val subscribers: Int,

        @SerializedName("network")
        val network: Int,

        @SerializedName("community_profile")
        val communityProfile: StandardV2GitHubCommunityProfile?,

        @SerializedName("last_checked")
        val lastChecked: Long?
)

data class StandardV2GitHubCommunityProfile(

        @SerializedName("files")
        val files: StandardV2GitHubCommunityProfileFiles?,

        @SerializedName("documentation")
        val documentationUrl: String?,

    // Requires whitespace cleanup
        @SerializedName("description")
        val description: String?,

        @SerializedName("health_percentage")
        val healthPercentage: Int?
)

data class StandardV2GitHubCommunityProfileFiles(

        @SerializedName("license")
        val license: StandardV2LinkedFile?,

        @SerializedName("readme")
        val readme: StandardV2LinkedFile?,

        @SerializedName("code_of_conduct")
        val codeOfConduct: StandardV2LinkedFile?
)
