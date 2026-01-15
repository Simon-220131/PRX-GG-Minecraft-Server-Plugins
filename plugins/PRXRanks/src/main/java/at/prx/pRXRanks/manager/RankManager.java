package at.prx.pRXRanks.manager;

import at.prx.pRXRanks.model.Ranks;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class RankManager {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "players.yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Setzt den Rang eines Spielers und speichert ihn
     */
    public void setRank(Player player, Ranks rank) {
        if (player == null || rank == null) return;

        String path = player.getUniqueId().toString() + ".rank";
        config.set(path, rank.name());
        save();
    }

    /**
     * Gibt den Rang eines Spielers zurück
     */
    public Ranks getRank(Player player) {
        if (player == null) return Ranks.SPIELER;

        String path = player.getUniqueId().toString() + ".rank";
        String rankName = config.getString(path);

        if (rankName == null) {
            return Ranks.SPIELER;
        }

        try {
            return Ranks.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            return Ranks.SPIELER;
        }
    }

    /**
     * Speichert players.yml
     */
    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
