package com.juraj.screenshot

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.JBColor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

class PreviewDialog(
    private val project: Project?,
    private val windowImage: BufferedImage
) : DialogWrapper(project) {

    private var currentBackground: Background = loadBackground()
    private val swatches = mutableListOf<Swatch>()
    private lateinit var imageLabel: JLabel

    init {
        title = "Beautiful Code Screenshot"
        cancelAction.putValue(Action.NAME, "Close")
        init()
    }

    override fun createNorthPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 8))

        panel.add(swatch(Background.Transparent))
        for (bg in Background.SOLID_PRESETS) panel.add(swatch(bg))

        panel.add(separator())

        for (bg in Background.GRADIENT_PRESETS) panel.add(swatch(bg))

        return panel
    }

    private fun separator(): JComponent = object : JComponent() {
        init { preferredSize = Dimension(9, SWATCH_SIZE) }
        override fun paintComponent(g: Graphics) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = SEPARATOR
            g2.drawLine(4, 2, 4, height - 2)
        }
    }

    private fun swatch(bg: Background): Swatch {
        return Swatch(bg).also {
            it.isSelected = (bg == currentBackground)
            swatches.add(it)
        }
    }

    override fun createCenterPanel(): JComponent {
        val displayImage = scaleForDisplay(compositeForDisplay(currentBackground))
        imageLabel = JLabel(ImageIcon(displayImage))
        imageLabel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return JBScrollPane(imageLabel).apply {
            preferredSize = Dimension(
                minOf(displayImage.width + 40, (screenSize.width * 0.8).toInt()),
                minOf(displayImage.height + 40, (screenSize.height * 0.7).toInt())
            )
        }
    }

    override fun createActions(): Array<Action> = arrayOf(
        object : AbstractAction("Copy to Clipboard") {
            override fun actionPerformed(e: ActionEvent) {
                copyToClipboard()
                close(OK_EXIT_CODE)
            }
        },
        object : AbstractAction("Save as PNG") {
            override fun actionPerformed(e: ActionEvent) {
                if (saveToFile()) close(OK_EXIT_CODE)
            }
        },
        cancelAction
    )

    private fun selectBackground(background: Background) {
        currentBackground = background
        saveBackground(background)
        swatches.forEach { it.isSelected = (it.bg == background) }
        imageLabel.icon = ImageIcon(scaleForDisplay(compositeForDisplay(background)))
        imageLabel.revalidate()
        imageLabel.repaint()
    }

    /** Composites a background behind the window image for on-screen display.
     *  Transparent uses a checkerboard so the user can see the transparency. */
    private fun compositeForDisplay(background: Background): BufferedImage {
        if (background is Background.Transparent) return withCheckerboard(windowImage)
        return composite(background)
    }

    /** Composites a background for export. Transparent returns the window image as-is. */
    private fun composite(background: Background): BufferedImage {
        if (background is Background.Transparent) return windowImage
        val result = BufferedImage(windowImage.width, windowImage.height, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        try {
            when (background) {
                is Background.Solid -> {
                    g.color = background.color
                    g.fillRect(0, 0, result.width, result.height)
                }
                is Background.Gradient -> {
                    g.paint = GradientPaint(
                        0f, 0f, background.from,
                        result.width.toFloat(), result.height.toFloat(), background.to
                    )
                    g.fillRect(0, 0, result.width, result.height)
                }
                else -> {}
            }
            g.drawImage(windowImage, 0, 0, null)
        } finally {
            g.dispose()
        }
        return result
    }

    private fun withCheckerboard(img: BufferedImage): BufferedImage {
        val result = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        val cell = 16
        for (row in 0..(img.height / cell)) {
            for (col in 0..(img.width / cell)) {
                g.color = if ((row + col) % 2 == 0) CHECKER_A else CHECKER_B
                g.fillRect(col * cell, row * cell, cell, cell)
            }
        }
        g.drawImage(img, 0, 0, null)
        g.dispose()
        return result
    }

    private fun scaleForDisplay(img: BufferedImage): BufferedImage {
        val screen = Toolkit.getDefaultToolkit().screenSize
        val maxW = (screen.width * 0.8).toInt()
        val maxH = (screen.height * 0.7).toInt()
        val scale = minOf(maxW.toDouble() / img.width, maxH.toDouble() / img.height, 1.0)
        if (scale >= 1.0) return img
        val w = (img.width * scale).toInt()
        val h = (img.height * scale).toInt()
        val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(img, 0, 0, w, h, null)
        g.dispose()
        return scaled
    }

    private fun copyToClipboard() {
        val image = composite(currentBackground)
        val transferable = object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any {
                if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
                return image
            }
        }
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    private fun saveToFile(): Boolean {
        val descriptor = FileSaverDescriptor("Save Code Image", "Save code image as PNG", "png")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = dialog.save(null as VirtualFile?, "code-screenshot") ?: return false
        val file = result.file
        file.parentFile?.mkdirs()
        ImageIO.write(composite(currentBackground), "png", file)
        return true
    }

    private inner class Swatch(val bg: Background) : JPanel() {

        var isSelected: Boolean = false
            set(value) { field = value; repaint() }

        init {
            preferredSize = Dimension(SWATCH_SIZE, SWATCH_SIZE)
            isOpaque = false
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = selectBackground(bg)
            })
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val arc = 6f
                val w = width.toFloat()
                val h = height.toFloat()
                val componentClip = g2.clip

                // Fill background content, clipped to rounded shape
                g2.clip(RoundRectangle2D.Float(0f, 0f, w, h, arc, arc))
                when (bg) {
                    is Background.Transparent -> {
                        val cell = 5
                        for (row in 0..(height / cell)) {
                            for (col in 0..(width / cell)) {
                                g2.color = if ((row + col) % 2 == 0) SWATCH_CHECKER_A else SWATCH_CHECKER_B
                                g2.fillRect(col * cell, row * cell, cell, cell)
                            }
                        }
                    }
                    is Background.Solid -> {
                        g2.color = bg.color
                        g2.fillRect(0, 0, width, height)
                    }
                    is Background.Gradient -> {
                        g2.paint = GradientPaint(0f, 0f, bg.from, w, h, bg.to)
                        g2.fillRect(0, 0, width, height)
                    }
                }

                // Restore clip for borders
                g2.clip = componentClip

                if (isSelected) {
                    // Dark halo — keeps the white ring visible on light swatches
                    g2.color = SELECTION_HALO
                    g2.stroke = BasicStroke(4f)
                    g2.draw(RoundRectangle2D.Float(2f, 2f, w - 4f, h - 4f, arc, arc))
                    // White selection ring
                    g2.color = JBColor.WHITE
                    g2.stroke = BasicStroke(2f)
                    g2.draw(RoundRectangle2D.Float(2f, 2f, w - 4f, h - 4f, arc, arc))
                }

                // Outer border — visible on both light and dark backgrounds
                g2.color = SWATCH_BORDER
                g2.stroke = BasicStroke(1f)
                g2.draw(RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc))
                // Inner highlight — visible on dark swatches
                g2.color = SWATCH_HIGHLIGHT
                g2.draw(RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, arc - 1f, arc - 1f))
            } finally {
                g2.dispose()
            }
        }
    }

    companion object {
        private const val SWATCH_SIZE = 32

        // Separator line
        private val SEPARATOR = JBColor(Color(128, 128, 128, 120), Color(180, 180, 180, 120))

        // Preview checkerboard (transparent background display)
        private val CHECKER_A = JBColor(Color(200, 200, 200,255), Color(55, 55, 55,255))
        private val CHECKER_B = JBColor(Color(240, 240, 240,255), Color(75, 75, 75,255))

        // Swatch checkerboard (transparent swatch)
        private val SWATCH_CHECKER_A = JBColor(Color(180, 180, 180,255), Color(65, 65, 65,255))
        private val SWATCH_CHECKER_B = JBColor(Color(240, 240, 240,255), Color(85, 85, 85,255))

        // Swatch borders and selection
        private val SWATCH_BORDER    = JBColor(Color(0, 0, 0, 100),   Color(255, 255, 255, 60))
        private val SWATCH_HIGHLIGHT = JBColor(Color(255, 255, 255, 70), Color(0, 0, 0, 40))
        private val SELECTION_HALO   = JBColor(Color(0, 0, 0, 160),   Color(0, 0, 0, 160))

        private const val KEY_TYPE  = "beautiful.code.screenshot.bg.type"
        private const val KEY_INDEX = "beautiful.code.screenshot.bg.index"

        private fun loadBackground(): Background {
            val props = PropertiesComponent.getInstance()
            val index = props.getInt(KEY_INDEX, 0)
            return when (props.getValue(KEY_TYPE, "gradient")) {
                "transparent" -> Background.Transparent
                "solid"       -> Background.SOLID_PRESETS.getOrElse(index) { Background.SOLID_PRESETS[0] }
                else          -> Background.GRADIENT_PRESETS.getOrElse(index) { Background.GRADIENT_PRESETS[0] }
            }
        }

        private fun saveBackground(background: Background) {
            val props = PropertiesComponent.getInstance()
            when (background) {
                is Background.Transparent -> props.setValue(KEY_TYPE, "transparent")
                is Background.Solid -> {
                    props.setValue(KEY_TYPE, "solid")
                    props.setValue(KEY_INDEX, Background.SOLID_PRESETS.indexOf(background), 0)
                }
                is Background.Gradient -> {
                    props.setValue(KEY_TYPE, "gradient")
                    props.setValue(KEY_INDEX, Background.GRADIENT_PRESETS.indexOf(background), 0)
                }
            }
        }
    }
}
