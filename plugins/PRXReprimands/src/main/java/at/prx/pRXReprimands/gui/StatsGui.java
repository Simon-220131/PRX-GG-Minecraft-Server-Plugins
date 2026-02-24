package at.prx.pRXReprimands.gui;

import at.prx.pRXReprimands.model.PunishmentType;
import at.prx.pRXReprimands.model.ReasonCount;
import at.prx.pRXReprimands.model.StatsSnapshot;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatsGui {
    public static final String TITLE = MessageUtil.color("&8PRX &cStats");
    private static final int SIZE = 27;

    private StatsGui() {
    }

    public static void open(Player player, StatsSnapshot snapshot) {
        player.openInventory(build(snapshot));
    }

    public static Inventory build(StatsSnapshot snapshot) {
        Inventory inventory = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        Map<PunishmentType, Integer> totals = snapshot.punishmentTotals();
        Map<PunishmentType, Integer> active = snapshot.activePunishments();

        List<String> totalLore = new ArrayList<>();
        totalLore.add("&7Gesamt: &f" + snapshot.totalPunishments());
        totalLore.add("&7Bans: &f" + totals.getOrDefault(PunishmentType.BAN, 0));
        totalLore.add("&7Mutes: &f" + totals.getOrDefault(PunishmentType.MUTE, 0));
        totalLore.add("&7Warns: &f" + snapshot.totalWarnings());

        List<String> activeLore = new ArrayList<>();
        activeLore.add("&7Aktive Bans: &f" + active.getOrDefault(PunishmentType.BAN, 0));
        activeLore.add("&7Aktive Mutes: &f" + active.getOrDefault(PunishmentType.MUTE, 0));

        List<String> topPunishmentLore = formatReasons(snapshot.topPunishmentReasons());
        List<String> topWarningLore = formatReasons(snapshot.topWarningReasons());

        inventory.setItem(10, buildItem(Material.BOOK, "&cGesamtstrafen", totalLore));
        inventory.setItem(12, buildItem(Material.IRON_BARS, "&cAktive Strafen", activeLore));
        inventory.setItem(14, buildItem(Material.PAPER, "&cTop Gruende (Strafen)", topPunishmentLore));
        inventory.setItem(16, buildItem(Material.MAP, "&cTop Gruende (Warns)", topWarningLore));

        return inventory;
    }

    private static List<String> formatReasons(List<ReasonCount> reasons) {
        List<String> lines = new ArrayList<>();
        if (reasons == null || reasons.isEmpty()) {
            lines.add("&7Keine Daten.");
            return lines;
        }
        int index = 1;
        for (ReasonCount reason : reasons) {
            String label = trimReason(reason.reason(), 32);
            lines.add("&7#" + index + " &f" + label + " &8- &c" + reason.count());
            index++;
        }
        return lines;
    }

    private static String trimReason(String reason, int maxLength) {
        if (reason == null) {
            return "Unbekannt";
        }
        String trimmed = reason.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        if (lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(MessageUtil.color(line));
            }
            meta.setLore(colored);
        }
        item.setItemMeta(meta);
        return item;
    }
}
