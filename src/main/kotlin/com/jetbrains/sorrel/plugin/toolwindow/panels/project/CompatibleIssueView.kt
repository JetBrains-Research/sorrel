package com.jetbrains.sorrel.plugin.toolwindow.panels.project

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.sorrel.plugin.SorrelUtilUI
import com.jetbrains.sorrel.plugin.issue.CompatibilityIssueData
import com.jetbrains.sorrel.plugin.updateAndRepaint
import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingUtilities

class CompatibleIssueView {

    fun createPanel(compatibilityIssues: Property<CompatibilityIssueData>, lifetime: Lifetime): JPanel {
        val compatibleIssueTitle: JLabel = JLabel(
            com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.tab.project.compatibilityIssue")
        ).apply {
            font = Font(font.family, Font.BOLD, (font.size * 1.2).toInt())
        }

        val separator = JSeparator()

        val panel = JPanel().apply {
            background = SorrelUtilUI.UsualBackgroundColor

            layout = MigLayout(
                "fillx,flowy,insets 0",
                "[left,grow]",
                "[top][top][top]"
            )
            add(compatibleIssueTitle)
            add(separator, CC().growX())
        }

        //Update issues
        compatibilityIssues.advise(lifetime) {
            SwingUtilities.invokeLater {
                panel.removeAll()
                panel.add(compatibleIssueTitle)
                panel.add(separator, CC().growX())

                if (it.isEmpty()) {
                    panel.add(createEmptyLabel())
                } else {
                    panel.add(createIssueLabel(it.convertCompatibilityIssuesDataToHtml()))
                }

                panel.updateAndRepaint()
            }
        }

        return panel
    }

    private fun createIssueLabel(content: String): JBLabel = JBLabel(content).apply {
        font = UIUtil.getListFont().let { Font(it.family, it.style, (it.size * 1.1).toInt()) }
        this.setCopyable(true)
    }

    private fun createEmptyLabel(): JBLabel = JBLabel(
        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.compatibilityIssues.emptyList")
    ).apply {
        font = UIUtil.getListFont().let { Font(it.family, it.style, (it.size * 1.1).toInt()) }
    }
}