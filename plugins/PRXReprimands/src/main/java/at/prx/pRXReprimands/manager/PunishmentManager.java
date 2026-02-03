package at.prx.pRXReprimands.manager;

import at.prx.pRXReprimands.PRXReprimands;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.model.PunishmentType;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PunishmentManager {
    private final PRXReprimands plugin;
    private final File dataFile;
    private final Map<UUID, PunishmentRecord> bans = new HashMap<>();
    private final Map<UUID, PunishmentRecord> mutes = new HashMap<>();

    public PunishmentManager(PRXReprimands plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "punishments.yml");
    }

    public void load() {
        bans.clear();
        mutes.clear();
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        loadSection(config.getConfigurationSection("bans"), PunishmentType.BAN, bans);
        loadSection(config.getConfigurationSection("mutes"), PunishmentType.MUTE, mutes);
        cleanupExpired();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        saveSection(config, "bans", bans);
        saveSection(config, "mutes", mutes);
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            config.save(dataFile);
        } catch (IOException ignored) {
        }
    }

    public void ban(OfflinePlayer target, String actor, String reason, long durationMillis) {
        long start = System.currentTimeMillis();
        long end = durationMillis > 0 ? start + durationMillis : 0L;
        PunishmentRecord record = new PunishmentRecord(
                PunishmentType.BAN,
                target.getUniqueId(),
                target.getName(),
                actor,
                reason,
                start,
                end
        );
        bans.put(record.target(), record);
    }

    public void mute(OfflinePlayer target, String actor, String reason, long durationMillis) {
        long start = System.currentTimeMillis();
        long end = durationMillis > 0 ? start + durationMillis : 0L;
        PunishmentRecord record = new PunishmentRecord(
                PunishmentType.MUTE,
                target.getUniqueId(),
                target.getName(),
                actor,
                reason,
                start,
                end
        );
        mutes.put(record.target(), record);
    }

    public boolean unban(UUID target) {
        return bans.remove(target) != null;
    }

    public boolean unmute(UUID target) {
        return mutes.remove(target) != null;
    }

    public PunishmentRecord getBan(UUID target) {
        PunishmentRecord record = bans.get(target);
        if (record != null && record.isExpired()) {
            bans.remove(target);
            return null;
        }
        return record;
    }

    public PunishmentRecord getMute(UUID target) {
        PunishmentRecord record = mutes.get(target);
        if (record != null && record.isExpired()) {
            mutes.remove(target);
            return null;
        }
        return record;
    }

    public void cleanupExpired() {
        bans.values().removeIf(PunishmentRecord::isExpired);
        mutes.values().removeIf(PunishmentRecord::isExpired);
    }

    public List<String> getBannedNames() {
        return bans.values().stream()
                .map(PunishmentRecord::targetName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    public List<String> getMutedNames() {
        return mutes.values().stream()
                .map(PunishmentRecord::targetName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    public OfflinePlayer resolvePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    public OfflinePlayer resolveExistingPlayer(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (player == null) {
            return null;
        }
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            return null;
        }
        return player;
    }

    private void loadSection(ConfigurationSection section,
                             PunishmentType type,
                             Map<UUID, PunishmentRecord> map) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                String targetName = entry.getString("targetName");
                String actor = entry.getString("actor", "Console");
                String reason = entry.getString("reason", "Keine Angabe");
                long start = entry.getLong("start", System.currentTimeMillis());
                long end = entry.getLong("end", 0L);
                map.put(uuid, new PunishmentRecord(type, uuid, targetName, actor, reason, start, end));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveSection(YamlConfiguration config,
                             String path,
                             Map<UUID, PunishmentRecord> map) {
        for (PunishmentRecord record : map.values()) {
            String key = path + "." + record.target();
            config.set(key + ".targetName", record.targetName());
            config.set(key + ".actor", record.actor());
            config.set(key + ".reason", record.reason());
            config.set(key + ".start", record.startMillis());
            config.set(key + ".end", record.endMillis());
        }
    }
}
