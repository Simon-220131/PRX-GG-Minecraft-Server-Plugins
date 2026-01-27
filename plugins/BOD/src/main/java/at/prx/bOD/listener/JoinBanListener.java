package at.prx.bOD.listener;

import at.prx.bOD.manager.DeathBanManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class JoinBanListener implements Listener {

    private final DeathBanManager manager;

    public JoinBanListener(DeathBanManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        if (!manager.isBanned(uuid))
            return;

        long until = manager.getBan(uuid);
        long now = System.currentTimeMillis();

        if (until <= now) {
            manager.unban(uuid);
            return;
        }

        long remaining = (until - now) / 1000;

        int deaths = manager.getDeaths(uuid);
        Component msg = buildBanMessage(deaths, format(remaining));

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg);
    }

    private Component buildBanMessage(int deaths, String duration) {
        String raw = """
            <dark_gray>───────────────</dark_gray>
            
            <gradient:#ff0000:#ff8800><bold>✦ DEATH BAN ✦</bold></gradient>

            <gray>Du bist leider gestorben…</gray>
            <gray>Strafe:</gray> <gold>%DURATION%</gold>
            <gray>Tode insgesamt:</gray> <red>%DEATHS%</red>

            <dark_gray>───────────────</dark_gray>
            """;

        raw = raw.replace("%DURATION%", duration);
        raw = raw.replace("%DEATHS%", String.valueOf(deaths));

        return MiniMessage.miniMessage().deserialize(raw);
    }

    private String format(long sec) {

        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long minutes = (sec % 3600) / 60;
        long seconds = sec % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
