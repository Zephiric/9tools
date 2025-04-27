
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.type.MapDecorationsComponent
import net.minecraft.item.map.MapDecorationType
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import win.ninegang.ninetools.api.util.Wrapper.mc
import win.ninegang.ninetools.api.util.render.Engine2d
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorSlider
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown
import impl.ui.collector.utils.SelectableGrid
import java.awt.Color



class MapDecorationsEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: MapDecorationsComponent?
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:map_decorations"

    private val customHeight = 130.0
    private val customWidth = 410.0
    private var isEnabled: Boolean = initialComponent != null
    private var decorations: MutableMap<String, MapDecorationsComponent.Decoration> =
        initialComponent?.decorations()?.toMutableMap() ?: mutableMapOf()

    private lateinit var enableToggle: CollectorToggleButton
    private lateinit var typeDropdown: UIDropdown
    private lateinit var xSlider: CollectorSlider
    private lateinit var zSlider: CollectorSlider
    private lateinit var directionGrid: SelectableGrid
    private lateinit var addButton: CollectorButton
    private lateinit var clearButton: CollectorButton

    private var currentType: String = "player"
    private var currentX: Double = 0.0
    private var currentZ: Double = 0.0
    private var currentRotation: Float = 0f
    private var nextDecorationId: Int = 1

    init {
        initializeDimensions(x, y)
        this.height = customHeight
        this.width = customWidth

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) MapDecorationsComponent(decorations) else null,
                isValid = true,
                columnSpan = 2,
                customHeight = height
            )
        )

        val decorationTypes = listOf(
            "Player",
            "Frame",
            "Red Marker",
            "Blue Marker",
            "Target X",
            "Target Point",
            "Banner White",
            "Banner Orange",
            "Banner Magenta",
            "Banner Light Blue",
            "Banner Yellow",
            "Banner Lime",
            "Banner Pink",
            "Banner Gray",
            "Banner Light Gray",
            "Banner Cyan",
            "Banner Purple",
            "Banner Blue",
            "Banner Brown",
            "Banner Green",
            "Banner Red",
            "Banner Black"
        )

        typeDropdown = UIDropdown(
            x = x + 5,
            y = y + 5,
            width = 100.0,
            label = "Decoration Type",
            onSelect = { selected ->
                currentType = convertTypeToIdentifier(selected)
            }
        ).apply {
            items = decorationTypes
            currentSelection = decorationTypes[0]
        }
        editorComponents.add(typeDropdown)

        xSlider = CollectorSlider(
            x = x + 5,
            y = y + 25,
            width = 130.0,
            label = "X Position",
            value = 0.0,
            min = -128.0,
            max = 127.0,
            step = 1.0
        ) { newValue ->
            currentX = newValue
        }
        editorComponents.add(xSlider)

        zSlider = CollectorSlider(
            x = x + 5,
            y = y + 35,
            width = 130.0,
            label = "Z Position",
            value = 0.0,
            min = -128.0,
            max = 127.0,
            step = 1.0
        ) { newValue ->
            currentZ = newValue
        }
        editorComponents.add(zSlider)

        directionGrid = SelectableGrid(
            x = x + 5.0,
            y = y + 55.0,
            width = 130.0,
            height = 20.0
        ) { direction ->
            currentRotation = when(direction) {
                "North" -> 0f
                "East" -> 4f
                "South" -> 8f
                "West" -> 12f
                else -> 0f
            }

            val directions = listOf("North", "East", "South", "West")
            directions.forEach { dir ->
                directionGrid.setActiveItem(dir, dir == direction)
            }
        }

        val directionItems = listOf(
            SelectableGrid.GridItem("North", true, "Facing North (0°)"),
            SelectableGrid.GridItem("East", true, "Facing East (90°)"),
            SelectableGrid.GridItem("South", true, "Facing South (180°)"),
            SelectableGrid.GridItem("West", true, "Facing West (270°)")
        )

        directionGrid.setItems(directionItems)
        directionGrid.setActiveItem("North", true)
        editorComponents.add(directionGrid)

        addButton = CollectorButton(
            x = x + 5,
            y = y + 85,
            width = 90.0,
            height = 20.0,
            text = "Add Decoration",
            onClickAction = { addDecoration() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        editorComponents.add(addButton)

        clearButton = CollectorButton(
            x = x + 105,
            y = y + 85,
            width = 90.0,
            height = 20.0,
            text = "Clear All",
            onClickAction = { clearAllDecorations() },
            type = CollectorButton.ButtonType.NEGATIVE
        )
        editorComponents.add(clearButton)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 110.0,
            label = "Enable",
            description = "Enable/disable map decorations",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            if (enabled) {
                notifyValueChanged(MapDecorationsComponent(decorations))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    private fun addDecoration() {
        val id = "custom_${nextDecorationId++}"

        try {
            val registryManager = mc.world?.registryManager

            val registry = registryManager?.(RegistryKeys.MAP_DECORATION_TYPE)
            if (registry != null) {
                val typeId = Identifier.of("minecraft", currentType)

                val decorationType = registry.get(typeId)

                if (decorationType != null) {
                    val entry = RegistryEntry.of(decorationType)

                    val decoration = MapDecorationsComponent.Decoration(
                        entry,
                        currentX,
                        currentZ,
                        currentRotation
                    )

                    decorations[id] = decoration

                    if (isEnabled) {
                        notifyValueChanged(MapDecorationsComponent(decorations))
                    }
                } else {
                    println("Failed to get decoration type for ID: $typeId")
                }
            }
        } catch (e: Exception) {
            println("Error adding decoration: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun clearAllDecorations() {
        decorations.clear()
        if (isEnabled) {
            notifyValueChanged(MapDecorationsComponent(decorations))
        }
    }

    private fun convertTypeToIdentifier(displayName: String): String {
        return when (displayName.lowercase()) {
            "player" -> "player"
            "frame" -> "frame"
            "red marker" -> "red_marker"
            "blue marker" -> "blue_marker"
            "target x" -> "target_x"
            "target point" -> "target_point"
            "banner white" -> "banner_white"
            "banner orange" -> "banner_orange"
            "banner magenta" -> "banner_magenta"
            "banner light blue" -> "banner_light_blue"
            "banner yellow" -> "banner_yellow"
            "banner lime" -> "banner_lime"
            "banner pink" -> "banner_pink"
            "banner gray" -> "banner_gray"
            "banner light gray" -> "banner_light_gray"
            "banner cyan" -> "banner_cyan"
            "banner purple" -> "banner_purple"
            "banner blue" -> "banner_blue"
            "banner brown" -> "banner_brown"
            "banner green" -> "banner_green"
            "banner red" -> "banner_red"
            "banner black" -> "banner_black"
            else -> "player"
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

        context.drawText(
            mc.textRenderer,
            "Decorations: ${decorations.size}",
            (x + 5).toInt(),
            (y + 75).toInt(),
            0xFFFFFF,
            true
        )
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled) MapDecorationsComponent(decorations) else null,
                isValid = true,
                columnSpan = 2,
                customHeight = height
            )
        )
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
