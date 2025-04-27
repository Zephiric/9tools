package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Unit
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class HideTooltipEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:hide_tooltip"

    private var isEnabled: Boolean = initialValue

    init {
        initializeDimensions(x, y)

        val hideTooltipToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Hide Tooltip",
            description = "Completely hides the item's tooltip",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (newValue) {
                notifyValueChanged(Unit.INSTANCE)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(hideTooltipToggle)
    }
}