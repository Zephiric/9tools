package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.component.type.UnbreakableComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class UnbreakableEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean,
    initialTooltip: Boolean = true
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:unbreakable"

    private var isEnabled: Boolean = initialValue
    private var showTooltip: Boolean = initialTooltip

    init {
        initializeDimensions(x, y)

        lateinit var unbreakableToggle: CollectorToggleButton
        lateinit var enableToggle: CollectorToggleButton

        unbreakableToggle = CollectorToggleButton(
            x = x + 5,
            y = y,
            label = "Unbreakable",
            description = "Makes the item immune to durability damage",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (enableToggle.state != newValue) {
                enableToggle.state = newValue
            }
            if (newValue) {
                notifyValueChanged(UnbreakableComponent(showTooltip))
            } else {
                notifyValueChanged(null)
            }
        }

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable unbreakable override",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (unbreakableToggle.state != newValue) {
                unbreakableToggle.state = newValue
            }
            if (newValue) {
                notifyValueChanged(UnbreakableComponent(showTooltip))
            } else {
                notifyValueChanged(null)
            }
        }

        val tooltipToggle = CollectorToggleButton(
            x = x + 5,
            y = y + 20,
            label = "Tooltip",
            description = "Shows 'Unbreakable' in item tooltip",
            state = showTooltip
        ) { newValue ->
            showTooltip = newValue
            if (isEnabled) {
                notifyValueChanged(UnbreakableComponent(showTooltip))
            }
        }

        editorComponents.add(unbreakableToggle)
        editorComponents.add(enableToggle)
        editorComponents.add(tooltipToggle)
    }
}