package com.jetbrains.licensedetector.intellij.plugin.licenses

interface SupportedLicense : License {
    val compatiblePackageLicenses: Set<SupportedLicense>
}