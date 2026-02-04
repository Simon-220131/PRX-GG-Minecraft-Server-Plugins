package at.prx.pRXReprimands.manager;

import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.model.PunishmentType;
import at.prx.pRXReprimands.model.PunishmentHistoryRecord;
import at.prx.pRXReprimands.storage.DatabaseManager;
import at.prx.pRXReprimands.model.WarningRecord;
import at.prx.pRXReprimands.model.NoteRecord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PunishmentManager {
    private final DatabaseManager databaseManager;
    private final Map<UUID, PunishmentRecord> bans = new ConcurrentHashMap<>();
    private final Map<UUID, PunishmentRecord> mutes = new ConcurrentHashMap<>();

    public PunishmentManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void load() {
        bans.clear();
        mutes.clear();
        applyRecords(databaseManager.loadActivePunishments());
    }

    public void save() {
        databaseManager.cleanupExpired();
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
        databaseManager.upsertPunishment(record);
        databaseManager.addPunishmentHistory(record);
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
        databaseManager.upsertPunishment(record);
        databaseManager.addPunishmentHistory(record);
    }

    public boolean unban(UUID target) {
        PunishmentRecord removed = bans.remove(target);
        if (removed != null) {
            databaseManager.closePunishmentHistory(target, PunishmentType.BAN, System.currentTimeMillis());
            databaseManager.deletePunishment(target, PunishmentType.BAN);
            return true;
        }
        return false;
    }

    public boolean unmute(UUID target) {
        PunishmentRecord removed = mutes.remove(target);
        if (removed != null) {
            databaseManager.closePunishmentHistory(target, PunishmentType.MUTE, System.currentTimeMillis());
            databaseManager.deletePunishment(target, PunishmentType.MUTE);
            return true;
        }
        return false;
    }

    public PunishmentRecord getBan(UUID target) {
        PunishmentRecord record = bans.get(target);
        if (record != null && record.isExpired()) {
            bans.remove(target);
            databaseManager.deletePunishment(target, PunishmentType.BAN);
            return null;
        }
        if (record != null) {
            return record;
        }
        PunishmentRecord fromDb = databaseManager.getActivePunishment(target, PunishmentType.BAN);
        if (fromDb != null) {
            bans.put(target, fromDb);
        }
        return fromDb;
    }

    public PunishmentRecord getMute(UUID target) {
        PunishmentRecord record = mutes.get(target);
        if (record != null && record.isExpired()) {
            mutes.remove(target);
            databaseManager.deletePunishment(target, PunishmentType.MUTE);
            return null;
        }
        if (record != null) {
            return record;
        }
        PunishmentRecord fromDb = databaseManager.getActivePunishment(target, PunishmentType.MUTE);
        if (fromDb != null) {
            mutes.put(target, fromDb);
        }
        return fromDb;
    }

    public void cleanupExpired() {
        bans.values().removeIf(PunishmentRecord::isExpired);
        mutes.values().removeIf(PunishmentRecord::isExpired);
        databaseManager.cleanupExpired();
    }

    public int addWarning(OfflinePlayer target, String actor, String reason) {
        return databaseManager.addWarning(target.getUniqueId(), target.getName(), actor, reason);
    }

    public int getWarningCount(UUID target) {
        return databaseManager.getWarningCount(target);
    }

    public List<WarningRecord> listWarnings(UUID target) {
        return databaseManager.listWarnings(target);
    }

    public int clearWarnings(UUID target) {
        return databaseManager.clearWarnings(target);
    }

    public WarningRecord getWarningById(long id) {
        return databaseManager.getWarningById(id);
    }

    public boolean deleteWarning(long id) {
        return databaseManager.deleteWarning(id);
    }

    public long addNote(OfflinePlayer target, String actor, String note) {
        return databaseManager.addNote(target.getUniqueId(), target.getName(), actor, note);
    }

    public List<NoteRecord> listNotes(UUID target) {
        return databaseManager.listNotes(target);
    }

    public int getHistoryCount(UUID target) {
        return databaseManager.getHistoryCount(target);
    }

    public List<PunishmentHistoryRecord> listHistory(UUID target, int offset, int limit) {
        return databaseManager.listHistory(target, offset, limit);
    }


    public NoteRecord getNoteById(long id) {
        return databaseManager.getNoteById(id);
    }

    public boolean deleteNote(long id) {
        return databaseManager.deleteNote(id);
    }

    public void refreshFromDatabase() {
        applyRecords(databaseManager.loadActivePunishments());
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

    private void applyRecords(List<PunishmentRecord> records) {
        Map<UUID, PunishmentRecord> nextBans = new ConcurrentHashMap<>();
        Map<UUID, PunishmentRecord> nextMutes = new ConcurrentHashMap<>();
        for (PunishmentRecord record : records) {
            if (record.type() == PunishmentType.BAN) {
                nextBans.put(record.target(), record);
            } else if (record.type() == PunishmentType.MUTE) {
                nextMutes.put(record.target(), record);
            }
        }
        bans.clear();
        bans.putAll(nextBans);
        mutes.clear();
        mutes.putAll(nextMutes);
        cleanupExpired();
    }
}
