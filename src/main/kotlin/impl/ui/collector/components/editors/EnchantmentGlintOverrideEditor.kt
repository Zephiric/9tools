package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class EnchantmentGlintOverrideEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:enchantment_glint_override"

    private var glintValue: Boolean = initialValue
    private var isOverrideEnabled: Boolean = true

    init {
        initializeDimensions(x, y)

        val glintToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Enchantment Glint",
            description = "Gives the item an enchantment glint",
            state = initialValue
        ) { newValue ->
            glintValue = newValue
            if (isOverrideEnabled) {
                notifyValueChanged(newValue)
            }
        }

        val enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable enchantment glint override",
            state = true
        ) { enabled ->
            isOverrideEnabled = enabled
            if (enabled) {
                notifyValueChanged(glintValue)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(glintToggle)
        editorComponents.add(enableToggle)
    }
}