package impl.ui.collector.screens

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.ComponentMap
import net.minecraft.component.ComponentType
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import impl.ui.collector.components.NineComponentEditorFactory
import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.data.CollectorCollection
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.SelectableGrid
import impl.ui.collector.utils.UIContainer
import impl.ui.collector.utils.UIDropdown
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil
import java.awt.Color

class NineComponentScreen(private val item: CollectorItem) : BaseCollectorScreen(Text.literal("Component Editor")) {

    companion object {
        private const val EDITOR_SPACING = 10.0
        private const val DEFAULT_COLUMN_COUNT = 3
    }

    private val baseScreenWidth = 650.0
    private val baseScreenHeight = 400.0

    private var screenX = 100.0
    private var screenY = 100.0
    private var screenWidth = baseScreenWidth
    private var screenHeight = baseScreenHeight
    private var columnCount = DEFAULT_COLUMN_COUNT
    private val maxAllowedColumns = 6

    private val columnConfigs = mapOf(
        "1 Column" to 3.0,
        "2 Columns" to 1.5,
        "3 Columns" to 1.0,
        "4 Columns" to 0.75,
        "5 Columns" to 0.6,
        "6 Columns" to 0.5
    )

    private val componentNameMap = mutableMapOf<String, String>()

    private val margins = Margins(
        left = 10.0,
        right = 10.0,
        top = 40.0,
        bottom = 10.0
    )

    private val effectiveColumnCount: Int
        get() {
            val availableWidth = screenWidth - margins.left - margins.right
            val editorScale = editorContainer.scale
            val scaledEditorWidth = BaseComponentEditor.STANDARD_WIDTH * editorScale
            val scaledSpacing = EDITOR_SPACING * editorScale
            val editorTotalWidth = scaledEditorWidth + scaledSpacing
            val maxPossibleColumns = maxOf(1, (availableWidth / editorTotalWidth).toInt())

            return minOf(maxPossibleColumns, maxAllowedColumns)
        }

    override fun getScreenX() = screenX
    override fun getScreenY() = screenY
    override fun getScreenWidth() = screenWidth
    override fun getScreenHeight() = screenHeight
    override fun getScreenIdentifier(): String = "component_editor_screen"

    private val activeEditors = mutableListOf<BaseComponentEditor>()
    private val editorRegistry = EditorRegistry()
    private val pendingChanges = mutableMapOf<ComponentType<*>, PendingChange>()

    private data class PendingChange(
        val value: Any? = null,
        val isRemoval: Boolean = false
    )

    private val saveButton = CollectorButton.createSaveButton(
        x = screenX + screenWidth - 65,
        y = screenY + margins.top - 5
    ) {
        saveChanges()
    }

    private val componentGrid = SelectableGrid(
        x = screenX + margins.left + 5,
        y = screenY + margins.top - 5,
        width = screenWidth - margins.left - margins.right - 70
    ) { componentName ->
        handleComponentSelection(componentName)
    }

    private val columnDropdown = UIDropdown(
        x = screenX + screenWidth - 175,
        y = screenY + margins.top + 80,
        width = 120.0,
        label = "Columns",
        onSelect = { selection ->
            val scale = columnConfigs[selection] ?: 1.0
            editorContainer.setScale(scale)
            repositionEditorsMultiColumn()
        }
    )

    private var editorContainer = UIContainer(
        x = screenX + margins.left,
        y = screenY + margins.top + componentGrid.height + 10.0,
        width = screenWidth - margins.left - margins.right,
        height = screenHeight - margins.top - margins.bottom - componentGrid.height - 10.0,
        renderBackground = true,
        backgroundColor = Color(0.1f, 0.1f, 0.1f, 0.5f),
        enableScissoring = true,
        enableScrolling = true,
        scaleOrigin = UIContainer.ScaleOrigin.FIRST_COMPONENT
    )

    init {
        loadSavedPosition()

        columnDropdown.items = columnConfigs.keys.toList()
        columnDropdown.currentSelection = "3 Columns"

        components.add(saveButton)
        components.add(componentGrid)
        components.add(columnDropdown)
        components.add(editorContainer)

        updateComponentList()
        updateComponentPositions()
    }

    private fun updateComponentPositions() {
        saveButton.setPosition(
            screenX + screenWidth - margins.right - 65,
            screenY + margins.top - 5
        )

        columnDropdown.setPosition(
            screenX + screenWidth - margins.right - 175,
            screenY + margins.top + 80
        )

        componentGrid.setPosition(
            screenX + margins.left + 5,
            screenY + margins.top - 5
        )

        editorContainer.setPosition(
            screenX + margins.left,
            screenY + margins.top + componentGrid.height + 10.0
        )

        editorContainer.width = screenWidth - margins.left - margins.right
        editorContainer.height = screenHeight - margins.top - margins.bottom - componentGrid.height - 10.0

        repositionEditorsMultiColumn()
    }
    private fun repositionEditorsMultiColumn() {
        if (activeEditors.isEmpty()) {
            editorContainer.clearComponents()
            return
        }

        val previousScrollY = editorContainer.getScrollY()
        editorContainer.clearComponents()

        val startY = 10.0
        val spacing = EDITOR_SPACING
        val editorWidth = BaseComponentEditor.STANDARD_WIDTH
        val columnWidth = editorWidth + spacing
        val positionOffset = -5.0

        val columnBottoms = DoubleArray(effectiveColumnCount) { startY }

        val sortedEditors = activeEditors.sortedByDescending { editor ->
            val editorState = editorRegistry.getEditorState(editor.componentType)
            editorState?.columnSpan ?: 1
        }

        var maxBottom = startY
        var largestEditor = 0.0

        sortedEditors.forEach { editor ->
            val editorState = editorRegistry.getEditorState(editor.componentType)
            val columnSpan = (editorState?.columnSpan ?: 1).coerceAtMost(effectiveColumnCount)
            val editorHeight = editorState?.customHeight ?: BaseComponentEditor.STANDARD_HEIGHT

            largestEditor = maxOf(largestEditor, editorHeight)

            var bestStartCol = 0
            var lowestY = Double.MAX_VALUE

            val maxStartCol = effectiveColumnCount - columnSpan
            for (startCol in 0..maxStartCol) {
                var highestBottom = columnBottoms[startCol]
                for (i in 1 until columnSpan) {
                    highestBottom = maxOf(highestBottom, columnBottoms[startCol + i])
                }

                if (highestBottom < lowestY) {
                    lowestY = highestBottom
                    bestStartCol = startCol
                }
            }

            val posX = (bestStartCol * columnWidth) + 10.0 + positionOffset
            val posY = lowestY

            editor.setPosition(editorContainer.x + posX, editorContainer.y + posY)
            editorContainer.addComponent(editor)

            val newBottom = lowestY + editorHeight + spacing
            for (i in 0 until columnSpan) {
                if (bestStartCol + i < columnBottoms.size) {
                    columnBottoms[bestStartCol + i] = newBottom
                }
            }

            maxBottom = maxOf(maxBottom, newBottom)
        }

        val minimumContentHeight = startY + largestEditor + spacing
        val finalContentHeight = maxOf(maxBottom + spacing, minimumContentHeight)

        editorContainer.setCustomContentHeight(finalContentHeight * 1.3)

        editorContainer.setScrollY(previousScrollY)
    }

    override fun updateScreenPosition(newX: Double, newY: Double) {
        screenX = newX
        screenY = newY
        updateComponentPositions()
    }

    private fun handleComponentSelection(cleanComponentName: String) {
        val fullComponentName = componentNameMap[cleanComponentName] ?: cleanComponentName

        val componentType = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(fullComponentName)
        if (componentType != null && !CollectorItem.EXCLUDED_COMPONENTS.contains(componentType)) {
            if (activeEditors.any { it.componentType == fullComponentName }) {
                activeEditors.removeIf { it.componentType == fullComponentName }
                componentGrid.setActiveItem(cleanComponentName, false)
                repositionEditorsMultiColumn()
            } else {
                addEditor(fullComponentName)
                componentGrid.setActiveItem(cleanComponentName, true)
            }
        }
    }

    private fun addEditor(componentName: String) {
        val componentType = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(componentName) ?: return
        if (CollectorItem.EXCLUDED_COMPONENTS.contains(componentType)) return

        val editor = NineComponentEditorFactory.createEditor(
            componentType = componentType,
            registry = editorRegistry,
            item = item,
            x = 0.0,
            y = 0.0
        ) ?: return

        editor.initializeItem(item)

        editor.onValueChange = { newValue ->
            @Suppress("UNCHECKED_CAST")
            pendingChanges[componentType as ComponentType<Any>] = PendingChange(value = newValue)
            updateComponentValue(componentName, newValue?.toString() ?: "Not Set")
        }

        activeEditors.add(editor)
        ninehack.logChat("Added editor for: $componentName")
        repositionEditorsMultiColumn()
    }

    private fun updateComponentList() {
        val gridItems = mutableListOf<SelectableGrid.GridItem>()
        val itemStack = CollectorItem.toItemStack(item)

        componentNameMap.clear()

        win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getAllDataComponentTypes().forEach { type ->
            if (!CollectorItem.EXCLUDED_COMPONENTS.contains(type)) {
                val fullComponentName = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentName(type)
                val cleanComponentName = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getCleanedComponentName(type)

                componentNameMap[cleanComponentName] = fullComponentName

                val hasEditor = NineComponentEditorFactory.hasEditorFor(fullComponentName)
                val value = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(itemStack, type)
                    .map { it.toString() }
                    .orElse("Not Set")

                gridItems.add(
                    SelectableGrid.GridItem(
                        name = cleanComponentName,
                        isSelectable = hasEditor,
                        description = value
                    )
                )
            }
        }

        componentGrid.setItems(gridItems)
    }

    private fun updateComponentValue(componentName: String, value: String) {
        componentGrid.updateItemDescription(componentName, value)
    }

    private fun saveChanges() {
        try {
            val newComponents = ComponentMap.builder().apply {
                item.components.forEach { component ->
                    val type = component.type()
                    if (!pendingChanges.containsKey(type)) {
                        @Suppress("UNCHECKED_CAST")
                        add(type as ComponentType<Any>, component.value())
                    }
                }
                pendingChanges.forEach { (type, change) ->
                    if (!change.isRemoval) {
                        @Suppress("UNCHECKED_CAST")
                        add(type as ComponentType<Any>, change.value)
                    }
                }
            }.build()

            val updatedItem = item.createModified(components = newComponents)

            CollectorCollection.getCurrent()?.let { collection ->
                collection.updateItem(item, updatedItem)
                collection.save()
            }

            pendingChanges.clear()
            updateComponentList()

            activeEditors.forEach { editor ->
                val componentType = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(editor.componentType)
                if (componentType != null) {
                    val value = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(
                        CollectorItem.toItemStack(updatedItem),
                        componentType
                    ).orElse(null)

                    val cleanName = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getCleanedComponentName(componentType)
                    updateComponentValue(cleanName, value?.toString() ?: "Not Set")
                }
            }

            ninehack.logChat("Changes saved successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            ninehack.logChat("Failed to save changes: ${e.message}")
        }
    }

    override fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderBackground(context, screenX, screenY, screenWidth, screenHeight)
            renderTitleBar(context, screenX, screenY, screenWidth, "Component Editor - ${item.customName}")

            components.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun close() {
        mc.setScreen(CollectorScreen())
    }

    private data class Margins(val left: Double, val right: Double, val top: Double, val bottom: Double)
}