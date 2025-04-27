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
                    "SCROLLINGTEST323" -> 1.0
                    "TEST323232" -> 1.0
                    "TEST12131" -> 1.0
                    "TEST|" -> 1.0
                    "TEST234" -> 1.0
                    "TEST11" -> 1.0
                    "TEST22" -> 1.0
                    "TEST33" -> 1.0
                    "TEST44" -> 1.0
                    "TEST55" -> 1.0
                    "TEST66" -> 1.0
                    "TEST77" -> 1.0
                    "TEST88" -> 1.0
                    "TEST99" -> 1.0
                    "TEST00" -> 1.0
                    "TEST1263" -> 1.0
                    "TEST00000" -> 1.0
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
                "SCROLLINGTEST323",
                "TEST323232",
                "TEST12131",
                "TEST|",
                "TEST234",
                "TEST11",
                "TEST22",
                "TEST33",
                "TEST44",
                "TEST55",
                "TEST66",
                "TEST77",
                "TEST88",
                "TEST99",
                "TEST00",
                "TEST1263",
                "TEST00000",
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
            label = "Save Default Components",
            description = "Include default components in saved JSON even if unchanged (i suggest keeping this on)",
            state = SettingsManager.saveDefaultComponents,
            onChange = { enabled ->
                SettingsManager.saveDefaultComponents = enabled
                SettingsManager.saveSettings()
            }
        )

        components += CollectorButton(
            x = x,
            y = y + 150,
            width = width * 0.4,
            height = 20.0,
            text = "Open Test Screen",
            onClickAction = {
                mc.setScreen(TestScreen())
            },
            type = CollectorButton.ButtonType.HIGHLIGHT
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        components.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
        components[0].setPosition(x, y)
        components[1].setPosition(x, y + 50)
        components[2].setPosition(x, y + 100)
        components[3].setPosition(x, y + 150)
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return components.any { it.onScroll(mouseX, mouseY, amount) }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return components.any { it.onClick(mouseX, mouseY, button) }
    }
}