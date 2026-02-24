package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.WarningRecord;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WarnsCommand implements CommandExecutor {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int PAGE_SIZE = 8;

    private final PunishmentManager punishmentManager;

    public WarnsCommand(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.warns")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/warns <Spieler>"));
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

        List<WarningRecord> warnings = punishmentManager.listWarnings(target.getUniqueId());
        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Warns von &f" + target.getName()
                + "&7: &f" + warnings.size()));

        if (warnings.isEmpty()) {
            return true;
        }

        int totalPages = (int) Math.ceil(warnings.size() / (double) PAGE_SIZE);
        if (page > totalPages) {
            page = totalPages;
        }
        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, warnings.size());

        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Seite &f" + page + "&7/&f" + totalPages));

        for (int i = startIndex; i < endIndex; i++) {
            WarningRecord warning = warnings.get(i);
            String when = FORMATTER.format(Instant.ofEpochMilli(warning.createdMillis()));
            String line = "&8#&7" + warning.id()
                    + " &7[" + when + "] &f" + warning.actor()
                    + " &7- &f" + warning.reason();
            sender.sendMessage(MessageUtil.color(line));
        }

        return true;
    }
}
