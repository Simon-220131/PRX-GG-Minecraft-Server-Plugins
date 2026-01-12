package at.htlle.listener;

import at.htlle.duel.DuelManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class SpectatorBuildListener implements Listener {
    private final DuelManager manager;

    public SpectatorBuildListener(DuelManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (manager.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (manager.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
