package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnmuteCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;

    public UnmuteCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/unmute <Spieler>"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        if (punishmentManager.unmute(target.getUniqueId())) {
            punishmentManager.save();
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName() + " &7ist entmutet."));
            reprimandLogger.log("UNMUTE: " + sender.getName() + " -> " + target.getName()
                    + " (" + target.getUniqueId() + ")");
        } else {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&e" + target.getName() + " &7ist nicht gemutet."));
        }
        return true;
    }
}
