package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton

class MaxStackSizeEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Int
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:max_stack_size"

    private var currentMaxStackSize: Int? = initialValue
    private var isEnabled: Boolean = false

    private lateinit var slider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        slider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 100.0,
            label = "Max Stack Size",
            value = initialValue.toDouble(),
            min = 1.0,
            max = 99.0,
            step = 1.0
        ) { newValue ->
            val defaultValue = item.baseItem.maxCount
            val newInt = newValue.toInt()

            if (newInt == defaultValue) {
                if (isEnabled) {
                    isEnabled = false
                    enableToggle.state = false
                    currentMaxStackSize = defaultValue
                    notifyValueChanged(null)
                }
            } else {
                if (!isEnabled) {
                    isEnabled = true
                    enableToggle.state = true
                }
                currentMaxStackSize = newInt
                notifyValueChanged(newInt)
            }
        }

        slider.setLabelPosition(0.0, -12.0)

        editorComponents.add(slider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable max stack size override",
            state = false
        ) { enabled ->
            isEnabled = enabled
            val defaultValue = item.baseItem.maxCount
            if (enabled) {
                if (currentMaxStackSize == null || currentMaxStackSize == defaultValue) {
                    currentMaxStackSize = slider.value.toInt()
                }
                slider.value = currentMaxStackSize!!.toDouble()
                notifyValueChanged(currentMaxStackSize)
            } else {
                slider.value = defaultValue.toDouble()
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)
        val defaultValue = collectorItem.baseItem.maxCount

        if (currentMaxStackSize == null || currentMaxStackSize == defaultValue) {
            currentMaxStackSize = defaultValue
            isEnabled = false
        } else {
            isEnabled = true
        }

        slider.value = if (isEnabled) currentMaxStackSize!!.toDouble() else defaultValue.toDouble()
        enableToggle.state = isEnabled

        if (isEnabled) {
            notifyValueChanged(currentMaxStackSize)
        } else {
            notifyValueChanged(null)
        }
    }
}