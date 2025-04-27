package impl.ui.collector.data

import win.ninegang.ninetools.Ninehack
import win.ninegang.ninetools.compat.util.Wrapper.ninehack
import impl.ui.collector.SchematicUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

object CollectorSchematic {
    private val baseDir = Paths.get(Ninehack.NAME, "collector", "schematics")
    private val availableSchematics = mutableMapOf<String, File>()

    init {
        initialize()
    }

    fun initialize() {
        if (!baseDir.exists()) {
            try {
                baseDir.createDirectories()
                ninehack.logChat("Created schematics directory")
            } catch (e: Exception) {
                ninehack.logChat("Failed to create schematics directory: ${e.message}")
            }
        }

        scanForSchematics()
    }

    fun scanForSchematics() {
        availableSchematics.clear()

        if (!baseDir.exists()) {
            return
        }

        try {
            baseDir.toFile().listFiles { file ->
                file.isFile && file.name.lowercase().endsWith(".litematic")
            }?.forEach { file ->
                val nameWithoutExtension = file.name.removeSuffix(".litematic")
                availableSchematics[nameWithoutExtension] = file
            }

            ninehack.logChat("Found ${availableSchematics.size} schematics")
        } catch (e: Exception) {
            ninehack.logChat("Failed to scan for schematics: ${e.message}")
        }
    }

    fun getAvailableSchematics(): List<String> {
        return availableSchematics.keys.sorted()
    }

    fun getSchematicFile(name: String): File? {
        return availableSchematics[name]
    }

    fun loadSchematic(name: String): SchematicUtils.SchematicData? {
        val file = getSchematicFile(name) ?: return null
        return SchematicUtils.readSchematic(file)
    }

    fun getSchematicsDirectory(): Path {
        return baseDir
    }

    fun getSchematicFileFromPath(path: String): File {
        return File(path)
    }
}