package com.jetbrains.sorrel.plugin.licenses

interface License {

    //Field from PackageSearch response
    val name: String
    val url: String?
    val htmlUrl: String?
    val spdxId: String?
}