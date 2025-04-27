package impl.ui.collector.components

import net.minecraft.component.ComponentType
import impl.ui.collector.UIComponent
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil

/**
 * Represents the state of an editor, including its current value and validation status.
 * This provides a standardized way to track changes and validity across different editor types.
 */
data class EditorState(
    val componentType: String,
    val value: Any?,
    val isValid: Boolean = true,
    val validationMessage: String? = null,
    val columnSpan: Int = 1,
    val customHeight: Double? = null
)

/**
 * The EditorRegistry manages communication and state tracking for all active component editors.
 * It serves as the central hub for:
 * - Tracking editor states and validity
 * - Managing editor lifecycle (registration/unregistration)
 * - Facilitating communication between editors
 * - Notifying the screen of changes
 */
class EditorRegistry {

    private val editors = mutableMapOf<String, UIComponent>()

    private val editorStates = mutableMapOf<String, EditorState>()

    private val stateListeners = mutableMapOf<String, MutableSet<(EditorState) -> Unit>>()

    private val globalListeners = mutableSetOf<(String, EditorState) -> Unit>()

    /**
     * Registers a new editor instance with the registry.
     * This should be called when a new editor is created.
     */
    fun registerEditor(componentType: String?, editor: UIComponent) {
        if (componentType != null) {
            editors[componentType] = editor
            if (!editorStates.containsKey(componentType)) {
                editorStates[componentType] = EditorState(componentType, null)
            }
        }
    }


    /**
     * Removes an editor from the registry.
     * This should be called when an editor is being deleted/removed.
     */
    fun unregisterEditor(componentType: String) {
        editors.remove(componentType)
        editorStates.remove(componentType)
        stateListeners.remove(componentType)
        val removalState = EditorState(componentType, null, isValid = true)
        notifyGlobalListeners(componentType, removalState)
    }

    /**
     * Updates the state for a specific editor.
     * This triggers notifications to all relevant listeners.
     */
    fun updateState(componentType: String, newState: EditorState) {
        editorStates[componentType] = newState
        stateListeners[componentType]?.forEach { listener ->
            listener(newState)
        }
        notifyGlobalListeners(componentType, newState)
    }

    /**
     * Retrieves the current state of an editor.
     */
    fun getEditorState(componentType: String): EditorState? = editorStates[componentType]

    /**
     * Retrieves all current editor states.
     * Useful for the screen to update its display or save changes.
     */
    fun getAllEditorStates(): Map<String, EditorState> = editorStates.toMap()

    /**
     * Subscribe to changes for a specific component type.
     * Returns a function that can be called to unsubscribe.
     */
    fun subscribeToChanges(
        componentType: String,
        listener: (EditorState) -> Unit
    ): () -> Unit {
        stateListeners.getOrPut(componentType) { mutableSetOf() }.add(listener)
        return { stateListeners[componentType]?.remove(listener) }
    }

    /**
     * Subscribe to all state changes in the registry.
     * The listener receives both the component type and the new state.
     * Returns a function that can be called to unsubscribe.
     */
    fun subscribeToAllChanges(
        listener: (String, EditorState) -> Unit
    ): () -> Unit {
        globalListeners.add(listener)
        return { globalListeners.remove(listener) }
    }

    /**
     * Gets a snapshot of all valid component values.
     * Useful when saving changes or updating the item.
     */
    fun getValidComponentValues(): Map<ComponentType<*>, Any> {
        return editorStates
            .filter { it.value.isValid && it.value.value != null }
            .mapNotNull { (componentType, state) ->
                val dataComponent = getDataComponentType(componentType)
                if (dataComponent != null) {
                    @Suppress("UNCHECKED_CAST")
                    dataComponent as ComponentType<Any> to state.value!!
                } else null
            }
            .toMap()
    }

    /**
     * Helper function to convert component type strings to DataComponentType instances.
     */
    private fun getDataComponentType(componentType: String): ComponentType<*>? {
        return win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentTypeByName(componentType)
    }

    private fun notifyGlobalListeners(componentType: String, state: EditorState) {
        globalListeners.forEach { listener ->
            listener(componentType, state)
        }
    }
}