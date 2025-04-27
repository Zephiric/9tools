
package impl.ui.collector.components.editors
/*
import net.minecraft.component.type.DyedColorComponent
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import win.ninegang.ninetools.impl.ui.collector.utils.UIContainer
import impl.ui.collector.utils.UIDropdown
import java.awt.Color
import net.minecraft.util.DyeColor
import net.minecraft.util.math.ColorHelper.Argb
import impl.ui.collector.components.EditorState

class DyedColorEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: DyedColorComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:dyed_color"

    private val customHeight = 110.0
    private val defaultRgb = DyedColorComponent.DEFAULT_COLOR // -6265536 is the default
    private var currentRgb: Int = initialValue?.rgb() ?: defaultRgb
    private var showInTooltip: Boolean = initialValue?.showInTooltip() ?: true
    private var isEnabled: Boolean = initialValue != null
    private var colorMode: ColorMode = ColorMode.CUSTOM

    private lateinit var redSlider: CollectorSlider
    private lateinit var greenSlider: CollectorSlider
    private lateinit var blueSlider: CollectorSlider
    private var tooltipToggle: CollectorToggleButton
    private var enableToggle: CollectorToggleButton
    private var colorPreview: UIContainer
    private var colorDropdown: UIDropdown


    private enum class ColorMode {
        CUSTOM, PREDEFINED
    }

    override fun setPosition(newX: Double, newY: Double) {
        super.setPosition(newX, newY)

        if (!editorComponents.contains(redSlider)) {
            redSlider.setPosition(newX + 5.0, newY + 40)
            greenSlider.setPosition(newX + 5.0, newY + 70)
            blueSlider.setPosition(newX + 5.0, newY + 100)
        }
    }

    init {
        initializeDimensions(x, y)

        this.height = customHeight

        val color = Color(Argb.fullAlpha(currentRgb))
        val initialRed = color.red
        val initialGreen = color.green
        val initialBlue = color.blue

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
            x = x + 145,
            y = y + 5,
            width = 20.0,
            height = 20.0
        ).apply {
            backgroundColor = color
        }
        editorComponents.add(colorPreview)

        val colorOptions = mutableListOf<String>().apply {
            add("Custom")
            addAll(DyeColor.values().map { formatDyeColor(it) })
        }

        colorDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 70.0,
            label = "Dyed Color",
            onSelect = { selected ->
                if (selected == "Custom") {
                    colorMode = ColorMode.CUSTOM
                    if (!editorComponents.contains(redSlider)) {
                        editorComponents.add(redSlider)
                        editorComponents.add(greenSlider)
                        editorComponents.add(blueSlider)
                    }
                } else {
                    colorMode = ColorMode.PREDEFINED
                    val rawName = selected.replace("§[0-9a-fk-or]".toRegex(), "").uppercase()
                    val dyeColor = DyeColor.values().find { it.name == rawName }
                    dyeColor?.let {
                        val r = (it.colorComponents[0] * 255).toInt()
                        val g = (it.colorComponents[1] * 255).toInt()
                        val b = (it.colorComponents[2] * 255).toInt()
                        redSlider.value = r.toDouble()
                        greenSlider.value = g.toDouble()
                        blueSlider.value = b.toDouble()
                        updateColor(r, g, b)
                    }

                    editorComponents.remove(redSlider)
                    editorComponents.remove(greenSlider)
                    editorComponents.remove(blueSlider)
                }
            }
        ).apply {
            items = colorOptions
            currentSelection = "Custom"
        }
        editorComponents.add(colorDropdown)

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

        greenSlider = CollectorSlider(
            x = x + 5,
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

        blueSlider = CollectorSlider(
            x = x + 5,
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

        if (colorMode == ColorMode.CUSTOM) {
            editorComponents.add(redSlider)
            editorComponents.add(greenSlider)
            editorComponents.add(blueSlider)
        }

        tooltipToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 70.0,
            label = "Tooltip",
            description = "Show color in item tooltip",
            state = showInTooltip
        ) { show ->
            showInTooltip = show
            if (isEnabled) {
                notifyValueChanged(DyedColorComponent(currentRgb, showInTooltip))
            }
        }
        editorComponents.add(tooltipToggle)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 90.0,
            label = "Enable",
            description = "Enable/disable dyed color",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                notifyValueChanged(DyedColorComponent(currentRgb, showInTooltip))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    private fun formatDyeColor(color: DyeColor): String {
        val colorName = color.name.lowercase().replaceFirstChar { it.uppercase() }

        val formattingCode = when (color) {
            DyeColor.WHITE -> "§f"
            DyeColor.ORANGE -> "§6"
            DyeColor.MAGENTA -> "§d"
            DyeColor.LIGHT_BLUE -> "§b"
            DyeColor.YELLOW -> "§e"
            DyeColor.LIME -> "§a"
            DyeColor.PINK -> "§d"
            DyeColor.GRAY -> "§8"
            DyeColor.LIGHT_GRAY -> "§7"
            DyeColor.CYAN -> "§3"
            DyeColor.PURPLE -> "§5"
            DyeColor.BLUE -> "§9"
            DyeColor.BROWN -> "§6"
            DyeColor.GREEN -> "§2"
            DyeColor.RED -> "§c"
            DyeColor.BLACK -> "§0"
        }

        return "$formattingCode$colorName§r"
    }

    private fun updateColor(red: Int?, green: Int?, blue: Int?) {
        val currentColor = Color(Argb.fullAlpha(currentRgb))

        val newRed = red ?: currentColor.red
        val newGreen = green ?: currentColor.green
        val newBlue = blue ?: currentColor.blue

        val newColor = Color(newRed, newGreen, newBlue)
        currentRgb = newColor.rgb

        colorPreview.backgroundColor = newColor

        if (colorMode != ColorMode.PREDEFINED) {
            colorDropdown.currentSelection = "Custom"
        }

        if (isEnabled) {
            notifyValueChanged(DyedColorComponent(currentRgb, showInTooltip))
        }
    }
}*/
