package com.jetbrains.sorrel.plugin.model

enum class InfoLink(val displayName: String) {
    PROJECT_SITE(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.projectSite")),
    DOCUMENTATION(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.documentation")),
    README(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.readme")),
    CODE_OF_CONDUCT(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.codeOfConduct")),
    GITHUB(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.github")),
    SCM(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.link.scm"))
}
