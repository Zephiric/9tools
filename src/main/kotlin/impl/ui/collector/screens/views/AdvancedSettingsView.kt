package impl.ui.collector.screens.views

import net.minecraft.client.gui.DrawContext
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.data.CollectorTags
import impl.ui.collector.UIComponent
import impl.ui.collector.tags.TagEditor
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.SelectableGrid
import java.awt.Color

class AdvancedSettingsView(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double
) : UIComponent {
    private val components = mutableListOf<UIComponent>()

    private val gridWidth = 180.0
    private val gridHeight = 150.0
    private val padding = 10.0

    private var groupsGrid: SelectableGrid
    private var tagsGrid: SelectableGrid

    private var newGroupButton: CollectorButton
    private var newTagButton: CollectorButton

    private var deleteGroupButton: CollectorButton? = null
    private var deleteTagButton: CollectorButton? = null

    private var groupEditor: TagEditor? = null
    private var tagEditor: TagEditor? = null

    private var selectedGroup: String? = null
    private var selectedTag: String? = null

    private var isDeleteGroupEnabled = false
    private var isDeleteTagEnabled = false

    init {
        newGroupButton = CollectorButton(
            x = x + padding,
            y = y,
            width = 80.0,
            height = 20.0,
            text = "New Group",
            onClickAction = { onNewGroupClick() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        components.add(newGroupButton)

        newTagButton = CollectorButton(
            x = x + padding + 90.0,
            y = y,
            width = 80.0,
            height = 20.0,
            text = "New Tag",
            onClickAction = { onNewTagClick() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        components.add(newTagButton)

        groupsGrid = SelectableGrid(
            x = x + padding,
            y = y + 80,
            width = gridWidth,
            height = 30.0,
            onSelect = { groupName -> onGroupSelected(groupName) }
        )
        components.add(groupsGrid)

        tagsGrid = SelectableGrid(
            x = x + padding,
            y = y + 150,
            width = gridWidth,
            height = 30.0,
            onSelect = { tagName -> onTagSelected(tagName) }
        )
        components.add(tagsGrid)

        updateDeleteButtons()

        loadTagGroups()
    }

    private fun updateDeleteButtons() {
        components.removeIf { it === deleteGroupButton || it === deleteTagButton }

        deleteGroupButton = CollectorButton(
            x = x + padding,
            y = y + 30,
            width = 80.0,
            height = 20.0,
            text = "Delete Group",
            onClickAction = {
                if (isDeleteGroupEnabled) {
                    onDeleteGroupClick()
                }
            },
            type = if (isDeleteGroupEnabled) CollectorButton.ButtonType.NEGATIVE else CollectorButton.ButtonType.NEUTRAL
        )
        components.add(deleteGroupButton!!)

        deleteTagButton = CollectorButton(
            x = x + padding + 90.0,
            y = y + 30,
            width = 80.0,
            height = 20.0,
            text = "Delete Tag",
            onClickAction = {
                if (isDeleteTagEnabled) {
                    onDeleteTagClick()
                }
            },
            type = if (isDeleteTagEnabled) CollectorButton.ButtonType.NEGATIVE else CollectorButton.ButtonType.NEUTRAL
        )
        components.add(deleteTagButton!!)
    }

    private fun loadTagGroups() {
        val groups = CollectorTags.getTagGroups()
        val items = groups.map { group ->
            SelectableGrid.GridItem(
                name = group.group,
                isSelectable = true,
                description = "${group.tags.size} tags"
            )
        }
        groupsGrid.setItems(items)

        selectedGroup?.let { groupName ->
            if (groups.any { it.group == groupName }) {
                groupsGrid.setActiveItem(groupName, true)
            } else {
                selectedGroup = null
            }
        }

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun loadTagsForGroup(groupName: String) {
        clearTagSelection()

        val group = CollectorTags.getTagGroup(groupName) ?: return
        val items = group.tags.map { tag ->
            SelectableGrid.GridItem(
                name = tag,
                isSelectable = true,
                description = "Tag in $groupName"
            )
        }
        tagsGrid.setItems(items)

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun clearTagsGrid() {
        clearTagSelection()

        tagsGrid.setItems(emptyList())

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun clearTagSelection() {
        selectedTag?.let { tagName ->
            tagsGrid.setActiveItem(tagName, false)
        }

        selectedTag = null

        removeTagEditor()

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun onGroupSelected(groupName: String) {
        if (selectedGroup == groupName) {
            selectedGroup = null
            groupsGrid.setActiveItem(groupName, false)
            clearTagsGrid()
            removeGroupEditor()
        } else {

            selectedGroup?.let {
                groupsGrid.setActiveItem(it, false)
            }

            clearTagSelection()

            selectedGroup = groupName
            groupsGrid.setActiveItem(groupName, true)

            loadTagsForGroup(groupName)

            createGroupEditor(groupName)
        }

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun onTagSelected(tagName: String) {
        val groupName = selectedGroup ?: return

        if (selectedTag == tagName) {
            clearTagSelection()
        } else {

            selectedTag?.let {
                tagsGrid.setActiveItem(it, false)
            }

            selectedTag = tagName
            tagsGrid.setActiveItem(tagName, true)

            createTagEditor(groupName, tagName)
        }

        isDeleteGroupEnabled = selectedGroup != null
        isDeleteTagEnabled = selectedTag != null
        updateDeleteButtons()
    }

    private fun onNewGroupClick() {
        var newGroupName = "New Group"
        var counter = 1

        while (CollectorTags.getTagGroup(newGroupName) != null) {
            newGroupName = "New Group ${counter++}"
        }

        CollectorTags.createTagGroup(newGroupName)

        loadTagGroups()

        onGroupSelected(newGroupName)
    }

    private fun onNewTagClick() {
        val groupName = selectedGroup
        if (groupName == null) {
            return
        }

        var newTagName = "New Tag"
        var counter = 1
        val group = CollectorTags.getTagGroup(groupName)

        while (group?.tags?.contains(newTagName) == true) {
            newTagName = "New Tag ${counter++}"
        }

        CollectorTags.addTag(groupName, newTagName)
        loadTagsForGroup(groupName)
        loadTagGroups()
        groupsGrid.setActiveItem(groupName, true)
        onTagSelected(newTagName)
    }

    private fun onDeleteGroupClick() {
        val groupName = selectedGroup ?: return

        if (CollectorTags.removeTagGroup(groupName)) {
            selectedGroup = null
            clearTagsGrid()
            removeGroupEditor()
            loadTagGroups()
        }
    }

    private fun onDeleteTagClick() {
        val groupName = selectedGroup ?: return
        val tagName = selectedTag ?: return

        if (CollectorTags.removeTag(groupName, tagName)) {
            clearTagSelection()
            loadTagsForGroup(groupName)
        }
    }

    private fun createGroupEditor(groupName: String) {
        removeGroupEditor()

        groupEditor = TagEditor(
            x = x + 200,
            y = y + 20,
            isGroup = true,
            groupName = groupName,
            onSave = { newName ->
                if (newName != groupName) {
                    CollectorTags.renameTagGroup(groupName, newName)

                    selectedGroup = newName
                }
                loadTagGroups()
                loadTagsForGroup(newName)
            },
            onCancel = {
                removeGroupEditor()
                selectedGroup?.let { groupName ->
                    groupsGrid.setActiveItem(groupName, false)
                }
                selectedGroup = null
                clearTagsGrid()
            }
        )

        components.add(groupEditor!!)
    }

    private fun createTagEditor(groupName: String, tagName: String) {
        removeTagEditor()

        tagEditor = TagEditor(
            x = x + 200,
            y = y + 100,
            isGroup = false,
            groupName = groupName,
            tagName = tagName,
            onSave = { newName ->
                if (newName != tagName) {
                    CollectorTags.renameTag(groupName, tagName, newName)

                    selectedTag = newName
                }

                loadTagsForGroup(groupName)

                tagsGrid.setActiveItem(newName, true)
                selectedTag = newName
            },
            onCancel = {
                clearTagSelection()
            }
        )

        components.add(tagEditor!!)
    }

    private fun removeGroupEditor() {
        groupEditor?.let {
            components.remove(it)
            groupEditor = null
        }
    }

    private fun removeTagEditor() {
        tagEditor?.let {
            components.remove(it)
            tagEditor = null
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawTextWithShadow(
            mc.textRenderer,
            "Tag Groups",
            (x + padding).toInt(),
            (y + 60).toInt(),
            Color.WHITE.rgb
        )

        context.drawTextWithShadow(
            mc.textRenderer,
            "Tags",
            (x + padding).toInt(),
            (y + 130).toInt(),
            Color.WHITE.rgb
        )

        components.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return components.any { it.contains(mouseX, mouseY) && it.onClick(mouseX, mouseY, button) }
    }

    override fun onDrag(mouseX: Double, mouseY: Double, deltaX: Double, deltaY: Double, button: Int): Boolean {
        return components.any { it.onDrag(mouseX, mouseY, deltaX, deltaY, button) }
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return components.any { it.onScroll(mouseX, mouseY, amount) }
    }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return components.any { it.onKeyPress(keyCode, scanCode, modifiers) }
    }

    override fun onCharTyped(chr: Char, modifiers: Int): Boolean {
        return components.any { it.onCharTyped(chr, modifiers) }
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        components.forEach { it.setPosition(it.x + deltaX, it.y + deltaY) }
    }

    override fun getChildComponents(): List<UIComponent> = components
}