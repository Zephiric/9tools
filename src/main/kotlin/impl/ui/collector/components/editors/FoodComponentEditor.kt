
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.component.type.FoodComponent
import win.ninegang.ninetools.api.util.entity.ItemComponentUtil
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import java.util.ArrayList

// TODO: Full Refactor. Requires an effects manager

class FoodComponentEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: FoodComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:food"

    private var nutrition: Int = initialComponent?.nutrition() ?: 4
    private var saturation: Float = initialComponent?.saturation() ?: 0.3f
    private var canAlwaysEat: Boolean = initialComponent?.canAlwaysEat() ?: false
    private var eatSeconds: Float = initialComponent?.eatSeconds() ?: 1.6f
    private var effects: MutableList<FoodComponent.StatusEffectEntry> =
        ArrayList(initialComponent?.effects() ?: emptyList())

    private lateinit var nutritionSlider: CollectorSlider
    private lateinit var saturationSlider: CollectorSlider
    private lateinit var eatSecondsSlider: CollectorSlider
    private lateinit var alwaysEdibleToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        nutritionSlider = CollectorSlider(
            x = x + 5,
            y = y + 15.0,
            width = width - 150.0,
            label = " Hunger",
            value = nutrition.toDouble(),
            min = 0.0,
            max = 20.0,
            step = 1.0
        ) { newValue ->
            nutrition = newValue.toInt()
            updateFoodComponent()
        }
        nutritionSlider.setLabelPosition(0.0, -12.0)
        editorComponents.add(nutritionSlider)

        saturationSlider = CollectorSlider(
            x = x + 85.0,
            y = y + 15.0,
            width = width - 150.0,
            label = "Saturation",
            value = saturation.toDouble(),
            min = 0.0,
            max = 2.0,
            step = 0.1
        ) { newValue ->
            saturation = newValue.toFloat()
            updateFoodComponent()
        }
        saturationSlider.setLabelPosition(0.0, -12.0)
        editorComponents.add(saturationSlider)

        eatSecondsSlider = CollectorSlider(
            x = x + 5,
            y = y + 45,
            width = width - 150.0,
            label = "Eat Time",
            value = eatSeconds.toDouble(),
            min = 0.1,
            max = 5.0,
            step = 0.1
        ) { newValue ->
            eatSeconds = newValue.toFloat()
            updateFoodComponent()
        }
        eatSecondsSlider.setLabelPosition(0.0, -12.0)
        editorComponents.add(eatSecondsSlider)

        alwaysEdibleToggle = CollectorToggleButton(
            x = x + 75.0,
            y = y + 35.0,
            label = "Always Edible",
            description = "Can eat even when not hungry",
            state = canAlwaysEat
        ) { newValue ->
            canAlwaysEat = newValue
            updateFoodComponent()
        }
        editorComponents.add(alwaysEdibleToggle)

        updateFoodComponent()
    }

    private fun updateFoodComponent() {
        val component = FoodComponent(
            nutrition,
            saturation,
            canAlwaysEat,
            eatSeconds,
            effects
        )

        notifyValueChanged(component)

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = component,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val componentType = ItemComponentUtil.getComponentTypeByName(componentType)

        if (componentType != null) {
            val component = ItemComponentUtil.getComponent(stack, componentType)
                .orElse(null) as? FoodComponent

            if (component != null) {
                nutrition = component.nutrition()
                saturation = component.saturation()
                canAlwaysEat = component.canAlwaysEat()
                eatSeconds = component.eatSeconds()
                effects = ArrayList(component.effects())

                nutritionSlider.value = nutrition.toDouble()
                saturationSlider.value = saturation.toDouble()
                eatSecondsSlider.value = eatSeconds.toDouble()
                alwaysEdibleToggle.state = canAlwaysEat

                notifyValueChanged(component)
            }
        }
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
}*/
