package at.prx.bOD.listener;

import at.prx.bOD.manager.DeathBanManager;
import at.prx.bOD.manager.DeathScoreboardManager;
import at.prx.gamblePluginTest.events.GambleActionEvent;
import at.prx.gamblePluginTest.states.SlotAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GambleActionListener implements Listener {

    private final DeathBanManager banManager;
    private final DeathScoreboardManager scoreboardManager;
    private final JavaPlugin plugin;

    public GambleActionListener(DeathBanManager banManager, DeathScoreboardManager scoreboardManager, JavaPlugin plugin) {
        this.banManager = banManager;
        this.scoreboardManager = scoreboardManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onGambleAction(GambleActionEvent event) {
        Player player = event.getPlayer();
        UUID pID = player.getUniqueId();
        SlotAction action = event.getAction();

        switch (action) {

            case INSTANT_BAN -> {
                player.closeInventory();

                player.showTitle(
                        Title.title(
                                Component.text("☠ VERLOREN", NamedTextColor.RED),
                                Component.text("SKILL ISSUE!", NamedTextColor.DARK_RED)
                        )
                );

                player.playSound(player.getLocation(),
                        Sound.ENTITY_WITHER_SPAWN, 1f, 0.8f);

                scoreboardManager.start();

                // Bann verzögert (1,5s = 30 Ticks)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        banManager.setBan(pID);
                    }
                }, 30L);
            }

            case BAN_COUNTER_PLUS -> {
                banManager.setDeaths(pID, banManager.getDeaths(pID) + 1);
            }

            case BAN_COUNTER_MINUS -> {
                banManager.setDeaths(pID, banManager.getDeaths(pID) - 1);
            }

            case BAN_SHIELD -> {
                banManager.giveBanShield(player);
            }

            case SPAWN_CREEPER -> { player.closeInventory(); }

            default -> {
                // BOD ignoriert alles andere
            }
        }
    }
}
