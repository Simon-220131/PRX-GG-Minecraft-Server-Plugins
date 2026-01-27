package at.prx.bOD.listener;

import at.prx.bOD.gui.DeathsGUI;
import at.prx.bOD.manager.DeathBanManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final JavaPlugin plugin;
    private final DeathBanManager manager;

    public InventoryClickListener(JavaPlugin plugin, DeathBanManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player p)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!title.contains("Death Editor")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        UUID target = (UUID) p.getMetadata("deathTarget").get(0).value();
        int deaths = manager.getDeaths(target);
        String name = clicked.getItemMeta().getDisplayName();

        switch (name) {

            case "§a+1 Death" -> {
                manager.setDeaths(target, deaths + 1);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }

            case "§c-1 Death" -> {
                manager.setDeaths(target, Math.max(0, deaths - 1));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.8f);
            }

            case "§4Reset Deaths" -> {
                manager.setDeaths(target, 0);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1, 1);
            }

            case "§7Close" -> {
                p.closeInventory();
                return;
            }
        }

        // Update GUI
        Bukkit.getScheduler().runTask(plugin, () -> {
            new DeathsGUI(plugin, manager, target).updateInventory(event.getInventory());
        });
    }
}


