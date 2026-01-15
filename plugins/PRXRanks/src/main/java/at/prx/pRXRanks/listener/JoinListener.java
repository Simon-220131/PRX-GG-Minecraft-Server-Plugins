package at.prx.pRXRanks.listener;

import at.prx.pRXRanks.manager.TablistManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private TablistManager tablistManager;
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tablistManager.update(event.getPlayer());
    }

}
