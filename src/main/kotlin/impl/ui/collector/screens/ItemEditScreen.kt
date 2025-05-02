package impl.ui.collector.screens

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.data.CollectorMaps
import impl.ui.collector.components.TooltipHandler
import impl.ui.collector.data.CollectorCollection
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorTextBox
import win.ninegang.ninetools.CollectorMod
import java.awt.Color

/**
 * Screen that lets you edit an existing CollectorItem (rename, location, etc.).
 * also acts as the add item screen.
 * thank you jippity again
 */
class ItemEditScreen(
    private val item: CollectorItem,
    private val isNewItem: Boolean = false,
    private val onSave: ((CollectorItem) -> Unit)? = null
) : BaseCollectorScreen(Text.literal("Edit Item")) {

    private var customName = item.customName
    private var givenBy = item.givenBy
    private var location = item.location
    private var description = item.description ?: ""

    private var screenX = 100.0
    private var screenY = 100.0
    private var screenWidth = 300.0
    private var screenHeight = 400.0

    override fun getScreenX() = screenX
    override fun getScreenY() = screenY
    override fun getScreenWidth() = screenWidth
    override fun getScreenHeight() = screenHeight
    override fun getScreenIdentifier(): String = "item_edit_screen"

    override fun updateScreenPosition(newX: Double, newY: Double) {
        screenX = newX
        screenY = newY
        updateComponentPositions()
    }

    private lateinit var saveButton: CollectorButton
    private lateinit var backButton: CollectorButton
    private lateinit var componentsButton: CollectorButton

    private lateinit var nameTextBox: CollectorTextBox
    private lateinit var givenByTextBox: CollectorTextBox
    private lateinit var locationTextBox: CollectorTextBox
    private lateinit var descriptionTextBox: CollectorTextBox

    init {
        initializeControlButtons()
        initializeTextBoxes()
        buildComponentsList()
        loadSavedPosition()
    }

    /**
     * Initializes and positions the control buttons.
     */
    private fun initializeControlButtons() {
        saveButton = CollectorButton(
            x = screenX + screenWidth / 2 - 55,
            y = screenY + screenHeight - 40,
            width = 50.0,
            text = "Save",
            onClickAction = { saveChanges() },
            type = CollectorButton.ButtonType.POSITIVE
        )

        backButton = CollectorButton(
            x = screenX + screenWidth / 2 + 5,
            y = screenY + screenHeight - 40,
            width = 50.0,
            text = "Back",
            onClickAction = { mc.setScreen(CollectorMod.collector) },
            type = CollectorButton.ButtonType.NEUTRAL
        )

        componentsButton = CollectorButton.createComponentsButton(
            x = screenX + screenWidth - 85,
            y = screenY + screenHeight - 40
        ) {

            ninehack.logChat("Components button clicked for item: ${item.customName}")
        }
    }

    /**
     * Initializes and positions the text boxes for each editable field.
     */
    private fun initializeTextBoxes() {
        nameTextBox = CollectorTextBox(
            x = screenX + 20,
            y = screenY + 100,
            width = screenWidth - 40,
            height = 25.0,
            label = "Name",
            placeholder = "Enter name",
            maxLength = 50,
            initialText = customName,
            onChange = { newText ->
                customName = newText
            }
        )

        givenByTextBox = CollectorTextBox(
            x = screenX + 20,
            y = screenY + 160,
            width = screenWidth - 40,
            height = 25.0,
            label = "Given By",
            placeholder = "Enter giver's name",
            maxLength = 32,
            initialText = givenBy,
            onChange = { newText ->
                givenBy = newText
            }
        )

        locationTextBox = CollectorTextBox(
            x = screenX + 20,
            y = screenY + 220,
            width = screenWidth - 40,
            height = 25.0,
            label = "Location",
            placeholder = "Enter location",
            maxLength = 32,
            initialText = location,
            onChange = { newText ->
                location = newText
            }
        )

        descriptionTextBox = CollectorTextBox(
            x = screenX + 20,
            y = screenY + 280,
            width = screenWidth - 40,
            height = 25.0,
            label = "Description",
            placeholder = "Enter description",
            maxLength = 200,
            initialText = description,
            onChange = { newText ->
                description = newText
            }
        )
    }

    /**
     * Adds all UI components to the components list for centralized management.
     */
    private fun buildComponentsList() {
        components.clear()
        components += saveButton
        components += backButton
        components += componentsButton
        components += nameTextBox
        components += givenByTextBox
        components += locationTextBox
        components += descriptionTextBox
    }

    /**
     * Updates the positions of all UI components when the screen is moved or resized.
     */
    private fun updateComponentPositions() {
        saveButton.setPosition(
            screenX + screenWidth / 2 - 55,
            screenY + screenHeight - 40
        )
        backButton.setPosition(
            screenX + screenWidth / 2 + 5,
            screenY + screenHeight - 40
        )
        componentsButton.setPosition(
            screenX + screenWidth - 85,
            screenY + screenHeight - 40
        )

        nameTextBox.setPosition(screenX + 20, screenY + 100)
        givenByTextBox.setPosition(screenX + 20, screenY + 160)
        locationTextBox.setPosition(screenX + 20, screenY + 220)
        descriptionTextBox.setPosition(screenX + 20, screenY + 280)

        buildComponentsList()
    }

    override fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderBackground(context, screenX, screenY, screenWidth, screenHeight)
            renderTitleBar(context, screenX, screenY, screenWidth, "Edit Item")

            renderItemPreview(context, mouseX, mouseY)

            components.sortedBy { it.priority }.forEach {
                it.render(context, mouseX, mouseY, delta)
            }
    }

    /**
     * Renders a small item icon preview near the top of the screen.
     */
    private fun renderItemPreview(context: DrawContext, mouseX: Int, mouseY: Int) {
        val previewX = screenX + 26
        val previewY = screenY + 40

        val previewSize = 20
        val padding = 2

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.8f),
            previewX,
            previewY,
            previewX + previewSize,
            previewY + previewSize,
            2f,
            10f
        )

        val stack = CollectorItem.toItemStack(item)
        context.drawItem(
            stack,
            (previewX + padding).toInt(),
            (previewY + padding).toInt()
        )

        val hoverLeft = previewX + 1
        val hoverRight = previewX + previewSize
        val hoverTop = previewY + 1
        val hoverBottom = previewY + previewSize

        if (mouseX >= hoverLeft && mouseX <= hoverRight &&
            mouseY >= hoverTop && mouseY <= hoverBottom
        ) {
            val lines = TooltipHandler.generateTooltip(stack, item)
            TooltipHandler.renderTooltip(context, lines, mouseX, mouseY)
        }
    }


    /**
     * Saves the changes made to the item and updates the collection.
     */
    private fun saveChanges() {
        val components = item._components
        val mapIdComponent = components.find { it.type() == DataComponentTypes.MAP_ID }

        if (mapIdComponent != null) {
            val mapId = (mapIdComponent.value() as? MapIdComponent)?.id ?: 0

            if (mapId > 0) {
                CollectorMaps.saveMap(
                    mapId = mapId,
                    name = customName,
                    description = description ?: ""
                )

                ninehack.logChat("Saved map data for map #${mapId}")
            }
        }

        val updatedItem = item.createModified(
            customName = customName,
            givenBy = givenBy,
            location = location,
            description = description
        )

        CollectorCollection.getCurrent()?.let { collection ->
            collection.updateItem(if (isNewItem) null else item, updatedItem)
            collection.save()

            onSave?.invoke(updatedItem)
        }

        if (onSave == null) {
            mc.setScreen(CollectorMod.collector)
        }
    }

    override fun close() {
        mc.setScreen(CollectorScreen())
    }
}