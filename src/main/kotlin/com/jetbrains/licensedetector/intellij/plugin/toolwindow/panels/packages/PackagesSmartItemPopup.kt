package com.jetbrains.licensedetector.intellij.plugin.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorPluginIcons
import com.jetbrains.licensedetector.intellij.plugin.model.InfoLink
import com.jetbrains.licensedetector.intellij.plugin.model.PackageDependency
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
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
            RiderUI.menuItem(
                LicenseDetectorBundle.message("licensedetector.ui.popup.show.online"),
                LicenseDetectorPluginIcons.Artifact
            ) {
                BrowserUtil.browse(
                    LicenseDetectorBundle.message(
                        "licensedetector.wellknown.url.jb.packagesearch.details",
                        meta.identifier
                    )
                )
            })
        addSeparator()

        meta.remoteInfo?.gitHub?.let {
            val gitHubLink = links.remove(InfoLink.GITHUB) ?: return@let
            add(
                RiderUI.menuItem(
                    LicenseDetectorBundle.message("licensedetector.ui.popup.open.github"),
                    AllIcons.Vcs.Vendors.Github
                ) {
                    BrowserUtil.browse(gitHubLink)
                })
        }

        meta.remoteInfo?.gitHub?.communityProfile?.files?.license?.let {
            val licenseUrl = it.htmlUrl ?: it.url
            val licenseName = it.name ?: LicenseDetectorBundle.message("licensedetector.terminology.license.unknown")
            if (!licenseUrl.isNullOrEmpty()) {
                add(
                    RiderUI.menuItem(
                        LicenseDetectorBundle.message(
                            "licensedetector.ui.popup.open.license",
                            licenseName
                        ), null
                    ) {
                        BrowserUtil.browse(licenseUrl)
                    })
            }
        }

        links.forEach {
            add(
                RiderUI.menuItem(
                    LicenseDetectorBundle.message(
                        "licensedetector.ui.popup.browse.thing",
                        it.key.displayName
                    ), null
                ) {
                    BrowserUtil.browse(it.value)
                })
        }

        // Other entries
        addSeparator()
        add(RiderUI.menuItem(LicenseDetectorBundle.message("licensedetector.ui.popup.copy.identifier"), null) {
            CopyPasteManager.getInstance().setContents(StringSelection(meta.identifier))
        })
    }
}
