package at.prx.pRXReprimands.command.completer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class BanMuteCompleter implements TabCompleter {
    private static final List<String> DURATIONS = List.of("10m", "30m", "1h", "6h", "12h", "1d", "7d", "30d");
    private final Plugin plugin;

    public BanMuteCompleter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return DURATIONS.stream()
                    .filter(value -> value.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length >= 3) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            return getReasonTemplates(prefix);
        }
        return List.of();
    }

    private List<String> getReasonTemplates(String prefix) {
        if (plugin == null) {
            return List.of();
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("reason-templates");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
                .filter(key -> key.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }
}
