package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.util.BroadcastUtil;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class UnmuteCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;
    private final Plugin plugin;

    public UnmuteCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger, Plugin plugin) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.unmute")) {
            return true;
        }
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
            BroadcastUtil.send(plugin, "broadcasts.unmute", Map.of(
                    "actor", sender.getName(),
                    "target", target.getName()
            ));
            reprimandLogger.log("UNMUTE: " + sender.getName() + " -> " + target.getName()
                    + " (" + target.getUniqueId() + ")");
        } else {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&e" + target.getName() + " &7ist nicht gemutet."));
        }
        return true;
    }
}
