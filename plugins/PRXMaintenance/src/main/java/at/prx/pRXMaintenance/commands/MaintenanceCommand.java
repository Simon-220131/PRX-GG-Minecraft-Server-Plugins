package at.prx.pRXMaintenance.commands;

import at.prx.pRXMaintenance.PRXMaintenance;
import at.prx.pRXMaintenance.whitelist.MaintenanceWhitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private final PRXMaintenance plugin;
    private final MaintenanceWhitelist maintenanceWhitelist;

    public MaintenanceCommand(PRXMaintenance plugin, MaintenanceWhitelist maintenanceWhitelist) {
        this.plugin = plugin;
        this.maintenanceWhitelist = maintenanceWhitelist;
    }

    /* ---------------- COMMAND ---------------- */

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("maintenance.admin")) {
            sender.sendMessage("§cKeine Rechte!");
            return true;
        }

        // /maintenance
        if (args.length == 0) {
            maintenanceWhitelist.toggleMaintenance(sender);
            return true;
        }

        // /maintenance on|off
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                maintenanceWhitelist.setMaintenance(sender, true);
                return true;
            }
            if (args[0].equalsIgnoreCase("off")) {
                maintenanceWhitelist.setMaintenance(sender, false);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {

                plugin.reloadConfig();

                // Maintenance-Status neu laden
                plugin.setMaintenance(
                        plugin.getConfig().getBoolean("maintenance.enabled")
                );

                sender.sendMessage("§aMaintenance-Config neu geladen.");
                return true;
            }

        }

        // /maintenance notify on|off
        if (args.length == 2 && args[0].equalsIgnoreCase("notify")) {

            if (args[1].equalsIgnoreCase("on")) {
                plugin.setNotifyEnabled(true);
                sender.sendMessage("§aMaintenance-Notifications aktiviert.");
                return true;
            }

            if (args[1].equalsIgnoreCase("off")) {
                plugin.setNotifyEnabled(false);
                sender.sendMessage("§cMaintenance-Notifications deaktiviert.");
                return true;
            }
        }


        // /maintenance whitelist ...
        if (args.length >= 2 && args[0].equalsIgnoreCase("whitelist")) {

            if (args[1].equalsIgnoreCase("on")) {
                plugin.setWhitelistEnabled(true);
                sender.sendMessage("§aMaintenance-Whitelist aktiviert.");
                return true;
            }

            if (args[1].equalsIgnoreCase("off")) {
                plugin.setWhitelistEnabled(false);
                sender.sendMessage("§cMaintenance-Whitelist deaktiviert.");
                return true;
            }

            if (args[1].equalsIgnoreCase("list")) {
                sender.sendMessage("§7Maintenance-Whitelist:");
                plugin.whitelist().getAll().forEach(uuid -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                    sender.sendMessage("§8- §f" + p.getName());
                });
                return true;
            }

            if (args.length == 3) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

                if (args[1].equalsIgnoreCase("add")) {
                    plugin.whitelist().add(target.getUniqueId());

                    List<String> list = plugin.getConfig()
                            .getStringList("maintenance.whitelist.players");
                    if (!list.contains(target.getName()))
                        list.add(target.getName());

                    plugin.getConfig().set("maintenance.whitelist.players", list);
                    plugin.saveConfig();

                    sender.sendMessage("§aSpieler zur Maintenance-Whitelist hinzugefügt.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("remove")) {
                    plugin.whitelist().remove(target.getUniqueId());

                    List<String> list = plugin.getConfig()
                            .getStringList("maintenance.whitelist.players");
                    list.remove(target.getName());

                    plugin.getConfig().set("maintenance.whitelist.players", list);
                    plugin.saveConfig();

                    sender.sendMessage("§cSpieler von der Maintenance-Whitelist entfernt.");
                    return true;
                }
            }
        }

        sender.sendMessage("§cVerwendung:");
        sender.sendMessage("§7/maintenance [on|off]");
        sender.sendMessage("§7/maintenance whitelist <add|remove|list> [player]");
        return true;
    }


    /* ---------------- TAB COMPLETER ---------------- */

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!sender.hasPermission("maintenance.admin")) return List.of();

        if (args.length == 1) {
            return List.of("on", "off", "whitelist", "reload", "notify").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("notify")) {
            return List.of("on", "off").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }


        if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            return List.of("add", "remove", "list", "on", "off").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 &&
                args[0].equalsIgnoreCase("whitelist") &&
                (args[1].equalsIgnoreCase("add")
                        || args[1].equalsIgnoreCase("remove"))) {

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
