package at.prx.pRXTeamChat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class TeamChatConfig {

    private static final String KEY_FORMAT = "format";
    private static final String DEFAULT_FORMAT = "&7[&bTC&7] &8<&b{server}&8> &f{player}&7: &r{message}";

    private final Path configPath;
    private String format;

    public TeamChatConfig(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.properties");
        this.format = DEFAULT_FORMAT;
    }

    public void load() {
        if (Files.notExists(configPath)) {
            saveDefaults();
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            this.format = props.getProperty(KEY_FORMAT, DEFAULT_FORMAT);
        } catch (IOException ignored) {
            this.format = DEFAULT_FORMAT;
        }
    }

    public String getFormat() {
        return format;
    }

    private void saveDefaults() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException ignored) {
            return;
        }
        Properties props = new Properties();
        props.setProperty(KEY_FORMAT, DEFAULT_FORMAT);
        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "PRXTeamChat configuration");
        } catch (IOException ignored) {
            // Keep defaults in memory if writing fails.
        }
    }
}
