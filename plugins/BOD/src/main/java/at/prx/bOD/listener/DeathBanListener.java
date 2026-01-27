package at.prx.bOD.listener;

import at.prx.bOD.manager.DeathBanManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;
import java.util.UUID;

public class DeathBanListener implements Listener {

    private final DeathBanManager manager;
    ForwardingAudience audience = Bukkit.getServer();

    public DeathBanListener(DeathBanManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (manager.consumeBanShield(player)) {
            return;
        }

        manager.setBan(uuid);

        Sound sound = Sound.sound(
                Key.key("minecraft:entity.wither.death"),
                Sound.Source.MASTER,
                200.0f,
                1.0f
        );
        audience.playSound(sound);
    }

}
