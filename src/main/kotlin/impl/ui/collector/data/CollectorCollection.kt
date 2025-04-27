package impl.ui.collector.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import win.ninegang.ninetools.Ninehack
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

data class CollectorCollection(
    val name: String,
    val owner: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = createdAt,
    val lastItemAdded: Long = createdAt,
    private val _modifiedCount: Int = 0,
    private val _items: MutableList<CollectorItem> = mutableListOf(),
    private val _modifiedItems: MutableSet<Int> = mutableSetOf(),
    private val _deletedItemFiles: MutableSet<String> = mutableSetOf(),
    val version: String = CURRENT_VERSION
) {
    val items: List<CollectorItem> get() = _items.toList()
    val itemCount: Int get() = _items.size
    val modifiedCount: Int get() = _modifiedCount

    companion object {
        const val CURRENT_VERSION = "0.1"
        private const val METADATA_FILE = "metadata.json"
        private const val ITEMS_DIR = "items"
        private val baseDir = Paths.get(Ninehack.NAME, "collector")

        private var currentCollection: CollectorCollection? = null
        private val collections = mutableMapOf<String, CollectorCollection>()

        val CODEC: Codec<CollectorCollection> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter { it.name },
                Codec.STRING.fieldOf("owner").forGetter { it.owner },
                Codec.LONG.fieldOf("createdAt").forGetter { it.createdAt },
                Codec.LONG.fieldOf("lastModified").forGetter { it.lastModified },
                Codec.LONG.fieldOf("lastItemAdded").forGetter { it.lastItemAdded },
                Codec.INT.fieldOf("itemCount").forGetter { it.itemCount },
                Codec.INT.fieldOf("modifiedCount").forGetter { it._modifiedCount },
                Codec.STRING.fieldOf("version").forGetter { it.version }
            ).apply(instance) { name, owner, createdAt, lastModified, lastItemAdded, itemCount, modifiedCount, version ->
                CollectorCollection(
                    name = name,
                    owner = owner,
                    createdAt = createdAt,
                    lastModified = lastModified,
                    lastItemAdded = lastItemAdded,
                    _modifiedCount = modifiedCount,
                    version = version
                )
            }
        }

        init {
            loadAllCollections()
        }

        fun getCurrent(): CollectorCollection? = currentCollection
        fun getAvailable(): List<String> = collections.keys.toList()

        fun createNew(name: String, owner: String): CollectorCollection {
            var uniqueName = name
            var counter = 1
            while (collections.containsKey(uniqueName)) {
                uniqueName = "$name ($counter)"
                counter++
            }

            val now = System.currentTimeMillis()
            val collection = CollectorCollection(
                name = uniqueName,
                owner = owner,
                createdAt = now,
                lastModified = now,
                lastItemAdded = now
            )

            collections[uniqueName] = collection
            collection.save()

            if (currentCollection == null) {
                currentCollection = collection
            }

            ninehack.logChat("Created new collection: $uniqueName")
            return collection
        }

        fun switchTo(name: String) {
            if (currentCollection?.name == name) return

            currentCollection?.save()
            currentCollection = null

            try {
                val collection = load(name, LoadType.LIGHT)
                if (collection != null) {
                    currentCollection = collection
                    collections[name] = collection
                    ninehack.logChat("Switched to collection: ${collection.name}")
                } else {
                    ninehack.logChat("Failed to load collection: $name")
                }
            } catch (e: Exception) {
                ninehack.logChat("Error switching collections: ${e.message}")
            }
        }

        fun delete(name: String): Boolean {
            if (collections.size <= 1) {
                ninehack.logChat("Cannot delete the last collection!")
                return false
            }

            val removed = collections.remove(name)
            removed?.let {
                if (currentCollection?.name == name) {
                    val nextCollection = collections.keys.first()
                    switchTo(nextCollection)
                }

                it.delete()
                ninehack.logChat("Deleted collection: $name")
                return true
            }
            return false
        }

        private fun loadAllCollections() {
            if (!baseDir.exists()) {
                baseDir.createDirectories()
            }

            CollectorTags.initialize()

            collections.clear()
            baseDir.toFile().listFiles { file -> file.isDirectory }?.forEach { dir ->
                try {
                    val collection = load(dir.name, LoadType.METADATA)
                    if (collection != null) {
                        collections[collection.name] = collection
                        ninehack.logChat("Found collection: ${collection.name}")
                    }
                } catch (e: Exception) {
                    ninehack.logChat("Failed to load collection: ${e.message}")
                }
            }

            if (collections.isEmpty()) {
                createNew("My Collection", mc.session?.username ?: "Player")
            } else {
                switchTo(collections.keys.first())
            }
        }

        private fun load(name: String, loadType: LoadType = LoadType.LIGHT): CollectorCollection? {
            val collectionDir = baseDir / name
            if (!collectionDir.exists()) return null

            val metadataFile = collectionDir / METADATA_FILE
            if (!metadataFile.exists()) return null

            val gson = GsonBuilder().create()
            val jsonObject = gson.fromJson(metadataFile.readText(), JsonObject::class.java)
            val collection = CODEC.decode(JsonOps.INSTANCE, jsonObject)
                .result()
                .map { it.first }
                .orElse(null) ?: return null

            if (loadType != LoadType.METADATA) {
                val itemsDir = collectionDir / ITEMS_DIR
                if (itemsDir.exists()) {
                    val loadedItems = loadItems(itemsDir, loadType)
                    collection._items.addAll(loadedItems)
                }
            }

            return collection
        }

        private fun loadItems(itemsDir: Path, loadType: LoadType): List<CollectorItem> {
            return itemsDir.toFile()
                .listFiles { file -> file.name.lowercase().endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        loadItemFromFile(file, loadType)
                    } catch (e: Exception) {
                        ninehack.logChat("Failed to load item from ${file.name}: ${e.message}")
                        null
                    }
                }
                ?.sortedBy { it.dateReceived }
                ?: emptyList()
        }

        private fun loadItemFromFile(file: File, loadType: LoadType): CollectorItem? {
            val gson = GsonBuilder().create()
            val jsonObject = gson.fromJson(file.readText(), JsonObject::class.java)
            return CollectorItem.fromJson(jsonObject)?.also { item ->
                when (loadType) {
                    LoadType.FULL -> item.setLightMode(false)
                    LoadType.LIGHT -> item.setLightMode(true)
                    LoadType.METADATA -> item.setLightMode(true)
                }
            }
        }
    }

    fun save() {
        val collectionDir = baseDir / name
        collectionDir.createDirectories()

        val metadataFile = collectionDir / METADATA_FILE
        val result = CODEC.encodeStart(JsonOps.INSTANCE, this)
        if (result.result().isPresent) {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonObject = gson.fromJson(gson.toJson(result.result().get()), JsonObject::class.java)

            //May be killing performance
            val orderedJson = CollectorJson.reorderFields(jsonObject, CollectorJson.FieldOrder.COLLECTION)

            metadataFile.writeText(gson.toJson(orderedJson))
        }

        val itemsDir = collectionDir / ITEMS_DIR
        itemsDir.createDirectories()

        val existingFiles = itemsDir.toFile().listFiles()
            ?.filter { it.name.lowercase().endsWith(".json") }
            ?.associate { it.name to it }
            ?.toMutableMap() ?: mutableMapOf()

        _modifiedItems.forEach { index ->
            if (index < _items.size) {
                val item = _items[index]
                val sanitizedName = sanitizeFileName(item.customName)
                val fileName = "${sanitizedName}_${index}.json"

                saveItem(itemsDir, index, item)

                existingFiles.remove(fileName)
            }
        }

        _deletedItemFiles.forEach { fileName ->
            val file = itemsDir / fileName
            if (file.exists()) {
                file.deleteIfExists()
            }
        }

        _modifiedItems.clear()
        _deletedItemFiles.clear()
    }

    private fun saveItem(itemsDir: Path, index: Int, item: CollectorItem) {
        val sanitizedName = sanitizeFileName(item.customName)
        val itemFile = itemsDir / "${sanitizedName}_${index}.json"
        val gson = GsonBuilder().setPrettyPrinting().create()
        // Temporarily disable light mode to ensure we save all components because my system is scuffed af
        item.setLightMode(false)
        itemFile.writeText(gson.toJson(item.toJson()))
        item.setLightMode(true)
    }

    fun delete() {
        val collectionDir = baseDir / name
        if (collectionDir.exists()) {
            collectionDir.toFile().deleteRecursively()
        }
    }

    fun updateItem(oldItem: CollectorItem?, newItem: CollectorItem) {
        if (oldItem == null) {
            _items.add(newItem)
            _modifiedItems.add(_items.size - 1)
        } else {
            val index = _items.indexOf(oldItem)
            if (index != -1) {
                _items[index] = newItem
                _modifiedItems.add(index)
            }
        }
        incrementModifiedCount()
    }

    fun removeItem(item: CollectorItem) {
        val index = _items.indexOf(item)
        if (index != -1) {
            val sanitizedName = sanitizeFileName(item.customName)
            _deletedItemFiles.add("${sanitizedName}_${index}.json")

            _items.removeAt(index)
            incrementModifiedCount()

            val updatedModified = mutableSetOf<Int>()
            _modifiedItems.forEach { modifiedIndex ->
                when {
                    modifiedIndex < index -> updatedModified.add(modifiedIndex)
                    modifiedIndex > index -> updatedModified.add(modifiedIndex - 1)
                }
            }
            _modifiedItems.clear()
            _modifiedItems.addAll(updatedModified)
        }
    }

    fun forceSaveAll() {
        _modifiedItems.clear()
        for (i in _items.indices) {
            _modifiedItems.add(i)
        }
        save()
    }

    private fun incrementModifiedCount(): CollectorCollection {
        return copy(
            _modifiedCount = _modifiedCount + 1,
            lastModified = System.currentTimeMillis(),
            _modifiedItems = _modifiedItems,
            _deletedItemFiles = _deletedItemFiles
        )
    }

    fun getCurrentLocation(): String {
        return mc.player?.let { player ->
            when (player.world.dimension.toString()) {
                "minecraft:overworld" -> "Overworld"
                "minecraft:the_nether" -> "Nether"
                "minecraft:the_end" -> "End"
                else -> "Unknown"
            }
        } ?: "Unknown"
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .take(50)
        return if (sanitized.isBlank()) "item" else sanitized
    }

    fun getItemsByTag(groupName: String, tagName: String): List<CollectorItem> {
        return _items.filter { it.hasTag(groupName, tagName) }
    }

    fun getUsedTags(): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        _items.forEach { item ->
            item.tags.forEach { tagRef ->
                val groupTags = result.getOrPut(tagRef.group) { mutableSetOf() }
                groupTags.addAll(tagRef.tags)
            }
        }

        return result
    }

    fun addTag(item: CollectorItem, groupName: String, tagName: String): Boolean {
        val index = _items.indexOf(item)
        if (index == -1) return false

        val updatedItem = item.addTag(groupName, tagName)
        if (updatedItem === item) return false

        _items[index] = updatedItem
        _modifiedItems.add(index)
        incrementModifiedCount()
        return true
    }

    fun removeTag(item: CollectorItem, groupName: String, tagName: String): Boolean {
        val index = _items.indexOf(item)
        if (index == -1) return false

        val updatedItem = item.removeTag(groupName, tagName)
        if (updatedItem === item) return false

        _items[index] = updatedItem
        _modifiedItems.add(index)
        incrementModifiedCount()
        return true
    }

    fun addTagToItems(items: List<CollectorItem>, groupName: String, tagName: String): Int {
        var updatedCount = 0

        items.forEach { item ->
            if (addTag(item, groupName, tagName)) {
                updatedCount++
            }
        }

        return updatedCount
    }

    fun removeTagFromItems(items: List<CollectorItem>, groupName: String, tagName: String): Int {
        var updatedCount = 0

        items.forEach { item ->
            if (removeTag(item, groupName, tagName)) {
                updatedCount++
            }
        }

        return updatedCount
    }

    enum class LoadType {
        FULL,      // Load everything
        LIGHT,     // Load with skipped components
        METADATA   // Load only metadata
    }
}