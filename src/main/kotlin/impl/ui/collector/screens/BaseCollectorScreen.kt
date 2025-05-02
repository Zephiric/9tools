package impl.ui.collector.screens

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.UIComponent
import impl.ui.collector.components.TooltipHandler
import impl.ui.collector.SettingsManager
import impl.ui.collector.utils.UIScaling
import java.awt.Color

/**
 * The mother of all screens in the Collector UI domain of 9hack. Acts as a manager in many ways for every single screen/view to reduce duplicated code. Works really close with UIComponent interface
 */

abstract class BaseCollectorScreen(title: Text) : Screen(title) {

    protected val tooltipHandler = TooltipHandler
    protected val components = mutableListOf<UIComponent>()
    protected var isWindowDragging = false
    protected var dragOffsetX = 0.0
    protected var dragOffsetY = 0.0

    protected abstract fun getScreenX(): Double
    protected abstract fun getScreenY(): Double
    protected abstract fun getScreenWidth(): Double
    protected abstract fun getScreenHeight(): Double
    protected abstract fun updateScreenPosition(newX: Double, newY: Double)
    protected abstract fun getScreenIdentifier(): String

    protected fun loadSavedPosition() {
        val screenId = getScreenIdentifier()
        val position = SettingsManager.getScreenPosition(screenId)
        if (position != null) {
            updateScreenPosition(position.x, position.y)
        } else {
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {

        if (button == 0) {
            val scale = UIScaling.getScale()
            val scaledMouseX = mouseX / scale
            val scaledMouseY = mouseY / scale

            val screenX = getScreenX()
            val screenY = getScreenY()
            val screenWidth = getScreenWidth()
            if (scaledMouseY >= screenY && scaledMouseY <= screenY + 20 &&
                scaledMouseX >= screenX && scaledMouseX <= screenX + screenWidth
            ) {
                isWindowDragging = true
                dragOffsetX = scaledMouseX - screenX
                dragOffsetY = scaledMouseY - screenY
                return true
            }

            components
                .sortedByDescending { it.priority }
                .forEach { component ->
                    if (component.onClick(scaledMouseX, scaledMouseY, button)) {
                        return true
                    }
                }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean {
        val scale = UIScaling.getScale()
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale
        val scaledDeltaX = deltaX / scale
        val scaledDeltaY = deltaY / scale

        if (isWindowDragging && button == 0) {
            val newX = scaledMouseX - dragOffsetX
            val newY = scaledMouseY - dragOffsetY

            val screenWidth = mc.window.scaledWidth.toDouble() / scale
            val screenHeight = mc.window.scaledHeight.toDouble() / scale
            val windowWidth = getScreenWidth()

            val clampedX = newX.coerceIn(-windowWidth + 50.0, screenWidth - 50.0)
            val clampedY = newY.coerceIn(0.0, screenHeight - 50.0)
            updateScreenPosition(clampedX, clampedY)

            SettingsManager.saveScreenPosition(getScreenIdentifier(), clampedX, clampedY)

            return true
        }
        components
            .sortedByDescending { it.priority }
            .forEach { component ->
                if (component.onDrag(scaledMouseX, scaledMouseY, scaledDeltaX, scaledDeltaY, button)) {
                    return true
                }
            }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {

        if (isWindowDragging && button == 0) {
            isWindowDragging = false
            return true
        }
        val scale = UIScaling.getScale()
        val scaledX = mouseX / scale
        val scaledY = mouseY / scale

        components
            .sortedByDescending { it.priority }
            .forEach { comp ->
                if (comp.onMouseRelease(scaledX, scaledY, button)) {
                    return true
                }
            }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val scale = UIScaling.getScale()
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        components
            .sortedByDescending { it.priority }
            .forEach { component ->
                if (component.onScroll(scaledMouseX, scaledMouseY, verticalAmount)) {
                    return true
                }
            }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        components
            .sortedByDescending { it.priority }
            .forEach { comp ->
                if (comp.onKeyRelease(keyCode, scanCode, modifiers)) {
                    return true
                }
            }
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        components
            .sortedByDescending { it.priority }
            .forEach { comp ->
                if (comp.onCharTyped(chr, modifiers)) {
                    return true
                }
            }
        return super.charTyped(chr, modifiers)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {

        components
            .sortedByDescending { it.priority }
            .forEach { component ->
                if (component.onKeyPress(keyCode, scanCode, modifiers)) {
                    return true
                }
            }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        context.matrices.push()
        context.matrices.translate(0f, 0f, 10f)

        val width = mc.window.scaledWidth.toDouble()
        val height = mc.window.scaledHeight.toDouble()

        win.ninegang.ninetools.compat.util.render.Engine2d.renderQuad(
            context.matrices,
            Color(0.0f, 0.0f, 0.0f, 0.4f),
            0.0,
            0.0,
            width,
            height
        )
        UIScaling.updateScale()
        val scale = UIScaling.getScale()
        val scaledMouseX = (mouseX / scale).toInt()
        val scaledMouseY = (mouseY / scale).toInt()

        UIScaling.scale(context)
        renderScaled(context, scaledMouseX, scaledMouseY, delta)
        UIScaling.unscale(context)
        context.matrices.pop()
    }

    protected open fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        renderBackground(context, getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight())


        renderTitleBar(context, getScreenX(), getScreenY(), getScreenWidth(), "Collector Screen")


        components.sortedBy { it.priority }.forEach { component ->
            component.render(context, mouseX, mouseY, delta)
        }
    }

    protected fun renderBackground(context: DrawContext, x: Double, y: Double, width: Double, height: Double) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.1f, 0.1f, 0.1f, 0.95f),
            x, y, x + width, y + height,
            3f, 10f
        )
    }

    protected fun renderTitleBar(context: DrawContext, x: Double, y: Double, width: Double, title: String) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.15f, 0.15f, 0.15f, 0.95f),
            x, y, x + width, y + 20,
            3f, 10f
        )
        context.drawTextWithShadow(
            mc.textRenderer,
            title,
            (x + 5).toInt(),
            (y + 6).toInt(),
            Color(1f, 1f, 1f, 0.9f).rgb
        )
    }

    protected fun renderTooltipIfNeeded(context: DrawContext, item: CollectorItem?, mouseX: Int, mouseY: Int) {
        item?.let {
            val tooltipLines = tooltipHandler.generateTooltip(
                CollectorItem.toItemStack(it),
                collectorItem = it
            )
            tooltipHandler.renderTooltip(context, tooltipLines, mouseX, mouseY)
        }
    }

    override fun close() {

        if (mc.world == null) {
            mc.setScreen(null)
        } else {
            super.close()
        }

    }
}