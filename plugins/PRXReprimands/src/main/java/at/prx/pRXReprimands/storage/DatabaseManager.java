package at.prx.pRXReprimands.storage;

import at.prx.pRXReprimands.model.PunishmentRecord;
import at.prx.pRXReprimands.model.PunishmentType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
