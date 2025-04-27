package win.ninegang.ninetools.compat.util.chat;

import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.ninegang.ninetools.compat.util.Wrapper;

public class MessageSendHelper {
    private static final Logger logger = LogManager.getLogger("9tools-collector");

    /**
     * Logs a message to console and game chat if available
     */
    public static void logChat(String message) {
        logger.info(message);
        try {
            if (Wrapper.mc.inGameHud != null) {
                Wrapper.mc.inGameHud.getChatHud().addMessage(Text.literal("[9tools] " + message));
            }
        } catch (Exception e) {

        }
    }

    /**
     * Logs a warning message
     */
    public static void logWarning(String message) {
        logChat("[!] " + message);
    }

    /**
     * Logs an error message
     */
    public static void logError(String message) {
        logChat("[ERROR] " + message);
    }
}