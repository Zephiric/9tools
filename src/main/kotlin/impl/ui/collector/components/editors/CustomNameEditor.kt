package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.text.Style
import net.minecraft.text.Text
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorTextBox
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.SelectableGrid
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil

class CustomNameEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: String,
    initialItalic: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:custom_name"

    private val customHeight = 130.0

    private val activeStyles = mutableMapOf(
        "bold" to false,
        "italic" to initialItalic,
        "underline" to false,
        "strikethrough" to false,
        "obfuscated" to false
    )

    private var currentText: String = initialValue
    private var isEnabled: Boolean = true

    private lateinit var textBox: CollectorTextBox
    private lateinit var styleGrid: SelectableGrid
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        this.height = customHeight

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = null,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )

        textBox = CollectorTextBox(
            x = x + 5.0,
            y = y + 20.0,
            width = width - 40.0,
            label = "Custom Name",
            placeholder = "Enter custom name...",
            initialText = initialValue
        ) { newText ->
            currentText = newText
            if (isEnabled) {
                updateTextComponent()
            }
        }

        textBox.setLabelPosition(0.0, -15.0)

        styleGrid = SelectableGrid(
            x = x + 5.0,
            y = y + 50.0,
            width = width - 20.0,
            height = 30.0
        ) { styleName ->
            activeStyles[styleName] = !activeStyles[styleName]!!

            styleGrid.setActiveItem(styleName, activeStyles[styleName]!!)

            updateTextComponent()
        }

        val styleItems = listOf(
            SelectableGrid.GridItem("bold", true, "Bold text"),
            SelectableGrid.GridItem("italic", true, "Italic text"),
            SelectableGrid.GridItem("underline", true, "Underlined text"),
            SelectableGrid.GridItem("strikethrough", true, "Strikethrough text"),
            SelectableGrid.GridItem("obfuscated", true, "Obfuscated text")
        )
        styleGrid.setItems(styleItems)

        if (initialItalic) {
            styleGrid.setActiveItem("italic", true)
        }

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 110.0,
            label = "Enable",
            description = "Enable/disable custom name",
            state = true
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                updateTextComponent()
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(textBox)
        editorComponents.add(styleGrid)
        editorComponents.add(enableToggle)
    }

    private fun createTextComponent(): Text {
        var style = Style.EMPTY

        if (activeStyles["bold"] == true) style = style.withBold(true)
        if (activeStyles["italic"] == true) style = style.withItalic(true)
        if (activeStyles["underline"] == true) style = style.withUnderline(true)
        if (activeStyles["strikethrough"] == true) style = style.withStrikethrough(true)
        if (activeStyles["obfuscated"] == true) style = style.withObfuscated(true)

        return Text.literal(currentText).setStyle(style)
    }

    private fun updateTextComponent() {
        val textComponent = createTextComponent()
        notifyValueChanged(textComponent)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val textComponent = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(
            CollectorItem.toItemStack(collectorItem),
            win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(componentType)
        ).orElse(null) as? Text

        if (textComponent != null) {
            currentText = textComponent.string
            textBox.setText(currentText)

            val style = textComponent.style
            activeStyles["bold"] = style.isBold()
            activeStyles["italic"] = style.isItalic()
            activeStyles["underline"] = style.isUnderlined()
            activeStyles["strikethrough"] = style.isStrikethrough()
            activeStyles["obfuscated"] = style.isObfuscated()

            activeStyles.forEach { (styleName, isActive) ->
                styleGrid.setActiveItem(styleName, isActive)
            }

            isEnabled = true
            enableToggle.state = true
        } else {
            isEnabled = false
            enableToggle.state = false
        }

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) createTextComponent() else null,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )
    }
}