package impl.ui.collector.utils

import impl.ui.collector.data.CollectorItem
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.block.MapColor
import net.minecraft.registry.Registries
import win.ninegang.ninetools.Ninehack
import impl.ui.collector.data.CollectorServers
import java.nio.file.Paths
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.util.Identifier
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.data.CollectorMaps

object MapRenderUtils {
    private val textureCache = mutableMapOf<String, CachedTexture>()

    private const val MAX_CACHE_SIZE = 50

    private data class CachedTexture(
        val textureId: Identifier,
        val texture: NativeImageBackedTexture,
        var lastUsed: Long = System.currentTimeMillis()
    )

    fun renderMapInItemSlot(
        context: DrawContext,
        mapId: Int,
        serverTag: String?,
        x: Int,
        y: Int,
        size: Int
    ): Boolean {
        val mapData = if (serverTag != null) {
            val serverMapFile = CollectorServers.getMapFilePath(mapId, serverTag)
            if (serverMapFile.exists()) {
                val gson = GsonBuilder().create()
                gson.fromJson(serverMapFile.readText(), JsonObject::class.java)
            } else {
                null
            }
        } else {
            val defaultMapFile = Paths.get(Ninehack.NAME, "collector", "maps") / "map_${mapId}.json"
            if (defaultMapFile.exists()) {
                val gson = GsonBuilder().create()
                gson.fromJson(defaultMapFile.readText(), JsonObject::class.java)
            } else {
                null
            }
        }

        if (mapData == null) return false

        try {
            val colors = CollectorMaps.getMapColors(mapData) ?: return false
            val cacheKey = "${mapId}_${serverTag ?: "default"}"
            val textureId = getOrCreateTexture(cacheKey, mapId, serverTag, colors)

            val matrix = context.matrices.peek().getPositionMatrix()
            val light = 15728880

            val vertexConsumer = context.vertexConsumers.getBuffer(RenderLayer.getText(textureId))

            vertexConsumer.vertex(matrix, x.toFloat(), y.toFloat() + size, 0.0f).color(-1).texture(0.0f, 1.0f).light(light)
            vertexConsumer.vertex(matrix, x.toFloat() + size, y.toFloat() + size, 0.0f).color(-1).texture(1.0f, 1.0f).light(light)
            vertexConsumer.vertex(matrix, x.toFloat() + size, y.toFloat(), 0.0f).color(-1).texture(1.0f, 0.0f).light(light)
            vertexConsumer.vertex(matrix, x.toFloat(), y.toFloat(), 0.0f).color(-1).texture(0.0f, 0.0f).light(light)

            context.draw()

            if (serverTag != null) {
                val shortTag = serverTag.take(3)
                context.drawText(
                    mc.textRenderer,
                    shortTag,
                    x + 1,
                    y + 1,
                    0xFFFFFF,
                    false
                )
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun getOrCreateTexture(cacheKey: String, mapId: Int, serverTag: String?, colors: ByteArray): Identifier {
        val cachedTexture = textureCache[cacheKey]
        if (cachedTexture != null) {
            cachedTexture.lastUsed = System.currentTimeMillis()
            return cachedTexture.textureId
        }

        if (textureCache.size >= MAX_CACHE_SIZE) {
            val oldestKey = textureCache.entries
                .minByOrNull { it.value.lastUsed }
                ?.key

            oldestKey?.let {
                cleanupTexture(it)
            }
        }

        val image = NativeImage(128, 128, false)

        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val colorIndex = colors[y * 128 + x].toInt() and 0xFF
                val argb = MapColor.getRenderColor(colorIndex)
                image.setColorArgb(x, y, argb)
            }
        }

        val texture = NativeImageBackedTexture(image)
        val textureId = Identifier.of("ninehack", "map_item_$cacheKey")
        mc.textureManager.registerTexture(textureId, texture)

        textureCache[cacheKey] = CachedTexture(textureId, texture)

        return textureId
    }

    private fun cleanupTexture(cacheKey: String) {
        val cachedTexture = textureCache.remove(cacheKey)
        cachedTexture?.texture?.close()
    }

    fun cleanupAllTextures() {
        textureCache.forEach { (_, cachedTexture) ->
            cachedTexture.texture.close()
        }
        textureCache.clear()
    }

    fun getMapIdFromItem(item: CollectorItem): Int? {
        return item.components.find { it.type() == DataComponentTypes.MAP_ID }
            ?.let { it.value() as? MapIdComponent }
            ?.id
    }

    fun getServerTagForMap(item: CollectorItem): String? {
        return item.tags.find { it.group == "servers" }?.tags?.firstOrNull()
    }

    fun isFilledMap(item: CollectorItem): Boolean {
        return Registries.ITEM.getId(item.baseItem).toString() == "minecraft:filled_map"
    }
}