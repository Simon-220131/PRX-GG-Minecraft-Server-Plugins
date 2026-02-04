package at.prx.pRXReprimands.command;

import at.prx.pRXReprimands.manager.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveNoteCompleter implements TabCompleter {
    private final PunishmentManager punishmentManager;

    public RemoveNoteCompleter(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
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
            OfflinePlayer target = punishmentManager.resolveExistingPlayer(args[0]);
            if (target == null) {
                return List.of();
            }
            String prefix = args[1];
            return punishmentManager.listNotes(target.getUniqueId()).stream()
                    .map(note -> "#" + note.id())
                    .filter(id -> id.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
