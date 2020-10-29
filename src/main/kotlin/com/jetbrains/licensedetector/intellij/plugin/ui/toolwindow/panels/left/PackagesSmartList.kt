package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left

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
import javax.swing.DefaultListModel

class PackagesSmartList(val viewModel: LicenseDetectorToolWindowModel) :
        JBList<PackagesSmartItem>(emptyList()), DataProvider, CopyProvider {

    private val updateContentLock = Object()

    private val listModel: DefaultListModel<PackagesSmartItem>
        get() = model as DefaultListModel<PackagesSmartItem>

    val installedHeader = PackagesSmartItem.Header(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.installedPackages"))

    private val packageItems: List<PackagesSmartItem.Package> get() = listModel.elements().toList().filterIsInstance<PackagesSmartItem.Package>()
    val hasPackageItems: Boolean get() = packageItems.any()
    val firstPackageIndex: Int get() = listModel.elements().toList().indexOfFirst { it is PackagesSmartItem.Package }

    init {
        @Suppress("UnstableApiUsage") // yolo
        putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)

        cellRenderer = PackagesSmartRenderer()

        listModel.addElement(installedHeader)

        RiderUI.addKeyboardPopupHandler(this, "alt ENTER") { items ->
            val item = items.first()
            if (item is PackagesSmartItem.Package) PackagesSmartItemPopup(item.meta) else null
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
        installedHeader.title = message

        displayItems.add(installedHeader)
        displayItems.addAll(installedPackages.map { PackagesSmartItem.Package(it) })
        return displayItems
    }

    fun updateAllPackages(packages: List<LicenseDetectorDependency>) {
        synchronized(updateContentLock) {
            val displayItems = calcDisplayItems(packages)

            //TODO:Implement reselect item

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

}
