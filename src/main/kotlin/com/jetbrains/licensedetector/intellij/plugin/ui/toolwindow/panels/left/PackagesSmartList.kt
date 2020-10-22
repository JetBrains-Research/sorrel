package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.components.JBList
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle
import java.awt.Cursor
import java.awt.Point
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel

class PackagesSmartList(val viewModel: LicenseDetectorToolWindowModel) :
        JBList<PackagesSmartItem>(emptyList()), DataProvider, CopyProvider {

    var transferFocusUp: () -> Unit = { transferFocusBackward() }

    private val updateContentLock = Object()

    private val listModel: DefaultListModel<PackagesSmartItem>
        get() = model as DefaultListModel<PackagesSmartItem>

    private val headerLinkHandler = createLinkActivationListener(this)
    //TODO: IS needed?
    /*private val upgradeAllLink = HyperlinkLabel(PackageSearchBundle.message("packagesearch.ui.toolwindow.actions.upgradeAll.text")).apply {
        isVisible = false

        addHyperlinkListener {
            if (viewModel.upgradeCountInContext.value > 0) {
                val selectedProjectModule = viewModel.selectedProjectModule.value
                val selectedRemoteRepository = viewModel.selectedRemoteRepository.value
                val selectedRemoteRepositoryIds = selectedRemoteRepository.asList()
                val projectModules = if (selectedProjectModule != null) {
                    listOf(selectedProjectModule)
                } else {
                    viewModel.projectModules.value
                }

                val selectedOnlyStable = viewModel.selectedOnlyStable.value
                val upgradeRequests = viewModel.preparePackageOperationTargetsFor(projectModules, selectedRemoteRepository)
                    .filter {
                        it.version.isNotBlank() &&
                            !looksLikeGradleVariable(it.version) &&
                            VersionComparatorUtil.compare(it.version, it.packageSearchDependency.getLatestAvailableVersion(
                                selectedOnlyStable, selectedRemoteRepositoryIds)) < 0
                    }
                    .sortedBy { it.projectModule.getFullName() }

                val message = PackageSearchBundle.message("packagesearch.ui.toolwindow.actions.upgradeAll.text")
                val upgradeRequestsToExecute = PackageOperationsConfirmationDialog.show(
                    viewModel.project,
                    message + (selectedProjectModule?.name?.let {
                        " ${PackageSearchBundle.message("packagesearch.ui.toolwindow.actions.upgradeAll.button.titleSuffix", it)}"
                    } ?: ""),
                    PackageSearchBundle.message("packagesearch.ui.toolwindow.actions.upgradeAll.button.text"),
                    selectedOnlyStable,
                    selectedRemoteRepositoryIds,
                    upgradeRequests)

                if (upgradeRequestsToExecute.any()) {
                    viewModel.executeOperations(upgradeRequestsToExecute.map { target ->
                        val targetVersion = target.packageSearchDependency.getLatestAvailableVersion(
                            selectedOnlyStable, selectedRemoteRepositoryIds) ?: ""
                        val operation = target.getApplyOperation(targetVersion)!!
                        ExecutablePackageOperation(operation, target, targetVersion)
                    })
                }
            }
        }
    }*/

    val installedHeader = PackagesSmartItem.Header(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.installedPackages"))
            .apply {
                //TODO: Is needed?
                //addHeaderLink(upgradeAllLink)
            }

    private val packageItems: List<PackagesSmartItem.Package> get() = listModel.elements().toList().filterIsInstance<PackagesSmartItem.Package>()
    val hasPackageItems: Boolean get() = packageItems.any()
    val firstPackageIndex: Int get() = listModel.elements().toList().indexOfFirst { it is PackagesSmartItem.Package }

    //TODO: mb
    //private val packageSelectionListeners = ArrayList<(PackageSearchDependency) -> Unit>()

    init {
        @Suppress("UnstableApiUsage") // yolo
        putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)

        cellRenderer = PackagesSmartRenderer(viewModel)

        listModel.addElement(installedHeader)

        RiderUI.overrideKeyStroke(this, "jlist:RIGHT", "RIGHT") { transferFocus() }
        RiderUI.overrideKeyStroke(this, "jlist:ENTER", "ENTER") { transferFocus() }
        RiderUI.overrideKeyStroke(this, "shift ENTER") { this.transferFocusUp() }
        //TODO: For popup
        /*RiderUI.addKeyboardPopupHandler(this, "alt ENTER") { items ->
            val item = items.first()
            if (item is PackagesSmartItem.Package) PackagesSmartItemPopup(viewModel, item.meta) else null
        }*/

        //TODO: For selection liseners
        /*addListSelectionListener {
            val item = selectedValue
            if (selectedIndex >= 0 && item is PackagesSmartItem.Package) {
                ensureIndexIsVisible(selectedIndex)
                for (listener in packageSelectionListeners) {
                    listener(item.meta)
                }
            }
        }*/

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                if (hasPackageItems && selectedIndex == -1) {
                    selectedIndex = firstPackageIndex
                }
            }
        })

        installPopupHandler()

        addMouseListener(headerLinkHandler)
        addMouseMotionListener(headerLinkHandler)

        ListSpeedSearch(this) {
            if (it is PackagesSmartItem.Package) {
                it.meta.identifier
            } else {
                ""
            }
        }.apply {
            comparator = SpeedSearchComparator(false)
        }

        //TODO: Don't know what is it
        /*viewModel.searchTerm.advise(viewModel.lifetime) {
            upgradeAllLink.isVisible = viewModel.upgradeCountInContext.value > 0 && it.isEmpty()
        }
        viewModel.upgradeCountInContext.advise(viewModel.lifetime) {
            if (it > 0 && viewModel.searchTerm.value.isBlank()) {
                upgradeAllLink.setHyperlinkText(PackageSearchBundle.message("packagesearch.ui.toolwindow.actions.upgradeAll.text.withCount", it))
                upgradeAllLink.isVisible = true
            } else {
                upgradeAllLink.isVisible = false
            }
        }*/
    }

    private fun installPopupHandler() {
        //TODO: Need fix
        /*
        val list = this
        list.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: Component?, x: Int, y: Int) {
                val index = list.locationToIndex(Point(x, y - 1))
                if (index != -1) {
                    val element = list.model.getElementAt(index)
                    if (element != null && element is PackagesSmartItem.Package) {
                        if (selectedValue == null || selectedValue as PackagesSmartItem != element) {
                            setSelectedValue(element, true)
                        }
                        val popup = PackagesSmartItemPopup(viewModel, element.meta)
                        popup.show(list, x, y)
                    }
                }
            }
        })

         */
    }

    private fun calcDisplayItems(packages: List<LicenseDetectorDependency>): List<PackagesSmartItem> {
        val displayItems = mutableListOf<PackagesSmartItem>()

        val installedPackages = packages.filter { it.isInstalled }

        val message = PackageSearchBundle.message(
                "packagesearch.ui.toolwindow.tab.packages.installedPackages.withCount",
                installedPackages.size
        )
        installedHeader.title = message

        displayItems.add(installedHeader)
        displayItems.addAll(installedPackages.map { PackagesSmartItem.Package(it) })
        return displayItems
    }

    fun updateAllPackages(packages: List<LicenseDetectorDependency>) {
        synchronized(updateContentLock) {
            val displayItems = calcDisplayItems(packages)

            // save selected package Id; we have to restore selection after the list rebuilding
            //val selectedPackageIndex = selectedIndex

            /*val selectedPackageId = viewModel.selectedPackage.value.apply {
                if (isEmpty()) {
                    (selectedValue as PackagesSmartItem.Package?)?.meta?.identifier
                }
            }
            var reselected = false*/

            for ((index, item) in displayItems.withIndex()) {
                if (index < listModel.size()) {
                    listModel.set(index, item)
                } else {
                    listModel.add(index, item)
                }

                /*if (item is PackagesSmartItem.Package && item.meta.identifier == selectedPackageId) {
                    if (index != selectedPackageIndex) {
                        selectedIndex = index
                    }

                    reselected = true
                }*/
            }

            if (listModel.size() > displayItems.size) {
                listModel.removeRange(displayItems.size, listModel.size() - 1)
            }

            /*if (!reselected) {
                // if there is no the old selected package in the new list
                clearSelection() // we have to clear the selection
            }*/
        }
    }

    //TODO: Not need
    /*fun addPackageSelectionListener(listener: (PackageSearchDependency) -> Unit) {
        packageSelectionListeners.add(listener)
    }*/

    private fun getSelectedPackage(): PackagesSmartItem.Package? =
            if (this.selectedIndex != -1) {
                listModel.getElementAt(this.selectedIndex) as? PackagesSmartItem.Package
            } else {
                null
            }

    //TODO: Maybe need for module
    //override fun getData(dataId: String) = getSelectedPackage()?.getData(dataId, viewModel.selectedProjectModule.value)
    override fun getData(dataId: String) = getSelectedPackage()?.getData(dataId, null)

    override fun performCopy(dataContext: DataContext) {
        getSelectedPackage()?.performCopy(dataContext)
    }

    override fun isCopyEnabled(dataContext: DataContext) = getSelectedPackage()?.isCopyEnabled(dataContext) ?: false
    override fun isCopyVisible(dataContext: DataContext) = getSelectedPackage()?.isCopyVisible(dataContext) ?: false

    private fun createLinkActivationListener(list: JBList<PackagesSmartItem>) = object : MouseAdapter() {

        @Suppress("TooGenericExceptionCaught") // Guarding against random runtime failures
        private val hyperlinkHoverField = try {
            HyperlinkLabel::class.java.getDeclaredField("myMouseHover")
        } catch (e: Throwable) {
            null
        }

        private var lastHoveredHyperlinkLabel: HyperlinkLabel? = null

        private fun trySetHovering(target: HyperlinkLabel?, value: Boolean) {
            if (target == null) return

            try {
                hyperlinkHoverField?.isAccessible = true
                hyperlinkHoverField?.set(target, value)
            } catch (ignored: Exception) {
                // No-op
            }
        }

        override fun mouseMoved(e: MouseEvent) {
            val hyperlinkLabel = findHyperlinkLabelAt(e.point)
            if (hyperlinkLabel != null) {
                if (hyperlinkLabel != lastHoveredHyperlinkLabel) {
                    trySetHovering(lastHoveredHyperlinkLabel, false)
                    trySetHovering(hyperlinkLabel, true)

                    lastHoveredHyperlinkLabel = hyperlinkLabel
                    list.repaint()
                }

                UIUtil.setCursor(list, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
            } else {
                if (lastHoveredHyperlinkLabel != null) {
                    trySetHovering(lastHoveredHyperlinkLabel, false)

                    lastHoveredHyperlinkLabel = null
                    list.repaint()
                }

                UIUtil.setCursor(list, Cursor.getDefaultCursor())
            }
        }

        override fun mouseClicked(e: MouseEvent) {
            val hyperlinkLabel = findHyperlinkLabelAt(e.point)
            if (hyperlinkLabel != null) {
                trySetHovering(lastHoveredHyperlinkLabel, false)
                trySetHovering(hyperlinkLabel, false)
                lastHoveredHyperlinkLabel = null
                list.repaint()
                hyperlinkLabel.doClick()
            }
        }

        private fun findHyperlinkLabelAt(point: Point): HyperlinkLabel? {
            val idx = list.locationToIndex(point)
            if (idx < 0) return null

            val cellBounds = list.getCellBounds(idx, idx)
            if (!cellBounds.contains(point)) return null

            val item = list.model.getElementAt(idx) as? PackagesSmartItem.Header ?: return null

            val rendererComponent = list.cellRenderer.getListCellRendererComponent(list, item, idx, true, true)
            rendererComponent.setBounds(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height)
            UIUtil.layoutRecursively(rendererComponent)

            val rendererRelativeX = point.x - cellBounds.x
            val rendererRelativeY = point.y - cellBounds.y
            val childComponent = UIUtil.getDeepestComponentAt(rendererComponent, rendererRelativeX, rendererRelativeY)
            if (childComponent is HyperlinkLabel) return childComponent

            return null
        }
    }
}
