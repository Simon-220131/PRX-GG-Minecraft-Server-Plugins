package at.prx.pRXRanks;

import at.prx.pRXRanks.listener.ChatListener;
import at.prx.pRXRanks.listener.JoinQuitListener;
import at.prx.pRXRanks.listener.LuckPermsListener;
import at.prx.pRXRanks.manager.NametagManager;
import at.prx.pRXRanks.manager.RankManager;
import at.prx.pRXRanks.manager.TablistManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PRXRanks extends JavaPlugin {

    @Override
    public void onEnable() {

        saveDefaultConfig();

        RankManager rankService = new RankManager();
        TablistManager tablistManager = new TablistManager(rankService);
        NametagManager nametagManager = new NametagManager(rankService);

        // 🔥 LuckPerms Live Updates
        new LuckPermsListener(this, tablistManager, nametagManager);

        // Join initial
        getServer().getPluginManager().registerEvents(
                new JoinQuitListener(rankService, tablistManager, nametagManager),
                this
        );

        // Chat
        getServer().getPluginManager().registerEvents(
                new ChatListener(rankService),
                this
        );
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
