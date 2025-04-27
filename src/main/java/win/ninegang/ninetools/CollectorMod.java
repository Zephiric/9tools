package win.ninegang.ninetools;

import impl.ui.collector.screens.CollectorScreen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectorMod implements ModInitializer {
    public static final String MOD_ID = "9tools-collector";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static CollectorScreen collector;

    @Override
    public void onInitialize() {

        LOGGER.info("Initializing 9Tools Collector");



        try {
            collector = new CollectorScreen();
            LOGGER.info("CollectorScreen static instance created.");
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to create CollectorScreen static instance!", e);

        }


    }


    public static CollectorScreen getCollector() {
        return collector;
    }
}