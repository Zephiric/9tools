package impl.ui.collector.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import win.ninegang.ninetools.Ninehack
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

data class ServerEntry(
    val tag: String,
    val serverIps: List<String>
) {
    companion object {
        val CODEC: Codec<ServerEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("tag").forGetter { it.tag },
                Codec.list(Codec.STRING).fieldOf("serverIps").forGetter { it.serverIps }
            ).apply(instance) { tag, serverIps ->
                ServerEntry(tag, serverIps)
            }
        }
    }
}

object CollectorServers {
    private val baseDir = Paths.get(Ninehack.NAME, "collector", "servers")
    private val registryPath = baseDir / "registry.json"
    private val mapsDir = Paths.get(Ninehack.NAME, "collector", "maps")
    private const val CURRENT_VERSION = "1.0"
    private val servers = mutableListOf<ServerEntry>()

    fun initialize() {
        if (!baseDir.exists()) {
            baseDir.createDirectories()
        }

        if (!mapsDir.exists()) {
            mapsDir.createDirectories()
        }

        if (registryPath.exists()) {
            loadRegistry()
        } else {
            createDefaultRegistry()
            saveRegistry()
        }
    }

    private fun createDefaultRegistry() {
        servers.add(
            ServerEntry(
                tag = "9b9t",
                serverIps = listOf("9b9t.com./198.251.83.216:9001", "minecraft.9b9t.org./198.251.83.216:9001", "eu.9b9t.com/198.251.89.41:25565", "minecraft.2b2t.com./198.251.83.216:9001")
            )
        )
        servers.add(
            ServerEntry(
                tag = "2b2t",
                serverIps = listOf("connect.2b2t.org./50.114.4.34:25565")
            )
        )
        servers.add(
            ServerEntry(
                tag = "constantiam",
                serverIps = listOf("connect.constantiam.net./91.107.201.59:25565")
            )
        )
        servers.add(
            ServerEntry(
                tag = "anarchadia",
                serverIps = listOf("anarchadia.org")
            )
        )
    }

    private fun loadRegistry() {
        try {
            val registryJson = Files.readString(registryPath)
            val jsonObject = JsonParser.parseString(registryJson).asJsonObject

            if (jsonObject.has("servers")) {
                val serversArray = jsonObject.getAsJsonArray("servers")
                servers.clear()

                serversArray.forEach { element ->
                    val serverObject = element.asJsonObject
                    ServerEntry.CODEC.decode(JsonOps.INSTANCE, serverObject)
                        .result()
                        .ifPresent { servers.add(it.first) }
                }
                ninehack.logChat("Loaded ${servers.size} server entries from registry")
            }
        } catch (e: Exception) {
            ninehack.logChat("Failed to load server registry: ${e.message}")
            createDefaultRegistry()
            saveRegistry()
        }
    }

    fun saveRegistry() {
        try {
            Files.createDirectories(registryPath.parent)

            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonObject = JsonObject()

            val serversArray = gson.toJsonTree(servers.map { server ->
                ServerEntry.CODEC.encodeStart(JsonOps.INSTANCE, server)
                    .result()
                    .orElseThrow()
            }).asJsonArray

            jsonObject.add("servers", serversArray)
            jsonObject.addProperty("version", CURRENT_VERSION)

            val orderedJson = CollectorJson.reorderFields(jsonObject, CollectorJson.FieldOrder.SERVER_REGISTRY)

            Files.writeString(registryPath, gson.toJson(orderedJson))
            ninehack.logChat("Saved server registry with ${servers.size} entries")
        } catch (e: Exception) {
            ninehack.logChat("Failed to save server registry: ${e.message}")
        }
    }

    fun getCurrentServerIp(): String {
        val minecraft = mc
        if (minecraft.isIntegratedServerRunning) {
            return "local"
        }

        val connection = minecraft.networkHandler?.connection
        val addressString = connection?.address?.toString()

        return when {
            addressString == null || addressString == "local" || addressString.isEmpty() -> "local"
            else -> addressString.removePrefix("/") // Remove potential leading slash
        }
    }

    fun getServerTag(serverIp: String): String? {
        return servers.find { server -> server.serverIps.contains(serverIp) }?.tag
    }

    fun getServers(): List<ServerEntry> = servers.toList()

    fun addServerIp(tag: String, serverIp: String): Boolean {

        val existingEntry = servers.find { server -> server.serverIps.contains(serverIp) }
        if (existingEntry != null && existingEntry.tag != tag) {

            val updatedIps = existingEntry.serverIps.filter { it != serverIp }
            servers.remove(existingEntry)

            if (updatedIps.isNotEmpty()) {
                servers.add(ServerEntry(existingEntry.tag, updatedIps))
            }
        }

        val tagEntry = servers.find { it.tag == tag }

        if (tagEntry != null) {

            if (tagEntry.serverIps.contains(serverIp)) {
                return false
            }

            val updatedIps = tagEntry.serverIps.toMutableList()
            updatedIps.add(serverIp)
            servers.remove(tagEntry)
            servers.add(ServerEntry(tag, updatedIps))
        } else {

            servers.add(ServerEntry(tag, listOf(serverIp)))
        }

        saveRegistry()
        return true
    }

    fun removeServerIp(tag: String, serverIp: String): Boolean {
        val tagEntry = servers.find { it.tag == tag } ?: return false

        if (!tagEntry.serverIps.contains(serverIp)) {
            return false
        }

        val updatedIps = tagEntry.serverIps.filter { it != serverIp }
        servers.remove(tagEntry)

        if (updatedIps.isNotEmpty()) {
            servers.add(ServerEntry(tag, updatedIps))
        }

        saveRegistry()
        return true
    }

    fun updateTagName(oldTag: String, newTag: String): Boolean {
        if (servers.any { it.tag == newTag }) {
            return false
        }
        val tagEntry = servers.find { it.tag == oldTag } ?: return false

        servers.remove(tagEntry)
        servers.add(ServerEntry(newTag, tagEntry.serverIps))

        val oldFolder = getServerFolder(oldTag)
        if (oldFolder.exists()) {
            try {
                val newFolder = getServerFolder(newTag)
                oldFolder.moveTo(newFolder, overwrite = false)
            } catch (e: Exception) {
                ninehack.logChat("Failed to rename server folder: ${e.message}")
            }
        }

        saveRegistry()
        return true
    }

    fun determineCurrentServer(): String? {
        val serverIp = getCurrentServerIp() ?: return null

        if (serverIp == "local") {
            return "local"
        }

        val existingTag = getServerTag(serverIp)
        if (existingTag != null) {
            return existingTag
        }

        val sanitizedIp = serverIp.replace(":", "_")
        addServerIp(sanitizedIp, serverIp)
        CollectorTags.addTag("servers", sanitizedIp)
        return sanitizedIp
    }

    fun sanitizeFolderName(name: String): String {
        return name.replace(":", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .replace("?", "_")
            .replace("*", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")
            .take(50)
    }

    fun getServerFolder(serverTag: String): Path {
        val folderName = sanitizeFolderName(serverTag)
        return mapsDir.resolve(folderName)
    }

    fun ensureServerFolder(serverTag: String): Path {
        val folderPath = getServerFolder(serverTag)
        Files.createDirectories(folderPath)
        return folderPath
    }

    fun getMapFilePath(mapId: Int, serverTag: String? = null): Path {
        val folderPath = if (serverTag != null) {
            ensureServerFolder(serverTag)
        } else {
            mapsDir
        }
        return folderPath.resolve("map_${mapId}.json")
    }

    fun listServerFolders(): List<Path> {
        if (!mapsDir.exists() || !mapsDir.isDirectory()) {
            return emptyList()
        }
        return mapsDir.toFile()
            .listFiles { file -> file.isDirectory }
            ?.map { file -> mapsDir.resolve(file.name) }
            ?.toList()
            ?: emptyList()
    }

    fun moveMapFile(mapId: Int, fromServer: String?, toServer: String): Boolean {
        val sourcePath = getMapFilePath(mapId, fromServer)
        if (!sourcePath.exists()) {
            return false
        }
        val targetPath = getMapFilePath(mapId, toServer)

        return try {
            Files.createDirectories(targetPath.parent)
            Files.move(sourcePath, targetPath)
            true
        } catch (e: Exception) {
            ninehack.logChat("Failed to move map file: ${e.message}")
            false
        }
    }
}