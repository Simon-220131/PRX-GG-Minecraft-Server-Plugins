package at.htlle.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public static boolean hasEnough(Player p, Material mat, int amount) {
        int count = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            if (it.getType() != mat) continue;
            count += it.getAmount();
            if (count >= amount) return true;
        }
        return false;
    }

    // Removes exact amount of a Material and returns removed ItemStacks (for escrow)
    public static List<ItemStack> removeMaterial(Player p, Material mat, int amount) {
        List<ItemStack> removed = new ArrayList<>();
        int remaining = amount;

        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;

            int take = Math.min(remaining, it.getAmount());
            ItemStack takenStack = it.clone();
            takenStack.setAmount(take);
            removed.add(takenStack);

            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) contents[i] = null;

            remaining -= take;
            if (remaining <= 0) break;
        }

        p.getInventory().setContents(contents);
        p.updateInventory();
        return removed;
    }

    // ------------------------------
    // Bannshield (CUSTOM) PLACEHOLDER
    // ------------------------------
    // Beispiel: Bannshield = NETHER_STAR mit DisplayName "Bannshield"
    private static boolean isBannShield(ItemStack it) {
        if (it == null) return false;
        if (it.getType() != Material.NETHER_STAR) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().toLowerCase().contains("bannshield");
    }

    public static boolean hasBannShield(Player p, int amount) {
        int count = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (!isBannShield(it)) continue;
            count += it.getAmount();
            if (count >= amount) return true;
        }
        return false;
    }

    public static void removeBannShield(Player p, int amount) {
        int remaining = amount;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!isBannShield(it)) continue;

            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) contents[i] = null;
            remaining -= take;
            if (remaining <= 0) break;
        }
        p.getInventory().setContents(contents);
        p.updateInventory();
    }

    public static void giveBannShield(Player p, int amount) {
        ItemStack shield = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = shield.getItemMeta();
        meta.setDisplayName("Bannshield");
        shield.setItemMeta(meta);
        p.getInventory().addItem(shield);
    }
}