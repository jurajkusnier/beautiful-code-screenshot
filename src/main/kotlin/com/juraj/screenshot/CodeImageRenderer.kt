package com.juraj.screenshot

import com.intellij.openapi.editor.Editor
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

object CodeImageRenderer {

    fun render(editor: Editor): BufferedImage {
        val selectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd

        val startPos = editor.offsetToVisualPosition(startOffset)
        val endPos = editor.offsetToVisualPosition(endOffset)

        val startPoint = editor.visualPositionToXY(startPos)
        val endPoint = editor.visualPositionToXY(endPos)

        val lineHeight = editor.lineHeight
        val contentHeight = (endPoint.y - startPoint.y) + lineHeight

        val horizontalPadding = 40
        val verticalPadding = 32
        val titleBarHeight = 56
        val cornerRadius = 28
        val shadowSize = 24

        val document = editor.document
        val startLine = document.getLineNumber(startOffset)
        val endLine = document.getLineNumber(endOffset)

        var maxLineX = 0
        for (line in startLine..endLine) {
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = document.getLineEndOffset(line)

            var lineWidth = editor.visualPositionToXY(editor.offsetToVisualPosition(lineEnd)).x

            // After-line-end inlays (e.g. "1 usage", VCS blame) are rendered beyond
            // the text end offset and are not reflected in visualPositionToXY.
            editor.inlayModel.getAfterLineEndElementsInRange(lineStart, lineEnd)
                .forEach { lineWidth += it.widthInPixels }

            maxLineX = maxOf(maxLineX, lineWidth)
        }

        val minWindowWidth = 300
        val windowWidth = maxOf(maxLineX + horizontalPadding * 2, minWindowWidth)
        val windowHeight = contentHeight + verticalPadding + titleBarHeight

        val finalWidth = windowWidth + shadowSize * 2
        val finalHeight = windowHeight + shadowSize * 2

        val image = UIUtil.createImage(editor.contentComponent, finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB)

        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val windowX = shadowSize
        val windowY = shadowSize

        // Drop shadow — dynamic alpha requires java.awt.Color directly (no JBColor alpha constructor)
        for (i in shadowSize downTo 1) {
            val alpha = (15 * (1f - i.toFloat() / shadowSize)).toInt().coerceAtLeast(2)
            g.color = JBColor(Color(0, 0, 0, alpha), Color(0, 0, 0, alpha))
            g.fillRoundRect(
                windowX - i / 2, windowY - i / 2,
                windowWidth + i, windowHeight + i,
                cornerRadius + i, cornerRadius + i
            )
        }

        val windowShape = RoundRectangle2D.Float(
            windowX.toFloat(), windowY.toFloat(),
            windowWidth.toFloat(), windowHeight.toFloat(),
            cornerRadius.toFloat(), cornerRadius.toFloat()
        )

        g.color = JBColor(0x1e1e1e, 0x1e1e1e)
        g.fill(windowShape)
        g.clip = windowShape

        // Remove selection highlight and caret row background before painting
        selectionModel.removeSelection()
        val settings = editor.settings
        val caretRowWasShown = settings.isCaretRowShown
        settings.isCaretRowShown = false

        val codeX = windowX + horizontalPadding
        val codeY = windowY + titleBarHeight - startPoint.y
        g.translate(codeX, codeY)
        editor.contentComponent.paint(g)
        g.translate(-codeX, -codeY)

        settings.isCaretRowShown = caretRowWasShown

        // Title bar (covers top rounded corners of code area)
        g.color = editor.colorsScheme.defaultBackground
        g.fillRoundRect(windowX, windowY, windowWidth, titleBarHeight, cornerRadius, cornerRadius)

        // Bottom bar (covers bottom rounded corners of code area)
        g.color = editor.colorsScheme.defaultBackground
        g.fillRoundRect(
            windowX, windowY + titleBarHeight + contentHeight,
            windowWidth, verticalPadding, cornerRadius, cornerRadius
        )

        // macOS traffic light buttons
        val circleDiameter = 14
        val circleY = windowY + (titleBarHeight / 2) - (circleDiameter / 2)
        val closeX = windowX + 20

        g.color = JBColor(0xFF5F56, 0xFF5F56)
        g.fillOval(closeX, circleY, circleDiameter, circleDiameter)

        g.color = JBColor(0xFFBD2E, 0xFFBD2E)
        g.fillOval(closeX + 22, circleY, circleDiameter, circleDiameter)

        g.color = JBColor(0x27C93F, 0x27C93F)
        g.fillOval(closeX + 44, circleY, circleDiameter, circleDiameter)

        // Restore selection (not visible in the rendered image)
        selectionModel.setSelection(startOffset, endOffset)

        g.dispose()

        return image
    }
}
