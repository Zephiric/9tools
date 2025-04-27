
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Instrument
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Text
import win.ninegang.ninetools.api.util.Wrapper.mc
import win.ninegang.ninetools.api.util.render.Engine2d
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown
import java.awt.Color

class InstrumentEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: RegistryEntry<Instrument>? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:instrument"

    private var currentInstrument: RegistryEntry<Instrument>? = initialValue
    private var isEnabled: Boolean = initialValue != null

    private val instrumentDropdown: UIDropdown
    private val enableToggle: CollectorToggleButton

    private val instrumentMap = mutableMapOf<String, RegistryEntry<Instrument>>()

    init {
        initializeDimensions(x, y)

        val instrumentOptions = mutableListOf<String>()

        Registries.SOUND_EVENT.forEach { instrument ->
            val id = Registries.SOUND_EVENT.getId(instrument)?.toString() ?: return@forEach

            val displayName = id.split(":").last().replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

            instrumentOptions.add(displayName)

            val key = Registries.SOUND_EVENT.getKey(instrument).orElse(null) ?: return@forEach
            val registryEntry = Registries.(key).orElse(null) ?: return@forEach

            instrumentMap[displayName] = registryEntry
        }

        instrumentOptions.add(0, "None")

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = initialValue,
                isValid = true
            )
        )

        instrumentDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 120.0,
            label = "Instrument",
            onSelect = { selected ->
                if (selected == "None") {
                    currentInstrument = null
                    if (isEnabled) {
                        notifyValueChanged(null)
                    }
                } else {
                    val selectedInstrument = instrumentMap[selected]
                    selectedInstrument?.let {
                        currentInstrument = it
                        if (isEnabled) {
                            notifyValueChanged(it)
                        }
                    }
                }
            }
        ).apply {
            items = instrumentOptions
            currentSelection = initialValue?.let {
                instrumentMap.entries.find { entry -> entry.value == it }?.key
            } ?: "None"
        }
        editorComponents.add(instrumentDropdown)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable instrument",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                currentInstrument?.let {
                    notifyValueChanged(it)
                } ?: run {
                    if (instrumentMap.isNotEmpty()) {
                        val firstInstrument = instrumentMap.values.firstOrNull()
                        firstInstrument?.let {
                            currentInstrument = it
                            instrumentDropdown.currentSelection = instrumentMap.entries
                                .find { entry -> entry.value == it }?.key ?: "None"
                            notifyValueChanged(it)
                        }
                    }
                }
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            x, y,
            x + width, y + height,
            2f, 10f
        )

        currentInstrument?.let { instrument ->
            val instrumentValue = instrument.value()

            val soundEventEntry = instrumentValue.soundEvent()
            val soundEvent = soundEventEntry.value()
            val soundRegistry = Registries.SOUND_EVENT
            val soundId = soundRegistry.getId(soundEvent)
            val soundName = soundId?.path?.replace("_", " ") ?: "unknown"

            val infoText = "Sound: $soundName | Duration: ${instrumentValue.useDuration()}t | Range: ${instrumentValue.range()}b"

            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal(infoText),
                (x + 5).toInt(),
                (y + 70).toInt(),
                0xCCCCCC
            )
        }

        editorComponents.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val componentOpt = stack.get(DataComponentTypes.INSTRUMENT)

        if (componentOpt != null) {
            isEnabled = true
            currentInstrument = componentOpt
            instrumentDropdown.currentSelection = instrumentMap.entries
                .find { entry -> entry.value == componentOpt }?.key ?: "None"
        } else {
            isEnabled = false
            currentInstrument = null
            instrumentDropdown.currentSelection = "None"
        }

        enableToggle.state = isEnabled

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) currentInstrument else null,
                isValid = true
            )
        )
    }
}*/
