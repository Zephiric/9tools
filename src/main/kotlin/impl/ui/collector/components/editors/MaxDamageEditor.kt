package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import net.minecraft.component.DataComponentTypes

class MaxDamageEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Int? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:max_damage"

    private var currentMaxDamage: Int? = initialValue
    private var isEnabled: Boolean = initialValue != null

    private lateinit var slider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton

    private var unsubscribe: (() -> Unit)? = null

    init {
        initializeDimensions(x, y)

        slider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 100.0,
            label = "Max Durability",
            value = initialValue?.toDouble() ?: 100.0,
            min = 1.0,
            max = 4000.0,
            step = 1.0
        ) { newValue ->
            val newInt = newValue.toInt()
            if (!isEnabled) {
                isEnabled = true
                enableToggle.state = true
            }

            currentMaxDamage = newInt
            notifyValueChanged(newInt)

            registry.updateState("minecraft:damage", EditorState(
                componentType = "minecraft:damage",
                value = registry.getEditorState("minecraft:damage")?.value,
                isValid = true
            )
            )
        }

        slider.setLabelPosition(0.0, -12.0)
        editorComponents.add(slider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable max durability override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                if (currentMaxDamage == null) {
                    currentMaxDamage = 100
                }
                slider.value = currentMaxDamage!!.toDouble()
                notifyValueChanged(currentMaxDamage)
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)

        unsubscribe = registry.subscribeToChanges("minecraft:damage") { state ->
        }
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val currentMaxDamage = if (stack.contains(DataComponentTypes.MAX_DAMAGE)) {
            stack.getMaxDamage()
        } else {
            100
        }

        slider.value = currentMaxDamage.toDouble()
        slider.min = 1.0
        slider.max = 4000.0

        if (isEnabled) {
            notifyValueChanged(currentMaxDamage)
        } else {
            notifyValueChanged(null)
        }
    }
}