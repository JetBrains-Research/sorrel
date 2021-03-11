package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

class PluginEnvironment {

    val pluginVersion
        get() = PluginManager.getPlugin(PluginId.getId("license-detector-plugin"))?.version
            ?: LicenseDetectorBundle.message("licensedetector.version.undefined")

    val ideVersion
        get() = ApplicationInfo.getInstance().strictVersion!!
}
