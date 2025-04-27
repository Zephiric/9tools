package impl.ui.collector.screens.views

import impl.ui.collector.CollectionView
import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.registry.Registries
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.UIComponent
import impl.ui.collector.utils.MapRenderUtils
import java.awt.Color

class GridView : CollectionView() {
    override var x = 0.0
    override var y = 0.0
    override var width = 0.0
    override var height = 0.0
    override var items = listOf<CollectorItem>()

    private val itemSize = 32
    private val padding = 8

    private val itemsPerRow: Int
        get() {
            val cellWidth = itemSize + padding
            if (cellWidth <= 0) return 1
            return ((width - 20) / cellWidth).toInt().coerceAtLeast(1)
        }

    override fun calculateContentHeight(): Double {
        val rows = (items.size + itemsPerRow - 1) / itemsPerRow
        return (rows * (itemSize + padding)).toDouble()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateScrollLimits()
        var foundHoveredItem = false

        context.matrices.push()
        context.matrices.translate(0f, -scrollOffset.toFloat(), 0f)

        val snapshot = items.toList()

        snapshot.forEachIndexed { index, item ->
            val (itemX, itemY) = getItemPosition(index)
            val localY = itemY - y

            if (localY + itemSize >= scrollOffset && localY <= scrollOffset + height) {
                renderItemSlot(context, item, itemX, itemY, mouseX, mouseY + scrollOffset.toInt())

                if (isMouseOver(mouseX.toDouble(), (mouseY + scrollOffset), itemX, itemY)) {
                    hoveredItem = item
                    mouseHoverX = mouseX.toDouble()
                    mouseHoverY = mouseY.toDouble()
                    onItemHover?.invoke(item, mouseX.toDouble(), mouseY.toDouble())
                    foundHoveredItem = true
                }
            }
        }

        if (!foundHoveredItem) {
            hoveredItem = null
            onItemHover?.invoke(null, mouseX.toDouble(), mouseY.toDouble())
        }

        context.matrices.pop()
    }

    private fun renderItemSlot(
        context: DrawContext,
        item: CollectorItem,
        itemX: Double,
        itemY: Double,
        mouseX: Int,
        mouseY: Int
    ) {

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            itemX,
            itemY,
            itemX + itemSize,
            itemY + itemSize,
            2f,
            10f
        )

        if (Registries.ITEM.getId(item.baseItem).toString() == "minecraft:filled_map") {
            val mapId = MapRenderUtils.getMapIdFromItem(item)

            if (mapId != null) {
                val serverTag = MapRenderUtils.getServerTagForMap(item)

                val rendered = MapRenderUtils.renderMapInItemSlot(
                    context,
                    mapId,
                    serverTag,
                    (itemX + 8).toInt(),
                    (itemY + 8).toInt(),
                    16
                )

                if (!rendered) {
                    renderNormalItem(context, item, itemX, itemY)
                }
            } else {
                renderNormalItem(context, item, itemX, itemY)
            }
        } else {
            renderNormalItem(context, item, itemX, itemY)
        }

        if (isMouseOver(mouseX.toDouble(), mouseY.toDouble(), itemX, itemY)) {
            win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedOutline(
                context.matrices,
                Color(0.4f, 0.4f, 0.9f, 0.9f),
                itemX,
                itemY,
                itemX + itemSize,
                itemY + itemSize,
                2f,
                1f,
                10f
            )

            val itemName = item.customName
            val nameWidth = mc.textRenderer.getWidth(itemName)
            val nameX = (itemX + (itemSize - nameWidth) / 2).toInt()
            val nameY = (itemY + itemSize + 2).toInt()

            context.drawTextWithShadow(
                mc.textRenderer,
                itemName,
                nameX,
                nameY,
                0xFFFFFF
            )
        }
    }

    private fun renderNormalItem(context: DrawContext, item: CollectorItem, itemX: Double, itemY: Double) {
        val stack = CollectorItem.toItemStack(item)
        context.drawItem(stack, (itemX + 8).toInt(), (itemY + 8).toInt())
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || !isMouseInBounds(mouseX, mouseY)) return false

        val snapshot = items.toList()
        snapshot.forEachIndexed { index, item ->
            val (itemX, itemY) = getItemPosition(index)
            if (isMouseOver(mouseX, mouseY + scrollOffset, itemX, itemY)) {
                onItemClick?.invoke(item)
                return true
            }
        }
        return false
    }

    private fun getItemPosition(index: Int): Pair<Double, Double> {
        val row = index / itemsPerRow
        val col = index % itemsPerRow
        val itemX = x + 10 + col * (itemSize + padding)
        val itemY = y + row * (itemSize + padding)
        return Pair(itemX, itemY)
    }

    private fun isMouseOver(mx: Double, my: Double, itemX: Double, itemY: Double): Boolean {
        return mx in itemX..(itemX + itemSize) &&
                my in itemY..(itemY + itemSize)
    }

    override fun getSettingsComponent(): UIComponent? = null

    override fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
    }
}