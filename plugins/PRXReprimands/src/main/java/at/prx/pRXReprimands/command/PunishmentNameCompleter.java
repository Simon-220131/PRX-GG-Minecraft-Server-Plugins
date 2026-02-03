package at.prx.pRXReprimands.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PunishmentNameCompleter implements TabCompleter {
    private final Supplier<List<String>> namesSupplier;

    public PunishmentNameCompleter(Supplier<List<String>> namesSupplier) {
        this.namesSupplier = namesSupplier;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return namesSupplier.get().stream()
                .filter(name -> name != null && name.toLowerCase().startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }
}
