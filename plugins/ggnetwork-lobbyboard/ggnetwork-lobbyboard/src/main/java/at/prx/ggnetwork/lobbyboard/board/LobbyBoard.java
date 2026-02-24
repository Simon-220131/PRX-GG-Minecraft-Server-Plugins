package at.prx.ggnetwork.lobbyboard.board;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Flicker-free Lobby Scoreboard (Paper 1.21+)
 * Layout:
 * 15 separator top
 * 14 free
 * 13 RANG:
 * 12 LuckPerms prefix (fallback: rank glyph)
 * 11 free
 * 10 NAME:
 *  9 player name
 *  8 free
 *  7 ONLINE: {zahl}
 *  6 free
 *  5 PING: {ping}
 *  4 free
 *  3 separator bottom
 *  2 animated server ip
 *  1 free
 */
public final class LobbyBoard {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SEC = LegacyComponentSerializer.legacySection();

    // 15 unique entries (never change!)
    private static final String[] ENTRIES = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74",
            "\u00A75", "\u00A76", "\u00A77", "\u00A78", "\u00A79",
            "\u00A7a", "\u00A7b", "\u00A7c", "\u00A7d", "\u00A7e"
    };

    private static final int L15 = 15;
    private static final int L14 = 14;
    private static final int L13 = 13;
    private static final int L12 = 12;
    private static final int L11 = 11;
    private static final int L10 = 10;
    private static final int L9 = 9;
    private static final int L8 = 8;
    private static final int L7 = 7;
    private static final int L6 = 6;
    private static final int L5 = 5;
    private static final int L4 = 4;
    private static final int L3 = 3;
    private static final int L2 = 2;
    private static final int L1 = 1;

    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private final UUID playerId;

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, Team> teams = new HashMap<>();

    // caches
    private int lastOnline = -1;
    private int lastPing = -1;
    private String lastPrimaryGroup = "";
    private String lastRankPrefix = "";

    // title animation
    private int titleShift = 0;

    // config-derived
    private final String titleText;
    private final int[] titlePalette;
    private final String labelStart;
    private final String labelEnd;
    private final String serverIpText;
    private final int pingGreenMax;
    private final int pingYellowMax;

    public LobbyBoard(JavaPlugin plugin, LuckPerms luckPerms, Player player) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.playerId = player.getUniqueId();

        this.titleText = plugin.getConfig().getString("title-text", "GG-NETWORK");
        this.titlePalette = readPalette(plugin.getConfig().getStringList("title-palette"),
                new int[]{0xFF004C, 0x6A00FF, 0x00D4FF});
        this.labelStart = plugin.getConfig().getString("label-gradient.start", "#ff004c");
        this.labelEnd = plugin.getConfig().getString("label-gradient.end", "#6a00ff");
        this.serverIpText = plugin.getConfig().getString("server-ip", "gg-network.net");
        this.pingGreenMax = plugin.getConfig().getInt("ping-colors.green-max", 79);
        this.pingYellowMax = plugin.getConfig().getInt("ping-colors.yellow-max", 149);

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) throw new IllegalStateException("ScoreboardManager is null");

        this.scoreboard = mgr.getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("lobby", Criteria.DUMMY, Component.text(" "));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Register lines once
        registerLine(L15, 0);
        registerLine(L14, 1);
        registerLine(L13, 2);
        registerLine(L12, 3);
        registerLine(L11, 4);
        registerLine(L10, 5);
        registerLine(L9, 6);
        registerLine(L8, 7);
        registerLine(L7, 8);
        registerLine(L6, 9);
        registerLine(L5, 10);
        registerLine(L4, 11);
        registerLine(L3, 12);
        registerLine(L2, 13);
        registerLine(L1, 14);

        // Static
        setLine(L15, separatorLine());
        setLine(L14, Component.text(" "));
        setLine(L13, label("RANG:"));
        setLine(L11, Component.text(" "));
        setLine(L10, label("NAME:"));
        setLine(L9, Component.text(player.getName()));
        setLine(L8, Component.text(" "));
        setLine(L6, Component.text(" "));
        setLine(L4, Component.text(" "));
        setLine(L3, separatorLine());
        setLine(L1, Component.text(" "));

        // Initial dynamics
        updateOnline(true);
        updatePing(true);
        updateRankFromLuckPerms();
        syncNametagTeams();

        // Apply once
        player.setScoreboard(scoreboard);

        // Title initial
        updateTitle();
    }

    private Component label(String text) {
        // bold + static gradient
        return MM.deserialize("<bold><gradient:" + labelStart + ":" + labelEnd + ">" + text + "</gradient></bold>");
    }

    public void updateTitle() {
        titleShift = (titleShift + 1) % 600;
        objective.displayName(animatedGradientText(titleText, titleShift, titlePalette));
        setLine(L2, animatedGradientText(serverIpText, titleShift, titlePalette));
    }

    public void updateOnline(boolean force) {
        // NOTE: This is ONLY backend online.
        // Replace this if you have proxy/redis global online.
        int online = Bukkit.getOnlinePlayers().size();
        if (!force && online == lastOnline) return;
        lastOnline = online;

        Component line = label("ONLINE:")
                .append(Component.text(" "))
                .append(Component.text(online));
        setLine(L7, line);
    }

    public void updatePing(boolean force) {
        Player p = Bukkit.getPlayer(playerId);
        if (p == null) return;

        int ping = p.getPing();
        if (!force && ping == lastPing) return;
        lastPing = ping;

        Component line = label("PING")
                .append(Component.text(": "))
                .append(Component.text(ping + "ms").color(resolvePingColor(ping)));

        setLine(L5, line);
    }

    public void updateRankFromLuckPerms() {
        Player p = Bukkit.getPlayer(playerId);
        if (p == null) return;

        User user = luckPerms.getUserManager().getUser(playerId);
        if (user == null) {
            // User may not be loaded yet; we'll just try again next time.
            return;
        }

        String primaryGroup = user.getPrimaryGroup();
        if (primaryGroup == null) primaryGroup = "default";

        QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(p);
        if (queryOptions == null) return;
        String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
        if (prefix == null) prefix = "";

        // Update only if changed
        if (primaryGroup.equalsIgnoreCase(lastPrimaryGroup) && prefix.equals(lastRankPrefix)) return;
        lastPrimaryGroup = primaryGroup;
        lastRankPrefix = prefix;

        if (!prefix.isBlank()) {
            setLine(L12, parseLuckPermsPrefix(prefix));
            return;
        }

        // Fallback if no LP prefix is set for the player.
        setLine(L12, Component.text(resolveRankGlyph(primaryGroup)));
    }

    private Component parseLuckPermsPrefix(String prefix) {
        String value = decodeUnicodeEscapes(prefix).trim();
        if (value.isEmpty()) return Component.empty();

        if (value.contains("<") && value.contains(">")) {
            try {
                return MM.deserialize(value);
            } catch (Exception ignored) {
                // Falls through to legacy parsing.
            }
        }

        if (value.indexOf('&') >= 0) {
            return LEGACY_AMP.deserialize(value);
        }
        return LEGACY_SEC.deserialize(value);
    }

    private String decodeUnicodeEscapes(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 5 < input.length()) {
                char u = input.charAt(i + 1);
                if (u == 'u' || u == 'U') {
                    String hex = input.substring(i + 2, i + 6);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 6;
                        continue;
                    } catch (NumberFormatException ignored) {
                        // Not a valid unicode escape, keep original chars.
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private String resolveRankGlyph(String primaryGroup) {
        // group-glyphs.<group>
        String path = "group-glyphs." + primaryGroup.toLowerCase(Locale.ROOT);
        String g = plugin.getConfig().getString(path);
        if (g != null && !g.isBlank()) return g;
        return plugin.getConfig().getString("glyphs.rank", "\uE010");
    }

    private TextColor resolvePingColor(int ping) {
        int greenMax = Math.min(pingGreenMax, pingYellowMax);
        int yellowMax = Math.max(pingGreenMax, pingYellowMax);

        if (ping <= greenMax) return TextColor.color(0x55FF55); // green
        if (ping <= yellowMax) return TextColor.color(0xFFFF55); // yellow
        return TextColor.color(0xFF5555); // red
    }

    private Component separatorLine() {
        return Component.text("-------------------").color(TextColor.color(0x7A7A7A));
    }

    public void destroy() {
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            ScoreboardManager mgr = Bukkit.getScoreboardManager();
            if (mgr != null) p.setScoreboard(mgr.getMainScoreboard());
        }

        try { objective.unregister(); } catch (Throwable ignored) {}
        for (Team t : teams.values()) {
            try { t.unregister(); } catch (Throwable ignored) {}
        }
        teams.clear();
    }

    public void syncNametagTeams() {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        Scoreboard main = mgr.getMainScoreboard();
        Set<String> mainTeamNames = new HashSet<>();

        for (Team source : main.getTeams()) {
            String teamName = source.getName();
            if (teamName.startsWith("l_")) continue;
            mainTeamNames.add(teamName);

            Team target = scoreboard.getTeam(teamName);
            if (target == null) {
                try {
                    target = scoreboard.registerNewTeam(teamName);
                } catch (IllegalArgumentException ignored) {
                    target = scoreboard.getTeam(teamName);
                }
            }
            if (target == null) continue;

            copyTeamSettings(source, target);
            syncTeamEntries(source, target);
        }

        for (Team local : new ArrayList<>(scoreboard.getTeams())) {
            String localName = local.getName();
            if (localName.startsWith("l_")) continue;
            if (!mainTeamNames.contains(localName)) {
                try { local.unregister(); } catch (Throwable ignored) {}
            }
        }
    }

    // -------- internals --------

    private void copyTeamSettings(Team source, Team target) {
        target.displayName(source.displayName());
        target.prefix(source.prefix());
        target.suffix(source.suffix());
        target.setAllowFriendlyFire(source.allowFriendlyFire());
        target.setCanSeeFriendlyInvisibles(source.canSeeFriendlyInvisibles());
        for (Team.Option option : Team.Option.values()) {
            target.setOption(option, source.getOption(option));
        }
        try {
            target.setColor(source.getColor());
        } catch (Throwable ignored) {}
    }

    private void syncTeamEntries(Team source, Team target) {
        Set<String> sourceEntries = source.getEntries();
        Set<String> currentEntries = new HashSet<>(target.getEntries());

        for (String entry : currentEntries) {
            if (!sourceEntries.contains(entry)) {
                target.removeEntry(entry);
            }
        }
        for (String entry : sourceEntries) {
            if (!target.hasEntry(entry)) {
                target.addEntry(entry);
            }
        }
    }

    private void registerLine(int score, int entryIndex) {
        String entry = ENTRIES[entryIndex];

        Team team = scoreboard.getTeam("l_" + score);
        if (team == null) team = scoreboard.registerNewTeam("l_" + score);

        if (!team.hasEntry(entry)) team.addEntry(entry);
        objective.getScore(entry).setScore(score);

        team.prefix(Component.empty());
        team.suffix(Component.empty());

        teams.put(score, team);
    }

    private void setLine(int score, Component content) {
        Team team = teams.get(score);
        if (team == null) return;
        team.prefix(content);
        team.suffix(Component.empty());
    }

    private static int[] readPalette(List<String> hexList, int[] fallback) {
        if (hexList == null || hexList.isEmpty()) return fallback;
        List<Integer> out = new ArrayList<>();
        for (String s : hexList) {
            Integer rgb = parseHexColor(s);
            if (rgb != null) out.add(rgb);
        }
        if (out.isEmpty()) return fallback;
        int[] arr = new int[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
        return arr;
    }

    private static Integer parseHexColor(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Component animatedGradientText(String text, int shift, int[] paletteRgb) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (paletteRgb == null || paletteRgb.length == 0) return Component.text(text);

        int len = text.length();
        Component out = Component.empty();

        for (int i = 0; i < len; i++) {
            double t = ((i * 1.0) / Math.max(1, (len - 1)));
            t = (t + (shift / 90.0)) % 1.0;

            int rgb = interpolatePalette(paletteRgb, t);
            out = out.append(
                    Component.text(String.valueOf(text.charAt(i)))
                            .color(TextColor.color(rgb))
                            .decorate(TextDecoration.BOLD)
            );
        }
        return out;
    }

    private static int interpolatePalette(int[] palette, double t) {
        int n = palette.length;
        if (n == 1) return palette[0];

        double scaled = t * (n - 1);
        int idx = (int) Math.floor(scaled);
        int next = Math.min(idx + 1, n - 1);
        double local = scaled - idx;

        return lerpRgb(palette[idx], palette[next], local);
    }

    private static int lerpRgb(int c1, int c2, double a) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

        int r = (int) Math.round(r1 + (r2 - r1) * a);
        int g = (int) Math.round(g1 + (g2 - g1) * a);
        int b = (int) Math.round(b1 + (b2 - b1) * a);

        return (r << 16) | (g << 8) | b;
    }
}
