package impl.ui.collector.screens.views

import impl.ui.collector.CollectionView
import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.UIComponent
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class ListView : CollectionView() {
    override var x = 0.0
    override var y = 0.0
    override var width = 0.0
    override var height = 0.0
    override var items = listOf<CollectorItem>()

    private val itemHeight = 32
    private val padding = 2
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy")

    override fun calculateContentHeight(): Double {
        return items.size * (itemHeight + padding).toDouble()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateScrollLimits()
        var foundHoveredItem = false

        context.matrices.push()
        context.matrices.translate(0f, -scrollOffset.toFloat(), 0f)

        var currentY = 0.0
        items.forEach { item ->
            val renderY = y + currentY

            if (currentY + itemHeight >= scrollOffset && currentY <= scrollOffset + height) {

                win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                    context.matrices,
                    Color(0.2f, 0.2f, 0.2f, 0.8f),
                    x,
                    renderY,
                    x + width,
                    renderY + itemHeight,
                    2f,
                    10f
                )


                val stack = CollectorItem.toItemStack(item)
                context.drawItem(stack, (x + 5).toInt(), (renderY + 5).toInt())


                val nameX = (x + 35).toInt()
                val nameY = (renderY + 5).toInt()

                context.drawTextWithShadow(
                    mc.textRenderer,
                    item.customName,
                    nameX,
                    nameY,
                    0xFFFFFF
                )

                val baseItemName = "(${item.baseItem.name.string})"
                context.drawTextWithShadow(
                    mc.textRenderer,
                    baseItemName,
                    nameX + mc.textRenderer.getWidth(item.customName) + 5,
                    nameY,
                    0x999999
                )


                val dateStr = dateFormat.format(Date(item.dateReceived))
                val rightSideInfo = "Given by: ${item.givenBy} | ${dateStr}"
                context.drawTextWithShadow(
                    mc.textRenderer,
                    rightSideInfo,
                    (x + width - mc.textRenderer.getWidth(rightSideInfo) - 5).toInt(),
                    nameY,
                    0x999999
                )


                val componentInfo = if (!item.components.isEmpty) {
                    val count = item.components.size()
                    "Has ${count} component${if (count > 1) "s" else ""}"
                } else {
                    "No special components"
                }

                context.drawTextWithShadow(
                    mc.textRenderer,
                    componentInfo,
                    nameX,
                    nameY + 12,
                    0x666666
                )


                if (isMouseOver(mouseX.toDouble(), (mouseY + scrollOffset), currentY)) {
                    win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedOutline(
                        context.matrices,
                        Color(0.4f, 0.4f, 0.9f, 0.9f),
                        x,
                        renderY,
                        x + width,
                        renderY + itemHeight,
                        2f,
                        1f,
                        10f
                    )

                    hoveredItem = item
                    mouseHoverX = mouseX.toDouble()
                    mouseHoverY = mouseY.toDouble()
                    onItemHover?.invoke(item, mouseX.toDouble(), mouseY.toDouble())
                    foundHoveredItem = true
                }
            }

            currentY += itemHeight + padding
        }

        if (!foundHoveredItem) {
            hoveredItem = null
            onItemHover?.invoke(null, mouseX.toDouble(), mouseY.toDouble())
        }

        context.matrices.pop()
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isMouseInBounds(mouseX, mouseY)) return false

        var currentY = 0.0
        items.forEach { item ->
            if (isMouseOver(mouseX, mouseY + scrollOffset, currentY)) {
                onItemClick?.invoke(item)
                return true
            }
            currentY += itemHeight + padding
        }
        return false
    }

    private fun isMouseOver(mx: Double, my: Double, localY: Double): Boolean {
        val screenY = y + localY
        return mx in x..(x + width) && my in screenY..(screenY + itemHeight)
    }


    override fun getSettingsComponent(): UIComponent? = null

    override fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
    }
}