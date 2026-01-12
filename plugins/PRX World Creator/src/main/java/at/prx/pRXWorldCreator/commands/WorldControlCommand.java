package at.prx.pRXWorldCreator.commands;

import at.prx.pRXWorldCreator.manager.WorldControlManager;
import at.prx.pRXWorldCreator.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class WorldControlCommand implements CommandExecutor, TabCompleter {

    private final WorldControlManager manager;

    public WorldControlCommand(WorldControlManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§c/wc create|delete|tp|list");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create" -> {

                if (args.length < 2) {
                    sender.sendMessage(
                            MessageUtil.message(
                                    "<gray>ℹ Nutze <yellow>/wc create <world> [normal|flat|nether|end]</yellow></gray>"
                            )
                    );
                    return true;
                }

                String worldName = "world_" + args[1].toLowerCase();
                String type = args.length >= 3 ? args[2].toLowerCase() : "normal";

                CreateWorldResult result =
                        manager.createWorld(worldName, type);

                switch (result) {

                    case WORLD_ALREADY_EXISTS -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Die Welt <yellow>" + worldName + "</yellow> existiert bereits</red>")
                    );

                    case INVALID_TYPE -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Ungültiger Welt-Typ</red> <gray>(normal, flat, nether, end)</gray>"
                            )
                    );

                    case SUCCESS -> sender.sendMessage(
                            MessageUtil.message(
                                    "<green>✔ Welt <yellow>" + worldName + "</yellow> erstellt</green> "
                                            + "<gray>(Typ: <yellow>{type}</yellow>)</gray>"
                            ).replaceText(builder ->
                                    builder.match("\\{type\\}")
                                            .replacement(type)
                            )
                    );

                }
            }

            case "delete" -> {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(
                            MessageUtil.message("<red>❌ Dieser Befehl kann nur von Spielern ausgeführt werden</red>")
                    );
                    return true;
                }

                if (args.length != 2) {
                    sender.sendMessage(
                            MessageUtil.message("<gray>ℹ Nutze <yellow>/wc delete <world></yellow></gray>")
                    );
                    return true;
                }

                String worldName = args[1];

                DeleteRequestResult result =
                        manager.requestDelete(player, worldName);

                switch (result) {

                    case WORLD_NOT_FOUND -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> existiert nicht</red>")
                    );

                    case MAIN_WORLD -> sender.sendMessage(
                            MessageUtil.message("<red>❌ Die Hauptwelt kann nicht gelöscht werden</red>")
                    );

                    case WORLD_IS_NOT_A_WORLD -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ <yellow>" + worldName + "</yellow> ist keine gültige Minecraft-Welt</red>")

                    );

                    case SUCCESS -> {
                        sender.sendMessage(
                                MessageUtil.message(
                                        "<red>⚠ Möchtest du die Welt <yellow>" + worldName + "</yellow> wirklich löschen?</red>")
                        );
                        sender.sendMessage(
                                MessageUtil.message("<gray>→ Bestätige mit <yellow>/wc confirm</yellow></gray>")
                                        .clickEvent(ClickEvent.runCommand("/wc confirm"))
                        );
                    }
                }
            }

            case "confirm" -> {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(
                            MessageUtil.message("<red>❌ Dieser Befehl kann nur von Spielern ausgeführt werden</red>")
                    );
                    return true;
                }

                ConfirmDeleteResult result =
                        manager.confirmDelete(player);

                switch (result) {

                    case NO_PENDING_DELETE -> sender.sendMessage(
                            MessageUtil.message("<red>❌ Keine Löschung ausstehend</red>")
                    );

                    case DELETE_FAILED -> sender.sendMessage(
                            MessageUtil.message("<red>❌ Welt konnte nicht gelöscht werden</red>")
                    );

                    case SUCCESS -> sender.sendMessage(
                            MessageUtil.message("<green>✔ Welt wurde gelöscht</green>")
                    );
                }
            }

            case "tp" -> {

                if (args.length != 3) {
                    sender.sendMessage(
                            MessageUtil.message("<gray>ℹ Nutze <yellow>/wc tp <player> <world></yellow></gray>")
                    );
                    return true;
                }

                String targetName = args[1];
                String worldName = args[2];

                TeleportResult result =
                        manager.teleportPlayer(targetName, worldName);

                switch (result) {

                    case PLAYER_NOT_FOUND -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Spieler <yellow>" + targetName + "</yellow> nicht gefunden</red>")
                    );

                    case WORLD_NOT_FOUND -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> existiert nicht</red>")
                    );

                    case WORLD_NOT_LOADED -> sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> ist nicht geladen</red> "
                                            + "<gray>(nutze <yellow>/wc load <world></yellow>)</gray>")
                                    .clickEvent(ClickEvent.runCommand("/wc load " + worldName))

                    );

                    case SUCCESS -> {

                        sender.sendMessage(
                                MessageUtil.message(
                                        "<green>✔ Spieler <yellow>" + targetName + "</yellow> wurde teleportiert</green>")
                        );

                        Player target = Bukkit.getPlayer(targetName);
                        if (target != null) {
                            target.sendMessage(
                                    MessageUtil.message(
                                            "<green>✔ Du wurdest in die Welt <yellow>" + worldName + "</yellow> teleportiert</green>")
                            );
                        }
                    }
                }
            }

            case "list" -> {
                Component check = MessageUtil.raw("<green>✔ </green>");
                Component cross = MessageUtil.raw("<gray>✖ </gray>");

                sender.sendMessage(
                        MessageUtil.message("<aqua>📜 Verfügbare Welten:</aqua>")
                );

                for (String world : manager.getAllWorldNames()) {

                    boolean loaded = Bukkit.getWorld(world) != null;

                    Component line = (loaded ? check : cross)
                            .append(Component.text(world, NamedTextColor.WHITE));

                    sender.sendMessage(line);
                }

            }

            case "info" -> {

                if (args.length != 2) {
                    sender.sendMessage(
                            MessageUtil.message(
                                    "<gray>ℹ Nutze <yellow>/wc info <world></yellow></gray>"
                            )
                    );
                    return true;
                }

                String worldName = args[1];

                WorldInfo info =
                        manager.getWorldInfo(worldName);

                if (info == null) {
                    sender.sendMessage(
                            MessageUtil.message(
                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> existiert nicht</red>")
                    );
                    return true;
                }

                // 📘 Header
                sender.sendMessage(
                        MessageUtil.message("<aqua>📘 Welt-Informationen</aqua>")
                );
                sender.sendMessage(
                        MessageUtil.message("<dark_gray>────────────────────</dark_gray>")
                );

                // 🌍 Basis
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🌍 Name:</gray> <white>" + info.name() + "</white>")
                );

                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>📦 Status:</gray> " +
                                        (info.loaded()
                                                ? "<green>✔ geladen</green>"
                                                : "<red>✖ nicht geladen</red>")
                        )
                );

                // ❌ Wenn nicht geladen → abbrechen
                if (!info.loaded())
                    return true;

                // 🗺 Typ & Env
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🗺 Typ:</gray> <white>" + info.type().name() + "</white>")
                );

                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🔥 Environment:</gray> <white>" + info.environment().name() + "</white>")
                );

                // 👥 Spieler
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>👥 Spieler:</gray> <white>" + info.players() + "</white>")
                );

                // 📍 Spawn
                Location s = info.spawn();

                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>📍 Spawn:</gray> <white>x=" + s.getBlockX() + " y=" + s.getBlockY() + " z=" + s.getBlockZ() + "</white>")
                        );


                // 🕒 Zeit
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🕒 Zeit:</gray> <white>" + info.time() + "</white>")
                );

                // 🌦 Wetter
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🌦 Wetter:</gray> <white>" + info.weather() + "</white>")
                );

                // ⚔ Difficulty
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>⚔ Difficulty:</gray> <white>" + info.difficulty().name() + "</white>")
                );

                // 🌱 Seed
                sender.sendMessage(
                        MessageUtil.message(
                                "<gray>🌱 Seed:</gray> <white>" + info.seed() + "</white>")
                );
            }

//            case "load" -> {
//
//                if (args.length != 2) {
//                    sender.sendMessage(
//                            MessageUtil.message("<gray>ℹ Nutze <yellow>/wc load <world></yellow></gray>")
//                    );
//                    return true;
//                }
//
//                String worldName = args[1];
//
//                LoadWorldResult result =
//                        manager.loadWorld(worldName);
//
//                switch (result) {
//
//                    case WORLD_NOT_FOUND -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> existiert nicht</red>")
//                    );
//
//                    case ALREADY_LOADED -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<gray>ℹ Welt <yellow>" + worldName + "</yellow> ist bereits geladen</gray>")
//                    );
//
//                    case SUCCESS -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<green>✔ Welt <yellow>" + worldName + "</yellow> wurde geladen</green>")
//                    );
//                }
//            }
//
//            case "unload" -> {
//
//                if (args.length != 2) {
//                    sender.sendMessage(
//                            MessageUtil.message("<gray>ℹ Nutze <yellow>/wc unload <world></yellow></gray>")
//                    );
//                    return true;
//                }
//
//                String worldName = args[1];
//
//                UnloadWorldResult result =
//                        manager.unloadWorld(worldName);
//
//                switch (result) {
//
//                    case WORLD_NOT_FOUND -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<red>❌ Welt <yellow>" + worldName + "</yellow> existiert nicht</red>")
//                    );
//
//                    case NOT_LOADED -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<gray>ℹ Welt <yellow>" + worldName + "</yellow> ist nicht geladen</gray>")
//                    );
//
//                    case MAIN_WORLD -> sender.sendMessage(
//                            MessageUtil.message("<red>❌ Die Hauptwelt kann nicht entladen werden</red>")
//                    );
//
//                    case SUCCESS -> sender.sendMessage(
//                            MessageUtil.message(
//                                    "<green>✔ Welt <yellow>" + worldName + "</yellow> wurde entladen</green>")
//                    );
//                }
//            }


        }
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command cmd,
            String alias,
            String[] args
    ) {

        // 🔹 Haupt-Subcommands
        if (args.length == 1) {
            return List.of("create", "delete", "confirm", "tp", "list", "info") // "load", "unload"
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // 🔹 /wc create <world> <type>
        if (args[0].equalsIgnoreCase("create")) {

            if (args.length == 3) {
                return List.of("normal", "flat","void", "nether", "end")
                        .stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .toList();
            }
        }

        // 🔹 /wc delete <world>
        if (args[0].equalsIgnoreCase("delete")) {

            if (args.length == 2) {
                return manager.getAllWorldNames().stream()
                        .filter(w -> w.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        // 🔹 /wc info <world>
        if (args[0].equalsIgnoreCase("info")) {

            if (args.length == 2) {
                return manager.getAllWorldNames().stream()
                        .filter(w -> w.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        // 🔹 /wc tp <player> <world>
        if (args[0].equalsIgnoreCase("tp")) {

            if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (args.length == 3) {
                return manager.getAllWorldNames().stream()
                        .filter(w -> w.startsWith(args[2].toLowerCase()))
                        .toList();
            }
        }

//        // 🔹 /wc load <world>
//        if (args[0].equalsIgnoreCase("load") && args.length == 2) {
//            return manager.getAllWorldNames().stream()
//                    .filter(w -> w.startsWith(args[1].toLowerCase()))
//                    .toList();
//        }
//
//        // 🔹 /wc unload <world>
//        if (args[0].equalsIgnoreCase("unload") && args.length == 2) {
//            return Bukkit.getWorlds().stream()
//                    .map(World::getName)
//                    .filter(w -> w.startsWith(args[1].toLowerCase()))
//                    .toList();
//        }

        // 🔹 /wc confirm, /wc list → keine weiteren Argumente
        return Collections.emptyList();
    }


}

