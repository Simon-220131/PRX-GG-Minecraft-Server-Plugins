package at.prx.gamblePluginTest.executor;

import at.prx.gamblePluginTest.events.GambleActionEvent;
import at.prx.gamblePluginTest.model.SlotSymbol;
import at.prx.gamblePluginTest.states.SlotAction;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public class SlotActionExecutor {

    private final JavaPlugin plugin;

    public SlotActionExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    private final MiniMessage miniMessage = MiniMessage.miniMessage();


    public boolean execute(Player player, SlotSymbol symbol) {

        sendSymbolMessage(player, symbol);
        logResult(player, symbol);

        switch (symbol.getAction()) {

            case FULL_HEAL -> {
                player.setHealth(player.getMaxHealth());
            }

            case POISON -> {
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.POISON,
                                symbol.getPoisonSeconds() * 20,
                                0
                        )
                );
            }

            case SPAWN_CREEPER -> {
                spawnCreeper(player, symbol);
                Bukkit.getPluginManager().callEvent(
                        new GambleActionEvent(player, symbol.getAction())
                );
            }


            case INSTANT_BAN, BAN_COUNTER_PLUS, BAN_COUNTER_MINUS, BAN_SHIELD -> {
                Bukkit.getPluginManager().callEvent(
                        new GambleActionEvent(player, symbol.getAction())
                );
            }

            case FORCE_RESPIN -> {
                return true; // 👈 sagt GUI: nochmal drehen
            }

        }

        return false;
    }

    private void sendSymbolMessage(Player player, SlotSymbol symbol) {
        String msg = symbol.getMessage();
        if (msg == null || msg.isBlank()) return;

        player.sendMessage(miniMessage.deserialize(msg));
    }

    private void logResult(Player player, SlotSymbol symbol) {
        String outcomeText = switch (symbol.getOutcome()) {
            case WIN -> "GEWONNEN";
            case LOSS -> "VERLOREN";
            case NEUTRAL -> "NEUTRAL";
        };

        plugin.getLogger().info(
                "[Casino] " + player.getName()
                        + " hat " + outcomeText
                        + ": " + symbol.getAction()
                        + " (" + symbol.getId()
                        + " - " + symbol.getWeight() + ")"
        );
    }

    private void spawnCreeper(Player player, SlotSymbol symbol) {
        Location loc = player.getLocation();

        Creeper creeper = (Creeper) loc.getWorld().spawnEntity(
                loc,
                EntityType.CREEPER
        );

        creeper.setTarget(player);
        creeper.setPowered(false); // kein Charged Creeper
        creeper.setExplosionRadius(3);

        // 🔥 WICHTIG: Blockschaden verhindern
        if (!symbol.isCreeperPlayerDamage()) {
            creeper.setExplosionRadius(0);
        }

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_CREEPER_PRIMED,
                1f,
                1f
        );
    }


}

