package at.prx.pRXWorldCreator.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // 🌍 Plugin Prefix
    private static final String PREFIX =
            "<gradient:#00ffcc:#00aaff><bold>WorldControl</bold></gradient> <dark_gray>»</dark_gray> ";

    // =========================
    // BASIS METHODEN
    // =========================

    public static Component message(String text) {
        return MM.deserialize(PREFIX + text);
    }

    public static Component raw(String text) {
        return MM.deserialize(text);
    }
}
