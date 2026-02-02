package at.prx.pRXRanks.listener;

import at.prx.pRXRanks.manager.NametagManager;
import at.prx.pRXRanks.manager.TablistManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LuckPermsListener {

    private final JavaPlugin plugin;

    public LuckPermsListener(
            JavaPlugin plugin,
            TablistManager tablistManager,
            NametagManager nametagManager
    ) {
        this.plugin = plugin;

        LuckPerms luckPerms = LuckPermsProvider.get();

        luckPerms.getEventBus().subscribe(
                UserDataRecalculateEvent.class,
                event -> handle(event, tablistManager, nametagManager)
        );
    }

    private void handle(
            UserDataRecalculateEvent event,
            TablistManager tablistManager,
            NametagManager nametagManager
    ) {
        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            tablistManager.update(player);
            nametagManager.update(player);
        });
    }
}
