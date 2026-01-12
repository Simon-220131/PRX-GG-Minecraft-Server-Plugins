package at.htlle.listener;

import at.htlle.duel.DuelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DuelRulesListener implements Listener {
    private final DuelManager manager;

    public DuelRulesListener(DuelManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // Wir interessieren uns primär für Player vs Player
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj
                && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        // Zuschauer dürfen NIE kämpfen
        if (manager.isSpectator(attacker) || manager.isSpectator(victim)) {
            e.setCancelled(true);
            return;
        }

        // PvP nur wenn beide im selben Duel sind
        var sAtt = manager.getSession(attacker).orElse(null);
        var sVic = manager.getSession(victim).orElse(null);

        if (sAtt == null || sVic == null) {
            e.setCancelled(true);
            return;
        }

        // nicht im selben duel => kein pvp
        if (sAtt != sVic) {
            e.setCancelled(true);
            return;
        }

        // TestDuel: PvP gegen Spieler soll aus bleiben
        if (sAtt.isTestDuel()) {
            e.setCancelled(true);
        }
    }
}