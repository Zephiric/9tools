package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import net.minecraft.util.Unit
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.utils.CollectorToggleButton

class IntangibleProjectileEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: Boolean = false
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:intangible_projectile"

    private var isEnabled: Boolean = initialValue

    init {
        initializeDimensions(x, y)

        val intangibleProjectileToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y,
            label = "Intangible Projectiles",
            description = "Makes it so projectiles cannot be picked back up",
            state = initialValue
        ) { newValue ->
            isEnabled = newValue
            if (newValue) {
                notifyValueChanged(Unit.INSTANCE)
            } else {
                notifyValueChanged(null)
            }
        }

        editorComponents.add(intangibleProjectileToggle)
    }
}