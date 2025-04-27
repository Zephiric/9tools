package impl.ui.collector

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import win.ninegang.ninetools.compat.util.Wrapper.mc
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object SettingsManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val settingsFile = File(mc.runDirectory, "CollectorSettings.json")

    var itemsPerPage: Int = 20
    var showDetailedTooltips = true
    var darkMode = false
    var uiScale = 1.0
    var uiOpacity = 0.8
    var accentColor = Color(0.4f, 0.6f, 1f, 1f)
    var dropdownVisibleItems = 15
    var skipCustomData = false
    var skipWritableBookContent = false
    var skipContainerData = false
    var defaultViewType = ViewType.GRID
    var saveDefaultComponents = true

    private val screenPositions = mutableMapOf<String, ScreenPosition>()

    data class ScreenPosition(
        val x: Double,
        val y: Double
    )

    private data class SettingsData(
        val showDetailedTooltips: Boolean = true,
        val darkMode: Boolean = false,
        val uiScale: Double = 1.0,
        val uiOpacity: Double = 0.8,
        val accentColorRed: Float = 0.4f,
        val accentColorGreen: Float = 0.6f,
        val accentColorBlue: Float = 1f,
        val accentColorAlpha: Float = 1f,
        val dropdownVisibleItems: Int = 6,
        val screenPositions: Map<String, ScreenPosition> = emptyMap(),
        val itemsPerPage: Int = 20,
        val skipCustomData: Boolean = false,
        val skipWritableBookContent: Boolean = false,
        val skipContainerData: Boolean = false,
        val saveDefaultComponents: Boolean = true,
        val defaultViewType: String = ViewType.GRID.name
    )

    fun loadSettings() {
        try {
            if (!settingsFile.exists()) {
                saveSettings()
                return
            }

            FileReader(settingsFile).use { reader ->
                val settings = gson.fromJson(reader, SettingsData::class.java)

                showDetailedTooltips = settings.showDetailedTooltips
                darkMode = settings.darkMode
                uiScale = settings.uiScale
                uiOpacity = settings.uiOpacity
                accentColor = Color(
                    settings.accentColorRed,
                    settings.accentColorGreen,
                    settings.accentColorBlue,
                    settings.accentColorAlpha
                )
                skipCustomData = settings.skipCustomData
                skipWritableBookContent = settings.skipWritableBookContent
                skipContainerData = settings.skipContainerData
                dropdownVisibleItems = settings.dropdownVisibleItems.coerceIn(3, 30)
                itemsPerPage = settings.itemsPerPage.coerceIn(1, 64)
                saveDefaultComponents = settings.saveDefaultComponents

                try {
                    defaultViewType = ViewType.valueOf(settings.defaultViewType)
                } catch (e: Exception) {
                    defaultViewType = ViewType.GRID
                }

                screenPositions.clear()
                screenPositions.putAll(settings.screenPositions)
            }
        } catch (e: Exception) {
            println("Failed to load settings: ${e.message}")
            saveSettings()
        }
    }

    fun saveSettings() {
        try {
            settingsFile.parentFile.mkdirs()

            val settings = SettingsData(
                showDetailedTooltips = showDetailedTooltips,
                darkMode = darkMode,
                uiScale = uiScale,
                uiOpacity = uiOpacity,
                accentColorRed = accentColor.red / 255f,
                accentColorGreen = accentColor.green / 255f,
                accentColorBlue = accentColor.blue / 255f,
                accentColorAlpha = accentColor.alpha / 255f,
                dropdownVisibleItems = dropdownVisibleItems,
                screenPositions = screenPositions.toMap(),
                itemsPerPage = itemsPerPage,
                skipCustomData = skipCustomData,
                skipWritableBookContent = skipWritableBookContent,
                skipContainerData = skipContainerData,
                saveDefaultComponents = saveDefaultComponents,
                defaultViewType = defaultViewType.name
            )

            FileWriter(settingsFile).use { writer ->
                gson.toJson(settings, writer)
            }
        } catch (e: Exception) {
            println("Failed to save settings: ${e.message}")
        }
    }

    fun saveScreenPosition(screenName: String, x: Double, y: Double) {
        screenPositions[screenName] = ScreenPosition(x, y)
        saveSettings()
    }

    fun getScreenPosition(screenName: String): ScreenPosition? {
        return screenPositions[screenName]
    }
}