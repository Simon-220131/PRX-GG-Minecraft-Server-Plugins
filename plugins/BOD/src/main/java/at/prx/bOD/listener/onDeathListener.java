package at.prx.bOD.listener;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;

public class onDeathListener implements Listener {
    Audience audience = Bukkit.getServer();

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player p = e.getPlayer();
        int deaths = p.getStatistic(Statistic.DEATHS);

        Duration duration = Duration.ofMinutes(deaths^2);

        if (deaths > 15){
            duration = Duration.ofDays(3);
        }

        p.ban("Du wurdest gebannt! SKILL ISSUE", duration, "NIGGER");

        Sound sound = Sound.sound(Key.key("minecraft:entity.wither.death"), Sound.Source.MASTER, 100.0f, 1.0f);

        audience.playSound(sound);
    }
}
