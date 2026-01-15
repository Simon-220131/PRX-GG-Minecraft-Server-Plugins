package at.prx.pRXRanks;

import at.prx.pRXRanks.commands.RankCommand;
import at.prx.pRXRanks.listener.ChatListener;
import at.prx.pRXRanks.manager.RankManager;
import at.prx.pRXRanks.manager.TablistManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PRXRanks extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        saveDefaultConfig();

        RankManager rankManager = new RankManager(this);
        TablistManager tablistManager = new TablistManager(rankManager);

        getCommand("rank").setExecutor(new RankCommand(rankManager,  tablistManager));
        getCommand("rank").setTabCompleter(new RankCommand(rankManager,  tablistManager));

        getServer().getPluginManager().registerEvents(
                new ChatListener(rankManager), this
        );

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
