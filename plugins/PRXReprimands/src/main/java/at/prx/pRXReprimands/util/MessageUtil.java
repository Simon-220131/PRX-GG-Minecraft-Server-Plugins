package at.prx.pRXReprimands.util;

import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {
    public static final String PREFIX = color("&8[&cPRX&8]&r ");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private MessageUtil() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static Component component(String input) {
        return LEGACY.deserialize(input);
    }

    public static Component banScreen(String reason, String durationText) {
        String message = "\n&8&m--------------------------------\n"
                + "&c&lGEBANNT\n"
                + "&7Grund: &f" + reason + "\n"
                + "&7Dauer: &f" + durationText + "\n"
                + "&8Bitte kontaktiere das Team.\n"
                + "&8&m--------------------------------";
        return component(message);
    }
}
