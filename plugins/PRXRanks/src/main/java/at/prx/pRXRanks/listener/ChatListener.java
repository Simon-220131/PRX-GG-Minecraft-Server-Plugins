package at.prx.pRXRanks.listener;

import at.prx.pRXRanks.manager.RankManager;
import at.prx.pRXRanks.model.Ranks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final RankManager rankManager;

    public ChatListener(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Ranks rank = rankManager.getRank(event.getPlayer());

        String prefix = rank.getGlyph() + " §7";

        event.setFormat(
                prefix + "§f" + event.getPlayer().getName() + " §8» §7" + event.getMessage()
        );
    }
}
