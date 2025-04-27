package impl.ui.collector

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair
import net.minecraft.client.gui.DrawContext

/**
 * A common interface for any UI element that needs to:
 *   - Render itself
 *   - Handle mouse/keyboard input
 *   - Manage its position/dimensions
 *   - Possibly participate in layering/priority
 *
 * All input methods are optional with default no-op implementations.
 * Override them in your component only if you need that input event.
 *
 * dumb dumb terms, helps pass inputs and fields to their respective place so i can reduce future redundant code
 *
 * thank you jippity
 */
interface UIComponent {
    /**
     * The X position of this UI component.
     */
    var x: Double

    /**
     * The Y position of this UI component.
     */
    var y: Double

    /**
     * The width of this UI component.
     */
    var width: Double

    /**
     * The height of this UI component.
     */
    var height: Double

    /**
     * An optional priority or "z-index" if multiple components overlap.
     * Components with higher priority can intercept events first.
     */
    val priority: Int
        get() = 0

    /**
     * Interface for components that support labels
     */
    interface LabeledComponent : UIComponent {
        /**
         * The label text for this component
         */
        val label: String?

        /**
         * The position offset for the label relative to the component
         * (0,0) would place the label at the component's top-left corner
         * Null value means use the component's default positioning
         */
        var labelOffset: Pair<Double, Double>?

        /**
         * Renders the label according to its position
         */
        fun renderLabel(context: DrawContext, defaultOffsetX: Double, defaultOffsetY: Double)
    }

    // ------------------------------------------------------------------------
    //                            Rendering
    // ------------------------------------------------------------------------

    /**
     * Renders this component to the screen.
     *
     * @param context The DrawContext for rendering.
     * @param mouseX The current mouse X position (already scaled if needed).
     * @param mouseY The current mouse Y position (already scaled if needed).
     * @param delta The partial tick or frame delta time.
     */
    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

    // ------------------------------------------------------------------------
    //                            Mouse Events
    // ------------------------------------------------------------------------

    /**
     * Called when a mouse click event occurs.
     * @param mouseX The mouse X position.
     * @param mouseY The mouse Y position.
     * @param button The mouse button (0 = left, 1 = right, etc.).
     * @return True if this component consumed the click, false otherwise.
     */
    fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    /**
     * Called when a mouse drag event occurs (press + move).
     * @param mouseX The mouse X position.
     * @param mouseY The mouse Y position.
     * @param deltaX The drag distance horizontally since last event.
     * @param deltaY The drag distance vertically since last event.
     * @param button The mouse button being held during the drag.
     * @return True if this component consumed the drag, false otherwise.
     */
    fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean = false

    /**
     * Called when the mouse wheel is scrolled.
     * @param mouseX The mouse X position.
     * @param mouseY The mouse Y position.
     * @param amount The scroll amount (positive/negative).
     * @return True if this component consumed the scroll, false otherwise.
     */
    fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    /**
     * Called when the mouse is released after a click/drag.
     * @param mouseX The mouse X position.
     * @param mouseY The mouse Y position.
     * @param button The mouse button (0 = left, 1 = right, etc.).
     * @return True if this component consumed the mouse release, false otherwise.
     */
    fun onMouseRelease(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    // ------------------------------------------------------------------------
    //                            Keyboard Events
    // ------------------------------------------------------------------------

    /**
     * Called when a key is pressed while this UI is active.
     * @param keyCode The GLFW key code.
     * @param scanCode The platform scan code.
     * @param modifiers Bitmask of modifiers (Shift, Ctrl, Alt).
     * @return True if this component consumed the key press, false otherwise.
     */
    fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    /**
     * Called when a key is released.
     * @param keyCode The GLFW key code.
     * @param scanCode The platform scan code.
     * @param modifiers Bitmask of modifiers (Shift, Ctrl, Alt).
     * @return True if this component consumed the key release, false otherwise.
     */
    fun onKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    /**
     * Called for typed characters.
     * This is separate from [onKeyPress] because many keys do not produce a character.
     * @param chr The typed character.
     * @param modifiers Bitmask of modifiers (Shift, Ctrl, Alt).
     * @return True if this component consumed the typed character, false otherwise.
     */
    fun onCharTyped(chr: Char, modifiers: Int): Boolean = false

    // ------------------------------------------------------------------------
    //                           Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Checks if the given coordinates are inside this component.
     * @return True if (mx, my) is within this component's bounds, else false.
     */
    fun contains(mx: Double, my: Double): Boolean {
        return (mx >= x && mx <= x + width &&
                my >= y && my <= y + height)
    }

    /**
     * Repositions the component.
     */
    fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
    }

    fun getChildComponents(): List<UIComponent> = emptyList()
}