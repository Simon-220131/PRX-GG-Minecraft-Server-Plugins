package at.prx.pRXWorldCreator;

import at.prx.pRXWorldCreator.commands.WorldControlCommand;
import at.prx.pRXWorldCreator.manager.WorldControlManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PRXWorldCreator extends JavaPlugin {

    private WorldControlManager manager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        manager= new WorldControlManager(this);

        getCommand("worldcontrol").setExecutor(new WorldControlCommand(manager));
        getCommand("worldcontrol").setTabCompleter(new WorldControlCommand(manager));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
