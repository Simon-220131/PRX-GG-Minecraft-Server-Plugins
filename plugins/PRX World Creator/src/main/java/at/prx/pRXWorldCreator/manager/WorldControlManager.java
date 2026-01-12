package at.prx.pRXWorldCreator.manager;

import at.prx.pRXWorldCreator.util.*;
import at.prx.pRXWorldCreator.worldGenerator.VoidChunkGenerator;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class WorldControlManager {

    private final JavaPlugin plugin;
    private final Map<UUID, String> pendingDeletes = new HashMap<>();

    public WorldControlManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CreateWorldResult createWorld(String name, String type) {

        if (worldExists(name))
            return CreateWorldResult.WORLD_ALREADY_EXISTS;

        WorldCreator creator = new WorldCreator(name);

        switch (type.toLowerCase()) {
            case "flat" -> creator.type(WorldType.FLAT);
            case "nether" -> creator.environment(World.Environment.NETHER);
            case "end" -> creator.environment(World.Environment.THE_END);
            case "normal" -> creator.type(WorldType.NORMAL);
            case "void" -> {
                creator.type(WorldType.FLAT);
                creator.generator(new VoidChunkGenerator());
            }
            default -> {
                return CreateWorldResult.INVALID_TYPE;
            }
        }

        Bukkit.createWorld(creator);
        return CreateWorldResult.SUCCESS;
    }

    public boolean deleteWorld(String name) {

        World world = Bukkit.getWorld(name);
        if (world == null)
            return false;

        if (Bukkit.getWorlds().get(0).equals(world))
            return false;

        // Spieler raus
        World fallback = Bukkit.getWorlds().get(0);
        for (Player p : world.getPlayers()) {
            p.teleport(fallback.getSpawnLocation());
        }

        Bukkit.unloadWorld(world, false);

        return FileUtils.deleteFolder(world.getWorldFolder());
    }

    public DeleteRequestResult requestDelete(Player player, String worldName) {

        if (!worldExists(worldName))
            return DeleteRequestResult.WORLD_NOT_FOUND;

        if (!worldName.startsWith("world"))
            return DeleteRequestResult.WORLD_IS_NOT_A_WORLD;

        World main = Bukkit.getWorlds().get(0);
        if (main.getName().equalsIgnoreCase(worldName))
            return DeleteRequestResult.MAIN_WORLD;

        pendingDeletes.put(player.getUniqueId(), worldName);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> pendingDeletes.remove(player.getUniqueId()),
                20L * 30
        );

        return DeleteRequestResult.SUCCESS;
    }

    public ConfirmDeleteResult confirmDelete(Player player) {

        String worldName = pendingDeletes.remove(player.getUniqueId());
        if (worldName == null)
            return ConfirmDeleteResult.NO_PENDING_DELETE;

        if (!deleteWorld(worldName))
            return ConfirmDeleteResult.DELETE_FAILED;

        return ConfirmDeleteResult.SUCCESS;
    }

    public boolean worldExists(String worldName) {

        if (Bukkit.getWorld(worldName) != null)
            return true;

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        return worldFolder.exists() && worldFolder.isDirectory();
    }


    public TeleportResult teleportPlayer(String playerName, String worldName) {

        Player target = Bukkit.getPlayer(playerName);
        if (target == null)
            return TeleportResult.PLAYER_NOT_FOUND;

        if (!worldExists(worldName))
            return TeleportResult.WORLD_NOT_FOUND;

        World world = Bukkit.getWorld(worldName);

        if (world == null)
            return TeleportResult.WORLD_NOT_LOADED;

        target.teleport(world.getSpawnLocation());
        return TeleportResult.SUCCESS;
    }


    public List<String> getAllWorldNames() {

        File container = Bukkit.getWorldContainer();

        return Arrays.stream(Objects.requireNonNull(container.listFiles(File::isDirectory)))
                .map(File::getName)
                .filter(name -> name.startsWith("world"))
                .toList();
    }

    public WorldInfo getWorldInfo(String worldName) {

        // Welt existiert nicht
        if (!worldExists(worldName))
            return null;

        World world = Bukkit.getWorld(worldName);

        // 🌍 Welt NICHT geladen
        if (world == null) {
            return new WorldInfo(
                    worldName,
                    false,
                    null,
                    null,
                    0,
                    null,
                    0,
                    null,
                    null,
                    0
            );
        }

        // 🌍 Welt geladen
        String weather =
                world.isThundering() ? "Gewitter"
                        : world.hasStorm() ? "Regen"
                        : "Klar";

        return new WorldInfo(
                world.getName(),
                true,
                world.getWorldType(),
                world.getEnvironment(),
                world.getPlayers().size(),
                world.getSpawnLocation(),
                world.getTime(),
                weather,
                world.getDifficulty(),
                world.getSeed()
        );
    }

//    public LoadWorldResult loadWorld(String worldName) {
//
//        if (!worldExists(worldName))
//            return LoadWorldResult.WORLD_NOT_FOUND;
//
//        if (Bukkit.getWorld(worldName) != null)
//            return LoadWorldResult.ALREADY_LOADED;
//
//        WorldInfo info = getWorldInfo(worldName);
//
//        World newWorld = Bukkit.createWorld(new WorldCreator(worldName)
//                .environment(info.environment())
//                .type(info.type())
//        );
//
//        if (info.type() == WorldType.FLAT && info.environment() == World.Environment.NORMAL) {
//
//        }
//
//        return LoadWorldResult.SUCCESS;
//    }
//
//    public UnloadWorldResult unloadWorld(String worldName) {
//
//        World world = Bukkit.getWorld(worldName);
//
//        if (world == null) {
//            if (!worldExists(worldName))
//                return UnloadWorldResult.WORLD_NOT_FOUND;
//            return UnloadWorldResult.NOT_LOADED;
//        }
//
//        World main = Bukkit.getWorlds().get(0);
//        if (world.equals(main))
//            return UnloadWorldResult.MAIN_WORLD;
//
//        // Spieler raus teleportieren
//        for (Player p : world.getPlayers()) {
//            p.teleport(main.getSpawnLocation());
//        }
//
//        Bukkit.unloadWorld(world, true);
//        return UnloadWorldResult.SUCCESS;
//    }


}
