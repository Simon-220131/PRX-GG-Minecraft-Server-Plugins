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

public class MuteCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;
    private final Plugin plugin;

    public MuteCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger, Plugin plugin) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.requirePermission(sender, "prxreprimands.mute")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/mute <Spieler> [Dauer] <Grund>"));
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
        if (CommandUtil.isBypassed(target, "prxreprimands.bypass.mute")) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cDieser Spieler ist vom Mute ausgenommen."));
            return true;
        }

        CommandUtil.ParsedInput parsed = CommandUtil.parseDurationAndReason(args, 1);
        long duration = parsed.durationMillis();
        String reason = CommandUtil.resolveReasonTemplate(plugin, parsed.reason());
        if (duration > 0 && args.length == 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cBitte gib einen Grund an."));
            return true;
        }

        punishmentManager.mute(target, sender.getName(), reason, duration);
        punishmentManager.save();

        String durationText = duration > 0 ? TimeUtil.formatRemaining(System.currentTimeMillis() + duration) : "Permanent";
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName()
                + " &7wurde gemutet. &7Dauer: &f" + durationText + " &7Grund: &f" + reason));
        BroadcastUtil.send(plugin, "broadcasts.mute", Map.of(
                "actor", sender.getName(),
                "target", target.getName(),
                "duration", durationText,
                "reason", reason
        ));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            String muteMessage = MessageUtil.color("&cDu wurdest gemutet! &7Grund: &f" + reason);
            if (duration > 0) {
                muteMessage += MessageUtil.color(" &7Dauer: &f" + durationText);
            }
            online.sendMessage(muteMessage);
        }

        PunishmentRecord record = punishmentManager.getMute(target.getUniqueId());
        reprimandLogger.log("MUTE: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason
                + " duration=" + (record != null && !record.isPermanent() ? durationText : "permanent"));
        return true;
    }
}
