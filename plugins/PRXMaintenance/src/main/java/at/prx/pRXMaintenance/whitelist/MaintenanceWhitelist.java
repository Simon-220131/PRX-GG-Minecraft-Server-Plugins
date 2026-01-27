package at.prx.pRXMaintenance.whitelist;

import at.prx.pRXMaintenance.PRXMaintenance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MaintenanceWhitelist {

    private final PRXMaintenance plugin;

    private final Set<UUID> whitelisted = new HashSet<>();

    public MaintenanceWhitelist(PRXMaintenance plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig(Set<String> names) {
        whitelisted.clear();
        for (String name : names) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            if (player.hasPlayedBefore() || player.isOnline()) {
                whitelisted.add(player.getUniqueId());
            }
        }
    }

    public boolean isWhitelisted(UUID uuid) {
        return whitelisted.contains(uuid);
    }

    public void add(UUID uuid) {
        whitelisted.add(uuid);
    }

    public void remove(UUID uuid) {
        whitelisted.remove(uuid);
    }

    public Set<UUID> getAll() {
        return whitelisted;
    }

    public void toggleMaintenance(CommandSender sender) {
        setMaintenance(sender, !plugin.isMaintenance());
    }

    public void setMaintenance(CommandSender sender, boolean state) {
        plugin.setMaintenance(state);

        sender.sendMessage("§7Maintenance ist jetzt: " +
                (state ? "§cAKTIV" : "§aINAKTIV"));

        if (state) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("maintenance.admin")
                        && !plugin.whitelist().isWhitelisted(p.getUniqueId())) {
                    p.kick(Component.text("§cServer ist in Maintenance!"));
                }
            }
        }
    }
}
