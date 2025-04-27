package win.ninegang.ninetools.compat.util.components;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mojang.text2speech.Narrator.LOGGER;

/**
 * Utility class for managing components within ItemStacks.
 * Provides methods to apply, remove, retrieve, serialize, and deserialize components.
 */
public class ItemComponentUtil {

    /**
     * Retrieves all components from the given ItemStack.
     *
     * @param itemStack The ItemStack from which to retrieve components.
     * @return A list of Components present in the ItemStack.
     */
    public static List<Component<?>> getAllComponents(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Collections.emptyList();
        }

        ComponentMap componentMap = itemStack.getComponents();
        List<Component<?>> componentsList = new ArrayList<>();
        for (Component<?> component : componentMap) {
            componentsList.add(component);
        }

        return componentsList;
    }

    /**
     * Retrieves a specific component's value from the ItemStack.
     *
     * @param itemStack The ItemStack from which to retrieve the component.
     * @param type      The DataComponentType of the component.
     * @param <T>       The type parameter of the component.
     * @return An Optional containing the component's value if present.
     */
    public static <T> Optional<T> getComponent(ItemStack itemStack, ComponentType<T> type) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemStack.get(type));
    }

    /**
     * Adds or updates a component on the given ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @param type      The DataComponentType of the component.
     * @param value     The value of the component.
     * @param <T>       The type parameter of the component.
     */
    public static <T> void addOrUpdateComponent(ItemStack itemStack, ComponentType<T> type, T value) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        itemStack.set(type, value);
    }

    /**
     * Removes a component from the given ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @param type      The DataComponentType of the component to remove.
     */
    public static void removeComponent(ItemStack itemStack, ComponentType<?> type) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        itemStack.remove(type);
    }

    /**
     * Serializes all components of an ItemStack into an NbtCompound.
     *
     * @param itemStack The ItemStack to serialize.
     * @return An NbtCompound representing the item's components.
     */
    @SuppressWarnings("unchecked")
    public static NbtCompound serializeComponents(ItemStack itemStack) {
        NbtCompound nbt = new NbtCompound();
        if (itemStack == null || itemStack.isEmpty()) {
            return nbt;
        }

        ComponentMap componentMap = itemStack.getComponents();
        for (Component<?> component : componentMap) {
            ComponentType<?> type = component.type();
            String componentName = getComponentName(type);
            Object value = component.value();


            Codec<Object> codec = (Codec<Object>) type.getCodec();
            if (codec != null) {

                DataResult<NbtElement> encodedResult = codec.encodeStart(NbtOps.INSTANCE, value);
                if (encodedResult.result().isPresent()) {
                    NbtElement encoded = encodedResult.result().get();
                    nbt.put(componentName, encoded);
                } else {

                    encodedResult.error().ifPresent(error -> {
                        LOGGER.warn("Failed to encode component '{}': {}", componentName, error.message());
                    });
                }
            }
        }

        return nbt;
    }

    /**
     * Deserializes components from an NbtCompound and applies them to an ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @param nbt       The NbtCompound containing component data.
     */
    @SuppressWarnings("unchecked")
    public static void deserializeComponents(ItemStack itemStack, NbtCompound nbt) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        ComponentMap.Builder builder = ComponentMap.builder();
        for (String key : nbt.getKeys()) {
            ComponentType<?> type = getComponentTypeByName(key);
            if (type != null) {
                Codec<?> codec = type.getCodec();
                if (codec != null) {
                    NbtElement element = nbt.get(key);
                    DataResult<Pair<Object, NbtElement>> decodedResult = ((Codec<Object>) codec).decode(NbtOps.INSTANCE, element);
                    if (decodedResult.result().isPresent()) {
                        Object value = decodedResult.result().get().getFirst();
                        builder.add((ComponentType<Object>) type, value);
                    } else {

                        decodedResult.error().ifPresent(error -> {
                            LOGGER.warn("Failed to decode component '{}': {}", key, error.message());
                        });
                    }
                }
            }
        }
        itemStack.applyComponentsFrom(builder.build());
    }


    /**
     * Retrieves a DataComponentType<?> by its name using the registry.
     *
     * @param name The name of the DataComponentType.
     * @return The corresponding DataComponentType<?> if found, else null.
     */
    public static ComponentType<?> getComponentTypeByName(String name) {
        Identifier identifier = Identifier.tryParse(name);
        if (identifier == null) {
            return null;
        }
        return Registries.DATA_COMPONENT_TYPE.get(identifier);
    }

    /**
     * Retrieves the name of the DataComponentType using the registry.
     *
     * @param type The DataComponentType<?> instance.
     * @return The name as a String.
     */
    public static String getComponentName(ComponentType<?> type) {
        Identifier id = Registries.DATA_COMPONENT_TYPE.getId(type);
        return id != null ? id.toString() : type.toString();
    }

    /**
     * Retrieves the name of the DataComponentType using the registry,
     * with the "minecraft:" prefix removed for cleaner display.
     *
     * @param type The DataComponentType<?> instance.
     * @return The name as a String, without the "minecraft:" prefix.
     */
    public static String getCleanedComponentName(ComponentType<?> type) {
        Identifier id = Registries.DATA_COMPONENT_TYPE.getId(type);
        String fullName = id != null ? id.toString() : type.toString();

        return fullName.replace("minecraft:", "");
    }

    /**
     * Retrieves all defined DataComponentTypes from the DataComponentTypes class using reflection.
     *
     * @return A list of all DataComponentType<?> instances.
     */
    public static List<ComponentType<?>> getAllDataComponentTypes() {
        List<ComponentType<?>> dataComponentTypes = new ArrayList<>();

        Field[] fields = DataComponentTypes.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && ComponentType.class.isAssignableFrom(field.getType())) {
                try {
                    ComponentType<?> type = (ComponentType<?>) field.get(null);
                    if (type != null) {
                        dataComponentTypes.add(type);
                    }
                } catch (IllegalAccessException e) {

                    LOGGER.error("Failed to access DataComponentType field '{}'", field.getName(), e);
                }
            }
        }

        return dataComponentTypes;
    }

    /**
     * Applies multiple component changes to the given ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @param changes   A map of DataComponentType<?> to their new values.
     */
    public static void applyBulkChanges(ItemStack itemStack, Map<ComponentType<?>, Object> changes) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        for (Map.Entry<ComponentType<?>, Object> entry : changes.entrySet()) {
            ComponentType<?> type = entry.getKey();
            Object value = entry.getValue();
            applyChange(itemStack, type, value);
        }
    }

    /**
     * Helper method to apply a single component change with proper type casting.
     *
     * @param itemStack The ItemStack to modify.
     * @param type      The DataComponentType<?> of the component.
     * @param value     The new value of the component.
     * @param <T>       The type parameter of the component.
     */
    @SuppressWarnings("unchecked")
    private static <T> void applyChange(ItemStack itemStack, ComponentType<?> type, Object value) {
        try {
            ComponentType<T> typedType = (ComponentType<T>) type;
            T typedValue = (T) value;
            itemStack.set(typedType, typedValue);
        } catch (ClassCastException e) {

            System.err.println("Type mismatch when applying component change: " + e.getMessage());
        }
    }

    /**
     * Clears all components from the given ItemStack.
     *
     * @param itemStack The ItemStack to clear components from.
     */
    public static void clearAllComponents(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        List<ComponentType<?>> allTypes = getAllDataComponentTypes();
        for (ComponentType<?> type : allTypes) {
            itemStack.remove(type);
        }
    }

    /**
     * Checks if a component is compatible with the given ItemStack.
     * For example, certain components may only be applicable to specific item types.
     *
     * @param itemStack     The ItemStack to check against.
     * @param componentType The DataComponentType<?> to check compatibility for.
     * @return True if compatible, false otherwise.
     */
    public static boolean isCompatible(ItemStack itemStack, ComponentType<?> componentType) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        ComponentMap testMap = ComponentMap.of(
                itemStack.getComponents(),
                ComponentMap.builder().add(componentType, null).build()
        );
        return ItemStack.validateComponents(testMap).result().isPresent();
    }

    /**
     * Retrieves a map of all component names to their corresponding DataComponentType<?> instances.
     *
     * @return A map where keys are component names and values are DataComponentType<?> instances.
     */
    public static Map<String, ComponentType<?>> getComponentNameMap() {
        return getAllDataComponentTypes().stream()
                .collect(Collectors.toMap(ItemComponentUtil::getComponentName, Function.identity()));
    }

    /**
     * Inspects and logs all fields of a given DataComponentType, including inherited fields.
     *
     * @param type The DataComponentType to inspect.
     */
    public static void inspectComponentType(ComponentType<?> type) {
        if (type == null) {
            LOGGER.warn("DataComponentType is null.");
            return;
        }

        Class<?> clazz = type.getClass();
        LOGGER.info("Inspecting DataComponentType: {}", getComponentName(type));

        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(type);
                    LOGGER.info("Field: {} | Type: {} | Value: {}", field.getName(), field.getType().getSimpleName(), value);
                } catch (IllegalAccessException e) {
                    LOGGER.error("Failed to access field '{}'", field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}