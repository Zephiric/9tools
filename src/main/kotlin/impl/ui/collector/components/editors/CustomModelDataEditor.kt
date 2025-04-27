
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.component.type.CustomModelDataComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton

// Would benefit from a more precise numerical input method. Middle click the slider value to manually change them is maybe smart

class CustomModelDataEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: CustomModelDataComponent?
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:custom_model_data"

    private var currentValue: Int = initialValue?.value() ?: 0
    private var isEnabled: Boolean = initialValue != null

    private lateinit var slider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        slider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 100.0,
            label = "Custom Model Data",
            value = currentValue.toDouble(),
            min = 1.0,
            max = 999.0,
            step = 1.0
        ) { newValue ->
            val newInt = newValue.toInt()
            currentValue = newInt

            if (isEnabled) {
                notifyValueChanged(CustomModelDataComponent(newInt))
            }
        }

        slider.setLabelPosition(0.0, -12.0)
        editorComponents.add(slider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable custom model data",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled

            if (enabled) {
                notifyValueChanged(CustomModelDataComponent(currentValue))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        slider.value = currentValue.toDouble()
        enableToggle.state = isEnabled

        if (isEnabled) {
            notifyValueChanged(CustomModelDataComponent(currentValue))
        } else {
            notifyValueChanged(null)
        }
    }
}*/
