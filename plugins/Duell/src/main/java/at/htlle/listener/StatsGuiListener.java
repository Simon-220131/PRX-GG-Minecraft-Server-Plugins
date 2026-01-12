package at.htlle.listener;

import at.htlle.duel.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

public class StatsGuiListener implements Listener {
    private final DuelManager manager;

    public StatsGuiListener(DuelManager manager) { this.manager = manager; }

    private boolean isStatsInventoryTitle(String title) {
        return title != null && title.equals("Duell Statistiken");
    }

    private boolean isLeaderboardInventoryTitle(String title) {
        return title != null && title.equals("Duell Leaderboard");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = e.getView().getTitle();
        boolean stats = isStatsInventoryTitle(title);
        boolean lb = isLeaderboardInventoryTitle(title);
        if (!stats && !lb) return;

        Inventory top = e.getView().getTopInventory();
        int topSize = top.getSize();

        // ✅ nur clicks im GUI blockieren
        if (e.getRawSlot() >= topSize) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();

        if (stats) {
            if (slot == 18) { // Abbruch
                p.closeInventory();
                return;
            }
            if (slot == 26) { // Leaderboard
                // NICHT close+open direkt, besser 1 Tick später:
                Bukkit.getScheduler().runTask(manager.getPlugin(), () -> manager.openLeaderboard(p));
                return;
            }

            // ❌ das "swap mit slot 17" ist aktuell unnötig + wirkt buggy
            // Wenn du es behalten willst: dann NUR im TopInventory arbeiten (top!)
            return;
        }

        if (lb) {
            if (slot == 45) { // Abbruch
                p.closeInventory();
                return;
            }
            if (slot == 53) { // Zurück
                Bukkit.getScheduler().runTask(manager.getPlugin(), () -> manager.openPersonalStats(p));
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (!isStatsInventoryTitle(title) && !isLeaderboardInventoryTitle(title)) return;

        int topSize = e.getView().getTopInventory().getSize();
        boolean dragsIntoTop = e.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (dragsIntoTop) e.setCancelled(true);
    }

    private ItemStack createPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        return pane;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // nothing for now
    }
}
