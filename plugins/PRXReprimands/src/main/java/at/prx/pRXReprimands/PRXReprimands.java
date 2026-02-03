package at.prx.pRXReprimands;

import at.prx.pRXReprimands.command.BanCommand;
import at.prx.pRXReprimands.command.BanMuteCompleter;
import at.prx.pRXReprimands.command.KickCommand;
import at.prx.pRXReprimands.command.MuteCommand;
import at.prx.pRXReprimands.command.PlayerNameCompleter;
import at.prx.pRXReprimands.command.PunishmentNameCompleter;
import at.prx.pRXReprimands.command.UnbanCommand;
import at.prx.pRXReprimands.command.UnmuteCommand;
import at.prx.pRXReprimands.listener.ChatListener;
import at.prx.pRXReprimands.listener.JoinListener;
import at.prx.pRXReprimands.logging.ReprimandLogger;
import at.prx.pRXReprimands.manager.PunishmentManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PRXReprimands extends JavaPlugin {

    private PunishmentManager punishmentManager;
    private ReprimandLogger reprimandLogger;

    @Override
    public void onEnable() {
        this.punishmentManager = new PunishmentManager(this);
        this.punishmentManager.load();
        this.reprimandLogger = new ReprimandLogger(this);

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
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.save();
        }
    }
}
