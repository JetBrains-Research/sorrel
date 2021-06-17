package com.jetbrains.sorrel.plugin.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.sorrel.plugin.SorrelUtilUI
import com.jetbrains.sorrel.plugin.model.InfoLink
import com.jetbrains.sorrel.plugin.model.PackageDependency
import java.awt.datatransfer.StringSelection
import javax.swing.JPopupMenu

class PackagesSmartItemPopup(private val meta: PackageDependency) : JPopupMenu() {

    init {
        // All links on package
        val links = meta.getAllLinks()

        // Encourage using Package Search website!
        links.remove(InfoLink.README)
        links.remove(InfoLink.CODE_OF_CONDUCT)
        add(
            SorrelUtilUI.menuItem(
                com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.popup.show.online"),
                com.jetbrains.sorrel.plugin.SorrelPluginIcons.Artifact
            ) {
                BrowserUtil.browse(
                    com.jetbrains.sorrel.plugin.SorrelBundle.message(
                        "sorrel.wellknown.url.jb.packagesearch.details",
                        meta.identifier
                    )
                )
            })
        addSeparator()

        meta.remoteInfo?.gitHub?.let {
            val gitHubLink = links.remove(InfoLink.GITHUB) ?: return@let
            add(
                SorrelUtilUI.menuItem(
                    com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.popup.open.github"),
                    AllIcons.Vcs.Vendors.Github
                ) {
                    BrowserUtil.browse(gitHubLink)
                })
        }

        meta.remoteInfo?.gitHub?.communityProfile?.files?.license?.let {
            val licenseUrl = it.htmlUrl ?: it.url
            val licenseName =
                it.name ?: com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.terminology.license.unknown")
            if (!licenseUrl.isNullOrEmpty()) {
                add(
                    SorrelUtilUI.menuItem(
                        com.jetbrains.sorrel.plugin.SorrelBundle.message(
                            "sorrel.ui.popup.open.license",
                            licenseName
                        ), null
                    ) {
                        BrowserUtil.browse(licenseUrl)
                    })
            }
        }

        links.forEach {
            add(
                SorrelUtilUI.menuItem(
                    com.jetbrains.sorrel.plugin.SorrelBundle.message(
                        "sorrel.ui.popup.browse.thing",
                        it.key.displayName
                    ), null
                ) {
                    BrowserUtil.browse(it.value)
                })
        }

        // Other entries
        addSeparator()
        add(
            SorrelUtilUI.menuItem(
                com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.popup.copy.identifier"),
                null
            ) {
                CopyPasteManager.getInstance().setContents(StringSelection(meta.identifier))
            })
    }
}
