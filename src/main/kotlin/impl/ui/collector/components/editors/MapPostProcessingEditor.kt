package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.component.type.MapPostProcessingComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown

class MapPostProcessingEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: MapPostProcessingComponent?
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:map_post_processing"

    private var isEnabled: Boolean = initialValue != null
    private var currentProcessingType: MapPostProcessingComponent = initialValue ?: MapPostProcessingComponent.SCALE

    private lateinit var typeDropdown: UIDropdown
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        val processingOptions = listOf(
            "Scale (Zoom Out)",
            "Lock (Prevent Updates)"
        )

        typeDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 130.0,
            label = "Processing",
            onSelect = { selected ->
                currentProcessingType = when (selected) {
                    "Scale (Zoom Out)" -> MapPostProcessingComponent.SCALE
                    "Lock (Prevent Updates)" -> MapPostProcessingComponent.LOCK
                    else -> MapPostProcessingComponent.SCALE
                }

                if (isEnabled) {
                    notifyValueChanged(currentProcessingType)
                }
            }
        ).apply {
            items = processingOptions
            currentSelection = when (currentProcessingType) {
                MapPostProcessingComponent.SCALE -> "Scale (Zoom Out)"
                MapPostProcessingComponent.LOCK -> "Lock (Prevent Updates)"
            }
        }
        editorComponents.add(typeDropdown)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable map post processing",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                notifyValueChanged(currentProcessingType)
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        if (isEnabled) {
            notifyValueChanged(currentProcessingType)
        } else {
            notifyValueChanged(null)
        }
    }
}