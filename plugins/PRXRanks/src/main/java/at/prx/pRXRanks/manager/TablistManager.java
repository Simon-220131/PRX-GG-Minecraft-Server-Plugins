package at.prx.pRXRanks.manager;

import at.prx.pRXRanks.model.Ranks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TablistManager {

    private final RankManager rankManager;

    public TablistManager(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    public void update(Player player) {
        Ranks rank = rankManager.getRank(player);

        player.setPlayerListName(
                rank.getSortPrefix()
                        + rank.getGlyph()
                        + " §f"
                        + player.getName()
        );
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }
}
