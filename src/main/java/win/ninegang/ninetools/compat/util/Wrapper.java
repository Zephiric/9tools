package win.ninegang.ninetools.compat.util;

import net.minecraft.client.MinecraftClient;
import win.ninegang.ninetools.Ninehack;


public interface Wrapper {
    MinecraftClient mc = MinecraftClient.getInstance();
    Ninehack ninehack = Ninehack.INSTANCE;
}