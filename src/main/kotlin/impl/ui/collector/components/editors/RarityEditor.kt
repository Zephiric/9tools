package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Rarity
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.UIDropdown
import impl.ui.collector.utils.CollectorToggleButton

class RarityEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Rarity? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:rarity"

    private var currentRarity: Rarity? = initialValue
    private var isEnabled: Boolean = initialValue != null
    private val rarityDropdown: UIDropdown
    private val enableToggle: CollectorToggleButton

    private fun formatRarity(rarity: Rarity?): String {
        return rarity?.let {
            "${it.formatting}${it.name.lowercase().replaceFirstChar { ch -> ch.uppercase() }}§r"
        } ?: "None"
    }

    init {
        initializeDimensions(x, y)

        val initialFormattedValue = if (isEnabled) formatRarity(currentRarity) else "None"
        val rarityOptions = mutableListOf<String>().apply {
            add("None")
            addAll(Rarity.values().map { formatRarity(it) })
        }

        rarityDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 120.0,
            label = "Rarity",
            onSelect = { selected ->
                if (selected == "None") {
                    currentRarity = null
                    if (isEnabled) {
                        notifyValueChanged(null)
                    }
                } else {
                    val rawName = selected.replace("§[0-9a-fk-or]".toRegex(), "").uppercase()
                    val newRarity = Rarity.values().find { it.name == rawName }
                    newRarity?.let {
                        currentRarity = it
                        if (isEnabled) {
                            notifyValueChanged(it)
                        }
                    }
                }
            }
        ).apply {
            items = rarityOptions
            currentSelection = initialFormattedValue
        }
        editorComponents.add(rarityDropdown)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable rarity override",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                if (currentRarity == null) {
                    currentRarity = Rarity.COMMON
                }
                rarityDropdown.currentSelection = formatRarity(currentRarity)
                notifyValueChanged(currentRarity)
            } else {
                rarityDropdown.currentSelection = "None"
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }
}