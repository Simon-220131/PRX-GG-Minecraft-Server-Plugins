package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.model.PunishmentType;
import at.prx.pRXReprimands.util.MessageUtil;
import at.prx.pRXReprimands.util.TimeUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WarnCommand implements CommandExecutor {
    private final PunishmentManager punishmentManager;
    private final ReprimandLogger reprimandLogger;
    private final Plugin plugin;

    public WarnCommand(PunishmentManager punishmentManager, ReprimandLogger reprimandLogger, Plugin plugin) {
        this.punishmentManager = punishmentManager;
        this.reprimandLogger = reprimandLogger;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!at.prx.pRXReprimands.util.CommandUtil.requirePermission(sender, "prxreprimands.warn")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&7Nutze: &f/warn <Spieler> <Grund>"));
            return true;
        }

        OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cSpieler nicht gefunden oder noch nie online."));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int warns = punishmentManager.addWarning(target, sender.getName(), reason);

        sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&a" + target.getName()
                + " &7wurde verwarnt. &7Warns: &f" + warns + " &7Grund: &f" + reason));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            String titleText = MessageUtil.color("&c&lVERWARNUNG");
            String subtitleText = MessageUtil.color("&7Grund: &f" + reason);
            Title title = Title.title(
                    MessageUtil.component(titleText),
                    MessageUtil.component(subtitleText),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
            );
            online.showTitle(title);

            online.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cDu hast eine Verwarnung erhalten."
                    + " &7Warns: &f" + warns));
        }

        applyAutoEscalation(target, warns, sender.getName());

        reprimandLogger.log("WARN: " + sender.getName() + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason + " warns=" + warns);
        return true;
    }

    private void applyAutoEscalation(OfflinePlayer target, int warns, String actor) {
        if (!plugin.getConfig().getBoolean("warnings.auto-escalation.enabled", false)) {
            return;
        }
        List<Map<?, ?>> rules = plugin.getConfig().getMapList("warnings.auto-escalation.rules");
        if (rules.isEmpty()) {
            return;
        }
        for (Map<?, ?> rule : rules) {
            Object countObj = rule.get("count");
            if (!(countObj instanceof Number)) {
                continue;
            }
            int count = ((Number) countObj).intValue();
            if (count != warns) {
                continue;
            }
            Object actionObj = rule.get("action");
            Object durationObj = rule.get("duration");
            Object reasonObj = rule.get("reason");
            String action = (actionObj == null ? "MUTE" : actionObj.toString()).toUpperCase(Locale.ROOT);
            String durationRaw = durationObj == null ? "1d" : durationObj.toString();
            String reason = reasonObj == null ? "Auto-Eskalation" : reasonObj.toString();
            long durationMillis = TimeUtil.parseDurationMillis(durationRaw);
            if (durationMillis < 0) {
                plugin.getLogger().warning("Invalid escalation duration: " + durationRaw);
                return;
            }

            if ("MUTE".equals(action)) {
                punishmentManager.mute(target, actor, reason, durationMillis);
                notifyEscalation(target, PunishmentType.MUTE, reason, durationMillis, actor);
            } else if ("BAN".equals(action)) {
                punishmentManager.ban(target, actor, reason, durationMillis);
                notifyEscalation(target, PunishmentType.BAN, reason, durationMillis, actor);
            }
            return;
        }
    }

    private void notifyEscalation(OfflinePlayer target,
                                  PunishmentType type,
                                  String reason,
                                  long durationMillis,
                                  String actor) {
        String durationText = durationMillis > 0
                ? TimeUtil.formatRemaining(System.currentTimeMillis() + durationMillis)
                : "Permanent";
        if (type == PunishmentType.BAN) {
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                online.kick(MessageUtil.banScreen(reason, durationText));
            }
        } else if (type == PunishmentType.MUTE) {
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                String muteMessage = MessageUtil.color("&cDu wurdest gemutet! &7Grund: &f" + reason);
                if (durationMillis > 0) {
                    muteMessage += MessageUtil.color(" &7Dauer: &f" + durationText);
                }
                online.sendMessage(muteMessage);
            }
        }
        PunishmentRecord record = type == PunishmentType.BAN
                ? punishmentManager.getBan(target.getUniqueId())
                : punishmentManager.getMute(target.getUniqueId());
        reprimandLogger.log("AUTO-" + type.name() + ": " + actor + " -> " + target.getName()
                + " (" + target.getUniqueId() + ") reason=" + reason
                + " duration=" + (record != null && !record.isPermanent() ? durationText : "permanent"));
    }
}
