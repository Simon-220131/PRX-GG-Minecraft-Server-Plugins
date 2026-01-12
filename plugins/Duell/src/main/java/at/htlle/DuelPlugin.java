package at.htlle;

import at.htlle.command.DuelCommand;
import at.htlle.duel.DuelManager;
import at.htlle.duel.SpawnManager;
import at.htlle.listener.DuelListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DuelPlugin extends JavaPlugin {

    private DuelManager duelManager;
    private SpawnManager spawnManager;

    @Override
    public void onEnable() {
        // 1) config.yml in plugins/<deinPlugin>/ anlegen, falls nicht vorhanden
        saveDefaultConfig();

        // 2) SpawnManager mit config verbinden
        this.spawnManager = new SpawnManager(getConfig());

        // Auto-cleanup (Welt fehlt -> spawn raus)
        int removed = this.spawnManager.cleanupMissingWorlds();
        if (removed > 0) {
            getLogger().warning("Removed " + removed + " spawns because their worlds are missing/not loaded.");
            saveConfig();
        }

        // 3) DuelManager bekommt SpawnManager rein
        this.duelManager = new DuelManager(this, spawnManager);

        var cmd = getCommand("duel");
        if (cmd != null) {
            DuelCommand executor = new DuelCommand(duelManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new DuelListener(duelManager), this);
        getServer().getPluginManager().registerEvents(new at.htlle.listener.StatsGuiListener(duelManager), this);

        getLogger().info("Duell enabled.");
    }

    @Override
    public void onDisable() {
        // Wenn du Spawns per Command speicherst -> config schreiben
        saveConfig();
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }
}