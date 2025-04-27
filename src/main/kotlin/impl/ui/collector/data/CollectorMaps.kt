package impl.ui.collector.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.minecraft.component.type.MapIdComponent
import win.ninegang.ninetools.Ninehack
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.*

class CollectorMaps {
    companion object {
        private const val MAPS_DIR = "maps"
        private val baseDir = Paths.get(Ninehack.NAME, "collector", MAPS_DIR)

        init {
            if (!baseDir.exists()) {
                baseDir.createDirectories()
            }
            CollectorServers.initialize()
        }

        private fun determineCurrentServer(): String? {
            return CollectorServers.determineCurrentServer()
        }

        fun saveMap(mapId: Int, name: String, description: String, serverName: String? = null): Boolean {
            try {
                val mapIdComponent = MapIdComponent(mapId)
                val mapState = mc.world?.getMapState(mapIdComponent) ?: return false

                val actualServerName = serverName ?: determineCurrentServer()

                val jsonObj = JsonObject()
                jsonObj.addProperty("version", "0.1")
                jsonObj.addProperty("mapId", mapId)
                jsonObj.addProperty("name", name.ifBlank { "Map ${mapId}" })
                jsonObj.addProperty("description", description)
                jsonObj.addProperty("scale", mapState.scale.toInt())
                jsonObj.addProperty("locked", mapState.locked)

                if (mapState.centerX != 0) {
                    jsonObj.addProperty("centerX", mapState.centerX)
                }
                if (mapState.centerZ != 0) {
                    jsonObj.addProperty("centerZ", mapState.centerZ)
                }

                if (actualServerName != null) {
                    jsonObj.addProperty("serverName", actualServerName)

                    val serversGroup = CollectorTags.getTagGroup("servers")
                    if (serversGroup != null && !serversGroup.tags.contains(actualServerName)) {
                        CollectorTags.addTag("servers", actualServerName)
                    }
                }

                val encodedColors = Base64.getEncoder().encodeToString(mapState.colors)
                jsonObj.addProperty("colors", encodedColors)


                val orderedJson = CollectorJson.reorderFieldsOptimized(
                    jsonObj,
                    CollectorJson.FieldOrder.MAP,
                    largeFields = setOf("colors")
                )

                val mapFile = if (actualServerName != null) {
                    CollectorServers.getMapFilePath(mapId, actualServerName)
                } else {
                    baseDir / "map_${mapId}.json"
                }

                mapFile.parent.createDirectories()

                val gson = GsonBuilder().setPrettyPrinting().create()
                mapFile.writeText(gson.toJson(orderedJson))

                ninehack.logChat(
                    "Saved map ${mapId}" +
                            (if (actualServerName != null) " from server $actualServerName" else "")
                )
                return true
            } catch (e: Exception) {
                ninehack.logChat("Failed to save map ${mapId}: ${e.message}")
                e.printStackTrace()
                return false
            }
        }

        fun loadMap(mapId: Int, serverTag: String? = null): JsonObject? {
            try {
                if (serverTag != null) {
                    val serverMapFile = CollectorServers.getMapFilePath(mapId, serverTag)
                    if (serverMapFile.exists()) {
                        val gson = GsonBuilder().create()
                        return gson.fromJson(serverMapFile.readText(), JsonObject::class.java)
                    }
                }

                val defaultMapFile = baseDir / "map_${mapId}.json"
                if (defaultMapFile.exists()) {
                    val gson = GsonBuilder().create()
                    return gson.fromJson(defaultMapFile.readText(), JsonObject::class.java)
                }

                val serverFolders = CollectorServers.listServerFolders()
                for (folder in serverFolders) {
                    val mapFile = folder.resolve("map_${mapId}.json")
                    if (mapFile.exists()) {
                        val gson = GsonBuilder().create()
                        return gson.fromJson(mapFile.readText(), JsonObject::class.java)
                    }
                }

                return null
            } catch (e: Exception) {
                ninehack.logChat("Failed to load map ${mapId}: ${e.message}")
                e.printStackTrace()
                return null
            }
        }

        fun getMapColors(mapData: JsonObject): ByteArray? {
            try {
                val encodedColors = mapData.get("colors").asString
                return Base64.getDecoder().decode(encodedColors)
            } catch (e: Exception) {
                ninehack.logChat("Failed to decode map colors: ${e.message}")
                e.printStackTrace()
                return null
            }
        }

        fun getSavedMaps(serverTag: String? = null): List<Int> {
            val result = mutableSetOf<Int>()

            if (serverTag != null) {
                val serverFolder = CollectorServers.ensureServerFolder(serverTag)

                serverFolder.toFile()
                    .listFiles { file -> file.isFile && file.name.startsWith("map_") && file.name.endsWith(".json") }
                    ?.forEach { file ->
                        val mapIdStr = file.name.removePrefix("map_").removeSuffix(".json")
                        mapIdStr.toIntOrNull()?.let { result.add(it) }
                    }

                return result.sorted()
            }

            if (baseDir.exists()) {
                baseDir.toFile()
                    .listFiles { file -> file.isFile && file.name.startsWith("map_") && file.name.endsWith(".json") }
                    ?.forEach { file ->
                        val mapIdStr = file.name.removePrefix("map_").removeSuffix(".json")
                        mapIdStr.toIntOrNull()?.let { result.add(it) }
                    }
            }

            val serverFolders = CollectorServers.listServerFolders()
            for (folder in serverFolders) {
                folder.toFile()
                    .listFiles { file -> file.isFile && file.name.startsWith("map_") && file.name.endsWith(".json") }
                    ?.forEach { file ->
                        val mapIdStr = file.name.removePrefix("map_").removeSuffix(".json")
                        mapIdStr.toIntOrNull()?.let { result.add(it) }
                    }
            }

            return result.sorted()
        }

        fun getMapMetadata(mapId: Int): MapMetadata? {
            val mapData = loadMap(mapId) ?: return null

            return try {
                MapMetadata(
                    id = mapData.get("mapId").asInt,
                    name = mapData.get("name").asString,
                    description = mapData.get("description").asString,
                    scale = if (mapData.has("scale")) mapData.get("scale").asInt else 0,
                    locked = if (mapData.has("locked")) mapData.get("locked").asBoolean else false,
                    centerX = if (mapData.has("centerX")) mapData.get("centerX").asInt else 0,
                    centerZ = if (mapData.has("centerZ")) mapData.get("centerZ").asInt else 0,
                    serverName = if (mapData.has("serverName")) mapData.get("serverName").asString else null
                )
            } catch (e: Exception) {
                ninehack.logChat("Failed to parse map metadata: ${e.message}")
                null
            }
        }

        fun deleteMap(mapId: Int): Boolean {
            val mapFile = baseDir / "map_${mapId}.json"
            return if (mapFile.exists()) {
                mapFile.deleteIfExists()
                ninehack.logChat("Deleted map ${mapId}")
                true
            } else {
                false
            }
        }
    }

    data class MapMetadata(
        val id: Int,
        val name: String,
        val description: String,
        val scale: Int = 0,
        val locked: Boolean = false,
        val centerX: Int = 0,
        val centerZ: Int = 0,
        val serverName: String? = null
    )
}