package com.juraj.screenshot

import com.intellij.ui.JBColor
import java.awt.Color

sealed class Background {
    object Transparent : Background()
    data class Solid(val color: Color) : Background()
    data class Gradient(val from: Color, val to: Color) : Background()

    companion object {
        val SOLID_PRESETS: List<Solid> = listOf(
            Solid(JBColor(0x000000, 0x000000)),   // Black
            Solid(JBColor(0xffffff, 0xffffff)),   // White
            Solid(JBColor(0x1a237e, 0x1a237e)),   // Deep indigo
            Solid(JBColor(0x4a148c, 0x4a148c)),   // Deep purple
            Solid(JBColor(0x880e4f, 0x880e4f)),   // Deep pink
            Solid(JBColor(0xbf360c, 0xbf360c)),   // Deep orange
            Solid(JBColor(0x1b5e20, 0x1b5e20)),   // Forest green
            Solid(JBColor(0x004d40, 0x004d40)),   // Dark teal
        )

        val GRADIENT_PRESETS: List<Gradient> = listOf(
            Gradient(JBColor(0x667eea, 0x667eea), JBColor(0x764ba2, 0x764ba2)),   // Violet
            Gradient(JBColor(0xf093fb, 0xf093fb), JBColor(0xf5576c, 0xf5576c)),   // Pink flame
            Gradient(JBColor(0x4facfe, 0x4facfe), JBColor(0x00f2fe, 0x00f2fe)),   // Ice blue
            Gradient(JBColor(0x43e97b, 0x43e97b), JBColor(0x38f9d7, 0x38f9d7)),   // Emerald
            Gradient(JBColor(0xfa709a, 0xfa709a), JBColor(0xfee140, 0xfee140)),   // Golden sunset
            Gradient(JBColor(0x30cfd0, 0x30cfd0), JBColor(0x330867, 0x330867)),   // Teal deep
            Gradient(JBColor(0xa18cd1, 0xa18cd1), JBColor(0xfbc2eb, 0xfbc2eb)),   // Lavender blush
            Gradient(JBColor(0xf77062, 0xf77062), JBColor(0xfe5196, 0xfe5196)),   // Coral flame
        )
    }
}
