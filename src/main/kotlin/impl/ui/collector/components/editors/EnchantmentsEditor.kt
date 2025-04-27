
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import win.ninegang.ninetools.api.util.entity.ItemComponentUtil
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown

class EnchantmentsEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: ItemEnchantmentsComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:enchantments"

    private var currentComponent: ItemEnchantmentsComponent = ItemEnchantmentsComponent.DEFAULT
    private var selectedEnchantment: RegistryEntry<Enchantment>? = null
    private var currentLevel = 0
    private var tooltipsEnabled = true

    private lateinit var tooltipToggle: CollectorToggleButton
    private lateinit var enchantmentDropdown: UIDropdown
    private lateinit var levelSlider: CollectorSlider

    private val availableEnchantments = mutableMapOf<String, RegistryEntry<Enchantment>>()
    private val enchantmentDisplayNames = mutableListOf<String>()

    init {
        initializeDimensions(x, y)

        if (initialComponent != null) {
            currentComponent = initialComponent
            val withoutTooltips = currentComponent.withShowInTooltip(false)
            tooltipsEnabled = (withoutTooltips != currentComponent)
        }
        prepareEnchantmentsList()

        tooltipToggle = CollectorToggleButton(
            x = x + 115.0,
            y = y + 30.0,
            label = "Tooltip",
            description = "Show enchantments in item tooltip",
            state = tooltipsEnabled
        ) { enabled ->
            tooltipsEnabled = enabled
            currentComponent = currentComponent.withShowInTooltip(enabled)
            notifyValueChanged(currentComponent)
        }
        editorComponents.add(tooltipToggle)

        enchantmentDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = width - 78,
            label = "Enchantment",
            onSelect = { selected ->
                val entryKey = availableEnchantments.keys.find { it == selected || it.startsWith("$selected ") }

                if (entryKey != null) {
                    selectedEnchantment = availableEnchantments[entryKey]
                    if (selectedEnchantment != null) {
                        val enchantment = selectedEnchantment!!.value()
                        val existingLevel = currentComponent.getLevel(enchantment)

                        currentLevel = existingLevel
                        levelSlider.value = currentLevel.toDouble()
                    }
                }
            }
        )
        enchantmentDropdown.items = enchantmentDisplayNames
        editorComponents.add(enchantmentDropdown)

        levelSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 30.0,
            width = width - 120.0,
            label = "Level",
            value = 0.0,
            min = 0.0,
            max = 100.0,
            step = 1.0
        ) { newValue ->
            currentLevel = newValue.toInt()
            selectedEnchantment?.let { enchantmentEntry ->
                val enchantment = enchantmentEntry.value()
                val builder = ItemEnchantmentsComponent.Builder(currentComponent)

                if (currentLevel <= 0) {
                    builder.set(enchantment, 0)
                } else {
                    builder.set(enchantment, currentLevel)
                }

                currentComponent = builder.build()

                if (tooltipsEnabled != isTooltipEnabled(currentComponent)) {
                    currentComponent = currentComponent.withShowInTooltip(tooltipsEnabled)
                }

                prepareEnchantmentsList()
                updateDropdown()

                notifyValueChanged(currentComponent)
            }
        }
        editorComponents.add(levelSlider)

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = currentComponent,
                isValid = true,
                columnSpan = 1,
            )
        )
    }

    private fun prepareEnchantmentsList() {
        availableEnchantments.clear()
        enchantmentDisplayNames.clear()

        Registries.ENCHANTMENT.forEach { enchantment ->
            val name = enchantment.getName(1).string.replace(" I$".toRegex(), "")

            val level = currentComponent.getLevel(enchantment)

            if (level > 0) {
                val levelDisplay = if (level <= 10) {
                    toRomanNumeral(level)
                } else {
                    level.toString()
                }

                val displayName = "$name $levelDisplay"
                enchantmentDisplayNames.add(displayName)
                availableEnchantments[displayName] = enchantment.registryEntry
            } else {
                enchantmentDisplayNames.add(name)
                availableEnchantments[name] = enchantment.registryEntry
            }
        }

        enchantmentDisplayNames.sort()
    }

    private fun updateDropdown() {
        if (::enchantmentDropdown.isInitialized) {
            enchantmentDropdown.items = enchantmentDisplayNames
        }
    }

    private fun toRomanNumeral(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> number.toString()
        }
    }

    private fun isTooltipEnabled(component: ItemEnchantmentsComponent): Boolean {
        val withTooltipsDisabled = component.withShowInTooltip(false)
        return withTooltipsDisabled != component
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val componentType = ItemComponentUtil.getComponentTypeByName(componentType)

        if (componentType != null) {
            val component = ItemComponentUtil.getComponent(stack, componentType)
                .orElse(null) as? ItemEnchantmentsComponent

            if (component != null) {
                currentComponent = component

                tooltipsEnabled = isTooltipEnabled(component)

                prepareEnchantmentsList()
                updateDropdown()

                tooltipToggle.state = tooltipsEnabled

                notifyValueChanged(component)
            }
        }
    }
}*/
