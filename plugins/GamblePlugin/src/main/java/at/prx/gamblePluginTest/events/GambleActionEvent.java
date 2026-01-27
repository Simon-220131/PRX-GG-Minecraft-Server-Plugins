package at.prx.gamblePluginTest.events;

import at.prx.gamblePluginTest.states.SlotAction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GambleActionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SlotAction action;

    public GambleActionEvent(Player player, SlotAction action) {
        this.player = player;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public SlotAction getAction() {
        return action;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

