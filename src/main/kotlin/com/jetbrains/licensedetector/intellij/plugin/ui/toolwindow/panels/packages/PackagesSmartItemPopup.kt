package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.licensedetector.intellij.plugin.ui.PackageSearchPluginIcons
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.InfoLink
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle
import java.awt.datatransfer.StringSelection
import javax.swing.JPopupMenu

class PackagesSmartItemPopup(private val meta: LicenseDetectorDependency) : JPopupMenu() {

    init {
        // All links on package
        val links = meta.getAllLinks()

        // Encourage using Package Search website!
        links.remove(InfoLink.README)
        links.remove(InfoLink.CODE_OF_CONDUCT)
        add(RiderUI.menuItem(PackageSearchBundle.message("packagesearch.ui.popup.show.online"), PackageSearchPluginIcons.Artifact) {
            BrowserUtil.browse(PackageSearchBundle.message("packagesearch.wellknown.url.jb.packagesearch.details", meta.identifier))
        })
        addSeparator()

        meta.remoteInfo?.gitHub?.let {
            val gitHubLink = links.remove(InfoLink.GITHUB) ?: return@let
            add(RiderUI.menuItem(PackageSearchBundle.message("packagesearch.ui.popup.open.github"), AllIcons.Vcs.Vendors.Github) {
                BrowserUtil.browse(gitHubLink)
            })
        }

        meta.remoteInfo?.gitHub?.communityProfile?.files?.license?.let {
            val licenseUrl = it.htmlUrl ?: it.url
            val licenseName = it.name ?: PackageSearchBundle.message("packagesearch.terminology.license.unknown")
            if (!licenseUrl.isNullOrEmpty()) {
                add(RiderUI.menuItem(PackageSearchBundle.message("packagesearch.ui.popup.open.license", licenseName), null) {
                    BrowserUtil.browse(licenseUrl)
                })
            }
        }

        links.forEach {
            add(RiderUI.menuItem(PackageSearchBundle.message("packagesearch.ui.popup.browse.thing", it.key.displayName), null) {
                BrowserUtil.browse(it.value)
            })
        }

        // Other entries
        addSeparator()
        add(RiderUI.menuItem(PackageSearchBundle.message("packagesearch.ui.popup.copy.identifier"), null) {
            CopyPasteManager.getInstance().setContents(StringSelection(meta.identifier))
        })
    }
}
