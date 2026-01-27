package at.prx.bOD;

import at.prx.bOD.commands.DeathsCommands;
import at.prx.bOD.commands.UnbanCommand;
import at.prx.bOD.listener.*;
import at.prx.bOD.manager.DeathBanManager;
import at.prx.bOD.manager.DeathScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BOD extends JavaPlugin {

    private DeathBanManager manager;
    private DeathScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        manager = new DeathBanManager(this);

        // Scoreboard Manager starten
        scoreboardManager = new DeathScoreboardManager(this, manager);
        scoreboardManager.start();

        manager.loadShields();

        // AUTO-SAVE TASK
        getServer().getScheduler().runTaskTimer(
                this,
                () -> manager.saveShields(),
                20L * 300, // Start nach 5 Minuten
                20L * 300  // Alle 5 Minuten
        );

        getLogger().info("[BOD] Enabled");

        // Listeners
        getServer().getPluginManager().registerEvents(new DeathBanListener(manager), this);
        getServer().getPluginManager().registerEvents(new JoinBanListener(manager), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this, manager), this);
        getServer().getPluginManager().registerEvents(new GambleActionListener(manager, scoreboardManager, this), this);

        // Commands
        getCommand("unban").setExecutor(new UnbanCommand(manager));
        getCommand("deaths").setExecutor(new DeathsCommands(manager, this));

        // TabCompleter
        getCommand("deaths").setTabCompleter(new DeathsCommands(manager, this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("[BOD] Disabled");

        manager.saveShields();

        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
    }
}
