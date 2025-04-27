package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import net.minecraft.component.DataComponentTypes

class DamageEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Int? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:damage"

    private var currentDamage: Int? = initialValue
    private var isEnabled: Boolean = initialValue != null

    private lateinit var slider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton
    
    private var unsubscribe: (() -> Unit)? = null

    init {
        initializeDimensions(x, y)

        val maxDamageState = registry.getEditorState("minecraft:max_damage")
        val maxValue = (maxDamageState?.value as? Int) ?: 100

        slider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 100.0,
            label = "Current Damage",
            value = initialValue?.toDouble() ?: 0.0,
            min = 0.0,
            max = maxValue.toDouble(),
            step = 1.0
        ) { newValue ->
            val newInt = newValue.toInt()
            if (!isEnabled) {
                isEnabled = true
                enableToggle.state = true
            }

            currentDamage = newInt
            notifyValueChanged(newInt)
        }

        slider.setLabelPosition(0.0, -12.0)
        editorComponents.add(slider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable damage override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                if (currentDamage == null) {
                    currentDamage = 0
                }
                slider.value = currentDamage!!.toDouble()
                notifyValueChanged(currentDamage)
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)

        unsubscribe = registry.subscribeToChanges("minecraft:max_damage") { state ->
            if (state.value is Int) {
                val newMax = state.value
                slider.max = newMax.toDouble()

                if (currentDamage != null && currentDamage!! > newMax) {
                    currentDamage = newMax
                    slider.value = newMax.toDouble()
                    if (isEnabled) {
                        notifyValueChanged(newMax)
                    }
                }
            }             else if (state.value == null) {
                slider.max = 4000.0
            }
        }
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)

        val maxDamage = if (stack.contains(DataComponentTypes.MAX_DAMAGE)) {
            stack.getMaxDamage()
        } else {
            val maxDamageState = registry.getEditorState("minecraft:max_damage")
            if (maxDamageState?.value is Int) maxDamageState.value else 100
        }

        val damage = if (stack.contains(DataComponentTypes.DAMAGE)) {
            stack.getDamage()
        } else {
            0
        }

        slider.max = maxDamage.toDouble()
        slider.value = damage.toDouble()
        currentDamage = damage

        if (isEnabled) {
            notifyValueChanged(damage)
        } else {
            notifyValueChanged(null)
        }
    }

}