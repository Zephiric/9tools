package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import net.minecraft.component.DataComponentTypes
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil

class RepairCostEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Int? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:repair_cost"

    private var currentRepairCost: Int? = initialValue
    private var isEnabled: Boolean = initialValue != null

    private lateinit var slider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        val startValue = initialValue?.toDouble() ?: 15.0

        slider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 100.0,
            label = "Repair Cost",
            value = startValue,
            min = 0.0,
            max = 39.0,
            step = 1.0
        ) { newValue ->
            val newInt = newValue.toInt()
            if (!isEnabled) {
                isEnabled = true
                enableToggle.state = true
            }

            currentRepairCost = newInt
            notifyValueChanged(newInt)
        }

        slider.setLabelPosition(0.0, -12.0)
        editorComponents.add(slider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable repair cost override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                if (currentRepairCost == null) {
                    currentRepairCost = 15
                }
                slider.value = currentRepairCost!!.toDouble()
                notifyValueChanged(currentRepairCost)
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)

        val repairCost = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(stack, DataComponentTypes.REPAIR_COST)
            .map { it as Int }
            .orElse(15)

        slider.value = repairCost.toDouble()

        if (isEnabled) {
            notifyValueChanged(repairCost)
        } else {
            notifyValueChanged(null)
        }
    }
}