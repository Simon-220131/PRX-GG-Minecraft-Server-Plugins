package at.htlle.duel;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HazardTracker {

    // TNT: primed TNT entity UUID -> owner player UUID
    private final Map<UUID, UUID> tntOwner = new ConcurrentHashMap<>();

    // End Crystal: crystal entity UUID -> owner player UUID (who hit it)
    private final Map<UUID, UUID> crystalOwner = new ConcurrentHashMap<>();

    // Lava/Fire: block key -> owner player UUID (short TTL via scheduled cleanup if wanted)
    private final Map<BlockKey, UUID> lavaOwner = new ConcurrentHashMap<>();
    private final Map<BlockKey, UUID> fireOwner = new ConcurrentHashMap<>();
    // Placed blocks: track blocks placed by duel players so only they can break them
    private final Map<BlockKey, UUID> placedBlocks = new ConcurrentHashMap<>();

    public void setTntOwner(Entity tnt, UUID owner) {
        tntOwner.put(tnt.getUniqueId(), owner);
    }

    public UUID getTntOwner(Entity tnt) {
        return tntOwner.get(tnt.getUniqueId());
    }

    public void forgetTnt(Entity tnt) {
        tntOwner.remove(tnt.getUniqueId());
    }

    public void setCrystalOwner(Entity crystal, UUID owner) {
        crystalOwner.put(crystal.getUniqueId(), owner);
    }

    public UUID getCrystalOwner(Entity crystal) {
        return crystalOwner.get(crystal.getUniqueId());
    }

    public void forgetCrystal(Entity crystal) {
        crystalOwner.remove(crystal.getUniqueId());
    }

    public UUID getLavaOwner(org.bukkit.block.Block block) {
        return lavaOwner.get(BlockKey.of(block));
    }

    public void setLavaOwner(org.bukkit.block.Block block, UUID owner) {
        lavaOwner.put(BlockKey.of(block), owner);
    }

    public UUID getLavaOwnerNear(Location loc) {
        // Check block at feet + surrounding (simple & effective)
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        UUID u;
        u = lavaOwner.get(new BlockKey(x, y, z)); if (u != null) return u;
        u = lavaOwner.get(new BlockKey(x, y - 1, z)); if (u != null) return u;

        // 4-neighborhood
        u = lavaOwner.get(new BlockKey(x + 1, y, z)); if (u != null) return u;
        u = lavaOwner.get(new BlockKey(x - 1, y, z)); if (u != null) return u;
        u = lavaOwner.get(new BlockKey(x, y, z + 1)); if (u != null) return u;
        u = lavaOwner.get(new BlockKey(x, y, z - 1)); if (u != null) return u;

        return null;
    }

    public void setFireOwner(Block block, UUID owner) {
        fireOwner.put(BlockKey.of(block), owner);
    }

    public void setPlacedBlock(org.bukkit.block.Block block, UUID owner) {
        placedBlocks.put(BlockKey.of(block), owner);
    }

    public UUID getPlacedBlockOwner(org.bukkit.block.Block block) {
        return placedBlocks.get(BlockKey.of(block));
    }

    public void removePlacedBlock(org.bukkit.block.Block block) {
        placedBlocks.remove(BlockKey.of(block));
    }

    public UUID getPlacedBlockOwnerNear(org.bukkit.Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        UUID u;
        u = placedBlocks.get(new BlockKey(x, y, z)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x, y - 1, z)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x, y + 1, z)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x + 1, y, z)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x - 1, y, z)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x, y, z + 1)); if (u != null) return u;
        u = placedBlocks.get(new BlockKey(x, y, z - 1)); if (u != null) return u;

        return null;
    }

    public UUID getFireOwnerNear(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        UUID u;
        u = fireOwner.get(new BlockKey(x, y, z)); if (u != null) return u;
        u = fireOwner.get(new BlockKey(x, y + 1, z)); if (u != null) return u;
        u = fireOwner.get(new BlockKey(x, y, z + 1)); if (u != null) return u;
        u = fireOwner.get(new BlockKey(x, y, z - 1)); if (u != null) return u;
        u = fireOwner.get(new BlockKey(x + 1, y, z)); if (u != null) return u;
        u = fireOwner.get(new BlockKey(x - 1, y, z)); if (u != null) return u;

        return null;
    }

    public record BlockKey(int x, int y, int z) {
        public static BlockKey of(Block b) {
            return new BlockKey(b.getX(), b.getY(), b.getZ());
        }
    }
}