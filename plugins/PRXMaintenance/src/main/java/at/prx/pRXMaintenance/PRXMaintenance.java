package at.prx.pRXMaintenance;

import at.prx.pRXMaintenance.commands.MaintenanceCommand;
import at.prx.pRXMaintenance.commands.ServerPingListener;
import at.prx.pRXMaintenance.listener.MaintenanceListener;
import at.prx.pRXMaintenance.whitelist.MaintenanceWhitelist;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;

public class PRXMaintenance extends JavaPlugin {

    private boolean maintenance;
    private MiniMessage miniMessage;
    private MaintenanceWhitelist maintenanceWhitelist;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maintenance = getConfig().getBoolean("maintenance.enabled");
        this.miniMessage = MiniMessage.miniMessage();

        maintenanceWhitelist = new MaintenanceWhitelist(this);
        maintenanceWhitelist.loadFromConfig(
                new HashSet<>(getConfig().getStringList("maintenance.whitelist.players"))
        );

        getCommand("maintenance").setExecutor(new MaintenanceCommand(this,  maintenanceWhitelist));
        getServer().getPluginManager().registerEvents(new MaintenanceListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerPingListener(this), this);
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public MiniMessage mm() {
        return miniMessage;
    }

    public MaintenanceWhitelist whitelist() {
        return maintenanceWhitelist;
    }

    public boolean isWhitelistEnabled() {
        return getConfig().getBoolean("maintenance.whitelist.enabled");
    }

    public void setWhitelistEnabled(boolean value) {
        getConfig().set("maintenance.whitelist.enabled", value);
        saveConfig();
    }

    public Component getKickComponent() {
        return mm().deserialize(
                Objects.requireNonNull(getConfig().getString("maintenance.kick-message"))
        );
    }

    public void notifyAdmins(Component message) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("maintenance.admin")) {
                p.sendMessage(message);
            }
        });
    }

    public void setNotifyEnabled(boolean value) {
        getConfig().set("maintenance.notify-admins", value);
        saveConfig();
    }

    public void setMaintenance(boolean value) {
        this.maintenance = value;
        getConfig().set("maintenance.enabled", value);
        saveConfig();
    }
}
