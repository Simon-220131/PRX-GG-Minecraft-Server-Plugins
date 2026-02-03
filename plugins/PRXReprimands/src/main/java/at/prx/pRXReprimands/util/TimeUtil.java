package at.prx.pRXReprimands.util;

import java.util.Locale;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        long total = 0L;
        long current = 0L;
        boolean foundUnit = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                current = (current * 10) + (c - '0');
                continue;
            }

            long multiplier = switch (c) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                case 'y' -> 31_536_000_000L;
                default -> -1L;
            };

            if (multiplier < 0 || current <= 0) {
                return -1L;
            }

            total += current * multiplier;
            current = 0L;
            foundUnit = true;
        }

        if (!foundUnit || current > 0) {
            return -1L;
        }

        return total;
    }

    public static String formatRemaining(long endMillis) {
        long remaining = endMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            return "0s";
        }

        long seconds = remaining / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        seconds %= 60L;
        minutes %= 60L;
        hours %= 24L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }
}
