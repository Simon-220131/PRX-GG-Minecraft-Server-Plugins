package at.prx.pRXRanks.listener;

import at.prx.pRXRanks.manager.NametagManager;
import at.prx.pRXRanks.manager.RankManager;
import at.prx.pRXRanks.manager.TablistManager;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final RankManager rankService;
    private final TablistManager tablistManager;
    private final NametagManager nametagManager;

    public JoinQuitListener(
            RankManager rankService,
            TablistManager tablistManager,
            NametagManager nametagManager
    ) {
        this.rankService = rankService;
        this.tablistManager = tablistManager;
        this.nametagManager = nametagManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        String prefix = rankService.getPrefix(event.getPlayer());

        String displayPrefix = "";
        if (!prefix.isEmpty()) {
            displayPrefix = prefix + " §8| §f";
        }

        event.joinMessage(Component.text("§8[§a+§8] §r" + displayPrefix + event.getPlayer().getName()));

        // Initiale Anzeige setzen
        tablistManager.update(event.getPlayer());
        nametagManager.update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        String prefix = rankService.getPrefix(event.getPlayer());

        String displayPrefix = "";
        if (!prefix.isEmpty()) {
            displayPrefix = prefix + " §8| §f";
        }

        event.quitMessage(Component.text("§8[§c-§8] " + displayPrefix + event.getPlayer().getName()));
    }
}
