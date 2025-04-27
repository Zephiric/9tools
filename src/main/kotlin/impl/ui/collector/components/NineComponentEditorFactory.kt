package impl.ui.collector.components

import impl.ui.collector.components.editors.*
import impl.ui.collector.data.CollectorItem
import net.minecraft.component.ComponentType
import net.minecraft.component.type.*
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Rarity
import win.ninegang.ninetools.compat.util.components.ItemComponentUtil

object NineComponentEditorFactory {
    private data class EditorConfig(
        val registry: EditorRegistry,
        val x: Double,
        val y: Double,
        val item: CollectorItem,
        val currentValue: Any?
    )


    fun hasEditorFor(componentName: String): Boolean {
        return EDITOR_CREATORS.containsKey(componentName)
    }

    private val EDITOR_CREATORS = mapOf<String, (EditorConfig) -> BaseComponentEditor>(
        "minecraft:max_stack_size" to NineComponentEditorFactory::createMaxStackSizeEditor,
        "minecraft:unbreakable" to NineComponentEditorFactory::createUnbreakableEditor,
        "minecraft:enchantment_glint_override" to NineComponentEditorFactory::createGlintEditor,
        "minecraft:custom_name" to NineComponentEditorFactory::createCustomNameEditor,
        "minecraft:item_name" to NineComponentEditorFactory::createItemNameEditor,
        "minecraft:rarity" to NineComponentEditorFactory::createRarityEditor,
        "minecraft:max_damage" to NineComponentEditorFactory::createMaxDamageEditor,
        "minecraft:damage" to NineComponentEditorFactory::createDamageEditor,


        "minecraft:hide_additional_tooltip" to NineComponentEditorFactory::createHideAdditionalTooltipEditor,
        "minecraft:hide_tooltip" to NineComponentEditorFactory::createHideTooltipEditor,
        "minecraft:intangible_projectile" to NineComponentEditorFactory::createIntangibleProjectileEditor,
        "minecraft:fire_resistant" to NineComponentEditorFactory::createFireResistantEditor,
        "minecraft:creative_slot_lock" to NineComponentEditorFactory::createCreativeSlotLockEditor,
        "minecraft:repair_cost" to NineComponentEditorFactory::createRepairCostEditor,

        "minecraft:base_color" to NineComponentEditorFactory::createBaseColorEditor,

        "minecraft:map_color" to NineComponentEditorFactory::createMapColorEditor,
        "minecraft:map_id" to NineComponentEditorFactory::createMapIdEditor,

        "minecraft:map_post_processing" to NineComponentEditorFactory::createMapPostProcessingEditor,
        "minecraft:lore" to NineComponentEditorFactory::createLoreEditor,

        "minecraft:suspicious_stew_effects" to NineComponentEditorFactory::createSuspiciousStewEditor,
        "minecraft:charged_projectiles" to NineComponentEditorFactory::createChargedProjectilesEditor,



        )

    fun createEditor(
        componentType: ComponentType<*>,
        registry: EditorRegistry,
        item: CollectorItem,
        x: Double,
        y: Double
    ): BaseComponentEditor? {
        val componentName = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponentName(componentType)
        val currentValue = win.ninegang.ninetools.compat.util.components.ItemComponentUtil.getComponent(
            CollectorItem.toItemStack(item),
            componentType
        ).orElse(null)

        val config = EditorConfig(registry, x, y, item, currentValue)
        return EDITOR_CREATORS[componentName]?.invoke(config)
    }
    private fun createMaxStackSizeEditor(config: EditorConfig) = MaxStackSizeEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Int ?: 64
    )
    private fun createUnbreakableEditor(config: EditorConfig) = UnbreakableEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = (config.currentValue as? UnbreakableComponent) != null,
        initialTooltip = (config.currentValue as? UnbreakableComponent)?.showInTooltip() ?: true
    )
    private fun createGlintEditor(config: EditorConfig) = EnchantmentGlintOverrideEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Boolean ?: false
    )
    private fun createCustomNameEditor(config: EditorConfig): CustomNameEditor {
        val textComponent = config.currentValue as? Text
        val initialText = textComponent?.string ?: ""

        return CustomNameEditor(
            registry = config.registry,
            x = config.x,
            y = config.y,
            initialValue = initialText,
            initialItalic = textComponent?.style?.isItalic ?: false
        )
    }
    private fun createItemNameEditor(config: EditorConfig): ItemNameEditor {
        val textComponent = config.currentValue as? Text
        val initialText = textComponent?.string ?: ""

        return ItemNameEditor(
            registry = config.registry,
            x = config.x,
            y = config.y,
            initialValue = initialText,
            initialItalic = textComponent?.style?.isItalic ?: false
        )
    }
    private fun createRarityEditor(config: EditorConfig) = RarityEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Rarity
    )
    private fun createMaxDamageEditor(config: EditorConfig) = MaxDamageEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Int
    )
    private fun createDamageEditor(config: EditorConfig) = DamageEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Int
    )
   /* private fun createEnchantmentsEditor(config: EditorConfig) = EnchantmentsEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? ItemEnchantmentsComponent
    )*/
/*    private fun createStoredEnchantmentsEditor(config: EditorConfig) = StoredEnchantmentsEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? ItemEnchantmentsComponent
    )*/
    private fun createHideAdditionalTooltipEditor(config: EditorConfig) = HideAdditionalTooltipEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue != null
    )
    private fun createHideTooltipEditor(config: EditorConfig) = HideTooltipEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue != null
    )
    private fun createIntangibleProjectileEditor(config: EditorConfig) = IntangibleProjectileEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue != null
    )
    private fun createFireResistantEditor(config: EditorConfig) = FireResistantEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue != null
    )
    private fun createCreativeSlotLockEditor(config: EditorConfig) = CreativeSlotLockEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue != null
    )
    private fun createRepairCostEditor(config: EditorConfig) = RepairCostEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? Int
    )
/*    private fun createFoodComponentEditor(config: EditorConfig) = FoodComponentEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? FoodComponent
    )*/
    private fun createBaseColorEditor(config: EditorConfig) = BaseColorEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? DyeColor
    )
/*    private fun createDyedColorEditor(config: EditorConfig) = DyedColorEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? DyedColorComponent
    )*/
    private fun createMapColorEditor(config: EditorConfig) = MapColorEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? MapColorComponent
    )
    private fun createMapIdEditor(config: EditorConfig) = MapIdEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? MapIdComponent
    )
/*    private fun createMapDecorationsEditor(config: EditorConfig) = MapDecorationsEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? MapDecorationsComponent
    )*/
    private fun createMapPostProcessingEditor(config: EditorConfig) = MapPostProcessingEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? MapPostProcessingComponent
    )
    private fun createLoreEditor(config: EditorConfig) = LoreEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? LoreComponent
    )
/*    private fun createPotionContentsEditor(config: EditorConfig) = PotionContentsEditor(
        registry = config.registry,
        x = config.x,
        y = config.y
    )*/
    private fun createSuspiciousStewEditor(config: EditorConfig) = SuspiciousStewEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? SuspiciousStewEffectsComponent
    )
    private fun createChargedProjectilesEditor(config: EditorConfig) = ChargedProjectilesEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialComponent = config.currentValue as? ChargedProjectilesComponent
    )
/*    private fun createCustomModelDataEditor(config: EditorConfig) = CustomModelDataEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? CustomModelDataComponent
    )*/
/*    private fun createContainerLockEditor(config: EditorConfig) = ContainerLockEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? ContainerLock
    )*/
/*    private fun createInstrumentEditor(config: EditorConfig) = InstrumentEditor(
        registry = config.registry,
        x = config.x,
        y = config.y,
        initialValue = config.currentValue as? RegistryEntry<Instrument>
    )*/
}
