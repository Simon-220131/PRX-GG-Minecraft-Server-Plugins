package at.prx.ggnetwork.lobbyboard.listener;

import at.prx.ggnetwork.lobbyboard.board.LobbyBoardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinQuitListener implements Listener {

    private final LobbyBoardManager boardManager;

    public JoinQuitListener(LobbyBoardManager boardManager) {
        this.boardManager = boardManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        boardManager.give(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boardManager.remove(event.getPlayer());
    }
}
