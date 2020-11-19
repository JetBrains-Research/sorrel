package com.jetbrains.licensedetector.intellij.plugin.ui.inlays

import com.intellij.codeInsight.hints.presentation.AttributesTransformerPresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.RoundWithBackgroundPresentation
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes

/**
 * Modified functions from PresentationFactory
 * roundWithBackground() only works well with the smallText() method,
 * but when working with text() it looks bad because of the top indent
 */

fun roundWithBackground(base: InlayPresentation, editor: Editor): InlayPresentation {
    val rounding = withInlayAttributes(RoundWithBackgroundPresentation(
            InsetPresentation(
                    base,
                    left = 7,
                    right = 7,
                    top = 0,
                    down = 0

            ),
            8,
            8
    ), editor)
    return InsetPresentation(rounding)
}

private fun withInlayAttributes(base: InlayPresentation, editor: Editor): InlayPresentation {
    return AttributesTransformerPresentation(base) {
        it.withDefault(attributesOf(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT, editor))
    }
}

private fun attributesOf(key: TextAttributesKey, editor: Editor) = editor.colorsScheme.getAttributes(key)
        ?: TextAttributes()

private fun TextAttributes.withDefault(other: TextAttributes): TextAttributes {
    val result = this.clone()
    if (result.foregroundColor == null) {
        result.foregroundColor = other.foregroundColor
    }
    if (result.backgroundColor == null) {
        result.backgroundColor = other.backgroundColor
    }
    if (result.effectType == null) {
        result.effectType = other.effectType
    }
    if (result.effectColor == null) {
        result.effectColor = other.effectColor
    }
    return result
}