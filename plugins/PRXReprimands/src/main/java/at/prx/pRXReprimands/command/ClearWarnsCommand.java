package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClearWarnsCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;

    public ClearWarnsCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.clearwarns")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/clearwarns <Spieler>"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        int removed = punishmentManager.clearWarnings(target.getUniqueId());
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&aWarns von &f" + target.getName()
                + " &7wurden gelöscht. &7Anzahl: &f" + removed));
        reprimandLogger.log("CLEARWARNS: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") removed=" + removed);
        return true;
    }
}
