package at.prx.gamblePluginTest;

import at.prx.gamblePluginTest.commands.CasinoCommand;
import at.prx.gamblePluginTest.gui.SlotMachineGUI;
import at.prx.gamblePluginTest.listener.OnPlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GamblePluginTest extends JavaPlugin {

    private SlotMachineGUI slotMachineGUI;

    @Override
    public void onEnable() {
        // Plugin startup logic

        saveDefaultConfig();

        slotMachineGUI = new SlotMachineGUI(this);

        getCommand("casino").setExecutor(new CasinoCommand(slotMachineGUI));
        getServer().getPluginManager().registerEvents(slotMachineGUI, this);

        getServer().getPluginManager().registerEvents(new OnPlayerJoinListener(this), this);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
