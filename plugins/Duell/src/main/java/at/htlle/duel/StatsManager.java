package at.htlle.duel;

import at.htlle.DuelPlugin;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class StatsManager {
    private final DuelPlugin plugin;
    private final Map<UUID, Stats> stats = new HashMap<>();

    public StatsManager(DuelPlugin plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    private void loadFromConfig() {
        var cfg = plugin.getConfig();
        if (!cfg.isConfigurationSection("stats")) return;
        for (String key : cfg.getConfigurationSection("stats").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int wins = cfg.getInt("stats." + key + ".wins", 0);
                int losses = cfg.getInt("stats." + key + ".losses", 0);
                int participated = cfg.getInt("stats." + key + ".participated", 0);
                stats.put(id, new Stats(wins, losses, participated));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveToConfig(UUID id) {
        var cfg = plugin.getConfig();
        String path = "stats." + id.toString() + ".";
        Stats s = stats.getOrDefault(id, new Stats(0,0,0));
        cfg.set(path + "wins", s.wins);
        cfg.set(path + "losses", s.losses);
        cfg.set(path + "participated", s.participated);
        plugin.saveConfig();
    }

    public void recordParticipation(UUID id) {
        Stats s = stats.computeIfAbsent(id, k -> new Stats(0,0,0));
        s.participated++;
        saveToConfig(id);
    }

    public void recordWin(UUID id) {
        Stats s = stats.computeIfAbsent(id, k -> new Stats(0,0,0));
        s.wins++;
        saveToConfig(id);
    }

    public void recordLoss(UUID id) {
        Stats s = stats.computeIfAbsent(id, k -> new Stats(0,0,0));
        s.losses++;
        saveToConfig(id);
    }

    public Optional<Stats> getStats(UUID id) {
        return Optional.ofNullable(stats.get(id));
    }

    public List<UUID> playersWithParticipation() {
        List<UUID> out = new ArrayList<>();
        for (var e : stats.entrySet()) {
            if (e.getValue().participated > 0) out.add(e.getKey());
        }
        return out;
    }

    public static class Stats {
        public int wins;
        public int losses;
        public int participated;

        public Stats(int wins, int losses, int participated) {
            this.wins = wins;
            this.losses = losses;
            this.participated = participated;
        }
    }
}
