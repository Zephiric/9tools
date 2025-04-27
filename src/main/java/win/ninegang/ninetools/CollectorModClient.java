package win.ninegang.ninetools;

import impl.ui.collector.screens.CollectorScreen; // Make sure this import is correct
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
        Ninehack.INSTANCE.logChat("Initializing 9Tools Collector Client..."); // Use logChat

        KeyBindingHelper.registerKeyBinding(OPEN_COLLECTOR);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_COLLECTOR.wasPressed()) {
                Ninehack.INSTANCE.logChat("[Keybind] Collector key pressed!"); // Use logChat

                // --- Attempt to get and open the static instance ---
                CollectorScreen screenInstance = CollectorMod.getCollector(); // Use the getter

                if (screenInstance == null) {
                    // Log if the static instance wasn't created successfully during initialization
                    Ninehack.INSTANCE.logChat("[Keybind] ERROR: Collector screen static instance is NULL! Check main init log.");
                    return; // Stop if screen is null
                }

                if (client.world == null) {
                    Ninehack.INSTANCE.logChat("[Keybind] ERROR: Cannot open GUI when not in a world.");
                    return; // Stop if not in world
                }

                // If we have an instance and are in a world, try to open it
                Ninehack.INSTANCE.logChat("[Keybind] Attempting to open static CollectorScreen instance...");
                try {
                    client.setScreen(screenInstance); // <-- This is the line that opens the GUI
                    Ninehack.INSTANCE.logChat("[Keybind] Set screen call completed."); // Log success *call*
                } catch (Exception e) {
                    // Log errors if setScreen itself fails
                    Ninehack.INSTANCE.logChat("[Keybind] ERROR opening screen: " + e.getMessage());
                    Ninehack.logger.error("[Keybind] Error during setScreen:", e); // Also log full error to console log
                }
                // --- End attempting to open ---
            }
        });

        Ninehack.INSTANCE.logChat("9Tools Collector Client Initialized."); // Use logChat
    }
}