package at.prx.gamblePluginTest.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class OnPlayerJoinListener implements Listener {

    private final JavaPlugin plugin;

    public OnPlayerJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        //Promo per Config deaktivierbar
        if (!plugin.getConfig().getBoolean("casino.join-message.enabled", true)) {
            return;
        }

        // Leerzeile für Abstand
        p.sendMessage(Component.empty());

        // 🎰 Titelzeile
        p.sendMessage(
                Component.text("🎰 CASINO 🎰", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
        );

        // Beschreibung
        p.sendMessage(
                Component.text("Teste dein Glück und riskiere alles!", NamedTextColor.YELLOW)
        );

        // Hinweis auf Command
        p.sendMessage(
                Component.text("➤ ", NamedTextColor.GRAY)
                        .append(
                                Component.text("/casino", NamedTextColor.GREEN)
                                        .clickEvent(ClickEvent.runCommand("/casino"))
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text("🎰 Klicke, um das Casino zu öffnen", NamedTextColor.YELLOW)
                                        ))
                        )
                        .append(Component.text(" zum Spielen", NamedTextColor.GRAY))
        );


        // Warnung / Flavor
        p.sendMessage(
                Component.text("☠ Gewinne Ruhm oder verliere alles…", NamedTextColor.RED)
        );

        // Leerzeile am Ende
        p.sendMessage(Component.empty());
    }
}
