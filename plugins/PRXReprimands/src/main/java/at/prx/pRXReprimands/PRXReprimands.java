package at.prx.pRXReprimands;

import at.prx.pRXReprimands.command.BanCommand;
import at.prx.pRXReprimands.command.BanMuteCompleter;
import at.prx.pRXReprimands.command.KickCommand;
import at.prx.pRXReprimands.command.MuteCommand;
import at.prx.pRXReprimands.command.PlayerNameCompleter;
import at.prx.pRXReprimands.command.PunishmentNameCompleter;
import at.prx.pRXReprimands.command.UnbanCommand;
import at.prx.pRXReprimands.command.UnmuteCommand;
import at.prx.pRXReprimands.command.WarnsCommand;
import at.prx.pRXReprimands.command.WarnCommand;
import at.prx.pRXReprimands.command.ClearWarnsCommand;
import at.prx.pRXReprimands.command.RemoveWarnCommand;
import at.prx.pRXReprimands.command.RemoveWarnCompleter;
import at.prx.pRXReprimands.command.NoteCommand;
import at.prx.pRXReprimands.command.NotesCommand;
import at.prx.pRXReprimands.command.RemoveNoteCommand;
import at.prx.pRXReprimands.command.RemoveNoteCompleter;
import at.prx.pRXReprimands.command.HistoryCommand;
import at.prx.pRXReprimands.listener.ChatListener;
import at.prx.pRXReprimands.listener.JoinListener;
import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import at.prx.pRXReprimands.storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PRXReprimands extends JavaPlugin {

    private PunishmentManager punishmentManager;
    private ReprimandLogger reprimandLogger;
    private DatabaseManager databaseManager;
    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.init();
        } catch (Exception ex) {
            getLogger().severe("Database init failed: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.punishmentManager = new PunishmentManager(databaseManager);
        this.punishmentManager.load();
        this.reprimandLogger = new ReprimandLogger(this);

        int refreshSeconds = getConfig().getInt("database.refresh-interval-seconds", 30);
        if (refreshSeconds > 0) {
            long ticks = refreshSeconds * 20L;
            refreshTask = getServer().getScheduler().runTaskTimerAsynchronously(
                    this,
                    punishmentManager::refreshFromDatabase,
                    ticks,
                    ticks
            );
        }

        getServer().getPluginManager().registerEvents(new JoinListener(punishmentManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(punishmentManager), this);

        if (getCommand("ban") != null) {
            getCommand("ban").setExecutor(new BanCommand(punishmentManager, reprimandLogger));
            getCommand("ban").setTabCompleter(new BanMuteCompleter());
        }
        if (getCommand("unban") != null) {
            getCommand("unban").setExecutor(new UnbanCommand(punishmentManager, reprimandLogger));
            getCommand("unban").setTabCompleter(new PunishmentNameCompleter(punishmentManager::getBannedNames));
        }
        if (getCommand("mute") != null) {
            getCommand("mute").setExecutor(new MuteCommand(punishmentManager, reprimandLogger));
            getCommand("mute").setTabCompleter(new BanMuteCompleter());
        }
        if (getCommand("unmute") != null) {
            getCommand("unmute").setExecutor(new UnmuteCommand(punishmentManager, reprimandLogger));
            getCommand("unmute").setTabCompleter(new PunishmentNameCompleter(punishmentManager::getMutedNames));
        }
        if (getCommand("kick") != null) {
            getCommand("kick").setExecutor(new KickCommand(reprimandLogger));
            getCommand("kick").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("warn") != null) {
            getCommand("warn").setExecutor(new WarnCommand(punishmentManager, reprimandLogger, this));
            getCommand("warn").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("warns") != null) {
            getCommand("warns").setExecutor(new WarnsCommand(punishmentManager));
            getCommand("warns").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("clearwarns") != null) {
            getCommand("clearwarns").setExecutor(new ClearWarnsCommand(punishmentManager, reprimandLogger));
            getCommand("clearwarns").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("removewarn") != null) {
            getCommand("removewarn").setExecutor(new RemoveWarnCommand(punishmentManager, reprimandLogger));
            getCommand("removewarn").setTabCompleter(new RemoveWarnCompleter(punishmentManager));
        }
        if (getCommand("note") != null) {
            getCommand("note").setExecutor(new NoteCommand(punishmentManager, reprimandLogger));
            getCommand("note").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("notes") != null) {
            getCommand("notes").setExecutor(new NotesCommand(punishmentManager));
            getCommand("notes").setTabCompleter(new PlayerNameCompleter());
        }
        if (getCommand("removenote") != null) {
            getCommand("removenote").setExecutor(new RemoveNoteCommand(punishmentManager, reprimandLogger));
            getCommand("removenote").setTabCompleter(new RemoveNoteCompleter(punishmentManager));
        }
        if (getCommand("history") != null) {
            getCommand("history").setExecutor(new HistoryCommand(punishmentManager));
            getCommand("history").setTabCompleter(new PlayerNameCompleter());
        }
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.save();
        }
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
