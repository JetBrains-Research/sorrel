package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages.components

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.PopupHandler
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.components.JBList
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle
import java.awt.Component
import java.awt.Point
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.*
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.ListSelectionModel

class PackagesSmartList(val viewModel: LicenseDetectorToolWindowModel) :
        JBList<PackagesSmartItem>(emptyList()), DataProvider, CopyProvider {

    var transferFocusUp: () -> Unit = { transferFocusBackward() }

    private val updateContentLock = Object()

    private val listModel: DefaultListModel<PackagesSmartItem>
        get() = model as DefaultListModel<PackagesSmartItem>

    val installedHeader = PackagesSmartItem.Header(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.installedPackages"))

    private val packageItems: List<PackagesSmartItem.Package> get() = listModel.elements().toList().filterIsInstance<PackagesSmartItem.Package>()
    val hasPackageItems: Boolean get() = packageItems.any()
    val firstPackageIndex: Int get() = listModel.elements().toList().indexOfFirst { it is PackagesSmartItem.Package }

    private val packageSelectionListeners = ArrayList<(LicenseDetectorDependency) -> Unit>()

    init {
        @Suppress("UnstableApiUsage") // yolo
        putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)

        cellRenderer = PackagesSmartRenderer()
        selectionModel = BriefItemsSelectionModel()

        listModel.addElement(installedHeader)

        RiderUI.overrideKeyStroke(this, "jlist:RIGHT", "RIGHT") { transferFocus() }
        RiderUI.overrideKeyStroke(this, "jlist:ENTER", "ENTER") { transferFocus() }
        RiderUI.overrideKeyStroke(this, "shift ENTER") { this.transferFocusUp() }
        RiderUI.addKeyboardPopupHandler(this, "alt ENTER") { items ->
            val item = items.first()
            if (item is PackagesSmartItem.Package) PackagesSmartItemPopup(item.meta) else null
        }

        addListSelectionListener {
            val item = selectedValue
            if (selectedIndex >= 0 && item is PackagesSmartItem.Package) {
                ensureIndexIsVisible(selectedIndex)
                for (listener in packageSelectionListeners) {
                    listener(item.meta)
                }
            }
        }

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                if (hasPackageItems && selectedIndex == -1) {
                    selectedIndex = firstPackageIndex
                }
            }
        })

        installPopupHandler()

        ListSpeedSearch(this) {
            if (it is PackagesSmartItem.Package) {
                it.meta.identifier
            } else {
                ""
            }
        }.apply {
            comparator = SpeedSearchComparator(false)
        }
    }

    private fun installPopupHandler() {

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
                        val popup = PackagesSmartItemPopup(element.meta)
                        popup.show(list, x, y)
                    }
                }
            }
        })
    }

    private fun calcDisplayItems(packages: List<LicenseDetectorDependency>): List<PackagesSmartItem> {
        val displayItems = mutableListOf<PackagesSmartItem>()

        val installedPackages = packages.filter { it.isInstalled }

        val message = PackageSearchBundle.message(
                "packagesearch.ui.toolwindow.tab.packages.installedPackages.withCount",
                installedPackages.size
        )
        installedHeader.title = message + (viewModel.selectedProjectModule.value?.name
                ?.let { " ${PackageSearchBundle.message("packagesearch.ui.toolwindow.tab.packages.installedPackages.titleSuffix", it)}" }
                ?: "")

        displayItems.add(installedHeader)
        displayItems.addAll(installedPackages.map { PackagesSmartItem.Package(it) })
        return displayItems
    }

    fun updateAllPackages(packages: List<LicenseDetectorDependency>) {
        synchronized(updateContentLock) {
            val displayItems = calcDisplayItems(packages)

            // save selected package Id; we have to restore selection after the list rebuilding
            val selectedPackageIndex = selectedIndex

            val selectedPackageId = viewModel.selectedPackage.value.apply {
                if (isEmpty()) {
                    (selectedValue as PackagesSmartItem.Package?)?.meta?.identifier
                }
            }
            var reselected = false

            for ((index, item) in displayItems.withIndex()) {
                if (index < listModel.size()) {
                    listModel.set(index, item)
                } else {
                    listModel.add(index, item)
                }

                if (item is PackagesSmartItem.Package && item.meta.identifier == selectedPackageId) {
                    if (index != selectedPackageIndex) {
                        selectedIndex = index
                    }

                    reselected = true
                }
            }

            if (listModel.size() > displayItems.size) {
                listModel.removeRange(displayItems.size, listModel.size() - 1)
            }

            if (!reselected) {
                // if there is no the old selected package in the new list
                clearSelection() // we have to clear the selection
            }
        }
    }

    fun addPackageSelectionListener(listener: (LicenseDetectorDependency) -> Unit) {
        packageSelectionListeners.add(listener)
    }

    // It is possible to select only package items; fakes and headers should be ignored
    private inner class BriefItemsSelectionModel : DefaultListSelectionModel() {

        init {
            this.selectionMode = ListSelectionModel.SINGLE_SELECTION // TODO: MULTIPLE_INTERVAL_SELECTION support
        }

        private fun isPackageItem(index: Int) = listModel.getElementAt(index) is PackagesSmartItem.Package

        override fun setSelectionInterval(index0: Int, index1: Int) {
            if (isPackageItem(index0)) {
                super.setSelectionInterval(index0, index0)
                return
            }

            if (anchorSelectionIndex < index0) {
                for (i in index0 until listModel.size()) {
                    if (isPackageItem(i)) {
                        super.setSelectionInterval(i, i)
                        return
                    }
                }
            } else {
                for (i in index0 downTo 0) {
                    if (isPackageItem(i)) {
                        super.setSelectionInterval(i, i)
                        return
                    }
                }
                super.clearSelection()
                transferFocusUp()
            }
        }
    }

    private fun getSelectedPackage(): PackagesSmartItem.Package? =
            if (this.selectedIndex != -1) {
                listModel.getElementAt(this.selectedIndex) as? PackagesSmartItem.Package
            } else {
                null
            }

    override fun getData(dataId: String) = getSelectedPackage()?.getData(dataId)

    override fun performCopy(dataContext: DataContext) {
        getSelectedPackage()?.performCopy(dataContext)
    }

    override fun isCopyEnabled(dataContext: DataContext) = getSelectedPackage()?.isCopyEnabled(dataContext) ?: false
    override fun isCopyVisible(dataContext: DataContext) = getSelectedPackage()?.isCopyVisible(dataContext) ?: false

}
