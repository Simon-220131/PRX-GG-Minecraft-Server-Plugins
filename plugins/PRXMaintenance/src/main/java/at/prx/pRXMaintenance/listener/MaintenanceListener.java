package at.prx.pRXMaintenance.listener;

import at.prx.pRXMaintenance.PRXMaintenance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceListener implements Listener {

    private final PRXMaintenance plugin;

    public MaintenanceListener(PRXMaintenance plugin) {
        this.plugin = plugin;
    }

    /* ---------------- ASYNC (UUID / WHITELIST) ---------------- */

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {

        if (!plugin.isMaintenance()) return;

        // Whitelist erlaubt → keine Notification
        if (plugin.isWhitelistEnabled()
                && plugin.whitelist().isWhitelisted(event.getUniqueId())) {
            return;
        }

        // Kick
        event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                plugin.getKickComponent()
        );

        // Admin-Notification (async → sync!)
        if (!plugin.getConfig().getBoolean("maintenance.notify-admins")) return;

        String name = event.getName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Component msg = plugin.mm().deserialize(
                    plugin.getConfig()
                            .getString("maintenance.notify-message")
                            .replace("<player>", name)
            );

            plugin.notifyAdmins(msg);

            String plain = PlainTextComponentSerializer.plainText().serialize(msg);
            plugin.getLogger().info(plain);
        });
    }


    /* ---------------- SYNC (PERMISSIONS) ---------------- */

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {

        if (!plugin.isMaintenance()) return;

        Player player = event.getPlayer();

        // Admins dürfen immer rein
        if (player.hasPermission("maintenance.admin")) return;

        // Whitelist nur prüfen, wenn aktiviert
        if (plugin.isWhitelistEnabled()
                && plugin.whitelist().isWhitelisted(player.getUniqueId())) {
            return;
        }

        event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                plugin.getKickComponent()
        );
    }
}
