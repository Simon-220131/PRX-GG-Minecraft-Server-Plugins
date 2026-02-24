package at.prx.ggnetwork.lobbyboard;

import at.prx.ggnetwork.lobbyboard.board.LobbyBoardManager;
import at.prx.ggnetwork.lobbyboard.listener.JoinQuitListener;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class GGNetworkLobbyBoardPlugin extends JavaPlugin {

    private LobbyBoardManager boardManager;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Hook LuckPerms
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (rsp == null) {
            getLogger().severe("LuckPerms not found (service provider missing). Plugin will disable.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.luckPerms = rsp.getProvider();

        this.boardManager = new LobbyBoardManager(this, luckPerms);
        this.boardManager.startTasks();

        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(boardManager), this);

        // Give boards to already online players (e.g., /reload)
        Bukkit.getOnlinePlayers().forEach(boardManager::give);

        getLogger().info("GGNetworkLobbyBoard enabled.");
    }

    @Override
    public void onDisable() {
        if (boardManager != null) {
            boardManager.stopTasks();
            boardManager.removeAll();
        }
        getLogger().info("GGNetworkLobbyBoard disabled.");
    }
}
