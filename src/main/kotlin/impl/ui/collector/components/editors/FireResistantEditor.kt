package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Unit
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class FireResistantEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:fire_resistant"

    private var isEnabled: Boolean = initialValue

    init {
        initializeDimensions(x, y)

        val fireResistantToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Fire Resistant",
            description = "Makes the item immune to fire and lava damage",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (newValue) {
                notifyValueChanged(Unit.INSTANCE)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(fireResistantToggle)
    }
}