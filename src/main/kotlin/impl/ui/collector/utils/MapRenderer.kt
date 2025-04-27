package win.ninegang.ninetools.impl.ui.collector.components

import com.google.gson.JsonObject
import net.minecraft.block.MapColor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.render.Engine2d
import impl.ui.collector.data.CollectorMaps
import impl.ui.collector.UIComponent
import java.awt.Color

/**
 * UI Component for rendering Minecraft maps
 */
class MapRenderer(
    override var x: Double,
    override var y: Double,
    override var width: Double,
    override var height: Double,
    private val mapId: Int,
    private val serverTag: String? = null,
    private val onClick: ((MapRenderer) -> Unit)? = null
) : UIComponent {

    var title: String? = null
    private var mapData: JsonObject? = null
    private var mapTextureId: Identifier? = null
    private var mapTexture: NativeImageBackedTexture? = null
    private var loaded = false
    private var loadAttempted = false
    private var isHovered = false

    init {
        val metadata = CollectorMaps.getMapMetadata(mapId)
        if (metadata != null) {
            title = if (metadata.name.isNotBlank()) metadata.name else "Map #${mapId}"
        } else {
            title = "Map #${mapId}"
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (!loadAttempted) {
            loadMapData()
            loadAttempted = true
        }

        isHovered = contains(mouseX.toDouble(), mouseY.toDouble())

        val backgroundColor = Color(0.15f, 0.15f, 0.15f, 0.7f)

        win.ninegang.ninetools.compat.util.render.Engine2d.renderRoundedQuad(
            context.matrices,
            backgroundColor,
            x, y,
            x + width, y + height,
            4f, 12f
        )

        val padding = 8.0
        val titleHeight = if (title != null) 20.0 else 0.0
        val availableHeight = height - titleHeight - padding * 2
        val mapRenderSize = availableHeight.coerceAtMost(width - padding * 2)

        val mapStartX = x + (width - mapRenderSize) / 2
        val mapStartY = y + padding + titleHeight

        title?.let {
            val titleWidth = mc.textRenderer.getWidth(it)
            context.drawTextWithShadow(
                mc.textRenderer,
                it,
                (x + (width - titleWidth) / 2).toInt(),
                (y + padding).toInt(),
                0xFFFFFF
            )
        }

        if (loaded && mapTextureId != null) {
            val matrix = context.matrices.peek().getPositionMatrix()
            val light = 15728880

            val vertexConsumer = context.vertexConsumers.getBuffer(RenderLayer.getText(mapTextureId!!))

            val startX = mapStartX.toFloat()
            val startY = mapStartY.toFloat()
            val size = mapRenderSize.toFloat()

            vertexConsumer.vertex(matrix, startX, startY + size, 0.0f).color(-1).texture(0.0f, 1.0f).light(light)
            vertexConsumer.vertex(matrix, startX + size, startY + size, 0.0f).color(-1).texture(1.0f, 1.0f).light(light)
            vertexConsumer.vertex(matrix, startX + size, startY, 0.0f).color(-1).texture(1.0f, 0.0f).light(light)
            vertexConsumer.vertex(matrix, startX, startY, 0.0f).color(-1).texture(0.0f, 0.0f).light(light)

            context.draw()
        } else if (loadAttempted) {
            val text = "Map not found"
            val textWidth = mc.textRenderer.getWidth(text)

            context.drawTextWithShadow(
                mc.textRenderer,
                text,
                (x + (width - textWidth) / 2).toInt(),
                (y + height / 2 - 4).toInt(),
                0xFFFFFF
            )
        }
    }

    private fun loadMapData() {
        mapData = CollectorMaps.loadMap(mapId, serverTag)

        if (mapData != null) {
            try {
                val colors = CollectorMaps.getMapColors(mapData!!)

                if (colors != null) {
                    createMapTexture(colors)
                    loaded = true
                }
            } catch (e: Exception) {
                loaded = false
            }
        }
    }

    private fun createMapTexture(colors: ByteArray) {
        try {
            val image = NativeImage(128, 128, false)

            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    val colorIndex = colors[y * 128 + x].toInt() and 0xFF
                    val argb = MapColor.getRenderColor(colorIndex)
                    image.setColorArgb(x, y, argb)
                }
            }

            mapTexture?.close()
            mapTexture = NativeImageBackedTexture(image)

            val identifier = Identifier.of("ninehack", "map_${mapId}")
            mc.textureManager.registerTexture(identifier, mapTexture!!)
            mapTextureId = identifier

        } catch (e: Exception) {
            loaded = false
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (contains(mouseX, mouseY) && button == 0) {
            onClick?.invoke(this)
            return true
        }
        return false
    }

    fun cleanup() {
        mapTexture?.close()
    }

    fun getMapId(): Int = mapId

    fun getServerTag(): String? = serverTag

    fun isLoaded(): Boolean = loaded
}