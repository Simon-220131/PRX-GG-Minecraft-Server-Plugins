package at.prx.pRXReprimands.storage;

import at.prx.pRXReprimands.model.NoteRecord;
import at.prx.pRXReprimands.model.PunishmentHistoryRecord;
import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.model.PunishmentType;
import at.prx.pRXReprimands.model.ReasonCount;
import at.prx.pRXReprimands.model.WarningRecord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager implements AutoCloseable {
    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("database.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String name = plugin.getConfig().getString("database.name", "minecraft");
        String user = plugin.getConfig().getString("database.user", "root");
        String password = plugin.getConfig().getString("database.password", "");
        int poolSize = plugin.getConfig().getInt("database.pool-size", 10);
        long timeout = plugin.getConfig().getLong("database.connection-timeout-ms", 10000L);

        String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + name
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false";
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MariaDB driver not found. Ensure the plugin jar includes the driver.", ex);
        }
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setMaximumPoolSize(poolSize);
        config.setConnectionTimeout(timeout);
        config.setPoolName("PRXReprimands");

        dataSource = new HikariDataSource(config);
        createSchema();
    }

    public List<PunishmentRecord> loadActivePunishments() {
        long now = System.currentTimeMillis();
        String sql = "SELECT type, target_uuid, target_name, actor, reason, start_ms, end_ms "
                + "FROM prx_reprimands WHERE end_ms = 0 OR end_ms > ?";
        List<PunishmentRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                    UUID target = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long start = rs.getLong("start_ms");
                    long end = rs.getLong("end_ms");
                    records.add(new PunishmentRecord(type, target, targetName, actor, reason, start, end));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load punishments: " + ex.getMessage());
        }
        return records;
    }

    public List<PunishmentRecord> loadExpiredPunishments() {
        long now = System.currentTimeMillis();
        String sql = "SELECT type, target_uuid, target_name, actor, reason, start_ms, end_ms "
                + "FROM prx_reprimands WHERE end_ms > 0 AND end_ms <= ?";
        List<PunishmentRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
                    UUID target = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long start = rs.getLong("start_ms");
                    long end = rs.getLong("end_ms");
                    records.add(new PunishmentRecord(type, target, targetName, actor, reason, start, end));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load expired punishments: " + ex.getMessage());
        }
        return records;
    }

    public void upsertPunishment(PunishmentRecord record) {
        String sql = "INSERT INTO prx_reprimands (type, target_uuid, target_name, actor, reason, start_ms, end_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE target_name=VALUES(target_name), actor=VALUES(actor), "
                + "reason=VALUES(reason), start_ms=VALUES(start_ms), end_ms=VALUES(end_ms)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.type().name());
            statement.setString(2, record.target().toString());
            statement.setString(3, record.targetName());
            statement.setString(4, record.actor());
            statement.setString(5, record.reason());
            statement.setLong(6, record.startMillis());
            statement.setLong(7, record.endMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to save punishment: " + ex.getMessage());
        }
    }

    public void addPunishmentHistory(PunishmentRecord record) {
        String sql = "INSERT INTO prx_reprimands_history "
                + "(type, target_uuid, target_name, actor, reason, start_ms, end_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.type().name());
            statement.setString(2, record.target().toString());
            statement.setString(3, record.targetName());
            statement.setString(4, record.actor());
            statement.setString(5, record.reason());
            statement.setLong(6, record.startMillis());
            statement.setLong(7, record.endMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to save punishment history: " + ex.getMessage());
        }
    }

    public boolean closePunishmentHistory(UUID target, PunishmentType type, long endMillis) {
        String select = "SELECT id FROM prx_reprimands_history "
                + "WHERE target_uuid = ? AND type = ? AND (end_ms = 0 OR end_ms > ?) "
                + "ORDER BY start_ms DESC LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, target.toString());
            statement.setString(2, type.name());
            statement.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String update = "UPDATE prx_reprimands_history SET end_ms = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(update)) {
                        updateStmt.setLong(1, endMillis);
                        updateStmt.setLong(2, id);
                        return updateStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to close punishment history: " + ex.getMessage());
        }
        return false;
    }

    public int getHistoryCount(UUID target) {
        String sql = "SELECT COUNT(*) AS cnt FROM prx_reprimands_history WHERE target_uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get history count: " + ex.getMessage());
        }
        return 0;
    }

    public int getPunishmentHistoryCount() {
        String sql = "SELECT COUNT(*) AS cnt FROM prx_reprimands_history";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get punishment history count: " + ex.getMessage());
        }
        return 0;
    }

    public Map<PunishmentType, Integer> getPunishmentCountsByType() {
        String sql = "SELECT type, COUNT(*) AS cnt FROM prx_reprimands_history GROUP BY type";
        Map<PunishmentType, Integer> counts = new EnumMap<>(PunishmentType.class);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("type");
                try {
                    counts.put(PunishmentType.valueOf(type), rs.getInt("cnt"));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get punishment counts: " + ex.getMessage());
        }
        return counts;
    }

    public Map<PunishmentType, Integer> getActivePunishmentCountsByType() {
        long now = System.currentTimeMillis();
        String sql = "SELECT type, COUNT(*) AS cnt FROM prx_reprimands WHERE end_ms = 0 OR end_ms > ? GROUP BY type";
        Map<PunishmentType, Integer> counts = new EnumMap<>(PunishmentType.class);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    try {
                        counts.put(PunishmentType.valueOf(type), rs.getInt("cnt"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get active punishment counts: " + ex.getMessage());
        }
        return counts;
    }

    public List<PunishmentHistoryRecord> listHistory(UUID target, int offset, int limit) {
        String sql = "SELECT id, type, target_uuid, target_name, actor, reason, start_ms, end_ms "
                + "FROM prx_reprimands_history WHERE target_uuid = ? "
                + "ORDER BY start_ms DESC LIMIT ? OFFSET ?";
        List<PunishmentHistoryRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    PunishmentType foundType = PunishmentType.valueOf(rs.getString("type"));
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long start = rs.getLong("start_ms");
                    long end = rs.getLong("end_ms");
                    records.add(new PunishmentHistoryRecord(id, foundType, targetUuid, targetName, actor, reason, start, end));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to list history: " + ex.getMessage());
        }
        return records;
    }

    public void deletePunishment(UUID target, PunishmentType type) {
        String sql = "DELETE FROM prx_reprimands WHERE target_uuid = ? AND type = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            statement.setString(2, type.name());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to delete punishment: " + ex.getMessage());
        }
    }

    public PunishmentRecord getActivePunishment(UUID target, PunishmentType type) {
        long now = System.currentTimeMillis();
        String sql = "SELECT type, target_uuid, target_name, actor, reason, start_ms, end_ms "
                + "FROM prx_reprimands WHERE target_uuid = ? AND type = ? AND (end_ms = 0 OR end_ms > ?) LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            statement.setString(2, type.name());
            statement.setLong(3, now);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PunishmentType foundType = PunishmentType.valueOf(rs.getString("type"));
                    UUID foundTarget = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long start = rs.getLong("start_ms");
                    long end = rs.getLong("end_ms");
                    return new PunishmentRecord(foundType, foundTarget, targetName, actor, reason, start, end);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load punishment: " + ex.getMessage());
        }
        return null;
    }

    public void cleanupExpired() {
        String sql = "DELETE FROM prx_reprimands WHERE end_ms > 0 AND end_ms <= ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to cleanup punishments: " + ex.getMessage());
        }
    }

    public int addWarning(UUID target, String targetName, String actor, String reason) {
        String insert = "INSERT INTO prx_warnings (target_uuid, target_name, actor, reason, created_ms) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, target.toString());
            statement.setString(2, targetName);
            statement.setString(3, actor);
            statement.setString(4, reason);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to add warning: " + ex.getMessage());
        }
        return getWarningCount(target);
    }

    public int getWarningCount(UUID target) {
        String sql = "SELECT COUNT(*) AS cnt FROM prx_warnings WHERE target_uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get warning count: " + ex.getMessage());
        }
        return 0;
    }

    public int getWarningTotalCount() {
        String sql = "SELECT COUNT(*) AS cnt FROM prx_warnings";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get warning total count: " + ex.getMessage());
        }
        return 0;
    }

    public List<ReasonCount> getTopPunishmentReasons(int limit) {
        String sql = "SELECT reason, COUNT(*) AS cnt FROM prx_reprimands_history "
                + "WHERE reason <> '' GROUP BY reason ORDER BY cnt DESC LIMIT ?";
        List<ReasonCount> reasons = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String reason = rs.getString("reason");
                    if (reason == null || reason.trim().isEmpty()) {
                        continue;
                    }
                    reasons.add(new ReasonCount(reason.trim(), rs.getInt("cnt")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get punishment reasons: " + ex.getMessage());
        }
        return reasons;
    }

    public List<ReasonCount> getTopWarningReasons(int limit) {
        String sql = "SELECT reason, COUNT(*) AS cnt FROM prx_warnings "
                + "WHERE reason <> '' GROUP BY reason ORDER BY cnt DESC LIMIT ?";
        List<ReasonCount> reasons = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String reason = rs.getString("reason");
                    if (reason == null || reason.trim().isEmpty()) {
                        continue;
                    }
                    reasons.add(new ReasonCount(reason.trim(), rs.getInt("cnt")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to get warning reasons: " + ex.getMessage());
        }
        return reasons;
    }

    public List<WarningRecord> listWarnings(UUID target) {
        String sql = "SELECT id, target_uuid, target_name, actor, reason, created_ms "
                + "FROM prx_warnings WHERE target_uuid = ? ORDER BY created_ms ASC";
        List<WarningRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long created = rs.getLong("created_ms");
                    records.add(new WarningRecord(id, targetUuid, targetName, actor, reason, created));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to list warnings: " + ex.getMessage());
        }
        return records;
    }

    public int clearWarnings(UUID target) {
        String sql = "DELETE FROM prx_warnings WHERE target_uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            return statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to clear warnings: " + ex.getMessage());
        }
        return 0;
    }

    public WarningRecord getWarningById(long id) {
        String sql = "SELECT id, target_uuid, target_name, actor, reason, created_ms "
                + "FROM prx_warnings WHERE id = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    long warningId = rs.getLong("id");
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String reason = rs.getString("reason");
                    long created = rs.getLong("created_ms");
                    return new WarningRecord(warningId, targetUuid, targetName, actor, reason, created);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load warning: " + ex.getMessage());
        }
        return null;
    }

    public boolean deleteWarning(long id) {
        String sql = "DELETE FROM prx_warnings WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to delete warning: " + ex.getMessage());
        }
        return false;
    }

    public long addNote(UUID target, String targetName, String actor, String note) {
        String insert = "INSERT INTO prx_notes (target_uuid, target_name, actor, note, created_ms) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, target.toString());
            statement.setString(2, targetName);
            statement.setString(3, actor);
            statement.setString(4, note);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to add note: " + ex.getMessage());
        }
        return -1L;
    }

    public List<NoteRecord> listNotes(UUID target) {
        String sql = "SELECT id, target_uuid, target_name, actor, note, created_ms "
                + "FROM prx_notes WHERE target_uuid = ? ORDER BY created_ms ASC";
        List<NoteRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String note = rs.getString("note");
                    long created = rs.getLong("created_ms");
                    records.add(new NoteRecord(id, targetUuid, targetName, actor, note, created));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to list notes: " + ex.getMessage());
        }
        return records;
    }

    public NoteRecord getNoteById(long id) {
        String sql = "SELECT id, target_uuid, target_name, actor, note, created_ms "
                + "FROM prx_notes WHERE id = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    long noteId = rs.getLong("id");
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    String targetName = rs.getString("target_name");
                    String actor = rs.getString("actor");
                    String note = rs.getString("note");
                    long created = rs.getLong("created_ms");
                    return new NoteRecord(noteId, targetUuid, targetName, actor, note, created);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load note: " + ex.getMessage());
        }
        return null;
    }

    public boolean deleteNote(long id) {
        String sql = "DELETE FROM prx_notes WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to delete note: " + ex.getMessage());
        }
        return false;
    }

    private void createSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS prx_reprimands ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "type VARCHAR(10) NOT NULL,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name VARCHAR(32),"
                + "actor VARCHAR(64) NOT NULL,"
                + "reason VARCHAR(256) NOT NULL,"
                + "start_ms BIGINT NOT NULL,"
                + "end_ms BIGINT NOT NULL,"
                + "UNIQUE KEY uniq_target_type (target_uuid, type),"
                + "KEY idx_end_ms (end_ms)"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to create schema: " + ex.getMessage());
        }

        String historySql = "CREATE TABLE IF NOT EXISTS prx_reprimands_history ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "type VARCHAR(10) NOT NULL,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name VARCHAR(32),"
                + "actor VARCHAR(64) NOT NULL,"
                + "reason VARCHAR(256) NOT NULL,"
                + "start_ms BIGINT NOT NULL,"
                + "end_ms BIGINT NOT NULL,"
                + "KEY idx_history_target (target_uuid),"
                + "KEY idx_history_start (start_ms)"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(historySql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to create history table: " + ex.getMessage());
        }

        String warningsSql = "CREATE TABLE IF NOT EXISTS prx_warnings ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name VARCHAR(32),"
                + "actor VARCHAR(64) NOT NULL,"
                + "reason VARCHAR(256) NOT NULL,"
                + "created_ms BIGINT NOT NULL,"
                + "KEY idx_target_uuid (target_uuid)"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(warningsSql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to create warnings table: " + ex.getMessage());
        }

        String notesSql = "CREATE TABLE IF NOT EXISTS prx_notes ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name VARCHAR(32),"
                + "actor VARCHAR(64) NOT NULL,"
                + "note VARCHAR(512) NOT NULL,"
                + "created_ms BIGINT NOT NULL,"
                + "KEY idx_notes_target_uuid (target_uuid)"
                + ")";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(notesSql);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to create notes table: " + ex.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
