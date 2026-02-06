package at.prx.pRXReprimands.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static ParsedInput parseDurationAndReason(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return new ParsedInput(-1L, "Keine Angabe");
        }

        long duration = TimeUtil.parseDurationMillis(args[startIndex]);
        if (duration > 0) {
            String reason = args.length > startIndex + 1
                    ? String.join(" ", Arrays.copyOfRange(args, startIndex + 1, args.length))
                    : "Keine Angabe";
            return new ParsedInput(duration, reason);
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        return new ParsedInput(-1L, reason);
    }

    public static boolean requirePermission(org.bukkit.command.CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cKeine Berechtigung."));
        return false;
    }

    public static boolean isBypassed(OfflinePlayer target, String permission) {
        if (target instanceof Player player) {
            return player.hasPermission(permission);
        }
        return false;
    }

    public static String resolveReasonTemplate(Plugin plugin, String input) {
        if (plugin == null || input == null) {
            return input;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return input;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("reason-templates");
        if (section == null) {
            return input;
        }
        String key = trimmed;
        String value = section.getString(key);
        if (value == null) {
            for (String candidate : section.getKeys(false)) {
                if (candidate.equalsIgnoreCase(key)) {
                    value = section.getString(candidate);
                    break;
                }
            }
        }
        return value != null ? value : input;
    }

    public record ParsedInput(long durationMillis, String reason) {
    }

}
