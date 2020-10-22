package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

object LicenseDetectorBundle : AbstractBundle("messages.licenseDetectorBundle") {

    fun message(
            @PropertyKey(resourceBundle = "messages.licenseDetectorBundle") key: String,
            vararg params: Any
    ): String = getMessage(key, *params)
}