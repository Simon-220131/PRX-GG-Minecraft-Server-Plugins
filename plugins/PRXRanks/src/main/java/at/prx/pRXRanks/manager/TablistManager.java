package at.prx.pRXRanks.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TablistManager {

    private final RankManager rankService;

    public TablistManager(RankManager rankService) {
        this.rankService = rankService;
    }

    public void update(Player player) {
        if (player == null) return;

        // 🔹 Daten aus LuckPerms
        String prefix = rankService.getPrefix(player);
        int weight = rankService.getWeight(player);

        // 🔹 Sortierung (LP-Weight)
        player.setPlayerListOrder(weight);

        // 🔹 Anzeige bauen
        String displayPrefix = "";
        if (!prefix.isEmpty()) {
            displayPrefix = prefix + " §8| §f";
        }

        player.setPlayerListName(displayPrefix + player.getName());
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }
}
