package win.ninegang.ninetools;

import impl.ui.collector.screens.CollectorScreen; // Make sure this import is correct
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectorMod implements ModInitializer {
    public static final String MOD_ID = "9tools-collector";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID); // Standard logger

    // --- Keep the static instance ---
    public static CollectorScreen collector;

    @Override
    public void onInitialize() {
        // Use standard logger for initialization message
        LOGGER.info("Initializing 9Tools Collector");

        // --- Initialize the static instance ---
        // If this line throws an error, the screen will be null later
        try {
            collector = new CollectorScreen();
            LOGGER.info("CollectorScreen static instance created.");
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to create CollectorScreen static instance!", e);
            // Collector will remain null if creation fails
        }

        // Any other initialization code
    }

    // --- Keep the getter for the static instance ---
    public static CollectorScreen getCollector() {
        return collector;
    }
}