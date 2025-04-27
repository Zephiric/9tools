package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color
import kotlin.math.min
import kotlin.math.max

/**
 * A reusable text input box that handles its own rendering and input events.
 * Implements the [UIComponent] interface for integration into the component-based UI system.
 */
class CollectorTextBox(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double = 25.0,
    private val label: String? = null,
    private val placeholder: String = "",
    private val maxLength: Int = 100,
    initialText: String = "",
    private val onChange: (String) -> Unit
) : UIComponent {

    // Internal state
    private var text = initialText.take(maxLength)
    private var focused = false
    private var cursorPos = text.length.coerceAtMost(maxLength)
    private var selectionStart: Int? = null

    // Label positioning
    private var labelOffsetX: Double? = null
    private var labelOffsetY: Double? = null

    // For handling double/triple clicks
    private var lastClickTime = 0L
    private var clickCount = 0

    override val priority: Int get() = 5

    /**
     * Sets custom label position offsets from the component's top-left corner
     * @param offsetX X offset from component left edge
     * @param offsetY Y offset from component top edge
     */
    fun setLabelPosition(offsetX: Double, offsetY: Double) {
        this.labelOffsetX = offsetX
        this.labelOffsetY = offsetY
    }

    /**
     * Renders the text box, including background, text, selection, and cursor.
     */
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Validate cursor position and selection
        cursorPos = cursorPos.coerceIn(0, text.length)
        selectionStart = selectionStart?.coerceIn(0, text.length)

        // Render label if present
        renderLabel(context)

        // Render background
        val bgColor = if (focused) {
            Color(0.25f, 0.25f, 0.25f, 0.8f)
        } else {
            Color(0.2f, 0.2f, 0.2f, 0.8f)
        }
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            bgColor,
            x, y,
            x + width, y + height,
            3f,
            10f
        )

        // Determine text to display
        val displayText = if (text.isEmpty() && !focused) placeholder else text
        val textColor = if (text.isEmpty() && !focused) {
            Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
        } else {
            Color(1f, 1f, 1f, 0.9f).rgb
        }

        // Render text
        context.drawTextWithShadow(
            mc.textRenderer,
            displayText,
            (x + 5).toInt(),
            (y + (height - mc.textRenderer.fontHeight) / 2).toInt(),
            textColor
        )

        // Render text selection background if we have a valid selection
        if (focused && selectionStart != null && selectionStart != cursorPos) {
            val selStart = min(selectionStart!!, cursorPos).coerceIn(0, text.length)
            val selEnd = max(selectionStart!!, cursorPos).coerceIn(0, text.length)

            if (selStart < selEnd && selEnd <= text.length) {
                val selectedText = text.substring(selStart, selEnd)
                val leftText = text.substring(0, selStart)
                val selectionWidth = mc.textRenderer.getWidth(selectedText)
                val selectionX = x + 5 + mc.textRenderer.getWidth(leftText)

                win.ninegang.ninetools.compat.util.render.Engine2d.renderQuad(
                    context.matrices,
                    Color(0.3f, 0.5f, 0.9f, 0.5f),
                    selectionX, y + 5,
                    selectionX + selectionWidth, y + height - 5
                )
            }
        }

        // Render blinking cursor
        if (focused && (System.currentTimeMillis() / 500) % 2L == 0L) {
            val cursorText = text.substring(0, cursorPos.coerceIn(0, text.length))
            val cursorX = x + 5 + mc.textRenderer.getWidth(cursorText)
            win.ninegang.ninetools.compat.util.render.Engine2d.renderQuad(
                context.matrices,
                Color(1f, 1f, 1f, 0.8f),
                cursorX, y + 5,
                cursorX + 1, y + height - 5
            )
        }
    }

    /**
     * Renders the label at the specified position or with default positioning
     */
    private fun renderLabel(context: DrawContext) {
        label?.let {
            if (labelOffsetX != null && labelOffsetY != null) {
                // Custom position
                context.drawTextWithShadow(
                    mc.textRenderer,
                    it,
                    (x + labelOffsetX!!).toInt(),
                    (y + labelOffsetY!!).toInt(),
                    Color(1f, 1f, 1f, 0.9f).rgb
                )
            } else {
                // Default position (above the text box)
                context.drawTextWithShadow(
                    mc.textRenderer,
                    it,
                    x.toInt(),
                    (y - 12).toInt(),
                    Color(1f, 1f, 1f, 0.9f).rgb
                )
            }
        }
    }

    /**
     * Handles mouse click events to manage focus and cursor positioning.
     */
    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false // Only handle left-click

        val wasFocused = focused
        focused = contains(mouseX, mouseY)

        if (focused) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) {
                clickCount++
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime

            when (clickCount) {
                2 -> selectWord()
                3 -> selectAll()
                else -> {
                    // Single click: position cursor
                    val relativeX = mouseX - (x + 5)
                    cursorPos = getCursorPosFromX(relativeX)
                    selectionStart = null
                }
            }
            return true
        } else {
            // Clicking outside unfocuses the text box
            selectionStart = null
            return wasFocused
        }
    }

    /**
     * Handles mouse drag events for text selection.
     */
    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int): Boolean {
        if (focused && button == 0) {
            val relativeX = mouseX - (x + 5)
            cursorPos = getCursorPosFromX(relativeX)
            if (selectionStart == null) {
                selectionStart = cursorPos
            }
            return true
        }
        return false
    }

    /**
     * Handles key press events for text navigation and manipulation.
     */
    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!focused) return false
        val isCtrl = (modifiers and GLFW.GLFW_MOD_CONTROL) != 0

        when (keyCode) {
            GLFW.GLFW_KEY_LEFT -> {
                cursorPos = if (isCtrl) findWordBoundary(false) else (cursorPos - 1).coerceAtLeast(0)
                if ((modifiers and GLFW.GLFW_MOD_SHIFT) == 0) selectionStart = null
                return true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                cursorPos = if (isCtrl) findWordBoundary(true) else (cursorPos + 1).coerceAtMost(text.length)
                if ((modifiers and GLFW.GLFW_MOD_SHIFT) == 0) selectionStart = null
                return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (selectionStart != null && selectionStart != cursorPos) {
                    deleteSelection()
                } else if (cursorPos > 0) {
                    text = text.removeRange((cursorPos - 1).coerceAtLeast(0), cursorPos.coerceAtMost(text.length))
                    cursorPos = (cursorPos - 1).coerceAtLeast(0)
                    onChange(text)
                }
                return true
            }
            GLFW.GLFW_KEY_DELETE -> {
                if (selectionStart != null && selectionStart != cursorPos) {
                    deleteSelection()
                } else if (cursorPos < text.length) {
                    text = text.removeRange(cursorPos, (cursorPos + 1).coerceAtMost(text.length))
                    onChange(text)
                }
                return true
            }
            GLFW.GLFW_KEY_HOME -> {
                cursorPos = 0
                if ((modifiers and GLFW.GLFW_MOD_SHIFT) == 0) selectionStart = null
                return true
            }
            GLFW.GLFW_KEY_END -> {
                cursorPos = text.length
                if ((modifiers and GLFW.GLFW_MOD_SHIFT) == 0) selectionStart = null
                return true
            }
            GLFW.GLFW_KEY_A -> {
                if (isCtrl) {
                    selectAll()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Handles character typing events for text input.
     */
    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        if (!focused) return false
        // Allow printable characters
        if (chr.isLetterOrDigit() || chr.isWhitespace() || chr in setOf('-', '_', '.', ',', '!', '?')) {
            if (text.length < maxLength) {
                val newText = text.substring(0, cursorPos) + chr + text.substring(cursorPos)
                text = newText
                cursorPos++
                onChange(text)
                return true
            }
        }
        return false
    }

    /**
     * Handles mouse release events. Can be used to finalize text selection.
     */
    override fun onMouseRelease(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Currently, no specific action needed on mouse release
        return false
    }

    /**
     * Finds the word boundary for cursor navigation.
     * @param forward If true, finds the end of the next word. If false, finds the start of the previous word.
     * @return The new cursor position.
     */
    private fun findWordBoundary(forward: Boolean): Int {
        return if (forward) {
            val nextSpace = text.indexOf(' ', cursorPos)
            if (nextSpace != -1) nextSpace else text.length
        } else {
            val prevSpace = text.lastIndexOf(' ', cursorPos - 1)
            if (prevSpace != -1) prevSpace + 1 else 0
        }
    }

    /**
     * Selects the entire text.
     */
    private fun selectAll() {
        selectionStart = 0
        cursorPos = text.length
    }

    /**
     * Selects the word at the current cursor position.
     */
    private fun selectWord() {
        val wordStart = text.lastIndexOf(' ', (cursorPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val wordEnd = text.indexOf(' ', cursorPos).let { if (it == -1) text.length else it }
        selectionStart = wordStart
        cursorPos = wordEnd
    }

    /**
     * Deletes the currently selected text.
     */
    private fun deleteSelection() {
        if (selectionStart != null && selectionStart != cursorPos) {
            val start = min(selectionStart!!, cursorPos).coerceIn(0, text.length)
            val end = max(selectionStart!!, cursorPos).coerceIn(0, text.length)
            if (start < end) {
                text = text.removeRange(start, end)
                cursorPos = start
                selectionStart = null
                onChange(text)
            }
        }
    }

    /**
     * Calculates the cursor position based on the X coordinate of the click.
     * @param relativeX The X position relative to the start of the text box.
     * @return The new cursor position.
     */
    private fun getCursorPosFromX(relativeX: Double): Int {
        var pos = 0
        for (i in text.indices) {
            val charWidth = mc.textRenderer.getWidth(text[i].toString())
            if (relativeX < mc.textRenderer.getWidth(text.substring(0, i + 1))) {
                pos = i
                break
            }
            pos = i + 1
        }
        return pos.coerceIn(0, text.length)
    }

    /**
     * Sets the text programmatically.
     * @param newText The new text to set.
     */
    fun setText(newText: String) {
        text = newText.take(maxLength)
        cursorPos = text.length
        selectionStart = null
        onChange(text)
    }

    /**
     * Retrieves the current text.
     * @return The current text.
     */
    fun getText(): String = text

    /**
     * Sets the focus state of the text box.
     * @param isFocused Whether the text box should be focused.
     */
    fun setFocus(isFocused: Boolean) {
        focused = isFocused
        if (!isFocused) {
            selectionStart = null
        }
    }

    /**
     * Retrieves whether the text box is currently focused.
     * @return True if focused, false otherwise.
     */
    fun isFocused(): Boolean = focused
}