package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.util.BroadcastUtil;
import at.prx.pRXReprimands.util.CommandUtil;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Map;

public class KickCommand implements CommandExecutor {
    private final ReprimandLogger reprimandLogger;
    private final Plugin plugin;

    public KickCommand(ReprimandLogger reprimandLogger, Plugin plugin) {
        this.reprimandLogger = reprimandLogger;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.kick")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/kick <Spieler> [Grund]"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler ist nicht online."));
            return true;
        }
        if (target.hasPermission("prxreprimands.bypass.kick")) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cDieser Spieler ist vom Kick ausgenommen."));
            return true;
        }

        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Keine Angabe";
        reason = CommandUtil.resolveReasonTemplate(plugin, reason);

        String kickMessage = MessageUtil.color("&cDu wurdest gekickt! &7Grund: &f" + reason);
        target.kick(MessageUtil.component(kickMessage));

        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName()
                + " &7wurde gekickt. &7Grund: &f" + reason));
        BroadcastUtil.send(plugin, "broadcasts.kick", Map.of(
                "actor", sender.getName(),
                "target", target.getName(),
                "reason", reason
        ));
        reprimandLogger.log("KICK: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason);
        return true;
    }
}
