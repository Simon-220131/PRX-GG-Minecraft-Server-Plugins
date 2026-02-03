package at.prx.pRXReprimands.listener;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.util.MessageUtil;
import at.prx.pRXReprimands.util.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class JoinListener implements Listener {
    private final PunishmentManager punishmentManager;

    public JoinListener(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        PunishmentRecord ban = punishmentManager.getBan(event.getPlayer().getUniqueId());
        if (ban == null) {
            return;
        }
        String durationText = ban.isPermanent()
                ? "Permanent"
                : TimeUtil.formatRemaining(ban.endMillis());
        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.banScreen(ban.reason(), durationText));
    }
}
