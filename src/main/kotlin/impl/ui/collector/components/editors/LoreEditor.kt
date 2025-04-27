package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.component.type.LoreComponent
import net.minecraft.text.Style
import net.minecraft.text.Text
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorTextBox
import impl.ui.collector.utils.CollectorToggleButton
import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil
import java.awt.Color
import java.util.ArrayList

class LoreEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: LoreComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:lore"

    private val customHeight = 130.0

    private var isEnabled: Boolean = initialComponent != null
    private val loreLines = ArrayList<Text>()

    private lateinit var textBox: CollectorTextBox
    private lateinit var addButton: CollectorButton
    private lateinit var deleteButton: CollectorButton
    private lateinit var enableToggle: CollectorToggleButton

    private var scrollOffset = 0
    private val maxVisibleLines = 3
    private var selectedLineIndex: Int? = null

    init {
        initializeDimensions(x, y)
        this.height = customHeight

        initialComponent?.lines()?.let { loreLines.addAll(it) }

        textBox = CollectorTextBox(
            x = x + 5.0,
            y = y + 20.0,
            width = width - 75.0,
            label = "Add Lore Line",
            placeholder = "Enter lore text...",
            initialText = ""
        ) {}
        textBox.setLabelPosition(0.0, -15.0)


        addButton = CollectorButton.createAddButton(
            x = x + width - 65.0,
            y = y + 22.0
        ) {
            val newText = textBox.getText()
            if (newText.isNotEmpty()) {
                val style = Style.EMPTY.withItalic(true)
                val loreText = Text.literal(newText).setStyle(style)

                loreLines.add(loreText)
                textBox.setText("")

                if (isEnabled) {
                    updateLoreComponent()
                }
            }
        }

        deleteButton = CollectorButton.createDeleteButton(
            x = x + width - 25.0,
            y = y + 22.0
        ) {
            selectedLineIndex?.let { index ->
                if (index >= 0 && index < loreLines.size) {
                    loreLines.removeAt(index)
                    selectedLineIndex = null

                    if (isEnabled) {
                        updateLoreComponent()
                    }

                    if (scrollOffset > 0 && loreLines.size <= maxVisibleLines) {
                        scrollOffset = 0
                    } else if (scrollOffset > 0 && scrollOffset >= loreLines.size - maxVisibleLines) {
                        scrollOffset = (loreLines.size - maxVisibleLines).coerceAtLeast(0)
                    }
                }
            }
        }

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 110.0,
            label = "Enable",
            description = "Enable/disable lore component",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                updateLoreComponent()
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(textBox)
        editorComponents.add(addButton)
        editorComponents.add(deleteButton)
        editorComponents.add(enableToggle)

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) createLoreComponent() else null,
                isValid = true,
                columnSpan = 1,
                customHeight = customHeight
            )
        )
    }

    private fun createLoreComponent(): LoreComponent {
        return LoreComponent(loreLines.toList())
    }

    private fun updateLoreComponent() {
        val component = createLoreComponent()
        notifyValueChanged(component)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            x, y,
            x + width, y + height,
            2f, 10f
        )
        editorComponents.forEach { it.render(context, mouseX, mouseY, delta) }

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.15f, 0.15f, 0.15f, 0.5f),
            x + 5, y + 50,
            x + width - 5, y + customHeight - 20,
            2f, 10f
        )

        val visibleLines = loreLines.size.coerceAtMost(maxVisibleLines)
        if (loreLines.isEmpty()) {
            context.drawTextWithShadow(
                mc.textRenderer,
                "No lore lines added",
                (x + 10).toInt(),
                (y + 60).toInt(),
                Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
            )
        } else {
            for (i in 0 until visibleLines) {
                val index = i + scrollOffset
                if (index < loreLines.size) {
                    val line = loreLines[index]
                    val yPos = y + 55 + (i * 20)

                    if (selectedLineIndex == index) {
                        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                            context.matrices,
                            Color(0.3f, 0.3f, 0.5f, 0.5f),
                            x + 6, yPos,
                            x + width - 6, yPos + 18,
                            1f, 5f
                        )
                    }

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        line,
                        (x + 10).toInt(),
                        (yPos + 6).toInt(),
                        line.style.color?.rgb ?: Color(0.6f, 0.4f, 0.8f, 1f).rgb
                    )
                }
            }

            if (loreLines.size > maxVisibleLines) {
                if (scrollOffset > 0) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        "▲",
                        (x + width / 2).toInt(),
                        (y + 50).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }

                if (scrollOffset + maxVisibleLines < loreLines.size) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        "▼",
                        (x + width / 2).toInt(),
                        (y + customHeight - 35).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }
            }
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 50 && mouseY <= y + customHeight - 30) {

            val relativeY = mouseY - (y + 55)
            val lineIndex = (relativeY / 20).toInt() + scrollOffset

            if (lineIndex >= 0 && lineIndex < loreLines.size) {
                selectedLineIndex = lineIndex
                return true
            } else {
                selectedLineIndex = null
            }
        } else if (!contains(mouseX, mouseY)) {
            selectedLineIndex = null
        }

        return super.onClick(mouseX, mouseY, button)
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 50 && mouseY <= y + customHeight - 30) {

            if (amount < 0 && scrollOffset + maxVisibleLines < loreLines.size) {
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

        loreLines.clear()

        val component = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(
            CollectorItem.toItemStack(collectorItem),
            win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(componentType)
        ).orElse(null) as? LoreComponent

        if (component != null) {
            loreLines.addAll(component.lines())
            isEnabled = true
            enableToggle.state = true
            notifyValueChanged(component)
        } else {
            isEnabled = false
            enableToggle.state = false
            notifyValueChanged(null)
        }

        scrollOffset = 0
        selectedLineIndex = null

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) createLoreComponent() else null,
                isValid = true,
                columnSpan = 1,
                customHeight = customHeight
            )
        )
    }
}