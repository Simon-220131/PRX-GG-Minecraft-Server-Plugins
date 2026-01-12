package at.htlle.listener;

import at.htlle.duel.DuelManager;
import at.htlle.duel.DuelSession;
import at.htlle.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.UUID;

public class DuelListener implements Listener {

    private final DuelManager duelManager;

    public DuelListener(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    private final java.util.Map<org.bukkit.Location, UUID> tntBlockOwner = new java.util.HashMap<>();
    private static final boolean DEBUG = true;

    // projectile -> shooter mapping for reliable projectile-owner detection
    private final java.util.Map<UUID, UUID> projectileOwner = new java.util.concurrent.ConcurrentHashMap<>();

    // Cancel ANY non-PvP damage to duelists (mobs, fall, fire, etc.) if you want full isolation.
    @EventHandler
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;

        // Note: test duels are handled specially elsewhere; allow normal damage
        // handling here so self-inflicted hazards and TNT/crystal damage
        // can be registered and processed for test duels.

        UUID victimId = victim.getUniqueId();
        UUID other = session.other(victimId); // kann null sein, aber im real duel nicht

        // 1) Damage by entity
        if (e instanceof EntityDamageByEntityEvent edbe) {
            Player attacker = null;

            Entity damager = edbe.getDamager();
            if (damager instanceof Player p) attacker = p;
            else if (damager instanceof org.bukkit.entity.Projectile proj) {
                UUID projId = proj.getUniqueId();
                UUID ownerId = projectileOwner.get(projId);
                // try metadata first
                if (proj.hasMetadata("duelOwner")) {
                    try {
                        String s = proj.getMetadata("duelOwner").get(0).asString();
                        ownerId = UUID.fromString(s);
                    } catch (Throwable ignored) {}
                }
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Projectile hit: id=" + projId + " ownerMap=" + ownerId + " shooterAPI=" + proj.getShooter());

                // first try resolved ownerId
                if (ownerId != null) {
                    Player owner = Bukkit.getPlayer(ownerId);
                    if (owner != null) attacker = owner;
                }

                // fallback to projectile shooter API
                if (attacker == null && proj.getShooter() instanceof Player p2) attacker = p2;
            }

            // ✅ Normal PvP/Projectile
            if (attacker != null) {
                // allow if attacker is the other duelist OR the victim themself (self-damage)
                if ((other != null && attacker.getUniqueId().equals(other)) || attacker.getUniqueId().equals(victimId)) {
                    if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Allow entity damage: attacker=" + attacker.getName() + " victim=" + victim.getName());
                    return;
                }
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Block entity damage from non-duelist: attacker=" + attacker.getName() + " victim=" + victim.getName());
                edbe.setCancelled(true);
                return;
            }

            // ✅ TNTPrimed owner damage (self-damage allowed!)
            if (damager instanceof org.bukkit.entity.TNTPrimed) {
                UUID owner = duelManager.hazards().getTntOwner(damager);
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] TNT damage: victim=" + victim.getName() + " tntOwner=" + owner + " victimId=" + victimId);
                if (owner != null && (owner.equals(victimId) || (other != null && owner.equals(other)))) {
                    return; // allow
                }
                edbe.setCancelled(true);
                return;
            }

            // ✅ EnderCrystal explosion damage (self-damage allowed!)
            if (damager instanceof org.bukkit.entity.EnderCrystal) {
                UUID owner = duelManager.hazards().getCrystalOwner(damager);
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Crystal damage: victim=" + victim.getName() + " crystalOwner=" + owner + " victimId=" + victimId);
                if (owner != null && (owner.equals(victimId) || (other != null && owner.equals(other)))) {
                    return; // allow
                }
                edbe.setCancelled(true);
                return;
            }

            // mobs / unknown entity sources blocked
            edbe.setCancelled(true);
            return;
        }

        // 2) Environmental damage (lava/fire ticks etc.)
        switch (e.getCause()) {
            case LAVA:
            case FIRE:
            case FIRE_TICK: {
                // 1) credited check (if someone was credited for igniting this player allow ongoing ticks)
                UUID credited = duelManager.credit().getCreditedAttacker(victimId);
                if (credited != null && (credited.equals(victimId) || (other != null && credited.equals(other)))) {
                    return; // allow tick damage
                }

                // 2) wenn kein credited: versuche ownerNear zu finden und credit länger setzen
                UUID owner = null;

                if (e.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    owner = duelManager.hazards().getLavaOwnerNear(victim.getLocation());
                    if (owner != null) duelManager.credit().credit(victimId, owner, 30000); // credit longer so fire ticks persist
                } else {
                    owner = duelManager.hazards().getFireOwnerNear(victim.getLocation());
                    if (owner != null) duelManager.credit().credit(victimId, owner, 30000);
                }

                // Wenn kein Besitzer gefunden wurde -> natürliche Umwelt (nicht platziert): Schaden erlauben
                if (owner == null) {
                    return; // allow natural lava/fire damage
                }

                if (owner != null && (owner.equals(victimId) || (other != null && owner.equals(other)))) {
                    return; // allow owned by duel participant
                }

                e.setCancelled(true);
                return;
            }
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION: {
                UUID credited = duelManager.credit().getCreditedAttacker(victimId);
                if (credited != null && (credited.equals(victimId) || (other != null && credited.equals(other)))) {
                    return; // allow
                }
                e.setCancelled(true);
                return;
            }
            default: {
                // fall, drowning, etc. blocked (du willst isolation)
                e.setCancelled(true);
                return;
            }
        }
    }

    // Death interception / win handling
    @EventHandler
    public void onPotentialDeath(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;

        double finalDamage = e.getFinalDamage();
        double health = victim.getHealth();

        if (finalDamage < health) return; // not lethal

        Entity damager = e.getDamager();
        Player killer = null;
        // try to resolve player killer via direct player, projectile metadata/map or shooter API
        if (damager instanceof Player p) killer = p;
        else if (damager instanceof org.bukkit.entity.Projectile proj) {
            // try metadata
            try {
                if (proj.hasMetadata("duelOwner")) {
                    String s = proj.getMetadata("duelOwner").get(0).asString();
                    UUID uid = UUID.fromString(s);
                    Player pl = Bukkit.getPlayer(uid);
                    if (pl != null) killer = pl;
                }
            } catch (Throwable ignored) {}

            // fallback to tracked map
            if (killer == null) {
                UUID ownerId = projectileOwner.get(proj.getUniqueId());
                if (ownerId != null) {
                    Player pl = Bukkit.getPlayer(ownerId);
                    if (pl != null) killer = pl;
                }
            }

            // final fallback to shooter API
            if (killer == null && proj.getShooter() instanceof Player p2) killer = p2;
        }

        boolean deathMode = session.isDeathMode();

        if (killer == null) {
            // non-player lethal (TNT, crystals, etc.)
            if (deathMode) return; // allow death in death-mode

            // determine credited attacker if any
            UUID credited = duelManager.credit().getCreditedAttacker(victim.getUniqueId());
            UUID winnerId;
            if (credited != null) {
                if (credited.equals(victim.getUniqueId())) winnerId = session.other(victim.getUniqueId());
                else winnerId = credited;
            } else {
                // fallback: opponent wins when a duelist dies to environment
                winnerId = session.other(victim.getUniqueId());
            }

            // prevent actual server death, but end duel as death win for the winner
            e.setCancelled(true);
            victim.setHealth(1.0);

            duelManager.endSession(session, winnerId, DuelManager.EndReason.DEATH_WIN);
            return;
        }

        if (!deathMode) {
            // determine winner (attacker, or opponent if self-hit)
            UUID winnerId = killer.getUniqueId().equals(victim.getUniqueId()) ? session.other(victim.getUniqueId()) : killer.getUniqueId();

            e.setCancelled(true);
            Player p1 = session.p1() == null ? null : Bukkit.getPlayer(session.p1());
            Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
            if (p1 != null) {
                p1.setHealth(Math.max(1.0, Math.min(p1.getHealth(), 1.0)));
                p1.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
                var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
                if (resistType != null) p1.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
                p1.playSound(p1.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            }
            if (p2 != null) {
                p2.setHealth(Math.max(1.0, Math.min(p2.getHealth(), 1.0)));
                p2.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
                var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
                if (resistType != null) p2.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
                p2.playSound(p2.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            }

            duelManager.endSession(session, winnerId, DuelManager.EndReason.PREVENTED_DEATH);
            duelManager.removeBossBarForSession(session);
            return;
        } else {}
        }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLethalEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;

        // death-mode allows death
        if (session.isDeathMode()) return;

        double finalDamage = e.getFinalDamage();
        double health = victim.getHealth();
        if (finalDamage < health) return; // not lethal

        // resolve killer robustly
        Entity damager = e.getDamager();
        Player killer = null;
        if (damager instanceof Player p) killer = p;
        else if (damager instanceof org.bukkit.entity.Projectile proj) {
            try {
                if (proj.hasMetadata("duelOwner")) {
                    String s = proj.getMetadata("duelOwner").get(0).asString();
                    UUID uid = UUID.fromString(s);
                    Player pl = Bukkit.getPlayer(uid);
                    if (pl != null) killer = pl;
                }
            } catch (Throwable ignored) {}

            if (killer == null) {
                UUID ownerId = projectileOwner.get(proj.getUniqueId());
                if (ownerId != null) {
                    Player pl = Bukkit.getPlayer(ownerId);
                    if (pl != null) killer = pl;
                }
            }
            if (killer == null && proj.getShooter() instanceof Player p2) killer = p2;
        }

        // determine winner
        UUID winnerId = null;
        if (killer == null) {
            UUID credited = duelManager.credit().getCreditedAttacker(victim.getUniqueId());
            if (credited != null) {
                winnerId = credited.equals(victim.getUniqueId()) ? session.other(victim.getUniqueId()) : credited;
            }
        } else {
            winnerId = killer.getUniqueId().equals(victim.getUniqueId()) ? session.other(victim.getUniqueId()) : killer.getUniqueId();
        }

        // prevent death and end duel
        e.setCancelled(true);
        victim.setHealth(1.0);

        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
            if (p1 != null) {
            p1.setHealth(Math.max(1.0, Math.min(p1.getHealth(), 1.0)));
            p1.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
            if (resistType != null) p1.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
        }
        if (p2 != null) {
            p2.setHealth(Math.max(1.0, Math.min(p2.getHealth(), 1.0)));
            p2.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
            if (resistType != null) p2.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
        }

        duelManager.endSession(session, winnerId, DuelManager.EndReason.PREVENTED_DEATH);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
        Player p = e.getPlayer();

        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        if (!duelManager.isFrozen(p)) return;

        if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
            e.setTo(e.getFrom());
        }
    }


    @EventHandler
    public void onPotentialDeathAny(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;
        // In death-mode darf man sterben
        if (session.isDeathMode()) return;

        // Wichtig: wenn es EntityDamageByEntityEvent ist, macht das schon onPotentialDeath()
        if (e instanceof EntityDamageByEntityEvent) return;

        // lethal?
        if (e.getFinalDamage() < victim.getHealth()) return;

        // determine winner by credited attacker if any
        UUID credited = duelManager.credit().getCreditedAttacker(victim.getUniqueId());
        UUID winnerId = null;
        if (credited != null) {
            if (credited.equals(victim.getUniqueId())) winnerId = session.other(victim.getUniqueId());
            else winnerId = credited;
        }

        // prevent death, stop duel, regen both
        e.setCancelled(true);
        victim.setHealth(1.0);

        Player p1 = session.p1() == null ? null : Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        if (p1 != null) {
            p1.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
            if (resistType != null) p1.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
            p1.playSound(p1.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        }
        if (p2 != null) {
            p2.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
            if (resistType != null) p2.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
            p2.playSound(p2.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        }

        duelManager.endSession(session, winnerId, DuelManager.EndReason.PREVENTED_DEATH);
    }

    // Mobs should not target duelists
    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent e) {
        LivingEntity target = e.getTarget();
        if (!(target instanceof Player p)) return;

        if (duelManager.isInDuel(p)) {
            e.setCancelled(true);
            e.setTarget(null);
        }
    }

    // If you also want to block natural regen to keep fights consistent, optional:
    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!duelManager.isInDuel(p)) return;

        // optional: allow only potions/our forced regen, block others
        // e.setCancelled(true);
    }

    // Disconnect handling
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        duelManager.removeSpectator(e.getPlayer());
        Player quitter = e.getPlayer();
        duelManager.removeSpectator(quitter);
        DuelSession session = duelManager.getSession(quitter).orElse(null);
        if (session == null) return;

        UUID otherId = session.other(quitter.getUniqueId());
        duelManager.endSession(session, otherId, DuelManager.EndReason.DISCONNECT);

        Player other = otherId == null ? null : Bukkit.getPlayer(otherId);
        if (other != null) {
            other.sendMessage(Chat.ok("Dein Gegner hat verlassen. Du gewinnst das Duell."));
            other.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        DuelSession session = duelManager.getSession(dead).orElse(null);
        if (session == null) return;

        if (!session.isDeathMode()) {
            // sollte nie passieren, weil wir lethal hits canceln
            duelManager.endSession(session, null, DuelManager.EndReason.PREVENTED_DEATH);
            return;
        }

        UUID winnerId = session.other(dead.getUniqueId());
        duelManager.endSession(session, winnerId, DuelManager.EndReason.DEATH_WIN);

        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        if (winner != null) {
            winner.sendMessage(Chat.ok("Du hast das Duell gewonnen!"));
            winner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        } else {
            // winner is not a real player (dummy) -> inform the dead player about loss
            Player p1 = Bukkit.getPlayer(session.p1());
            if (p1 != null && p1.equals(dead)) {
                p1.sendMessage(Chat.err("Du hast das Test-Duell verloren."));
                p1.sendTitle(org.bukkit.ChatColor.RED + "Verloren!", org.bukkit.ChatColor.GRAY + "Dummy hat dich besiegt", 10, 70, 10);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent e) {
        // Dummy death -> end test duel
        var ent = e.getEntity();
        for (DuelSession s : new HashSet<>(duelManager.getAllSessions())) {
            if (s.isTestDuel() && s.dummy() != null && s.dummy().equals(ent.getUniqueId())) {
                duelManager.endSession(s, s.p1(), DuelManager.EndReason.DEATH_WIN);
                break;
            }
        }

        // Crystal cleanup (independent)
        if (e.getEntity() instanceof org.bukkit.entity.EnderCrystal) {
            duelManager.hazards().forgetCrystal(e.getEntity());
        }
    }

    @EventHandler
    public void onEntityExplodeGeneric(EntityExplodeEvent e) {
        // also handle EnderCrystal explosions so they don't destroy environment when owned by duel players
        if (e.getEntity() instanceof org.bukkit.entity.EnderCrystal crystal) {
            UUID owner = duelManager.hazards().getCrystalOwner(crystal);
            if (owner != null) {
                Player p = Bukkit.getPlayer(owner);
                if (p != null && duelManager.isInDuel(p)) {
                    e.blockList().clear();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnyDamageStrict(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;

        UUID victimId = victim.getUniqueId();
        UUID other = session.other(victimId);
        // If some other plugin or the server cancelled PvP (game rule), allow re-enabling
        // when this is a player-vs-player event between duel participants (so duels bypass gamerule).
        if (e.isCancelled() && e instanceof EntityDamageByEntityEvent edbeReenable) {
            Entity damager = edbeReenable.getDamager();
            Player attacker = null;
            if (damager instanceof Player p) attacker = p;
            else if (damager instanceof org.bukkit.entity.Projectile proj) {
                try {
                    if (proj.hasMetadata("duelOwner")) {
                        String s = proj.getMetadata("duelOwner").get(0).asString();
                        UUID uid = UUID.fromString(s);
                        Player pl = Bukkit.getPlayer(uid);
                        if (pl != null) attacker = pl;
                    }
                } catch (Throwable ignored) {}
                if (attacker == null) {
                    UUID ownerId = projectileOwner.get(((org.bukkit.entity.Projectile) damager).getUniqueId());
                    if (ownerId != null) attacker = Bukkit.getPlayer(ownerId);
                }
                if (attacker == null && ((org.bukkit.entity.Projectile) damager).getShooter() instanceof Player p2) attacker = p2;
            }
            if (attacker != null && ((other != null && attacker.getUniqueId().equals(other)) || attacker.getUniqueId().equals(victimId))) {
                e.setCancelled(false);
                return;
            }
            return;
        }

        // If damage is from an entity
        if (e instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();

            // resolve player attacker if possible
            Player attacker = null;
            if (damager instanceof Player p) attacker = p;
            else if (damager instanceof org.bukkit.entity.Projectile proj) {
                // try metadata
                try {
                    if (proj.hasMetadata("duelOwner")) {
                        String s = proj.getMetadata("duelOwner").get(0).asString();
                        UUID uid = UUID.fromString(s);
                        Player pl = Bukkit.getPlayer(uid);
                        if (pl != null) attacker = pl;
                    }
                } catch (Throwable ignored) {}

                // fallback to tracked map
                if (attacker == null) {
                    UUID ownerId = projectileOwner.get(proj.getUniqueId());
                    if (ownerId != null) {
                        Player pl = Bukkit.getPlayer(ownerId);
                        if (pl != null) attacker = pl;
                    }
                }

                // final fallback
                if (attacker == null && proj.getShooter() instanceof Player p2) attacker = p2;
            }

            if (attacker != null) {
                // allow only opponent or self
                if ((other != null && attacker.getUniqueId().equals(other)) || attacker.getUniqueId().equals(victimId)) {
                    return; // allowed
                }
                edbe.setCancelled(true);
                return;
            }

            // TNT
            if (damager instanceof org.bukkit.entity.TNTPrimed tnt) {
                UUID owner = duelManager.hazards().getTntOwner(tnt);
                if (owner != null) {
                    Player ownerP = Bukkit.getPlayer(owner);
                    DuelSession ownerSess = ownerP == null ? null : duelManager.getSession(ownerP).orElse(null);
                    if (ownerSess != null && ownerSess.equals(session)) {
                        if (owner.equals(victimId) || (other != null && owner.equals(other))) {
                            return; // allowed within same duel
                        }
                        edbe.setCancelled(true);
                        return;
                    }
                    // owner exists but not in same session -> ignore mapping; allow
                }
            }

            // EnderCrystal
            if (damager instanceof org.bukkit.entity.EnderCrystal crystal) {
                UUID owner = duelManager.hazards().getCrystalOwner(crystal);
                if (owner != null) {
                    Player ownerP = Bukkit.getPlayer(owner);
                    DuelSession ownerSess = ownerP == null ? null : duelManager.getSession(ownerP).orElse(null);
                    if (ownerSess != null && ownerSess.equals(session)) {
                        if (owner.equals(victimId) || (other != null && owner.equals(other))) {
                            return; // allowed within same duel
                        }
                        edbe.setCancelled(true);
                        return;
                    }
                    // owner exists but not in same session -> ignore mapping; allow
                }
            }

            // other entity sources blocked
            edbe.setCancelled(true);
            return;
        }

        // Environmental causes: allow credited, allow duel-owned, and allow natural (no owner)
        switch (e.getCause()) {
            case LAVA:
            case FIRE:
            case FIRE_TICK: {
                UUID credited = duelManager.credit().getCreditedAttacker(victimId);
                if (credited != null && (credited.equals(victimId) || (other != null && credited.equals(other)))) return;

                UUID owner = null;
                if (e.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    owner = duelManager.hazards().getLavaOwnerNear(victim.getLocation());
                } else {
                    owner = duelManager.hazards().getFireOwnerNear(victim.getLocation());
                }

                // Wenn kein Besitzer gefunden wurde -> natürliche Umwelt: Schaden erlauben
                if (owner == null) return;

                if (owner != null && (owner.equals(victimId) || (other != null && owner.equals(other)))) return;
                e.setCancelled(true);
                return;
            }
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION: {
                UUID credited = duelManager.credit().getCreditedAttacker(victimId);
                if (credited != null && (credited.equals(victimId) || (other != null && credited.equals(other)))) return;
                e.setCancelled(true);
                return;
            }
            default: {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent e) {
        // prevent fire spreading from duel-placed fire
        org.bukkit.block.Block src = e.getSource();
        if (src.getType() != org.bukkit.Material.FIRE) return;
        UUID owner = duelManager.hazards().getFireOwnerNear(src.getLocation());
        if (owner != null) {
            Player p = Bukkit.getPlayer(owner);
            if (p != null && duelManager.isInDuel(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent e) {
        // prevent blocks from burning if the fire source was placed by a duel player
        UUID owner = duelManager.hazards().getFireOwnerNear(e.getBlock().getLocation());
        if (owner != null) {
            Player p = Bukkit.getPlayer(owner);
            if (p != null && duelManager.isInDuel(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageDummy(EntityDamageByEntityEvent e) {
        // Victim must be the dummy
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        // Resolve attacker player
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

        if (attacker == null) return;

        DuelSession session = duelManager.getSession(attacker).orElse(null);
        if (session == null || !session.isTestDuel() || session.dummy() == null) return;

        // Only allow hitting OUR dummy
        if (!victim.getUniqueId().equals(session.dummy())) {
            e.setCancelled(true);
            return;
        }

        // ✅ allow damage (do NOT cancel)
    }

    @EventHandler
    public void onCrystalHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.EnderCrystal crystal)) return;

        DuelSession s = duelManager.getSession(attacker).orElse(null);
        if (s == null) return;

        // Nur wenn attacker wirklich im Duell ist
        duelManager.hazards().setCrystalOwner(crystal, attacker.getUniqueId());
    }

    @EventHandler
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        if (e.getBucket() != org.bukkit.Material.LAVA_BUCKET) return;

        // Der Block, in den entleert wird (relative)
        org.bukkit.block.Block placed = e.getBlockClicked().getRelative(e.getBlockFace());
        duelManager.hazards().setLavaOwner(placed, p.getUniqueId());
        // record placed block ownership
        duelManager.hazards().setPlacedBlock(placed, p.getUniqueId());
        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Lava placed by " + p.getName());
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        Player p = e.getPlayer();
        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        // track placed blocks so the player can break them later during duel
        duelManager.hazards().setPlacedBlock(e.getBlockPlaced(), p.getUniqueId());

        // If placing lava/fire via block place, also tag as lava/fire owner
        if (e.getBlockPlaced().getType() == org.bukkit.Material.LAVA) {
            duelManager.hazards().setLavaOwner(e.getBlockPlaced(), p.getUniqueId());
        } else if (e.getBlockPlaced().getType() == org.bukkit.Material.FIRE) {
            duelManager.hazards().setFireOwner(e.getBlockPlaced(), p.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player p = e.getPlayer();
        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return; // not in duel -> normal behaviour

        UUID owner = duelManager.hazards().getPlacedBlockOwner(e.getBlock());
        if (owner == null || !owner.equals(p.getUniqueId())) {
            // not placed by this player -> prevent breaking
            e.setCancelled(true);
            p.sendMessage(Chat.err("Du kannst in einem Duell nur deine eigenen platzierten Blöcke abbauen."));
            return;
        }

        // allowed: remove tracking
        duelManager.hazards().removePlacedBlock(e.getBlock());
    }

    @EventHandler
    public void onLavaPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        Player p = e.getPlayer();
        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        if (e.getBlockPlaced().getType() != org.bukkit.Material.LAVA) return;
        duelManager.hazards().setLavaOwner(e.getBlockPlaced(), p.getUniqueId());
        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Lava block placed by " + p.getName());
    }

    @EventHandler
    public void onLavaFlow(org.bukkit.event.block.BlockFromToEvent e) {
        // nur Lava
        if (e.getBlock().getType() != org.bukkit.Material.LAVA) return;

        // Owner von der Quelle/aktuellen Lava holen
        UUID owner = duelManager.hazards().getLavaOwner(e.getBlock());
        if (owner == null) return;

        // Owner auf den Ziel-Block übertragen (fließende Lava)
        duelManager.hazards().setLavaOwner(e.getToBlock(), owner);
    }

    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        DuelSession s = duelManager.getSession(victim).orElse(null);
        if (s == null) return;

        UUID victimId = victim.getUniqueId();
        UUID other = s.other(victimId);

        UUID fireOwner = duelManager.hazards().getFireOwnerNear(victim.getLocation());
        UUID lavaOwner = duelManager.hazards().getLavaOwnerNear(victim.getLocation());

        UUID owner = (fireOwner != null) ? fireOwner : lavaOwner;
        if (owner != null && (owner.equals(victimId) || (other != null && owner.equals(other)))) {
            duelManager.credit().credit(victimId, owner, 30000); // 30s brenn-tag
            if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Combust credited owner=" + owner + " for " + victim.getName());
            return;
        }

        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Combust blocked for " + victim.getName());
        e.setCancelled(true); // kein duell-owner -> block
    }

    @EventHandler
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent e) {
        // Prevent fire creation from duel-placed fire or duel-placed lava
        org.bukkit.event.block.BlockIgniteEvent.IgniteCause cause = e.getCause();
        org.bukkit.block.Block target = e.getBlock();
        org.bukkit.Location loc = target.getLocation();

        // find any nearby owner from fire, lava or placed blocks
        UUID fireOwner = duelManager.hazards().getFireOwnerNear(loc);
        UUID lavaOwner = duelManager.hazards().getLavaOwnerNear(loc);
        UUID placedOwner = duelManager.hazards().getPlacedBlockOwnerNear(loc);

        UUID owner = null;
        if (fireOwner != null) owner = fireOwner;
        else if (lavaOwner != null) owner = lavaOwner;
        else if (placedOwner != null) owner = placedOwner;

        if (owner != null) {
            Player p = Bukkit.getPlayer(owner);
            if (p != null && duelManager.isInDuel(p)) {
                // cancel creation of fire if it's caused by lava or spread from duel-placed fire
                e.setCancelled(true);
                return;
            }
        }

        // otherwise allow (player ignites will be handled elsewhere)
    }

    @EventHandler
    public void onTntPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        Player p = e.getPlayer();
        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        if (e.getBlockPlaced().getType() != org.bukkit.Material.TNT) return;

        org.bukkit.Location key = e.getBlockPlaced().getLocation().toBlockLocation();
        tntBlockOwner.put(key, p.getUniqueId());
        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] TNT block placed by " + p.getName() + " at " + key);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.TNTPrimed tnt) {
            Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> {
                duelManager.hazards().forgetTnt(tnt);
            }, 1L);
        }
    }

    @EventHandler
    public void onTntExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.TNTPrimed tnt)) return;

        UUID owner = duelManager.hazards().getTntOwner(tnt);
        if (owner == null) return;

        // alle Player im Radius 8 taggen (für DAMAGE Event danach)
        for (Entity ent : tnt.getNearbyEntities(8, 8, 8)) {
            if (!(ent instanceof Player victim)) continue;

            DuelSession s = duelManager.getSession(victim).orElse(null);
                if (s == null) continue;

            UUID victimId = victim.getUniqueId();
            UUID other = s.other(victimId);

            // erlauben nur wenn owner = victim oder gegner
            if (owner.equals(victimId) || (other != null && owner.equals(other))) {
                // tagge Explosion Owner für 3 Sekunden
                duelManager.credit().credit(victimId, owner, 3000);
            }
        }

        // TNT Owner erst 1 Tick später löschen
        Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> duelManager.hazards().forgetTnt(tnt), 1L);
        // prevent TNT from damaging blocks when placed by a duel player
        if (owner != null) {
            e.blockList().clear();
        }
    }

    @EventHandler
    public void onEntitySpawn(org.bukkit.event.entity.EntitySpawnEvent e) {
        // TNT primed spawn: map owner from recently placed TNT block
        if (e.getEntity() instanceof org.bukkit.entity.TNTPrimed tnt) {
            // finde nahe TNT-Block-Owner (Radius 2)
            org.bukkit.Location l = tnt.getLocation().toBlockLocation();
            UUID owner = null;

            for (var it = tntBlockOwner.entrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                if (!entry.getKey().getWorld().equals(l.getWorld())) continue;

                if (entry.getKey().distanceSquared(l) <= 4.0) { // <= 2 blocks
                    owner = entry.getValue();
                    it.remove();
                    break;
                }
            }

            if (owner != null) {
                duelManager.hazards().setTntOwner(tnt, owner);
            }
            return;
        }

        // EnderCrystal spawn: if placed by a duel player, try to attribute owner (nearest duel player)
        if (e.getEntity() instanceof org.bukkit.entity.EnderCrystal crystal) {
            org.bukkit.Location loc = crystal.getLocation();
            Player nearest = null;
            double best = Double.MAX_VALUE;
            for (Player p : Bukkit.getOnlinePlayers()) {
                DuelSession s = duelManager.getSession(p).orElse(null);
                if (s == null) continue;
                double d = p.getLocation().distanceSquared(loc);
                if (d < best && d <= 9.0) { // within 3 blocks
                    best = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                duelManager.hazards().setCrystalOwner(crystal, nearest.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(org.bukkit.event.entity.ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) return;

        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        UUID projId = e.getEntity().getUniqueId();
        projectileOwner.put(projId, p.getUniqueId());
        // cleanup after 30s
        Bukkit.getScheduler().runTaskLater(duelManager.getPlugin(), () -> projectileOwner.remove(projId), 20L * 30);
        // also set metadata on the projectile so owner info survives across contexts
        try {
            e.getEntity().setMetadata("duelOwner", new org.bukkit.metadata.FixedMetadataValue(duelManager.getPlugin(), p.getUniqueId().toString()));
        } catch (Throwable ignored) {}
        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Projectile launched by " + p.getName() + " id=" + projId);
    }

    @EventHandler
    public void onIgniteTnt(org.bukkit.event.block.BlockIgniteEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getBlock().getType() != org.bukkit.Material.TNT) return;

        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        org.bukkit.Location key = e.getBlock().getLocation().toBlockLocation();
        tntBlockOwner.put(key, p.getUniqueId());
    }

    @EventHandler
    public void onIgnite(org.bukkit.event.block.BlockIgniteEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        DuelSession s = duelManager.getSession(p).orElse(null);
        if (s == null) return;

        // Feuerblock (da wo es brennt)
        duelManager.hazards().setFireOwner(e.getBlock(), p.getUniqueId());
        if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Fire ignited by " + p.getName());
    }

    @EventHandler
    public void onDummyLethal(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        // attacker player?
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
        if (attacker == null) return;

        DuelSession session = duelManager.getSession(attacker).orElse(null);
        if (session == null || !session.isTestDuel() || session.dummy() == null) return;

        if (!victim.getUniqueId().equals(session.dummy())) return;

        // lethal?
        if (e.getFinalDamage() < victim.getHealth()) return;

        if (!session.isDeathMode()) {
            // prevent dummy death -> end duel "prevented death" style
            e.setCancelled(true);
            victim.setHealth(1.0);

            attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3, true, true, true));
            var resistType = PotionEffectType.getByName("DAMAGE_RESISTANCE");
            if (resistType != null) attacker.addPotionEffect(new PotionEffect(resistType, 20 * 10, 4, true, true, true));
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

            duelManager.endSession(session, attacker.getUniqueId(), DuelManager.EndReason.PREVENTED_DEATH);
        } else {
            // death mode -> allow death, payout in EntityDeathEvent or watchdog
            attacker.sendMessage(Chat.ok("Death-Mode: Dummy geht gleich down -> du gewinnst."));
            // don't cancel
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileDamageOverride(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof org.bukkit.entity.Projectile proj)) return;

        // resolve shooter id from metadata/map/shooter API
        UUID shooterId = null;
        if (proj.hasMetadata("duelOwner")) {
            try {
                String s = proj.getMetadata("duelOwner").get(0).asString();
                shooterId = UUID.fromString(s);
            } catch (Throwable ignored) {}
        }
        if (shooterId == null) shooterId = projectileOwner.get(proj.getUniqueId());
        if (shooterId == null && proj.getShooter() instanceof Player p) shooterId = p.getUniqueId();

        if (shooterId != null && shooterId.equals(victim.getUniqueId())) {
            double finalDamage = e.getFinalDamage();
            double health = victim.getHealth();
            if (finalDamage < health) {
                // non-lethal: allow
                e.setCancelled(false);
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Allowing non-lethal self-projectile: proj=" + proj.getUniqueId() + " player=" + victim.getName());
            } else {
                // lethal: do NOT override cancellation, let onPotentialDeath handle duel end
                if (DEBUG) duelManager.getPlugin().getLogger().info("[Duell-debug] Not forcing lethal self-projectile: proj=" + proj.getUniqueId() + " player=" + victim.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorBreak(org.bukkit.event.block.BlockBreakEvent e) {
        if (duelManager.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Chat.err("Zuschauer dürfen keine Blöcke abbauen."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpectatorPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (duelManager.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Chat.err("Zuschauer dürfen keine Blöcke setzen."));
        }
    }

    // Update bossbar on any damage to reflect opponent HP
    @EventHandler
    public void onDamageUpdate(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        DuelSession session = duelManager.getSession(victim).orElse(null);
        if (session == null) return;
        duelManager.updateBossBar(session);
    }
}