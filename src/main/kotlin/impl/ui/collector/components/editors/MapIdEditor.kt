package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.component.type.MapIdComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton

class MapIdEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: MapIdComponent?
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:map_id"

    private var currentMapId: Int = initialValue?.id() ?: 0
    private var isEnabled: Boolean = initialValue != null

    private lateinit var mapIdSlider: CollectorSlider
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        mapIdSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 18,
            width = 130.0,
            label = "Map ID",
            value = currentMapId.toDouble(),
            min = 0.0,
            max = 99999.0,
            step = 1.0
        ) { newValue ->
            currentMapId = newValue.toInt()
            if (isEnabled) {
                notifyValueChanged(MapIdComponent(currentMapId))
            }
        }

        mapIdSlider.setLabelPosition(0.0, -12.0)
        editorComponents.add(mapIdSlider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable map ID override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                notifyValueChanged(MapIdComponent(currentMapId))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        if (isEnabled) {
            notifyValueChanged(MapIdComponent(currentMapId))
        } else {
            notifyValueChanged(null)
        }
    }
}