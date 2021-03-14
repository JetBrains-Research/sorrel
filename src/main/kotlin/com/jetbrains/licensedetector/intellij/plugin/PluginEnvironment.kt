package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

class PluginEnvironment {

    companion object {
        const val PLUGIN_ID = "com.jetbrains.licensedetector.intellij-plugin"
    }

    val pluginVersion
        get() = PluginManagerCore.getPlugin(PluginId.getId("license-detector-plugin"))?.version
            ?: LicenseDetectorBundle.message("licensedetector.version.undefined")

    val ideVersion
        get() = ApplicationInfo.getInstance().strictVersion
}
