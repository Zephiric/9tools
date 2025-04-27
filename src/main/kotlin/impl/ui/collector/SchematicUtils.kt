package impl.ui.collector

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.nbt.*
import net.minecraft.registry.Registries
import net.minecraft.state.property.Property
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import java.io.File
import java.io.FileInputStream

/**
 * Utility class for reading and parsing Litematica schematic files
 */
object SchematicUtils {
    /**
     * Reads a litematic file and returns a SchematicData object
     */
    fun readSchematic(file: File): SchematicData? {
        try {
            val inputStream = FileInputStream(file)
            // Use NbtSizeTracker.ofUnlimitedBytes() instead of UNLIMITED
            val nbt = NbtIo.readCompressed(inputStream, NbtSizeTracker.ofUnlimitedBytes())
            inputStream.close()

            return parseSchematicNBT(nbt)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parses the NBT data of a schematic file
     */
    private fun parseSchematicNBT(nbt: NbtCompound): SchematicData {
        val metadata = parseMetadata(nbt.getCompound("Metadata"))
        val regions = mutableMapOf<String, RegionData>()

        val regionsNbt = nbt.getCompound("Regions")
        for (regionName in regionsNbt.keys) {
            val regionNbt = regionsNbt.getCompound(regionName)
            regions[regionName] = parseRegion(regionNbt)
        }

        return SchematicData(metadata, regions)
    }

    /**
     * Parses the metadata of a schematic file
     */
    private fun parseMetadata(nbt: NbtCompound): SchematicMetadata {
        val enclosingSizeNbt = nbt.getCompound("EnclosingSize")
        val enclosingSize = Vec3i(
            enclosingSizeNbt.getInt("x"),
            enclosingSizeNbt.getInt("y"),
            enclosingSizeNbt.getInt("z")
        )

        return SchematicMetadata(
            name = nbt.getString("Name"),
            author = nbt.getString("Author"),
            description = nbt.getString("Description"),
            timeCreated = nbt.getLong("TimeCreated"),
            timeModified = nbt.getLong("TimeModified"),
            totalBlocks = nbt.getInt("TotalBlocks"),
            totalVolume = nbt.getInt("TotalVolume"),
            regionCount = nbt.getInt("RegionCount"),
            enclosingSize = enclosingSize
        )
    }

    /**
     * Parses a region from a schematic file
     */
    private fun parseRegion(nbt: NbtCompound): RegionData {
        val posNbt = nbt.getCompound("Position")
        val sizeNbt = nbt.getCompound("Size")

        val pos = BlockPos(
            posNbt.getInt("x"),
            posNbt.getInt("y"),
            posNbt.getInt("z")
        )

        val size = BlockPos(
            sizeNbt.getInt("x"),
            sizeNbt.getInt("y"),
            sizeNbt.getInt("z")
        )

        val container = parseBlockContainer(nbt, size)

        return RegionData(pos, size, container)
    }

    /**
     * Parses a block container from a schematic file
     */
    private fun parseBlockContainer(nbt: NbtCompound, size: BlockPos): BlockStateContainer {
        val paletteTag = nbt.getList("BlockStatePalette", NbtElement.COMPOUND_TYPE.toInt())
        val blockStatesArray = nbt.getLongArray("BlockStates")

        return BlockStateContainer(size.x, size.y, size.z, paletteTag, blockStatesArray)
    }

    /**
     * Container for block states with bit-packing logic
     */
    class BlockStateContainer(
        val sizeX: Int,
        val sizeY: Int,
        val sizeZ: Int,
        paletteTag: NbtList,
        private val blockStates: LongArray
    ) {
        private val palette: Array<BlockState>
        private val bitsPerEntry: Int
        private val entriesPerLong: Int
        private val maxEntryValue: Long
        private val totalVolume: Int = Math.abs(sizeX) * Math.abs(sizeY) * Math.abs(sizeZ)

        init {
            // Parse the palette
            val tempPalette = ArrayList<BlockState>()
            for (i in 0 until paletteTag.size) {
                val blockTag = paletteTag.getCompound(i)
                val blockState = parseBlockState(blockTag)
                tempPalette.add(blockState)
            }
            palette = tempPalette.toTypedArray()

            // Calculate bits needed per block ID
            bitsPerEntry = maxOf(2, 32 - Integer.numberOfLeadingZeros(palette.size - 1))
            entriesPerLong = 64 / bitsPerEntry
            maxEntryValue = (1L shl bitsPerEntry) - 1L
        }

        /**
         * Gets the block state at the specified coordinates
         * Now correctly handles all coordinate ranges
         */
        fun getBlock(x: Int, y: Int, z: Int): BlockState {
            // Validate coordinates against absolute dimensions
            if (x < 0 || x >= Math.abs(sizeX) ||
                y < 0 || y >= Math.abs(sizeY) ||
                z < 0 || z >= Math.abs(sizeZ)) {
                return Blocks.AIR.defaultState
            }

            val index = getIndex(x, y, z)

            // Validate index against data
            if (index < 0 || index >= totalVolume) {
                return Blocks.AIR.defaultState
            }

            val paletteIndex = getAt(index)

            return if (paletteIndex >= 0 && paletteIndex < palette.size) {
                palette[paletteIndex]
            } else {
                Blocks.AIR.defaultState
            }
        }

        /**
         * Gets the index in the bit array for the given coordinates
         * Now uses absolute dimensions for calculation
         */
        private fun getIndex(x: Int, y: Int, z: Int): Int {
            val absX = Math.abs(x)
            val absY = Math.abs(y)
            val absZ = Math.abs(z)
            val absSizeX = Math.abs(sizeX)
            val absSizeZ = Math.abs(sizeZ)

            return (absY * absSizeX * absSizeZ) + (absZ * absSizeX) + absX
        }

        /**
         * Gets the value at the specified index in the bit array
         * Added additional validation to prevent array index errors
         */
        private fun getAt(index: Int): Int {
            if (index < 0 || blockStates.isEmpty()) {
                return 0
            }

            val bitIndex = index * bitsPerEntry
            val startLongIndex = bitIndex / 64

            // Validate array bounds
            if (startLongIndex >= blockStates.size) {
                return 0
            }

            val startBitOffset = bitIndex % 64
            val endBitOffset = startBitOffset + bitsPerEntry

            // If the value is contained within a single long
            return if (endBitOffset <= 64) {
                ((blockStates[startLongIndex] ushr startBitOffset) and maxEntryValue).toInt()
            } else {
                // Value spans two longs
                val endLongIndex = startLongIndex + 1

                // Check if the second long exists
                if (endLongIndex >= blockStates.size) {
                    return ((blockStates[startLongIndex] ushr startBitOffset) and maxEntryValue).toInt()
                }

                val value1 = blockStates[startLongIndex] ushr startBitOffset
                val value2 = blockStates[endLongIndex] and ((1L shl (endBitOffset - 64)) - 1L)
                (value1 or (value2 shl (64 - startBitOffset))).toInt()
            }
        }

        /**
         * Parses a block state from NBT data
         */
        private fun parseBlockState(tag: NbtCompound): BlockState {
            val blockId = tag.getString("Name")
            val block = Registries.BLOCK.get(Identifier.of(blockId))

            if (block === Blocks.AIR) {
                return Blocks.AIR.defaultState
            }

            var state = block.defaultState

            if (tag.contains("Properties", NbtElement.COMPOUND_TYPE.toInt())) {
                val properties = tag.getCompound("Properties")
                for (propName in properties.keys) {
                    val property = block.stateManager.getProperty(propName)
                    if (property != null) {
                        state = withPropertyValue(state, property, properties.getString(propName))
                    }
                }
            }

            return state
        }

        /**
         * Sets a property value on a block state
         */
        private fun <T : Comparable<T>> withPropertyValue(state: BlockState, property: Property<T>, valueString: String): BlockState {
            val optional = property.parse(valueString)
            return if (optional.isPresent) {
                state.with(property, optional.get())
            } else {
                state
            }
        }
    }

    /**
     * Data class containing all schematic information
     */
    data class SchematicData(
        val metadata: SchematicMetadata,
        val regions: Map<String, RegionData>
    )

    /**
     * Data class containing schematic metadata
     */
    data class SchematicMetadata(
        val name: String,
        val author: String,
        val description: String = "",
        val timeCreated: Long = 0,
        val timeModified: Long = 0,
        val totalBlocks: Int = 0,
        val totalVolume: Int = 0,
        val regionCount: Int = 0,
        val enclosingSize: Vec3i
    )

    /**
     * Data class containing region information
     */
    data class RegionData(
        val position: BlockPos,
        val size: BlockPos,
        val container: BlockStateContainer
    )
}