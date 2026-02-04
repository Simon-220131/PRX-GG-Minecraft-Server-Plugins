package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {
    private final ReprimandLogger reprimandLogger;

    public KickCommand(ReprimandLogger reprimandLogger) {
        this.reprimandLogger = reprimandLogger;
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

        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Keine Angabe";

        String kickMessage = MessageUtil.color("&cDu wurdest gekickt! &7Grund: &f" + reason);
        target.kick(MessageUtil.component(kickMessage));

        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName()
                + " &7wurde gekickt. &7Grund: &f" + reason));
        reprimandLogger.log("KICK: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason);
        return true;
    }
}
