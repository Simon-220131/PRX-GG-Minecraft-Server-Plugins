package at.prx.pRXReprimands.logging;

import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ReprimandLogger {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Plugin plugin;
    private final File logFile;

    public ReprimandLogger(Plugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "reprimands.log");
    }

    public void log(String message) {
        String line = "[" + FORMATTER.format(Instant.now()) + "] " + message;
        plugin.getLogger().info(line);
        appendToFile(line);
    }

    private void appendToFile(String line) {
        if (!logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException ignored) {
        }
    }
}
