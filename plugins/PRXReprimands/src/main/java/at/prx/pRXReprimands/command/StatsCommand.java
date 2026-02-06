package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.gui.StatsGui;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.model.StatsSnapshot;
import at.prx.pRXReprimands.util.CommandUtil;
import at.prx.pRXReprimands.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StatsCommand implements CommandExecutor {
    private static final int TOP_LIMIT = 5;

    private final PunishmentManager punishmentManager;
    private final Plugin plugin;

    public StatsCommand(PunishmentManager punishmentManager, Plugin plugin) {
        this.punishmentManager = punishmentManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.color(MessageUtil.PREFIX + "&cNur Spieler."));
            return true;
        }
        if (!CommandUtil.requirePermission(sender, "prxreprimands.stats")) {
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StatsSnapshot snapshot = punishmentManager.getStatsSnapshot(TOP_LIMIT);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    StatsGui.open(player, snapshot);
                }
            });
        });
        return true;
    }
}
