package win.ninegang.ninetools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Ninehack {
    public static final String NAME = "9tools-collector";
    public static final String VERSION = "1.0.0";
    public static final Logger logger = LogManager.getLogger("9tools-collector");
    public static final File DIRECTORY = new File("9tools-collector");

    public static Ninehack INSTANCE;

    static {
        INSTANCE = new Ninehack();


        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
        }
    }

    public void logChat(String message) {

        logger.info(message);
    }
}