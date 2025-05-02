package impl.ui.collector.screens.views

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import impl.ui.collector.SettingsManager
import impl.ui.collector.utils.UIScaling
import impl.ui.collector.screens.TestScreen
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown

class GeneralSettingsView(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double
) : UIComponent {
    private val components = mutableListOf<UIComponent>()

    override fun getChildComponents(): List<UIComponent> = components

    init {
        components += UIDropdown(
            x = x,
            y = y,
            width = width * 0.4,
            label = "UI Scale",
            onSelect = { selected ->
                val scale = when (selected) {
                    "Small (0.5x)" -> 0.5
                    "Medium (0.75x)" -> 0.75
                    "Default (1.0x)" -> 1.0
                    "Large (1.25x)" -> 1.25
                    "Extra Large (1.5x)" -> 1.5
                    "Huge (2.0x)" -> 2.0
                    else -> 1.0
                }
                SettingsManager.uiScale = scale
                UIScaling.updateScale()
                SettingsManager.saveSettings()
            }
        ).apply {
            items = listOf(
                "Small (0.5x)",
                "Medium (0.75x)",
                "Default (1.0x)",
                "Large (1.25x)",
                "Extra Large (1.5x)",
                "Huge (2.0x)",
            )
        }

        components += CollectorToggleButton(
            x = x,
            y = y + 50,
            width = 30.0,
            label = "Show Tooltips",
            description = "Show detailed item tooltips",
            state = SettingsManager.showDetailedTooltips,
            onChange = { enabled ->
                SettingsManager.showDetailedTooltips = enabled
                SettingsManager.saveSettings()
            }
        )

        components += CollectorToggleButton(
            x = x,
            y = y + 100,
            width = 30.0,
            label = "Save Default",
            description = "Include default components in saved JSON even if unchanged (i suggest keeping this on)",
            state = SettingsManager.saveDefaultComponents,
            onChange = { enabled ->
                SettingsManager.saveDefaultComponents = enabled
                SettingsManager.saveSettings()
            }
        )
        
        components.getOrNull(1)?.setPosition(x, y + 50)
        components.getOrNull(2)?.setPosition(x, y + 100)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
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

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return components.any { it.onScroll(mouseX, mouseY, amount) }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return components.any { it.onClick(mouseX, mouseY, button) }
    }
}