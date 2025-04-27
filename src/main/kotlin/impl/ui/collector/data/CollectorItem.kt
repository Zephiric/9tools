package impl.ui.collector.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.component.ComponentMap
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryOps
import net.minecraft.util.Identifier
import win.ninegang.ninetools.compat.util.Wrapper.mc
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import impl.ui.collector.SettingsManager

data class CollectorItem(
    val baseItem: Item,
    val customName: String,
    val givenBy: String,
    val location: String,
    val description: String? = null,
    val dateReceived: Long = System.currentTimeMillis(),
    val _components: ComponentMap,
    val deferredComponents: Set<String> = emptySet(),
    val tags: List<TagReference> = emptyList(),
    val version: String = CURRENT_VERSION
) {
    /**
     * Creates a copy of the item with specified modifications
     */
    fun createModified(
        customName: String = this.customName,
        givenBy: String = this.givenBy,
        location: String = this.location,
        description: String? = this.description,
        components: ComponentMap = this._components,
        deferredComponents: Set<String> = this.deferredComponents,
        tags: List<TagReference> = this.tags,
    ) = CollectorItem(
        baseItem = this.baseItem,
        customName = customName,
        givenBy = givenBy,
        location = location,
        description = description,
        dateReceived = this.dateReceived,
        _components = components,
        deferredComponents = deferredComponents,
        tags = tags,
        version = CURRENT_VERSION
    )

    val components: ComponentMap get() = if (isLightMode) loadComponents(skipDeferred = true) else _components
    private var isLightMode: Boolean = false

    companion object {
        const val CURRENT_VERSION = "0.1"


        val EXCLUDED_COMPONENTS = setOf<ComponentType<*>>()

        private val CAPTURE_ONLY_IF_MODIFIED = setOf(
            DataComponentTypes.MAX_STACK_SIZE,
            DataComponentTypes.LORE,
            DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.REPAIR_COST,
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.RARITY
        )

        @JvmStatic
        val CODEC: Codec<CollectorItem> = RecordCodecBuilder.create { instance ->
            instance.group(
                Identifier.CODEC.xmap(
                    { id -> Registries.ITEM.get(id) },
                    { item -> Registries.ITEM.getId(item) }
                ).fieldOf("baseItem").forGetter { it.baseItem },
                Codec.STRING.fieldOf("customName").forGetter { it.customName },
                Codec.STRING.fieldOf("givenBy").forGetter { it.givenBy },
                Codec.STRING.fieldOf("location").forGetter { it.location },
                Codec.LONG.fieldOf("dateReceived").forGetter { it.dateReceived },
                ComponentMap.CODEC.fieldOf("components").forGetter { it._components },
                Codec.STRING.optionalFieldOf("description", "").forGetter { it.description ?: "" },
                Codec.list(Codec.STRING).xmap(
                    { list -> list.toSet() },
                    { set -> set.toList() }
                ).optionalFieldOf("deferredComponents", emptySet()).forGetter { it.deferredComponents },
                Codec.list(TagReference.CODEC).optionalFieldOf("tags", emptyList()).forGetter { it.tags },
                Codec.STRING.fieldOf("version").forGetter { it.version }
            ).apply(instance) { baseItem, customName, givenBy, location, dateReceived, components, description, deferred, tags, version ->
                CollectorItem(
                    baseItem = baseItem,
                    customName = customName,
                    givenBy = givenBy,
                    location = location,
                    dateReceived = dateReceived,
                    _components = components,
                    description = description.ifEmpty { null },
                    deferredComponents = deferred,
                    tags = tags,
                    version = version
                )
            }
        }

        fun fromItemStack(stack: ItemStack, givenBy: String, location: String) = CollectorItem(
            baseItem = stack.item,
            customName = getDisplayName(stack),
            givenBy = givenBy,
            location = location,
            _components = buildComponents(stack)
        )

        fun fromJson(json: JsonObject): CollectorItem? {
            return try {

                val registryOps = getRegistryOps()

                val result = CODEC.decode(registryOps, json)
                    .result()
                    .map { it.first }
                    .orElse(null)

                result?.tags?.let { CollectorTags.registerTagReferences(it) }

                result
            } catch (e: Exception) {
                ninehack.logChat("Failed to decode item: ${e.message}")
                null
            }
        }

        private fun getRegistryOps() = if (mc.world != null) {
            RegistryOps.of(JsonOps.INSTANCE, mc.world?.registryManager)
        } else {
            JsonOps.INSTANCE
        }

        private fun getDisplayName(stack: ItemStack): String = when {
            stack.contains(DataComponentTypes.CUSTOM_NAME) -> stack.getName().string
            stack.contains(DataComponentTypes.ITEM_NAME) ->
                stack.get(DataComponentTypes.ITEM_NAME)?.string ?: stack.item.name.string
            else -> stack.item.name.string
        }

        private fun buildComponents(stack: ItemStack): ComponentMap {
            val builder = ComponentMap.builder()

            stack.components.forEach { component ->
                val type = component.type()

                val shouldCapture = if (SettingsManager.saveDefaultComponents) {
                    true
                } else if (CAPTURE_ONLY_IF_MODIFIED.contains(type)) {
                    val defaultValue = DataComponentTypes.DEFAULT_ITEM_COMPONENTS.get(type)
                    defaultValue != component.value()
                } else {
                    true
                }

                if (shouldCapture) {
                    @Suppress("UNCHECKED_CAST")
                    builder.add(type as ComponentType<Any>, component.value())
                }
            }

            return builder.build()
        }

        fun toItemStack(item: CollectorItem): ItemStack {
            val stack = ItemStack(item.baseItem)

            DataComponentTypes.DEFAULT_ITEM_COMPONENTS.forEach { component ->
                val type = component.type()
                @Suppress("UNCHECKED_CAST")
                stack.set(type as ComponentType<Any>, component.value())
            }

            item._components.forEach { component ->
                val type = component.type()
                @Suppress("UNCHECKED_CAST")
                stack.set(type as ComponentType<Any>, component.value())
            }

            return stack
        }
    }

    fun setLightMode(light: Boolean) {
        isLightMode = light
    }

    private fun loadComponents(skipDeferred: Boolean): ComponentMap {
        if (!skipDeferred) return _components

        val builder = ComponentMap.builder()
        _components.forEach { comp ->
            val type = comp.type()
            val typeName = type.toString()
            if (!deferredComponents.contains(typeName)) {
                @Suppress("UNCHECKED_CAST")
                builder.add(type as ComponentType<Any>, comp.value())
            }
        }
        return builder.build()
    }

    fun toJson(): JsonObject {
        val gson = GsonBuilder().setPrettyPrinting().create()

        val registryOps = if (mc.world != null) {
            RegistryOps.of(JsonOps.INSTANCE, mc.world?.registryManager)
        } else {
            JsonOps.INSTANCE
        }

        val result = CODEC.encodeStart(registryOps, this)

        return if (result.result().isPresent) {
            val jsonObject = gson.fromJson(gson.toJson(result.result().get()), JsonObject::class.java)

            CollectorJson.reorderFieldsOptimized(
                jsonObject,
                CollectorJson.FieldOrder.ITEM,
                largeFields = setOf("components")
            )
        } else {
            throw RuntimeException("Failed to encode item: ${result.error().get().message()}")
        }
    }

    fun hasTag(groupName: String, tagName: String): Boolean {
        return tags.any { tagRef ->
            tagRef.group == groupName && tagRef.tags.contains(tagName)
        }
    }

    fun addTag(groupName: String, tagName: String): CollectorItem {
        CollectorTags.addTag(groupName, tagName)

        val mutableTags = tags.toMutableList()
        val existingGroup = mutableTags.find { it.group == groupName }

        if (existingGroup != null) {
            if (!existingGroup.tags.contains(tagName)) {
                val updatedTags = existingGroup.tags.toMutableList()
                updatedTags.add(tagName)

                mutableTags.remove(existingGroup)
                mutableTags.add(TagReference(groupName, updatedTags))
            }
        } else {
            mutableTags.add(TagReference(groupName, listOf(tagName)))
        }

        return createModified(tags = mutableTags)
    }

    fun removeTag(groupName: String, tagName: String): CollectorItem {
        val mutableTags = tags.toMutableList()
        val existingGroup = mutableTags.find { it.group == groupName } ?: return this

        if (!existingGroup.tags.contains(tagName)) {
            return this
        }

        val updatedTags = existingGroup.tags.filter { it != tagName }

        mutableTags.remove(existingGroup)

        if (updatedTags.isNotEmpty()) {
            mutableTags.add(TagReference(groupName, updatedTags))
        }

        return createModified(tags = mutableTags)
    }

    fun getAllTags(): Map<String, List<String>> {
        return tags.associate { it.group to it.tags }
    }
}