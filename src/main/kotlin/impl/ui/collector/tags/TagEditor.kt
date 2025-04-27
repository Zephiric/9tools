package impl.ui.collector.tags

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.UIComponent
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorTextBox
import java.awt.Color

class TagEditor(
    override var x: Double,
    override var y: Double,
    override var width: Double = 200.0,
    override var height: Double = 75.0,
    private val isGroup: Boolean,
    private val groupName: String,
    private val tagName: String? = null,
    private val onSave: (name: String) -> Unit = { _ -> },
    private val onCancel: () -> Unit = {}
) : UIComponent {

    private val components = mutableListOf<UIComponent>()

    private val padding = 10.0

    private lateinit var nameTextBox: CollectorTextBox
    private lateinit var saveButton: CollectorButton
    private lateinit var cancelButton: CollectorButton

    private var originalName: String

    init {
        originalName = if (isGroup) groupName else tagName ?: ""

        createComponents()
    }

    private fun createComponents() {
        val startY = y + padding
        val startX = x + padding

        nameTextBox = CollectorTextBox(
            x = startX,
            y = startY + 10,
            width = width - (padding * 2),
            height = 25.0,
            initialText = originalName,
            onChange = {}
        )
        components.add(nameTextBox)

        saveButton = CollectorButton.createSaveButton(
            x = startX,
            y = startY + 40,
            onClick = {
                onSaveClick()
            }
        )
        components.add(saveButton)

        cancelButton = CollectorButton.createBackButton(
            x = startX + 70,
            y = startY + 40,
            onClick = {
                onCancelClick()
            }
        )
        components.add(cancelButton)
    }

    private fun onSaveClick() {
        val newName = nameTextBox.getText()
        if (newName.isNotEmpty()) {
            onSave(newName)
            onCancel()
        }
    }

    private fun onCancelClick() {
        onCancel()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.12f, 0.12f, 0.12f, 0.9f),
            x, y,
            x + width, y + height,
            5f, 10f
        )

        val title = if (isGroup) "Edit Group" else "Edit Tag"
        context.drawTextWithShadow(
            mc.textRenderer,
            title,
            (x + padding).toInt(),
            (y + 5).toInt(),
            Color.WHITE.rgb
        )

        for (component in components) {
            component.render(context, mouseX, mouseY, delta)
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (component in components) {
            if (component.contains(mouseX, mouseY) && component.onClick(mouseX, mouseY, button)) {
                return true
            }
        }
        return contains(mouseX, mouseY)
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean {
        for (component in components) {
            if (component.onDrag(mouseX, mouseY, deltaX, deltaY, button)) {
                return true
            }
        }
        return false
    }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (component in components) {
            if (component.onKeyPress(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        for (component in components) {
            if (component.onCharTyped(chr, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        for (component in components) {
            component.setPosition(component.x + deltaX, component.y + deltaY)
        }
    }

    override fun getChildComponents(): List<UIComponent> {
        return components
    }
}