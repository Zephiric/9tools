package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import impl.ui.collector.SettingsManager
import java.awt.Color

class UIDropdown(
    override var x: Double,
    override var y: Double,
    override var width: Double = 150.0,
    override var height: Double = 20.0,
    val label: String? = null,
    private val itemHeight: Double = 20.0,
    private val maxVisibleItems: Int = SettingsManager.dropdownVisibleItems,
    private val maxDropdownHeight: Double = 600.0,
    private val onSelect: (String) -> Unit,
    private val onDelete: ((String) -> Unit)? = null,
    private val tooltipText: String? = null
) : UIComponent {

    override val priority: Int = 10
    private var isOpen = false
    private var isHovered = false
    private val buttons = mutableMapOf<String, CollectorButton>()
    var currentSelection: String = ""
    private var scrollOffset = 0


    var items: List<String> = listOf()
        set(value) {
            field = value
            scrollOffset = scrollOffset.coerceIn(0, getMaxScroll())
        }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        isHovered = isMouseOverMain(mouseX.toDouble(), mouseY.toDouble())




        context.matrices.push()

        context.matrices.translate(0f, 0f, 300f)


        var dropdownX = x
        label?.let {

            context.drawTextWithShadow(
                mc.textRenderer,
                it,
                x.toInt(),
                (y + 6).toInt(),
                Color.WHITE.rgb
            )
            dropdownX = x + mc.textRenderer.getWidth(it) + 10
        }

        val bgColor = if (isHovered) {
            Color(0.25f, 0.25f, 0.25f, 0.85f)
        } else {
            Color(0.2f, 0.2f, 0.2f, 0.8f)
        }

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            bgColor,
            dropdownX, y,
            dropdownX + width, y + height,
            2f,
            10f
        )

        val displayText = if (currentSelection.isEmpty()) "Select..." else currentSelection
        context.drawTextWithShadow(
            mc.textRenderer,
            displayText,
            (dropdownX + 5).toInt(),
            (y + 6).toInt(),
            Color(1f,1f,1f, if (currentSelection.isEmpty()) 0.7f else 0.9f).rgb
        )

        val maxScroll = getMaxScroll()
        val arrowChar = when {
            !isOpen -> "▼"
            maxScroll > 0 -> "▲▼"
            else -> "▲"
        }
        context.drawTextWithShadow(
            mc.textRenderer,
            arrowChar,
            (dropdownX + width - 15).toInt(),
            (y + 6).toInt(),
            Color.WHITE.rgb
        )


        if (isHovered && tooltipText != null) {
            val lines = tooltipText.split("\n").map { net.minecraft.text.Text.literal(it) }
            context.drawTooltip(mc.textRenderer, lines, mouseX, mouseY)
        }


        if (isOpen) {
            renderDropdownList(context, dropdownX, items, mouseX, mouseY, delta)
        } else {

            buttons.clear()
        }




        context.matrices.pop()
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val dx = label?.let { x + mc.textRenderer.getWidth(it) + 10 } ?: x

        if (isMouseOverMain(mouseX, mouseY)) {
            isOpen = !isOpen
            return true
        }


        if (isOpen) {
            val effectiveVisible = getEffectiveVisibleItems(items.size)
            val visibleItems = items.drop(scrollOffset).take(effectiveVisible)

            visibleItems.forEachIndexed { index, item ->
                val itemY = y + height + index * itemHeight

                if (isMouseOverItem(mouseX, mouseY, dx, itemY)) {
                    val actualIndex = scrollOffset + index
                    if (actualIndex < items.size) {
                        currentSelection = items[actualIndex]
                        onSelect(currentSelection)
                        isOpen = false
                        return true
                    }
                }


                buttons["delete_$item"]?.let { btn ->
                    if (btn.contains(mouseX, mouseY)) {
                        btn.onClick(mouseX, mouseY, button)
                        return true
                    }
                }
            }


            if (!isMouseOverDropdown(mouseX, mouseY, dx)) {
                isOpen = false
                buttons.clear()
            }
        }
        return false
    }

    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int) = false
    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!isOpen) return false
        val dx = label?.let { x + mc.textRenderer.getWidth(it) + 10 } ?: x
        if (!isMouseOverDropdown(mouseX, mouseY, dx)) return false

        val maxS = getMaxScroll()
        return if (amount < 0 && scrollOffset < maxS) {
            scrollOffset++
            true
        } else if (amount > 0 && scrollOffset > 0) {
            scrollOffset--
            true
        } else false
    }
    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) = false

    private fun renderDropdownList(
        context: DrawContext,
        dropdownX: Double,
        items: List<String>,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        val total = items.size
        val visCount = getEffectiveVisibleItems(total)
        scrollOffset = scrollOffset.coerceIn(0, getMaxScroll())

        val visibleItems = items.drop(scrollOffset).take(visCount)
        val ddHeight = visibleItems.size * itemHeight
        val extraWidth = if (onDelete != null) 30.0 else 0.0

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.15f, 0.15f, 0.15f, 0.95f),
            dropdownX,
            y + height,
            dropdownX + width + extraWidth,
            (y + height + ddHeight).coerceAtMost(y + maxDropdownHeight),
            2f, 10f
        )

        cleanupButtons(items)

        visibleItems.forEachIndexed { index, item ->
            val itemY = y + height + index * itemHeight
            val hovered = isMouseOverItem(mouseX.toDouble(), mouseY.toDouble(), dropdownX, itemY)

            if (hovered) {
                win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                    context.matrices,
                    Color(0.3f, 0.3f, 0.3f, 0.8f),
                    dropdownX, itemY,
                    dropdownX + width, itemY + itemHeight,
                    2f, 10f
                )
            }

            context.drawTextWithShadow(
                mc.textRenderer,
                item,
                (dropdownX + 5).toInt(),
                (itemY + 6).toInt(),
                Color.WHITE.rgb
            )

            onDelete?.let {
                ensureDeleteButton(item, dropdownX + width + 5, itemY)
                buttons["delete_$item"]?.render(context, mouseX, mouseY, delta)
            }
        }


        val maxS = getMaxScroll()
        if (maxS > 0) {
            if (scrollOffset > 0) {
                context.drawTextWithShadow(
                    mc.textRenderer,
                    "▲",
                    (dropdownX + width - 15).toInt(),
                    (y + height + 2).toInt(),
                    Color(1f, 1f, 1f, 0.7f).rgb
                )
            }
            if (scrollOffset < maxS) {
                context.drawTextWithShadow(
                    mc.textRenderer,
                    "▼",
                    (dropdownX + width - 15).toInt(),
                    (y + height + (visibleItems.size - 1) * itemHeight).toInt(),
                    Color(1f, 1f, 1f, 0.7f).rgb
                )
            }
        }
    }

    private fun ensureDeleteButton(item: String, btnX: Double, itemY: Double) {
        val btnId = "delete_$item"
        if (!buttons.containsKey(btnId)) {
            buttons[btnId] = CollectorButton.createDeleteButton(
                btnX, itemY
            ) {
                onDelete?.invoke(item)
                isOpen = false
            }
        } else {
            buttons[btnId]?.setPosition(btnX, itemY)
        }
    }

    private fun cleanupButtons(items: List<String>) {
        buttons.keys.toList().forEach { key ->
            val name = key.removePrefix("delete_")
            if (!items.contains(name)) {
                buttons.remove(key)
            }
        }
    }

    private fun isMouseOverMain(mx: Double, my: Double): Boolean {
        val dx = label?.let { x + mc.textRenderer.getWidth(it) + 10 } ?: x
        return mx in dx..(dx + width) && my in y..(y + height)
    }

    private fun isMouseOverItem(mx: Double, my: Double, dropX: Double, itemY: Double): Boolean {
        return mx in dropX..(dropX + width) && my in itemY..(itemY + itemHeight)
    }

    private fun isMouseOverDropdown(mx: Double, my: Double, dropX: Double): Boolean {
        val visCount = getEffectiveVisibleItems(items.size)
        val ddHeight = visCount * itemHeight
        val extraWidth = if (onDelete != null) 30.0 else 0.0
        val bottomY = (y + height + ddHeight).coerceAtMost(y + maxDropdownHeight)
        val rightX = dropX + width + extraWidth

        return (mx in dropX..rightX) && (my in y..bottomY)
    }

    private fun getMaxScroll(): Int {
        return (items.size - maxVisibleItems).coerceAtLeast(0)
    }

    private fun getEffectiveVisibleItems(total: Int): Int {
        return minOf(maxVisibleItems, total)
    }
}
