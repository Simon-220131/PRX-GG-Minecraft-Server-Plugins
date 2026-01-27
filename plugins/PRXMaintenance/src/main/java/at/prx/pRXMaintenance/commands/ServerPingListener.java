package at.prx.pRXMaintenance.commands;

import at.prx.pRXMaintenance.PRXMaintenance;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

public class ServerPingListener implements Listener {

    private final PRXMaintenance plugin;
    private final LegacyComponentSerializer legacy =
            LegacyComponentSerializer.legacySection();

    public ServerPingListener(PRXMaintenance plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {

        if (!plugin.isMaintenance()) return;

        // MOTD
        Component motdComponent = plugin.mm()
                .deserialize(plugin.getConfig().getString("maintenance.motd"));

        event.setMotd(legacy.serialize(motdComponent));
        event.setMaxPlayers(0);

        Component versionComponent = plugin.mm()
                .deserialize(plugin.getConfig().getString("maintenance.version"));

        event.setProtocolVersion(-1);
        event.setVersion(legacy.serialize(versionComponent));

        List<PaperServerListPingEvent.ListedPlayerInfo> sample = event.getListedPlayers();
        sample.clear();

        for (String line : plugin.getConfig().getStringList("maintenance.sample")) {
            Component lineComponent = plugin.mm().deserialize(line);
            sample.add(new PaperServerListPingEvent.ListedPlayerInfo(
                    legacy.serialize(lineComponent),
                    UUID.randomUUID()
                        ));
        }
    }
}
