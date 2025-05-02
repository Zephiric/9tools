package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color



class CollectorSearchBar(
    override var x: Double,
    override var y: Double,
    override var width: Double = 200.0,
    override var height: Double = 25.0,
    private val onSearch: (String) -> Unit,
    private val getSuggestions: (String) -> List<String>
) : UIComponent {

    private var text = ""
    private var focused = false
    private var cursorPos = 0
    private var showSuggestions = false

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.8f),
            x, y,
            x + width, y + height,
            3f, 10f
        )

        context.drawTextWithShadow(
            mc.textRenderer,
            "🔍",
            (x + 5).toInt(),
            (y + 8).toInt(),
            Color(0.7f, 0.7f, 0.7f, 0.9f).rgb
        )

        val displayText = if (text.isEmpty() && !focused) "Search..." else text
        val color = if (text.isEmpty() && !focused) {
            Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
        } else {
            Color(1f, 1f, 1f, 0.9f).rgb
        }
        context.drawTextWithShadow(
            mc.textRenderer,
            displayText,
            (x + 25).toInt(),
            (y + 8).toInt(),
            color
        )


        if (focused && (System.currentTimeMillis() % 1000 < 500)) {
            val cursorX = x + 25 + mc.textRenderer.getWidth(text.substring(0, cursorPos))
            win.ninegang.ninetools.compat.util.render.Engine2d.renderQuad(
                context.matrices,
                Color(1f, 1f, 1f, 0.8f),
                cursorX, y + 7,
                cursorX + 1, y + 18
            )
        }

        if (showSuggestions && text.isNotEmpty()) {
            val suggestions = getSuggestions(text).take(5)
            if (suggestions.isNotEmpty()) {
                renderSuggestions(context, suggestions, mouseX, mouseY)
            }
        }
    }

    private fun renderSuggestions(context: DrawContext, suggestions: List<String>, mouseX: Int, mouseY: Int) {
        val suggestionsHeight = suggestions.size * 20.0

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.15f, 0.15f, 0.15f, 0.95f),
            x, y + height + 5,
            x + width, y + height + 5 + suggestionsHeight,
            3f, 10f
        )

        suggestions.forEachIndexed { index, suggestion ->
            val sy = y + height + 5 + (index * 20)
            context.drawTextWithShadow(
                mc.textRenderer,
                suggestion,
                (x + 5).toInt(),
                (sy + 5).toInt(),
                Color.WHITE.rgb
            )
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        focused = contains(mouseX, mouseY)
        showSuggestions = focused
        return focused
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean = false

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!focused) return false

        when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPos > 0) {
                    text = text.removeRange(cursorPos - 1, cursorPos)
                    cursorPos--
                    onSearch(text)
                }
            }
            GLFW.GLFW_KEY_LEFT -> if (cursorPos > 0) cursorPos--
            GLFW.GLFW_KEY_RIGHT -> if (cursorPos < text.length) cursorPos++

            else -> return false
        }
        return true
    }

    /**
     * Accept typed characters
     */
    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        if (!focused) return false


        if (chr.isLetterOrDigit() || chr.isWhitespace()) {
            text = text.substring(0, cursorPos) + chr + text.substring(cursorPos)
            cursorPos++
            onSearch(text)
            return true
        }
        return false
    }

}
