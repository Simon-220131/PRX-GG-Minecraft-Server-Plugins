package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.PunishmentHistoryRecord;
import at.prx.pRXReprimands.util.CommandUtil;
import at.prx.pRXReprimands.util.MessageUtil;
import at.prx.pRXReprimands.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistoryCommand implements CommandExecutor {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int PAGE_SIZE = 8;

    private final PunishmentManager punishmentManager;

    public HistoryCommand(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.requirePermission(sender, "prxreprimands.history")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/history <Spieler> [Seite]"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (page < 1) {
            page = 1;
        }

        int total = punishmentManager.getHistoryCount(target.getUniqueId());
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Straf-Historie von &f" + target.getName()
                + "&7: &f" + total));
        if (total == 0) {
            return true;
        }

        int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
        if (page > totalPages) {
            page = totalPages;
        }
        int offset = (page - 1) * PAGE_SIZE;

        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Seite &f" + page + "&7/&f" + totalPages));

        List<PunishmentHistoryRecord> history = punishmentManager.listHistory(target.getUniqueId(), offset, PAGE_SIZE);
        for (PunishmentHistoryRecord record : history) {
            String when = FORMATTER.format(Instant.ofEpochMilli(record.startMillis()));
            String endText;
            if (record.isPermanent()) {
                endText = "Permanent";
            } else if (record.isActive()) {
                endText = "Aktiv (" + TimeUtil.formatRemaining(record.endMillis()) + ")";
            } else {
                endText = "Ende " + FORMATTER.format(Instant.ofEpochMilli(record.endMillis()));
            }

            StringBuilder line = new StringBuilder("&8#&7")
                    .append(record.id())
                    .append(" &7[")
                    .append(when)
                    .append("] &f")
                    .append(record.type().name())
                    .append(" &7von &f")
                    .append(record.actor())
                    .append(" &7- &f")
                    .append(record.reason())
                    .append(" &7(")
                    .append(endText)
                    .append(")");

            sender.sendMessage(MessageUtil.color(line.toString()));
        }
        return true;
    }
}
