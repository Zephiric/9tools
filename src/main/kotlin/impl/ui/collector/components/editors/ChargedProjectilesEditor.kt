package impl.ui.collector.components.editors

import impl.ui.collector.components.BaseComponentEditor
import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ChargedProjectilesComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.components.EditorRegistry
import impl.ui.collector.components.EditorState
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorToggleButton
import impl.ui.collector.utils.UIDropdown
import java.awt.Color

// This editor will need to use either a right click menu for effect/entity/block presets or it will quickly get out of hand.

class ChargedProjectilesEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialComponent: ChargedProjectilesComponent? = null
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:charged_projectiles"

    private val customHeight = 180.0

    private var isEnabled: Boolean = initialComponent != null
    private val projectiles = mutableListOf<ItemStack>()

    private var selectedProjectileIndex: Int? = null
    private var scrollOffset = 0
    private val maxVisibleProjectiles = 6

    private var selectedProjectileType: ItemStack? = null

    private lateinit var projectileTypeDropdown: UIDropdown
    private lateinit var addProjectileButton: CollectorButton
    private lateinit var deleteProjectileButton: CollectorButton
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)
        this.height = customHeight

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = initialComponent,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )

        initializeProjectilesUI()

        if (initialComponent != null && !initialComponent.isEmpty()) {
            projectiles.clear()
            projectiles.addAll(initialComponent.getProjectiles())
        }

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + height - 25.0,
            label = "Enable",
            description = "Enable/disable charged projectiles",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled
            updateComponent()
        }
        editorComponents.add(enableToggle)
    }

    private fun initializeProjectilesUI() {
        val validProjectiles = listOf(
            Items.ARROW,
            Items.TIPPED_ARROW,
            Items.SPECTRAL_ARROW,
            Items.FIREWORK_ROCKET,
            //This has to be a joke right...
            Items.DIRT,
            Items.LEATHER_BOOTS,
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.WOODEN_HOE,
            Items.BLACK_SHULKER_BOX
            //Imagine running around at spawn and shooting shitkits at new players
        )

        projectileTypeDropdown = UIDropdown(
            x = x + 5.0,
            y = y + 20.0,
            width = 90.0,
            height = 20.0,
            label = "Projectile",
            maxVisibleItems = 8,
            onSelect = { selected ->
                validProjectiles.forEach { item ->
                    val itemName = formatItemName(item.translationKey)
                    if (selected == itemName) {
                        selectedProjectileType = ItemStack(item)
                        return@forEach
                    }
                }
            }
        )

        val projectileOptions = mutableListOf<String>().apply {
            validProjectiles.forEach { item ->
                add(formatItemName(item.translationKey))
            }
        }

        projectileTypeDropdown.items = projectileOptions
        if (projectileOptions.isNotEmpty()) {
            projectileTypeDropdown.currentSelection = projectileOptions.first()
            selectedProjectileType = ItemStack(validProjectiles.first())
        }
        editorComponents.add(projectileTypeDropdown)

        addProjectileButton = CollectorButton(
            x = x + 160.0,
            y = y + 20.0,
            width = 30.0,
            height = 20.0,
            text = "Add",
            onClickAction = { addProjectile() },
            type = CollectorButton.ButtonType.POSITIVE
        )
        editorComponents.add(addProjectileButton)

        deleteProjectileButton = CollectorButton(
            x = x + 160.0,
            y = y + 155.0,
            width = 30.0,
            height = 20.0,
            text = "Del",
            onClickAction = { deleteSelectedProjectile() },
            type = CollectorButton.ButtonType.NEGATIVE
        )
        editorComponents.add(deleteProjectileButton)
    }

    private fun formatItemName(translationKey: String): String {
        val name = translationKey.removePrefix("item.minecraft.")
        return name.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    private fun addProjectile() {
        selectedProjectileType?.let { projectile ->
            projectiles.add(projectile.copy())
            updateComponent()
        }
    }

    private fun deleteSelectedProjectile() {
        selectedProjectileIndex?.let { index ->
            if (index >= 0 && index < projectiles.size) {
                projectiles.removeAt(index)
                selectedProjectileIndex = null
                updateComponent()

                if (scrollOffset > 0 && projectiles.size <= maxVisibleProjectiles) {
                    scrollOffset = 0
                } else if (scrollOffset > 0 && scrollOffset >= projectiles.size - maxVisibleProjectiles) {
                    scrollOffset = (projectiles.size - maxVisibleProjectiles).coerceAtLeast(0)
                }
            }
        }
    }

    private fun updateComponent() {
        if (isEnabled && projectiles.isNotEmpty()) {
            val component = ChargedProjectilesComponent.of(projectiles)
            notifyValueChanged(component)
        } else {
            notifyValueChanged(null)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.3f),
            x, y,
            x + width, y + height,
            2f, 10f
        )

        context.drawTextWithShadow(
            mc.textRenderer,
            Text.literal("Charged Projectiles"),
            (x + 5).toInt(),
            (y + 5).toInt(),
            0xFFFFFF
        )

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            Color(0.2f, 0.2f, 0.2f, 0.4f),
            x + 5, y + 50,
            x + width - 5, y + height - 30,
            2f, 10f
        )

        val visibleProjectiles = projectiles.size.coerceAtMost(maxVisibleProjectiles)
        if (projectiles.isEmpty()) {
            context.drawTextWithShadow(
                mc.textRenderer,
                Text.literal("No projectiles added"),
                (x + 10).toInt(),
                (y + 60).toInt(),
                Color(0.5f, 0.5f, 0.5f, 0.7f).rgb
            )
        } else {
            for (i in 0 until visibleProjectiles) {
                val index = i + scrollOffset
                if (index < projectiles.size) {
                    val projectile = projectiles[index]
                    val yPos = y + 60 + (i * 16)

                    if (selectedProjectileIndex == index) {
                        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                            context.matrices,
                            Color(0.3f, 0.3f, 0.5f, 0.5f),
                            x + 6, yPos - 2,
                            x + width - 6, yPos + 14,
                            1f, 5f
                        )
                    }

                    context.drawItem(projectile, (x + 10).toInt(), yPos.toInt())

                    context.drawTextWithShadow(
                        mc.textRenderer,
                        projectile.getName(),
                        (x + 30).toInt(),
                        (yPos + 4).toInt(),
                        0xFFFFFF
                    )
                }
            }

            if (projectiles.size > maxVisibleProjectiles) {
                if (scrollOffset > 0) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▲"),
                        (x + width / 2).toInt(),
                        (y + 50).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }

                if (scrollOffset + maxVisibleProjectiles < projectiles.size) {
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal("▼"),
                        (x + width / 2).toInt(),
                        (y + height - 40).toInt(),
                        Color(1f, 1f, 1f, 0.7f).rgb
                    )
                }
            }
        }

        editorComponents.forEach { it.render(context, mouseX, mouseY, delta) }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 50 && mouseY <= y + height - 30) {

            val relativeY = mouseY - (y + 60)
            val projectileIndex = (relativeY / 16).toInt() + scrollOffset

            if (projectileIndex >= 0 && projectileIndex < projectiles.size) {
                selectedProjectileIndex = projectileIndex
                return true
            } else {
                selectedProjectileIndex = null
            }
        } else if (!contains(mouseX, mouseY)) {
            selectedProjectileIndex = null
        }

        return super.onClick(mouseX, mouseY, button)
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (mouseX >= x + 5 && mouseX <= x + width - 5 &&
            mouseY >= y + 50 && mouseY <= y + height - 30) {

            if (amount < 0 && scrollOffset + maxVisibleProjectiles < projectiles.size) {
                scrollOffset++
                return true
            }

            if (amount > 0 && scrollOffset > 0) {
                scrollOffset--
                return true
            }
        }

        return super.onScroll(mouseX, mouseY, amount)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        val stack = CollectorItem.toItemStack(collectorItem)
        val componentOpt = stack.get(DataComponentTypes.CHARGED_PROJECTILES)

        if (componentOpt != null && !componentOpt.isEmpty()) {
            isEnabled = true
            projectiles.clear()
            projectiles.addAll(componentOpt.getProjectiles())
            selectedProjectileIndex = null
            scrollOffset = 0
        } else {
            isEnabled = false
            projectiles.clear()
            selectedProjectileIndex = null
            scrollOffset = 0
        }

        enableToggle.state = isEnabled

        registry.updateState(
            componentType,
            EditorState(
                componentType = componentType,
                value = if (isEnabled && projectiles.isNotEmpty()) ChargedProjectilesComponent.of(projectiles) else null,
                isValid = true,
                columnSpan = 1,
                customHeight = height
            )
        )
    }
}