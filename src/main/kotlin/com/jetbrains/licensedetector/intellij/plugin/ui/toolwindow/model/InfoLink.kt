package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle

enum class InfoLink(val displayName: String) {
    PROJECT_SITE(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.projectSite")),
    DOCUMENTATION(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.documentation")),
    README(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.readme")),
    CODE_OF_CONDUCT(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.codeOfConduct")),
    GITHUB(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.github")),
    SCM(PackageSearchBundle.message("packagesearch.ui.toolwindow.link.scm"))
}
