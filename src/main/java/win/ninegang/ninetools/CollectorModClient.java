package win.ninegang.ninetools;

import impl.ui.collector.screens.CollectorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CollectorModClient implements ClientModInitializer {
    private static final KeyBinding OPEN_COLLECTOR = new KeyBinding(
            "key.9tools-collector.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.9tools-collector"
    );

    @Override
    public void onInitializeClient() {
        Ninehack.INSTANCE.logChat("Initializing 9Tools Collector Client...");

        KeyBindingHelper.registerKeyBinding(OPEN_COLLECTOR);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_COLLECTOR.wasPressed()) {
                Ninehack.INSTANCE.logChat("[Keybind] Collector key pressed!");


                CollectorScreen screenInstance = CollectorMod.getCollector();

                if (screenInstance == null) {

                    Ninehack.INSTANCE.logChat("[Keybind] ERROR: Collector screen static instance is NULL! Check main init log.");
                    return;
                }

                if (client.world == null) {
                    Ninehack.INSTANCE.logChat("[Keybind] ERROR: Cannot open GUI when not in a world.");
                    return;
                }


                Ninehack.INSTANCE.logChat("[Keybind] Attempting to open static CollectorScreen instance...");
                try {
                    client.setScreen(screenInstance);
                    Ninehack.INSTANCE.logChat("[Keybind] Set screen call completed.");
                } catch (Exception e) {

                    Ninehack.INSTANCE.logChat("[Keybind] ERROR opening screen: " + e.getMessage());
                    Ninehack.logger.error("[Keybind] Error during setScreen:", e);
                }

            }
        });

        Ninehack.INSTANCE.logChat("9Tools Collector Client Initialized.");
    }
}