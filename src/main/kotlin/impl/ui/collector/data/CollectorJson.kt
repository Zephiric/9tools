package impl.ui.collector.data

import com.google.gson.JsonObject

object CollectorJson {

    object FieldOrder {

        val ITEM = listOf(
            "version",
            "baseItem",
            "customName",
            "givenBy",
            "location",
            "description",
            "dateReceived",
            "tags",
            "deferredComponents",
            "components"
        )

        val COLLECTION = listOf(
            "version",
            "name",
            "owner",
            "createdAt",
            "lastModified",
            "lastItemAdded",
            "itemCount",
            "modifiedCount"
        )

        val MAP = listOf(
            "version",
            "mapId",
            "name",
            "description",
            "serverName",
            "scale",
            "locked",
            "centerX",
            "centerZ",
            "colors"
        )

        val TAG_REGISTRY = listOf(
            "version",
            "tagGroups"
        )

        val SERVER_REGISTRY = listOf(
            "version",
            "servers"
        )
    }

    private fun isReorderingEnabled(): Boolean {
        // Allow the user to turn off reordering in the settings for performance reasons
        // Maybe also allow users to manage what gets reordered and maybe even the order itself
        return true // TODO: Replace with SettingsManager.enableJsonReordering
    }

    fun reorderFields(originalObj: JsonObject, fieldOrder: List<String>): JsonObject {
        if (!isReorderingEnabled()) {
            return originalObj
        }

        val result = JsonObject()

        fieldOrder.forEach { field ->
            if (originalObj.has(field)) {
                result.add(field, originalObj.get(field))
            }
        }

        originalObj.entrySet().forEach { (key, value) ->
            if (!fieldOrder.contains(key)) {
                result.add(key, value)
            }
        }

        return result
    }

    fun reorderFieldsOptimized(
        originalObj: JsonObject,
        fieldOrder: List<String>,
        largeFields: Set<String> = setOf("components", "colors")
    ): JsonObject {
        if (!isReorderingEnabled()) {
            return originalObj
        }

        val result = JsonObject()

        fieldOrder.forEach { field ->
            if (originalObj.has(field) && !largeFields.contains(field)) {
                result.add(field, originalObj.get(field))
            }
        }

        originalObj.entrySet().forEach { (key, value) ->
            if (!fieldOrder.contains(key) && !largeFields.contains(key)) {
                result.add(key, value)
            }
        }

        largeFields.forEach { field ->
            if (originalObj.has(field)) {
                result.add(field, originalObj.get(field))
            }
        }

        return result
    }
}