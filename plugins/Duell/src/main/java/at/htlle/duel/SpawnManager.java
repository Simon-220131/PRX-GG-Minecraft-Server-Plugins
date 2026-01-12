package at.htlle.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {

    private final FileConfiguration config;

    public SpawnManager(FileConfiguration config) {
        this.config = config;
    }

    /* ================= PATHS ================= */
    private static final String DUEL_PATH = "duel-spawns";
    private static final String SPECT_PATH = "spectator-spawns";

    /* ================= GET RANDOM (WORLD FILTER) ================= */

    public Location getRandomSpectatorSpawn(World w) {
        return getRandomInWorld(SPECT_PATH, w);
    }

    public Location[] getTwoRandomDuelSpawns(World w) {
        List<Location> list = loadInWorld(DUEL_PATH, w);
        if (list.size() < 2) return null;

        int a = ThreadLocalRandom.current().nextInt(list.size());
        int b;
        do { b = ThreadLocalRandom.current().nextInt(list.size()); } while (b == a);

        return new Location[]{ list.get(a).clone(), list.get(b).clone() };
    }

    private Location getRandomInWorld(String path, World w) {
        List<Location> list = loadInWorld(path, w);
        if (list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size())).clone();
    }

    public List<Location> loadInWorld(String path, World w) {
        List<Location> out = new ArrayList<>();
        for (Location l : loadAll(path)) {
            if (l.getWorld() != null && l.getWorld().equals(w)) out.add(l);
        }
        return out;
    }

    public World findWorldWithAtLeastDuelSpawns(int min) {
        World best = null;
        int bestCount = -1;

        for (World w : Bukkit.getWorlds()) {
            int count = loadInWorld(DUEL_PATH, w).size();
            if (count >= min) return w; // sofort ok
            if (count > bestCount) { bestCount = count; best = w; }
        }
        return null; // keine Welt erfüllt min
    }

    public Location[] getTwoRandomDuelSpawnsAnyWorldPrefer(World preferred) {
        // 1) preferred versuchen
        if (preferred != null) {
            Location[] picks = getTwoRandomDuelSpawns(preferred);
            if (picks != null) return picks;
        }
        // 2) irgendeine Welt mit >=2 spawns suchen
        World w = findWorldWithAtLeastDuelSpawns(2);
        if (w == null) return null;
        return getTwoRandomDuelSpawns(w);
    }

    public boolean hasEnoughDuelSpawns(World w, int min) {
        if (w == null) return false;
        return loadInWorld(DUEL_PATH, w).size() >= min;
    }

    public List<String> getWorldNamesWithEnoughDuelSpawns(int min) {
        List<String> out = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            if (hasEnoughDuelSpawns(w, min)) out.add(w.getName());
        }
        return out;
    }

    public World pickRandomWorldWithEnoughDuelSpawns(int min) {
        List<World> candidates = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            if (hasEnoughDuelSpawns(w, min)) candidates.add(w);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /* ================= ADD ================= */

    public int addDuelSpawn(Location loc) {
        return addAndReturnIndex(DUEL_PATH, loc);
    }

    public int addSpectatorSpawn(Location loc) {
        return addAndReturnIndex(SPECT_PATH, loc);
    }

    private int addAndReturnIndex(String path, Location l) {
        List<String> list = config.getStringList(path);
        list.add(serialize(l));
        config.set(path, list);
        return list.size(); // 1-based index
    }

    /* ================= LIST ================= */

    public List<Location> getAllDuelSpawns() {
        return loadAll(DUEL_PATH);
    }

    public List<Location> getAllSpectatorSpawns() {
        return loadAll(SPECT_PATH);
    }

    /* ================= REMOVE / CLEAR (INDEX BASED) ================= */

    public boolean removeDuelSpawn(int index1based) {
        return removeAt(DUEL_PATH, index1based);
    }

    public boolean removeSpectatorSpawn(int index1based) {
        return removeAt(SPECT_PATH, index1based);
    }

    public void clearDuelSpawns() {
        config.set(DUEL_PATH, new ArrayList<String>());
    }

    public void clearSpectatorSpawns() {
        config.set(SPECT_PATH, new ArrayList<String>());
    }

    private boolean removeAt(String path, int index1based) {
        List<String> list = config.getStringList(path);
        int idx = index1based - 1;
        if (idx < 0 || idx >= list.size()) return false;
        list.remove(idx);                // <-- dadurch rutscht alles automatisch nach
        config.set(path, list);
        return true;
    }

    /* ================= CLEANUP (MISSING WORLDS) ================= */

    /** Entfernt Spawns, deren Welt nicht (mehr) existiert/geladen ist. */
    public int cleanupMissingWorlds() {
        int removed = 0;
        removed += cleanupPath(DUEL_PATH);
        removed += cleanupPath(SPECT_PATH);
        return removed;
    }

    private int cleanupPath(String path) {
        List<String> list = config.getStringList(path);
        if (list.isEmpty()) return 0;

        int before = list.size();
        list.removeIf(s -> {
            String worldName = s.split(";", 2)[0];
            return Bukkit.getWorld(worldName) == null;
        });
        config.set(path, list);
        return before - list.size();
    }

    /* ================= SERIALIZE ================= */

    private String serialize(Location l) {
        return l.getWorld().getName() + ";"
                + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";"
                + l.getYaw() + ";" + l.getPitch();
    }

    private Location deserialize(String s) {
        try {
            String[] p = s.split(";");
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            return new Location(
                    w,
                    Double.parseDouble(p[1]),
                    Double.parseDouble(p[2]),
                    Double.parseDouble(p[3]),
                    Float.parseFloat(p[4]),
                    Float.parseFloat(p[5])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private List<Location> loadAll(String path) {
        List<Location> out = new ArrayList<>();
        List<String> raw = config.getStringList(path);
        for (String s : raw) {
            Location l = deserialize(s);
            if (l != null) out.add(l);
        }
        return out;
    }
}