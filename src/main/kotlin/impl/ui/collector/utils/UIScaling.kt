package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import impl.ui.collector.SettingsManager

object UIScaling {
    private var lastScale = 1.0
    private var scaledWidth = 0.0
    private var scaledHeight = 0.0

    fun updateScale() {
        val newScale = SettingsManager.uiScale
        if (newScale != lastScale) {
            lastScale = newScale
            recalculateScaledDimensions()
        }
    }

    fun scale(context: DrawContext) {
        context.matrices.push()
        context.matrices.scale(lastScale.toFloat(), lastScale.toFloat(), 1f)
    }

    fun unscale(context: DrawContext) {
        context.matrices.pop()
    }

    fun getScale() = lastScale

    private fun recalculateScaledDimensions() {
        val mc = win.ninegang.ninetools.compat.util.Wrapper.mc
        scaledWidth = mc.window.scaledWidth / lastScale
        scaledHeight = mc.window.scaledHeight / lastScale
    }
}