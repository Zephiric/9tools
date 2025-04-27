package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.component.type.MapColorComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIContainer
import java.awt.Color

class MapColorEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: MapColorComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:map_color"
    private val customHeight = 110.0

    private val defaultColor = 4603950

    private var currentRgb: Int = initialValue?.rgb() ?: defaultColor
    private var isEnabled: Boolean = initialValue != null

    private var redSlider: CollectorSlider
    private var greenSlider: CollectorSlider
    private var blueSlider: CollectorSlider
    private var enableToggle: CollectorToggleButton
    private var colorPreview: UIContainer

    init {
        initializeDimensions(x, y)

        this.height = customHeight

        val initialColor = Color(currentRgb)
        val initialRed = initialColor.red
        val initialGreen = initialColor.green
        val initialBlue = initialColor.blue

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = null,
                isValid = true,
                customHeight = height
            )
        )

        colorPreview = UIContainer(
            x = x + width - 40,
            y = y + 10,
            width = 30.0,
            height = 30.0
        ).apply {
            backgroundColor = Color(currentRgb)
        }
        editorComponents.add(colorPreview)

        redSlider = CollectorSlider(
            x = x + 5,
            y = y + 25,
            width = 130.0,
            label = "Red",
            value = initialRed.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(newValue.toInt(), null, null)
        }
        editorComponents.add(redSlider)

        greenSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 40,
            width = 130.0,
            label = "Green",
            value = initialGreen.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(null, newValue.toInt(), null)
        }
        editorComponents.add(greenSlider)

        blueSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 55,
            width = 130.0,
            label = "Blue",
            value = initialBlue.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(null, null, newValue.toInt())
        }
        editorComponents.add(blueSlider)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 110.0,
            label = "Enable",
            description = "Enable/disable map color override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                notifyValueChanged(MapColorComponent(currentRgb))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    private fun updateColor(red: Int?, green: Int?, blue: Int?) {
        val currentColor = Color(currentRgb)

        val newRed = red ?: currentColor.red
        val newGreen = green ?: currentColor.green
        val newBlue = blue ?: currentColor.blue

        val newColor = Color(newRed, newGreen, newBlue)
        currentRgb = newColor.rgb

        colorPreview.backgroundColor = newColor

        if (isEnabled) {
            notifyValueChanged(MapColorComponent(currentRgb))
        }
    }
}