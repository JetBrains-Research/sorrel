package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle

enum class InfoLink(val displayName: String) {
    PROJECT_SITE(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.projectSite")),
    DOCUMENTATION(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.documentation")),
    README(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.readme")),
    CODE_OF_CONDUCT(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.codeOfConduct")),
    GITHUB(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.github")),
    SCM(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.link.scm"))
}
