package at.prx.bOD.commands;

import at.prx.bOD.manager.DeathBanManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UnbanCommand implements CommandExecutor {

    private final DeathBanManager manager;

    public UnbanCommand(DeathBanManager manager) {
        this.manager = manager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length != 1) {
            sender.sendMessage("§cBenutzung: /unban <Spieler>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        manager.unban(target.getUniqueId());

        sender.sendMessage("§a" + target.getName() + " wurde entbannt.");
        return true;
    }
}
