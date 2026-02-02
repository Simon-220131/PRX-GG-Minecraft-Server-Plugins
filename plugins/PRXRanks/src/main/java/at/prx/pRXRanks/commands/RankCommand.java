package at.prx.pRXRanks.commands;

import at.prx.pRXRanks.manager.RankManager;
import at.prx.pRXRanks.manager.TablistManager;
import at.prx.pRXRanks.model.Ranks;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankManager rankManager;
    private final TablistManager tablistManager;

    public RankCommand(RankManager rankManager, TablistManager tablistManager) {
        this.rankManager = rankManager;
        this.tablistManager = tablistManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("prxranks.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length != 3 || !args[0].equalsIgnoreCase("set")) {
            sender.sendMessage("§cBenutzung: /rank set <player> <rank>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }

        Ranks rank;
        try {
            rank = Ranks.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUnbekannter Rang.");
            return true;
        }

        tablistManager.update(target);
        sender.sendMessage("§aRang von §f" + target.getName() + " §aauf §f" + rank.name() + " §agesetzt.");
        target.sendMessage("§7Dein Rang wurde auf §f" + rank.name() + " §7gesetzt.");

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!sender.hasPermission("prxranks.admin")) {
            return List.of();
        }

        // /rank <set>
        if (args.length == 1) {
            return List.of("set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /rank set <player>
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // /rank set <player> <rank>
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> ranks = new ArrayList<>();
            for (Ranks r : Ranks.values()) {
                ranks.add(r.name().toLowerCase());
            }

            return ranks.stream()
                    .filter(r -> r.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
