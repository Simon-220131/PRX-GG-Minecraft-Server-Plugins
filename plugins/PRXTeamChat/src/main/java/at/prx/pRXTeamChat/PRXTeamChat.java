package at.prx.pRXTeamChat;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "prxteamchat", name = "PRXTeamChat", version = "1.0")
public class PRXTeamChat {

    public static final String PERMISSION_USE = "prxteamchat.use";
    public static final String PERMISSION_SEE = "prxteamchat.see";

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private TeamChatConfig config;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config = new TeamChatConfig(dataDirectory);
        this.config.load();

        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register("tc", new TeamChatCommand(this), "teamchat");
        logger.info("PRXTeamChat loaded.");
    }

    public void broadcastTeamChat(CommandSource source, String message) {
        Component formatted = buildMessage(source, message);
        for (Player player : proxy.getAllPlayers()) {
            if (player.hasPermission(PERMISSION_SEE)) {
                player.sendMessage(formatted);
            }
        }
        if (!(source instanceof Player) && source.hasPermission(PERMISSION_SEE)) {
            source.sendMessage(formatted);
        }
        logger.info(legacySerializer.serialize(formatted));
    }

    public void sendSystemMessage(CommandSource source, String message) {
        source.sendMessage(legacySerializer.deserialize(message));
    }

    private Component buildMessage(CommandSource source, String message) {
        String playerName = source instanceof Player player ? player.getUsername() : "CONSOLE";
        String serverName = "Proxy";
        if (source instanceof Player player) {
            serverName = player.getCurrentServer()
                    .map(current -> current.getServerInfo().getName())
                    .orElse("Unknown");
        }
        String formatted = config.getFormat()
                .replace("{server}", serverName)
                .replace("{player}", playerName)
                .replace("{message}", message);
        return legacySerializer.deserialize(formatted);
    }
}
