package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color

class SelectableGrid(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double = 100.0,
    private val onSelect: (String) -> Unit
) : UIComponent {

    override val priority: Int = 10

    private val buttonSpacing = 2.0
    private val textPadding = 8.0
    private val minButtonWidth = 40.0
    private val buttonHeight = 12.0

    private var items = mutableListOf<GridItem>()
    private var activeItems = mutableSetOf<String>()
    private var buttonLayout = mutableListOf<ButtonPosition>()

    data class GridItem(
        val name: String,
        val isSelectable: Boolean,
        var description: String = "Not Set"
    )

    private data class ButtonPosition(
        val item: GridItem,
        var x: Double,
        var y: Double,
        var width: Double
    )

    fun setItems(newItems: List<GridItem>) {
        items = newItems.toMutableList()
        calculateLayout()
    }

    override fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
        calculateLayout()
    }

    private fun calculateLayout() {
        buttonLayout.clear()

        var currentX = x
        var currentY = y
        val scale = 0.75f

        items.forEach { item ->
            val textWidth = mc.textRenderer.getWidth(item.name) * scale
            val buttonWidth = maxOf(minButtonWidth, textWidth + textPadding)

            if (currentX + buttonWidth > x + width && currentX > x) {
                currentX = x
                currentY += buttonHeight + buttonSpacing
            }

            buttonLayout.add(ButtonPosition(
                item = item,
                x = currentX,
                y = currentY,
                width = buttonWidth
            ))

            currentX += buttonWidth + buttonSpacing
        }

        height = (currentY - y) + buttonHeight
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        buttonLayout.forEach { button ->
            val isHovered = mouseX >= button.x && mouseX <= button.x + button.width &&
                    mouseY >= button.y && mouseY <= button.y + buttonHeight

            val bgColor = when {
                !button.item.isSelectable -> Color(0.2f, 0.2f, 0.2f, 0.5f)
                isHovered -> Color(0.3f, 0.5f, 0.9f, 0.4f)
                activeItems.contains(button.item.name) -> Color(0.3f, 0.5f, 0.9f, 0.8f)
                else -> if (button.item.isSelectable) {
                    Color(0.3f, 0.5f, 0.9f, 0.2f)
                } else {
                    Color(0.2f, 0.2f, 0.2f, 0.5f)
                }
            }

            win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                context.matrices,
                bgColor,
                button.x, button.y,
                button.x + button.width, button.y + buttonHeight,
                2f, 10f
            )

            context.matrices.push()
            context.matrices.scale(0.75f, 0.75f, 1f)
            val scaledX = (button.x + (button.width - mc.textRenderer.getWidth(button.item.name) * 0.75f) / 2) / 0.75f
            val scaledY = (button.y + 3) / 0.75f

            val textColor = if (!button.item.isSelectable)
                Color(0.5f, 0.5f, 0.5f, 0.5f) else Color.WHITE

            context.drawTextWithShadow(
                mc.textRenderer,
                button.item.name,
                scaledX.toInt(),
                scaledY.toInt(),
                textColor.rgb
            )
            context.matrices.pop()

            if (isHovered) {
                context.drawTooltip(
                    mc.textRenderer,
                    listOf(Text.literal("${button.item.name}: ${button.item.description}")),
                    mouseX,
                    mouseY
                )
            }
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        buttonLayout.forEach { buttonPos ->
            if (mouseX >= buttonPos.x && mouseX <= buttonPos.x + buttonPos.width &&
                mouseY >= buttonPos.y && mouseY <= buttonPos.y + buttonHeight) {

                if (!buttonPos.item.isSelectable) return@forEach
                onSelect(buttonPos.item.name)
                return true
            }
        }
        return false
    }


    fun updateItemDescription(name: String, description: String) {
        items.find { it.name == name }?.description = description
    }

    fun setActiveItem(name: String, active: Boolean) {
        if (active) {
            activeItems.add(name)
        } else {
            activeItems.remove(name)
        }
    }

    fun getActiveItems(): Set<String> = activeItems.toSet()
}