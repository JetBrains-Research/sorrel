package com.jetbrains.sorrel.plugin

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

object SorrelBundle : AbstractBundle("messages.SorrelBundle") {

    fun message(
        @PropertyKey(resourceBundle = "messages.SorrelBundle") key: String,
        vararg params: Any
    ): String = getMessage(key, *params)
}