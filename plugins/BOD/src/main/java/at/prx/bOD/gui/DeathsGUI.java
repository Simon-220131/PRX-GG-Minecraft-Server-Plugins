package at.prx.bOD.gui;

import at.prx.bOD.manager.DeathBanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DeathsGUI {

    private final DeathBanManager manager;
    private final UUID target;
    private final JavaPlugin plugin;

    public DeathsGUI(JavaPlugin plugin, DeathBanManager manager, UUID target) {
        this.manager = manager;
        this.plugin = plugin;
        this.target = target;
    }

    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(null, 27,
                MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ffaa00><bold>✦ Death Editor ✦</bold></gradient>")
        );

        updateInventory(inv);
        return inv;
    }

    public void updateInventory(Inventory inv) {

        // == Glass Border ==
        ItemStack glass = coloredGlass(7); // dark gray
        for (int i = 0; i < 27; i++) {
            if (i <= 8 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }

        int deaths = manager.getDeaths(target);

        // == Player Head ==
        inv.setItem(13, glowing(playerHead(target, "§e" + Bukkit.getOfflinePlayer(target).getName())));

        // == +1 ==
        inv.setItem(10, glowing(createItem(Material.LIME_DYE, "§a+1 Death")));

        // == -1 ==
        inv.setItem(16, glowing(createItem(Material.RED_DYE, "§c-1 Death")));

        // == Reset ==
        inv.setItem(12, createItem(Material.BARRIER, "§4Reset Deaths"));
        inv.setItem(14, createItem(Material.BARRIER, "§4Reset Deaths"));

        // == Death Counter ==
        inv.setItem(22, glowing(createItem(Material.PAPER, "§6Tode: §e" + deaths)));

        // == Close ==
        inv.setItem(26, createItem(Material.ARROW, "§7Close"));
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glowing(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack coloredGlass(int color) {
        return new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    }

    private ItemStack playerHead(UUID uuid, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        meta.setDisplayName(name);
        head.setItemMeta(meta);
        return head;
    }
}

