
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.potion.Potion
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import win.ninegang.ninetools.api.util.Wrapper.mc
import win.ninegang.ninetools.api.util.render.Engine2d
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import win.ninegang.ninetools.impl.ui.collector.utils.UIContainer
import impl.ui.collector.utils.UIDropdown
import java.awt.Color
import java.util.Optional

class PotionContentsEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:potion_contents"
    private val customHeight = 200.0
    private val customWidth = 410.0

    private var currentPotion: RegistryEntry<Potion>? = null
    private var isEnabled: Boolean = false
    private var hasCustomColor: Boolean = false
    private var currentColor: Int = 0x385DC6

    private val customEffects = mutableListOf<StatusEffectInstance>()
    private var selectedEffectIndex: Int? = null
    private var scrollOffset = 0
    private val maxVisibleEffects = 11

    private var selectedEffectType: StatusEffect? = null
    private var effectDuration: Int = 200
    private var effectAmplifier: Int = 0

    private lateinit var potionDropdown: UIDropdown
    private lateinit var enableToggle: CollectorToggleButton
    private lateinit var colorToggle: CollectorToggleButton
    private lateinit var colorPreview: UIContainer
    private lateinit var redSlider: CollectorSlider
    private lateinit var greenSlider: CollectorSlider
    private lateinit var blueSlider: CollectorSlider

    private lateinit var effectTypeDropdown: UIDropdown
    private lateinit var durationSlider: CollectorSlider
    private lateinit var amplifierSlider: CollectorSlider
    private lateinit var addEffectButton: CollectorButton
    private lateinit var deleteEffectButton: CollectorButton

    init {
        initializeDimensions(x, y)

        this.height = customHeight
        this.width = customWidth

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = null,
                isValid = true,
                columnSpan = 2,
                customHeight = height
            )
        )

        potionDropdown = UIDropdown(
            x = x + 5.0,
            y = y + 15.0,
            width = 120.0,
            height = 20.0,
            label = "Potion",
            maxVisibleItems = 10,
            onSelect = { selected ->
                if (selected == "None") {
                    currentPotion = null
                    updateComponent()
                } else {
                    val potions = Registries.POTION
                    potions.forEach { potion ->
                        val potionId = potions.getId(potion)
                        if (potionId != null) {
                            val formattedName = formatPotionName(potionId.path)
                            if (selected == formattedName) {
                                currentPotion = potions.getEntry(potion)
                                updateComponent()
                                return@forEach
                            }
                        }
                    }
                }
            }
        )

        val potionOptions = mutableListOf<String>().apply {
            add("None")

            Registries.POTION.forEach { potion ->
                val potionId = Registries.POTION.getId(potion)
                if (potionId != null) {
                    add(formatPotionName(potionId.path))
                }
            }
        }

        potionDropdown.items = potionOptions
        potionDropdown.currentSelection = "None"
        editorComponents.add(potionDropdown)

        colorPreview = UIContainer(
            x = x + 125.0,
            y = y + 110.0,
            width = 20.0,
            height = 20.0
        ).apply {
            backgroundColor = Color(currentColor)
        }
        editorComponents.add(colorPreview)

        colorToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 110.0,
            label = "Custom Color",
            description = "Override the potion's default color",
            state = hasCustomColor
        ) { enabled ->
            hasCustomColor = enabled
            updateComponent()
        }
        editorComponents.add(colorToggle)

        val initialColor = Color(currentColor)

        redSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 125.0,
            width = 160.0,
            label = "Red",
            value = initialColor.red.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(newValue.toInt(), null, null)
        }
        editorComponents.add(redSlider)

        greenSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 140.0,
            width = 160.0,
            label = "Green",
            value = initialColor.green.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(null, newValue.toInt(), null)
        }
        editorComponents.add(greenSlider)

        blueSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 155.0,
            width = 160.0,
            label = "Blue",
            value = initialColor.blue.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            updateColor(null, null, newValue.toInt())
        }
        editorComponents.add(blueSlider)

        initializeEffectsUI()

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + height - 20.0,
            label = "Enable",
            description = "Enable/disable potion contents",
            state = false
        ) { enabled ->
            isEnabled = enabled
            updateComponent()
        }
        editorComponents.add(enableToggle)
    }

    private fun initializeEffectsUI() {
        effectTypeDropdown = UIDropdown(
            x = x + 5.0,
            y = y + 50.0,
            width = 119.0,
            height = 20.0,
            label = "Effect",
            maxVisibleItems = 10,
            onSelect = { selected ->
                Registries.STATUS_EFFECT.forEach { effect ->
                    val effectId = Registries.STATUS_EFFECT.getId(effect)
                    if (effectId != null) {
                        val formattedName = formatEffectName(effectId.path)
                        if (selected == formattedName) {
                            selectedEffectType = effect
                            return@forEach
                        }
                    }
                }
            }
        )

        val effectOptions = mutableListOf<String>().apply {
            Registries.STATUS_EFFECT.forEach { effect ->
                val effectId = Registries.STATUS_EFFECT.getId(effect)
                if (effectId != null) {
                    add(formatEffectName(effectId.path))
                }
            }
        }.sorted()

        effectTypeDropdown.items = effectOptions
        if (effectOptions.isNotEmpty()) {
            effectTypeDropdown.currentSelection = effectOptions.first()
            Registries.STATUS_EFFECT.forEach { effect ->
                val effectId = Registries.STATUS_EFFECT.getId(effect)
                if (effectId != null && formatEffectName(effectId.path) == effectOptions.first()) {
                    selectedEffectType = effect
                }
            }
        }
        editorComponents.add(effectTypeDropdown)

        durationSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 70.0,
            width = 160.0,
            label = "Duration",
            value = effectDuration / 20.0,
            min = 1.0,
            max = 257.0,
            step = 1.0
        ) { newValue ->
            effectDuration = (newValue * 20).toInt()
        }
        editorComponents.add(durationSlider)

        amplifierSlider = CollectorSlider(
            x = x + 5.0,
            y = y + 85.0,
            width = 160.0,
            label = "Level",
            value = effectAmplifier.toDouble(),
            min = 0.0,
            max = 255.0,
            step = 1.0
        ) { newValue ->
            effectAmplifier = newValue.toInt()
        }
        editorComponents.add(amplifierSlider)

        addEffectButton = CollectorButton(
            x = x + 170.0,
            y = y + 50.0,
            width = 30.0,
            height = 20.0,
            text = "Add",
            onClickAction = { addEffect() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        editorComponents.add(addEffectButton)

        deleteEffectButton = CollectorButton(
            x = x + 315.0,
            y = y + 183.0,
            width = 90.0,
            height = 15.0,
            text = "Delete Selected",
            onClickAction = { deleteSelectedEffect() },
            type = CollectorButton.ButtonType.NEGATIVE
        )
        editorComponents.add(deleteEffectButton)
    }

    private fun formatPotionName(name: String): String {
        return name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    private fun formatEffectName(name: String): String {
        return name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    private fun updateColor(red: Int?, green: Int?, blue: Int?) {
        val currentColorObj = Color(currentColor)

        val newRed = red ?: currentColorObj.red
        val newGreen = green ?: currentColorObj.green
        val newBlue = blue ?: currentColorObj.blue

        val newColor = Color(newRed, newGreen, newBlue)
        currentColor = newColor.rgb

        colorPreview.backgroundColor = newColor

        updateComponent()
    }

    private fun addEffect() {
        selectedEffectType?.let { effectType ->

            val effectEntry = Registries.STATUS_EFFECT.getEntry(effectType)

            val newEffect = StatusEffectInstance(
                effectEntry,
                effectDuration,
                effectAmplifier
            )

            customEffects.add(newEffect)
            updateComponent()
        }
    }

    private fun deleteSelectedEffect() {
        selectedEffectIndex?.let { index ->
            if (index >= 0 && index < customEffects.size) {
                customEffects.removeAt(index)
                selectedEffectIndex = null
                updateComponent()

                if (scrollOffset > 0 && customEffects.size <= maxVisibleEffects) {
                    scrollOffset = 0
                } else if (scrollOffset > 0 && scrollOffset >= customEffects.size - maxVisibleEffects) {
                    scrollOffset = (customEffects.size - maxVisibleEffects).coerceAtLeast(0)
                }
            }
        }
    }

    private fun updateComponent() {
        if (isEnabled) {
            val potionOpt = Optional.ofNullable(currentPotion)
            val customColorOpt = if (hasCustomColor) Optional.of(currentColor) else Optional.empty()

            val component = PotionContentsComponent(
                potionOpt,
                customColorOpt,
                customEffects
            )

            notifyValueChanged(component)
        } else {
            notifyValueChanged(null)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            x, y,
            x + width, y + height,
            2f, 10f
        )

        editorComponents.forEach { it.render(context, mouseX, mouseY, delta) }

        context.drawTextWithShadow(
            mc.textRenderer,
            Text.literal("Base Potion"),
            (x + 5).toInt(),
            (y + 5).toInt(),
            0xFFFFFF
        )

        context.drawTextWithShadow(
            mc.textRenderer,
            Text.literal("Custom Effects"),
            (x + 5).toInt(),
            (y + 40).toInt(),
            0xFFFFFF
        )

        Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.4f),
            x + width - 200, y + 5,
            x + width - 5, y + 180,
            2f, 10f
        )

        val visibleEffects = customEffects.size.coerceAtMost(maxVisibleEffects)
        if (customEffects.isEmpty()) {
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("No custom effects added"),
                (x + width - 195).toInt(),
                (y + 10).toInt(),
                Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
            )
        } else {
            for (i in 0 until visibleEffects) {
                val index = i + scrollOffset
                if (index < customEffects.size) {
                    val effect = customEffects[index]
                    val effectEntryType = effect.effectType
                    val effectType = effectEntryType.value()

                    val effectId = Registries.STATUS_EFFECT.getId(effectType)
                    val effectName = effectId?.path ?: "unknown"
                    val formattedName = formatEffectName(effectName)

                    val amplifierText = when (effect.amplifier) {
                        0 -> "I"
                        1 -> "II"
                        2 -> "III"
                        3 -> "IV"
                        4 -> "V"
                        else -> (effect.amplifier + 1).toString()
                    }

                    val seconds = effect.duration / 20
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    val durationText = "$minutes:${remainingSeconds.toString().padStart(2, '0')}"

                    val yPos = y + 5 + (i * 16) + 4

                    if (selectedEffectIndex == index) {
                        Engine2d.renderRoundedQuad(
                            context.matrices,
                            Color(0.3f, 0.3f, 0.5f, 0.5f),
                            x + width - 200, yPos - 2,
                            x + width - 6, yPos + 14,
                            1f, 5f
                        )
                    }

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("$formattedName $amplifierText"),
                        (x + width - 195).toInt(),
                        yPos.toInt(),
                        getEffectColor(effectType)
                    )

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal(durationText),
                        (x + width - 50).toInt(),
                        yPos.toInt(),
                        0xCCCCCC
                    )
                }
            }

            if (customEffects.size > maxVisibleEffects) {
                if (scrollOffset > 0) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▲"),
                        (x + width / 1.5).toInt(),
                        (y + 5).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }

                if (scrollOffset + maxVisibleEffects < customEffects.size) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▼"),
                        (x + width / 1.5).toInt(),
                        (y + 170).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }
            }
        }
    }

    private fun getEffectColor(effect: StatusEffect): Int {
        return when {
            effect.isBeneficial -> 0x7FB8A4
            effect.isInstant -> 0xE49A9A
            else -> 0xFFFFFF
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x + width - 200 && mouseX <= x + width - 5 &&
            mouseY >= y + 5 && mouseY <= y + 180) {

            val relativeY = mouseY - (y + 5)
            val effectIndex = (relativeY / 16).toInt() + scrollOffset

            if (effectIndex >= 0 && effectIndex < customEffects.size) {
                selectedEffectIndex = effectIndex
                return true
            } else {
                selectedEffectIndex = null
            }
        } else if (!contains(mouseX, mouseY)) {
            selectedEffectIndex = null
        }

        return super.onClick(mouseX, mouseY, button)
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (mouseX >= x + width - 200 && mouseX <= x + width - 5 &&
            mouseY >= y + 5 && mouseY <= y + 180) {

            if (amount < 0 && scrollOffset + maxVisibleEffects < customEffects.size) {
                scrollOffset++
                return true
            }

            if (amount > 0 && scrollOffset > 0) {
                scrollOffset--
                return true
            }
        }

        return super.onScroll(mouseX, mouseY, amount)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val componentOpt = stack.get(DataComponentTypes.POTION_CONTENTS)

        if (componentOpt != null) {
            isEnabled = true

            val potionOpt = componentOpt.potion()
            if (potionOpt.isPresent) {
                val potionEntry = potionOpt.get()
                currentPotion = potionEntry

                val potionValue = potionEntry.value()
                val potionId = Registries.POTION.getId(potionValue)
                if (potionId != null) {
                    potionDropdown.currentSelection = formatPotionName(potionId.path)
                } else {
                    potionDropdown.currentSelection = "None"
                }
            } else {
                currentPotion = null
                potionDropdown.currentSelection = "None"
            }

            val colorOpt = componentOpt.customColor()
            if (colorOpt.isPresent) {
                hasCustomColor = true
                currentColor = colorOpt.get()

                val color = Color(currentColor)
                redSlider.value = color.red.toDouble()
                greenSlider.value = color.green.toDouble()
                blueSlider.value = color.blue.toDouble()
                colorPreview.backgroundColor = color
            } else {
                hasCustomColor = false
                colorToggle.state = false
            }

            customEffects.clear()
            customEffects.addAll(componentOpt.customEffects())
            selectedEffectIndex = null
            scrollOffset = 0

        } else {
            isEnabled = false
            hasCustomColor = false
            currentPotion = null
            customEffects.clear()
            selectedEffectIndex = null
            scrollOffset = 0
            potionDropdown.currentSelection = "None"
        }

        enableToggle.state = isEnabled
        colorToggle.state = hasCustomColor
        colorPreview.backgroundColor = Color(currentColor)

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) createPotionComponent() else null,
                isValid = true,
                columnSpan = 2,
                customHeight = height
            )
        )
    }

    private fun createPotionComponent(): PotionContentsComponent {
        val potionOpt = Optional.ofNullable(currentPotion)
        val customColorOpt = if (hasCustomColor) Optional.of(currentColor) else Optional.empty()

        return PotionContentsComponent(
            potionOpt,
            customColorOpt,
            customEffects
        )
    }
}*/
