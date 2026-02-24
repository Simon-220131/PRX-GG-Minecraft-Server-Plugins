package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class NoteCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;

    public NoteCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.note")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/note <Spieler> <Notiz>"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        long id = punishmentManager.addNote(target, sender.getName(), note);
        if (id > 0) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&aNotiz erstellt: &f#" + id
                    + " &7Spieler: &f" + target.getName()));
            reprimandLogger.log("NOTE: " + sender.getName() + " -> " + target.getName()
                    + " (" + target.getUniqueId() + ") id=" + id + " note=" + note);
        } else {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cKonnte Notiz nicht speichern."));
        }
        return true;
    }
}
