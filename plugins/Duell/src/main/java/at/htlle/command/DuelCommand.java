package at.htlle.command;

import at.htlle.duel.DuelManager;
import at.htlle.duel.Stake;
import at.htlle.duel.StakeParser;
import at.htlle.util.Chat;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import at.htlle.duel.DuelSession;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final DuelManager duelManager;

    public DuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    private boolean requireOp(Player p) {
        if (!p.isOp()) {
            p.sendMessage(Chat.err("Nur Operatoren dürfen diesen Command benutzen."));
            return false;
        }
        return true;
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String p = prefix.toLowerCase();
        return input.stream()
                .filter(s -> s.toLowerCase().startsWith(p))
                .collect(Collectors.toList());
    }

    private List<String> duelPlayersNames() {
        // Nur Spieler die gerade IN einem Duel sind
        return Bukkit.getOnlinePlayers().stream()
                .filter(duelManager::isInDuel)
                .map(Player::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> publicSubcommands() {
        return List.of(
                "invite","accept","deny","status","cancel","stats",
                "watch","unwatch"
        );
    }

    private List<String> opOnlySubcommands() {
        return List.of(
                "setduelspawn","setspectatorspawn","listspawns","clearspawns","tp"
        );
    }

    private List<String> visibleSubcommands(Player p) {
        List<String> out = new ArrayList<>(publicSubcommands());
        if (p.isOp()) out.addAll(opOnlySubcommands()); // oder Permission-Check (Variante B)
        return out;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            Player p = (Player) sender;
            return filterPrefix(visibleSubcommands(p), args[0]);
        }

        String sub = args[0].toLowerCase();

        // /duel watch <playerInDuel>
        if (args.length == 2 && sub.equals("watch")) {
            return filterPrefix(duelPlayersNames(), args[1]);
        }

        // /duel unwatch   (keine weiteren args nötig)
        if (args.length == 2 && sub.equals("unwatch")) {
            // optional: nix vorschlagen
            return Collections.emptyList();
        }

        // dein bestehendes Verhalten:
        if (args.length == 2) {
            if (sub.equals("invite") || sub.equals("accept") || sub.equals("deny")) {
                java.util.List<String> names = new ArrayList<>();
                names.add("@s");
                names.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                return filterPrefix(names, args[1]);
            }

            if (sub.equals("listspawns") || sub.equals("clearspawns")) {
                return filterPrefix(java.util.List.of("duel","spectator","all"), args[1]);
            }

            if (sub.equals("tp")) {
                return filterPrefix(java.util.List.of("duel","spectator"), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("clearspawns")) {
            return filterPrefix(java.util.List.of("all","1","2","3"), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
            return filterPrefix(java.util.List.of("1","2","3"), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            String prefix = args[2].toLowerCase();

            List<String> worlds = new ArrayList<>();
            worlds.add("random");
            worlds.addAll(duelManager.spawns().getWorldNamesWithEnoughDuelSpawns(2));

            return worlds.stream()
                    .filter(s -> s.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("invite")) {
            List<String> suggestions = List.of("toTheDeath", "<stakes>");
            String cur = args[args.length - 1].toLowerCase();
            return suggestions.stream().filter(s -> s.toLowerCase().startsWith(cur)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("duel.use")) {
            p.sendMessage(Chat.err("Keine Rechte."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        // OP-only: nicht mal Tab-Vorschläge anzeigen
        if (opOnlySubcommands().contains(sub) && !((Player) sender).isOp()) {
            return true;
        }

        try {
            switch (sub) {
                case "invite": {
                    if (args.length < 3) {
                        p.sendMessage(Chat.err("Usage: /duel invite <player|@s> <world|random> [toTheDeath] [stakes...]"));
                        return true;
                    }

                    String targetArg = args[1];
                    String worldArg = args[2];
                    Player target;

                    // @s support
                    if (targetArg.equalsIgnoreCase("@s")) target = p;
                    else target = Bukkit.getPlayerExact(targetArg);

                    if (target == null) {
                        p.sendMessage(Chat.err("Spieler nicht online."));
                        return true;
                    }

                    // optional toTheDeath at args[3]
                    boolean toTheDeath = false;
                    int stakesStart = 3;
                    if (args.length >= 4 && args[3].equalsIgnoreCase("toTheDeath")) {
                        toTheDeath = true;
                        stakesStart = 4;
                    }

                    List<Stake> stakes = StakeParser.parse(args, stakesStart);

                    // wenn toTheDeath angegeben wurde, sicherstellen dass DeathStake drin ist
                    if (toTheDeath) {
                        boolean hasDeath = stakes.stream().anyMatch(s -> s instanceof Stake.DeathStake);
                        if (!hasDeath) stakes.add(new Stake.DeathStake());
                    }

                    // und jetzt: invite mit worldArg
                    boolean inviteOk = duelManager.invite(p, target, stakes, worldArg);
                    if (!inviteOk) return true;

                    // ✅ SELF-INVITE: DuelManager startet bereits das Testduell -> keine Anfrage/Buttons schicken
                    if (target.equals(p)) {
                        p.sendMessage(Chat.ok("Test-Duell gestartet."));
                        return true;
                    }


                    // send messages per request
                    // invited message
                    String inviteMsg;
                    if (toTheDeath) inviteMsg = p.getName() + " will mit dir bis zum Tod kämpfen";
                    else inviteMsg = p.getName() + " will mit dir kämpfen";
                    if (!stakes.isEmpty()) inviteMsg += " und bietet noch " + Stake.pretty(stakes) + " dazu";

                    // plain info
                    target.sendMessage(Chat.info(inviteMsg + Chat.gray(" (Welt: " + worldArg + ")")));

                    // clickable accept/deny components
                    TextComponent accept = new TextComponent("[ANNEHMEN]");
                    accept.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + p.getName()));
                    accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept the duel").create()));

                    TextComponent deny = new TextComponent("[ABLEHNEN]");
                    deny.setColor(net.md_5.bungee.api.ChatColor.RED);
                    deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel deny " + p.getName()));
                    deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Deny the duel").create()));

                    TextComponent spacer = new TextComponent(" ");
                    target.spigot().sendMessage(accept, spacer, deny);

                    // inviter acknowledgement
                    p.sendMessage(Chat.ok("Einladung gesendet"));

                    // server broadcast (to all not in duel and not the two players)
                    if (stakes.isEmpty()) {
                        String msg = toTheDeath ? p.getName() + " und " + target.getName() + " kämpfen bis zum Tod" : p.getName() + " und " + target.getName() + " fordern sich zum Duell heraus";
                        for (Player pl : Bukkit.getOnlinePlayers()) {
                            if (pl.getUniqueId().equals(p.getUniqueId()) || pl.getUniqueId().equals(target.getUniqueId())) continue;
                            if (duelManager.isInDuel(pl)) continue;
                            pl.sendMessage(at.htlle.util.Chat.announce(msg));
                        }
                    } else {
                        // compute total per material (double since both players)
                        java.util.Map<String, Integer> totals = new java.util.HashMap<>();
                        for (Stake s : stakes) {
                            if (s instanceof Stake.ItemStake) {
                                Stake.ItemStake it = (Stake.ItemStake) s;
                                String mat = it.material().name();
                                totals.put(mat, totals.getOrDefault(mat, 0) + it.amount() * 2);
                            }
                        }
                        java.util.List<String> parts = new ArrayList<>();
                        for (java.util.Map.Entry<String, Integer> e : totals.entrySet()) {
                            String mat = e.getKey();
                            int amt = e.getValue();
                            // make nicer material name (capitalize lower-case)
                            String nice = mat.substring(0,1).toUpperCase() + mat.substring(1).toLowerCase();
                            parts.add(amt + " " + nice + "/s");
                        }
                        String stakeText = String.join(", ", parts);
                        String msg = p.getName() + " und " + target.getName() + " kämpfen bis zum Tod mit " + stakeText + " auf dem Spiel";
                        for (Player pl : Bukkit.getOnlinePlayers()) {
                            if (pl.getUniqueId().equals(p.getUniqueId()) || pl.getUniqueId().equals(target.getUniqueId())) continue;
                            if (duelManager.isInDuel(pl)) continue;
                            pl.sendMessage(at.htlle.util.Chat.announce(msg));
                        }
                    }
                    break;
                }

                case "setduelspawn": {
                    if (!requireOp(p)) return true;
                    int idx = duelManager.spawns().addDuelSpawn(p.getLocation());
                    duelManager.getPlugin().saveConfig();
                    p.sendMessage(Chat.ok("Duel-Spawn gespeichert: duelspawn" + idx));
                    return true;
                }

                case "setspectatorspawn": {
                    if (!requireOp(p)) return true;
                    int idx = duelManager.spawns().addSpectatorSpawn(p.getLocation());
                    duelManager.getPlugin().saveConfig();
                    p.sendMessage(Chat.ok("Spectator-Spawn gespeichert: spectatorspawn" + idx));
                    return true;
                }

                case "listspawns": {
                    if (!requireOp(p)) return true;

                    // optional: /duel listspawns duel|spectator|all
                    String type = (args.length >= 2) ? args[1].toLowerCase() : "all";

                    // cleanup before listing (falls world fehlt)
                    int removed = duelManager.spawns().cleanupMissingWorlds();
                    if (removed > 0) duelManager.getPlugin().saveConfig();

                    if (type.equals("duel") || type.equals("all")) {
                        List<Location> duel = duelManager.spawns().getAllDuelSpawns();
                        p.sendMessage(Chat.gray("---- Duel Spawns ----"));
                        if (duel.isEmpty()) p.sendMessage(Chat.info("Keine duel-spawns gesetzt."));
                        for (int i = 0; i < duel.size(); i++) {
                            Location l = duel.get(i);
                            p.sendMessage(Chat.info("duelspawn" + (i+1) + " -> " + l.getWorld().getName()
                                    + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()));
                        }
                    }

                    if (type.equals("spectator") || type.equals("all")) {
                        List<Location> spec = duelManager.spawns().getAllSpectatorSpawns();
                        p.sendMessage(Chat.gray("---- Spectator Spawns ----"));
                        if (spec.isEmpty()) p.sendMessage(Chat.info("Keine spectator-spawns gesetzt."));
                        for (int i = 0; i < spec.size(); i++) {
                            Location l = spec.get(i);
                            p.sendMessage(Chat.info("spectatorspawn" + (i+1) + " -> " + l.getWorld().getName()
                                    + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()));
                        }
                    }

                    return true;
                }

                case "clearspawns": {
                    if (!requireOp(p)) return true;

                    // /duel clearspawns <duel|spectator|all> [index|all]
                    if (args.length < 2) {
                        p.sendMessage(Chat.err("Usage: /duel clearspawns <duel|spectator|all> [index|all]"));
                        return true;
                    }

                    String type = args[1].toLowerCase();
                    String target = (args.length >= 3) ? args[2].toLowerCase() : "all";

                    // cleanup first
                    int removedMissing = duelManager.spawns().cleanupMissingWorlds();
                    if (removedMissing > 0) duelManager.getPlugin().saveConfig();

                    if (target.equals("all")) {
                        if (type.equals("duel")) duelManager.spawns().clearDuelSpawns();
                        else if (type.equals("spectator")) duelManager.spawns().clearSpectatorSpawns();
                        else if (type.equals("all")) {
                            duelManager.spawns().clearDuelSpawns();
                            duelManager.spawns().clearSpectatorSpawns();
                        } else {
                            p.sendMessage(Chat.err("Type muss duel|spectator|all sein."));
                            return true;
                        }
                        duelManager.getPlugin().saveConfig();
                        p.sendMessage(Chat.ok("Spawns gelöscht: " + type + " (all)"));
                        return true;
                    }

                    int idx;
                    try { idx = Integer.parseInt(target); }
                    catch (NumberFormatException e) {
                        p.sendMessage(Chat.err("Index muss eine Zahl sein oder 'all'."));
                        return true;
                    }

                    boolean ok;
                    if (type.equals("duel")) ok = duelManager.spawns().removeDuelSpawn(idx);
                    else if (type.equals("spectator")) ok = duelManager.spawns().removeSpectatorSpawn(idx);
                    else {
                        p.sendMessage(Chat.err("Type muss duel|spectator sein (für index-delete)."));
                        return true;
                    }

                    if (!ok) {
                        p.sendMessage(Chat.err("Ungültiger Index. Nutze /duel listspawns " + type));
                        return true;
                    }

                    duelManager.getPlugin().saveConfig();
                    p.sendMessage(Chat.ok("Gelöscht: " + type + "spawn" + idx + " (Liste ist nachgerutscht)"));
                    return true;
                }

                case "tp": {
                    if (!requireOp(p)) return true;

                    // /duel tp <duel|spectator> <index>
                    if (args.length < 3) {
                        p.sendMessage(Chat.err("Usage: /duel tp <duel|spectator> <index>"));
                        return true;
                    }

                    String type = args[1].toLowerCase();
                    int idx;
                    try { idx = Integer.parseInt(args[2]); }
                    catch (NumberFormatException e) {
                        p.sendMessage(Chat.err("Index muss eine Zahl sein."));
                        return true;
                    }

                    // cleanup first
                    int removedMissing = duelManager.spawns().cleanupMissingWorlds();
                    if (removedMissing > 0) duelManager.getPlugin().saveConfig();

                    Location dest = null;
                    if (type.equals("duel")) {
                        List<Location> duel = duelManager.spawns().getAllDuelSpawns();
                        if (idx >= 1 && idx <= duel.size()) dest = duel.get(idx-1);
                    } else if (type.equals("spectator")) {
                        List<Location> spec = duelManager.spawns().getAllSpectatorSpawns();
                        if (idx >= 1 && idx <= spec.size()) dest = spec.get(idx-1);
                    } else {
                        p.sendMessage(Chat.err("Type muss duel|spectator sein."));
                        return true;
                    }

                    if (dest == null || dest.getWorld() == null) {
                        p.sendMessage(Chat.err("Spawn existiert nicht (oder Welt fehlt). Nutze /duel listspawns."));
                        return true;
                    }

                    boolean ok = p.teleport(dest);
                    if (ok) {
                        p.sendMessage(Chat.ok("Teleportiert zu " + type + "spawn" + idx + " in " + dest.getWorld().getName()));
                    } else {
                        p.sendMessage(Chat.err("Teleport fehlgeschlagen."));
                    }
                    return true;
                }

                case "accept": {
                    if (args.length < 2) { p.sendMessage(Chat.err("Usage: /duell accept <player>")); return true; }
                    Player inviter = Bukkit.getPlayerExact(args[1]);
                    if (inviter == null) { p.sendMessage(Chat.err("Spieler nicht online.")); return true; }
                    duelManager.accept(p, inviter);
                    break;
                }
                case "deny": {
                    if (args.length < 2) { p.sendMessage(Chat.err("Usage: /duell deny <player>")); return true; }
                    Player inviter = Bukkit.getPlayerExact(args[1]);
                    if (inviter == null) { p.sendMessage(Chat.err("Spieler nicht online.")); return true; }
                    duelManager.deny(p, inviter);
                    break;
                }
                case "status": {
                    if (duelManager.isInDuel(p)) p.sendMessage(Chat.info("Du bist in einem Duell."));
                    else p.sendMessage(Chat.info("Du bist in keinem Duell."));
                    break;
                }
                case "watch": {
                // /duel watch <player>
                if (args.length < 2) {
                    p.sendMessage(Chat.err("Usage: /duel watch <spieler>"));
                    return true;
                }

                // optional: nicht erlauben wenn watcher selbst in duel ist
                if (duelManager.isInDuel(p)) {
                    p.sendMessage(Chat.err("Du bist selbst gerade in einem Duell."));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    p.sendMessage(Chat.err("Spieler nicht online."));
                    return true;
                }

                DuelSession s = duelManager.getSession(target).orElse(null);
                if (s == null) {
                    p.sendMessage(Chat.err("Kein gültiges Duell gefunden."));
                    return true;
                }
                    // teleport -> spectator location im selben World wie das Duell
                    World w = target.getWorld();
                    Location dest = duelManager.randomSpectatorLocation(w);

                    if (dest == null) {
                        p.sendMessage(Chat.err("Keine Spectator-Spawns gesetzt! (mind. 1)"));
                        return true;
                    }

                    duelManager.startWatching(p, target);

                    boolean ok = p.teleport(dest);
                    if (ok) {
                        p.sendMessage(Chat.ok("Du wurdest zu einem Zuschauerplatz teleportiert."));
                    } else {
                        // teleport failed -> rollback spectator state
                        duelManager.stopWatching(p);
                        p.sendMessage(Chat.err("Teleport fehlgeschlagen."));
                    }
                    return true;
                }

                case "unwatch": {
                    if (!duelManager.isSpectator(p)) {
                        p.sendMessage(Chat.err("Du schaust gerade niemandem zu."));
                        return true;
                    }
                    duelManager.stopWatching(p);
                    p.sendMessage(Chat.ok("Du schaust nicht mehr zu und wurdest zurück teleportiert."));
                    return true;
                }

                case "stats": {
                    openStatsInventory(p);
                    break;
                }
                case "cancel": {
                    duelManager.cancel(p);
                    break;
                }
                default: sendHelp(p);
            }
        } catch (IllegalArgumentException ex) {
            p.sendMessage(Chat.err(ex.getMessage()));
        }

        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(Chat.gray("----- ") + Chat.info("Duell") + Chat.gray(" -----"));
        p.sendMessage(Chat.cmd("/duel invite <player|@s> <world|random> [toTheDeath] [stakes...]"));
        p.sendMessage(Chat.cmd("/duel accept <player>"));
        p.sendMessage(Chat.cmd("/duel deny <player>"));
        p.sendMessage(Chat.cmd("/duel status"));
        p.sendMessage(Chat.cmd("/duel cancel"));
        p.sendMessage(Chat.cmd("/duel stats"));
        p.sendMessage(Chat.cmd("/duel watch <spieler>"));
        p.sendMessage(Chat.cmd("/duel unwatch"));

        if (p.isOp()) {
            p.sendMessage(Chat.gray("----- ") + Chat.info("Admin") + Chat.gray(" -----"));
            p.sendMessage(Chat.cmd("/duel setduelspawn"));
            p.sendMessage(Chat.cmd("/duel setspectatorspawn"));
            p.sendMessage(Chat.cmd("/duel listspawns [duel|spectator|all]"));
            p.sendMessage(Chat.cmd("/duel clearspawns <duel|spectator|all> [index|all]"));
            p.sendMessage(Chat.cmd("/duel tp <duel|spectator> <index>"));
        }
    }

    private void openStatsInventory(Player p) {
        // 27 slots, middle row 9..17 will contain player heads of players with participation
        Inventory inv = Bukkit.createInventory(null, 27, "Duell Statistiken");

        // fill with gray panes
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) {
            pm.setDisplayName(" ");
            pane.setItemMeta(pm);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // place custom decorations
        String crownBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFiMGE1YmJjNjk3YzBjNDJhNmNmMWI5YzRjNDQzNWIwNzMyMmZjZTViYjI3ZDgyYjY5MzA4NDNlNWFiN2EwOSJ9fX0=";
        String skullBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWI3ZjU3NjFmNGNhNzI0NTJkYzE4ZWEyMThkYjE0OTAyMGY3ZmNiMTQ4NDMxNTQ3YTBhYWI0NGQ1NGI2Y2M3NCJ9fX0=";
        String xBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAzMTljMWVkMWU4NGQ4OTA2NSJ9fX0=";

        ItemStack crown = duelManager.createCustomHead(crownBase64);
        ItemStack skull = duelManager.createCustomHead(skullBase64);
        ItemStack xhead = duelManager.createCustomHead(xBase64);

        // place decorative items: crown (wins only), skull (losses only), X (Abbruch)
        // Crown at slot 11
        ItemMeta crownMeta = crown.getItemMeta();
        if (crownMeta != null) {
            crownMeta.setDisplayName(org.bukkit.ChatColor.GOLD + "Siege");
            var s = duelManager.getStatsForPlayer(p.getUniqueId());
            java.util.List<String> crownLore = new ArrayList<>();
            crownLore.add(org.bukkit.ChatColor.WHITE + String.valueOf(s.wins));
            crownMeta.setLore(crownLore);
            crown.setItemMeta(crownMeta);
        }
        inv.setItem(11, crown);

        // Skull at slot 15
        ItemMeta skullMeta = skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName(org.bukkit.ChatColor.DARK_RED + "Niederlagen");
            var s = duelManager.getStatsForPlayer(p.getUniqueId());
            java.util.List<String> skullLore = new ArrayList<>();
            skullLore.add(org.bukkit.ChatColor.WHITE + String.valueOf(s.losses));
            skullMeta.setLore(skullLore);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(15, skull);

        // X head at left-bottom slot (18)
        ItemMeta xmeta = xhead.getItemMeta();
        if (xmeta != null) { xmeta.setDisplayName(org.bukkit.ChatColor.RED + "Abbruch"); xhead.setItemMeta(xmeta); }
        inv.setItem(18, xhead);

        // place nether star at right bottom (26)
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = star.getItemMeta();
        if (starMeta != null) { starMeta.setDisplayName(org.bukkit.ChatColor.GOLD + "Leaderboard"); star.setItemMeta(starMeta); }
        inv.setItem(26, star);

        // place own head in center slot 13 showing only W/L ratio
        ItemStack ownHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta ownMeta = (SkullMeta) ownHead.getItemMeta();
        if (ownMeta != null) {
            ownMeta.setOwningPlayer(p);
            var s = duelManager.getStatsForPlayer(p.getUniqueId());
            double ratio = s.losses==0 ? s.wins : ((double)s.wins)/(double)s.losses;
            ownMeta.setDisplayName(org.bukkit.ChatColor.YELLOW + p.getName());
            java.util.List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.GRAY + "W/L: " + org.bukkit.ChatColor.WHITE + String.format("%.1f", ratio));
            ownMeta.setLore(lore);
            ownHead.setItemMeta(ownMeta);
        }
        inv.setItem(13, ownHead);

        p.openInventory(inv);
    }
}