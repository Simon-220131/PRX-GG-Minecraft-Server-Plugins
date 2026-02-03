package at.prx.pRXReprimands.listener;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.util.MessageUtil;
import at.prx.pRXReprimands.util.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final PunishmentManager punishmentManager;

    public ChatListener(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        PunishmentRecord mute = punishmentManager.getMute(event.getPlayer().getUniqueId());
        if (mute == null) {
            return;
        }
        String message = MessageUtil.color("&cDu bist gemutet! &7Grund: &f" + mute.reason());
        if (!mute.isPermanent()) {
            message += MessageUtil.color(" &7Restzeit: &f" + TimeUtil.formatRemaining(mute.endMillis()));
        }
        event.getPlayer().sendMessage(message);
        event.setCancelled(true);
    }
}
