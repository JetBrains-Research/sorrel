package com.jetbrains.sorrel.plugin.toolwindow.panels.packages

import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.jetbrains.sorrel.plugin.SorrelUtilUI
import java.awt.Dimension
import java.awt.event.KeyEvent

class PackagesSmartSearchField : SearchTextField(false) {

    init {
        SorrelUtilUI.setHeight(this, height = 25)

        @Suppress("MagicNumber") // Gotta love Swing APIs
        minimumSize = Dimension(JBUI.scale(100), minimumSize.height)

        font = SorrelUtilUI.BigFont
        textEditor.setTextToTriggerEmptyTextStatus(com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.packages.searchfield.hint"))
        textEditor.emptyText.isShowAboveCenter = true

        SorrelUtilUI.overrideKeyStroke(textEditor, "shift ENTER", this::transferFocusBackward)
    }

    /**
     * Trying to navigate to the first element in the brief list
     * @return true in case of success; false if the list is empty
     */
    var goToList: () -> Boolean = { false }

    override fun preprocessEventForTextField(e: KeyEvent?): Boolean {
        // run our own logic
        if (e?.keyCode == KeyEvent.VK_DOWN || e?.keyCode == KeyEvent.VK_PAGE_DOWN) {
            goToList() // trying to navigate to the list instead of "show history"
            e.consume() // suppress default "show history" logic anyway
            return true
        }
        return super.preprocessEventForTextField(e)
    }

    override fun getBackground() = SorrelUtilUI.HeaderBackgroundColor
}
