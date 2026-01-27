package at.prx.bOD.commands;

import at.prx.bOD.gui.DeathsGUI;
import at.prx.bOD.manager.DeathBanManager;
import at.prx.bOD.manager.DeathScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DeathsCommands implements CommandExecutor, TabCompleter {
    private final DeathBanManager manager;
    private final JavaPlugin plugin;

    public DeathsCommands(DeathBanManager manager, JavaPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        // /deaths <set|reset> <player> [amount]
        if (args.length < 2) {
            sender.sendMessage("§cBenutzung: /deaths <set|reset> <player> [amount]");
            return true;
        }

        String action = args[0].toLowerCase();
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = player.getUniqueId();

        switch (action) {

            case "reset" -> {
                manager.setDeaths(uuid, 0);
                sender.sendMessage("§aTode von §e" + player.getName() + "§a wurden zurückgesetzt.");
            }

            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cBitte gebe eine Anzahl an: /deaths set <player> <amount>");
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cUngültige Zahl.");
                    return true;
                }

                manager.setDeaths(uuid, amount);
                sender.sendMessage("§aTode von §e" + player.getName()
                        + "§a wurden auf §e" + amount + "§a gesetzt.");
            }

    /* =======================
       SHIELD COMMANDS
       ======================= */
            case "shield" -> {

                if (args.length < 3) {
                    sender.sendMessage("§cBenutzung: /deaths shield <set|reset> <player> [amount]");
                    return true;
                }

                String shieldAction = args[1].toLowerCase();
                OfflinePlayer shieldTarget = Bukkit.getOfflinePlayer(args[2]);
                UUID shieldUUID = shieldTarget.getUniqueId();

                switch (shieldAction) {

                    case "set" -> {
                        if (args.length < 4) {
                            sender.sendMessage("§cBenutzung: /deaths shield set <player> <amount>");
                            return true;
                        }

                        int amount;
                        try {
                            amount = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cUngültige Zahl.");
                            return true;
                        }

                        manager.setBanShields(shieldUUID, amount);
                        sender.sendMessage("§aBan-Shields von §e" + shieldTarget.getName()
                                + "§a wurden auf §e" + amount + "§a gesetzt.");
                    }

                    case "reset" -> {
                        manager.setBanShields(shieldUUID, 0);
                        sender.sendMessage("§aBan-Shields von §e" + shieldTarget.getName()
                                + "§a wurden zurückgesetzt.");
                    }

                    default -> sender.sendMessage("§cNutze: /deaths shield <set|reset> <player> [amount]");
                }
            }

            case "gui" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§cNur Ingame möglich.");
                    return true;
                }

                p.setMetadata("deathTarget", new FixedMetadataValue(plugin, uuid));
                Inventory gui = new DeathsGUI(plugin, manager, uuid).getInventory();
                p.openInventory(gui);

                sender.sendMessage("§aGUI für §e" + player.getName() + "§a geöffnet.");
            }

            default -> sender.sendMessage("§cUnbekannte Aktion.");
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command cmd,
                                                @NotNull String label,
                                                @NotNull String[] args) {

        if (!sender.hasPermission("deathban.admin")) {
            return Collections.emptyList();
        }

    /* =========================
       /deaths <action>
       ========================= */
        if (args.length == 1) {
            return Arrays.asList("set", "reset", "gui", "shield")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

    /* =========================
       /deaths shield <sub>
       ========================= */
        if (args.length == 2 && args[0].equalsIgnoreCase("shield")) {
            return Arrays.asList("set", "reset")
                    .stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

    /* =========================
       /deaths set|reset|gui <player>
       ========================= */
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

    /* =========================
       /deaths set <player> <amount>
       ========================= */
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("0", "1", "2", "5")
                    .stream()
                    .filter(s -> s.startsWith(args[2]))
                    .toList();
        }

    /* =========================
       /deaths shield set <player>
       ========================= */
        if (args.length == 3
                && args[0].equalsIgnoreCase("shield")
                && args[1].equalsIgnoreCase("set")) {

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

    /* =========================
       /deaths shield reset <player>
       ========================= */
        if (args.length == 3
                && args[0].equalsIgnoreCase("shield")
                && args[1].equalsIgnoreCase("reset")) {

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

    /* =========================
       /deaths shield set <player> <amount>
       ========================= */
        if (args.length == 4
                && args[0].equalsIgnoreCase("shield")
                && args[1].equalsIgnoreCase("set")) {

            return Arrays.asList("0", "1", "2", "5")
                    .stream()
                    .filter(s -> s.startsWith(args[3]))
                    .toList();
        }

        return Collections.emptyList();
    }

}
