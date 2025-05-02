package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color

/**
 * A clickable button that also implements UIComponent so it can
 * be stored in the `components` list of your screen.
 */
class CollectorButton(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double = 20.0,
    val text: String,
    private val onClickAction: () -> Unit,
    var type: ButtonType = ButtonType.NEUTRAL,
    val tooltipText: String? = null
) : UIComponent {

    private var isHovered = false

    enum class ButtonType {
        NEUTRAL,
        POSITIVE,
        NEGATIVE,
        HIGHLIGHT,
        NEW_COLLECTION
    }

    /**
     * Renders the button with an optional hover effect.
     */
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        isHovered = contains(mouseX.toDouble(), mouseY.toDouble())

        val baseColor = when (type) {
            ButtonType.NEUTRAL -> Color(51, 51, 51, 204)
            ButtonType.POSITIVE -> Color(51, 102, 51, 204)
            ButtonType.NEGATIVE -> Color(102, 51, 51, 204)
            ButtonType.HIGHLIGHT -> Color(51, 51, 102, 204)
            ButtonType.NEW_COLLECTION -> Color(25, 25, 102, 204)
        }


        val buttonColor = if (isHovered) {
            Color(
                (baseColor.red * 2f).coerceIn(0f, 255f).toInt(),
                (baseColor.green * 2f).coerceIn(0f, 255f).toInt(),
                (baseColor.blue * 2f).coerceIn(0f, 255f).toInt(),
                baseColor.alpha
            )
        } else {
            baseColor
        }

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            buttonColor,
            x,
            y,
            x + width,
            y + height,
            2f,
            10f
        )

        val textWidth = mc.textRenderer.getWidth(text)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - textHeight) / 2

        context.drawTextWithShadow(
            mc.textRenderer,
            text,
            (textX + 1).toInt(),
            (textY + 1).toInt(),
            Color(1f, 1f, 1f, 0.9f).rgb
        )
    }

    /**
     * Called when a mouse click occurs. Return true if the click is consumed.
     */
    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {

        if (button == 0 && contains(mouseX, mouseY)) {
            onClickAction()
            return true
        }
        return false
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ) = false

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double) = false
    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) = false

    companion object {

        fun createNewButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 35.0,
                height = 20.0,
                text = "+",
                onClickAction = onClick,
                type = ButtonType.NEW_COLLECTION,
                tooltipText = "New Collection"
            )
        }

        fun createAddButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 35.0,
                height = 20.0,
                text = "+",
                onClickAction = onClick,
                type = ButtonType.POSITIVE,
                tooltipText = "Add Item"
            )
        }

        fun createDeleteButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 20.0,
                height = 20.0,
                text = "×",
                onClickAction = onClick,
                type = ButtonType.NEGATIVE,
                tooltipText = "Delete Item"
            )
        }

        fun createBackButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 20.0,
                height = 20.0,
                text = "←",
                onClickAction = onClick,
                type = ButtonType.NEUTRAL,
                tooltipText = "Back"
            )
        }

        fun createEditButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 20.0,
                height = 20.0,
                text = "✎",
                onClickAction = onClick,
                type = ButtonType.HIGHLIGHT,
                tooltipText = "Edit Item"
            )
        }

        fun createSpawnButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 20.0,
                height = 20.0,
                text = "↯",
                onClickAction = onClick,
                type = ButtonType.POSITIVE,
                tooltipText = "Spawn Item"
            )
        }

        fun createComponentsButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 20.0,
                height = 20.0,
                text = "⚙",
                onClickAction = onClick,
                type = ButtonType.NEW_COLLECTION,
                tooltipText = "View Components"
            )
        }

        fun createSettingsButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 25.0,
                height = 20.0,
                text = "⚙",
                onClickAction = onClick,
                type = ButtonType.NEUTRAL,
                tooltipText = "Settings"
            )
        }

        fun createSaveButton(x: Double, y: Double, onClick: () -> Unit): CollectorButton {
            return CollectorButton(
                x = x,
                y = y,
                width = 60.0,
                height = 20.0,
                text = "Save",
                onClickAction = onClick,
                type = ButtonType.POSITIVE,
                tooltipText = "Save Changes"
            )
        }
    }
}
