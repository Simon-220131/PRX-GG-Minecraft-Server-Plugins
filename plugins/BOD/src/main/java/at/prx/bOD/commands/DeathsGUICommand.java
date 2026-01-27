package at.prx.bOD.commands;

import at.prx.bOD.gui.DeathsGUI;
import at.prx.bOD.manager.DeathBanManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathsGUICommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final DeathBanManager manager;

    public DeathsGUICommand(JavaPlugin plugin, DeathBanManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur Ingame.");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("gui")) {
            p.sendMessage("§cBenutzung: /deaths gui <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        // Metadata speichern
        p.setMetadata("deathTarget", new FixedMetadataValue(plugin, target.getUniqueId()));

        Inventory gui = new DeathsGUI(plugin, manager, target.getUniqueId()).getInventory();
        p.openInventory(gui);

        return true;
    }
}
