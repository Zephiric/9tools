package impl.ui.collector.components

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.ComponentType
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.UIComponent
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil
import java.awt.Color

abstract class BaseComponentEditor(
    protected val registry: EditorRegistry
) : UIComponent {
    companion object {
        const val STANDARD_WIDTH = 200.0
        const val STANDARD_HEIGHT = 60.0
    }

    override var x: Double = 0.0
    override var y: Double = 0.0
    override var width: Double = STANDARD_WIDTH
    override var height: Double = STANDARD_HEIGHT


    protected val editorComponents = mutableListOf<UIComponent>()


    abstract val componentType: String

    private lateinit var _item: CollectorItem
    protected val item: CollectorItem get() = _item

    init {
        registry.registerEditor(componentType, this)
    }

    open fun initializeItem(collectorItem: CollectorItem) {
        _item = collectorItem
    }

    var onValueChange: ((Any?) -> Unit)? = null

    protected fun notifyValueChanged(newValue: Any?) {
        onValueChange?.invoke(newValue)
        registry.updateState(componentType, EditorState(
            componentType = componentType,
            value = newValue,
            isValid = true
        )
        )
    }

    fun cleanup() {
        registry.unregisterEditor(componentType)
    }

    protected fun initializeDimensions(x: Double, y: Double) {
        this.x = x
        this.y = y
        this.width = STANDARD_WIDTH
        this.height = STANDARD_HEIGHT
    }

    /**
     * Get the DataComponentType for this editor
     */
    protected fun getComponentType(): ComponentType<*>? {
        return win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(componentType)
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
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return editorComponents.sortedByDescending { it.priority }
            .any { it.onClick(mouseX, mouseY, button) }
    }

    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int): Boolean {
        return editorComponents
            .sortedByDescending { it.priority }
            .any { it.onDrag(mouseX, mouseY, deltaX, deltaY, button) }
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return editorComponents
            .sortedByDescending { it.priority }
            .any { it.onScroll(mouseX, mouseY, amount) }
    }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return editorComponents
            .sortedByDescending { it.priority }
            .any { it.onKeyPress(keyCode, scanCode, modifiers) }
    }

    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        return editorComponents
            .sortedByDescending { it.priority }
            .any { it.onCharTyped(chr, modifiers) }
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        editorComponents.forEach { component ->
            component.setPosition(component.x + deltaX, component.y + deltaY)
        }
    }

    override fun getChildComponents(): List<UIComponent> = editorComponents
}