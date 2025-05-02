package impl.ui.collector.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import impl.ui.collector.utils.UIScaling
import impl.ui.collector.screens.views.AdvancedSettingsView
import impl.ui.collector.screens.views.DisplaySettingsView
import impl.ui.collector.screens.views.GeneralSettingsView
import impl.ui.collector.screens.views.PerformanceSettingsView
import impl.ui.collector.utils.CollectorButton

class SettingsScreen : BaseCollectorScreen(Text.literal("Settings")) {
    private var screenX = 100.0
    private var screenY = 100.0
    private var screenWidth = 600.0
    private var screenHeight = 300.0
    private var currentView: UIComponent? = null

    override fun getScreenX() = screenX
    override fun getScreenY() = screenY
    override fun getScreenWidth() = screenWidth
    override fun getScreenHeight() = screenHeight
    override fun getScreenIdentifier(): String = "settings_screen"

    inner class SettingsLayout {
        private val categories = listOf("General", "Display", "Performance", "Advanced")
        private val categoryButtons = categories.mapIndexed { index, category ->
            CollectorButton(
                x = screenX + 10.0 + index * 95,
                y = screenY + 30.0,
                width = 90.0,
                height = 20.0,
                text = category,
                onClickAction = { selectCategory(category) },
                type = CollectorButton.ButtonType.NEUTRAL
            )
        }

        val contentX get() = screenX + 10
        val contentY get() = screenY + 60
        val contentWidth get() = screenWidth - 20
        val contentHeight get() = screenHeight - 70

        fun updateButtonHighlights(selectedCategory: String) {
            categoryButtons.forEach { btn ->
                btn.type = if (btn.text == selectedCategory)
                    CollectorButton.ButtonType.HIGHLIGHT
                else
                    CollectorButton.ButtonType.NEUTRAL
            }
        }

        fun renderCommonElements(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderBackground(context, screenX, screenY, screenWidth, screenHeight)
            renderTitleBar(context, screenX, screenY, screenWidth, "Settings")
            categoryButtons.forEach { it.render(context, mouseX, mouseY, delta) }
        }

        fun handleCommonClicks(mouseX: Double, mouseY: Double, button: Int): Boolean {
            categoryButtons.forEach { btn ->
                if (btn.contains(mouseX, mouseY)) {
                    btn.onClick(mouseX, mouseY, button)
                    return true
                }
            }
            return false
        }

        fun updatePositions(newX: Double, newY: Double) {
            categoryButtons.forEachIndexed { index, button ->
                button.setPosition(
                    newX + 10.0 + index * 95,
                    newY + 30.0
                )
            }
        }
    }

    private object ViewRegistry {
        private val views = mutableMapOf<String, (SettingsLayout) -> UIComponent>()

        init {
            registerView("General") { layout ->
                GeneralSettingsView(
                    x = layout.contentX,
                    y = layout.contentY,
                    width = layout.contentWidth,
                    height = layout.contentHeight
                )
            }
            registerView("Display") { layout ->
                DisplaySettingsView(
                    x = layout.contentX,
                    y = layout.contentY,
                    width = layout.contentWidth,
                    height = layout.contentHeight
                )
            }


            registerView("Performance") { layout ->
                PerformanceSettingsView(
                    x = layout.contentX,
                    y = layout.contentY,
                    width = layout.contentWidth,
                    height = layout.contentHeight
                )
            }
            registerView("Advanced") { layout ->
                AdvancedSettingsView(
                    x = layout.contentX,
                    y = layout.contentY,
                    width = layout.contentWidth,
                    height = layout.contentHeight
                )
            }

        }

        fun registerView(category: String, creator: (SettingsLayout) -> UIComponent) {
            views[category] = creator
        }

        fun createView(category: String, layout: SettingsLayout): UIComponent? {
            return views[category]?.invoke(layout)
        }
    }

    private val layout = SettingsLayout()

    init {
        loadSavedPosition()
        selectCategory("General")
    }

    private fun selectCategory(category: String) {
        layout.updateButtonHighlights(category)
        currentView = ViewRegistry.createView(category, layout)
    }

    override fun updateScreenPosition(newX: Double, newY: Double) {
        screenX = newX
        screenY = newY
        layout.updatePositions(newX, newY)
        currentView?.setPosition(layout.contentX, layout.contentY)
    }

    override fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            layout.renderCommonElements(context, mouseX, mouseY, delta)
            currentView?.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)

        val scale = UIScaling.getScale()
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true
        }

        if (layout.handleCommonClicks(scaledMouseX, scaledMouseY, button)) {
            return true
        }

        if (currentView?.onClick(scaledMouseX, scaledMouseY, button) == true) {
            return true
        }

        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }
        return currentView?.onKeyPress(keyCode, scanCode, modifiers)
            ?: super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        val scale = UIScaling.getScale()
        if (currentView?.onCharTyped(chr, modifiers) == true) {
            return true
        }
        return super.charTyped(chr, modifiers)
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean {
        val scale = UIScaling.getScale()
        if (currentView?.onDrag(mouseX / scale, mouseY / scale, deltaX / scale, deltaY / scale, button) == true) {
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val scale = UIScaling.getScale()
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        if (currentView?.onScroll(scaledMouseX, scaledMouseY, verticalAmount) == true) {
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun close() {
        mc.setScreen(CollectorScreen())
    }
}