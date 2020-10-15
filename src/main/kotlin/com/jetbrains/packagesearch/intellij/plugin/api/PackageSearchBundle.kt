package com.jetbrains.packagesearch.intellij.plugin.api

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

object PackageSearchBundle : AbstractBundle("messages.packageSearchBundle") {

    fun message(
            @PropertyKey(resourceBundle = "messages.packageSearchBundle") key: String,
            vararg params: Any
    ): String = getMessage(key, *params)
}
