package impl.ui.collector.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.data.CollectorMaps
import impl.ui.collector.utils.CollectorSlider
import win.ninegang.ninetools.impl.ui.collector.components.MapRenderer
import impl.ui.collector.utils.UIScaling
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Currently a test screen for map rendering
 */

class TestScreen : BaseCollectorScreen(Text.literal("Map Browser")) {
    private var screenX = 100.0
    private var screenY = 100.0
    private var screenWidth = 700.0
    private var screenHeight = 450.0

    private var mapSize = 160.0
    private val mapsPerRow = 6
    private val padding = 10.0
    private var selectedMap: MapRenderer? = null

    private var scrollY = 0.0
    private var maxScrollY = 0.0
    private var contentHeight = 0.0
    private val contentAreaY = 70.0
    private val contentAreaHeight get() = screenHeight - contentAreaY - 20.0

    private val allMapIds = CollectorMaps.getSavedMaps()
    private val mapRenderers = mutableListOf<MapRenderer>()

    private val mapSizeSlider = CollectorSlider(
        x = screenX + 30,
        y = screenY + 40,
        width = 200.0,
        height = 20.0,
        label = "Map Size",
        value = mapSize,
        min = 100.0,
        max = 250.0,
        step = 10.0,
        format = { "${it.toInt()}px" },
        onChange = { newSize ->
            mapSize = newSize
            updateMapPositions()
            calculateContentHeight()
        }
    )

    init {
        loadSavedPosition()
        components.add(mapSizeSlider)
        createMapRenderers()
        calculateContentHeight()
    }

    private fun calculateContentHeight() {
        if (mapRenderers.isEmpty()) {
            contentHeight = 0.0
            maxScrollY = 0.0
            return
        }

        val lastIndex = mapRenderers.size - 1
        val lastRow = lastIndex / mapsPerRow
        contentHeight = (lastRow + 1) * (mapSize + padding + 25) + padding

        maxScrollY = max(0.0, contentHeight - contentAreaHeight)
        scrollY = min(scrollY, maxScrollY)
    }

    private fun createMapRenderers() {
        mapRenderers.forEach {
            it.cleanup()
        }
        mapRenderers.clear()
        components.removeIf { it is MapRenderer }

        allMapIds.forEachIndexed { index, mapId ->
            val row = index / mapsPerRow
            val col = index % mapsPerRow

            val mapX = screenX + 20 + col * (mapSize + padding)
            val mapY = screenY + contentAreaY + row * (mapSize + padding + 25)

            val renderer = MapRenderer(
                x = mapX,
                y = mapY,
                width = mapSize,
                height = mapSize + 25,
                mapId = mapId,
                onClick = { clickedRenderer ->
                    selectedMap = clickedRenderer
                }
            )

            mapRenderers.add(renderer)
            components.add(renderer)
        }
    }

    private fun updateMapPositions() {
        mapRenderers.forEachIndexed { index, renderer ->
            val row = index / mapsPerRow
            val col = index % mapsPerRow

            val mapX = screenX + 20 + col * (mapSize + padding)
            val mapY = screenY + contentAreaY + row * (mapSize + padding + 25)

            renderer.x = mapX
            renderer.y = mapY
            renderer.width = mapSize
            renderer.height = mapSize + 25
        }
    }

    override fun getScreenX(): Double = screenX
    override fun getScreenY(): Double = screenY
    override fun getScreenWidth(): Double = screenWidth
    override fun getScreenHeight(): Double = screenHeight

    override fun updateScreenPosition(newX: Double, newY: Double) {
        screenX = newX
        screenY = newY

        mapSizeSlider.setPosition(screenX + 30, screenY + 40)
        updateMapPositions()
    }

    override fun getScreenIdentifier(): String = "map_browser_screen"

    override fun renderScaled(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderBackground(context, screenX, screenY, screenWidth, screenHeight)
            renderTitleBar(context, screenX, screenY, screenWidth, "Map Collection Browser")

            val statText = "${mapRenderers.size} maps loaded"
            context.drawTextWithShadow(
                mc.textRenderer,
                statText,
                (screenX + screenWidth - 20 - mc.textRenderer.getWidth(statText)).toInt(),
                (screenY + 40).toInt(),
                0xAAAAAA
            )

            mapSizeSlider.render(context, mouseX, mouseY, delta)

            val uiScale = UIScaling.getScale()
            val windowScale = mc.window.scaleFactor
            val totalScale = uiScale * windowScale

            val scissorX = (screenX * totalScale).toInt()
            val scissorY = mc.window.height - ((screenY + contentAreaY + contentAreaHeight) * totalScale).toInt()
            val scissorWidth = (screenWidth * totalScale).toInt()
            val scissorHeight = (contentAreaHeight * totalScale).toInt()

            val prevScissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            val prevScissorBox = IntArray(4)
            if (prevScissorEnabled) {
                GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, prevScissorBox)
            }

            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight)

            context.matrices.push()
            context.matrices.translate(0.0f, -scrollY.toFloat(), 0.0f)

            mapRenderers.forEach {
                if (it.y + it.height >= screenY + contentAreaY + scrollY &&
                    it.y - scrollY <= screenY + contentAreaY + contentAreaHeight) {
                    it.render(context, mouseX, (mouseY + scrollY).toInt(), delta)
                }
            }

            context.matrices.pop()

            if (!prevScissorEnabled) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            } else {
                GL11.glScissor(
                    prevScissorBox[0],
                    prevScissorBox[1],
                    prevScissorBox[2],
                    prevScissorBox[3]
                )
            }

            selectedMap?.let { selected ->
                val infoX = screenX + screenWidth - 220
                val infoY = screenY + 70

                win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
                    context.matrices,
                    Color(0.15f, 0.15f, 0.15f, 0.85f),
                    infoX, infoY,
                    infoX + 200, infoY + 150,
                    4f, 12f
                )

                val mapId = selected.getMapId()
                val metadata = CollectorMaps.getMapMetadata(mapId)

                val title = metadata?.name ?: "Map #$mapId"
                val serverName = metadata?.serverName ?: "Unknown Server"
                val description = metadata?.description ?: "No description"

                context.drawTextWithShadow(
                    mc.textRenderer,
                    "Selected Map Info:",
                    (infoX + 10).toInt(),
                    (infoY + 10).toInt(),
                    0xFFFFFF
                )

                context.drawTextWithShadow(
                    mc.textRenderer,
                    "ID: $mapId",
                    (infoX + 10).toInt(),
                    (infoY + 30).toInt(),
                    0xCCCCCC
                )

                context.drawTextWithShadow(
                    mc.textRenderer,
                    "Name: $title",
                    (infoX + 10).toInt(),
                    (infoY + 45).toInt(),
                    0xCCCCCC
                )

                context.drawTextWithShadow(
                    mc.textRenderer,
                    "Server: $serverName",
                    (infoX + 10).toInt(),
                    (infoY + 60).toInt(),
                    0xCCCCCC
                )

                val wrappedDesc = mc.textRenderer.wrapLines(Text.of(description), 180)
                wrappedDesc.forEachIndexed { index, line ->
                    context.drawTextWithShadow(
                        mc.textRenderer,
                        line,
                        (infoX + 10).toInt(),
                        (infoY + 80 + index * 12).toInt(),
                        0xAAAAAA
                    )
                }
            }

            if (allMapIds.isEmpty()) {
                val text = "No saved maps found"
                context.drawTextWithShadow(
                    mc.textRenderer,
                    text,
                    (screenX + screenWidth / 2 - mc.textRenderer.getWidth(text) / 2).toInt(),
                    (screenY + screenHeight / 2).toInt(),
                    0xFFFFFF
                )
            }

    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val scale = UIScaling.getScale()
        val scaledX = mouseX / scale
        val scaledY = mouseY / scale

        if (scaledX >= screenX && scaledX <= screenX + screenWidth &&
            scaledY >= screenY + contentAreaY && scaledY <= screenY + contentAreaY + contentAreaHeight) {

            val scrollAmount = verticalAmount * 20.0
            scrollY = (scrollY - scrollAmount).coerceIn(0.0, maxScrollY)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scale = UIScaling.getScale()
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        if (scaledMouseY >= screenY + contentAreaY &&
            scaledMouseY <= screenY + contentAreaY + contentAreaHeight) {

            val scrolledMouseY = scaledMouseY + scrollY

            for (mapRenderer in mapRenderers) {
                if (mapRenderer.contains(scaledMouseX, scrolledMouseY)) {
                    if (mapRenderer.onClick(scaledMouseX, scrolledMouseY, button)) {
                        return true
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedMap != null) {
                selectedMap = null
                return true
            } else {
                close()
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun close() {
        mapRenderers.forEach { it.cleanup() }
        super.close()
    }
}