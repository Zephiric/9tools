package impl.ui.collector.screens

import impl.ui.collector.SettingsManager
import net.minecraft.client.gui.DrawContext
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import win.ninegang.ninetools.impl.ui.collector.*
import impl.ui.collector.data.CollectorCollection
import impl.ui.collector.data.CollectorItem
import impl.ui.collector.screens.views.CollectorScreenItemDetailView
import impl.ui.collector.CollectionView
import impl.ui.collector.UIComponent
import impl.ui.collector.screens.views.GridView
import impl.ui.collector.screens.views.ListView
import impl.ui.collector.ViewType
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.UIDropdown


/**
 * Main screen for viewing a collection's items (in a grid),
 * plus a detail view when an item is selected.
 */
class CollectorScreen : BaseCollectorScreen(Text.literal("Collector")) {

    // ------------------------------------------------------------------------
    //  Screen geometry
    // ------------------------------------------------------------------------
    private var screenX = 100.0
    private var screenY = 100.0
    private var screenWidth = 400.0
    private var screenHeight = 300.0

    override fun getScreenX() = screenX
    override fun getScreenY() = screenY
    override fun getScreenWidth() = screenWidth
    override fun getScreenHeight() = screenHeight
    override fun getScreenIdentifier(): String = "collector_screen"

    // ------------------------------------------------------------------------
    //  Pagination
    // ------------------------------------------------------------------------
    private var currentPage = 0
    private val itemsPerPage: Int
        get() = SettingsManager.itemsPerPage

    private val totalPages: Int
        get() = if (items.isEmpty()) 1 else ((items.size - 1) / itemsPerPage) + 1

    /**
     * Returns just the slice of items for the current page.
     */
    private val paginatedItems: List<CollectorItem>
        get() {
            val startIndex = currentPage * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(items.size)
            return items.subList(startIndex, endIndex).toList()
        }

    // The currently loaded items from the active collection
    private val items: List<CollectorItem>
        get() = CollectorCollection.getCurrent()?.items ?: emptyList()

    // The item currently in "detail" mode, or null.
    private var selectedItem: CollectorItem? = null

    // For tooltip hover logic
    private var hoveredItem: CollectorItem? = null
    private var mouseHoverX = 0
    private var mouseHoverY = 0

    // ------------------------------------------------------------------------
    //  List Values
    // ------------------------------------------------------------------------



    private var currentViewType = SettingsManager.defaultViewType

    // View instances
    private val gridView = GridView()
    private val listView = ListView()

    // Current view reference
    private val currentView: CollectionView
        get() = when (currentViewType) {
            ViewType.GRID -> gridView
            ViewType.LIST -> listView
        }


    private fun setupViews() {
        val setupView = { view: CollectionView ->
            view.onItemClick = { clickedItem ->
                selectedItem = clickedItem
                refreshComponentsList()
            }
            view.onItemHover = { item, hoverX, hoverY ->
                hoveredItem = item
                if (item != null) {
                    mouseHoverX = hoverX.toInt()
                    mouseHoverY = hoverY.toInt()
                }
            }
        }

        setupView(gridView)
        setupView(listView)
    }

    // ------------------------------------------------------------------------
    //  UI Elements
    // ------------------------------------------------------------------------

    // View switch button
    private val viewSwitchButton = CollectorButton(
        x = screenX + screenWidth - 130,
        y = screenY + 30,
        width = 30.0,
        height = 20.0,
        text = "⋮",
        onClickAction = {
            currentViewType = when (currentViewType) {
                ViewType.GRID -> ViewType.LIST
                ViewType.LIST -> ViewType.GRID
            }
            SettingsManager.defaultViewType = currentViewType
            SettingsManager.saveSettings()
            updateComponentPositions()
        }
    )

    // A dropdown for switching or deleting collections
    private val dropdown = UIDropdown(
        x = screenX + 5,
        y = screenY + 30,
        label = "Collection",
        onSelect = { collectionName ->
            CollectorCollection.switchTo(collectionName)
            selectedItem = null
            currentPage = 0
            updateComponentPositions()
        },
        onDelete = { collectionName ->
            CollectorCollection.delete(collectionName)
            selectedItem = null
            currentPage = 0
            updateComponentPositions()
        }
    )

    // Some top bar buttons (newCollection, addItem, settings)
    private val controlButtons = mutableMapOf<String, CollectorButton>()

    // Detail view for one item
    private val detailView = CollectorScreenItemDetailView(
        x = screenX,
        y = screenY,
        width = screenWidth,
        height = screenHeight,
        onBack = {
            selectedItem = null
            updateComponentPositions()
        },
        onEdit = { item: CollectorItem ->
            mc.setScreen(ItemEditScreen(
                item = item,
                isNewItem = false,
                onSave = { updatedItem ->
                    selectedItem = updatedItem
                    updateComponentPositions()
                }
            ))
        },
        onDelete = { item: CollectorItem ->
            CollectorCollection.getCurrent()?.removeItem(item)
            selectedItem = null
            updateComponentPositions()
        },
        onSpawn = { item: CollectorItem ->
            val stack = CollectorItem.toItemStack(item)
            mc.player?.let { player ->
                val slot = 36 + player.inventory.selectedSlot
                player.networkHandler.sendPacket(CreativeInventoryActionC2SPacket(slot, stack))
                player.inventory.setStack(player.inventory.selectedSlot, stack)
                ninehack.logChat("Spawned: ${item.customName}")
            }
        },
        onViewComponents = { item: CollectorItem ->
            mc.setScreen(ItemEditScreen(item))
            ninehack.logChat("Ninehack item component system disabled due to maintenance")
        },
        onItemUpdated = { oldItem, newItem ->
            if (selectedItem == oldItem) {
                selectedItem = newItem
            }
            updateComponentPositions()
        }
    )

    // A label showing the current page near the bottom or top
    private val pageLabel = object : UIComponent {
        override var x = screenX + 180
        override var y = screenY + 285
        override var width = 0.0
        override var height = 0.0

        override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val pageText = "Page ${currentPage + 1} / $totalPages"
            context.drawText(
                mc.textRenderer,
                pageText,
                x.toInt(),
                y.toInt(),
                0xFFFFFF,
                false
            )
        }

        override fun onClick(mouseX: Double, mouseY: Double, button: Int) = false
        override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int) = false
        override fun onScroll(mouseX: Double, mouseY: Double, amount: Double) = false
        override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) = false
        override fun setPosition(newX: Double, newY: Double) {
            x = newX
            y = newY
        }
    }

    // Buttons to go to prev/next page
    private val prevPageButton = CollectorButton(
        x = screenX + -5,
        y = screenY + 145,
        width = 20.0,
        height = 20.0,
        text = "<",
        onClickAction = {
            if (currentPage > 0) currentPage--
        }
    )

    private val nextPageButton = CollectorButton(
        x = screenX + 385,
        y = screenY + 145,
        width = 20.0,
        height = 20.0,
        text = ">",
        onClickAction = {
            if (currentPage < totalPages - 1) currentPage++
        }
    )

    init {
        loadSavedPosition()
        setupViews()
        initializeControlButtons()
        updateComponentPositions()
    }

    private fun initializeControlButtons() {
        controlButtons["newCollection"] = CollectorButton.createNewButton(
            x = screenX + 220,
            y = screenY + 30
        ) {
            mc.player?.let { player ->
                CollectorCollection.createNew(
                    "Collection ${CollectorCollection.getAvailable().size + 1}",
                    player.name.string
                )
            }
        }

        controlButtons["addItem"] = CollectorButton.createAddButton(
            x = screenX + screenWidth - 60,
            y = screenY + 30
        ) {
            mc.player?.mainHandStack?.let { stack ->
                if (!stack.isEmpty) {
                    val newItem = CollectorItem.fromItemStack(
                        stack = stack,
                        givenBy = mc.player?.name?.string ?: "",
                        location = CollectorCollection.getCurrent()?.getCurrentLocation() ?: "Unknown"
                    )
                    mc.setScreen(ItemEditScreen(newItem, true))
                }
            }
        }

        controlButtons["settings"] = CollectorButton.createSettingsButton(
            x = screenX + screenWidth - 95,
            y = screenY + 30
        ) {
            mc.setScreen(SettingsScreen())
        }
    }

    private fun refreshComponentsList() {
        components.clear()

        if (selectedItem == null) {
            components += dropdown
            components += currentView
            components += pageLabel
            components += prevPageButton
            components += nextPageButton
            components += viewSwitchButton
            controlButtons.values.forEach { components += it }
        } else {
            detailView.setItem(selectedItem)
            components += detailView
        }
    }

    private fun updateComponentPositions() {
        val viewHeight = screenHeight - 100

        gridView.setPosition(screenX + 13, screenY + 60)
        gridView.width = screenWidth - 20
        gridView.height = viewHeight

        listView.setPosition(screenX + 13, screenY + 60)
        listView.width = screenWidth - 20
        listView.height = viewHeight

        viewSwitchButton.setPosition(screenX + screenWidth - 130, screenY + 30)

        dropdown.setPosition(screenX + 5, screenY + 30)

        detailView.setPosition(screenX, screenY)
        detailView.width = screenWidth
        detailView.height = screenHeight

        controlButtons["newCollection"]?.setPosition(screenX + 220, screenY + 30)
        controlButtons["addItem"]?.setPosition(screenX + screenWidth - 60, screenY + 30)
        controlButtons["settings"]?.setPosition(screenX + screenWidth - 95, screenY + 30)

        pageLabel.setPosition(screenX + 180, screenY + 285)
        prevPageButton.setPosition(screenX + -5, screenY + 145)
        nextPageButton.setPosition(screenX + 385, screenY + 145)

        refreshComponentsList()
    }

    override fun updateScreenPosition(newX: Double, newY: Double) {
        screenX = newX
        screenY = newY
        updateComponentPositions()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedItem != null) {
                selectedItem = null
                updateComponentPositions()
                return true
            } else {
                close()
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        if (selectedItem != null) {
            detailView.setItem(selectedItem)
        } else {
            currentView.items = paginatedItems
        }

        dropdown.items = CollectorCollection.getAvailable()

            renderBackground(context, screenX, screenY, screenWidth, screenHeight)
            renderTitleBar(context, screenX, screenY, screenWidth, "Collection")

            components.sortedBy { it.priority }.forEach { it.render(context, mouseX, mouseY, delta) }

            if (selectedItem == null && hoveredItem != null) {
                renderTooltipIfNeeded(context, hoveredItem, mouseHoverX, mouseHoverY)
            }
        }



    override fun close() {
        mc.setScreen(null)
    }
}
