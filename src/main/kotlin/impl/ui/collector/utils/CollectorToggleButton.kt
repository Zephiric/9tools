package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color

/**
 * A UI component representing a toggle button with an optional label and tooltip.
 *
 * The toggle consists of a pill-shaped track with an animated circular handle that slides
 * between the off and on states. The component supports hover effects, tooltips, and a callback
 * function when the state changes.
 *
 * @param x The x-coordinate of the component.
 * @param y The y-coordinate of the component.
 * @param width The width of the toggle track.
 * @param height The height of the toggle track.
 * @param label Optional text label to display to the left of the toggle.
 * @param description Optional tooltip description to display when hovering over the component.
 * @param state The initial toggle state.
 * @param onChange A callback invoked when the toggle state changes.
 */
class CollectorToggleButton(
    override var x: Double,
    override var y: Double,
    override var width: Double = 40.0,
    override var height: Double = 20.0,
    val label: String?,
    private val description: String?,
    state: Boolean = false,
    private val onChange: (Boolean) -> Unit
) : UIComponent {

    /**
     * The current state of the toggle.
     *
     * Setting the state triggers the [onChange] callback if the state is updated.
     */
    var state: Boolean = state
        set(value) {
            if (field != value) {
                field = value
                onChange(value)
            }
        }


    private var animation = 0f

    private var lastUpdate = System.currentTimeMillis()

    private var hovered = false

    /**
     * Calculates the full width of the component, taking into account the optional label.
     *
     * @return The total width including label (if present) and toggle track.
     */
    private val totalWidth: Double
        get() = if (label != null) {
            mc.textRenderer.getWidth(label) + 10 + width
        } else width

    /**
     * Checks if a given point is within the bounds of the component.
     *
     * @param mx The x-coordinate of the point.
     * @param my The y-coordinate of the point.
     * @return `true` if the point is within the component; `false` otherwise.
     */
    override fun contains(mx: Double, my: Double): Boolean {
        return mx >= x && mx <= x + totalWidth &&
                my >= y && my <= y + height
    }

    /**
     * Renders the toggle button, including the label (if present), the track, the animated handle,
     * and the tooltip on hover.
     *
     * @param context The draw context to render the component.
     * @param mouseX The current mouse x-coordinate.
     * @param mouseY The current mouse y-coordinate.
     * @param delta The partial tick time.
     */
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        hovered = contains(mouseX.toDouble(), mouseY.toDouble())

        val now = System.currentTimeMillis()
        val deltaTime = (now - lastUpdate) / 300f
        lastUpdate = now

        animation = if (state) {
            (animation + deltaTime).coerceAtMost(1f)
        } else {
            (animation - deltaTime).coerceAtLeast(0f)
        }


        label?.let {
            context.drawTextWithShadow(
                mc.textRenderer,
                it,
                x.toInt(),
                (y + 6).toInt(),
                Color.WHITE.rgb
            )
        }


        val toggleX = if (label != null) {
            x + mc.textRenderer.getWidth(label) + 10
        } else {
            x
        }


        val trackColor = Color(
            0.2f + (0.2f * animation),
            0.2f + (0.4f * animation),
            0.2f + (0.8f * animation),
            if (hovered) 0.9f else 0.8f
        )

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            trackColor,
            toggleX, y + 6,
            toggleX + width, y + height - 6,
            ((height - 12) / 2).toFloat(),
            10f
        )

        val handleDiameter = height - 10
        val handleX = toggleX + (animation * (width - handleDiameter))
        val handleY = y + (height - handleDiameter) / 2
        val handleColor = if (hovered)
            Color(1f, 1f, 1f, 0.9f)
        else
            Color(0.9f, 0.9f, 0.9f, 0.8f)

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            handleColor,
            handleX, handleY,
            handleX + handleDiameter, handleY + handleDiameter,
            (handleDiameter / 2).toFloat(),
            10f
        )

        if (hovered && description != null) {
            context.drawTooltip(
                mc.textRenderer,
                listOf(net.minecraft.text.Text.literal(description)),
                mouseX,
                mouseY
            )
        }
    }

    /**
     * Handles mouse click events. If the left mouse button is clicked within the component bounds,
     * the toggle state is switched.
     *
     * @param mouseX The x-coordinate of the mouse click.
     * @param mouseY The y-coordinate of the mouse click.
     * @param button The mouse button that was pressed.
     * @return `true` if the click was handled; `false` otherwise.
     */
    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && contains(mouseX, mouseY)) {
            toggle()
            return true
        }
        return false
    }

    /**
     * Called when a drag event occurs. This component does not support dragging, so this method
     * always returns false.
     *
     * @return `false` since dragging is not handled.
     */
    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int): Boolean = false

    /**
     * Called when a scroll event occurs. This component does not support scrolling, so this method
     * always returns false.
     *
     * @return `false` since scrolling is not handled.
     */
    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    /**
     * Called when a key is pressed. This component does not support key events, so this method
     * always returns false.
     *
     * @return `false` since key events are not handled.
     */
    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    /**
     * Toggles the state of the button and triggers the [onChange] callback.
     */
    fun toggle() {
        state = !state
    }
}
