package impl.ui.collector.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import win.ninegang.ninetools.Ninehack
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import java.nio.file.Files
import java.nio.file.Paths

data class TagGroup(
    val group: String,
    val tags: List<String>,
    val isCore: Boolean = false
) {
    companion object {
        val CODEC: Codec<TagGroup> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("group").forGetter { it.group },
                Codec.list(Codec.STRING).fieldOf("tags").forGetter { it.tags },
                Codec.BOOL.optionalFieldOf("isCore", false).forGetter { it.isCore }
            ).apply(instance) { group, tags, isCore ->
                TagGroup(
                    group = group,
                    tags = tags,
                    isCore = isCore
                )
            }
        }
    }
}

data class TagReference(
    val group: String,
    val tags: List<String>
) {
    companion object {
        val CODEC: Codec<TagReference> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("group").forGetter { it.group },
                Codec.list(Codec.STRING).fieldOf("tags").forGetter { it.tags }
            ).apply(instance) { group, tags ->
                TagReference(
                    group = group,
                    tags = tags
                )
            }
        }
    }
}

object CollectorTags {
    private val registryPath = Paths.get(Ninehack.NAME, "collector", "tagRegistry.json")
    private const val CURRENT_VERSION = "0.1"

    private val tagGroups = mutableListOf<TagGroup>()

    fun initialize() {
        if (Files.exists(registryPath)) {
            loadRegistry()
        } else {
            createDefaultTagGroups()
            saveRegistry()
        }
        ensureServerTagGroup()
    }

    private fun ensureServerTagGroup() {
        val serversGroup = tagGroups.find { it.group == "servers" }
        if (serversGroup == null) {
            tagGroups.add(
                TagGroup(
                    group = "servers",
                    tags = listOf("9b9t", "2b2t", "constantiam", "anarchardia"),
                    isCore = true
                )
            )
            saveRegistry()
        } else if (!serversGroup.isCore) {
            tagGroups.remove(serversGroup)
            tagGroups.add(
                TagGroup(
                    group = serversGroup.group,
                    tags = serversGroup.tags,
                    isCore = true
                )
            )
            saveRegistry()
        }
    }

    private fun createDefaultTagGroups() {
        tagGroups.add(
            TagGroup(
                group = "servers",
                tags = listOf("9b9t", "2b2t", "constantiam", "anarchardia"),
                isCore = true
            )
        )

        tagGroups.add(
            TagGroup(
                group = "mapart",
                tags = listOf("staircased", "full block", "carpet")
            )
        )

        tagGroups.add(
            TagGroup(
                group = "kits",
                tags = listOf("pvp", "build", "utility")
            )
        )
    }

    private fun loadRegistry() {
        try {
            val registryJson = Files.readString(registryPath)
            val jsonObject = JsonParser.parseString(registryJson).asJsonObject

            if (jsonObject.has("tagGroups")) {
                val groupsArray = jsonObject.getAsJsonArray("tagGroups")
                tagGroups.clear()

                groupsArray.forEach { element ->
                    val groupObject = element.asJsonObject
                    TagGroup.CODEC.decode(JsonOps.INSTANCE, groupObject)
                        .result()
                        .ifPresent { tagGroups.add(it.first) }
                }

                ninehack.logChat("Loaded ${tagGroups.size} tag groups from registry")
            }
        } catch (e: Exception) {
            ninehack.logChat("Failed to load tag registry: ${e.message}")
            createDefaultTagGroups()
            saveRegistry()
        }
    }

    fun saveRegistry() {
        try {
            Files.createDirectories(registryPath.parent)

            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonObject = JsonObject()

            val groupsArray = gson.toJsonTree(tagGroups.map { group ->
                TagGroup.CODEC.encodeStart(JsonOps.INSTANCE, group)
                    .result()
                    .orElseThrow()
            }).asJsonArray

            jsonObject.add("tagGroups", groupsArray)
            jsonObject.addProperty("version", CURRENT_VERSION)

            val orderedJson = CollectorJson.reorderFields(jsonObject, CollectorJson.FieldOrder.TAG_REGISTRY)

            Files.writeString(registryPath, gson.toJson(orderedJson))
            ninehack.logChat("Saved tag registry with ${tagGroups.size} groups")
        } catch (e: Exception) {
            ninehack.logChat("Failed to save tag registry: ${e.message}")
        }
    }

    fun getTagGroups(): List<TagGroup> = tagGroups.toList()

    fun getTagGroup(groupName: String): TagGroup? = tagGroups.find { it.group == groupName }

    fun renameTagGroup(oldName: String, newName: String): Boolean {
        val group = tagGroups.find { it.group == oldName } ?: return false
        if (group.isCore) {
            ninehack.logChat("Cannot rename core group: ${group.group}")
            return false
        }
        if (tagGroups.any { it.group == newName }) {
            return false
        }
        tagGroups.remove(group)

        tagGroups.add(
            TagGroup(
                group = newName,
                tags = group.tags,
                isCore = group.isCore
            )
        )

        saveRegistry()
        return true
    }

    fun renameTag(groupName: String, oldTagName: String, newTagName: String): Boolean {
        val group = tagGroups.find { it.group == groupName } ?: return false

        if (group.tags.contains(newTagName)) {
            return false
        }

        val newTags = group.tags.map { if (it == oldTagName) newTagName else it }

        tagGroups.remove(group)

        tagGroups.add(
            TagGroup(
                group = group.group,
                tags = newTags,
                isCore = group.isCore
            )
        )

        saveRegistry()
        return true
    }

    fun addTag(groupName: String, tagName: String) {
        val group = tagGroups.find { it.group == groupName }

        if (group != null) {
            if (!group.tags.contains(tagName)) {
                val updatedTags = group.tags.toMutableList()
                updatedTags.add(tagName)

                tagGroups.remove(group)
                tagGroups.add(
                    TagGroup(
                        group = group.group,
                        tags = updatedTags,
                        isCore = group.isCore
                    )
                )
                saveRegistry()
            }
        } else {
            tagGroups.add(
                TagGroup(
                    group = groupName,
                    tags = listOf(tagName)
                )
            )
            saveRegistry()
        }
    }

    fun registerTagReferences(references: List<TagReference>) {
        var registryChanged = false

        references.forEach { reference ->
            val groupName = reference.group
            val group = getTagGroup(groupName)

            if (group != null) {
                val unknownTags = reference.tags.filter { !group.tags.contains(it) }

                if (unknownTags.isNotEmpty()) {
                    val updatedTags = group.tags.toMutableList()
                    updatedTags.addAll(unknownTags)

                    tagGroups.remove(group)
                    tagGroups.add(
                        TagGroup(
                            group = group.group,
                            tags = updatedTags,
                            isCore = group.isCore
                        )
                    )
                    registryChanged = true
                }
            } else {
                tagGroups.add(
                    TagGroup(
                        group = groupName,
                        tags = reference.tags
                    )
                )
                registryChanged = true
            }
        }

        if (registryChanged) {
            saveRegistry()
        }
    }

    fun removeTagGroup(groupName: String): Boolean {
        val group = tagGroups.find { it.group == groupName } ?: return false
        if (group.isCore) {
            ninehack.logChat("Cannot remove core group: ${group.group}")
            return false
        }
        val result = tagGroups.remove(group)
        if (result) {
            saveRegistry()
        }
        return result
    }

    fun removeTag(groupName: String, tagName: String): Boolean {
        val group = tagGroups.find { it.group == groupName } ?: return false

        if (!group.tags.contains(tagName)) {
            return false
        }

        val updatedTags = group.tags.filter { it != tagName }

        tagGroups.remove(group)
        tagGroups.add(
            TagGroup(
                group = group.group,
                tags = updatedTags,
                isCore = group.isCore
            )
        )
        saveRegistry()
        return true
    }

    fun createTagGroup(name: String): Boolean {
        if (tagGroups.any { it.group == name }) {
            return false
        }

        tagGroups.add(
            TagGroup(
                group = name,
                tags = listOf()
            )
        )
        saveRegistry()
        return true
    }
}