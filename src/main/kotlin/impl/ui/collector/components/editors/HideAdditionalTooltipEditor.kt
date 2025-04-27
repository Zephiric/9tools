package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Unit
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class HideAdditionalTooltipEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:hide_additional_tooltip"

    private var isEnabled: Boolean = initialValue

    init {
        initializeDimensions(x, y)

        val hideAdditionalTooltipToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Hide Additional Tooltip",
            description = "Hides additional information in the tooltip like durability, enchantments, etc.",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (newValue) {
                notifyValueChanged(Unit.INSTANCE)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(hideAdditionalTooltipToggle)
    }
}