package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.CompatibilityIssueData
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
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
            LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.compatibilityIssue")
        ).apply {
            font = Font(font.family, Font.BOLD, (font.size * 1.2).toInt())
        }

        val separator = JSeparator()

        val panel = JPanel().apply {
            background = RiderUI.UsualBackgroundColor

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

                if (it.packageDependencyLicenseIssues.isEmpty() &&
                    it.submoduleLicenseIssues.isEmpty()
                ) {
                    panel.add(createEmptyLabel())
                } else {
                    val stringBuilder = StringBuilder("<html><body><ol>")
                    if (it.packageDependencyLicenseIssues.isNotEmpty()) {
                        it.packageDependencyLicenseIssues.forEach { issue ->
                            stringBuilder.append("<li>$issue</li>")
                        }
                    }
                    if (it.submoduleLicenseIssues.isNotEmpty()) {
                        it.submoduleLicenseIssues.forEach { issue ->
                            stringBuilder.append("<li>$issue</li>")
                        }
                    }
                    stringBuilder.append("</ol></body></html>")
                    panel.add(createIssueLabel(stringBuilder.toString()))
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
            LicenseDetectorBundle.message("licensedetector.ui.compatibilityIssues.emptyList")
    ).apply {
        font = UIUtil.getListFont().let { Font(it.family, it.style, (it.size * 1.1).toInt()) }
    }
}