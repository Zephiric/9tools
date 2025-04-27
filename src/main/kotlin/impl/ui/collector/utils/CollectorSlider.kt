package impl.ui.collector.utils

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.render.Engine2d
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.UIComponent
import java.awt.Color
import kotlin.math.roundToInt

class CollectorSlider(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double = 20.0,
    val label: String,
    var value: Double,
    var min: Double,
    var max: Double,
    private val step: Double? = null,
    private val format: (Double) -> String = { "%.1f".format(it) },
    private val onChange: (Double) -> Unit
) : UIComponent {

    private var labelOffsetX: Double? = null
    private var labelOffsetY: Double? = null

    private var isDragging = false
    private var dragStartValue = 0.0
    private var dragStartX = 0.0

    fun setLabelPosition(offsetX: Double, offsetY: Double) {
        this.labelOffsetX = offsetX
        this.labelOffsetY = offsetY
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val (effectiveLabelWidth, sliderStartX) = calculateSliderPosition()
        val sliderWidth = width - effectiveLabelWidth

        renderLabel(context)

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.8f),
            sliderStartX, y + 8,
            sliderStartX + sliderWidth, y + 12,
            2f, 10f
        )

        val progress = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        val handleX = sliderStartX + (sliderWidth * progress)

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.4f, 0.6f, 1f, 0.9f),
            sliderStartX, y + 8,
            handleX, y + 12,
            2f, 10f
        )

        val handleColor = when {
            isDragging -> Color(0.6f, 0.8f, 1f, 1f)
            contains(mouseX.toDouble(), mouseY.toDouble()) -> Color(0.5f, 0.7f, 1f, 0.9f)
            else -> Color(0.4f, 0.6f, 1f, 0.8f)
        }
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            handleColor,
            handleX - 4, y + 4,
            handleX + 4, y + 16,
            2f, 10f
        )

        val valueText = format(value)
        context.drawTextWithShadow(
            mc.textRenderer,
            valueText,
            (sliderStartX + sliderWidth + 5).toInt(),
            (y + 6).toInt(),
            Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
        )
    }

    private fun renderLabel(context: DrawContext) {
        if (labelOffsetX != null && labelOffsetY != null) {
            context.drawTextWithShadow(
                mc.textRenderer,
                label,
                (x + labelOffsetX!!).toInt(),
                (y + labelOffsetY!!).toInt(),
                Color(1f, 1f, 1f, 0.9f).rgb
            )
        } else {
            context.drawTextWithShadow(
                mc.textRenderer,
                label,
                x.toInt(),
                (y + 6).toInt(),
                Color(1f, 1f, 1f, 0.9f).rgb
            )
        }
    }

    private fun calculateSliderPosition(): Pair<Double, Double> {
        if (labelOffsetX != null && labelOffsetY != null) {
            return Pair(0.0, x)
        }

        val labelWidth = mc.textRenderer.getWidth(label).toDouble()
        val sliderStartX = x + labelWidth + 10
        return Pair(labelWidth + 10, sliderStartX)
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && contains(mouseX, mouseY)) {
            isDragging = true
            dragStartValue = value
            dragStartX = mouseX
            return true
        }
        return false
    }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ): Boolean {
        if (isDragging && button == 0) {
            if (mouseY < y || mouseY > y + height) {
                return false
            }

            val (effectiveLabelWidth, sliderStartX) = calculateSliderPosition()
            val sliderWidth = width - effectiveLabelWidth

            val relativeX = (mouseX - sliderStartX).coerceIn(0.0, sliderWidth)
            val percentage = relativeX / sliderWidth
            val newValue = min + (max - min) * percentage

            updateValue(newValue)
            return true
        }
        return false
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    private fun updateValue(newValue: Double) {
        value = newValue.coerceIn(min, max)
        step?.let { stepVal ->
            value = (value / stepVal).roundToInt() * stepVal
        }
        onChange(value)
    }
}