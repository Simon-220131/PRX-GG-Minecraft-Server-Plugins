package at.prx.ggnetwork.lobbyboard.board;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LobbyBoardManager {

    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;

    private final Map<UUID, LobbyBoard> boards = new ConcurrentHashMap<>();

    private int taskTitle = -1;
    private int taskPing = -1;
    private int taskOnline = -1;
    private int taskNametags = -1;

    public LobbyBoardManager(JavaPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public void startTasks() {
        long titleEvery = Math.max(1, plugin.getConfig().getLong("update.title", 3));
        long pingEvery = Math.max(1, plugin.getConfig().getLong("update.ping", 20));
        long onlineEvery = Math.max(1, plugin.getConfig().getLong("update.online", 40));
        long nametagsEvery = Math.max(1, plugin.getConfig().getLong("update.nametags", 20));

        taskTitle = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (LobbyBoard b : boards.values()) b.updateTitle();
        }, 0L, titleEvery).getTaskId();

        taskPing = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (LobbyBoard b : boards.values()) b.updatePing(false);
        }, 0L, pingEvery).getTaskId();

        taskOnline = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (LobbyBoard b : boards.values()) b.updateOnline(false);
            // Rank changes are rare, but checking occasionally keeps it updated without listeners.
            for (LobbyBoard b : boards.values()) b.updateRankFromLuckPerms();
        }, 0L, onlineEvery).getTaskId();

        taskNametags = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (LobbyBoard b : boards.values()) b.syncNametagTeams();
        }, 0L, nametagsEvery).getTaskId();
    }

    public void stopTasks() {
        if (taskTitle != -1) Bukkit.getScheduler().cancelTask(taskTitle);
        if (taskPing != -1) Bukkit.getScheduler().cancelTask(taskPing);
        if (taskOnline != -1) Bukkit.getScheduler().cancelTask(taskOnline);
        if (taskNametags != -1) Bukkit.getScheduler().cancelTask(taskNametags);
        taskTitle = -1;
        taskPing = -1;
        taskOnline = -1;
        taskNametags = -1;
    }

    public void give(Player player) {
        boards.computeIfAbsent(player.getUniqueId(), id -> {
            LobbyBoard b = new LobbyBoard(plugin, luckPerms, player);
            // Initial rank update right away
            b.updateRankFromLuckPerms();
            b.syncNametagTeams();
            return b;
        });
    }

    public void remove(Player player) {
        LobbyBoard b = boards.remove(player.getUniqueId());
        if (b != null) b.destroy();
    }

    public void removeAll() {
        for (LobbyBoard b : boards.values()) b.destroy();
        boards.clear();
    }
}
