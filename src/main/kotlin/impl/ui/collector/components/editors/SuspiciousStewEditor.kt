package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.SuspiciousStewEffectsComponent
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown
import java.awt.Color

class SuspiciousStewEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: SuspiciousStewEffectsComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:suspicious_stew_effects"

    private val customHeight = 200.0

    private var isEnabled: Boolean = initialComponent != null
    private val stewEffects = mutableListOf<SuspiciousStewEffectsComponent.StewEffect>()

    private var selectedEffectIndex: Int? = null
    private var scrollOffset = 0
    private val maxVisibleEffects = 7

    private var selectedEffectType: StatusEffect? = null
    private var effectDuration: Int = 160

    private lateinit var effectTypeDropdown: UIDropdown
    private lateinit var durationSlider: CollectorSlider
    private lateinit var addEffectButton: CollectorButton
    private lateinit var deleteEffectButton: CollectorButton
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)
        this.height = customHeight

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = initialComponent,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )

        initializeEffectsUI()

        if (initialComponent != null) {
            stewEffects.clear()
            stewEffects.addAll(initialComponent.effects())
        }

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + height - 25.0,
            label = "Enable",
            description = "Enable/disable suspicious stew effects",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            updateComponent()
        }
        editorComponents.add(enableToggle)
    }

    private fun initializeEffectsUI() {
        effectTypeDropdown = UIDropdown(
            x = x + 5.0,
            y = y + 20.0,
            width = 110.0,
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
            y = y + 55.0,
            width = 130.0,
            label = "Duration",
            value = effectDuration / 20.0,
            min = 1.0,
            max = 257.0,
            step = 1.0
        ) { newValue ->
            effectDuration = (newValue * 20).toInt()
        }
        editorComponents.add(durationSlider)

        addEffectButton = CollectorButton(
            x = x + 160.0,
            y = y + 20.0,
            width = 30.0,
            height = 20.0,
            text = "Add",
            onClickAction = { addEffect() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        editorComponents.add(addEffectButton)

        deleteEffectButton = CollectorButton(
            x = x + 160.0,
            y = y + 175.0,
            width = 30.0,
            height = 20.0,
            text = "Del",
            onClickAction = { deleteSelectedEffect() },
            type = CollectorButton.ButtonType.NEGATIVE
        )
        editorComponents.add(deleteEffectButton)
    }

    private fun formatEffectName(name: String): String {
        return name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    private fun addEffect() {
        selectedEffectType?.let { effectType ->
            val effectEntry = Registries.STATUS_EFFECT.getEntry(effectType)
            val newEffect = SuspiciousStewEffectsComponent.StewEffect(
                effectEntry,
                effectDuration
            )

            stewEffects.add(newEffect)
            updateComponent()
        }
    }

    private fun deleteSelectedEffect() {
        selectedEffectIndex?.let { index ->
            if (index >= 0 && index < stewEffects.size) {
                stewEffects.removeAt(index)
                selectedEffectIndex = null
                updateComponent()

                if (scrollOffset > 0 && stewEffects.size <= maxVisibleEffects) {
                    scrollOffset = 0
                } else if (scrollOffset > 0 && scrollOffset >= stewEffects.size - maxVisibleEffects) {
                    scrollOffset = (stewEffects.size - maxVisibleEffects).coerceAtLeast(0)
                }
            }
        }
    }

    private fun updateComponent() {
        if (isEnabled && stewEffects.isNotEmpty()) {
            val component = SuspiciousStewEffectsComponent(stewEffects.toList())
            notifyValueChanged(component)
        } else {
            notifyValueChanged(null)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            x, y,
            x + width, y + height,
            2f, 10f
        )

        context.drawTextWithShadow(
            mc.textRenderer,
            Text.literal("Suspicious Stew Effects"),
            (x + 5).toInt(),
            (y + 5).toInt(),
            0xFFFFFF
        )

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.4f),
            x + 5, y + 85,
            x + width - 5, y + height - 30,
            2f, 10f
        )

        val visibleEffects = stewEffects.size.coerceAtMost(maxVisibleEffects)
        if (stewEffects.isEmpty()) {
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("No effects added"),
                (x + 10).toInt(),
                (y + 95).toInt(),
                Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
            )
        } else {
            for (i in 0 until visibleEffects) {
                val index = i + scrollOffset
                if (index < stewEffects.size) {
                    val effect = stewEffects[index]
                    val effectEntry = effect.effect()
                    val effectType = effectEntry.value()

                    val effectId = Registries.STATUS_EFFECT.getId(effectType)
                    val effectName = effectId?.path ?: "unknown"
                    val formattedName = formatEffectName(effectName)

                    val seconds = effect.duration() / 20
                    val durationText = "${seconds}s"

                    val yPos = y + 95 + (i * 16)

                    if (selectedEffectIndex == index) {
                        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                            context.matrices,
                            Color(0.3f, 0.3f, 0.5f, 0.5f),
                            x + 6, yPos - 2,
                            x + width - 6, yPos + 14,
                            1f, 5f
                        )
                    }

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal(formattedName),
                        (x + 10).toInt(),
                        yPos.toInt(),
                        getEffectColor(effectType)
                    )

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal(durationText),
                        (x + width - 40).toInt(),
                        yPos.toInt(),
                        0xCCCCCC
                    )
                }
            }

            if (stewEffects.size > maxVisibleEffects) {
                if (scrollOffset > 0) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▲"),
                        (x + width / 2).toInt(),
                        (y + 85).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }

                if (scrollOffset + maxVisibleEffects < stewEffects.size) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▼"),
                        (x + width / 2).toInt(),
                        (y + height - 40).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }
            }
        }

        editorComponents.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    private fun getEffectColor(effect: StatusEffect): Int {
        return when {
            effect.isBeneficial -> 0x7FB8A4
            effect.isInstant -> 0xE49A9A
            else -> 0xFFFFFF
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 85 && mouseY <= y + height - 30) {

            val relativeY = mouseY - (y + 95)
            val effectIndex = (relativeY / 16).toInt() + scrollOffset

            if (effectIndex >= 0 && effectIndex < stewEffects.size) {
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
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 85 && mouseY <= y + height - 30) {

            if (amount < 0 && scrollOffset + maxVisibleEffects < stewEffects.size) {
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
        val componentOpt = stack.get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS)

        if (componentOpt != null) {
            isEnabled = true
            stewEffects.clear()
            stewEffects.addAll(componentOpt.effects())
            selectedEffectIndex = null
            scrollOffset = 0
        } else {
            isEnabled = false
            stewEffects.clear()
            selectedEffectIndex = null
            scrollOffset = 0
        }

        enableToggle.state = isEnabled

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) SuspiciousStewEffectsComponent(stewEffects.toList()) else null,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )
    }
}