package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left

import com.intellij.util.IconUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.licensedetector.intellij.plugin.ui.PackageSearchPluginIcons
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderColor
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toHtml
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

private val packageIconSize by lazy { JBUI.scale(16) }
private val packageIcon by lazy { IconUtil.toSize(PackageSearchPluginIcons.Package, packageIconSize, packageIconSize) }

class PackagesSmartRenderer(private val viewModel: LicenseDetectorToolWindowModel) : ListCellRenderer<PackagesSmartItem> {

    override fun getListCellRendererComponent(
            list: JList<out PackagesSmartItem>,
            item: PackagesSmartItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
    ): Component? =
            when (item) {
                is PackagesSmartItem.Package -> {
                    val iconLabel = JLabel(packageIcon).apply {
                        minimumSize = Dimension(packageIconSize, packageIconSize)
                        preferredSize = Dimension(packageIconSize, packageIconSize)
                        maximumSize = Dimension(packageIconSize, packageIconSize)
                    }

                    val packagePanel = createPackagePanel(item.meta, isSelected, list, iconLabel)
                    packagePanel
                }
                is PackagesSmartItem.Header -> item.panel
                is PackagesSmartItem.Fake -> PackagesSmartItem.Fake.panel
                null -> null
            }

    private fun createPackagePanel(
            packageSearchDependency: LicenseDetectorDependency,
            isSelected: Boolean,
            list: JList<out PackagesSmartItem>,
            iconLabel: JLabel
    ): JPanel {
        //val selectedProjectModule = viewModel.selectedProjectModule.value

        val textColor = RiderUI.getTextColor(isSelected)
        val textColor2 = RiderUI.getTextColor2(isSelected)

        val isMultiPlatform = packageSearchDependency.remoteInfo?.mpp != null

        return buildPanel(
                packageSearchDependency = packageSearchDependency,
                applyColors = applyColors(isSelected, list),
                iconLabel = iconLabel,
                idMessage = buildIdMessage(packageSearchDependency, textColor, textColor2),
                repositoryMessage = buildRepositoryMessage(isSelected, list, packageSearchDependency),
                //TODO: set version
                versionMessage = "",
                isMultiPlatform = isMultiPlatform
        )
    }

    private fun buildIdMessage(
            packageSearchDependency: LicenseDetectorDependency,
            textColor: RiderColor,
            textColor2: RiderColor
    ): String = buildString {
        if (packageSearchDependency.remoteInfo?.name != null && packageSearchDependency.remoteInfo?.name != packageSearchDependency.identifier) {
            append(colored(StringUtils.normalizeSpace(packageSearchDependency.remoteInfo?.name), textColor))
            append(" ")
            append(colored(packageSearchDependency.identifier, textColor2))
        } else {
            append(colored(packageSearchDependency.identifier, textColor))
        }
    }

    @Suppress("ComplexMethod")
    private fun buildRepositoryMessage(
            isSelected: Boolean,
            list: JList<out PackagesSmartItem>,
            packageSearchDependency: LicenseDetectorDependency
    ): String = buildString {

        /*val repositoryIdsForDependency =
            if (packageSearchDependency.isInstalled &&
                viewModel.areMultipleRepositoriesSelected() &&
                viewModel.areMultipleDistinctRepositoriesAvailable()) {

                // Show repository info for installed packages when multiple repositories are selected.
                // NOTE: Including blanks/variables here, as they count as "installed"
                val installedVersions = packageSearchDependency.installationInformation
                    .map { it.installedVersion }
                    .distinct()
                    .toList()

                (packageSearchDependency.remoteInfo?.versions ?: emptyList())
                    .filter { installedVersions.any { installed ->
                        installed.isBlank() || looksLikeGradleVariable(installed) || installed == it.version } }
                    .flatMap { it.repositoryIds ?: emptyList() }
                    .distinct()
            } else if (!packageSearchDependency.isInstalled &&
                viewModel.areMultipleRepositoriesSelected()) {

                // Show repository info for remote packages
                (packageSearchDependency.remoteInfo?.versions ?: emptyList())
                    .flatMap { it.repositoryIds ?: emptyList() }
                    .distinct()
            } else {
                emptyList()
            }

        if (repositoryIdsForDependency.isNotEmpty()) {
            val remoteRepositories = viewModel.remoteRepositories.value
                .filter { repositoryIdsForDependency.contains(it.id) }
                .distinctBy { it.id }

            if (remoteRepositories.isNotEmpty()) {
                append("<small>")
                append(" •")
                for (remoteRepository in remoteRepositories) {

                    val repositoryColor = if (isSelected) {
                        list.selectionForeground
                    } else {
                        viewModel.repositoryColorManager.getColor(remoteRepository).let { RepositoryColorManager.getIndicatorColor(it) }
                    }

                    append(colored(" " + remoteRepository.localizedName(), RiderColor(repositoryColor)))
                }
                append("</small>")
            }
        }*/
    }

    private fun applyColors(isSelected: Boolean, list: JList<out PackagesSmartItem>): (JComponent) -> Unit {
        val itemBackground = if (isSelected) list.selectionBackground else list.background
        val itemForeground = if (isSelected) list.selectionForeground else list.foreground

        return {
            it.background = itemBackground
            it.foreground = itemForeground
        }
    }

    @Suppress("LongParameterList")
    private fun buildPanel(
            packageSearchDependency: LicenseDetectorDependency,
            applyColors: (JComponent) -> Unit,
            iconLabel: JLabel,
            idMessage: String,
            repositoryMessage: String,
            versionMessage: String,
            isMultiPlatform: Boolean
    ): JPanel = JPanel(BorderLayout()).apply {
        @Suppress("MagicNumber") // Gotta love Swing APIs
        if (packageSearchDependency.identifier.isNotBlank()) {
            applyColors(this)
            border = JBEmptyBorder(0, 0, 0, 18)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBEmptyBorder(2, 8, 2, 4)

                add(iconLabel)
            }, BorderLayout.WEST)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JLabel("<html>$idMessage$repositoryMessage</html>"))
            }, BorderLayout.CENTER)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                if (versionMessage.isNotEmpty()) {
                    add(JLabel("<html>$versionMessage</html>"))
                } else if (isMultiPlatform) {
                    add(
                            RiderUI.createPlatformTag(
                                    PackageSearchBundle.message("packagesearch.terminology.multiplatform")
                            )
                    )
                }
            }, BorderLayout.EAST)
        }
    }

    private fun colored(text: String?, color: RiderColor) = "<font color=${color.toHtml()}>${text ?: ""}</font>"
}