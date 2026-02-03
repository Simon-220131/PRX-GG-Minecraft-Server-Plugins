package at.prx.pRXRanks.util;

public final class UnicodeUtil {

    private UnicodeUtil() {}

    public static String unescapeUnicode(String input) {
        if (input == null) return "";

        StringBuilder out = new StringBuilder(input.length());
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\\' && i + 5 < chars.length && chars[i + 1] == 'u') {
                try {
                    String hex = new String(chars, i + 2, 4);
                    int code = Integer.parseInt(hex, 16);
                    out.append((char) code);
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            out.append(chars[i]);
        }

        return out.toString();
    }
}
