package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.DyeColor
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.UIDropdown
import impl.ui.collector.utils.CollectorToggleButton

class BaseColorEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: DyeColor? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:base_color"

    private var currentColor: DyeColor? = initialValue
    private var isEnabled: Boolean = initialValue != null
    private val colorDropdown: UIDropdown
    private val enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        val initialFormattedValue = if (isEnabled) formatColor(currentColor) else "None"
        val colorOptions = mutableListOf<String>().apply {
            add("None")
            addAll(DyeColor.values().map { formatColor(it) })
        }

        colorDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 120.0,
            label = "Color",
            onSelect = { selected ->
                if (selected == "None") {
                    currentColor = null
                    if (isEnabled) {
                        notifyValueChanged(null)
                    }
                } else {
                    val rawName = selected.replace("§[0-9a-fk-or]".toRegex(), "").uppercase()
                    val newColor = DyeColor.values().find { it.name == rawName }
                    newColor?.let {
                        currentColor = it
                        if (isEnabled) {
                            notifyValueChanged(it)
                        }
                    }
                }
            }
        ).apply {
            items = colorOptions
            currentSelection = initialFormattedValue
        }
        editorComponents.add(colorDropdown)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable color override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                if (currentColor == null) {
                    currentColor = DyeColor.WHITE
                }
                colorDropdown.currentSelection = formatColor(currentColor)
                notifyValueChanged(currentColor)
            } else {
                colorDropdown.currentSelection = "None"
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    private fun formatColor(color: DyeColor?): String {
        if (color == null) return "None"

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
}