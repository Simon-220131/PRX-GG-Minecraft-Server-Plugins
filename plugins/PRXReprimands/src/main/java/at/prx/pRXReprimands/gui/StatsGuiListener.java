package at.prx.pRXReprimands.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class StatsGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!StatsGui.TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!StatsGui.TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }
}
