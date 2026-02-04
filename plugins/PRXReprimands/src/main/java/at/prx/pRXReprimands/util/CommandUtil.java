package at.prx.pRXReprimands.util;

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

    public record ParsedInput(long durationMillis, String reason) {
    }
}
