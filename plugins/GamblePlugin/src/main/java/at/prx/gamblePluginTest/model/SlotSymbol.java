package at.prx.gamblePluginTest.model;

import at.prx.gamblePluginTest.states.Outcome;
import at.prx.gamblePluginTest.states.SlotAction;
import org.bukkit.inventory.ItemStack;

public class SlotSymbol {

    private final String id;
    private final ItemStack displayItem;
    private final int weight;
    private final SlotAction action;
    private final Outcome outcome;
    private final String message;
    private final int poisonSeconds;
    private final boolean creeperPlayerDamage;

    public SlotSymbol(
            String id,
            ItemStack displayItem,
            int weight,
            SlotAction action,
            Outcome outcome,
            String message,
            int poisonSeconds,
            boolean creeperPlayerDamage
    ) {
        this.id = id;
        this.displayItem = displayItem;
        this.weight = weight;
        this.action = action;
        this.outcome = outcome;
        this.message = message;
        this.poisonSeconds = poisonSeconds;
        this.creeperPlayerDamage = creeperPlayerDamage;
    }

    public ItemStack getDisplayItem() {
        return displayItem.clone();
    }

    public int getWeight() {
        return weight;
    }

    public SlotAction getAction() {
        return action;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getMessage() {
        return message;
    }

    public int getPoisonSeconds() {
        return poisonSeconds;
    }

    public String getId() {
        return id;
    }

    public boolean isCreeperPlayerDamage() {
        return creeperPlayerDamage;
    }

}
