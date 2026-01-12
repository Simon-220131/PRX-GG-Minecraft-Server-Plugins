package at.htlle.duel;

import at.htlle.DuelPlugin;
import at.htlle.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.concurrent.ConcurrentHashMap;

import java.util.*;
import org.bukkit.World;
import org.bukkit.Location;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

public class DuelManager {

    private final DuelPlugin plugin;

    private final DamageCredit damageCredit = new DamageCredit();

    private final HazardTracker hazards = new HazardTracker();

    private final StatsManager statsManager;

    private final SpawnManager spawns;

    // key = invited player's UUID, value = invite
    private final Map<UUID, DuelInvite> pendingInvites = new HashMap<>();

    private final Map<UUID, Location> returnLocation = new HashMap<>();

    // key = player UUID, value = active session
    private final Map<UUID, DuelSession> activeSessions = new HashMap<>();

    private final Set<UUID> spectators = new HashSet<>();

    // countdown bossbars keyed by player uuid (so each duelist sees their own bar)
    private final Map<UUID, BossBar> countdownBars = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> countdownTaskIds = new ConcurrentHashMap<>();

    public boolean isSpectator(Player p) {
        return spectators.contains(p.getUniqueId());
    }

    public void addSpectator(Player p) {
        spectators.add(p.getUniqueId());
    }

    public void removeSpectator(Player p) {
        spectators.remove(p.getUniqueId());
    }

    // duelist uuid -> freeze-until timestamp (ms)
    private final Map<UUID, Long> frozenUntil = new ConcurrentHashMap<>();

    public void freezeSession(DuelSession session, long ms) {
        long until = System.currentTimeMillis() + ms;
        frozenUntil.put(session.p1(), until);
        if (session.p2() != null) frozenUntil.put(session.p2(), until);
    }

    public boolean isFrozen(Player p) {
        Long until = frozenUntil.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    public void clearFreeze(DuelSession session) {
        frozenUntil.remove(session.p1());
        if (session.p2() != null) frozenUntil.remove(session.p2());
    }

    public DuelManager(DuelPlugin plugin, SpawnManager spawns) {
        this.plugin = plugin;
        this.spawns = spawns;
        this.statsManager = new StatsManager(plugin);
    }

    public SpawnManager spawns() { return spawns; }


    public Location randomSpectatorLocation(World w) {
        return spawns.getRandomSpectatorSpawn(w);
    }

    public Location[] randomTwoDuelSpawns(World w) {
        return spawns.getTwoRandomDuelSpawns(w);
    }

    // watcher -> return location (where they stood before /duel watch)
    private final Map<UUID, Location> spectatorReturn = new HashMap<>();

    // optional: watcher -> watched duelist (or session p1 uuid)
    private final Map<UUID, UUID> spectatorTarget = new HashMap<>();

    public void startWatching(Player watcher, Player targetDuelist) {
        // store last location only once (don’t overwrite if already watching)
        spectatorReturn.putIfAbsent(watcher.getUniqueId(), watcher.getLocation().clone());
        spectators.add(watcher.getUniqueId());
        spectatorTarget.put(watcher.getUniqueId(), targetDuelist.getUniqueId());
    }

    public void stopWatching(Player watcher) {
        UUID wid = watcher.getUniqueId();
        spectators.remove(wid);
        spectatorTarget.remove(wid);

        Location back = spectatorReturn.remove(wid);
        if (back != null) {
            watcher.teleport(back);
        }
    }

    public void stopWatching(UUID watcherId) {
        Player watcher = Bukkit.getPlayer(watcherId);
        if (watcher != null) stopWatching(watcher);
        else {
            spectators.remove(watcherId);
            spectatorTarget.remove(watcherId);
            spectatorReturn.remove(watcherId);
        }
    }

    public List<UUID> getWatchersOf(Player duelPlayer) {
        UUID tid = duelPlayer.getUniqueId();
        List<UUID> out = new ArrayList<>();
        for (var e : spectatorTarget.entrySet()) {
            if (e.getValue().equals(tid)) out.add(e.getKey());
        }
        return out;
    }

    // Convenience: when duel ends, remove all watchers that watched either player
    public void stopWatchersOfSession(DuelSession session) {
        if (session == null) return;
        Set<UUID> duelists = new HashSet<>();
        duelists.add(session.p1());
        if (session.p2() != null) duelists.add(session.p2());

        List<UUID> toStop = new ArrayList<>();
        for (var e : spectatorTarget.entrySet()) {
            if (duelists.contains(e.getValue())) {
                toStop.add(e.getKey());
            }
        }
        for (UUID wid : toStop) stopWatching(wid);
    }

    // PvP override tracking per world: remember original state and a reference count
    private final Map<World, Boolean> originalPvp = new HashMap<>();
    private final Map<World, Integer> pvpRefCount = new HashMap<>();

    private void enablePvpForSession(DuelSession session) {
        if (session == null || session.isTestDuel()) return;
        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
        if (p1 == null || p2 == null) return;
        World w = p1.getWorld();
        if (!w.equals(p2.getWorld())) return; // different worlds, skip

        synchronized (pvpRefCount) {
            if (!originalPvp.containsKey(w)) {
                originalPvp.put(w, w.getPVP());
            }
            int cnt = pvpRefCount.getOrDefault(w, 0) + 1;
            pvpRefCount.put(w, cnt);
            if (!w.getPVP()) {
                w.setPVP(true);
                plugin.getLogger().info("DuelManager: enabled PVP in world " + w.getName() + " for duel between " + p1.getName() + " and " + p2.getName());
            }
        }
    }

    private void disablePvpForSession(DuelSession session) {
        if (session == null || session.isTestDuel()) return;
        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
        if (p1 == null || p2 == null) return;
        World w = p1.getWorld();
        if (!w.equals(p2.getWorld())) return;

        synchronized (pvpRefCount) {
            int cnt = pvpRefCount.getOrDefault(w, 0) - 1;
            if (cnt <= 0) {
                pvpRefCount.remove(w);
                Boolean orig = originalPvp.remove(w);
                if (orig != null) {
                    w.setPVP(orig);
                    plugin.getLogger().info("DuelManager: restored PVP in world " + w.getName() + " to " + orig);
                }
            } else {
                pvpRefCount.put(w, cnt);
            }
        }
    }

    // bossbars keyed by session p1 UUID
    private final java.util.Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    // scheduled task ids for bossbar updaters
    private final java.util.Map<UUID, Integer> bossBarTaskIds = new ConcurrentHashMap<>();

    public void createBossBarForSession(DuelSession session) {
        if (session == null) return;
        UUID p1 = session.p1();
        if (bossBars.containsKey(p1)) return;

        // Create separate bossbars per player so each can see the opponent's HP correctly
        Player player1 = Bukkit.getPlayer(session.p1());
        Player player2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        if (player1 != null) {
            BossBar bar1 = Bukkit.createBossBar("Duell: --", BarColor.WHITE, BarStyle.SOLID);
            bar1.addPlayer(player1);
            bossBars.put(session.p1(), bar1);
        }
        if (session.p2() != null && player2 != null) {
            BossBar bar2 = Bukkit.createBossBar("Duell: --", BarColor.WHITE, BarStyle.SOLID);
            bar2.addPlayer(player2);
            bossBars.put(session.p2(), bar2);
        }

        // initial update
        updateBossBar(session);

        // schedule repeating update task (every tick) and keep id so we can cancel later
        var sess = session;
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // if session no longer active, cancel
            boolean active = activeSessions.containsKey(sess.p1()) || (sess.p2() != null && activeSessions.containsKey(sess.p2()));
            if (!active) {
                removeBossBarForSession(sess);
                return;
            }
            updateBossBar(sess);
        }, 1L, 1L);
        if (player1 != null) bossBarTaskIds.put(session.p1(), task.getTaskId());
        if (session.p2() != null && player2 != null) bossBarTaskIds.put(session.p2(), task.getTaskId());
    }

    public void updateBossBar(DuelSession session) {
        if (session == null) return;
        // update each player's bossbar separately so they see the opponent's health
        if (session.p1() != null) {
            BossBar bar1 = bossBars.get(session.p1());
            Player p1 = Bukkit.getPlayer(session.p1());
            if (bar1 != null && p1 != null) {
                double health = 0.0;
                double max = 1.0;
                String name = "--";
                if (session.isTestDuel()) {
                    var ent = Bukkit.getEntity(session.dummy());
                    if (ent instanceof org.bukkit.entity.LivingEntity le) {
                        health = le.getHealth();
                        var attr = le.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (attr != null) max = attr.getValue();
                        name = le.getCustomName() == null ? "Duel Dummy" : le.getCustomName();
                    }
                } else {
                    Player opponent = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
                    if (opponent != null) {
                        health = opponent.getHealth();
                        var attr = opponent.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (attr != null) max = attr.getValue();
                        name = opponent.getName();
                    }
                }
                double progress = 0.0;
                if (max > 0) progress = Math.max(0.0, Math.min(1.0, health / max));
                bar1.setProgress(progress);
                bar1.setTitle(at.htlle.util.Chat.opponent(name) + (progress > 0 ? " - " + String.format("%.1f", health) + "❤" : ""));
                if (!bar1.getPlayers().contains(p1)) bar1.addPlayer(p1);
            }
        }

        if (session.p2() != null) {
            BossBar bar2 = bossBars.get(session.p2());
            Player p2 = Bukkit.getPlayer(session.p2());
            if (bar2 != null && p2 != null) {
                double health = 0.0;
                double max = 1.0;
                String name = "--";
                // opponent for p2 is p1 (or dummy if test duel and p1 is dummy)
                if (session.isTestDuel()) {
                    // p2 shouldn't happen in test duel, but handle gracefully
                    Player opponent = Bukkit.getPlayer(session.p1());
                    if (opponent != null) {
                        health = opponent.getHealth();
                        var attr = opponent.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (attr != null) max = attr.getValue();
                        name = opponent.getName();
                    }
                } else {
                    Player opponent = Bukkit.getPlayer(session.p1());
                    if (opponent != null) {
                        health = opponent.getHealth();
                        var attr = opponent.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        if (attr != null) max = attr.getValue();
                        name = opponent.getName();
                    }
                }
                double progress = 0.0;
                if (max > 0) progress = Math.max(0.0, Math.min(1.0, health / max));
                bar2.setProgress(progress);
                bar2.setTitle(at.htlle.util.Chat.opponent(name) + (progress > 0 ? " - " + String.format("%.1f", health) + "❤" : ""));
                if (!bar2.getPlayers().contains(p2)) bar2.addPlayer(p2);
            }
        }
    }

    public void removeBossBarForSession(DuelSession session) {
        if (session == null) return;
        BossBar bar1 = bossBars.remove(session.p1());
        BossBar bar2 = session.p2() == null ? null : bossBars.remove(session.p2());
        Integer tid = bossBarTaskIds.remove(session.p1());
        if (session.p2() != null) bossBarTaskIds.remove(session.p2());
        if (tid != null) Bukkit.getScheduler().cancelTask(tid);
        if (bar1 != null) bar1.removeAll();
        if (bar2 != null) bar2.removeAll();
    }

    public DuelPlugin getPlugin() {
        return plugin;
    }

    public Optional<DuelInvite> getPendingInvite(Player invited) {
        return Optional.ofNullable(pendingInvites.get(invited.getUniqueId()));
    }

    public boolean isInDuel(Player p) {
        return activeSessions.containsKey(p.getUniqueId());
    }

    public Optional<DuelSession> getSession(Player p) {
        return Optional.ofNullable(activeSessions.get(p.getUniqueId()));
    }

    public DamageCredit credit() {
        return damageCredit;
    }

    public HazardTracker hazards() {
        return hazards;
    }

    /**
     * Player vs Player invite.
     * If inviter == invited => start dummy test duel (self duel).
     */
    public boolean invite(Player inviter, Player invited, List<Stake> stakes, String worldArg) {

        if (inviter.equals(invited)) {
            // self duel: du kannst hier auch worldArg berücksichtigen, wenn du willst
            startTestDuel(inviter, stakes);
            return true;
        }

        if (isInDuel(inviter) || isInDuel(invited)) {
            inviter.sendMessage(Chat.err("Einer von euch ist bereits in einem Duell."));
            return false;
        }

        // Welt wählen / prüfen
        World chosen;
        if (worldArg.equalsIgnoreCase("random")) {
            chosen = spawns.pickRandomWorldWithEnoughDuelSpawns(2);
            if (chosen == null) {
                inviter.sendMessage(Chat.err("Keine Welt hat genug Duel-Spawns (mind. 2)."));
                return false;
            }
        } else {
            chosen = Bukkit.getWorld(worldArg);
            if (chosen == null) {
                inviter.sendMessage(Chat.err("Welt existiert nicht oder ist nicht geladen: " + worldArg));
                return false;
            }
            if (!spawns.hasEnoughDuelSpawns(chosen, 2)) {
                inviter.sendMessage(Chat.err("Diese Welt hat zu wenig Duel-Spawns (mind. 2): " + chosen.getName()));
                return false;
            }
        }

        String chosenWorldName = chosen.getName();

        DuelInvite invite = new DuelInvite(inviter.getUniqueId(), invited.getUniqueId(), stakes, System.currentTimeMillis(), chosenWorldName);
        pendingInvites.put(invited.getUniqueId(), invite);

        // expire after 60s wie gehabt...
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelInvite current = pendingInvites.get(invited.getUniqueId());
            if (current != null && current.equals(invite)) {
                pendingInvites.remove(invited.getUniqueId());
                Player inv = Bukkit.getPlayer(inviter.getUniqueId());
                Player tgt = Bukkit.getPlayer(invited.getUniqueId());
                if (inv != null) inv.sendMessage(Chat.err("Deine Duell-Anfrage an " + invited.getName() + " ist abgelaufen."));
                if (tgt != null) tgt.sendMessage(Chat.err("Die Duell-Anfrage von " + inviter.getName() + " ist abgelaufen."));
            }
        }, 20L * 60);

        inviter.sendMessage(Chat.ok("Duell-Anfrage gesendet (Welt: " + chosenWorldName + ")"));
        return true;
    }

    public Collection<DuelSession> getAllSessions() {
        return activeSessions.values();
    }

    /**
     * ✅ Dummy Test Duel (self duel)
     */
    public void startTestDuel(Player player, List<Stake> stakes) {
        if (isInDuel(player)) {
            player.sendMessage(Chat.err("Du bist bereits in einem Duell."));
            return;
        }

        Location loc = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(2.5));

        // spawn dummy at one of the duel spawn locations; player will be teleported to the other
        World preferred = player.getWorld();
        Location[] picks = spawns.getTwoRandomDuelSpawnsAnyWorldPrefer(preferred);

        if (picks == null) {
            player.sendMessage(Chat.err("Es sind zu wenig Duel-Spawns gesetzt! (mind. 2)"));
            return;
        }

        returnLocation.put(player.getUniqueId(), player.getLocation().clone());
        Location dummyLoc = picks[0];
        Location playerLoc = picks[1];
        Zombie dummy = (Zombie) dummyLoc.getWorld().spawnEntity(dummyLoc, EntityType.ZOMBIE);
        dummy.setCustomName("§cDuel Dummy");
        dummy.setCustomNameVisible(true);

        dummy.setAI(false);
        dummy.setSilent(true);
        dummy.setRemoveWhenFarAway(false);
        dummy.setInvulnerable(false);

        var attr = dummy.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) attr.setBaseValue(100.0);
        dummy.setHealth(100.0);

        // Session speichern, record participation
        Escrow escrow = new Escrow();
        String fail = escrow.withdrawFromOne(player, stakes);
        if (fail != null) {
            player.sendMessage(Chat.err("Test-Duell konnte nicht starten: " + fail));
            dummy.remove();
            return;
        }

        DuelSession session = DuelSession.test(player.getUniqueId(), dummy.getUniqueId(), stakes, escrow);
        try { statsManager.recordParticipation(player.getUniqueId()); } catch (Throwable ignored) {}
        activeSessions.put(player.getUniqueId(), session);

        // teleport player to assigned playerLoc
        try { player.teleport(playerLoc); } catch (Throwable ignored) {}

        // ✅ Watchdog: wenn Dummy weg ist -> Spieler gewinnt automatisch
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            DuelSession current = activeSessions.get(player.getUniqueId());
            if (current == null || !current.isTestDuel()) {
                task.cancel();
                return;
            }

            Entity ent = Bukkit.getEntity(current.dummy());
            if (ent == null || !ent.isValid() || ent.isDead()) {
                task.cancel();

                Player stillThere = Bukkit.getPlayer(current.p1());
                if (stillThere != null) {
                    stillThere.sendMessage(Chat.ok("Dummy ist weg -> Du hast das Test-Duell gewonnen!"));
                }

                endSession(current, current.p1(), EndReason.DEATH_WIN);
            }
        }, 20L, 20L);

        player.sendMessage(Chat.ok("Test-Duell gestartet!") + " " , org.bukkit.ChatColor.RED + at.htlle.util.Chat.opponent("Duel Dummy")
            + Chat.gray(" (Stakes: " + Stake.pretty(stakes) + ")"));

        // Bossbar + start particles + title
        startFightCountdown(session, 5);
        startCountdownBossBar(session, 5);
        player.spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0,1,0), 60, 0.5,0.8,0.5, 0.02);
        player.sendTitle(org.bukkit.ChatColor.BLUE + "Duell gestartet!", org.bukkit.ChatColor.RED + "Gegner: " + org.bukkit.ChatColor.RED + "Duel Dummy", 10, 70, 10);
        // Announce test duel to non-duel players for server visibility (helpful for testing)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isInDuel(p)) {
                net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(at.htlle.util.Chat.announce(player.getName() + " startet ein Test-Duell mit einem Dummy "));
                net.md_5.bungee.api.chat.TextComponent watch = new net.md_5.bungee.api.chat.TextComponent("[Zuschauen]");
                watch.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                watch.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/duel watch " + player.getName()));
                msg.addExtra(watch);
                p.spigot().sendMessage(msg);
            }
        }
    }

    public void deny(Player invited, Player inviter) {
        DuelInvite invite = pendingInvites.get(invited.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter.getUniqueId())) {
            invited.sendMessage(Chat.err("Keine passende Duell-Anfrage gefunden."));
            return;
        }
        pendingInvites.remove(invited.getUniqueId());

        invited.sendMessage(Chat.ok("Duell-Anfrage abgelehnt."));
        inviter.sendMessage(Chat.err(invited.getName() + " hat die Duell-Anfrage abgelehnt."));
    }

    public void accept(Player invited, Player inviter) {
        DuelInvite invite = pendingInvites.get(invited.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter.getUniqueId())) {
            invited.sendMessage(Chat.err("Keine passende Duell-Anfrage gefunden."));
            return;
        }

        Player inv = Bukkit.getPlayer(invite.inviter());
        if (inv == null) {
            invited.sendMessage(Chat.err("Der Einladende ist nicht mehr online."));
            pendingInvites.remove(invited.getUniqueId());
            return;
        }

        // ✅ If somehow accept is called with self, just start dummy duel
        if (inv.equals(invited)) {
            pendingInvites.remove(invited.getUniqueId());
            startTestDuel(invited, invite.stakes());
            return;
        }

        if (isInDuel(inv) || isInDuel(invited)) {
            invited.sendMessage(Chat.err("Einer von euch ist bereits in einem Duell."));
            pendingInvites.remove(invited.getUniqueId());
            return;
        }

        Escrow escrow = new Escrow();
        List<Stake> stakes = invite.stakes();

        String fail = escrow.withdrawAll(inv, invited, stakes);
        if (fail != null) {
            invited.sendMessage(Chat.err("Duell konnte nicht gestartet werden: " + fail));
            inv.sendMessage(Chat.err("Duell konnte nicht gestartet werden: " + fail));
            pendingInvites.remove(invited.getUniqueId());
            escrow.refundAll(inv, invited);
            return;
        }

        pendingInvites.remove(invited.getUniqueId());

        DuelSession session = DuelSession.players(inv.getUniqueId(), invited.getUniqueId(), stakes, escrow);
        // record participation for both
        try {
            statsManager.recordParticipation(inv.getUniqueId());
            statsManager.recordParticipation(invited.getUniqueId());
        } catch (Throwable ignored) {}
        activeSessions.put(inv.getUniqueId(), session);
        activeSessions.put(invited.getUniqueId(), session);

        // enable PvP in world for these duelists even if server/world PvP is disabled
        World arena = Bukkit.getWorld(invite.worldName());
        if (arena == null) {
            // Welt ist plötzlich weg -> abort + refund
            session.escrow().refundAll(inv, invited);
            activeSessions.remove(inv.getUniqueId());
            activeSessions.remove(invited.getUniqueId());
            inv.sendMessage(Chat.err("Welt existiert nicht mehr: " + invite.worldName()));
            invited.sendMessage(Chat.err("Welt existiert nicht mehr: " + invite.worldName()));
            return;
        }

        Location[] picks = spawns.getTwoRandomDuelSpawns(arena);
        if (picks == null) {
            session.escrow().refundAll(inv, invited);
            activeSessions.remove(inv.getUniqueId());
            activeSessions.remove(invited.getUniqueId());
            inv.sendMessage(Chat.err("Zu wenig Duel-Spawns in Welt " + arena.getName() + " (mind. 2)."));
            invited.sendMessage(Chat.err("Zu wenig Duel-Spawns in Welt " + arena.getName() + " (mind. 2)."));
            return;
        }

        returnLocation.put(inv.getUniqueId(), inv.getLocation().clone());
        returnLocation.put(invited.getUniqueId(), invited.getLocation().clone());
        inv.teleport(picks[0]);
        invited.teleport(picks[1]);

        enablePvpForSession(session); // jetzt sind beide in der arena welt
        //start particles + titles (particles only shown to duel players)
        startFightCountdown(session, 5);         // für Freeze + Title
        startCountdownBossBar(session, 5);       // für grüne Bossbar
        inv.spawnParticle(org.bukkit.Particle.END_ROD, inv.getLocation().add(0,1,0), 60, 0.5,0.8,0.5, 0.02);
        invited.spawnParticle(org.bukkit.Particle.END_ROD, invited.getLocation().add(0,1,0), 60, 0.5,0.8,0.5, 0.02);
        inv.sendTitle(org.bukkit.ChatColor.BLUE + "Duell gestartet!", org.bukkit.ChatColor.RED + "Gegner: " + org.bukkit.ChatColor.RED + invited.getName(), 10, 70, 10);
        invited.sendTitle(org.bukkit.ChatColor.BLUE + "Duell gestartet!", org.bukkit.ChatColor.RED + "Gegner: " + org.bukkit.ChatColor.RED + inv.getName(), 10, 70, 10);

        inv.sendMessage(Chat.ok("Duell gestartet gegen " + invited.getName() + "!")
                + Chat.gray(" (Stakes: " + Stake.pretty(stakes) + ")"));
        invited.sendMessage(Chat.ok("Duell gestartet gegen " + inv.getName() + "!")
                + Chat.gray(" (Stakes: " + Stake.pretty(stakes) + ")"));

        // Inform non-dueling players about the duel with clickable watch button
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(inv.getUniqueId()) || p.getUniqueId().equals(invited.getUniqueId())) continue;
            if (isInDuel(p)) continue;
            net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(at.htlle.util.Chat.announce(inv.getName() + " hat ein Duell mit " + invited.getName() + " gestartet "));
            net.md_5.bungee.api.chat.TextComponent watch = new net.md_5.bungee.api.chat.TextComponent("[Zuschauen]");
            watch.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            watch.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/duel watch " + inv.getName()));
            msg.addExtra(watch);
            p.spigot().sendMessage(msg);
        }
    }

    public void endSession(DuelSession session, UUID winnerOrNull, EndReason reason) {
        // disable any PvP override for this session first
        try {
            disablePvpForSession(session);
        } catch (Throwable ignored) {}

        try {
            clearFreeze(session);
        } catch (Throwable ignored) {}

        // remove mappings
        activeSessions.remove(session.p1());
        if (session.p2() != null) activeSessions.remove(session.p2());

        // ✅ remove dummy if test duel
        if (session.isTestDuel() && session.dummy() != null) {
            Entity ent = Bukkit.getEntity(session.dummy());
            if (ent != null) ent.remove();
        }

        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        // remove bossbar
        removeBossBarForSession(session);

        // payout/refund logic
        if (session.isTestDuel()) {
            // Testduell: nur p1 hat eingezahlt
            if (winnerOrNull == null) {
                // If duel ended due to death or prevented death, treat as player loss -> do NOT refund.
                if (reason == EndReason.DEATH_WIN || reason == EndReason.PREVENTED_DEATH) {
                    // player lost to dummy: escrow is consumed (no refund)
                } else {
                    // other endings (cancelled etc.) -> refund to the single player
                    session.escrow().refundToOne(p1);
                }
            } else {
                Player winner = Bukkit.getPlayer(winnerOrNull);
                session.escrow().payoutToOne(winner);
            }
        } else {
            // normales Duell: beide haben eingezahlt
            if (winnerOrNull == null) {
                session.escrow().refundAll(p1, p2);
            } else {
                Player winner = Bukkit.getPlayer(winnerOrNull);
                session.escrow().payoutWinner(winner);
            }
        }

        if (p1 != null) p1.sendMessage(Chat.info("Duell beendet: " + reason.display()));
        if (p2 != null) p2.sendMessage(Chat.info("Duell beendet: " + reason.display()));

        // update stats: wins / losses
        try {
            if (session.isTestDuel()) {
                // only p1 is a real player
                if (winnerOrNull != null && winnerOrNull.equals(session.p1())) {
                    statsManager.recordWin(session.p1());
                } else {
                    statsManager.recordLoss(session.p1());
                }
            } else {
                if (winnerOrNull != null) {
                    UUID winner = winnerOrNull;
                    UUID loser = null;
                    if (session.p1() != null && !session.p1().equals(winnerOrNull)) loser = session.p1();
                    if (session.p2() != null && !session.p2().equals(winnerOrNull)) loser = session.p2();
                    if (winner != null) statsManager.recordWin(winner);
                    if (loser != null) statsManager.recordLoss(loser);
                }
            }
        } catch (Throwable ignored) {}

        // play end particles/titles/sounds, and ensure loser screen appears for real players
        if (session.isTestDuel()) {
            // test duels: only p1 is a player, dummy is opponent
            if (winnerOrNull == null) {
                // dummy won -> player lost
                if (p1 != null) {
                    p1.spawnParticle(org.bukkit.Particle.SQUID_INK, p1.getLocation().add(0,1,0), 30, 0.3,0.6,0.3);
                    p1.sendTitle(org.bukkit.ChatColor.RED + "Verloren!", org.bukkit.ChatColor.GRAY + "Dummy hat dich besiegt", 10, 70, 10);
                    p1.playSound(p1.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.8f);
                }
            } else {
                // player won
                Player winner = Bukkit.getPlayer(winnerOrNull);
                if (winner != null) {
                    winner.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, winner.getLocation().add(0,1,0), 40, 0.3,0.6,0.3);
                    winner.sendTitle(org.bukkit.ChatColor.GREEN + "Gewonnen!", org.bukkit.ChatColor.GRAY + "Glückwunsch", 10, 70, 10);
                    winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }
            }
        } else {
            if (winnerOrNull != null) {
                Player winner = Bukkit.getPlayer(winnerOrNull);
                Player loser = null;
                if (session.p1() != null && !session.p1().equals(winnerOrNull)) loser = Bukkit.getPlayer(session.p1());
                if (session.p2() != null && !session.p2().equals(winnerOrNull)) loser = Bukkit.getPlayer(session.p2());

                if (winner != null) {
                    winner.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, winner.getLocation().add(0,1,0), 40, 0.3,0.6,0.3);
                    winner.sendTitle(org.bukkit.ChatColor.GREEN + "Gewonnen!", org.bukkit.ChatColor.GRAY + "Glückwunsch", 10, 70, 10);
                    winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }
                if (loser != null) {
                    loser.spawnParticle(org.bukkit.Particle.SQUID_INK, loser.getLocation().add(0,1,0), 30, 0.3,0.6,0.3);
                    loser.sendTitle(org.bukkit.ChatColor.RED + "Verloren!", org.bukkit.ChatColor.GRAY + "Schade", 10, 70, 10);
                    loser.playSound(loser.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.8f);
                }
            } else {
                // cancelled/ended without winner
                if (p1 != null) p1.playSound(p1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                if (p2 != null) p2.playSound(p2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
        // ✅ Always teleport players back after duel ends (test + pvp)
        startReturnCountdown(session, 5);
    }

    public void cancel(Player p) {
        DuelSession s = activeSessions.get(p.getUniqueId());
        if (s == null) {
            p.sendMessage(Chat.err("Du bist in keinem Duell."));
            return;
        }

        // Wenn PvP-Duell: optional nur p1 darf canceln – für Demo lassen wir jeden canceln
        endSession(s, null, EndReason.CANCELLED);

        p.sendMessage(Chat.ok("Duell abgebrochen."));
    }

    public void startFightCountdown(DuelSession session, int seconds) {
        if (session == null) return;

        // Freeze setzen
        freezeSession(session, seconds * 1000L);

        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        // Jeder Sekunde Title
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = seconds;

            @Override
            public void run() {
                boolean active = activeSessions.containsKey(session.p1())
                        || (session.p2() != null && activeSessions.containsKey(session.p2()));
                if (!active) { cancel(); return; }

                if (t > 0) {
                    String title = org.bukkit.ChatColor.GOLD + "Duell startet in " + org.bukkit.ChatColor.WHITE + t;
                    String sub = org.bukkit.ChatColor.GRAY + "Bewegen gesperrt";
                    if (p1 != null) p1.sendTitle(title, sub, 0, 20, 0);
                    if (p2 != null) p2.sendTitle(title, sub, 0, 20, 0);
                    t--;
                    return;
                }

                String title = org.bukkit.ChatColor.RED + "KAMPF!";
                String sub = org.bukkit.ChatColor.GRAY + "Viel Glück!";
                if (p1 != null) p1.sendTitle(title, sub, 0, 30, 10);
                if (p2 != null) p2.sendTitle(title, sub, 0, 30, 10);

                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startReturnCountdown(DuelSession session, int seconds) {
        if (session == null) return;

        // Empfänger: duelists + watchers
        List<Player> receivers = new ArrayList<>();

        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        if (p1 != null) receivers.add(p1);
        if (p2 != null) receivers.add(p2);

        // watchers von beiden holen
        if (p1 != null) {
            for (UUID wid : getWatchersOf(p1)) {
                Player w = Bukkit.getPlayer(wid);
                if (w != null) receivers.add(w);
            }
        }
        if (p2 != null) {
            for (UUID wid : getWatchersOf(p2)) {
                Player w = Bukkit.getPlayer(wid);
                if (w != null) receivers.add(w);
            }
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int t = seconds;

            @Override
            public void run() {
                if (t <= 0) {
                    if (p1 != null) {
                        Location back = returnLocation.remove(p1.getUniqueId());
                        if (back != null) p1.teleport(back);
                    }
                    if (p2 != null) {
                        Location back = returnLocation.remove(p2.getUniqueId());
                        if (back != null) p2.teleport(back);
                    }

                    stopWatchersOfSession(session);

                    cancel();
                    return;
                }

                for (Player r : receivers) {
                    r.sendMessage(Chat.info("Kampf ist vorbei: Rückkehr in " + t));
                }
                t--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void startCountdownBossBar(DuelSession session, int seconds) {
        if (session == null) return;

        // Entferne evtl. alte Fight-Bars vorher (Safety)
        removeBossBarForSession(session);
        removeCountdownBossBar(session);

        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());

        if (p1 != null) {
            BossBar b1 = Bukkit.createBossBar("§aDuell startet in " + seconds, BarColor.GREEN, BarStyle.SOLID);
            b1.setProgress(1.0);
            b1.addPlayer(p1);
            countdownBars.put(p1.getUniqueId(), b1);
        }
        if (p2 != null) {
            BossBar b2 = Bukkit.createBossBar("§aDuell startet in " + seconds, BarColor.GREEN, BarStyle.SOLID);
            b2.setProgress(1.0);
            b2.addPlayer(p2);
            countdownBars.put(p2.getUniqueId(), b2);
        }

        // Scheduler: WICHTIG -> BukkitRunnable#runTaskTimer verwenden
        var task = new org.bukkit.scheduler.BukkitRunnable() {
            int t = seconds;

            @Override
            public void run() {
                boolean active = activeSessions.containsKey(session.p1())
                        || (session.p2() != null && activeSessions.containsKey(session.p2()));
                if (!active) { cancel(); removeCountdownBossBar(session); return; }

                double prog = Math.max(0.0, Math.min(1.0, (double) t / (double) seconds));

                if (p1 != null) {
                    BossBar b = countdownBars.get(p1.getUniqueId());
                    if (b != null) {
                        b.setTitle("§aDuell startet in §f" + t);
                        b.setProgress(prog);
                    }
                }
                if (p2 != null) {
                    BossBar b = countdownBars.get(p2.getUniqueId());
                    if (b != null) {
                        b.setTitle("§aDuell startet in §f" + t);
                        b.setProgress(prog);
                    }
                }

                if (p1 != null) {
                    p1.playSound(
                            p1.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_HAT,
                            0.6f,                 // Lautstärke
                            1.8f                  // Pitch → "Tick"-Gefühl
                    );
                }

                if (p2 != null) {
                    p2.playSound(
                            p2.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_HAT,
                            0.6f,
                            1.8f
                    );
                }

                if (t <= 0) {
                    cancel();
                    removeCountdownBossBar(session);

                    // 🔥 KAMPF BEGINNT – Sound
                    if (p1 != null) {
                        p1.playSound(
                                p1.getLocation(),
                                Sound.ENTITY_ENDER_DRAGON_GROWL,
                                0.8f,
                                1.2f
                        );
                    }
                    if (p2 != null) {
                        p2.playSound(
                                p2.getLocation(),
                                Sound.ENTITY_ENDER_DRAGON_GROWL,
                                0.8f,
                                1.2f
                        );
                    }

                    createBossBarForSession(session);
                    clearFreeze(session);
                    return;
                }
                t--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        if (p1 != null) countdownTaskIds.put(p1.getUniqueId(), task.getTaskId());
        if (p2 != null) countdownTaskIds.put(p2.getUniqueId(), task.getTaskId());
    }

    public void removeCountdownBossBar(DuelSession session) {
        if (session == null) return;

        BossBar b1 = countdownBars.remove(session.p1());
        if (b1 != null) b1.removeAll();

        if (session.p2() != null) {
            BossBar b2 = countdownBars.remove(session.p2());
            if (b2 != null) b2.removeAll();
        }

        Integer t1 = countdownTaskIds.remove(session.p1());
        if (t1 != null) Bukkit.getScheduler().cancelTask(t1);

        if (session.p2() != null) {
            Integer t2 = countdownTaskIds.remove(session.p2());
            if (t2 != null) Bukkit.getScheduler().cancelTask(t2);
        }
    }

    public enum EndReason {
        CANCELLED("abgebrochen"),
        PREVENTED_DEATH("Todesstoß verhindert"),
        DEATH_WIN("Tod im Duell"),
        DISCONNECT("Spieler disconnect");

        private final String display;
        EndReason(String display) { this.display = display; }
        public String display() { return display; }
    }

    // Expose stats helpers for UI
    public java.util.List<java.util.UUID> getStatsPlayers() {
        try {
            return statsManager.playersWithParticipation();
        } catch (Throwable t) { return java.util.Collections.emptyList(); }
    }

    public StatsManager.Stats getStatsForPlayer(java.util.UUID id) {
        return statsManager.getStats(id).orElse(new StatsManager.Stats(0,0,0));
    }

    public java.util.List<java.util.UUID> getLeaderboardPlayers() {
        var list = new ArrayList<java.util.UUID>(statsManager.playersWithParticipation());
        list.sort((a,b) -> Integer.compare(statsManager.getStats(b).map(s->s.wins).orElse(0), statsManager.getStats(a).map(s->s.wins).orElse(0)));
        return list;
    }

    public void openLeaderboard(org.bukkit.entity.Player p) {
        java.util.List<java.util.UUID> players = getLeaderboardPlayers();
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, "Duell Leaderboard");
        // fill with panes
        org.bukkit.inventory.ItemStack pane = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta pm = pane.getItemMeta(); if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        for (int i=0;i<54;i++) inv.setItem(i, pane);

        // start placing heads at second row, second column (row=1,col=1 -> index 10)
        int slot = 10;
        for (java.util.UUID id : players) {
            if (slot >= 54) break;
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(id);
            org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(op);
                String name = op.getName() == null ? id.toString() : op.getName();
                sm.setDisplayName(org.bukkit.ChatColor.YELLOW + name);
                var s = getStatsForPlayer(id);
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(org.bukkit.ChatColor.GRAY + "Siege: " + org.bukkit.ChatColor.WHITE + s.wins);
                lore.add(org.bukkit.ChatColor.GRAY + "Niederlagen: " + org.bukkit.ChatColor.WHITE + s.losses);
                double ratio = s.losses==0 ? s.wins : ((double)s.wins)/(double)s.losses;
                lore.add(org.bukkit.ChatColor.GRAY + "W/L: " + org.bukkit.ChatColor.WHITE + String.format("%.1f", ratio));
                sm.setLore(lore);
                head.setItemMeta(sm);
            }
            // if current slot is reserved (bottom-left 45 or bottom-right 53), skip to next
            while (slot == 45 || slot == 53) slot++;
            if (slot >= 54) break;
            inv.setItem(slot, head);
            // move to next column
            slot++;
            // skip the rightmost column to keep UI buttons free
            if (slot % 9 == 8) slot++;
        }
        // left bottom X (slot 45) -> close
        org.bukkit.inventory.ItemStack x = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAzMTljMWVkMWU4NGQ4OTA2NSJ9fX0=");
        org.bukkit.inventory.meta.ItemMeta xm = x.getItemMeta(); if (xm != null) { xm.setDisplayName(org.bukkit.ChatColor.RED + "Abbruch"); x.setItemMeta(xm); }
        inv.setItem(45, x);

        // right bottom star (slot 53) -> back to personal stats
        org.bukkit.inventory.ItemStack star = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta stm = star.getItemMeta(); if (stm != null) { stm.setDisplayName(org.bukkit.ChatColor.GOLD + "Zurück"); star.setItemMeta(stm); }
        inv.setItem(53, star);

        p.openInventory(inv);
    }

    private boolean teleportParticipants(DuelSession session) {
        if (session == null || session.isTestDuel()) return false;
        Player p1 = Bukkit.getPlayer(session.p1());
        Player p2 = session.p2() == null ? null : Bukkit.getPlayer(session.p2());
        if (p1 == null || p2 == null) return false;

        World preferred = p1.getWorld();
        Location[] picks = spawns.getTwoRandomDuelSpawnsAnyWorldPrefer(preferred);

        if (picks == null) {
            p1.sendMessage(Chat.err("Es sind zu wenig Duel-Spawns gesetzt! (mind. 2)"));
            p2.sendMessage(Chat.err("Es sind zu wenig Duel-Spawns gesetzt! (mind. 2)"));
            return false;
        }

        p1.teleport(picks[0]);
        p2.teleport(picks[1]);
        return true;
    }

    // Open personal stats inventory (same layout as DuelCommand.openStatsInventory)
    public void openPersonalStats(org.bukkit.entity.Player p) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, "Duell Statistiken");
        // fill panes
        org.bukkit.inventory.ItemStack pane = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta pm = pane.getItemMeta(); if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // custom heads
        ItemStack crown = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiMGE1YmJjNjk3YzBjNDJhNmNmMWI5YzRjNDQzNWIwNzMyMmZjZTViYjI3ZDgyYjY5MzA4NDNlNWFiN2EwOSJ9fX0=");
        ItemStack skull = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWI3ZjU3NjFmNGNhNzI0NTJkYzE4ZWEyMThkYjE0OTAyMGY3ZmNiMTQ4NDMxNTQ3YTBhYWI0NGQ1NGI2Y2M3NCJ9fX0=");
        ItemStack xhead = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAzMTljMWVkMWU4NGQ4OTA2NSJ9fX0=");

        // Crown (wins)
        var s = getStatsForPlayer(p.getUniqueId());
        var cmeta = crown.getItemMeta(); if (cmeta != null) { cmeta.setDisplayName(org.bukkit.ChatColor.GOLD + "Siege"); cmeta.setLore(java.util.List.of(org.bukkit.ChatColor.WHITE + String.valueOf(s.wins))); crown.setItemMeta(cmeta); }
        inv.setItem(11, crown);

        // Skull (losses)
        var sm = skull.getItemMeta(); if (sm != null) { sm.setDisplayName(org.bukkit.ChatColor.DARK_RED + "Niederlagen"); sm.setLore(java.util.List.of(org.bukkit.ChatColor.WHITE + String.valueOf(s.losses))); skull.setItemMeta(sm); }
        inv.setItem(15, skull);

        // X (Abbruch) at left bottom
        var xm = xhead.getItemMeta(); if (xm != null) { xm.setDisplayName(org.bukkit.ChatColor.RED + "Abbruch"); xhead.setItemMeta(xm); }
        inv.setItem(18, xhead);

        // Nether star at right bottom -> open leaderboard
        org.bukkit.inventory.ItemStack star = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
        var stm = star.getItemMeta(); if (stm != null) { stm.setDisplayName(org.bukkit.ChatColor.GOLD + "Leaderboard"); star.setItemMeta(stm); }
        inv.setItem(26, star);

        // own head in center showing W/L ratio
        org.bukkit.inventory.ItemStack ownHead = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta ownMeta = (org.bukkit.inventory.meta.SkullMeta) ownHead.getItemMeta();
        if (ownMeta != null) {
            ownMeta.setOwningPlayer(p);
            double ratio = s.losses==0 ? s.wins : ((double)s.wins)/(double)s.losses;
            ownMeta.setDisplayName(org.bukkit.ChatColor.YELLOW + p.getName());
            ownMeta.setLore(java.util.List.of(org.bukkit.ChatColor.GRAY + "W/L: " + org.bukkit.ChatColor.WHITE + String.format("%.1f", ratio)));
            ownHead.setItemMeta(ownMeta);
        }
        inv.setItem(13, ownHead);

        p.openInventory(inv);
    }

    public ItemStack createCustomHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) return head;

        PlayerProfile profile = plugin.getServer().createProfile(UUID.randomUUID());
        profile.getProperties().add(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}