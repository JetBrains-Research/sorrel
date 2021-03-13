package com.jetbrains.licensedetector.intellij.plugin.utils

import com.intellij.openapi.util.registry.Registry

object FeatureFlags {

    val useDebugLogging: Boolean
        get() = Registry.`is`("licensedetector.plugin.debug.logging", false)
}
