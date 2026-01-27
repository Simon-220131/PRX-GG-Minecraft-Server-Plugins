package at.prx.gamblePluginTest.commands;

import at.prx.gamblePluginTest.gui.SlotMachineGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CasinoCommand implements CommandExecutor {

    private final SlotMachineGUI gui;

    public CasinoCommand(SlotMachineGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        // /casino reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("casino.admin")) {
                sender.sendMessage(
                        Component.text("❌ Keine Berechtigung.", NamedTextColor.RED)
                );
                return true;
            }

            gui.reload();

            sender.sendMessage(
                    Component.text("✅ Casino Config neu geladen.", NamedTextColor.GREEN)
            );
            return true;
        }

        // normales /casino
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können das Casino benutzen.");
            return true;
        }

        gui.open(player);
        return true;
    }
}
