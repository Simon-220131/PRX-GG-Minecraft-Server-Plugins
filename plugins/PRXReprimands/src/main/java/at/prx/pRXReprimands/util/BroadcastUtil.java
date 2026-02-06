package at.prx.pRXReprimands.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public final class BroadcastUtil {
    private BroadcastUtil() {
    }

    public static void send(Plugin plugin, String messagePath, Map<String, String> placeholders) {
        if (!plugin.getConfig().getBoolean("broadcasts.enabled", true)) {
            return;
        }
        String template = plugin.getConfig().getString(messagePath);
        if (template == null || template.isBlank()) {
            return;
        }
        String permission = plugin.getConfig().getString("broadcasts.permission", "prxreprimands.notify");
        String message = MessageUtil.color(applyPlaceholders(template, placeholders));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
