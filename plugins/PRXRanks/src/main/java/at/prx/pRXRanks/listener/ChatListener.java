package at.prx.pRXRanks.listener;

import at.prx.pRXRanks.manager.RankManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final RankManager rankService;

    public ChatListener(RankManager rankService) {
        this.rankService = rankService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        // 🔹 Prefix aus LuckPerms
        String prefix = rankService.getPrefix(event.getPlayer());

        // 🔹 Anzeige-Prefix bauen
        String displayPrefix = "";
        if (!prefix.isEmpty()) {
            displayPrefix = prefix + " §8| §f";
        }

        event.setFormat(
                displayPrefix
                        + event.getPlayer().getName()
                        + " §8» §7"
                        + event.getMessage()
        );
    }
}
