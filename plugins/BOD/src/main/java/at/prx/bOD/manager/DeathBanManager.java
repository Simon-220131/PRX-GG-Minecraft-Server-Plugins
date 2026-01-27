package at.prx.bOD.manager;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathBanManager {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration deathConfig;
    private File shieldsFile;
    private YamlConfiguration shieldsConfig;

    private final long baseSeconds = 60;                   // 1 Minute
    private final long maxBanSeconds = 3 * 24 * 3600;      // 3 Tage

    private final Map<UUID, Integer> banShields = new HashMap<>();

    public DeathBanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "deathdata.yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        deathConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            deathConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getDeaths(UUID uuid) {
        return deathConfig.getInt("deaths." + uuid, 0);
    }

    public void setDeaths(UUID uuid, int amount) {
        if (amount < 0) {
            amount = 0;
        }

        deathConfig.set("deaths." + uuid, amount);
        save();
    }

    public void setBan(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);

        if (consumeBanShield(p)) {
            return;
        }

        // Todeszähler
        int deaths = getDeaths(uuid) + 1;
        setDeaths(uuid, deaths);

        // exponentieller Anstieg
        long banSeconds = (long) (baseSeconds * Math.pow(2, deaths - 1));
        banSeconds = Math.min(banSeconds, maxBanSeconds);

        // Bannzeit speichern
        long until = System.currentTimeMillis() + (banSeconds * 1000);

        deathConfig.set("banned." + uuid, until);
        save();

        // Kick message
        Component msg = buildBanMessage(deaths, format(banSeconds));
        p.kick(msg);
    }

    public long getBan(UUID uuid) {
        return deathConfig.getLong("banned." + uuid, 0);
    }

    public void unban(UUID uuid) {
        deathConfig.set("banned." + uuid, null);
        save();
    }

    public boolean isBanned(UUID uuid) {
        return deathConfig.contains("banned." + uuid);
    }

/* =========================================================
   SHIELD LOGIK
   ========================================================= */

    public int getBanShields(Player player) {
        return banShields.getOrDefault(player.getUniqueId(), 0);
    }

    public void setBanShields(UUID id, int amount) {
        if (amount < 0) {
            plugin.getLogger().warning("[DeathBanManager] Zahl muss über null sein!");
            Bukkit.getPlayer(id).sendMessage(Component.text("§c❌ Zahl muss über null sein!"));
            return;
        }
        banShields.put(id, amount);
        saveShields();
    }

    public void giveBanShield(Player player) {
        UUID id = player.getUniqueId();
        int current = banShields.getOrDefault(id, 0);
        banShields.put(id, current + 1);
    }

    public boolean consumeBanShield(Player player) {
        UUID id = player.getUniqueId();
        int current = banShields.getOrDefault(id, 0);

        if (current <= 0) {
            return false;
        }

        if (current == 1) {
            banShields.remove(id);
        } else {
            banShields.put(id, current - 1);
        }

        // 🎬 TITLE
        Title.Times times = Title.Times.times(
                Duration.ofMillis(300),
                Duration.ofSeconds(2),
                Duration.ofMillis(500)
        );

        player.showTitle(
                Title.title(
                        Component.text("GERETTET!", NamedTextColor.AQUA),
                        Component.text("Dein Bann-Schild hat dich beschützt!", NamedTextColor.GRAY),
                        times
                )
        );

        // 💬 CHAT
        player.sendMessage(
                Component.text(
                        "Dein Bann-Schild wurde verbraucht und hat dich vor dem Bann bewahrt!",
                        NamedTextColor.GREEN
                )
        );

        // 🔊 SOUND (TOTEM = perfekt)
        Sound sound = Sound.sound(Key.key("item.totem.use"), Sound.Source.MASTER, 1.0f, 1.2f);
        player.playSound(sound);

        // ✨ PARTICLES (Schutz-Effekt)
        player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0),
                80,
                0.5,
                1.0,
                0.5,
                0.05
        );

        return true;
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

/* =========================================================
   LOAD / SAVE
   ========================================================= */

    public void loadShields() {
        shieldsFile = new File(plugin.getDataFolder(), "shields.yml");

        if (!shieldsFile.exists()) {
            shieldsFile.getParentFile().mkdirs();
            try {
                shieldsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte shields.yml nicht erstellen!");
                e.printStackTrace();
            }
        }

        shieldsConfig = YamlConfiguration.loadConfiguration(shieldsFile);

        banShields.clear();

        for (String key : shieldsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int amount = shieldsConfig.getInt(key, 0);

                if (amount > 0) {
                    banShields.put(uuid, amount);
                }
            } catch (IllegalArgumentException ignored) {
                // ungültiger UUID-String → ignorieren
            }
        }

        plugin.getLogger().info("[BOD] " + banShields.size() + " Ban-Shields geladen.");
    }

    public void saveShields() {
        if (shieldsConfig == null || shieldsFile == null) return;

        // Alte Einträge löschen
        for (String key : shieldsConfig.getKeys(false)) {
            shieldsConfig.set(key, null);
        }

        // Aktuelle Map speichern
        for (Map.Entry<UUID, Integer> entry : banShields.entrySet()) {
            shieldsConfig.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            shieldsConfig.save(shieldsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte shields.yml nicht speichern!");
            e.printStackTrace();
        }
    }
}
