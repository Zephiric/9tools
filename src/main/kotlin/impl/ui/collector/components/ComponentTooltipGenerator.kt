package impl.ui.collector.components

import impl.ui.collector.data.CollectorItem
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import win.ninegang.ninetools.compat.util.Wrapper.mc
import impl.ui.collector.SettingsManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified tooltip handler for both collector items and component items.
 * Handles generation and rendering of tooltips.
 */
object TooltipHandler {
    /**
     * Generates a complete tooltip for any item
     */
    fun generateTooltip(
        stack: ItemStack,
        collectorItem: CollectorItem? = null
    ): MutableList<Text> {
        val tooltipContext = createTooltipContext()
        val tooltipType = determineTooltipType()

        // Get the base tooltip
        val tooltipLines = stack.getTooltip(tooltipContext, mc.player, tooltipType).toMutableList()

        // Check if we need to fix the name formatting at index 0
        if (tooltipLines.isNotEmpty() && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            // Get the name and apply gold formatting while preserving other styles
            val customName = stack.getName()
            tooltipLines[0] = customName.copy().setStyle(
                customName.style
                    .withColor(Formatting.GOLD)
                    .withItalic(true) // Custom names are always italic
            )
        }

        // Add collector info if present
        collectorItem?.let {
            addCollectorInfo(it, tooltipLines)
        }

        // Add component details if appropriate
        if (shouldShowDetailedComponents(stack)) {
            addComponentDetails(stack, tooltipLines)
        }

        return tooltipLines
    }

    /**
     * Renders the tooltip at the given position
     */
    fun renderTooltip(context: DrawContext, tooltipLines: List<Text>, mouseX: Int, mouseY: Int) {
        context.drawTooltip(mc.textRenderer, tooltipLines, mouseX, mouseY)
    }

    /**
     * Adds collector-specific information to tooltip
     */
    private fun addCollectorInfo(collectorItem: CollectorItem, tooltipLines: MutableList<Text>) {
        tooltipLines.add(Text.empty()) // Separator

        tooltipLines.add(
            Text.literal("Collection Info")
                .setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE))
        )

        tooltipLines.add(
            Text.literal("Given by: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(collectorItem.givenBy).formatted(Formatting.DARK_GRAY))
        )

        tooltipLines.add(
            Text.literal("Location: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(collectorItem.location).formatted(Formatting.DARK_GRAY))
        )

        val dateFormat = SimpleDateFormat("MM/dd/yyyy")
        val formattedDate = dateFormat.format(Date(collectorItem.dateReceived))
        tooltipLines.add(
            Text.literal("Received: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(formattedDate).formatted(Formatting.DARK_GRAY))
        )

        // Description with wrapping
        collectorItem.description?.let { desc ->
            if (desc.isNotEmpty()) {
                tooltipLines.add(Text.empty())
                wrapText(desc, 200).forEach { line ->
                    tooltipLines.add(
                        Text.literal(line)
                            .formatted(Formatting.DARK_GRAY, Formatting.ITALIC)
                    )
                }
            }
        }
    }

    /**
     * Adds component-specific details to tooltip
     */
    private fun addComponentDetails(stack: ItemStack, tooltipLines: MutableList<Text>) {
        if (!stack.components.isEmpty() && shouldShowDetailedComponents(stack)) {
            tooltipLines.add(Text.empty())
            tooltipLines.add(Text.literal("Components:").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))

            stack.components.forEach { component ->
                tooltipLines.add(
                    Text.literal("  ${component.type()}")
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                )
            }
        }
    }

    private fun formatCustomName(name: Text, tooltipLines: MutableList<Text>) {
        tooltipLines.add(
            Text.literal("  Name: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                .append(name)
        )
    }

    private fun formatLore(lore: Any, tooltipLines: MutableList<Text>) {
        tooltipLines.add(
            Text.literal("  Lore:")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
        )

        when (lore) {
            is LoreComponent -> {
                lore.styledLines().forEach { line ->
                    tooltipLines.add(
                        Text.literal("    ")
                            .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                            .append(line)
                    )
                }
            }
            is Text -> {
                wrapText(lore.string, 200).forEach { line ->
                    tooltipLines.add(
                        Text.literal("    $line")
                            .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                    )
                }
            }
            else -> {
                tooltipLines.add(
                    Text.literal("    Invalid lore format")
                        .setStyle(Style.EMPTY.withColor(Formatting.RED))
                )
            }
        }
    }

    private fun formatEnchantments(enchants: Any, tooltipLines: MutableList<Text>) {
        tooltipLines.add(
            Text.literal("  Enchantments:")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
        )

        if (enchants is ItemEnchantmentsComponent) {
            enchants.enchantmentEntries.forEach { entry ->
                tooltipLines.add(
                    Text.literal("    ${entry.key.value()} ${entry.intValue}")
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                )
            }
        }
    }

    private fun formatGenericComponent(type: ComponentType<*>, value: Any, tooltipLines: MutableList<Text>) {
        tooltipLines.add(
            Text.literal("  ${type}: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                .append(
                    Text.literal(value.toString())
                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                )
        )
    }

    /**
     * Helper function to wrap text
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (mc.textRenderer.getWidth(testLine) > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }

    private fun createTooltipContext(): Item.TooltipContext {
        return Item.TooltipContext.create(mc.world)
    }

    private fun determineTooltipType(): TooltipType {
        return if (mc.options.advancedItemTooltips) TooltipType.ADVANCED else TooltipType.BASIC
    }

    private fun shouldShowDetailedComponents(stack: ItemStack): Boolean {
        if (!SettingsManager.showDetailedTooltips) return false
        if (stack.contains(DataComponentTypes.HIDE_TOOLTIP)) return false
        if (stack.contains(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP)) return false
        return mc.options.advancedItemTooltips
    }
}