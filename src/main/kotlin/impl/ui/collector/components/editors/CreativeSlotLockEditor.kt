package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Unit
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class CreativeSlotLockEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:creative_slot_lock"

    private var isEnabled: Boolean = initialValue

    init {
        initializeDimensions(x, y)

        val creativeSlotLockToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Creative Slot Lock",
            description = "Does something and i wish i knew what",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (newValue) {
                notifyValueChanged(Unit.INSTANCE)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(creativeSlotLockToggle)
    }
}