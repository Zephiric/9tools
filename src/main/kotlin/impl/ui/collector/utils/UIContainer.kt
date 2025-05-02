package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import org.lwjgl.opengl.GL11
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class UIContainer(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double,
    var renderBackground: Boolean = true,
    var backgroundColor: Color = Color(0.15f, 0.15f, 0.15f, 0.5f),
    var enableScissoring: Boolean = true,
    var enableScrolling: Boolean = false,
    var scrollbarWidth: Double = 6.0,
    var scrollbarColor: Color = Color(0.4f, 0.6f, 1f, 0.8f),
    var scrollbarHoverColor: Color = Color(0.5f, 0.7f, 1f, 0.9f),
    var scaleOrigin: ScaleOrigin = ScaleOrigin.FIRST_COMPONENT,

) : UIComponent {

    enum class ScaleOrigin {
        TOP_LEFT, CENTER, FIRST_COMPONENT
    }

    private val children = mutableListOf<UIComponent>()
    private var activeComponent: UIComponent? = null
    private var scrollY = 0.0
    private var maxScrollY = 0.0
    private var contentHeight = 0.0
    private var isScrollbarDragging = false
    private var scrollStartY = 0.0
    private var scrollStartMouseY = 0.0
    private var isScrollbarHovered = false

    private var _scale: Double = 1.0
    val scale: Double get() = _scale

    fun setScale(newScale: Double) {
        if (_scale != newScale) {
            val scrollPercentage = if (maxScrollY > 0) scrollY / maxScrollY else 0.0

            _scale = newScale
            updateContentHeight()

            if (maxScrollY > 0) {
                scrollY = scrollPercentage * maxScrollY
            }
        }
    }
    private val tooltips = mutableListOf<Pair<List<net.minecraft.text.Text>, Pair<Int, Int>>>()

    fun addComponent(component: UIComponent) {
        children.add(component)
        updateContentHeight()
    }

    fun removeComponent(component: UIComponent) {
        children.remove(component)
        if (activeComponent == component) {
            activeComponent = null
        }
        updateContentHeight()
    }

    fun clearComponents() {
        children.clear()
        activeComponent = null
        scrollY = 0.0
        updateContentHeight()
    }

    private fun updateContentHeight() {
        if (children.isEmpty()) {
            contentHeight = 0.0
            maxScrollY = 0.0
            return
        }
        contentHeight = children.maxOf { child ->
            val relativeY = child.y - y
            val scaledHeight = child.height * scale

            relativeY * scale + scaledHeight
        }
        contentHeight += 20.0

        maxScrollY = max(0.0, contentHeight - height)

        if (scrollY > maxScrollY) {
            scrollY = maxScrollY
        }
    }

    override fun getChildComponents(): List<UIComponent> = children.toList()

    fun getScrollY(): Double = scrollY

    fun setScrollY(newScrollY: Double) {
        scrollY = newScrollY.coerceIn(0.0, maxScrollY)
    }

    fun getContentHeight(): Double = contentHeight

    fun getMaxScrollY(): Double = maxScrollY

    fun setCustomContentHeight(height: Double) {
        contentHeight = height
        maxScrollY = maxOf(0.0, contentHeight - this.height)

        if (scrollY > maxScrollY) {
            scrollY = maxScrollY
        }
    }

    private fun getScaleOrigin(): Pair<Double, Double> {
        return when (scaleOrigin) {
            ScaleOrigin.TOP_LEFT -> Pair(x, y)
            ScaleOrigin.CENTER -> Pair(x + width / 2, y + height / 2)
            ScaleOrigin.FIRST_COMPONENT -> {
                if (children.isNotEmpty()) {
                    val firstChild = children.first()
                    Pair(firstChild.x, firstChild.y)
                } else {
                    Pair(x, y)
                }
            }
        }
    }

    private fun transformPoint(px: Double, py: Double): Pair<Double, Double> {
        val (originX, originY) = getScaleOrigin()

        val transformedX = if (scale != 1.0) (px - originX) / scale + originX else px
        val transformedY = if (scale != 1.0) (py - originY) / scale + originY else py
        val scrolledY = if (enableScrolling) transformedY + scrollY else transformedY

        return Pair(transformedX, scrolledY)
    }

    fun renderContainerOnly(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateScrollbarHover(mouseX, mouseY)

        if (renderBackground) {
            win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                context.matrices,
                backgroundColor,
                x, y,
                x + width, y + height,
                4f, 10f
            )
        }

        if (enableScrolling && maxScrollY > 0) {
            renderScrollbar(context)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        tooltips.clear()

        if (renderBackground) {
            win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                context.matrices,
                backgroundColor,
                x, y,
                x + width, y + height,
                4f, 10f
            )
        }

        if (enableScissoring) {
            val uiScale = UIScaling.getScale()
            val windowScale = mc.window.scaleFactor
            val totalScale = uiScale * windowScale

            val scissorX = (x * totalScale).toInt()
            val scissorY = mc.window.height - ((y + height) * totalScale).toInt()
            val scissorWidth = (width * totalScale).toInt()
            val scissorHeight = (height * totalScale).toInt()

            val prevScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            val prevScissorBox = IntArray(4)
            if (prevScissorEnabled) {
                GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, prevScissorBox)
            }

            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight)

            renderChildren(context, mouseX, mouseY, delta)

            if (!prevScissorEnabled) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            } else {
                GL11.glScissor(
                    prevScissorBox[0],
                    prevScissorBox[1],
                    prevScissorBox[2],
                    prevScissorBox[3]
                )
            }
        } else {
            renderChildren(context, mouseX, mouseY, delta)
        }

        if (enableScrolling && maxScrollY > 0) {
            renderScrollbar(context)
        }

        tooltips.forEach { (text, pos) ->
            context.drawTooltip(
                mc.textRenderer,
                text,
                pos.first,
                pos.second
            )
        }
    }

    private fun renderChildren(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val sortedChildren = children.sortedBy { it.priority }
        if (sortedChildren.isEmpty()) return

        val (originX, originY) = getScaleOrigin()

        context.matrices.push()

        if (scale != 1.0) {
            context.matrices.translate(originX, originY, 0.0)
            context.matrices.scale(scale.toFloat(), scale.toFloat(), 1f)
            context.matrices.translate(-originX, -originY, 0.0)
        }

        if (enableScrolling && scrollY > 0) {
            context.matrices.translate(0.0, -scrollY, 0.0)
        }

        val (transformedMouseX, transformedMouseY) = transformPoint(mouseX.toDouble(), mouseY.toDouble())

        for (child in sortedChildren) {

            val oldTooltipRenderer = if (child is TooltipOwner) {
                val originalRenderer = child.tooltipRenderer
                child.tooltipRenderer = { text, x, y ->
                    tooltips.add(Pair(text, Pair(x, y)))
                }
                originalRenderer
            } else null

            child.render(context, transformedMouseX.toInt(), transformedMouseY.toInt(), delta)

            if (child is TooltipOwner && oldTooltipRenderer != null) {
                child.tooltipRenderer = oldTooltipRenderer
            }
        }

        context.matrices.pop()
    }

    interface TooltipOwner {
        var tooltipRenderer: (List<net.minecraft.text.Text>, Int, Int) -> Unit
    }

    private fun renderScrollbar(context: DrawContext) {
        updateScrollbarHover(mouseX = -1, mouseY = -1)

        val scrollbarX = x + width - scrollbarWidth - 2
        val viewRatio = min(1.0, height / contentHeight)
        val scrollbarHeight = max(20.0, height * viewRatio)

        val scrollRatio = if (maxScrollY > 0) scrollY / maxScrollY else 0.0
        val scrollbarY = y + (height - scrollbarHeight) * scrollRatio

        val color = if (isScrollbarHovered || isScrollbarDragging) scrollbarHoverColor else scrollbarColor

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            color,
            scrollbarX, scrollbarY,
            scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight,
            (scrollbarWidth / 2).toFloat(), 10f
        )
    }

    private fun updateScrollbarHover(mouseX: Int, mouseY: Int) {
        if (!enableScrolling || maxScrollY <= 0 || mouseX < 0 || mouseY < 0) {
            isScrollbarHovered = false
            return
        }

        val scrollbarX = x + width - scrollbarWidth - 2
        val viewRatio = min(1.0, height / contentHeight)
        val scrollbarHeight = max(20.0, height * viewRatio)
        val scrollRatio = if (maxScrollY > 0) scrollY / maxScrollY else 0.0
        val scrollbarY = y + (height - scrollbarHeight) * scrollRatio

        isScrollbarHovered = mouseX >= scrollbarX &&
                mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY &&
                mouseY <= scrollbarY + scrollbarHeight
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (enableScrolling && maxScrollY > 0 && button == 0) {
            val scrollbarX = x + width - scrollbarWidth - 2
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth) {
                val viewRatio = min(1.0, height / contentHeight)
                val scrollbarHeight = max(20.0, height * viewRatio)
                val scrollRatio = if (maxScrollY > 0) scrollY / maxScrollY else 0.0
                val scrollbarY = y + (height - scrollbarHeight) * scrollRatio

                if (mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                    isScrollbarDragging = true
                    scrollStartY = scrollY
                    scrollStartMouseY = mouseY
                    return true
                } else if (mouseY >= y && mouseY <= y + height) {
                    val clickRatio = (mouseY - y) / height
                    scrollY = maxScrollY * clickRatio
                    return true
                }
            }
        }

        if (activeComponent != null) {
            val (transformedX, transformedY) = transformPoint(mouseX, mouseY)
            if (activeComponent!!.onClick(transformedX, transformedY, button)) {
                return true
            }
            activeComponent = null
        }

        val inContainer = contains(mouseX, mouseY)
        if (!inContainer) return false

        val (transformedX, transformedY) = transformPoint(mouseX, mouseY)

        val sortedChildren = children.sortedByDescending { it.priority }

        for (child in sortedChildren) {
            if (child.contains(transformedX, transformedY)) {
                if (child.onClick(transformedX, transformedY, button)) {
                    activeComponent = child
                    return true
                }
            }
        }

        return true
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean {
        if (isScrollbarDragging && button == 0) {
            val dragDelta = mouseY - scrollStartMouseY
            val viewRatio = min(1.0, height / contentHeight)
            val scrollAmount = dragDelta / viewRatio * height / contentHeight

            scrollY = (scrollStartY + scrollAmount).coerceIn(0.0, maxScrollY)
            return true
        }

        val (transformedX, transformedY) = transformPoint(mouseX, mouseY)
        val scaledDeltaX = if (scale != 1.0) deltaX / scale else deltaX
        val scaledDeltaY = if (scale != 1.0) deltaY / scale else deltaY

        if (activeComponent != null) {
            if (activeComponent!!.onDrag(transformedX, transformedY, scaledDeltaX, scaledDeltaY, button)) {
                return true
            }
        }

        val sortedChildren = children.sortedByDescending { it.priority }

        for (child in sortedChildren) {
            if (child.onDrag(transformedX, transformedY, scaledDeltaX, scaledDeltaY, button)) {
                return true
            }
        }

        return false
    }

    override fun onMouseRelease(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isScrollbarDragging && button == 0) {
            isScrollbarDragging = false
            return true
        }

        val (transformedX, transformedY) = transformPoint(mouseX, mouseY)

        if (activeComponent != null) {
            if (activeComponent!!.onMouseRelease(transformedX, transformedY, button)) {
                return true
            }
        }

        val sortedChildren = children.sortedByDescending { it.priority }

        for (child in sortedChildren) {
            if (child.onMouseRelease(transformedX, transformedY, button)) {
                return true
            }
        }

        return false
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (enableScrolling && contains(mouseX, mouseY) && maxScrollY > 0) {
            val visibleRatio = if (contentHeight > 0.0) height / contentHeight else 1.0
            val scrollSpeed = 15.0 * max(1.0, 1.0 / visibleRatio)
            val newScrollY = (scrollY - amount * scrollSpeed).coerceIn(0.0, maxScrollY)

            if (newScrollY != scrollY) {
                scrollY = newScrollY
                return true
            }
        }

        val (transformedX, transformedY) = transformPoint(mouseX, mouseY)

        if (activeComponent != null) {
            if (activeComponent!!.onScroll(transformedX, transformedY, amount)) {
                return true
            }
        }

        if (!contains(mouseX, mouseY)) return false

        val sortedChildren = children.sortedByDescending { it.priority }

        for (child in sortedChildren) {
            if (child.contains(transformedX, transformedY)) {
                if (child.onScroll(transformedX, transformedY, amount)) {
                    return true
                }
            }
        }

        return false
    }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (activeComponent != null) {
            if (activeComponent!!.onKeyPress(keyCode, scanCode, modifiers)) {
                return true
            }
        }

        for (child in children) {
            if (child.onKeyPress(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        if (activeComponent != null) {
            if (activeComponent!!.onCharTyped(chr, modifiers)) {
                return true
            }
        }

        for (child in children) {
            if (child.onCharTyped(chr, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun contains(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    fun setActiveComponent(component: UIComponent?) {
        activeComponent = component
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        for (child in children) {
            child.setPosition(child.x + deltaX, child.y + deltaY)
        }
    }
}