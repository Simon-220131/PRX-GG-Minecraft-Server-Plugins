package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.util.CommandUtil;
import at.prx.pRXReprimands.util.BroadcastUtil;
import at.prx.pRXReprimands.util.MessageUtil;
import at.prx.pRXReprimands.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class BanCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;
    private final Plugin plugin;

    public BanCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger, Plugin plugin) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.requirePermission(sender, "prxreprimands.ban")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/ban <Spieler> [Dauer] <Grund>"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cBitte gib einen Grund an."));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }
        if (CommandUtil.isBypassed(target, "prxreprimands.bypass.ban")) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cDieser Spieler ist vom Ban ausgenommen."));
            return true;
        }

        CommandUtil.ParsedInput parsed = CommandUtil.parseDurationAndReason(args, 1);
        long duration = parsed.durationMillis();
        String reason = CommandUtil.resolveReasonTemplate(plugin, parsed.reason());
        if (duration > 0 && args.length == 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cBitte gib einen Grund an."));
            return true;
        }

        punishmentManager.ban(target, sender.getName(), reason, duration);
        punishmentManager.save();

        String durationText = duration > 0 ? TimeUtil.formatRemaining(System.currentTimeMillis() + duration) : "Permanent";
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName()
                + " &7wurde gebannt. &7Dauer: &f" + durationText + " &7Grund: &f" + reason));
        BroadcastUtil.send(plugin, "broadcasts.ban", Map.of(
                "actor", sender.getName(),
                "target", target.getName(),
                "duration", durationText,
                "reason", reason
        ));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            online.kick(MessageUtil.banScreen(reason, durationText));
        }

        PunishmentRecord record = punishmentManager.getBan(target.getUniqueId());
        reprimandLogger.log("BAN: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason
                + " duration=" + (record != null && !record.isPermanent() ? durationText : "permanent"));
        return true;
    }
}
