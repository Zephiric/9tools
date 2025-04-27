package impl.ui.collector.screens.views

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.ComponentType
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.data.CollectorTags
import impl.ui.collector.UIComponent
import impl.ui.collector.components.TooltipHandler
import impl.ui.collector.data.CollectorCollection
import impl.ui.collector.screens.ItemEditScreen
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.MapRenderUtils
import impl.ui.collector.utils.SelectableGrid
import win.ninegang.ninetools.CollectorMod

import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

/**
 * A detail view for a single CollectorItem that implements [UIComponent].
 *
 * The parent screen sets [currentItem] to the item we want to display, or null if none.
 *
 */
class CollectorScreenItemDetailView(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double,
    private val onBack: () -> Unit,
    private val onEdit: (CollectorItem) -> Unit,
    private val onDelete: (CollectorItem) -> Unit,
    private val onSpawn: (CollectorItem) -> Unit,
    private val onViewComponents: (CollectorItem) -> Unit,
    private val onItemUpdated: (CollectorItem, CollectorItem) -> Unit
) : UIComponent {

    var currentItem: CollectorItem? = null
        private set

    private val tooltipHandler = TooltipHandler
    private val buttons = mutableMapOf<String, CollectorButton>()

    private val tagGrids = mutableMapOf<String, SelectableGrid>()
    private var tagsInitialized = false

    fun setItem(item: CollectorItem?) {
        currentItem = item
        buttons.clear()
        tagsInitialized = false
    }

    fun updateItem(newItem: CollectorItem) {
        currentItem?.let { oldItem ->
            if (oldItem != newItem) {
                currentItem = newItem
                onItemUpdated(oldItem, newItem)
            }
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val item = currentItem ?: return

        if (buttons.isEmpty()) {
            initializeButtons()
        }

        if (!tagsInitialized) {
            initializeTagGrids(item)
        }


        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.0f),
            x,
            y + 20,
            x + width,
            y + height,
            3f,
            10f
        )

        renderItemPreview(context, item, mouseX, mouseY)
        renderItemInfo(context, item)
        renderButtons(context, mouseX, mouseY, delta)
        renderTagSection(context, mouseX, mouseY, delta)
    }

    private fun renderTagSection(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val item = currentItem ?: return

        val tagSectionX = x + 10
        val tagSectionY = y + 160

        context.drawTextWithShadow(
            mc.textRenderer,
            "Tags",
            tagSectionX.toInt(),
            tagSectionY.toInt(),
            Color.WHITE.rgb
        )

        var currentY = tagSectionY + 15
        tagGrids.forEach { (groupName, grid) ->
            context.drawTextWithShadow(
                mc.textRenderer,
                groupName,
                (tagSectionX + 5).toInt(),
                currentY.toInt(),
                Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
            )

            grid.setPosition(tagSectionX, currentY + 15)

            grid.render(context, mouseX, mouseY, delta)

            currentY += 15 + grid.height + 5
        }
    }

    private fun initializeTagGrids(item: CollectorItem) {
        tagGrids.clear()

        val tagSectionX = x + 10
        var currentY = y + 150

        val groups = CollectorTags.getTagGroups()

        groups.forEach { group ->
            val gridWidth = width - 30

            val grid = SelectableGrid(
                x = tagSectionX,
                y = currentY,
                width = gridWidth,
                height = 20.0,
                onSelect = { tagName -> toggleTag(group.group, tagName) }
            )

            val tagItems = group.tags.map { tag ->
                SelectableGrid.GridItem(
                    name = tag,
                    isSelectable = true,
                    description = "Tag in ${group.group}"
                )
            }
            grid.setItems(tagItems)

            item.tags.find { it.group == group.group }?.tags?.forEach { tagName ->
                grid.setActiveItem(tagName, true)
            }

            tagGrids[group.group] = grid

            currentY += grid.height + 20
        }

        tagsInitialized = true
    }

    private fun toggleTag(groupName: String, tagName: String) {
        val item = currentItem ?: return

        val hasTag = item.hasTag(groupName, tagName)

        val updatedItem = if (hasTag) {
            item.removeTag(groupName, tagName)
        } else {
            item.addTag(groupName, tagName)
        }

        if (updatedItem !== item) {
            CollectorCollection.getCurrent()?.updateItem(item, updatedItem)

            currentItem = updatedItem

            onItemUpdated(item, updatedItem)

            updateTagActiveStates()
            CollectorCollection.getCurrent()?.save()
        }
    }

    private fun updateTagActiveStates() {
        val item = currentItem ?: return

        tagGrids.forEach { (groupName, grid) ->
            val activeTags = item.tags.find { it.group == groupName }?.tags ?: emptyList()

            grid.getActiveItems().forEach { tagName ->
                grid.setActiveItem(tagName, false)
            }

            activeTags.forEach { tagName ->
                grid.setActiveItem(tagName, true)
            }
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val item = currentItem ?: return false

        for (grid in tagGrids.values) {
            if (grid.contains(mouseX, mouseY) && grid.onClick(mouseX, mouseY, button)) {
                return true
            }
        }

        val previewX = x + 26
        val previewY = y + 23
        val hoverLeft = previewX + 1
        val hoverRight = previewX + 20
        val hoverTop = previewY + 1
        val hoverBottom = previewY + 20


        buttons.values.forEach { btn ->
            if (btn.contains(mouseX, mouseY)) {
                btn.onClick(mouseX, mouseY, button)
                return true
            }
        }
        return false
    }

    override fun setPosition(newX: Double, newY: Double) {
        x = newX
        y = newY
        updateButtonPositions()
    }

    private fun updateButtonPositions() {
        buttons["back"]?.setPosition(
            x + 10,
            y + 30
        )

        val startX = x + width - 50
        val startY = y + height - 40

        buttons["delete"]?.setPosition(startX, startY)
        buttons["edit"]?.setPosition(startX, startY)
        buttons["spawn"]?.setPosition(startX, startY)
        buttons["components"]?.setPosition(startX, startY)
    }



    private fun initializeButtons() {
        buttons["back"] = CollectorButton.createBackButton(
            x = x + 3,
            y = y + 23
        ) {
            onBack()
        }



        buttons["delete"] = CollectorButton.createDeleteButton(
            x = x + 119,
            y = y + 23
        ) {
            currentItem?.let { onDelete(it) }
        }

        buttons["edit"] = CollectorButton.createEditButton(
            x = x + 50,
            y = y + 23
        ) {
            currentItem?.let { item ->
                mc.setScreen(ItemEditScreen(
                    item = item,
                    isNewItem = false,
                    onSave = { updatedItem ->
                        updateItem(updatedItem)
                        mc.setScreen(CollectorMod.collector)
                    }
                ))
            }
        }

        buttons["spawn"] = CollectorButton.createSpawnButton(
            x = x + 96,
            y = y + 23
        ) {
            currentItem?.let { onSpawn(it) }
        }

        buttons["components"] = CollectorButton.createComponentsButton(
            x = x + 73,
            y = y + 23
        ) {
            currentItem?.let { onViewComponents(it) }
        }
    }

    private fun renderButtons(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        for (btn in buttons.values) {
            btn.render(context, mouseX, mouseY, delta)
        }
    }


    private fun renderItemPreview(context: DrawContext, item: CollectorItem, mouseX: Int, mouseY: Int) {
        val previewX = x + 26
        val previewY = y + 23

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.8f),
            previewX,
            previewY,
            previewX + 20,
            previewY + 20,
            2f,
            10f
        )

        if (MapRenderUtils.isFilledMap(item)) {
            val mapId = MapRenderUtils.getMapIdFromItem(item)

            if (mapId != null) {
                val serverTag = MapRenderUtils.getServerTagForMap(item)

                val rendered = MapRenderUtils.renderMapInItemSlot(
                    context,
                    mapId,
                    serverTag,
                    (previewX + 2).toInt(),
                    (previewY + 2).toInt(),
                    16
                )

                if (!rendered) {
                    val stack = CollectorItem.toItemStack(item)
                    context.drawItem(
                        stack,
                        (previewX + 2).toInt(),
                        (previewY + 3).toInt()
                    )
                }
            } else {
                val stack = CollectorItem.toItemStack(item)
                context.drawItem(
                    stack,
                    (previewX + 2).toInt(),
                    (previewY + 3).toInt()
                )
            }
        } else {
            val stack = CollectorItem.toItemStack(item)
            context.drawItem(
                stack,
                (previewX + 2).toInt(),
                (previewY + 3).toInt()
            )
        }

        val hoverLeft = previewX + 1
        val hoverRight = previewX + 20
        val hoverTop = previewY + 1
        val hoverBottom = previewY + 20
        if (mouseX >= hoverLeft && mouseX <= hoverRight &&
            mouseY >= hoverTop && mouseY <= hoverBottom
        ) {
            val itemStack = CollectorItem.toItemStack(item)
            val lines = tooltipHandler.generateTooltip(itemStack, item)
            tooltipHandler.renderTooltip(context, lines, mouseX, mouseY)
        }
    }


    fun renderItemInfo(context: DrawContext, item: CollectorItem) {
        var headerY = y + 6
        var headerX = x + 53

        CollectorCollection.getCurrent()?.let { collection ->
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal(" : ")
                    .append(
                        Text.literal(collection.name)
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                    ),
                headerX.toInt(),
                headerY.toInt(),
                Color.WHITE.rgb
            )

            headerX += 70
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("| Owner : ")
                    .append(
                        Text.literal(collection.owner)
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                    ),
                headerX.toInt(),
                headerY.toInt(),
                Color.WHITE.rgb
            )
        }

        var leftTextY = y + 50
        val leftTextX = x + 10
        val leftColumnWidth = (width / 2.5).toInt()

        val basicInfo = listOf(
            Triple("Name", item.customName, Formatting.GOLD),
            Triple("Given by", item.givenBy, Formatting.GRAY),
            Triple("Location", item.location, Formatting.GRAY),
            Triple("Received", SimpleDateFormat("MM/dd/yyyy HH:mm").format(Date(item.dateReceived)), Formatting.GRAY)
        )

        basicInfo.forEach { (label, value, color) ->
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("$label: ")
                    .append(
                        Text.literal(value)
                            .setStyle(Style.EMPTY.withColor(color))
                    ),
                leftTextX.toInt(),
                leftTextY.toInt(),
                Color.WHITE.rgb
            )
            leftTextY += 15
        }

        val rightTextX = x + width / 2.1
        var rightTextY = y + 30

        val modifiedComponents = item.components.count { component ->
            val type = component.type()
            if (!CollectorItem.EXCLUDED_COMPONENTS.contains(type)) {
                @Suppress("UNCHECKED_CAST")
                val defaultComponent = CollectorItem.toItemStack(item).components.get(type as ComponentType<Any>)
                defaultComponent != component
            } else false
        }

        context.drawTextWithShadow(
            mc.textRenderer,
            Text.literal("Modified Components: ")
                .append(
                    Text.literal(modifiedComponents.toString())
                        .setStyle(Style.EMPTY.withColor(Formatting.AQUA))
                ),
            rightTextX.toInt(),
            rightTextY.toInt(),
            Color.WHITE.rgb
        )

        rightTextY += 20
        item.components.forEach { component ->
            val type = component.type()
            if (!CollectorItem.EXCLUDED_COMPONENTS.contains(type)) {
                @Suppress("UNCHECKED_CAST")
                val defaultComponent = CollectorItem.toItemStack(item).components.get(type as ComponentType<Any>)
                if (defaultComponent != component) {
                    val componentName = type.toString().substringAfterLast(".")
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("• $componentName")
                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                        rightTextX.toInt(),
                        rightTextY.toInt(),
                        Color.WHITE.rgb
                    )
                    rightTextY += 15
                }
            }
        }

        if (!item.description.isNullOrEmpty()) {
            val descriptionY = y + 120
            val descriptionX = x + 10
            val descriptionMaxWidth = (width / 2 - 50).toInt()
            val maxOverflow = 1.2

            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("Description:"),
                descriptionX.toInt(),
                descriptionY.toInt(),
                Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
            )

            var descriptionLineY = descriptionY + 15
            var currentLine = StringBuilder()
            var currentWidth = 0
            var index = 0

            val words = item.description.split(" ")
            var wordIndex = 0


            while (wordIndex < words.size) {
                val word = words[wordIndex]
                val wordWidth = mc.textRenderer.getWidth(word)
                val spaceWidth = mc.textRenderer.getWidth(" ")

                if (currentWidth + wordWidth + (if (currentWidth > 0) spaceWidth else 0) > descriptionMaxWidth) {
                    if (currentLine.isEmpty()) {
                        var subIndex = 0
                        while (subIndex < word.length) {
                            val char = word[subIndex]
                            val charWidth = mc.textRenderer.getWidth(char.toString())

                            if (currentWidth + charWidth > descriptionMaxWidth) {
                                if (currentLine.isNotEmpty()) {
                                    context.drawTextWithShadow(
                                        mc.textRenderer,
                                        Text.literal(currentLine.toString())
                                            .setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                                        descriptionX.toInt(),
                                        descriptionLineY.toInt(),
                                        Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
                                    )
                                    descriptionLineY += 12
                                }
                                currentLine = StringBuilder()
                                currentWidth = 0
                            }

                            currentLine.append(char)
                            currentWidth += charWidth
                            subIndex++
                        }
                        wordIndex++
                    }
                    else if (currentWidth + wordWidth > descriptionMaxWidth * maxOverflow) {
                        context.drawTextWithShadow(
                            mc.textRenderer,
                            Text.literal(currentLine.toString().trimEnd())
                                .setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                            descriptionX.toInt(),
                            descriptionLineY.toInt(),
                            Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
                        )
                        descriptionLineY += 12
                        currentLine = StringBuilder()
                        currentWidth = 0
                    }
                    else if (wordIndex + 1 < words.size) {
                        val nextWordWidth = mc.textRenderer.getWidth(words[wordIndex + 1])
                        if (nextWordWidth < wordWidth && currentWidth + wordWidth > descriptionMaxWidth * 1.1) {
                            context.drawTextWithShadow(
                                mc.textRenderer,
                                Text.literal(currentLine.toString().trimEnd())
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                                descriptionX.toInt(),
                                descriptionLineY.toInt(),
                                Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
                            )
                            descriptionLineY += 12
                            currentLine = StringBuilder()
                            currentWidth = 0
                        }
                    }
                }
                if (currentWidth > 0) {
                    currentLine.append(" ")
                    currentWidth += spaceWidth
                }
                currentLine.append(word)
                currentWidth += wordWidth
                wordIndex++
            }
            if (currentLine.isNotEmpty()) {
                context.drawTextWithShadow(
                    mc.textRenderer,
                    Text.literal(currentLine.toString().trimEnd()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)),
                    descriptionX.toInt(),
                    descriptionLineY.toInt(),
                    Color(0.8f, 0.8f, 0.8f, 0.9f).rgb
                )
            }
        }
    }
}