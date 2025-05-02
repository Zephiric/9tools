package impl.ui.collector.screens.views

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent

class PerformanceSettingsView(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double
) : UIComponent {
    private val components = mutableListOf<UIComponent>()

    override fun getChildComponents(): List<UIComponent> = components

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawTextWithShadow(
            mc.textRenderer,
            "Performance Settings",
            (x + 2).toInt(),
            (y + 8).toInt(),
            0xFFFFFF
        )
        components.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        components.forEach { component ->
            component.setPosition(component.x + deltaX, component.y + deltaY)
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return components.any { it.onClick(mouseX, mouseY, button) }
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean {
        return components.any { it.onDrag(mouseX, mouseY, deltaX, deltaY, button) }
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return components.any { it.onScroll(mouseX, mouseY, amount) }
    }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return components.any { it.onKeyPress(keyCode, scanCode, modifiers) }
    }
}