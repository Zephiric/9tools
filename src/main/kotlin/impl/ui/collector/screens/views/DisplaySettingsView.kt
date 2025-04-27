package impl.ui.collector.screens.views

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.util.math.Box
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import impl.ui.collector.data.CollectorMaps
import impl.ui.collector.UIComponent
import impl.ui.collector.SettingsManager
import impl.ui.collector.utils.CollectorButton
import impl.ui.collector.utils.CollectorSlider

class DisplaySettingsView(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double
) : UIComponent {

    private val components = mutableListOf<UIComponent>()
    private var scanResult: String? = null
    private var scanResultTime: Long = 0

    init {
        components += CollectorSlider(
            x = x + 20,
            y = y + 30,
            width = width * 0.5,
            height = 20.0,
            label = "Items per Page:",
            value = SettingsManager.itemsPerPage.toDouble(),
            min = 1.0,
            max = 99.0,
            step = 1.0,
            format = { v -> v.toInt().toString() },
            onChange = { newValue ->
                SettingsManager.itemsPerPage = newValue.toInt()
                SettingsManager.saveSettings()
            }
        )

        components += CollectorButton(
            x = x + 20,
            y = y + 70,
            width = 150.0,
            height = 20.0,
            text = "Scan Maps in Render Distance",
            onClickAction = { scanForMapsInRenderDistance() },
            type = CollectorButton.ButtonType.POSITIVE
        )
    }

    private fun scanForMapsInRenderDistance() {
        val player = mc.player ?: run {
            showScanResult("No player found")
            return
        }
        val world = mc.world ?: run {
            showScanResult("No world found")
            return
        }

        val renderDistance = mc.options.viewDistance.value * 16
        val box = Box(
            player.x - renderDistance, player.y - renderDistance, player.z - renderDistance,
            player.x + renderDistance, player.y + renderDistance, player.z + renderDistance
        )

        var count = 0
        val locationDesc = "Found at ${player.blockPos}"

        fun processItemFrame(entity: ItemFrameEntity) {
            if (!entity.containsMap()) return

            val mapIdComponent = entity.getMapId(entity.heldItemStack) ?: return
            val mapId = mapIdComponent.id

            val coords = entity.blockPos
            val name = "Map #$mapId (${coords.x}, ${coords.y}, ${coords.z})"

            if (CollectorMaps.saveMap(mapId, name, locationDesc)) {
                count++
            }
        }

        world.getEntitiesByType(EntityType.ITEM_FRAME, box) { it is ItemFrameEntity }
            .forEach { processItemFrame(it as ItemFrameEntity) }

        world.getEntitiesByType(EntityType.GLOW_ITEM_FRAME, box) { it is ItemFrameEntity }
            .forEach { processItemFrame(it as ItemFrameEntity) }

        showScanResult("Added $count maps to collection from render distance")
    }

    private fun showScanResult(message: String) {
        scanResult = message
        scanResultTime = System.currentTimeMillis()
        ninehack.logChat(message)
    }

    override fun getChildComponents(): List<UIComponent> = components

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawTextWithShadow(
            mc.textRenderer,
            "Display Settings",
            (x + 2).toInt(),
            (y + 8).toInt(),
            0xFFFFFF
        )

        components.forEach { it.render(context, mouseX, mouseY, delta) }

        scanResult?.let {
            val currentTime = System.currentTimeMillis()
            if (currentTime - scanResultTime < 5000) {
                context.drawTextWithShadow(
                    mc.textRenderer,
                    it,
                    (x + 20).toInt(),
                    (y + 100).toInt(),
                    0x00FF00
                )
            } else {
                scanResult = null
            }
        }
    }

    override fun setPosition(newX: Double, newY: Double) {
        val deltaX = newX - x
        val deltaY = newY - y

        x = newX
        y = newY

        components.forEach { component ->
            component.setPosition(
                component.x + deltaX,
                component.y + deltaY
            )
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int) =
        components.any { it.onClick(mouseX, mouseY, button) }

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        deltaX: Double,
        deltaY: Double,
        button: Int
    ) = components.any { it.onDrag(mouseX, mouseY, deltaX, deltaY, button) }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double) =
        components.any { it.onScroll(mouseX, mouseY, amount) }

    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) =
        components.any { it.onKeyPress(keyCode, scanCode, modifiers) }
}