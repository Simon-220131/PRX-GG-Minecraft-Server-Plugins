package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.WarningRecord;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveWarnCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;

    public RemoveWarnCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.removewarn")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/removewarn <Spieler> <ID>"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        String raw = args[1].trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        raw = raw.replaceAll("[^0-9]", "");
        long id;
        try {
            id = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX
                    + "&cUngültige ID. &7Nutze /warns <Spieler> und nimm die #ID."));
            return true;
        }

        WarningRecord record = punishmentManager.getWarningById(id);
        if (record == null || !record.target().equals(target.getUniqueId())) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cWarnung nicht gefunden."));
            return true;
        }

        boolean removed = punishmentManager.deleteWarning(id);
        if (removed) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&aWarnung gelöscht: &f#" + id
                    + " &7Spieler: &f" + target.getName()));
            reprimandLogger.log("REMOVEWARN: " + sender.getName() + " -> " + target.getName()
                    + " (" + record.target() + ") id=" + id);
        } else {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cKonnte Warnung nicht löschen."));
        }
        return true;
    }
}
